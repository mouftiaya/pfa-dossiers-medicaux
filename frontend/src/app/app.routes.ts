import { Routes } from '@angular/router';

import { ActivateComponent } from './activation/activate.component';
import { AlertDetailComponent } from './alerts/alert-detail.component';
import { AlertsComponent } from './alerts/alerts.component';
import { AuditComponent } from './admin/audit.component';
import { UsersComponent } from './admin/users.component';
import { authGuard, homeGuard, roleGuard } from './auth/auth.guard';
import { ShellComponent } from './layout/shell.component';
import { LoginComponent } from './login/login.component';
import { PortalComponent } from './portal/portal.component';
import { PortalGlucoseComponent } from './portal/portal-glucose.component';
import { PatientDetailComponent } from './patients/patient-detail.component';
import { PatientListComponent } from './patients/patient-list.component';
import { EvolutionComponent } from './results/evolution.component';
import { ResultDetailComponent } from './results/result-detail.component';
import { ResultsComponent } from './results/results.component';

const clinical = [roleGuard(['DOCTOR', 'ASSISTANT'])];
const patientOnly = [roleGuard(['PATIENT'])];
const admin = [roleGuard(['ADMIN'])];

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'activate', component: ActivateComponent },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: 'patients', component: PatientListComponent, canActivate: clinical },
      { path: 'patients/:id', component: PatientDetailComponent, canActivate: clinical },
      { path: 'patients/:id/results', component: ResultsComponent, canActivate: clinical },
      { path: 'patients/:id/evolution', component: EvolutionComponent, canActivate: clinical },
      { path: 'results/:id', component: ResultDetailComponent, canActivate: clinical },
      { path: 'alerts', component: AlertsComponent, canActivate: clinical },
      { path: 'alerts/:id', component: AlertDetailComponent, canActivate: clinical },
      { path: 'portal', component: PortalComponent, canActivate: patientOnly },
      { path: 'portal/glucose', component: PortalGlucoseComponent, canActivate: patientOnly },
      { path: 'admin/users', component: UsersComponent, canActivate: admin },
      { path: 'admin/audit', component: AuditComponent, canActivate: admin },
      { path: '', pathMatch: 'full', canActivate: [homeGuard], children: [] },
      { path: '**', canActivate: [homeGuard], children: [] },
    ],
  },
];
