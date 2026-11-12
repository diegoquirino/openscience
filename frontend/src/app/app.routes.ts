import { Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { NotFoundComponent } from './not-found/not-found.component';
import { ParticipateComponent } from './participate/participate.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'participate', component: ParticipateComponent },
  { path: '**', component: NotFoundComponent },
];
