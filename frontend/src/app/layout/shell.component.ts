import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from '../auth/auth.service';

const ROLE_LABELS = {
  DOCTOR: 'Médecin',
  ASSISTANT: 'Assistant(e)',
  ADMIN: 'Administrateur',
};

/** Top bar with the navigation of the user's role, shared by every page after login. */
@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header class="topbar">
      <strong>Cabinet de diabétologie</strong>
      <nav class="nav">
        @if (isAdmin) {
          <a routerLink="/admin/users" routerLinkActive="active">Utilisateurs</a>
          <a routerLink="/admin/audit" routerLinkActive="active">Journal d'activité</a>
        } @else {
          <a routerLink="/patients" routerLinkActive="active">Patients</a>
        }
      </nav>
      <span class="spacer"></span>
      <span class="muted">{{ username }} ({{ roleLabel }})</span>
      <button class="secondary" (click)="logout()">Déconnexion</button>
    </header>

    <main class="container">
      <router-outlet />
    </main>
  `,
})
export class ShellComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  get username(): string {
    return this.auth.session?.username ?? '';
  }

  get isAdmin(): boolean {
    return this.auth.session?.role === 'ADMIN';
  }

  get roleLabel(): string {
    const role = this.auth.session?.role;
    return role ? ROLE_LABELS[role] : '';
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
