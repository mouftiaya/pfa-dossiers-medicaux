import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { Consultation } from '../consultations/consultation.model';
import { ConsultationService } from '../consultations/consultation.service';
import { Patient } from '../patients/patient.model';
import { PatientService } from '../patients/patient.service';
import {
  AnalysisParameter,
  ComparisonRow,
  EvolutionPoint,
  FLAG_LABELS,
  LabResult,
  MODE_LABELS,
  STATUS_LABELS,
  errorText,
} from './result.model';
import { ResultService } from './result.service';

interface Row {
  parameterId: number | null;
  value: number | null;
}

const today = () => new Date().toISOString().slice(0, 10);

@Component({
  selector: 'app-results',
  standalone: true,
  imports: [FormsModule, DatePipe, RouterLink],
  template: `
    <a [routerLink]="['/patients', patientId]">← Dossier du patient</a>

    <div class="toolbar">
      <h2>
        Résultats d'analyses
        @if (patient(); as p) {
          <span class="muted"> — {{ p.firstName }} {{ p.lastName }}</span>
        }
      </h2>
      <span class="spacer"></span>
      <a [routerLink]="['/patients', patientId, 'evolution']">Évolution glycémie / HbA1c</a>
      <button (click)="open('upload')">{{ mode() === 'upload' ? 'Annuler' : 'Importer un fichier' }}</button>
      <button class="secondary" (click)="open('manual')">
        {{ mode() === 'manual' ? 'Annuler' : 'Saisie manuelle' }}
      </button>
    </div>

    @if (error) {
      <p class="error">{{ error }}</p>
    }

    @if (mode() === 'upload') {
      <form class="card form-grid" (ngSubmit)="submitUpload()">
        <div class="full">
          <p class="muted">
            Importez le document apporté par le patient (PDF, PNG ou JPEG, 10 Mo maximum). Seuls les paramètres connus
            du système (glycémie, HbA1c, …) sont extraits. Vous pourrez vérifier et corriger les valeurs ensuite.
          </p>
        </div>
        <div>
          <label for="uploadDate">Date de l'analyse</label>
          <input id="uploadDate" name="uploadDate" type="date" [(ngModel)]="date" [max]="maxDate" required />
        </div>
        <div>
          <label for="uploadConsultation">Consultation liée (facultatif)</label>
          <select id="uploadConsultation" name="uploadConsultation" [(ngModel)]="consultationId">
            <option [ngValue]="null">Aucune</option>
            @for (c of consultations(); track c.id) {
              <option [ngValue]="c.id">{{ c.consultationDate | date: 'dd/MM/yyyy' }} — {{ c.reason }}</option>
            }
          </select>
        </div>
        <div class="full">
          <label for="file">Fichier</label>
          <input id="file" name="file" type="file" accept=".pdf,.png,.jpg,.jpeg" (change)="onFile($event)" />
        </div>
        <div class="full">
          <button type="submit" [disabled]="busy || !file || !date">
            {{ busy ? 'Import en cours…' : 'Importer et extraire' }}
          </button>
        </div>
      </form>
    }

    @if (mode() === 'manual') {
      <form class="card" (ngSubmit)="submitManual()">
        <div class="form-grid">
          <div>
            <label for="manualDate">Date de l'analyse</label>
            <input id="manualDate" name="manualDate" type="date" [(ngModel)]="date" [max]="maxDate" required />
          </div>
          <div>
            <label for="manualConsultation">Consultation liée (facultatif)</label>
            <select id="manualConsultation" name="manualConsultation" [(ngModel)]="consultationId">
              <option [ngValue]="null">Aucune</option>
              @for (c of consultations(); track c.id) {
                <option [ngValue]="c.id">{{ c.consultationDate | date: 'dd/MM/yyyy' }} — {{ c.reason }}</option>
              }
            </select>
          </div>
        </div>

        <label>Valeurs</label>
        @for (r of rows; track $index; let i = $index) {
          <div class="value-row">
            <select [name]="'param' + i" [(ngModel)]="r.parameterId">
              <option [ngValue]="null">Paramètre…</option>
              @for (p of catalogue(); track p.id) {
                <option [ngValue]="p.id">{{ p.label }} ({{ p.unit }})</option>
              }
            </select>
            <input [name]="'value' + i" type="number" step="any" min="0" placeholder="Valeur" [(ngModel)]="r.value" />
            <button type="button" class="danger" (click)="removeRow(i)" [disabled]="rows.length === 1">✕</button>
          </div>
        }
        <p><button type="button" class="secondary" (click)="addRow()">+ Ajouter un paramètre</button></p>

        <button type="submit" [disabled]="busy || !manualValid()">
          {{ busy ? 'Enregistrement…' : 'Enregistrer' }}
        </button>
      </form>
    }

    <div class="card">
      @if (list().length > 0) {
        <table>
          <thead>
            <tr>
              <th></th>
              <th>Date</th>
              <th>Source</th>
              <th>Statut</th>
              <th>Valeurs</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            @for (r of list(); track r.id) {
              <tr>
                <td>
                  @if (r.status === 'VALIDATED') {
                    <input
                      type="checkbox"
                      class="check"
                      [checked]="selected().includes(r.id)"
                      (change)="toggle(r.id)"
                      [attr.aria-label]="'Comparer le résultat du ' + r.resultDate"
                    />
                  }
                </td>
                <td>{{ r.resultDate | date: 'dd/MM/yyyy' }}</td>
                <td>{{ modeLabels[r.entryMode] }}</td>
                <td>
                  <span class="badge" [class.ok]="r.status === 'VALIDATED'" [class.warn]="r.status !== 'VALIDATED'">
                    {{ statusLabels[r.status] }}
                  </span>
                  @if (r.reviewNote) {
                    <span class="badge danger">Correction demandée</span>
                  }
                </td>
                <td>
                  {{ r.values.length }}
                  @if (abnormalCount(r) > 0) {
                    <span class="badge danger">{{ abnormalCount(r) }} anormale(s)</span>
                  }
                </td>
                <td class="actions"><a [routerLink]="['/results', r.id]">Ouvrir</a></td>
              </tr>
            }
          </tbody>
        </table>
        <p class="muted">Cochez au moins deux résultats validés pour les comparer.</p>
        <button class="secondary" [disabled]="selected().length < 2" (click)="compare()">Comparer la sélection</button>
      } @else {
        <p class="muted">Aucun résultat d'analyse pour ce patient.</p>
      }
    </div>

    @if (comparison(); as cmp) {
      <div class="card">
        <h2>Comparaison</h2>
        <table>
          <thead>
            <tr>
              <th>Paramètre</th>
              @for (c of columns(); track c.resultId) {
                <th>{{ c.date | date: 'dd/MM/yyyy' }}</th>
              }
              <th>Variation</th>
            </tr>
          </thead>
          <tbody>
            @for (row of cmp; track row.parameter.id) {
              <tr>
                <td>{{ row.parameter.label }} <span class="muted">({{ row.parameter.unit }})</span></td>
                @for (c of columns(); track c.resultId) {
                  <td>
                    @if (cell(row, c.resultId); as pt) {
                      <span [class]="'flag ' + pt.flag">{{ pt.value }}</span>
                    } @else {
                      <span class="muted">—</span>
                    }
                  </td>
                }
                <td>
                  @if (row.change !== null) {
                    {{ row.change > 0 ? '+' : '' }}{{ round(row.change) }}
                  } @else {
                    <span class="muted">—</span>
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
        <p class="muted">Rouge : au-dessus de la norme. Orange : en dessous. La variation est calculée entre la première et la dernière date.</p>
      </div>
    }
  `,
})
export class ResultsComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private patientService = inject(PatientService);
  private consultationService = inject(ConsultationService);
  private resultService = inject(ResultService);

  readonly patientId = Number(this.route.snapshot.paramMap.get('id'));
  readonly maxDate = today();
  readonly flagLabels = FLAG_LABELS;
  readonly statusLabels = STATUS_LABELS;
  readonly modeLabels = MODE_LABELS;

  patient = signal<Patient | null>(null);
  list = signal<LabResult[]>([]);
  catalogue = signal<AnalysisParameter[]>([]);
  consultations = signal<Consultation[]>([]);
  mode = signal<'none' | 'upload' | 'manual'>('none');
  selected = signal<number[]>([]);
  comparison = signal<ComparisonRow[] | null>(null);
  columns = signal<{ resultId: number; date: string }[]>([]);

  date = today();
  consultationId: number | null = null;
  file: File | null = null;
  rows: Row[] = [{ parameterId: null, value: null }];
  busy = false;
  error = '';

  ngOnInit(): void {
    this.patientService.get(this.patientId).subscribe({
      next: (p) => this.patient.set(p),
      error: () => (this.error = 'Patient introuvable.'),
    });
    this.consultationService.listForPatient(this.patientId).subscribe({
      next: (list) => this.consultations.set(list),
      error: () => this.consultations.set([]),
    });
    this.resultService.parameters().subscribe({
      next: (list) => this.catalogue.set(list),
      error: () => (this.error = 'Impossible de charger la liste des paramètres.'),
    });
    this.load();
  }

  load(): void {
    this.resultService.list(this.patientId).subscribe({
      next: (list) => this.list.set(list),
      error: (err) => (this.error = errorText(err, 'Impossible de charger les résultats.')),
    });
  }

  open(mode: 'upload' | 'manual'): void {
    this.mode.set(this.mode() === mode ? 'none' : mode);
    this.error = '';
    this.date = today();
    this.consultationId = null;
    this.file = null;
    this.rows = [{ parameterId: null, value: null }];
  }

  onFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.file = input.files && input.files.length > 0 ? input.files[0] : null;
  }

  submitUpload(): void {
    if (!this.file) {
      return;
    }
    this.busy = true;
    this.error = '';
    this.resultService.upload(this.patientId, this.date, this.file, this.consultationId).subscribe({
      next: (response) => {
        this.busy = false;
        // The detail page shows what the extraction found, for the assistant to check
        this.router.navigate(['/results', response.result.id], { state: { message: response.message } });
      },
      error: (err) => {
        this.busy = false;
        this.error = errorText(err, "Échec de l'import. Vérifiez le fichier (PDF, PNG ou JPEG, 10 Mo maximum).");
      },
    });
  }

  addRow(): void {
    this.rows.push({ parameterId: null, value: null });
  }

  removeRow(index: number): void {
    this.rows.splice(index, 1);
  }

  manualValid(): boolean {
    const ids = this.rows.map((r) => r.parameterId);
    const complete = this.rows.every((r) => r.parameterId !== null && r.value !== null && r.value >= 0);
    return complete && new Set(ids).size === ids.length && !!this.date;
  }

  submitManual(): void {
    if (!this.manualValid()) {
      this.error = 'Chaque ligne doit avoir un paramètre et une valeur, sans doublon.';
      return;
    }
    this.busy = true;
    this.error = '';
    this.resultService
      .createManual({
        patientId: this.patientId,
        consultationId: this.consultationId,
        resultDate: this.date,
        values: this.rows.map((r) => ({ parameterId: r.parameterId as number, value: r.value as number })),
      })
      .subscribe({
        next: (result) => {
          this.busy = false;
          this.router.navigate(['/results', result.id]);
        },
        error: (err) => {
          this.busy = false;
          this.error = errorText(err, "Erreur lors de l'enregistrement.");
        },
      });
  }

  abnormalCount(result: LabResult): number {
    return result.values.filter((v) => v.flag !== 'NORMAL').length;
  }

  toggle(id: number): void {
    this.selected.update((ids) => (ids.includes(id) ? ids.filter((i) => i !== id) : [...ids, id]));
    this.comparison.set(null);
  }

  compare(): void {
    this.error = '';
    this.resultService.compare(this.selected()).subscribe({
      next: (rows) => {
        const byId = new Map<number, string>();
        rows.forEach((row) => row.points.forEach((p) => byId.set(p.resultId, p.date)));
        const columns = [...byId.entries()]
          .map(([resultId, date]) => ({ resultId, date }))
          .sort((a, b) => a.date.localeCompare(b.date) || a.resultId - b.resultId);
        this.columns.set(columns);
        this.comparison.set(rows);
      },
      error: (err) => (this.error = errorText(err, 'Comparaison impossible.')),
    });
  }

  cell(row: ComparisonRow, resultId: number): EvolutionPoint | undefined {
    return row.points.find((p) => p.resultId === resultId);
  }

  round(n: number): string {
    return String(Math.round(n * 100) / 100).replace('.', ',');
  }
}
