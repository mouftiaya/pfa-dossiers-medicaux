import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { AuthService } from '../auth/auth.service';

/** Public page opened from the activation link given by the assistant: /activate?token=... */
@Component({
  selector: 'app-activate',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="login-wrapper">
      <form class="card login-card" (ngSubmit)="submit()" #activateForm="ngForm">
        <h1>Activer mon compte</h1>
        <p class="muted">Choisissez le mot de passe qui vous permettra d'accéder à votre espace patient.</p>

        @if (!token) {
          <p class="error">Lien d'activation invalide : le code est manquant.</p>
        } @else {
          <label for="password">Nouveau mot de passe (8 caractères minimum)</label>
          <input
            id="password"
            name="password"
            type="password"
            [(ngModel)]="password"
            required
            minlength="8"
            autocomplete="new-password"
          />

          <label for="confirm">Confirmer le mot de passe</label>
          <input
            id="confirm"
            name="confirm"
            type="password"
            [(ngModel)]="confirm"
            required
            minlength="8"
            autocomplete="new-password"
          />

          @if (error) {
            <p class="error">{{ error }}</p>
          }

          <button type="submit" [disabled]="loading || activateForm.invalid">
            {{ loading ? 'Activation…' : 'Activer mon compte' }}
          </button>
        }
      </form>
    </div>
  `,
})
export class ActivateComponent {
  private auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  readonly token = this.route.snapshot.queryParamMap.get('token') ?? '';

  password = '';
  confirm = '';
  error = '';
  loading = false;

  submit(): void {
    this.error = '';
    if (this.password.length < 8) {
      this.error = 'Le mot de passe doit contenir au moins 8 caractères.';
      return;
    }
    if (this.password !== this.confirm) {
      this.error = 'Les deux mots de passe ne sont pas identiques.';
      return;
    }
    this.loading = true;
    this.auth.activate(this.token, this.password).subscribe({
      next: () => this.router.navigate(['/login'], { queryParams: { activated: '1' } }),
      error: (err) => {
        this.loading = false;
        if (err.status === 400 || err.status === 410) {
          this.error =
            "Ce lien d'activation est invalide ou a expiré. Demandez un nouveau lien au cabinet.";
        } else if (err.status === 0) {
          this.error = 'Impossible de joindre le serveur. Réessayez plus tard.';
        } else {
          this.error = "L'activation a échoué. Réessayez ou contactez le cabinet.";
        }
      },
    });
  }
}
