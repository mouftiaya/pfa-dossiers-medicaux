import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../config';
import { GlucoseSummary, Measurement, MeasurementRequest, Period, Stats, Target, TargetRequest } from './glucose.model';

@Injectable({ providedIn: 'root' })
export class GlucoseService {
  private http = inject(HttpClient);
  private url = `${API_URL}/api/glucose`;

  // ---------- patient (own data only) ----------

  measurements(from?: string, to?: string): Observable<Measurement[]> {
    let params = new HttpParams();
    if (from) {
      params = params.set('from', from);
    }
    if (to) {
      params = params.set('to', to);
    }
    return this.http.get<Measurement[]>(`${this.url}/me/measurements`, { params });
  }

  add(request: MeasurementRequest): Observable<Measurement> {
    return this.http.post<Measurement>(`${this.url}/me/measurements`, request);
  }

  update(id: number, request: MeasurementRequest): Observable<Measurement> {
    return this.http.put<Measurement>(`${this.url}/me/measurements/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/me/measurements/${id}`);
  }

  stats(period: Period, date: string): Observable<Stats> {
    return this.http.get<Stats>(`${this.url}/me/stats`, { params: new HttpParams().set('period', period).set('date', date) });
  }

  myTarget(): Observable<Target> {
    return this.http.get<Target>(`${this.url}/me/target`);
  }

  // ---------- clinical side ----------

  target(patientId: number): Observable<Target> {
    return this.http.get<Target>(`${this.url}/patients/${patientId}/target`);
  }

  saveTarget(patientId: number, request: TargetRequest): Observable<Target> {
    return this.http.put<Target>(`${this.url}/patients/${patientId}/target`, request);
  }

  summary(patientId: number): Observable<GlucoseSummary> {
    return this.http.get<GlucoseSummary>(`${this.url}/patients/${patientId}/summary`);
  }
}
