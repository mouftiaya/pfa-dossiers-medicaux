import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { Patient } from '../patients/patient.model';
import { PatientService } from '../patients/patient.service';
import {
  ALERT_TYPE_LABELS,
  Alert,
  AlertSeverity,
  AlertStatus,
  AlertSummary,
  SEVERITY_LABELS,
  STATUS_LABELS,
} from './alert.model';
import { AlertService } from './alert.service';

/** Alerts page for the doctor and the assistant (the doctor never receives the detail text). */
@Component({
  selector: 'app-alerts',
  standalone: true,
  imports: [FormsModule, DatePipe, RouterLink],
  template: `
    <div class="toolbar">
      <h2>Alertes</h2>
      <button type="button" (click)="runAi()" [disabled]="aiBusy">
        {{ aiBusy ? 'Analyse en cours…' : "Lancer l'analyse IA" }}
      </button>
    </div>
    <p class="muted">
      L'IA surveille l'historique des mesures de chaque patient sur plusieurs semaines : une alerte n'est créée que si la
      glycémie reste hors de la cible de façon persistante, jamais après une seule mesure.
    </p>
    @if (aiMessage) {
      <p class="alert info">{{ aiMessage }}</p>
    }

    @if (error) {
      <p class="error">{{ error }}</p>
    }

    @if (summary(); as s) {
      <div class="stats">
        <div class="stat"><div class="label">Total</div><div class="num">{{ s.total }}</div></div>
        <div class="stat"><div class="label">Ouvertes</div><div class="num">{{ s.open }}</div></div>
        <div class="stat"><div class="label">Nouvelles</div><div class="num">{{ s.newCount }}</div></div>
        <div class="stat"><div class="label">En cours</div><div class="num">{{ s.inProgress }}</div></div>
        <div class="stat"><div class="label">Résolues</div><div class="num">{{ s.resolved }}</div></div>
        <div class="stat"><div class="label">Critiques</div><div class="num">{{ s.bySeverity['CRITICAL'] || 0 }}</div></div>
      </div>
    }

    <div class="card">
      <div class="row-inline">
        <select name="status" [ngModel]="status" (ngModelChange)="status = $event; load()">
          <option value="">Tous les statuts</option>
          @for (k of statusKeys; track k) {
            <option [value]="k">{{ statusLabels[k] }}</option>
          }
        </select>
        <select name="severity" [ngModel]="severity" (ngModelChange)="severity = $event; load()">
          <option value="">Toutes les gravités</option>
          @for (k of severityKeys; track k) {
            <option [value]="k">{{ severityLabels[k] }}</option>
          }
        </select>
        <select name="patient" [ngModel]="patientId" (ngModelChange)="patientId = $event; load()">
          <option [ngValue]="null">Tous les patients</option>
          @for (p of patients(); track p.id) {
            <option [ngValue]="p.id">{{ p.firstName }} {{ p.lastName }}</option>
          }
        </select>
      </div>
    </div>

    <div class="card">
      <table>
        <thead>
          <tr>
            <th>Date</th>
            <th>Patient</th>
            <th>Type</th>
            <th>Gravité</th>
            <th>Statut</th>
            @if (showDetail()) {
              <th>Détail</th>
            }
            <th>Actions</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          @for (a of alerts(); track a.id) {
            <tr>
              <td>{{ a.createdAt | date: 'dd/MM/yyyy HH:mm' }}</td>
              <td><a [routerLink]="['/patients', a.patientId]">{{ patientName(a.patientId) }}</a></td>
              <td>
                {{ typeLabels[a.type] }}
                @if (a.aiScore !== null && a.aiScore !== undefined) {
                  <span class="badge warn" title="Alerte créée par l'analyse IA de l'historique">IA {{ a.aiScore }}/100</span>
                }
              </td>
              <td>
                <span class="badge" [class.warn]="a.severity === 'WARNING'" [class.critical]="a.severity === 'CRITICAL'">
                  {{ severityLabels[a.severity] }}
                </span>
              </td>
              <td>
                <span
                  class="badge"
                  [class.danger]="a.status === 'NEW'"
                  [class.warn]="a.status === 'IN_PROGRESS'"
                  [class.ok]="a.status === 'RESOLVED'"
                >
                  {{ statusLabels[a.status] }}
                </span>
              </td>
              @if (showDetail()) {
                <td>{{ a.detail }}</td>
              }
              <td>{{ a.actionsCount }}</td>
              <td class="actions"><a [routerLink]="['/alerts', a.id]">Ouvrir</a></td>
            </tr>
          } @empty {
            <tr>
              <td [attr.colspan]="showDetail() ? 8 : 7" class="muted">Aucune alerte.</td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export class AlertsComponent implements OnInit {
  private alertService = inject(AlertService);
  private patientService = inject(PatientService);
  private route = inject(ActivatedRoute);

  readonly typeLabels = ALERT_TYPE_LABELS;
  readonly severityLabels = SEVERITY_LABELS;
  readonly statusLabels = STATUS_LABELS;
  readonly statusKeys = Object.keys(STATUS_LABELS) as AlertStatus[];
  readonly severityKeys = Object.keys(SEVERITY_LABELS) as AlertSeverity[];

  alerts = signal<Alert[]>([]);
  summary = signal<AlertSummary | null>(null);
  patients = signal<Patient[]>([]);

  status: AlertStatus | '' = '';
  severity: AlertSeverity | '' = '';
  patientId: number | null = null;
  error = '';
  aiBusy = false;
  aiMessage = '';

  /** The detail column is hidden when the server sends no detail (doctor). */
  showDetail(): boolean {
    return this.alerts().some((a) => a.detail !== null && a.detail !== undefined);
  }

  patientName(id: number): string {
    const p = this.patients().find((x) => x.id === id);
    return p ? `${p.firstName} ${p.lastName}` : `Patient n° ${id}`;
  }

  ngOnInit(): void {
    const fromQuery = Number(this.route.snapshot.queryParamMap.get('patientId'));
    this.patientId = Number.isInteger(fromQuery) && fromQuery > 0 ? fromQuery : null;
    this.patientService.list().subscribe({
      next: (list) => this.patients.set(list),
      error: () => undefined, // names fall back to the patient number
    });
    this.load();
  }

  /** Runs the AI analysis now, then reloads the list. */
  runAi(): void {
    this.aiBusy = true;
    this.aiMessage = '';
    this.alertService.runAiAnalysis().subscribe({
      next: (r) => {
        this.aiBusy = false;
        this.aiMessage =
          r.alertsCreated > 0
            ? `Analyse terminée : ${r.alertsCreated} nouvelle(s) alerte(s) créée(s) sur ${r.analysed} patient(s) analysé(s).`
            : `Analyse terminée : aucun nouveau patient à risque (${r.analysed} patient(s) analysé(s)).`;
        this.load();
      },
      error: () => {
        this.aiBusy = false;
        this.aiMessage = "L'analyse n'a pas pu être lancée (service IA indisponible ?).";
      },
    });
  }

  load(): void {
    this.error = '';
    this.alertService.list({ status: this.status, severity: this.severity, patientId: this.patientId }).subscribe({
      next: (list) => this.alerts.set(list),
      error: () => (this.error = 'Impossible de charger les alertes.'),
    });
    this.alertService.summary(this.patientId).subscribe({
      next: (s) => this.summary.set(s),
      error: () => undefined,
    });
  }
}
