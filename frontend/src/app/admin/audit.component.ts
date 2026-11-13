import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';

import { AdminService, AuditLog, Page } from './admin.service';

const ACTION_LABELS: Record<string, string> = {
  LOGIN_SUCCESS: 'Connexion',
  LOGIN_FAILED: 'Échec de connexion',
  LOGIN_REFUSED: 'Connexion refusée (compte désactivé)',
  USER_CREATED: 'Compte créé',
  USER_ROLE_CHANGED: 'Rôle modifié',
  USER_ENABLED: 'Compte réactivé',
  USER_DISABLED: 'Compte désactivé',
  USER_PASSWORD_RESET: 'Mot de passe modifié',
};

@Component({
  selector: 'app-audit',
  standalone: true,
  imports: [DatePipe],
  template: `
    <div class="toolbar">
      <h2>Journal d'activité</h2>
      <span class="spacer"></span>
      <button class="secondary" (click)="load(page().number)">Actualiser</button>
    </div>

    @if (error) {
      <p class="error">{{ error }}</p>
    }

    <div class="card">
      <table>
        <thead>
          <tr>
            <th>Date</th>
            <th>Utilisateur</th>
            <th>Action</th>
            <th>Détails</th>
          </tr>
        </thead>
        <tbody>
          @for (log of page().content; track log.id) {
            <tr>
              <td>{{ log.createdAt | date: 'dd/MM/yyyy HH:mm:ss' }}</td>
              <td>{{ log.username }}</td>
              <td>
                <span class="badge" [class.danger]="isAlert(log)">{{ label(log) }}</span>
              </td>
              <td class="muted">{{ log.details || '—' }}</td>
            </tr>
          } @empty {
            <tr>
              <td colspan="4" class="muted">Aucune activité enregistrée.</td>
            </tr>
          }
        </tbody>
      </table>

      @if (page().totalPages > 1) {
        <p class="pager">
          <button class="secondary" (click)="load(page().number - 1)" [disabled]="page().number === 0">←</button>
          <span class="muted">Page {{ page().number + 1 }} sur {{ page().totalPages }}</span>
          <button class="secondary" (click)="load(page().number + 1)" [disabled]="page().number + 1 >= page().totalPages">
            →
          </button>
        </p>
      }
    </div>
  `,
})
export class AuditComponent implements OnInit {
  private admin = inject(AdminService);

  page = signal<Page<AuditLog>>({ content: [], number: 0, totalPages: 0, totalElements: 0 });
  error = '';

  ngOnInit(): void {
    this.load(0);
  }

  load(pageNumber: number): void {
    this.error = '';
    this.admin.auditLogs(pageNumber).subscribe({
      next: (page) => this.page.set(page),
      error: () => (this.error = "Impossible de charger le journal d'activité."),
    });
  }

  label(log: AuditLog): string {
    return ACTION_LABELS[log.action] ?? log.action;
  }

  /** Failed or refused logins are highlighted. */
  isAlert(log: AuditLog): boolean {
    return log.action === 'LOGIN_FAILED' || log.action === 'LOGIN_REFUSED';
  }
}
