import { Routes } from '@angular/router';

import { authGuard } from './auth/auth.guard';
import { LoginComponent } from './login/login.component';
import { PatientDetailComponent } from './patients/patient-detail.component';
import { PatientListComponent } from './patients/patient-list.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'patients', component: PatientListComponent, canActivate: [authGuard] },
  { path: 'patients/:id', component: PatientDetailComponent, canActivate: [authGuard] },
  { path: '', redirectTo: 'patients', pathMatch: 'full' },
  { path: '**', redirectTo: 'patients' },
];
