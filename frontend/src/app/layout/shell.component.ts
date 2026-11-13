import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from '../auth/auth.service';

const ROLE_LABELS = {
  DOCTOR: 'Médecin',
  ASSISTANT: 'Assistant(e)',
  ADMIN: 'Administrateur',
  PATIENT: 'Patient',
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
        } @else if (isPatient) {
          <a routerLink="/portal" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">Mon dossier</a>
          <a routerLink="/portal/glucose" routerLinkActive="active">Ma glycémie</a>
        } @else {
          <a routerLink="/patients" routerLinkActive="active">Patients</a>
          <a routerLink="/alerts" routerLinkActive="active">Alertes</a>
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

  get isPatient(): boolean {
    return this.auth.session?.role === 'PATIENT';
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
