import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../config';
import {
  AnalysisParameter,
  ComparisonRow,
  EvolutionResponse,
  LabResult,
  ManualResultRequest,
  UploadResponse,
  ValueInput,
} from './result.model';

@Injectable({ providedIn: 'root' })
export class ResultService {
  private http = inject(HttpClient);
  private url = `${API_URL}/api/results`;

  /** Catalogue of the known parameters (glycemia, HbA1c, ...). */
  parameters(): Observable<AnalysisParameter[]> {
    return this.http.get<AnalysisParameter[]>(`${this.url}/parameters`);
  }

  list(patientId: number): Observable<LabResult[]> {
    return this.http.get<LabResult[]>(this.url, { params: new HttpParams().set('patientId', patientId) });
  }

  get(id: number): Observable<LabResult> {
    return this.http.get<LabResult>(`${this.url}/${id}`);
  }

  createManual(request: ManualResultRequest): Observable<LabResult> {
    return this.http.post<LabResult>(this.url, request);
  }

  upload(patientId: number, resultDate: string, file: File, consultationId: number | null): Observable<UploadResponse> {
    const body = new FormData();
    body.append('patientId', String(patientId));
    body.append('resultDate', resultDate);
    body.append('file', file);
    if (consultationId !== null) {
      body.append('consultationId', String(consultationId));
    }
    return this.http.post<UploadResponse>(`${this.url}/upload`, body);
  }

  replaceValues(id: number, values: ValueInput[]): Observable<LabResult> {
    return this.http.put<LabResult>(`${this.url}/${id}/values`, { values });
  }

  requestCorrection(id: number, note: string): Observable<LabResult> {
    return this.http.post<LabResult>(`${this.url}/${id}/request-correction`, { note });
  }

  validate(id: number): Observable<LabResult> {
    return this.http.post<LabResult>(`${this.url}/${id}/validate`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  /** The original document; downloaded with the token, then shown from a temporary local URL. */
  file(id: number): Observable<Blob> {
    return this.http.get(`${this.url}/${id}/file`, { responseType: 'blob' });
  }

  evolution(patientId: number, parameterCode: string): Observable<EvolutionResponse> {
    const params = new HttpParams().set('patientId', patientId).set('parameter', parameterCode);
    return this.http.get<EvolutionResponse>(`${this.url}/evolution`, { params });
  }

  // ---------- patient portal (own validated results only) ----------

  mine(): Observable<LabResult[]> {
    return this.http.get<LabResult[]>(`${this.url}/me`);
  }

  mineFile(id: number): Observable<Blob> {
    return this.http.get(`${this.url}/me/${id}/file`, { responseType: 'blob' });
  }

  mineEvolution(parameterCode: string): Observable<EvolutionResponse> {
    return this.http.get<EvolutionResponse>(`${this.url}/me/evolution`, {
      params: new HttpParams().set('parameter', parameterCode),
    });
  }

  compare(ids: number[]): Observable<ComparisonRow[]> {
    return this.http.get<ComparisonRow[]>(`${this.url}/compare`, {
      params: new HttpParams().set('ids', ids.join(',')),
    });
  }
}
