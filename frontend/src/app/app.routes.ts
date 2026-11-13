import { Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { NotFoundComponent } from './not-found/not-found.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  {
    path: 'participate',
    loadComponent: () => import('./participate/sign-in.component').then((m) => m.SignInComponent),
  },
  {
    path: 'participate/code',
    loadComponent: () => import('./participate/code.component').then((m) => m.CodeComponent),
  },
  { path: 'welcome', loadComponent: () => import('./welcome/welcome.component').then((m) => m.WelcomeComponent) },
  { path: '**', component: NotFoundComponent },
];
