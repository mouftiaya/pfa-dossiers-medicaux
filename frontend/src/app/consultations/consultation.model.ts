export interface Consultation {
  id: number;
  patientId: number;
  consultationDate: string; // yyyy-MM-dd
  reason: string;
  notes: string | null;
  prescribedAnalyses: string | null;
  diagnosis: string | null;
  createdBy: string;
  createdAt: string;
}

export type ConsultationForm = Omit<Consultation, 'id' | 'patientId' | 'createdBy' | 'createdAt'>;
