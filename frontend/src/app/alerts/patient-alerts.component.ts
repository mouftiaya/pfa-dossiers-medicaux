import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ALERT_TYPE_LABELS, Alert, SEVERITY_LABELS, STATUS_LABELS } from './alert.model';
import { AlertService } from './alert.service';

/** Compact list of the alerts of one patient, shown on the patient record. */
@Component({
  selector: 'app-patient-alerts',
  standalone: true,
  imports: [DatePipe, RouterLink],
  template: `
    <div class="card">
      <div class="toolbar">
        <h2>Alertes</h2>
        <span class="spacer"></span>
        <a [routerLink]="['/alerts']" [queryParams]="{ patientId: patientId() }">Toutes les alertes</a>
      </div>

      @if (error) {
        <p class="error">{{ error }}</p>
      }

      @for (a of shown(); track a.id) {
        <div class="consultation-head">
          <span class="muted">{{ a.createdAt | date: 'dd/MM/yyyy HH:mm' }}</span>
          <strong>{{ typeLabels[a.type] }}</strong>
          <span class="badge" [class.warn]="a.severity === 'WARNING'" [class.critical]="a.severity === 'CRITICAL'">
            {{ severityLabels[a.severity] }}
          </span>
          <span class="badge" [class.danger]="a.status === 'NEW'" [class.warn]="a.status === 'IN_PROGRESS'" [class.ok]="a.status === 'RESOLVED'">
            {{ statusLabels[a.status] }}
          </span>
          <span class="spacer"></span>
          <a [routerLink]="['/alerts', a.id]">Ouvrir</a>
        </div>
      } @empty {
        @if (!error) {
          <p class="muted">Aucune alerte pour ce patient.</p>
        }
      }

      @if (hiddenCount() > 0) {
        <p class="muted">… et {{ hiddenCount() }} autre(s) alerte(s).</p>
      }
    </div>
  `,
})
export class PatientAlertsComponent implements OnInit {
  private alertService = inject(AlertService);

  patientId = input.required<number>();

  readonly typeLabels = ALERT_TYPE_LABELS;
  readonly severityLabels = SEVERITY_LABELS;
  readonly statusLabels = STATUS_LABELS;

  private readonly maxShown = 5;
  alerts = signal<Alert[]>([]);
  error = '';

  shown(): Alert[] {
    return this.alerts().slice(0, this.maxShown);
  }

  hiddenCount(): number {
    return Math.max(0, this.alerts().length - this.maxShown);
  }

  ngOnInit(): void {
    this.alertService.list({ patientId: this.patientId() }).subscribe({
      next: (list) => this.alerts.set(list),
      error: () => (this.error = 'Impossible de charger les alertes.'),
    });
  }
}
