import { Routes } from '@angular/router';
import { administratorOnly, participantOnly, visitorOnly } from './core/guards';
import { onboardingStep, participationOnly } from './core/onboarding-guards';
import { HomeComponent } from './home/home.component';
import { NotFoundComponent } from './not-found/not-found.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  {
    path: 'participate',
    canActivate: [visitorOnly],
    loadComponent: () => import('./participate/sign-in.component').then((m) => m.SignInComponent),
  },
  {
    path: 'participate/code',
    canActivate: [visitorOnly],
    loadComponent: () => import('./participate/code.component').then((m) => m.CodeComponent),
  },
  {
    path: 'welcome',
    canActivate: [participantOnly],
    loadComponent: () => import('./welcome/welcome.component').then((m) => m.WelcomeComponent),
  },
  {
    path: 'consent',
    canActivate: [participantOnly, onboardingStep('consent')],
    loadComponent: () => import('./onboarding/consent.component').then((m) => m.ConsentComponent),
  },
  {
    path: 'consent/declined',
    canActivate: [participantOnly],
    data: { kind: 'declined' },
    loadComponent: () => import('./onboarding/closing.component').then((m) => m.ClosingComponent),
  },
  {
    path: 'consent/reference/:version',
    canActivate: [participantOnly],
    loadComponent: () =>
      import('./onboarding/consent-reference.component').then((m) => m.ConsentReferenceComponent),
  },
  {
    path: 'skill',
    canActivate: [participantOnly, onboardingStep('skill')],
    loadComponent: () => import('./onboarding/skill.component').then((m) => m.SkillComponent),
  },
  {
    path: 'screening',
    canActivate: [participantOnly, onboardingStep('screening')],
    loadComponent: () => import('./onboarding/screening.component').then((m) => m.ScreeningComponent),
  },
  {
    path: 'screening/closed',
    canActivate: [participantOnly, onboardingStep('ineligible')],
    data: { kind: 'ineligible' },
    loadComponent: () => import('./onboarding/closing.component').then((m) => m.ClosingComponent),
  },
  {
    path: 'not-open',
    canActivate: [participantOnly, onboardingStep('not-open')],
    data: { kind: 'not-open' },
    loadComponent: () => import('./onboarding/closing.component').then((m) => m.ClosingComponent),
  },
  {
    path: 'label',
    canActivate: [participantOnly, participationOnly],
    loadComponent: () => import('./label/label-home.component').then((m) => m.LabelHomeComponent),
  },
  {
    path: 'label/items/:position',
    canActivate: [participantOnly, participationOnly],
    loadComponent: () => import('./label/item/item.component').then((m) => m.LabelItemComponent),
  },
  {
    path: 'label/review',
    canActivate: [participantOnly, participationOnly],
    loadComponent: () => import('./label/review/review.component').then((m) => m.LabelReviewComponent),
  },
  {
    path: 'label/done',
    canActivate: [participantOnly, participationOnly],
    loadComponent: () => import('./label/label-done.component').then((m) => m.LabelDoneComponent),
  },
  {
    path: 'admin',
    canActivate: [administratorOnly],
    loadComponent: () => import('./admin/admin.component').then((m) => m.AdminComponent),
  },
  {
    path: 'admin/import',
    canActivate: [administratorOnly],
    loadComponent: () => import('./admin/import/import.component').then((m) => m.ImportComponent),
  },
  {
    path: 'admin/export',
    canActivate: [administratorOnly],
    loadComponent: () => import('./admin/export/export.component').then((m) => m.ExportComponent),
  },
  {
    path: 'admin/dashboard',
    canActivate: [administratorOnly],
    loadComponent: () => import('./admin/dashboard/dashboard.component').then((m) => m.DashboardComponent),
  },
  {
    path: 'admin/participants',
    canActivate: [administratorOnly],
    loadComponent: () =>
      import('./admin/participants/participants.component').then((m) => m.ParticipantsComponent),
  },
  { path: '**', component: NotFoundComponent },
];
