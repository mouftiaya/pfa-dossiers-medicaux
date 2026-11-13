import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { AuthService } from '../auth/auth.service';
import { Consultation, ConsultationForm } from '../consultations/consultation.model';
import { ConsultationService } from '../consultations/consultation.service';
import { PatientAlertsComponent } from '../alerts/patient-alerts.component';
import { GlucoseFollowupComponent } from '../glucose/glucose-followup.component';
import { PatientAccountComponent } from './patient-account.component';
import { DIABETES_LABELS, Patient } from './patient.model';
import { PatientService } from './patient.service';

function emptyForm(): ConsultationForm {
  return {
    consultationDate: new Date().toISOString().slice(0, 10),
    reason: '',
    notes: '',
    prescribedAnalyses: '',
    diagnosis: '',
  };
}

@Component({
  selector: 'app-patient-detail',
  standalone: true,
  imports: [FormsModule, DatePipe, RouterLink, PatientAccountComponent, GlucoseFollowupComponent, PatientAlertsComponent],
  template: `
      <a routerLink="/patients">← Retour à la liste</a>

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
            <div>
              <span class="muted">Diabète</span><br />{{ p.diabetesType ? labels[p.diabetesType] : '—' }}
            </div>
            <div class="full"><span class="muted">Allergies</span><br />{{ p.allergies || '—' }}</div>
            <div class="full">
              <span class="muted">Antécédents médicaux</span><br />{{ p.medicalHistory || '—' }}
            </div>
          </div>
          <p class="links">
            <a [routerLink]="['/patients', p.id, 'results']">Résultats d'analyses</a>
            <a [routerLink]="['/patients', p.id, 'evolution']">Évolution (HbA1c, glycémie…)</a>
          </p>
        </div>

        @if (isAssistant) {
          <app-patient-account [patient]="p" />
        }
        <app-glucose-followup [patientId]="p.id" />
        <app-patient-alerts [patientId]="p.id" />
      }

      <div class="toolbar">
        <h2>Historique des consultations</h2>
        <span class="spacer"></span>
        @if (isDoctor) {
          <button (click)="toggleForm()">{{ showForm() ? 'Annuler' : 'Nouvelle consultation' }}</button>
        }
      </div>

      @if (showForm()) {
        <form class="card form-grid" (ngSubmit)="save()" #consultationForm="ngForm">
          <div>
            <label for="consultationDate">Date</label>
            <input
              id="consultationDate"
              name="consultationDate"
              type="date"
              [(ngModel)]="form.consultationDate"
              required
            />
          </div>
          <div>
            <label for="reason">Motif</label>
            <input id="reason" name="reason" [(ngModel)]="form.reason" required />
          </div>
          <div class="full">
            <label for="notes">Notes de consultation</label>
            <textarea id="notes" name="notes" rows="3" [(ngModel)]="form.notes"></textarea>
          </div>
          <div class="full">
            <label for="prescribedAnalyses">Analyses prescrites</label>
            <textarea
              id="prescribedAnalyses"
              name="prescribedAnalyses"
              rows="2"
              [(ngModel)]="form.prescribedAnalyses"
            ></textarea>
          </div>
          <div class="full">
            <label for="diagnosis">Diagnostic</label>
            <textarea id="diagnosis" name="diagnosis" rows="2" [(ngModel)]="form.diagnosis"></textarea>
          </div>
          @if (formError) {
            <p class="error full">{{ formError }}</p>
          }
          <div class="full">
            <button type="submit" [disabled]="consultationForm.invalid">Enregistrer</button>
          </div>
        </form>
      }

      @for (c of consultations(); track c.id) {
        <div class="card">
          <div class="consultation-head">
            <strong>{{ c.consultationDate | date: 'dd/MM/yyyy' }}</strong>
            <span>{{ c.reason }}</span>
            <span class="spacer"></span>
            <span class="muted">par {{ c.createdBy }}</span>
            @if (isDoctor) {
              <button class="danger" (click)="remove(c)">Supprimer</button>
            }
          </div>
          @if (c.notes) {
            <p><span class="muted">Notes :</span> {{ c.notes }}</p>
          }
          @if (c.prescribedAnalyses) {
            <p><span class="muted">Analyses prescrites :</span> {{ c.prescribedAnalyses }}</p>
          }
          @if (c.diagnosis) {
            <p><span class="muted">Diagnostic :</span> {{ c.diagnosis }}</p>
          }
        </div>
      } @empty {
        <p class="muted">Aucune consultation enregistrée pour ce patient.</p>
      }
  `,
})
export class PatientDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private auth = inject(AuthService);
  private patientService = inject(PatientService);
  private consultationService = inject(ConsultationService);

  readonly labels = DIABETES_LABELS;
  readonly patientId = Number(this.route.snapshot.paramMap.get('id'));

  patient = signal<Patient | null>(null);
  consultations = signal<Consultation[]>([]);
  showForm = signal(false);
  form: ConsultationForm = emptyForm();
  error = '';
  formError = '';

  get isDoctor(): boolean {
    return this.auth.session?.role === 'DOCTOR';
  }

  get isAssistant(): boolean {
    return this.auth.session?.role === 'ASSISTANT';
  }

  ngOnInit(): void {
    this.patientService.get(this.patientId).subscribe({
      next: (patient) => this.patient.set(patient),
      error: () => (this.error = 'Patient introuvable.'),
    });
    this.loadConsultations();
  }

  loadConsultations(): void {
    this.consultationService.listForPatient(this.patientId).subscribe({
      next: (list) => this.consultations.set(list),
      error: () => (this.error = "Impossible de charger l'historique des consultations."),
    });
  }

  toggleForm(): void {
    this.showForm.update((open) => !open);
    this.form = emptyForm();
    this.formError = '';
  }

  save(): void {
    this.consultationService.create(this.patientId, this.form).subscribe({
      next: () => {
        this.showForm.set(false);
        this.form = emptyForm();
        this.formError = '';
        this.loadConsultations();
      },
      error: () => (this.formError = "Erreur lors de l'enregistrement. Vérifiez les champs."),
    });
  }

  remove(consultation: Consultation): void {
    if (!confirm('Supprimer cette consultation ?')) {
      return;
    }
    this.consultationService.delete(consultation.id).subscribe({
      next: () => this.loadConsultations(),
      error: () => (this.error = 'Suppression impossible.'),
    });
  }
}
