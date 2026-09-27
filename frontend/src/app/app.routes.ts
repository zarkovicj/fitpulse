import { Routes } from '@angular/router';

import { adminGuard, authGuard, guestGuard, userGuard } from './core/auth/auth-guard';
import { unsavedChangesGuard } from './shared/guards/unsaved-changes';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    title: 'Prijava · FitPulse',
    loadComponent: () => import('./features/auth/login').then((m) => m.Login),
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    title: 'Novi nalog · FitPulse',
    loadComponent: () => import('./features/auth/register').then((m) => m.Register),
  },
  {
    path: 'forgot-password',
    canActivate: [guestGuard],
    title: 'Zaboravljena lozinka · FitPulse',
    loadComponent: () => import('./features/auth/forgot-password').then((m) => m.ForgotPassword),
  },
  {
    path: 'reset-password',
    title: 'Nova lozinka · FitPulse',
    loadComponent: () => import('./features/auth/reset-password').then((m) => m.ResetPassword),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./core/layout/shell').then((m) => m.Shell),
    children: [
      {
        path: '',
        canActivate: [userGuard],
        title: 'FitPulse',
        loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.Dashboard),
      },
      {
        path: 'workouts',
        canActivate: [userGuard],
        title: 'Treninzi · FitPulse',
        loadComponent: () => import('./features/workouts/workouts').then((m) => m.Workouts),
      },
      {
        path: 'workouts/:id',
        canActivate: [userGuard],
        title: 'Trening · FitPulse',
        loadComponent: () => import('./features/workouts/workout-view').then((m) => m.WorkoutView),
      },
      {
        path: 'templates',
        canActivate: [userGuard],
        title: 'Šabloni · FitPulse',
        loadComponent: () => import('./features/templates/templates').then((m) => m.Templates),
      },
      {
        path: 'templates/new',
        title: 'Novi šablon · FitPulse',
        loadComponent: () => import('./features/templates/template-editor').then((m) => m.TemplateEditor),
        canDeactivate: [unsavedChangesGuard],
      },
      {
        path: 'templates/:id',
        title: 'Šablon · FitPulse',
        loadComponent: () => import('./features/templates/template-detail').then((m) => m.TemplateDetail),
      },
      {
        path: 'templates/:id/edit',
        title: 'Izmena šablona · FitPulse',
        loadComponent: () => import('./features/templates/template-editor').then((m) => m.TemplateEditor),
        canDeactivate: [unsavedChangesGuard],
      },
      {
        path: 'exercises',
        canActivate: [userGuard],
        title: 'Vežbe · FitPulse',
        loadComponent: () => import('./features/exercises/exercises').then((m) => m.Exercises),
      },
      {
        path: 'progress',
        canActivate: [userGuard],
        loadComponent: () => import('./features/progress/progress-layout').then((m) => m.ProgressLayout),
        children: [
          {
            path: '',
            title: 'Napredak · FitPulse',
            loadComponent: () => import('./features/progress/strength').then((m) => m.Strength),
          },
          {
            path: 'body',
            title: 'Telo · FitPulse',
            loadComponent: () => import('./features/progress/body').then((m) => m.Body),
          },
        ],
      },
      {
        path: 'profile',
        title: 'Profil · FitPulse',
        loadComponent: () => import('./features/profile/profile').then((m) => m.Profile),
        canDeactivate: [unsavedChangesGuard],
      },
      {
        path: 'admin',
        canActivate: [adminGuard],
        title: 'Administracija · FitPulse',
        loadComponent: () => import('./features/admin/admin').then((m) => m.Admin),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
