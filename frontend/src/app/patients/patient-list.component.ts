import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AuthService } from '../auth/auth.service';
import { DIABETES_LABELS, DiabetesType, Patient, PatientForm } from './patient.model';
import { PatientService } from './patient.service';

function emptyForm(): PatientForm {
  return {
    firstName: '',
    lastName: '',
    dateOfBirth: '',
    gender: 'F',
    phone: '',
    diabetesType: 'TYPE_2',
    allergies: '',
    medicalHistory: '',
  };
}

@Component({
  selector: 'app-patient-list',
  standalone: true,
  imports: [FormsModule, DatePipe, RouterLink],
  template: `
      <div class="toolbar">
        <h2>Patients</h2>
        <input
          class="search"
          placeholder="Rechercher par nom ou prénom…"
          [ngModel]="search"
          (ngModelChange)="onSearch($event)"
          name="search"
        />
        <button (click)="toggleForm()">{{ showForm() ? 'Annuler' : 'Nouveau patient' }}</button>
      </div>

      @if (showForm()) {
        <form class="card form-grid" (ngSubmit)="save()" #patientForm="ngForm">
          <div>
            <label for="firstName">Prénom</label>
            <input id="firstName" name="firstName" [(ngModel)]="form.firstName" required />
          </div>
          <div>
            <label for="lastName">Nom</label>
            <input id="lastName" name="lastName" [(ngModel)]="form.lastName" required />
          </div>
          <div>
            <label for="dateOfBirth">Date de naissance</label>
            <input id="dateOfBirth" name="dateOfBirth" type="date" [(ngModel)]="form.dateOfBirth" required />
          </div>
          <div>
            <label for="gender">Sexe</label>
            <select id="gender" name="gender" [(ngModel)]="form.gender">
              <option value="F">Femme</option>
              <option value="M">Homme</option>
            </select>
          </div>
          <div>
            <label for="phone">Téléphone</label>
            <input id="phone" name="phone" [(ngModel)]="form.phone" />
          </div>
          <div>
            <label for="diabetesType">Type de diabète</label>
            <select id="diabetesType" name="diabetesType" [(ngModel)]="form.diabetesType">
              @for (type of diabetesTypes; track type) {
                <option [value]="type">{{ labels[type] }}</option>
              }
            </select>
          </div>
          <div class="full">
            <label for="allergies">Allergies</label>
            <input id="allergies" name="allergies" [(ngModel)]="form.allergies" />
          </div>
          <div class="full">
            <label for="medicalHistory">Antécédents médicaux</label>
            <textarea id="medicalHistory" name="medicalHistory" rows="3" [(ngModel)]="form.medicalHistory"></textarea>
          </div>
          @if (formError) {
            <p class="error full">{{ formError }}</p>
          }
          <div class="full">
            <button type="submit" [disabled]="patientForm.invalid">Enregistrer</button>
          </div>
        </form>
      }

      @if (error) {
        <p class="error">{{ error }}</p>
      }

      <div class="card">
        <table>
          <thead>
            <tr>
              <th>Nom</th>
              <th>Date de naissance</th>
              <th>Téléphone</th>
              <th>Diabète</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            @for (patient of patients(); track patient.id) {
              <tr>
                <td>
                  <a [routerLink]="['/patients', patient.id]">{{ patient.lastName }} {{ patient.firstName }}</a>
                </td>
                <td>{{ patient.dateOfBirth | date: 'dd/MM/yyyy' }}</td>
                <td>{{ patient.phone }}</td>
                <td>{{ patient.diabetesType ? labels[patient.diabetesType] : '—' }}</td>
                <td class="actions">
                  @if (isDoctor) {
                    <button class="danger" (click)="remove(patient)">Supprimer</button>
                  }
                </td>
              </tr>
            } @empty {
              <tr>
                <td colspan="5" class="muted">Aucun patient trouvé.</td>
              </tr>
            }
          </tbody>
        </table>
      </div>
  `,
})
export class PatientListComponent implements OnInit {
  private auth = inject(AuthService);
  private patientService = inject(PatientService);

  readonly labels = DIABETES_LABELS;
  readonly diabetesTypes = Object.keys(DIABETES_LABELS) as DiabetesType[];

  patients = signal<Patient[]>([]);
  showForm = signal(false);
  search = '';
  form: PatientForm = emptyForm();
  error = '';
  formError = '';

  get isDoctor(): boolean {
    return this.auth.session?.role === 'DOCTOR';
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.patientService.list(this.search).subscribe({
      next: (patients) => {
        this.error = '';
        this.patients.set(patients);
      },
      error: (err) => {
        this.error =
          err.status === 403
            ? "Vous n'avez pas le droit de consulter les patients."
            : 'Impossible de charger les patients.';
      },
    });
  }

  onSearch(value: string): void {
    this.search = value;
    this.load();
  }

  toggleForm(): void {
    this.showForm.update((open) => !open);
    this.form = emptyForm();
    this.formError = '';
  }

  save(): void {
    this.patientService.create(this.form).subscribe({
      next: () => {
        this.showForm.set(false);
        this.form = emptyForm();
        this.formError = '';
        this.load();
      },
      error: () => (this.formError = "Erreur lors de l'enregistrement. Vérifiez les champs."),
    });
  }

  remove(patient: Patient): void {
    if (!confirm(`Supprimer le dossier de ${patient.firstName} ${patient.lastName} ?`)) {
      return;
    }
    this.patientService.delete(patient.id).subscribe({
      next: () => this.load(),
      error: () => (this.error = 'Suppression impossible.'),
    });
  }
}
