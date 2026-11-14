import { Routes } from '@angular/router';
import { administratorOnly, participantOnly, visitorOnly } from './core/guards';
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
    path: 'admin',
    canActivate: [administratorOnly],
    loadComponent: () => import('./admin/admin.component').then((m) => m.AdminComponent),
  },
  { path: '**', component: NotFoundComponent },
];
