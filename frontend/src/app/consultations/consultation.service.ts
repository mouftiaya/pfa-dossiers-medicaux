import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../config';
import { Consultation, ConsultationForm } from './consultation.model';

@Injectable({ providedIn: 'root' })
export class ConsultationService {
  private http = inject(HttpClient);
  private url = `${API_URL}/api/consultations`;

  listForPatient(patientId: number): Observable<Consultation[]> {
    return this.http.get<Consultation[]>(this.url, {
      params: new HttpParams().set('patientId', patientId),
    });
  }

  create(patientId: number, form: ConsultationForm): Observable<Consultation> {
    return this.http.post<Consultation>(this.url, { patientId, ...form });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
