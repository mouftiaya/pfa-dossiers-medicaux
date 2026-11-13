export type MeasureContext = 'FASTING' | 'BEFORE_MEAL' | 'AFTER_MEAL' | 'BEDTIME';
export type GlucoseFlag = 'CRITICAL_LOW' | 'LOW' | 'IN_TARGET' | 'OUT_OF_TARGET' | 'HIGH' | 'CRITICAL_HIGH';
export type Period = 'DAY' | 'WEEK' | 'MONTH';

/** Unit: mg/dL. */
export interface Measurement {
  id: number;
  value: number;
  measuredAt: string; // yyyy-MM-ddTHH:mm:ss
  context: MeasureContext;
  note: string | null;
  flag: GlucoseFlag;
}

export interface MeasurementRequest {
  value: number;
  measuredAt: string;
  context: MeasureContext;
  note: string | null;
}

export interface TargetRequest {
  targetMin: number;
  targetMax: number;
  lowThreshold: number;
  highThreshold: number;
  criticalLow: number;
  criticalHigh: number;
  absenceDays: number;
}

export interface Target extends TargetRequest {
  patientId: number;
  defined: boolean;
  validated: boolean;
  definedBy: string | null;
  updatedAt: string | null;
}

export interface StatsPoint {
  at: string;
  average: number;
  minimum: number;
  maximum: number;
  count: number;
}

export interface Stats {
  period: Period;
  from: string;
  to: string;
  count: number;
  average: number | null;
  minimum: number | null;
  maximum: number | null;
  percentInTarget: number | null;
  highCount: number;
  lowCount: number;
  target: Target;
  points: StatsPoint[];
}

export interface GlucoseSummary {
  patientId: number;
  periodDays: number;
  count: number;
  average: number | null;
  percentInTarget: number | null;
  highCount: number;
  lowCount: number;
  lastMeasurementAt: string | null;
  target: Target;
}

export const CONTEXT_LABELS: Record<MeasureContext, string> = {
  FASTING: 'À jeun',
  BEFORE_MEAL: 'Avant repas',
  AFTER_MEAL: 'Après repas',
  BEDTIME: 'Au coucher',
};

export const GLUCOSE_FLAG_LABELS: Record<GlucoseFlag, string> = {
  CRITICAL_LOW: 'Critique bas',
  LOW: 'Bas',
  IN_TARGET: 'Dans la cible',
  OUT_OF_TARGET: 'Hors cible',
  HIGH: 'Élevé',
  CRITICAL_HIGH: 'Critique élevé',
};

export const PERIOD_LABELS: Record<Period, string> = {
  DAY: 'Jour',
  WEEK: 'Semaine',
  MONTH: 'Mois',
};

/** Same rule as the server, used to colour the points of the chart (daily averages have no flag). */
export function flagFor(value: number, t: TargetRequest): GlucoseFlag {
  if (value <= t.criticalLow) {
    return 'CRITICAL_LOW';
  }
  if (value < t.lowThreshold) {
    return 'LOW';
  }
  if (value >= t.criticalHigh) {
    return 'CRITICAL_HIGH';
  }
  if (value > t.highThreshold) {
    return 'HIGH';
  }
  return value >= t.targetMin && value <= t.targetMax ? 'IN_TARGET' : 'OUT_OF_TARGET';
}

/** Returns an error message when the ordering rule of the contract is broken, otherwise null. */
export function targetProblem(t: TargetRequest): string | null {
  const nums = [t.criticalLow, t.lowThreshold, t.targetMin, t.targetMax, t.highThreshold, t.criticalHigh];
  if (nums.some((n) => typeof n !== 'number' || !Number.isFinite(n))) {
    return 'Tous les seuils doivent être renseignés.';
  }
  const ok =
    t.criticalLow > 0 &&
    t.criticalLow < t.lowThreshold &&
    t.lowThreshold <= t.targetMin &&
    t.targetMin < t.targetMax &&
    t.targetMax <= t.highThreshold &&
    t.highThreshold < t.criticalHigh;
  if (!ok) {
    return 'Les seuils doivent respecter : critique bas < seuil bas ≤ cible min < cible max ≤ seuil haut < critique haut.';
  }
  if (!Number.isInteger(t.absenceDays) || t.absenceDays < 1 || t.absenceDays > 30) {
    return "Le délai d'absence de mesure doit être compris entre 1 et 30 jours.";
  }
  return null;
}

export function fmt(n: number | null | undefined, digits = 0): string {
  return n === null || n === undefined ? '—' : n.toFixed(digits).replace('.', ',');
}

/** Server error text (ResponseStatusException reason) if present, otherwise the fallback. */
export function serverMessage(err: { error?: unknown }, fallback: string): string {
  const e = err.error as { message?: unknown } | string | null | undefined;
  if (e && typeof e === 'object' && typeof e.message === 'string' && e.message.trim()) {
    return e.message;
  }
  return fallback;
}

/** Badge colour class (see styles.css: ok, warn, danger, critical). */
export function flagClass(flag: GlucoseFlag): string {
  switch (flag) {
    case 'CRITICAL_LOW':
    case 'CRITICAL_HIGH':
      return 'critical';
    case 'LOW':
    case 'HIGH':
      return 'danger';
    case 'OUT_OF_TARGET':
      return 'warn';
    default:
      return 'ok';
  }
}

function pad(n: number): string {
  return String(n).padStart(2, '0');
}

/** Local date as yyyy-MM-dd (not UTC). */
export function localDate(d: Date = new Date()): string {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

/** Local date-time as yyyy-MM-ddTHH:mm, the format of <input type="datetime-local">. */
export function localDateTimeInput(d: Date = new Date()): string {
  return `${localDate(d)}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}
