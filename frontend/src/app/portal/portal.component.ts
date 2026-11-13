import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { DIABETES_LABELS, Patient } from '../patients/patient.model';
import { PatientService } from '../patients/patient.service';
import {
  AnalysisParameter,
  EvolutionResponse,
  FLAG_LABELS,
  LabResult,
  ValueFlag,
  criticalText,
  errorText,
  isCriticalFlag,
  rangeText,
} from '../results/result.model';
import { ResultService } from '../results/result.service';
import { LineChartComponent } from '../shared/line-chart.component';

/** Patient space: own record, own VALIDATED results and their evolution. */
@Component({
  selector: 'app-portal',
  standalone: true,
  imports: [FormsModule, DatePipe, RouterLink, LineChartComponent],
  template: `
    <div class="toolbar">
      <h2>Mon dossier</h2>
      <span class="spacer"></span>
      <a routerLink="/portal/glucose">Ma glycémie →</a>
    </div>

    @if (error) {
      <p class="error">{{ error }}</p>
    }

    @if (patient(); as p) {
      <div class="card">
        <h2>{{ p.firstName }} {{ p.lastName }}</h2>
        <div class="info-grid">
          <div><span class="muted">Date de naissance</span><br />{{ p.dateOfBirth | date: 'dd/MM/yyyy' }}</div>
          <div><span class="muted">Sexe</span><br />{{ p.gender || '—' }}</div>
          <div><span class="muted">Téléphone</span><br />{{ p.phone || '—' }}</div>
          <div><span class="muted">E-mail</span><br />{{ p.email || '—' }}</div>
          <div><span class="muted">Diabète</span><br />{{ p.diabetesType ? labels[p.diabetesType] : '—' }}</div>
          <div class="full"><span class="muted">Allergies</span><br />{{ p.allergies || '—' }}</div>
        </div>
      </div>
    }

    <h2>Mes résultats d'analyses</h2>
    <p class="muted">Seuls les résultats validés par votre médecin sont affichés.</p>

    @for (r of results(); track r.id) {
      <div class="card">
        <div class="consultation-head">
          <strong>{{ r.resultDate | date: 'dd/MM/yyyy' }}</strong>
          <span class="spacer"></span>
          @if (r.hasFile) {
            <button class="secondary" (click)="openFile(r)">Voir le document original</button>
          }
        </div>
        <table>
          <thead>
            <tr>
              <th>Paramètre</th>
              <th>Valeur</th>
              <th>Valeurs de référence</th>
              <th>Interprétation</th>
            </tr>
          </thead>
          <tbody>
            @for (v of r.values; track v.id) {
              <tr>
                <td>{{ v.parameter.label }}</td>
                <td>
                  <strong [class]="'flag ' + v.flag">{{ v.value }}</strong>
                  <span class="muted">{{ v.parameter.unit }}</span>
                </td>
                <td class="muted">
                  {{ range(v.parameter) }}
                  @if (critical(v.parameter); as c) {
                    <br /><small>{{ c }}</small>
                  }
                </td>
                <td>
                  <span
                    class="badge"
                    [class.ok]="v.flag === 'NORMAL'"
                    [class.danger]="v.flag !== 'NORMAL'"
                    [class.critical]="isCritical(v.flag)"
                  >
                    {{ flagLabels[v.flag] }}
                  </span>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    } @empty {
      @if (!loading) {
        <p class="muted">Aucun résultat validé pour le moment.</p>
      }
    }

    @if (parameters().length > 0) {
      <div class="toolbar">
        <h2>Évolution</h2>
        <span class="spacer"></span>
        <select class="param-select" name="parameter" [ngModel]="code()" (ngModelChange)="select($event)">
          @for (p of parameters(); track p.code) {
            <option [value]="p.code">{{ p.label }}</option>
          }
        </select>
      </div>

      @if (evolution(); as e) {
        <div class="card">
          <p class="muted">Valeurs de référence : {{ range(e.parameter) }}.</p>
          <app-line-chart
            [points]="e.points"
            [refMin]="e.parameter.refMin"
            [refMax]="e.parameter.refMax"
            [unit]="e.parameter.unit"
          />
          <p class="legend">
            <span><i class="dot-key NORMAL"></i> Normal</span>
            <span><i class="dot-key HIGH"></i> Élevé</span>
            <span><i class="dot-key LOW"></i> Bas</span>
            <span><i class="dot-key CRITICAL_HIGH"></i> Critique</span>
            <span><i class="band-key"></i> Valeurs de référence</span>
          </p>
        </div>
      }
    }
  `,
})
export class PortalComponent implements OnInit {
  private patientService = inject(PatientService);
  private resultService = inject(ResultService);

  readonly labels = DIABETES_LABELS;
  readonly flagLabels = FLAG_LABELS;
  readonly range = rangeText;
  readonly critical = criticalText;
  readonly isCritical = (flag: ValueFlag): boolean => isCriticalFlag(flag);

  patient = signal<Patient | null>(null);
  results = signal<LabResult[]>([]);
  /** Parameters that appear in the patient's own results (the catalogue is not open to patients). */
  parameters = signal<AnalysisParameter[]>([]);
  code = signal('');
  evolution = signal<EvolutionResponse | null>(null);
  error = '';
  loading = true;

  ngOnInit(): void {
    this.patientService.me().subscribe({
      next: (p) => this.patient.set(p),
      error: () => (this.error = 'Impossible de charger votre dossier.'),
    });
    this.resultService.mine().subscribe({
      next: (list) => {
        this.loading = false;
        const sorted = [...list].sort((a, b) => b.resultDate.localeCompare(a.resultDate));
        this.results.set(sorted);
        const byCode = new Map<string, AnalysisParameter>();
        for (const r of sorted) {
          for (const v of r.values) {
            if (!byCode.has(v.parameter.code)) {
              byCode.set(v.parameter.code, v.parameter);
            }
          }
        }
        const params = [...byCode.values()];
        this.parameters.set(params);
        const first = params.find((p) => p.code === 'HBA1C') ?? params[0];
        if (first) {
          this.select(first.code);
        }
      },
      error: (err) => {
        this.loading = false;
        this.error = errorText(err, 'Impossible de charger vos résultats.');
      },
    });
  }

  select(code: string): void {
    this.code.set(code);
    this.resultService.mineEvolution(code).subscribe({
      next: (e) => this.evolution.set(e),
      error: (err) => (this.error = errorText(err, "Impossible de charger l'évolution.")),
    });
  }

  openFile(result: LabResult): void {
    this.resultService.mineFile(result.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.target = '_blank';
        link.rel = 'noopener';
        link.click();
        setTimeout(() => URL.revokeObjectURL(url), 60_000);
      },
      error: (err) => (this.error = errorText(err, 'Document introuvable.')),
    });
  }
}
