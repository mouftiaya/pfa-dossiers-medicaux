import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AuthService, Role } from '../auth/auth.service';
import { AdminService, AppUser, CreateUserForm, ROLE_OPTIONS } from './admin.service';

function emptyForm(): CreateUserForm {
  return { username: '', password: '', role: 'ASSISTANT' };
}

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="toolbar">
      <h2>Utilisateurs</h2>
      <span class="spacer"></span>
      <button (click)="toggleForm()">{{ showForm() ? 'Annuler' : 'Nouveau compte' }}</button>
    </div>

    @if (error) {
      <p class="error">{{ error }}</p>
    }
    @if (info) {
      <p class="alert info">{{ info }}</p>
    }

    @if (showForm()) {
      <form class="card form-grid" (ngSubmit)="create()" #userForm="ngForm">
        <div>
          <label for="username">Nom d'utilisateur</label>
          <input id="username" name="username" [(ngModel)]="form.username" required minlength="3" maxlength="50" />
        </div>
        <div>
          <label for="password">Mot de passe (8 caractères minimum)</label>
          <input
            id="password"
            name="password"
            type="password"
            [(ngModel)]="form.password"
            required
            minlength="8"
            autocomplete="new-password"
          />
        </div>
        <div>
          <label for="role">Rôle</label>
          <select id="role" name="role" [(ngModel)]="form.role">
            @for (r of roles; track r.value) {
              <option [value]="r.value">{{ r.label }}</option>
            }
          </select>
        </div>
        <div class="full">
          <button type="submit" [disabled]="userForm.invalid || busy">Créer le compte</button>
        </div>
      </form>
    }

    <div class="card">
      <table>
        <thead>
          <tr>
            <th>Utilisateur</th>
            <th>Rôle</th>
            <th>État</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          @for (u of users(); track u.id) {
            <tr>
              <td>
                {{ u.username }}
                @if (isSelf(u)) {
                  <span class="muted">(vous)</span>
                }
              </td>
              <td>
                <select
                  class="inline"
                  [name]="'role' + u.id"
                  [ngModel]="u.role"
                  (ngModelChange)="changeRole(u, $event)"
                  [disabled]="isSelf(u)"
                >
                  @for (r of roles; track r.value) {
                    <option [value]="r.value">{{ r.label }}</option>
                  }
                </select>
              </td>
              <td>
                <span class="badge" [class.ok]="u.active" [class.danger]="!u.active">
                  {{ u.active ? 'Actif' : 'Désactivé' }}
                </span>
              </td>
              <td class="actions">
                <button class="secondary" (click)="resetPassword(u)">Nouveau mot de passe</button>
                <button
                  [class]="u.active ? 'danger' : 'secondary'"
                  (click)="setActive(u, !u.active)"
                  [disabled]="isSelf(u)"
                >
                  {{ u.active ? 'Désactiver' : 'Réactiver' }}
                </button>
              </td>
            </tr>
          }
        </tbody>
      </table>
      <p class="muted">
        Une personne désactivée ne peut plus se connecter. Une session déjà ouverte reste valable jusqu'à son
        expiration.
      </p>
    </div>
  `,
})
export class UsersComponent implements OnInit {
  private admin = inject(AdminService);
  private auth = inject(AuthService);

  readonly roles = ROLE_OPTIONS;

  users = signal<AppUser[]>([]);
  showForm = signal(false);
  form: CreateUserForm = emptyForm();
  busy = false;
  error = '';
  info = '';

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.admin.users().subscribe({
      next: (list) => this.users.set(list),
      error: () => (this.error = 'Impossible de charger les utilisateurs.'),
    });
  }

  isSelf(user: AppUser): boolean {
    return user.username === this.auth.session?.username;
  }

  toggleForm(): void {
    this.showForm.update((open) => !open);
    this.form = emptyForm();
    this.error = '';
  }

  create(): void {
    this.busy = true;
    this.error = '';
    this.info = '';
    this.admin.createUser({ ...this.form, username: this.form.username.trim() }).subscribe({
      next: (user) => {
        this.busy = false;
        this.showForm.set(false);
        this.info = `Compte « ${user.username} » créé.`;
        this.load();
      },
      error: (err) => {
        this.busy = false;
        this.error = err.status === 409 ? "Ce nom d'utilisateur existe déjà." : 'Création impossible. Vérifiez les champs.';
      },
    });
  }

  changeRole(user: AppUser, role: Role): void {
    this.update(user, { role }, `Rôle de « ${user.username} » modifié.`);
  }

  setActive(user: AppUser, active: boolean): void {
    if (!active && !confirm(`Désactiver le compte « ${user.username} » ?`)) {
      return;
    }
    this.update(user, { active }, `Compte « ${user.username} » ${active ? 'réactivé' : 'désactivé'}.`);
  }

  resetPassword(user: AppUser): void {
    const password = prompt(`Nouveau mot de passe pour « ${user.username} » (8 caractères minimum) :`);
    if (password === null) {
      return;
    }
    if (password.length < 8) {
      this.error = 'Le mot de passe doit contenir au moins 8 caractères.';
      return;
    }
    this.update(user, { password }, `Mot de passe de « ${user.username} » modifié.`);
  }

  private update(user: AppUser, change: { role?: Role; active?: boolean; password?: string }, message: string): void {
    this.error = '';
    this.info = '';
    this.admin.updateUser(user.id, change).subscribe({
      next: () => {
        this.info = message;
        this.load();
      },
      error: () => {
        this.error = 'Modification impossible.';
        this.load(); // puts the list back to the real state
      },
    });
  }
}
