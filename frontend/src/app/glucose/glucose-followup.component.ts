import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AuthService } from '../auth/auth.service';
import { GlucoseSummary, Target, TargetRequest, fmt, serverMessage, targetProblem } from './glucose.model';
import { GlucoseService } from './glucose.service';

function toRequest(t: Target): TargetRequest {
  return {
    targetMin: t.targetMin,
    targetMax: t.targetMax,
    lowThreshold: t.lowThreshold,
    highThreshold: t.highThreshold,
    criticalLow: t.criticalLow,
    criticalHigh: t.criticalHigh,
    absenceDays: t.absenceDays,
  };
}

/**
 * Glycemic follow-up on the patient record.
 * DOCTOR: aggregated summary (never individual values) and the editor of the targets.
 * ASSISTANT: targets in read-only mode.
 */
@Component({
  selector: 'app-glucose-followup',
  standalone: true,
  imports: [FormsModule, DatePipe],
  template: `
    @if (isDoctor || isAssistant) {
      <div class="card">
        <div class="toolbar">
          <h2>Suivi glycémique</h2>
          <span class="spacer"></span>
          @if (isDoctor && target() && !editing()) {
            <button class="secondary" (click)="startEdit()">Modifier les objectifs</button>
          }
        </div>

        @if (error) {
          <p class="error">{{ error }}</p>
        }
        @if (info) {
          <p class="alert info">{{ info }}</p>
        }

        @if (isDoctor) {
          @if (summary(); as s) {
            <p class="muted">Synthèse des {{ s.periodDays }} derniers jours.</p>
            <div class="stats">
              <div class="stat"><div class="label">Mesures</div><div class="num">{{ s.count }}</div></div>
              <div class="stat"><div class="label">Moyenne (mg/dL)</div><div class="num">{{ fmt(s.average) }}</div></div>
              <div class="stat"><div class="label">Dans la cible</div><div class="num">{{ s.percentInTarget === null ? '—' : fmt(s.percentInTarget) + ' %' }}</div></div>
              <div class="stat"><div class="label">Mesures hautes</div><div class="num">{{ s.highCount }}</div></div>
              <div class="stat"><div class="label">Mesures basses</div><div class="num">{{ s.lowCount }}</div></div>
            </div>
            <p>
              <span class="muted">Dernière mesure :</span>
              @if (s.lastMeasurementAt) {
                {{ s.lastMeasurementAt | date: 'dd/MM/yyyy HH:mm' }}
              } @else {
                aucune
              }
            </p>
            <p class="muted">
              Les valeurs détaillées restent dans l'espace du patient : seule cette synthèse est visible ici.
            </p>
          }
        }

        @if (target(); as t) {
          <h3>Objectifs glycémiques</h3>
          @if (!t.defined) {
            <p class="alert warn">
              Aucun objectif n'a encore été défini pour ce patient : les valeurs par défaut ci-dessous sont utilisées.
            </p>
          }

          @if (editing()) {
            <form (ngSubmit)="save()" #targetForm="ngForm">
              <div class="target-grid">
                <div>
                  <label for="criticalLow">Critique bas (mg/dL)</label>
                  <input id="criticalLow" name="criticalLow" type="number" [(ngModel)]="form.criticalLow" required />
                </div>
                <div>
                  <label for="lowThreshold">Seuil bas</label>
                  <input id="lowThreshold" name="lowThreshold" type="number" [(ngModel)]="form.lowThreshold" required />
                </div>
                <div>
                  <label for="targetMin">Cible min</label>
                  <input id="targetMin" name="targetMin" type="number" [(ngModel)]="form.targetMin" required />
                </div>
                <div>
                  <label for="targetMax">Cible max</label>
                  <input id="targetMax" name="targetMax" type="number" [(ngModel)]="form.targetMax" required />
                </div>
                <div>
                  <label for="highThreshold">Seuil haut</label>
                  <input id="highThreshold" name="highThreshold" type="number" [(ngModel)]="form.highThreshold" required />
                </div>
                <div>
                  <label for="criticalHigh">Critique élevé</label>
                  <input id="criticalHigh" name="criticalHigh" type="number" [(ngModel)]="form.criticalHigh" required />
                </div>
                <div>
                  <label for="absenceDays">Alerte sans mesure (jours)</label>
                  <input id="absenceDays" name="absenceDays" type="number" min="1" max="30" [(ngModel)]="form.absenceDays" required />
                </div>
              </div>
              <p class="muted">
                Ordre exigé : critique bas &lt; seuil bas ≤ cible min &lt; cible max ≤ seuil haut &lt; critique élevé.
              </p>
              @if (formError) {
                <p class="error">{{ formError }}</p>
              }
              <div class="row-inline">
                <button type="submit" [disabled]="busy || targetForm.invalid">Enregistrer</button>
                <button type="button" class="secondary" (click)="cancelEdit()">Annuler</button>
              </div>
            </form>
          } @else {
            <div class="target-grid">
              <div><span class="muted">Critique bas</span><br />≤ {{ t.criticalLow }} mg/dL</div>
              <div><span class="muted">Seuil bas</span><br />&lt; {{ t.lowThreshold }} mg/dL</div>
              <div><span class="muted">Cible</span><br />{{ t.targetMin }} – {{ t.targetMax }} mg/dL</div>
              <div><span class="muted">Seuil haut</span><br />&gt; {{ t.highThreshold }} mg/dL</div>
              <div><span class="muted">Critique élevé</span><br />≥ {{ t.criticalHigh }} mg/dL</div>
              <div><span class="muted">Alerte sans mesure</span><br />{{ t.absenceDays }} jour(s)</div>
            </div>
            @if (t.defined && t.definedBy) {
              <p class="muted">
                Défini par {{ t.definedBy }}
                @if (t.updatedAt) {
                  le {{ t.updatedAt | date: 'dd/MM/yyyy' }}
                }
              </p>
            }
          }
        }
      </div>
    }
  `,
})
export class GlucoseFollowupComponent implements OnInit {
  private auth = inject(AuthService);
  private glucose = inject(GlucoseService);

  patientId = input.required<number>();

  readonly fmt = fmt;

  summary = signal<GlucoseSummary | null>(null);
  target = signal<Target | null>(null);
  editing = signal(false);
  form: TargetRequest = { targetMin: 70, targetMax: 180, lowThreshold: 70, highThreshold: 250, criticalLow: 54, criticalHigh: 300, absenceDays: 3 };
  error = '';
  info = '';
  formError = '';
  busy = false;

  get isDoctor(): boolean {
    return this.auth.session?.role === 'DOCTOR';
  }

  get isAssistant(): boolean {
    return this.auth.session?.role === 'ASSISTANT';
  }

  ngOnInit(): void {
    const id = this.patientId();
    if (this.isDoctor) {
      this.glucose.summary(id).subscribe({
        next: (s) => {
          this.summary.set(s);
          this.target.set(s.target);
        },
        error: () => this.loadTarget(id),
      });
    } else if (this.isAssistant) {
      this.loadTarget(id);
    }
  }

  private loadTarget(id: number): void {
    this.glucose.target(id).subscribe({
      next: (t) => this.target.set(t),
      error: (err) => (this.error = serverMessage(err, 'Impossible de charger le suivi glycémique.')),
    });
  }

  startEdit(): void {
    const t = this.target();
    if (!t) {
      return;
    }
    this.form = toRequest(t);
    this.formError = '';
    this.info = '';
    this.editing.set(true);
  }

  cancelEdit(): void {
    this.editing.set(false);
    this.formError = '';
  }

  save(): void {
    const problem = targetProblem(this.form);
    if (problem) {
      this.formError = problem;
      return;
    }
    this.busy = true;
    this.formError = '';
    this.glucose.saveTarget(this.patientId(), this.form).subscribe({
      next: (t) => {
        this.busy = false;
        this.target.set(t);
        this.editing.set(false);
        this.info = 'Objectifs enregistrés.';
      },
      error: (err) => {
        this.busy = false;
        this.formError = serverMessage(err, "Enregistrement impossible : vérifiez les valeurs.");
      },
    });
  }
}
