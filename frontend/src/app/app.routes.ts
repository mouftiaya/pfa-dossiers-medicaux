import { Routes } from '@angular/router';

import { AuditComponent } from './admin/audit.component';
import { UsersComponent } from './admin/users.component';
import { authGuard, homeGuard, roleGuard } from './auth/auth.guard';
import { ShellComponent } from './layout/shell.component';
import { LoginComponent } from './login/login.component';
import { PatientDetailComponent } from './patients/patient-detail.component';
import { PatientListComponent } from './patients/patient-list.component';
import { EvolutionComponent } from './results/evolution.component';
import { ResultDetailComponent } from './results/result-detail.component';
import { ResultsComponent } from './results/results.component';

const clinical = [roleGuard(['DOCTOR', 'ASSISTANT'])];
const admin = [roleGuard(['ADMIN'])];

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
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
      { path: 'admin/users', component: UsersComponent, canActivate: admin },
      { path: 'admin/audit', component: AuditComponent, canActivate: admin },
      { path: '', pathMatch: 'full', canActivate: [homeGuard], children: [] },
      { path: '**', canActivate: [homeGuard], children: [] },
    ],
  },
];
