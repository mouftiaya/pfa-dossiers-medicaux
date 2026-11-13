import { Component, computed, input } from '@angular/core';

export interface ChartPoint {
  date: string; // yyyy-MM-dd, or yyyy-MM-ddTHH:mm:ss for time series
  value: number;
  /** Any flag name (NORMAL, HIGH, CRITICAL_LOW, IN_TARGET, ...): used as CSS class of the point. */
  flag: string;
}

/** Optional horizontal line (e.g. a threshold). `kind` only picks the colour. */
export interface ChartLine {
  value: number;
  label: string;
  kind: 'warn' | 'crit';
}

interface Dot {
  x: number;
  y: number;
  flag: string;
  tooltip: string;
}

/**
 * Small line chart drawn in SVG (no chart library to install).
 * The green band is the reference range; points are coloured by flag.
 */
@Component({
  selector: 'app-line-chart',
  standalone: true,
  template: `
    @if (geometry(); as g) {
      <svg class="chart" [attr.viewBox]="'0 0 ' + width + ' ' + height" role="img" aria-label="Évolution dans le temps">
        @if (g.band) {
          <rect class="band" [attr.x]="left" [attr.y]="g.band.y" [attr.width]="width - left - right" [attr.height]="g.band.height" />
        }
        @for (t of g.yTicks; track t.y) {
          <line class="grid" [attr.x1]="left" [attr.x2]="width - right" [attr.y1]="t.y" [attr.y2]="t.y" />
          <text class="tick" [attr.x]="left - 8" [attr.y]="t.y + 4" text-anchor="end">{{ t.label }}</text>
        }
        <line class="axis" [attr.x1]="left" [attr.x2]="left" [attr.y1]="top" [attr.y2]="height - bottom" />
        <line class="axis" [attr.x1]="left" [attr.x2]="width - right" [attr.y1]="height - bottom" [attr.y2]="height - bottom" />
        @for (h of g.hLines; track $index) {
          <line [attr.class]="'hline ' + h.kind" [attr.x1]="left" [attr.x2]="width - right" [attr.y1]="h.y" [attr.y2]="h.y" />
          <text [attr.class]="'hline-label ' + h.kind" [attr.x]="width - right" [attr.y]="h.y - 3" text-anchor="end">{{ h.label }}</text>
        }
        <polyline class="line" [attr.points]="g.path" fill="none" />
        @for (d of g.dots; track $index) {
          <circle [attr.class]="'dot ' + d.flag" [attr.cx]="d.x" [attr.cy]="d.y" r="5">
            <title>{{ d.tooltip }}</title>
          </circle>
        }
        @for (l of g.xLabels; track $index) {
          <text class="tick" [attr.x]="l.x" [attr.y]="height - bottom + 18" text-anchor="middle">{{ l.label }}</text>
        }
        @if (unit()) {
          <text class="tick" [attr.x]="left" [attr.y]="12">{{ unit() }}</text>
        }
      </svg>
    } @else {
      <p class="muted">{{ emptyText() }}</p>
    }
  `,
})
export class LineChartComponent {
  points = input.required<ChartPoint[]>();
  refMin = input<number | null>(null);
  refMax = input<number | null>(null);
  unit = input('');
  lines = input<ChartLine[]>([]);
  emptyText = input('Aucune valeur validée pour ce paramètre.');

  readonly width = 720;
  readonly height = 300;
  readonly left = 56;
  readonly right = 20;
  readonly top = 22;
  readonly bottom = 44;

  geometry = computed(() => {
    const pts = this.points();
    if (pts.length === 0) {
      return null;
    }
    const { width: W, height: H, left: L, right: R, top: T, bottom: B } = this;
    const refMin = this.refMin();
    const refMax = this.refMax();

    const lines = this.lines();
    const values = [...pts.map((p) => p.value), ...lines.map((l) => l.value)];
    let lo = Math.min(...values, refMin ?? Infinity);
    let hi = Math.max(...values, refMax ?? -Infinity);
    if (lo === hi) {
      lo -= 1;
      hi += 1;
    }
    const pad = (hi - lo) * 0.1;
    const allPositive = lo >= 0;
    lo -= pad;
    hi += pad;
    if (allPositive && lo < 0) {
      lo = 0;
    }

    const times = pts.map((p) => new Date(p.date).getTime());
    const t0 = Math.min(...times);
    const t1 = Math.max(...times);
    const x = (i: number) => (t1 === t0 ? (L + W - R) / 2 : L + ((times[i] - t0) / (t1 - t0)) * (W - L - R));
    const y = (v: number) => T + (1 - (v - lo) / (hi - lo)) * (H - T - B);

    const decimals = hi - lo < 10 ? 2 : 1;
    const yTicks = [0, 1, 2, 3, 4].map((i) => {
      const v = lo + (i * (hi - lo)) / 4;
      return { y: y(v), label: v.toFixed(decimals).replace('.', ',') };
    });

    let band: { y: number; height: number } | null = null;
    if (refMin !== null || refMax !== null) {
      const yTop = Math.max(T, y(refMax ?? hi));
      const yBottom = Math.min(H - B, y(refMin ?? lo));
      if (yBottom > yTop) {
        band = { y: yTop, height: yBottom - yTop };
      }
    }

    const dots: Dot[] = pts.map((p, i) => ({
      x: x(i),
      y: y(p.value),
      flag: p.flag,
      tooltip: `${this.formatDate(p.date)} : ${String(p.value).replace('.', ',')}`,
    }));

    const step = Math.ceil(pts.length / (pts[0].date.includes('T') ? 5 : 8));
    const xLabels = pts
      .map((p, i) => ({ x: x(i), label: this.formatDate(p.date), i }))
      .filter((l) => l.i % step === 0 || l.i === pts.length - 1);

    const hLines = lines.map((l) => ({ y: y(l.value), kind: l.kind, label: l.label }));

    return { yTicks, band, dots, hLines, xLabels, path: dots.map((d) => `${d.x},${d.y}`).join(' ') };
  });

  private formatDate(iso: string): string {
    const [day, time] = iso.split('T');
    const [yyyy, mm, dd] = day.split('-');
    const text = `${dd}/${mm}/${yyyy.slice(2)}`;
    return time ? `${text} ${time.slice(0, 5)}` : text;
  }
}
