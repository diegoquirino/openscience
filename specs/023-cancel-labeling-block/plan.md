# Plan: 023-cancel-labeling-block

## Summary

Add cancel-reservation: UI next to Continue (actions panel from 022), confirmation dialog, API that frees the held task and deletes its drafts only, then a done screen with two icon cards (another block / leave). Version **0.23.0**.

## Technical approach

- Backend: `TaskReservationService.cancel(participantId)` (or DELETE on held task); clear responses for task id; set status `free`, clear participant/expiry; expose HAL `vd:cancel-reservation`.
- Frontend: Cancel button ? MatDialog (or confirm pattern used in admin); on confirm call cancel link; navigate to `/label/done?outcome=cancelled` (or dedicated outcome).
- Done screen: two cards with Material icons — “Rotular outro bloco” ? `/label`; “Sair” ? sign-out or home (match existing leave patterns on done).
- Toast on failure; optional brief success toast before navigation.

## Contracts

HAL/OpenAPI: cancel link on held task or reservations resource.

## Target platform

Local Docker staging; production Cloud Run (deploy only with approval).
