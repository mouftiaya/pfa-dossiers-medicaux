import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService, Role } from './auth.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.isLoggedIn() ? true : router.createUrlTree(['/login']);
};

/** Lets only the given roles in; anyone else goes back to the home page of their role. */
export const roleGuard =
  (roles: Role[]): CanActivateFn =>
  () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    const role = auth.session?.role;
    return role && roles.includes(role) ? true : router.createUrlTree(['/']);
  };

/** "/" sends each role to its own start page (the administrator has no access to patient data). */
export const homeGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const role = auth.session?.role;
  if (!role) {
    return router.createUrlTree(['/login']);
  }
  const home = role === 'ADMIN' ? '/admin/users' : role === 'PATIENT' ? '/portal' : '/patients';
  return router.createUrlTree([home]);
};
