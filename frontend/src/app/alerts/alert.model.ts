export type AlertType =
  | 'HIGH_GLUCOSE'
  | 'LOW_GLUCOSE'
  | 'UNUSUAL_VARIATION'
  | 'PERSISTENT_HIGH'
  | 'PERSISTENT_LOW'
  | 'NO_MEASUREMENT'
  | 'CRITICAL_LAB_RESULT';
export type AlertSeverity = 'WARNING' | 'CRITICAL';
export type AlertStatus = 'NEW' | 'IN_PROGRESS' | 'RESOLVED';
export type ActionType = 'CONTACTED' | 'FOLLOW_UP_ORGANIZED' | 'CONSULTATION_PROPOSED' | 'NOTE' | 'RESOLVED';
export type Channel = 'EMAIL' | 'SMS';
export type DeliveryStatus = 'SENT' | 'FAILED' | 'SKIPPED_NO_CONTACT' | 'THROTTLED';

export interface Alert {
  id: number;
  patientId: number;
  type: AlertType;
  severity: AlertSeverity;
  status: AlertStatus;
  createdAt: string;
  updatedAt: string;
  /** null for the doctor (privacy rule). */
  detail: string | null;
  actionsCount: number;
  /** 0-100, only for alerts raised by the AI trend analysis (several weeks of measurements). */
  aiScore?: number | null;
}

export interface AlertAction {
  id: number;
  actor: string;
  type: ActionType;
  note: string | null;
  createdAt: string;
}

export interface AlertNotification {
  channel: Channel;
  status: DeliveryStatus;
  createdAt: string;
}

export interface AlertDetail extends Alert {
  actions: AlertAction[];
  notifications: AlertNotification[];
}

export interface AlertSummary {
  total: number;
  open: number;
  newCount: number;
  inProgress: number;
  resolved: number;
  byType: Record<string, number>;
  bySeverity: Record<string, number>;
}

export interface EmailDraft {
  alertId: number;
  patientId: number;
  /** null when the patient has no e-mail address (call him instead). */
  to: string | null;
  phone: string | null;
  subject: string;
  body: string;
}

export interface AiRunReport {
  patients: number;
  analysed: number;
  atRisk: number;
  alertsCreated: number;
  errors: number;
}

export interface ActionRequest {
  type: ActionType;
  note: string | null;
}

export const ALERT_TYPE_LABELS: Record<AlertType, string> = {
  HIGH_GLUCOSE: 'Glycémie élevée',
  LOW_GLUCOSE: 'Glycémie basse',
  UNUSUAL_VARIATION: 'Variation inhabituelle',
  PERSISTENT_HIGH: 'Hyperglycémie persistante',
  PERSISTENT_LOW: 'Hypoglycémie persistante',
  NO_MEASUREMENT: 'Absence de mesure',
  CRITICAL_LAB_RESULT: "Résultat d'analyse critique",
};

export const SEVERITY_LABELS: Record<AlertSeverity, string> = {
  WARNING: 'Attention',
  CRITICAL: 'Critique',
};

export const STATUS_LABELS: Record<AlertStatus, string> = {
  NEW: 'Nouvelle',
  IN_PROGRESS: 'En cours',
  RESOLVED: 'Résolue',
};

export const ACTION_LABELS: Record<ActionType, string> = {
  CONTACTED: 'Patient contacté',
  FOLLOW_UP_ORGANIZED: 'Suivi organisé',
  CONSULTATION_PROPOSED: 'Consultation proposée',
  NOTE: 'Note',
  RESOLVED: 'Clôturer',
};

export const CHANNEL_LABELS: Record<Channel, string> = { EMAIL: 'E-mail', SMS: 'SMS' };

export const DELIVERY_LABELS: Record<DeliveryStatus, string> = {
  SENT: 'Envoyé',
  FAILED: 'Échec',
  SKIPPED_NO_CONTACT: 'Non envoyé (pas de contact)',
  THROTTLED: 'Non envoyé (limite de fréquence)',
};
