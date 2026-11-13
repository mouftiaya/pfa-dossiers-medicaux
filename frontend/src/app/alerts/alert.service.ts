import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../config';
import {
  ActionRequest,
  AiRunReport,
  Alert,
  AlertDetail,
  AlertSeverity,
  AlertStatus,
  AlertSummary,
  EmailDraft,
} from './alert.model';

@Injectable({ providedIn: 'root' })
export class AlertService {
  private http = inject(HttpClient);
  private url = `${API_URL}/api/alerts`;

  list(filter: { status?: AlertStatus | ''; severity?: AlertSeverity | ''; patientId?: number | null } = {}): Observable<Alert[]> {
    let params = new HttpParams();
    if (filter.status) {
      params = params.set('status', filter.status);
    }
    if (filter.severity) {
      params = params.set('severity', filter.severity);
    }
    if (filter.patientId) {
      params = params.set('patientId', filter.patientId);
    }
    return this.http.get<Alert[]>(this.url, { params });
  }

  summary(patientId?: number | null): Observable<AlertSummary> {
    let params = new HttpParams();
    if (patientId) {
      params = params.set('patientId', patientId);
    }
    return this.http.get<AlertSummary>(`${this.url}/summary`, { params });
  }

  get(id: number): Observable<AlertDetail> {
    return this.http.get<AlertDetail>(`${this.url}/${id}`);
  }

  /** The suggested e-mail for the patient (assistant only). */
  emailDraft(id: number): Observable<EmailDraft> {
    return this.http.get<EmailDraft>(`${this.url}/${id}/email-draft`);
  }

  sendEmail(id: number, request: { subject: string; body: string }): Observable<AlertDetail> {
    return this.http.post<AlertDetail>(`${this.url}/${id}/send-email`, request);
  }

  /** Runs the AI analysis of the glucose history now (it also runs every night). */
  runAiAnalysis(): Observable<AiRunReport> {
    return this.http.post<AiRunReport>(`${API_URL}/api/ai/glucose-risk/run`, {});
  }

  addAction(id: number, request: ActionRequest): Observable<AlertDetail> {
    return this.http.post<AlertDetail>(`${this.url}/${id}/actions`, request);
  }
}
