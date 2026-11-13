import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { AuthService } from '../auth/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="login-wrapper">
      <form class="card login-card" (ngSubmit)="submit()" #loginForm="ngForm">
        <h1>Cabinet de diabétologie</h1>
        <p class="muted">Connectez-vous pour accéder aux dossiers patients.</p>

        <label for="username">Nom d'utilisateur</label>
        <input id="username" name="username" [(ngModel)]="username" required autocomplete="username" />

        <label for="password">Mot de passe</label>
        <input
          id="password"
          name="password"
          type="password"
          [(ngModel)]="password"
          required
          autocomplete="current-password"
        />

        @if (activated) {
          <p class="alert info">Votre compte est activé. Vous pouvez maintenant vous connecter.</p>
        }
        @if (error) {
          <p class="error">{{ error }}</p>
        }

        <button type="submit" [disabled]="loading || loginForm.invalid">
          {{ loading ? 'Connexion…' : 'Se connecter' }}
        </button>
      </form>
    </div>
  `,
})
export class LoginComponent {
  private auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  readonly activated = this.route.snapshot.queryParamMap.get('activated') === '1';

  username = '';
  password = '';
  error = '';
  loading = false;

  submit(): void {
    this.loading = true;
    this.error = '';
    this.auth.login(this.username, this.password).subscribe({
      next: () => this.router.navigate(['/']), // the home guard picks the page of the role
      error: (err) => {
        this.loading = false;
        this.error =
          err.status === 401
            ? 'Identifiants incorrects.'
            : 'Impossible de joindre le serveur. Vérifiez que le backend est démarré.';
      },
    });
  }
}
