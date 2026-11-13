import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../config';

export interface PatientAccount {
  patientId: number;
  username: string;
  activated: boolean;
  activationUrl: string | null;
  activationExpiresAt: string | null;
}

@Injectable({ providedIn: 'root' })
export class PatientAccountService {
  private http = inject(HttpClient);
  private url = `${API_URL}/api/patient-accounts`;

  /** 404 means that the patient has no account yet. */
  byPatient(patientId: number): Observable<PatientAccount> {
    return this.http.get<PatientAccount>(`${this.url}/by-patient/${patientId}`);
  }

  create(patientId: number, username: string): Observable<PatientAccount> {
    return this.http.post<PatientAccount>(this.url, { patientId, username });
  }

  resetActivation(patientId: number): Observable<PatientAccount> {
    return this.http.post<PatientAccount>(`${this.url}/by-patient/${patientId}/reset-activation`, {});
  }
}
