import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../config';
import { AiSuggestion, AnalysisResponse, Decision } from './ai.model';

@Injectable({ providedIn: 'root' })
export class AiService {
  private http = inject(HttpClient);
  private url = `${API_URL}/api/ai`;

  /** Asks the analysis of a VALIDATED result. */
  analyse(labResultId: number, consultationId: number | null): Observable<AnalysisResponse> {
    return this.http.post<AnalysisResponse>(`${this.url}/analyses`, { labResultId, consultationId });
  }

  forResult(labResultId: number): Observable<AiSuggestion[]> {
    return this.http.get<AiSuggestion[]>(`${this.url}/suggestions`, {
      params: new HttpParams().set('labResultId', labResultId),
    });
  }

  decide(id: number, decision: Decision, finalDiagnosis: string | null): Observable<AiSuggestion> {
    return this.http.put<AiSuggestion>(`${this.url}/suggestions/${id}/decision`, { decision, finalDiagnosis });
  }
}
