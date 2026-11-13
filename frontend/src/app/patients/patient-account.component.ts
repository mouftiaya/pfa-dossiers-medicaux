import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { Patient } from './patient.model';
import { PatientAccount, PatientAccountService } from './patient-account.service';

/** Default login: firstname.lastname, lower case, without accents or spaces. */
export function defaultUsername(firstName: string, lastName: string): string {
  const clean = (s: string) =>
    s
      .normalize('NFD')
      .replace(/[̀-ͯ]/g, '')
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '');
  return `${clean(firstName)}.${clean(lastName)}`;
}

/** "Compte patient" panel of the patient record (assistant only). */
@Component({
  selector: 'app-patient-account',
  standalone: true,
  imports: [FormsModule, DatePipe],
  template: `
    <div class="card">
      <h2>Compte patient</h2>

      @if (error) {
        <p class="error">{{ error }}</p>
      }

      @if (loading()) {
        <p class="muted">Chargement…</p>
      } @else {
        @if (account(); as acc) {
        <p>
          <span class="muted">Identifiant :</span> <strong>{{ acc.username }}</strong>
          @if (acc.activated) {
            <span class="badge ok">Compte activé</span>
          } @else {
            <span class="badge warn">En attente d'activation</span>
          }
        </p>

        @if (!acc.activated) {
          @if (acc.activationUrl) {
            <p class="muted">
              Transmettez ce lien au patient pour qu'il choisisse son mot de passe
              @if (acc.activationExpiresAt) {
                (valable jusqu'au {{ acc.activationExpiresAt | date: 'dd/MM/yyyy HH:mm' }})
              }
              :
            </p>
            <div class="copy-box">{{ acc.activationUrl }}</div>
            <div class="row-inline">
              <button class="secondary" type="button" (click)="copy(acc.activationUrl)">Copier</button>
              @if (copied()) {
                <span class="muted">Lien copié.</span>
              }
            </div>
          }
          <p>
            <button class="secondary" type="button" [disabled]="busy" (click)="regenerate()">
              Générer un nouveau lien
            </button>
          </p>
        }
        } @else {
        <p class="muted">Ce patient n'a pas encore de compte pour accéder à son espace.</p>
        <form (ngSubmit)="create()" #accountForm="ngForm">
          <label for="accountUsername">Identifiant</label>
          <input id="accountUsername" name="accountUsername" [(ngModel)]="username" required minlength="3" maxlength="50" />
          <p>
            <button type="submit" [disabled]="busy || accountForm.invalid">Créer le compte</button>
          </p>
        </form>
        }
      }
    </div>
  `,
})
export class PatientAccountComponent implements OnInit {
  private service = inject(PatientAccountService);

  patient = input.required<Patient>();

  account = signal<PatientAccount | null>(null);
  loading = signal(true);
  copied = signal(false);
  username = '';
  error = '';
  busy = false;

  ngOnInit(): void {
    const p = this.patient();
    this.username = defaultUsername(p.firstName, p.lastName);
    this.service.byPatient(p.id).subscribe({
      next: (acc) => {
        this.account.set(acc);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        if (err.status !== 404) {
          this.error = 'Impossible de charger le compte patient.';
        }
      },
    });
  }

  create(): void {
    this.busy = true;
    this.error = '';
    this.service.create(this.patient().id, this.username.trim()).subscribe({
      next: (acc) => {
        this.busy = false;
        this.account.set(acc);
      },
      error: (err) => {
        this.busy = false;
        this.error =
          err.status === 409
            ? "Cet identifiant existe déjà ou ce patient a déjà un compte."
            : 'Création du compte impossible. Vérifiez l\'identifiant.';
      },
    });
  }

  regenerate(): void {
    if (!confirm("Générer un nouveau lien ? L'ancien lien ne fonctionnera plus.")) {
      return;
    }
    this.busy = true;
    this.error = '';
    this.copied.set(false);
    this.service.resetActivation(this.patient().id).subscribe({
      next: (acc) => {
        this.busy = false;
        this.account.set(acc);
      },
      error: (err) => {
        this.busy = false;
        this.error =
          err.status === 409 ? 'Ce compte est déjà activé.' : 'Impossible de générer un nouveau lien.';
      },
    });
  }

  copy(text: string): void {
    const done = () => {
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 3000);
    };
    if (navigator.clipboard) {
      navigator.clipboard.writeText(text).then(done, () => (this.error = 'Copie impossible : sélectionnez le lien à la main.'));
    } else {
      this.error = 'Copie impossible : sélectionnez le lien à la main.';
    }
  }
}
