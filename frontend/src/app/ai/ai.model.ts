export type SuggestionStatus = 'PENDING' | 'APPROVED' | 'MODIFIED' | 'REJECTED';
export type Decision = 'APPROVE' | 'MODIFY' | 'REJECT';

export interface AiSuggestion {
  id: number;
  patientId: number;
  labResultId: number;
  consultationId: number | null;
  suggestedDiagnosis: string;
  confidence: number; // 0..1 : share of the key parameters available, not a probability
  status: SuggestionStatus;
  finalDiagnosis: string | null;
  requestedBy: string;
  createdAt: string;
  decidedBy: string | null;
  decidedAt: string | null;
}

export interface AnalysisResponse {
  suggestionId: number;
  labResultId: number;
  suggestedDiagnosis: string;
  confidence: number;
  status: SuggestionStatus;
}

export const SUGGESTION_LABELS: Record<SuggestionStatus, string> = {
  PENDING: 'En attente de décision',
  APPROVED: 'Validée par le médecin',
  MODIFIED: 'Modifiée par le médecin',
  REJECTED: 'Rejetée par le médecin',
};
