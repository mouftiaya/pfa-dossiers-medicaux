import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { API_URL } from '../config';

export type Role = 'DOCTOR' | 'ASSISTANT' | 'ADMIN';

export interface Session {
  token: string;
  username: string;
  role: Role;
}

const STORAGE_KEY = 'clinic_session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);

  login(username: string, password: string): Observable<Session> {
    return this.http
      .post<Session>(`${API_URL}/api/auth/login`, { username, password })
      .pipe(tap((session) => localStorage.setItem(STORAGE_KEY, JSON.stringify(session))));
  }

  logout(): void {
    localStorage.removeItem(STORAGE_KEY);
  }

  get session(): Session | null {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as Session) : null;
  }

  get token(): string | null {
    return this.session?.token ?? null;
  }

  isLoggedIn(): boolean {
    return this.token !== null;
  }
}
