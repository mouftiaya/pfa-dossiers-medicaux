export type ValueFlag = 'NORMAL' | 'LOW' | 'HIGH';
export type ReviewStatus = 'PENDING_REVIEW' | 'VALIDATED';
export type EntryMode = 'MANUAL' | 'AI_EXTRACTED';

export interface AnalysisParameter {
  id: number;
  code: string;
  label: string;
  unit: string;
  refMin: number | null;
  refMax: number | null;
}

export interface LabValue {
  id: number;
  parameter: AnalysisParameter;
  value: number;
  flag: ValueFlag;
}

export interface LabResult {
  id: number;
  patientId: number;
  consultationId: number | null;
  resultDate: string; // yyyy-MM-dd
  fileName: string | null;
  hasFile: boolean;
  entryMode: EntryMode;
  status: ReviewStatus;
  reviewNote: string | null;
  uploadedBy: string;
  uploadedAt: string;
  validatedBy: string | null;
  validatedAt: string | null;
  values: LabValue[];
}

export interface UploadResponse {
  result: LabResult;
  extracted: boolean;
  recognizedCount: number;
  message: string;
}

export interface ValueInput {
  parameterId: number;
  value: number;
}

export interface ManualResultRequest {
  patientId: number;
  consultationId: number | null;
  resultDate: string;
  values: ValueInput[];
}

export interface EvolutionPoint {
  resultId: number;
  date: string;
  value: number;
  flag: ValueFlag;
}

export interface EvolutionResponse {
  parameter: AnalysisParameter;
  points: EvolutionPoint[];
}

export interface ComparisonRow {
  parameter: AnalysisParameter;
  points: EvolutionPoint[];
  change: number | null;
}

export const FLAG_LABELS: Record<ValueFlag, string> = {
  NORMAL: 'Normal',
  LOW: 'Bas',
  HIGH: 'Élevé',
};

export const STATUS_LABELS: Record<ReviewStatus, string> = {
  PENDING_REVIEW: 'À valider',
  VALIDATED: 'Validé',
};

export const MODE_LABELS: Record<EntryMode, string> = {
  MANUAL: 'Saisie manuelle',
  AI_EXTRACTED: 'Extraction automatique',
};

/** "0,70 – 1,10 g/L", "≤ 5,6 %" or "—" depending on the reference range of the parameter. */
export function rangeText(p: AnalysisParameter): string {
  const fmt = (n: number) => String(n).replace('.', ',');
  if (p.refMin !== null && p.refMax !== null) {
    return `${fmt(p.refMin)} – ${fmt(p.refMax)} ${p.unit}`;
  }
  if (p.refMax !== null) {
    return `≤ ${fmt(p.refMax)} ${p.unit}`;
  }
  if (p.refMin !== null) {
    return `≥ ${fmt(p.refMin)} ${p.unit}`;
  }
  return '—';
}

/** Readable message for an error answer of the API. */
export function errorText(err: { status?: number }, fallback: string): string {
  switch (err.status) {
    case 400:
      return 'Données invalides. Vérifiez les champs.';
    case 403:
      return "Vous n'avez pas le droit d'effectuer cette action.";
    case 404:
      return 'Élément introuvable.';
    case 409:
      return "Action impossible dans l'état actuel (résultat déjà validé, ou non validé).";
    case 0:
      return 'Impossible de joindre le serveur.';
    default:
      return fallback;
  }
}
