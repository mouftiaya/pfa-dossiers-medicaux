import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Role } from '../auth/auth.service';
import { API_URL } from '../config';

export interface AppUser {
  id: number;
  username: string;
  role: Role;
  active: boolean;
}

export interface CreateUserForm {
  username: string;
  password: string;
  role: Role;
}

/** Only the fields sent are changed. */
export interface UpdateUserForm {
  role?: Role;
  active?: boolean;
  password?: string;
}

export interface AuditLog {
  id: number;
  username: string;
  action: string;
  details: string | null;
  createdAt: string;
}

export interface Page<T> {
  content: T[];
  number: number; // current page, from 0
  totalPages: number;
  totalElements: number;
}

export const ROLE_OPTIONS: { value: Role; label: string }[] = [
  { value: 'DOCTOR', label: 'Médecin' },
  { value: 'ASSISTANT', label: 'Assistant(e)' },
  { value: 'ADMIN', label: 'Administrateur' },
];

@Injectable({ providedIn: 'root' })
export class AdminService {
  private http = inject(HttpClient);

  users(): Observable<AppUser[]> {
    return this.http.get<AppUser[]>(`${API_URL}/api/users`);
  }

  createUser(form: CreateUserForm): Observable<AppUser> {
    return this.http.post<AppUser>(`${API_URL}/api/users`, form);
  }

  updateUser(id: number, form: UpdateUserForm): Observable<AppUser> {
    return this.http.put<AppUser>(`${API_URL}/api/users/${id}`, form);
  }

  auditLogs(page: number, size = 50): Observable<Page<AuditLog>> {
    return this.http.get<Page<AuditLog>>(`${API_URL}/api/audit-logs`, {
      params: new HttpParams().set('page', page).set('size', size),
    });
  }
}
