export type DiabetesType = 'TYPE_1' | 'TYPE_2' | 'GESTATIONAL' | 'OTHER';

export interface Patient {
  id: number;
  firstName: string;
  lastName: string;
  dateOfBirth: string; // yyyy-MM-dd
  gender: string | null;
  phone: string | null;
  email: string | null;
  diabetesType: DiabetesType | null;
  allergies: string | null;
  medicalHistory: string | null;
  createdAt: string;
}

export type PatientForm = Omit<Patient, 'id' | 'createdAt'>;

export const DIABETES_LABELS: Record<DiabetesType, string> = {
  TYPE_1: 'Type 1',
  TYPE_2: 'Type 2',
  GESTATIONAL: 'Gestationnel',
  OTHER: 'Autre',
};
