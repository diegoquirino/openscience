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
    path: 'label',
    canActivate: [participantOnly],
    loadComponent: () => import('./label/label-home.component').then((m) => m.LabelHomeComponent),
  },
  {
    path: 'label/items/:position',
    canActivate: [participantOnly],
    loadComponent: () => import('./label/item/item.component').then((m) => m.LabelItemComponent),
  },
  {
    path: 'label/review',
    canActivate: [participantOnly],
    loadComponent: () => import('./label/review/review.component').then((m) => m.LabelReviewComponent),
  },
  {
    path: 'label/done',
    canActivate: [participantOnly],
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
  { path: '**', component: NotFoundComponent },
];
