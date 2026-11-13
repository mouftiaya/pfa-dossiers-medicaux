import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { AuthService } from '../auth/auth.service';
import { Patient } from '../patients/patient.model';
import { PatientService } from '../patients/patient.service';
import {
  ACTION_LABELS,
  ALERT_TYPE_LABELS,
  ActionType,
  AlertDetail,
  CHANNEL_LABELS,
  DELIVERY_LABELS,
  EmailDraft,
  SEVERITY_LABELS,
  STATUS_LABELS,
} from './alert.model';
import { AlertService } from './alert.service';

/** One alert with its history of actions and the notifications sent to the patient. */
@Component({
  selector: 'app-alert-detail',
  standalone: true,
  imports: [FormsModule, DatePipe, RouterLink],
  template: `
    <a routerLink="/alerts">← Retour aux alertes</a>

    @if (error) {
      <p class="error">{{ error }}</p>
    }

    @if (alert(); as a) {
      <div class="card">
        <div class="consultation-head">
          <h2>{{ typeLabels[a.type] }}</h2>
          <span class="badge" [class.warn]="a.severity === 'WARNING'" [class.critical]="a.severity === 'CRITICAL'">
            {{ severityLabels[a.severity] }}
          </span>
          @if (a.aiScore !== null && a.aiScore !== undefined) {
            <span class="badge warn" title="Alerte créée par l'analyse IA de l'historique">IA {{ a.aiScore }}/100</span>
          }
          <span
            class="badge"
            [class.danger]="a.status === 'NEW'"
            [class.warn]="a.status === 'IN_PROGRESS'"
            [class.ok]="a.status === 'RESOLVED'"
          >
            {{ statusLabels[a.status] }}
          </span>
        </div>
        <p>
          <span class="muted">Patient :</span>
          <a [routerLink]="['/patients', a.patientId]">{{ patientName() }}</a>
        </p>
        <p><span class="muted">Créée le :</span> {{ a.createdAt | date: 'dd/MM/yyyy HH:mm' }}</p>
        <p><span class="muted">Dernière mise à jour :</span> {{ a.updatedAt | date: 'dd/MM/yyyy HH:mm' }}</p>
        @if (a.detail) {
          <p><span class="muted">Détail :</span> {{ a.detail }}</p>
        }
      </div>

      <div class="card">
        <h2>Historique des actions</h2>
        @for (x of a.actions; track x.id) {
          <p>
            <strong>{{ actionLabels[x.type] }}</strong>
            <span class="muted"> — {{ x.actor }}, {{ x.createdAt | date: 'dd/MM/yyyy HH:mm' }}</span>
            @if (x.note) {
              <br />{{ x.note }}
            }
          </p>
        } @empty {
          <p class="muted">Aucune action enregistrée.</p>
        }
      </div>

      <div class="card">
        <h2>Notifications envoyées au patient</h2>
        @for (n of a.notifications; track $index) {
          <p>
            <strong>{{ channelLabels[n.channel] }}</strong> : {{ deliveryLabels[n.status] }}
            <span class="muted"> ({{ n.createdAt | date: 'dd/MM/yyyy HH:mm' }})</span>
          </p>
        } @empty {
          <p class="muted">Aucune notification.</p>
        }
      </div>

      @if (isAssistant && a.aiScore !== null && a.aiScore !== undefined && a.status !== 'RESOLVED') {
        <div class="card">
          <h2>Contacter le patient</h2>
          <p class="muted">
            L'IA a détecté une tendance persistante sur plusieurs semaines. Vous pouvez appeler le patient ou lui envoyer
            cet e-mail (modifiable avant l'envoi).
          </p>
          @if (draft(); as d) {
            @if (d.phone) {
              <p><span class="muted">Téléphone :</span> {{ d.phone }}</p>
            }
            @if (d.to) {
              <p><span class="muted">E-mail :</span> {{ d.to }}</p>
              <label for="mailSubject">Objet</label>
              <input id="mailSubject" name="mailSubject" maxlength="150" [(ngModel)]="mailSubject" />
              <label for="mailBody">Message</label>
              <textarea id="mailBody" name="mailBody" rows="9" maxlength="1000" [(ngModel)]="mailBody"></textarea>
              @if (mailError) {
                <p class="error">{{ mailError }}</p>
              }
              <button type="button" (click)="sendMail()" [disabled]="busy || !mailSubject.trim() || !mailBody.trim()">
                Envoyer l'e-mail au patient
              </button>
            } @else {
              <p class="alert info">Ce patient n'a pas d'adresse e-mail enregistrée : contactez-le par téléphone.</p>
            }
          } @else {
            <p class="muted">Chargement du message suggéré…</p>
          }
        </div>
      }

      @if (isAssistant) {
        <form class="card" (ngSubmit)="submit()" #actionForm="ngForm">
          <h2>Ajouter une action</h2>
          @if (a.status === 'RESOLVED') {
            <p class="alert info">Cette alerte est résolue : plus aucune action ne peut être ajoutée.</p>
          }
          <label for="type">Action</label>
          <select id="type" name="type" [(ngModel)]="type" [disabled]="a.status === 'RESOLVED'">
            @for (t of actionKeys; track t) {
              <option [value]="t">{{ actionLabels[t] }}</option>
            }
          </select>

          <label for="note">Note {{ type === 'NOTE' ? '(obligatoire)' : '(facultatif)' }}</label>
          <textarea
            id="note"
            name="note"
            rows="3"
            maxlength="1000"
            [(ngModel)]="note"
            [disabled]="a.status === 'RESOLVED'"
          ></textarea>

          @if (formError) {
            <p class="error">{{ formError }}</p>
          }
          <button type="submit" [disabled]="busy || a.status === 'RESOLVED'">Enregistrer l'action</button>
        </form>
      }
    }
  `,
})
export class AlertDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private auth = inject(AuthService);
  private alertService = inject(AlertService);
  private patientService = inject(PatientService);

  readonly id = Number(this.route.snapshot.paramMap.get('id'));
  readonly typeLabels = ALERT_TYPE_LABELS;
  readonly severityLabels = SEVERITY_LABELS;
  readonly statusLabels = STATUS_LABELS;
  readonly actionLabels = ACTION_LABELS;
  readonly channelLabels = CHANNEL_LABELS;
  readonly deliveryLabels = DELIVERY_LABELS;
  readonly actionKeys = Object.keys(ACTION_LABELS) as ActionType[];

  alert = signal<AlertDetail | null>(null);
  draft = signal<EmailDraft | null>(null);
  mailSubject = '';
  mailBody = '';
  mailError = '';
  patient = signal<Patient | null>(null);
  type: ActionType = 'CONTACTED';
  note = '';
  error = '';
  formError = '';
  busy = false;

  get isAssistant(): boolean {
    return this.auth.session?.role === 'ASSISTANT';
  }

  patientName(): string {
    const p = this.patient();
    const a = this.alert();
    return p ? `${p.firstName} ${p.lastName}` : a ? `Patient n° ${a.patientId}` : '';
  }

  ngOnInit(): void {
    this.alertService.get(this.id).subscribe({
      next: (a) => {
        this.alert.set(a);
        if (this.isAssistant && a.aiScore !== null && a.aiScore !== undefined) {
          this.loadDraft();
        }
        this.patientService.get(a.patientId).subscribe({
          next: (p) => this.patient.set(p),
          error: () => undefined,
        });
      },
      error: () => (this.error = 'Alerte introuvable.'),
    });
  }

  private loadDraft(): void {
    this.alertService.emailDraft(this.id).subscribe({
      next: (d) => {
        this.draft.set(d);
        this.mailSubject = d.subject;
        this.mailBody = d.body;
      },
      error: () => (this.mailError = "Le message suggéré n'a pas pu être chargé."),
    });
  }

  sendMail(): void {
    this.busy = true;
    this.mailError = '';
    this.alertService.sendEmail(this.id, { subject: this.mailSubject.trim(), body: this.mailBody.trim() }).subscribe({
      next: (a) => {
        this.busy = false;
        this.alert.set(a);
      },
      error: (err) => {
        this.busy = false;
        this.mailError =
          err.status === 409
            ? "Envoi impossible : alerte résolue ou patient sans adresse e-mail."
            : "L'e-mail n'a pas pu être envoyé.";
      },
    });
  }

  submit(): void {
    const note = this.note.trim();
    if (this.type === 'NOTE' && !note) {
      this.formError = 'Une note est obligatoire pour ce type d\'action.';
      return;
    }
    this.busy = true;
    this.formError = '';
    this.alertService.addAction(this.id, { type: this.type, note: note ? note : null }).subscribe({
      next: (a) => {
        this.busy = false;
        this.alert.set(a);
        this.note = '';
        this.type = 'CONTACTED';
      },
      error: (err) => {
        this.busy = false;
        this.formError =
          err.status === 409
            ? 'Cette alerte est déjà résolue.'
            : err.status === 400
              ? 'Action invalide : une note est requise pour ce type.'
              : "Impossible d'enregistrer l'action.";
      },
    });
  }
}
