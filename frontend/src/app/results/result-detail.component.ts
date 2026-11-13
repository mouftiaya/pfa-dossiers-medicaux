import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AiSuggestion, SUGGESTION_LABELS } from '../ai/ai.model';
import { AiService } from '../ai/ai.service';
import { AuthService } from '../auth/auth.service';
import {
  AnalysisParameter,
  FLAG_LABELS,
  LabResult,
  MODE_LABELS,
  STATUS_LABELS,
  criticalText,
  errorText,
  rangeText,
} from './result.model';
import { ResultService } from './result.service';

interface Row {
  parameterId: number | null;
  value: number | null;
}

@Component({
  selector: 'app-result-detail',
  standalone: true,
  imports: [FormsModule, DatePipe, DecimalPipe, RouterLink],
  template: `
    @if (result(); as r) {
      <a [routerLink]="['/patients', r.patientId, 'results']">← Résultats du patient</a>

      <div class="toolbar">
        <h2>Résultat du {{ r.resultDate | date: 'dd/MM/yyyy' }}</h2>
        <span class="badge" [class.ok]="r.status === 'VALIDATED'" [class.warn]="r.status !== 'VALIDATED'">
          {{ statusLabels[r.status] }}
        </span>
        <span class="spacer"></span>
        @if (r.hasFile) {
          <button class="secondary" (click)="openFile()">Voir le document</button>
        }
        @if (r.status === 'PENDING_REVIEW') {
          <button class="danger" (click)="remove()">Supprimer</button>
        }
      </div>

      @if (info) {
        <p class="alert info">{{ info }}</p>
      }
      @if (error) {
        <p class="error">{{ error }}</p>
      }
      @if (r.reviewNote) {
        <p class="alert warn"><strong>Correction demandée par le médecin :</strong> {{ r.reviewNote }}</p>
      }

      <div class="card">
        <div class="info-grid">
          <div><span class="muted">Source</span><br />{{ modeLabels[r.entryMode] }}</div>
          <div><span class="muted">Ajouté par</span><br />{{ r.uploadedBy }}</div>
          <div><span class="muted">Le</span><br />{{ r.uploadedAt | date: 'dd/MM/yyyy HH:mm' }}</div>
          <div>
            <span class="muted">Validé par</span><br />
            @if (r.validatedBy) {
              {{ r.validatedBy }} le {{ r.validatedAt | date: 'dd/MM/yyyy' }}
            } @else {
              —
            }
          </div>
        </div>
        @if (r.entryMode === 'AI_EXTRACTED' && r.status === 'PENDING_REVIEW') {
          <p class="muted">
            Ces valeurs ont été extraites automatiquement du document : comparez-les avec le document avant de les
            soumettre au médecin.
          </p>
        }
      </div>

      <div class="card">
        <div class="toolbar">
          <h2>Valeurs</h2>
          <span class="spacer"></span>
          @if (r.status === 'PENDING_REVIEW' && !editing()) {
            <button class="secondary" (click)="startEdit(r)">Corriger les valeurs</button>
          }
        </div>

        @if (editing()) {
          @for (row of rows; track $index; let i = $index) {
            <div class="value-row">
              <select [name]="'param' + i" [(ngModel)]="row.parameterId">
                <option [ngValue]="null">Paramètre…</option>
                @for (p of catalogue(); track p.id) {
                  <option [ngValue]="p.id">{{ p.label }} ({{ p.unit }})</option>
                }
              </select>
              <input [name]="'value' + i" type="number" step="any" min="0" placeholder="Valeur" [(ngModel)]="row.value" />
              <button type="button" class="danger" (click)="removeRow(i)" [disabled]="rows.length === 1">✕</button>
            </div>
          }
          <p><button type="button" class="secondary" (click)="addRow()">+ Ajouter un paramètre</button></p>
          <button (click)="saveEdit()" [disabled]="busy || !rowsValid()">Enregistrer les corrections</button>
          <button class="secondary" (click)="editing.set(false)">Annuler</button>
        } @else {
          @if (r.values.length > 0) {
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
                        [class.critical]="v.flag === 'CRITICAL_LOW' || v.flag === 'CRITICAL_HIGH' || v.flag === 'INVALID'"
                      >
                        {{ flagLabels[v.flag] }}
                      </span>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          } @else {
            <p class="muted">
              Aucune valeur enregistrée. Utilisez « Corriger les valeurs » pour les saisir à partir du document.
            </p>
          }
        }
      </div>

      @if (isDoctor && r.status === 'PENDING_REVIEW' && !editing()) {
        <div class="card">
          <h2>Validation</h2>
          <p class="muted">
            Une fois validé, le résultat entre dans l'historique, les graphiques et l'analyse par l'IA. Il ne pourra
            plus être modifié.
          </p>
          @if (hasInvalid(r)) {
            <p class="alert warn">
              Ce résultat contient une valeur invalide (physiologiquement impossible, probable erreur de saisie ou
              d'extraction). Renvoyez-le pour correction : il ne peut pas être validé en l'état.
            </p>
          }
          <button (click)="validate()" [disabled]="busy || r.values.length === 0 || hasInvalid(r)">
            Valider ce résultat
          </button>

          <label for="note">Ou renvoyer pour correction</label>
          <textarea id="note" name="note" rows="2" [(ngModel)]="note" placeholder="Que faut-il corriger ?"></textarea>
          <p>
            <button class="secondary" (click)="requestCorrection()" [disabled]="busy || !note.trim()">
              Demander une correction
            </button>
          </p>
        </div>
      }

      @if (isDoctor && r.status === 'VALIDATED') {
        <div class="card">
          <div class="toolbar">
            <h2>Aide à l'interprétation (IA)</h2>
            <span class="spacer"></span>
            <button (click)="askAi()" [disabled]="aiBusy">
              {{ aiButtonLabel() }}
            </button>
          </div>
          <p class="alert info">
            Les suggestions sont une aide à l'interprétation. La décision médicale finale vous appartient.
            @if (r.consultationId) {
              Le diagnostic retenu sera enregistré dans la consultation liée.
            } @else {
              Aucune consultation n'est liée à ce résultat : le diagnostic retenu reste enregistré avec la suggestion.
            }
          </p>
          @if (aiError) {
            <p class="error">{{ aiError }}</p>
          }

          @for (s of suggestions(); track s.id) {
            <div class="suggestion">
              <div class="consultation-head">
                <strong>{{ s.createdAt | date: 'dd/MM/yyyy HH:mm' }}</strong>
                <span
                  class="badge"
                  [class.warn]="s.status === 'PENDING'"
                  [class.ok]="s.status === 'APPROVED' || s.status === 'MODIFIED'"
                  [class.danger]="s.status === 'REJECTED'"
                  >{{ suggestionLabels[s.status] }}</span
                >
                <span class="spacer"></span>
                <span class="muted">Données disponibles : {{ s.confidence * 100 | number: '1.0-0' }} %</span>
              </div>
              <p class="pre">{{ s.suggestedDiagnosis }}</p>

              @if (s.status === 'PENDING') {
                @if (modifyingId() === s.id) {
                  <label for="modify">Diagnostic retenu</label>
                  <textarea id="modify" name="modify" rows="3" [(ngModel)]="modifyText"></textarea>
                  <p>
                    <button (click)="decide(s, 'MODIFY')" [disabled]="aiBusy || !modifyText.trim()">
                      Enregistrer ma version
                    </button>
                    <button class="secondary" (click)="modifyingId.set(null)">Annuler</button>
                  </p>
                } @else {
                  <p>
                    <button (click)="decide(s, 'APPROVE')" [disabled]="aiBusy">Valider</button>
                    <button class="secondary" (click)="startModify(s)">Modifier</button>
                    <button class="danger" (click)="decide(s, 'REJECT')" [disabled]="aiBusy">Rejeter</button>
                  </p>
                }
              } @else {
                <p class="muted">
                  Décision de {{ s.decidedBy }} le {{ s.decidedAt | date: 'dd/MM/yyyy HH:mm' }}.
                  @if (s.finalDiagnosis) {
                    <br /><strong>Diagnostic retenu :</strong> <span class="pre">{{ s.finalDiagnosis }}</span>
                  }
                </p>
              }
            </div>
          } @empty {
            <p class="muted">Aucune analyse demandée pour ce résultat.</p>
          }
        </div>
      }
    } @else if (error) {
      <p class="error">{{ error }}</p>
    }
  `,
})
export class ResultDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private auth = inject(AuthService);
  private resultService = inject(ResultService);
  private aiService = inject(AiService);

  readonly id = Number(this.route.snapshot.paramMap.get('id'));
  readonly flagLabels = FLAG_LABELS;
  readonly statusLabels = STATUS_LABELS;
  readonly modeLabels = MODE_LABELS;
  readonly suggestionLabels = SUGGESTION_LABELS;
  readonly range = rangeText;
  readonly critical = criticalText;

  result = signal<LabResult | null>(null);
  catalogue = signal<AnalysisParameter[]>([]);
  suggestions = signal<AiSuggestion[]>([]);
  editing = signal(false);
  modifyingId = signal<number | null>(null);

  rows: Row[] = [];
  note = '';
  modifyText = '';
  busy = false;
  aiBusy = false;
  error = '';
  aiError = '';
  info = '';

  aiButtonLabel(): string {
    if (this.aiBusy) {
      return 'Analyse…';
    }
    return this.suggestions().length > 0 ? "Relancer l'analyse" : "Analyser avec l'IA";
  }

  hasInvalid(result: LabResult): boolean {
    return result.values.some((v) => v.flag === 'INVALID');
  }

  get isDoctor(): boolean {
    return this.auth.session?.role === 'DOCTOR';
  }

  ngOnInit(): void {
    // Message sent by the upload page (what the extraction found)
    const state = history.state as { message?: string } | null;
    this.info = state?.message ?? '';

    this.resultService.parameters().subscribe({ next: (list) => this.catalogue.set(list) });
    this.load();
  }

  load(): void {
    this.resultService.get(this.id).subscribe({
      next: (result) => {
        this.result.set(result);
        if (this.isDoctor && result.status === 'VALIDATED') {
          this.loadSuggestions();
        }
      },
      error: (err) => (this.error = errorText(err, 'Résultat introuvable.')),
    });
  }

  // ---------- correction (assistant or doctor, while pending) ----------

  startEdit(result: LabResult): void {
    this.rows =
      result.values.length > 0
        ? result.values.map((v) => ({ parameterId: v.parameter.id, value: v.value }))
        : [{ parameterId: null, value: null }];
    this.error = '';
    this.editing.set(true);
  }

  addRow(): void {
    this.rows.push({ parameterId: null, value: null });
  }

  removeRow(index: number): void {
    this.rows.splice(index, 1);
  }

  rowsValid(): boolean {
    const ids = this.rows.map((r) => r.parameterId);
    return (
      this.rows.every((r) => r.parameterId !== null && r.value !== null && r.value >= 0) &&
      new Set(ids).size === ids.length
    );
  }

  saveEdit(): void {
    this.busy = true;
    this.error = '';
    this.info = '';
    const values = this.rows.map((r) => ({ parameterId: r.parameterId as number, value: r.value as number }));
    this.resultService.replaceValues(this.id, values).subscribe({
      next: (result) => {
        this.busy = false;
        this.editing.set(false);
        this.result.set(result);
      },
      error: (err) => {
        this.busy = false;
        this.error = errorText(err, 'Enregistrement impossible.');
      },
    });
  }

  // ---------- doctor: validation ----------

  validate(): void {
    if (!confirm('Valider ce résultat ? Il ne pourra plus être modifié.')) {
      return;
    }
    this.busy = true;
    this.error = '';
    this.info = '';
    this.resultService.validate(this.id).subscribe({
      next: (result) => {
        this.busy = false;
        this.result.set(result);
        this.loadSuggestions();
      },
      error: (err) => {
        this.busy = false;
        this.error = errorText(err, 'Validation impossible.');
      },
    });
  }

  requestCorrection(): void {
    this.busy = true;
    this.error = '';
    this.resultService.requestCorrection(this.id, this.note.trim()).subscribe({
      next: (result) => {
        this.busy = false;
        this.note = '';
        this.result.set(result);
      },
      error: (err) => {
        this.busy = false;
        this.error = errorText(err, 'Envoi impossible.');
      },
    });
  }

  remove(): void {
    const patientId = this.result()?.patientId;
    if (!confirm('Supprimer ce résultat ?')) {
      return;
    }
    this.resultService.delete(this.id).subscribe({
      next: () => this.router.navigate(['/patients', patientId, 'results']),
      error: (err) => (this.error = errorText(err, 'Suppression impossible.')),
    });
  }

  openFile(): void {
    this.resultService.file(this.id).subscribe({
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

  // ---------- doctor: AI suggestions ----------

  loadSuggestions(): void {
    this.aiService.forResult(this.id).subscribe({
      next: (list) => this.suggestions.set(list),
      error: () => (this.aiError = "Le service d'IA n'est pas joignable."),
    });
  }

  askAi(): void {
    const result = this.result();
    if (!result) {
      return;
    }
    this.aiBusy = true;
    this.aiError = '';
    this.aiService.analyse(result.id, result.consultationId).subscribe({
      next: () => {
        this.aiBusy = false;
        this.loadSuggestions();
      },
      error: (err) => {
        this.aiBusy = false;
        this.aiError = err.status === 502 ? "Le service d'IA est indisponible." : errorText(err, 'Analyse impossible.');
      },
    });
  }

  startModify(suggestion: AiSuggestion): void {
    this.modifyText = suggestion.suggestedDiagnosis;
    this.modifyingId.set(suggestion.id);
  }

  decide(suggestion: AiSuggestion, decision: 'APPROVE' | 'MODIFY' | 'REJECT'): void {
    if (decision === 'REJECT' && !confirm('Rejeter cette suggestion ?')) {
      return;
    }
    this.aiBusy = true;
    this.aiError = '';
    const text = decision === 'MODIFY' ? this.modifyText.trim() : null;
    this.aiService.decide(suggestion.id, decision, text).subscribe({
      next: () => {
        this.aiBusy = false;
        this.modifyingId.set(null);
        this.loadSuggestions();
      },
      error: (err) => {
        this.aiBusy = false;
        this.aiError = err.status === 502 ? 'Consultation ou service indisponible.' : errorText(err, 'Décision non enregistrée.');
      },
    });
  }
}
