import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { Patient } from '../patients/patient.model';
import { PatientService } from '../patients/patient.service';
import { LineChartComponent } from '../shared/line-chart.component';
import { AnalysisParameter, EvolutionResponse, FLAG_LABELS, errorText, rangeText } from './result.model';
import { ResultService } from './result.service';

/** Evolution over time of one parameter (HbA1c by default), from the VALIDATED results of the patient. */
@Component({
  selector: 'app-evolution',
  standalone: true,
  imports: [FormsModule, DatePipe, RouterLink, LineChartComponent],
  template: `
    <a [routerLink]="['/patients', patientId, 'results']">← Résultats du patient</a>

    <div class="toolbar">
      <h2>
        Évolution
        @if (patient(); as p) {
          <span class="muted"> — {{ p.firstName }} {{ p.lastName }}</span>
        }
      </h2>
      <span class="spacer"></span>
      <select class="param-select" name="parameter" [ngModel]="code()" (ngModelChange)="select($event)">
        @for (p of catalogue(); track p.id) {
          <option [value]="p.code">{{ p.label }}</option>
        }
      </select>
    </div>

    @if (error) {
      <p class="error">{{ error }}</p>
    }

    @if (evolution(); as e) {
      <div class="card">
        <p class="muted">
          Valeurs de référence : {{ range(e.parameter) }}. Seuls les résultats validés sont affichés.
        </p>
        <app-line-chart
          [points]="e.points"
          [refMin]="e.parameter.refMin"
          [refMax]="e.parameter.refMax"
          [unit]="e.parameter.unit"
        />
        <p class="legend">
          <span><i class="dot-key NORMAL"></i> Normal</span>
          <span><i class="dot-key HIGH"></i> Élevé</span>
          <span><i class="dot-key LOW"></i> Bas</span>
          <span><i class="band-key"></i> Valeurs de référence</span>
        </p>
      </div>

      @if (e.points.length > 0) {
        <div class="card">
          <table>
            <thead>
              <tr>
                <th>Date</th>
                <th>Valeur</th>
                <th>Interprétation</th>
                <th>Écart avec le précédent</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              @for (pt of e.points; track pt.resultId; let i = $index) {
                <tr>
                  <td>{{ pt.date | date: 'dd/MM/yyyy' }}</td>
                  <td>
                    <strong [class]="'flag ' + pt.flag">{{ pt.value }}</strong>
                    <span class="muted">{{ e.parameter.unit }}</span>
                  </td>
                  <td>
                    <span class="badge" [class.ok]="pt.flag === 'NORMAL'" [class.danger]="pt.flag !== 'NORMAL'">
                      {{ flagLabels[pt.flag] }}
                    </span>
                  </td>
                  <td>{{ i === 0 ? '—' : delta(e, i) }}</td>
                  <td class="actions"><a [routerLink]="['/results', pt.resultId]">Ouvrir</a></td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    }
  `,
})
export class EvolutionComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private patientService = inject(PatientService);
  private resultService = inject(ResultService);

  readonly patientId = Number(this.route.snapshot.paramMap.get('id'));
  readonly flagLabels = FLAG_LABELS;
  readonly range = rangeText;

  patient = signal<Patient | null>(null);
  catalogue = signal<AnalysisParameter[]>([]);
  code = signal('');
  evolution = signal<EvolutionResponse | null>(null);
  error = '';

  ngOnInit(): void {
    this.patientService.get(this.patientId).subscribe({
      next: (p) => this.patient.set(p),
      error: () => (this.error = 'Patient introuvable.'),
    });
    this.resultService.parameters().subscribe({
      next: (list) => {
        this.catalogue.set(list);
        const first = list.find((p) => p.code === 'HBA1C') ?? list[0];
        if (first) {
          this.select(first.code);
        }
      },
      error: (err) => (this.error = errorText(err, 'Impossible de charger les paramètres.')),
    });
  }

  select(code: string): void {
    this.code.set(code);
    this.error = '';
    this.resultService.evolution(this.patientId, code).subscribe({
      next: (e) => this.evolution.set(e),
      error: (err) => (this.error = errorText(err, "Impossible de charger l'évolution.")),
    });
  }

  delta(e: EvolutionResponse, index: number): string {
    const diff = Math.round((e.points[index].value - e.points[index - 1].value) * 100) / 100;
    return `${diff > 0 ? '+' : ''}${String(diff).replace('.', ',')} ${e.parameter.unit}`;
  }
}
