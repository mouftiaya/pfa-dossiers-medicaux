import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../config';
import { Patient, PatientForm } from './patient.model';

@Injectable({ providedIn: 'root' })
export class PatientService {
  private http = inject(HttpClient);
  private url = `${API_URL}/api/patients`;

  list(search?: string): Observable<Patient[]> {
    let params = new HttpParams();
    if (search && search.trim()) {
      params = params.set('search', search.trim());
    }
    return this.http.get<Patient[]>(this.url, { params });
  }

  get(id: number): Observable<Patient> {
    return this.http.get<Patient>(`${this.url}/${id}`);
  }

  create(form: PatientForm): Observable<Patient> {
    return this.http.post<Patient>(this.url, form);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
