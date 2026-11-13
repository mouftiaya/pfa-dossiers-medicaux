import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ChartLine, ChartPoint, LineChartComponent } from '../shared/line-chart.component';
import {
  CONTEXT_LABELS,
  GLUCOSE_FLAG_LABELS,
  MeasureContext,
  Measurement,
  PERIOD_LABELS,
  Period,
  Stats,
  flagClass,
  flagFor,
  fmt,
  localDate,
  localDateTimeInput,
  serverMessage,
} from '../glucose/glucose.model';
import { GlucoseService } from '../glucose/glucose.service';

interface MeasurementForm {
  value: number | null;
  measuredAt: string; // yyyy-MM-ddTHH:mm
  context: MeasureContext;
  note: string;
}

function emptyForm(): MeasurementForm {
  return { value: null, measuredAt: localDateTimeInput(), context: 'FASTING', note: '' };
}

/** Patient space: self-monitoring of blood glucose (mg/dL). */
@Component({
  selector: 'app-portal-glucose',
  standalone: true,
  imports: [FormsModule, DatePipe, RouterLink, LineChartComponent],
  template: `
    <a routerLink="/portal">← Mon dossier</a>

    <div class="toolbar">
      <h2>Ma glycémie</h2>
      <span class="spacer"></span>
      <button (click)="toggleForm()">{{ showForm() ? 'Annuler' : 'Ajouter une mesure' }}</button>
    </div>

    @if (error) {
      <p class="error">{{ error }}</p>
    }
    @if (info) {
      <p class="alert info">{{ info }}</p>
    }

    @if (stats(); as s) {
      @if (!s.target.defined) {
        <p class="alert warn">
          Votre médecin n'a pas encore défini vos objectifs : les valeurs cibles affichées ci-dessous sont des valeurs
          par défaut, données à titre indicatif.
        </p>
      }
    }

    @if (showForm()) {
      <form class="card form-grid" (ngSubmit)="save()" #measureForm="ngForm">
        <div>
          <label for="value">Glycémie (mg/dL)</label>
          <input id="value" name="value" type="number" step="1" min="20" max="800" [(ngModel)]="form.value" required />
        </div>
        <div>
          <label for="measuredAt">Date et heure</label>
          <input id="measuredAt" name="measuredAt" type="datetime-local" [(ngModel)]="form.measuredAt" required />
        </div>
        <div>
          <label for="context">Moment de la mesure</label>
          <select id="context" name="context" [(ngModel)]="form.context">
            @for (c of contexts; track c.value) {
              <option [value]="c.value">{{ c.label }}</option>
            }
          </select>
        </div>
        <div>
          <label for="note">Remarque (facultatif)</label>
          <input id="note" name="note" maxlength="500" [(ngModel)]="form.note" />
        </div>
        @if (formError) {
          <p class="error full">{{ formError }}</p>
        }
        <div class="full">
          <button type="submit" [disabled]="busy || measureForm.invalid">
            {{ editingId() === null ? 'Enregistrer la mesure' : 'Enregistrer les modifications' }}
          </button>
        </div>
      </form>
    }

    <div class="card">
      <div class="row-inline">
        @for (p of periods; track p.value) {
          <button [class.secondary]="period() !== p.value" (click)="setPeriod(p.value)">{{ p.label }}</button>
        }
        <input type="date" name="date" [ngModel]="date()" (ngModelChange)="setDate($event)" [max]="today" />
        <button class="secondary" (click)="setDate(today)">Aujourd'hui</button>
      </div>
    </div>

    @if (stats(); as s) {
      <p class="muted">
        Période du {{ s.from | date: 'dd/MM/yyyy' }} au {{ s.to | date: 'dd/MM/yyyy' }}. Objectif :
        {{ fmt(s.target.targetMin) }} – {{ fmt(s.target.targetMax) }} mg/dL.
      </p>
      <div class="stats">
        <div class="stat"><div class="label">Mesures</div><div class="num">{{ s.count }}</div></div>
        <div class="stat"><div class="label">Moyenne (mg/dL)</div><div class="num">{{ fmt(s.average) }}</div></div>
        <div class="stat"><div class="label">Minimum</div><div class="num">{{ fmt(s.minimum) }}</div></div>
        <div class="stat"><div class="label">Maximum</div><div class="num">{{ fmt(s.maximum) }}</div></div>
        <div class="stat"><div class="label">Dans la cible</div><div class="num">{{ s.percentInTarget === null ? '—' : fmt(s.percentInTarget) + ' %' }}</div></div>
        <div class="stat"><div class="label">Mesures hautes</div><div class="num">{{ s.highCount }}</div></div>
        <div class="stat"><div class="label">Mesures basses</div><div class="num">{{ s.lowCount }}</div></div>
      </div>

      <div class="card">
        <app-line-chart
          [points]="chartPoints()"
          [refMin]="s.target.targetMin"
          [refMax]="s.target.targetMax"
          [lines]="chartLines()"
          unit="mg/dL"
          emptyText="Aucune mesure sur cette période."
        />
        <p class="legend">
          <span><i class="dot-key NORMAL"></i> Dans la cible</span>
          <span><i class="dot-key HIGH"></i> Élevé</span>
          <span><i class="dot-key LOW"></i> Bas</span>
          <span><i class="dot-key CRITICAL_HIGH"></i> Critique</span>
          <span><i class="band-key"></i> Cible</span>
        </p>
      </div>
    }

    <div class="card">
      <table>
        <thead>
          <tr>
            <th>Date</th>
            <th>Valeur</th>
            <th>Moment</th>
            <th>Interprétation</th>
            <th>Remarque</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          @for (m of measurements(); track m.id) {
            <tr>
              <td>{{ m.measuredAt | date: 'dd/MM/yyyy HH:mm' }}</td>
              <td>
                <strong [class]="'flag ' + m.flag">{{ fmt(m.value) }}</strong> <span class="muted">mg/dL</span>
              </td>
              <td>{{ contextLabels[m.context] }}</td>
              <td>
                <span [class]="'badge ' + badgeClass(m.flag)">{{ flagLabels[m.flag] }}</span>
              </td>
              <td class="muted">{{ m.note || '' }}</td>
              <td class="actions">
                <button class="secondary" (click)="edit(m)">Modifier</button>
                <button class="danger" (click)="remove(m)">Supprimer</button>
              </td>
            </tr>
          } @empty {
            <tr>
              <td colspan="6" class="muted">Aucune mesure sur cette période.</td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export class PortalGlucoseComponent implements OnInit {
  private glucose = inject(GlucoseService);

  readonly contextLabels = CONTEXT_LABELS;
  readonly flagLabels = GLUCOSE_FLAG_LABELS;
  readonly badgeClass = flagClass;
  readonly fmt = fmt;
  readonly today = localDate();
  readonly contexts = (Object.keys(CONTEXT_LABELS) as MeasureContext[]).map((value) => ({
    value,
    label: CONTEXT_LABELS[value],
  }));
  readonly periods = (Object.keys(PERIOD_LABELS) as Period[]).map((value) => ({
    value,
    label: PERIOD_LABELS[value],
  }));

  period = signal<Period>('DAY');
  date = signal(localDate());
  stats = signal<Stats | null>(null);
  measurements = signal<Measurement[]>([]);
  showForm = signal(false);
  editingId = signal<number | null>(null);
  form: MeasurementForm = emptyForm();
  error = '';
  info = '';
  formError = '';
  busy = false;

  readonly chartPoints = computed<ChartPoint[]>(() => {
    const s = this.stats();
    if (!s) {
      return [];
    }
    const daily = s.period !== 'DAY';
    return s.points.map((p) => ({
      date: daily ? p.at.slice(0, 10) : p.at,
      value: p.average,
      flag: flagFor(p.average, s.target),
    }));
  });

  readonly chartLines = computed<ChartLine[]>(() => {
    const t = this.stats()?.target;
    if (!t) {
      return [];
    }
    return [
      { value: t.highThreshold, label: `Seuil haut ${t.highThreshold}`, kind: 'warn' },
      { value: t.lowThreshold, label: `Seuil bas ${t.lowThreshold}`, kind: 'warn' },
      { value: t.criticalHigh, label: `Critique ${t.criticalHigh}`, kind: 'crit' },
      { value: t.criticalLow, label: `Critique ${t.criticalLow}`, kind: 'crit' },
    ];
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.glucose.stats(this.period(), this.date()).subscribe({
      next: (s) => {
        this.stats.set(s);
        this.glucose.measurements(s.from, s.to).subscribe({
          next: (list) => this.measurements.set(list),
          error: (err) => (this.error = serverMessage(err, 'Impossible de charger vos mesures.')),
        });
      },
      error: (err) => (this.error = serverMessage(err, 'Impossible de charger vos statistiques.')),
    });
  }

  setPeriod(period: Period): void {
    this.period.set(period);
    this.load();
  }

  setDate(date: string): void {
    if (!date) {
      return;
    }
    this.date.set(date);
    this.load();
  }

  toggleForm(): void {
    if (this.showForm()) {
      this.closeForm();
    } else {
      this.form = emptyForm();
      this.editingId.set(null);
      this.formError = '';
      this.showForm.set(true);
    }
  }

  private closeForm(): void {
    this.showForm.set(false);
    this.editingId.set(null);
    this.form = emptyForm();
    this.formError = '';
  }

  edit(m: Measurement): void {
    this.form = {
      value: m.value,
      measuredAt: m.measuredAt.slice(0, 16),
      context: m.context,
      note: m.note ?? '',
    };
    this.editingId.set(m.id);
    this.formError = '';
    this.info = '';
    this.showForm.set(true);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  save(): void {
    const value = this.form.value;
    if (value === null || !Number.isFinite(value) || value < 20 || value > 800) {
      this.formError = 'La glycémie doit être comprise entre 20 et 800 mg/dL.';
      return;
    }
    if (!this.form.measuredAt) {
      this.formError = 'Indiquez la date et l\'heure de la mesure.';
      return;
    }
    const when = new Date(this.form.measuredAt);
    if (when.getTime() > Date.now() + 5 * 60_000) {
      this.formError = 'La date de la mesure ne peut pas être dans le futur.';
      return;
    }
    const request = {
      value,
      measuredAt: this.form.measuredAt.length === 16 ? `${this.form.measuredAt}:00` : this.form.measuredAt,
      context: this.form.context,
      note: this.form.note.trim() ? this.form.note.trim() : null,
    };
    const id = this.editingId();
    this.busy = true;
    this.formError = '';
    const call = id === null ? this.glucose.add(request) : this.glucose.update(id, request);
    call.subscribe({
      next: () => {
        this.busy = false;
        this.info = id === null ? 'Mesure enregistrée.' : 'Mesure modifiée.';
        // show the period that contains the measurement
        this.date.set(request.measuredAt.slice(0, 10));
        this.closeForm();
        this.load();
      },
      error: (err) => {
        this.busy = false;
        this.formError = serverMessage(err, "Enregistrement impossible. Vérifiez les champs.");
      },
    });
  }

  remove(m: Measurement): void {
    if (!confirm('Supprimer cette mesure ?')) {
      return;
    }
    this.glucose.delete(m.id).subscribe({
      next: () => {
        this.info = 'Mesure supprimée.';
        this.load();
      },
      error: (err) => (this.error = serverMessage(err, 'Suppression impossible.')),
    });
  }
}
