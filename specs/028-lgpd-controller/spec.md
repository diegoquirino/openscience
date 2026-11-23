# Feature Specification: LGPD controller = researcher (natural person)

**Feature Branch**: `028-lgpd-controller`

**Created**: 2026-11-23

**Status**: Complete at **0.28.0** (1.0 still gated; no cloud deploy)

**Input**: Section VIII of the TCLE currently lists **Universidade Federal de Campina Grande** (CNPJ) as the LGPD **Controlador**. The researcher decided the controller for data held in Veredito is **Carlos Diego Quirino Lima** (natural person), with the same contact already used as responsible researcher. The UFCG institutional DPO bullet must not imply the university is the controller. CEP contact (section XIII) and academic affiliation stay unchanged.

**Depends on**: 006, 027

## User Scenarios

### US1 - Controller named as researcher (P1)

1. Given a clean local staging database after this slice, When a participant opens the current consent form in pt-BR, Then section VIII lists **Controlador: Carlos Diego Quirino Lima — PPGCC/CEEI/UFCG — e-mail diegoquirino@gmail.com — telefone (83) 98818-1876** and does **not** list UFCG CNPJ as Controlador.
2. Given en-US / es-ES consent texts, When shown, Then Controller / Responsable del tratamiento use the same person and contact (not UFCG CNPJ).

### US2 - Encarregado / LGPD contact (P1)

1. Given section VIII, When the Encarregado / operational LGPD contact bullet is read, Then it points to the controller (diegoquirino@gmail.com) and does **not** describe an institutional UFCG DPO as if UFCG were the controller.
2. Given section XIII (CEP), When read, Then CEP-CES/UFCG contact remains available for ethics complaints (unchanged role of the committee).

### US3 - CEP package mirror (P2)

1. Given the CEP draft [`TCLE_ambiente_virtual_pt-BR.md`](../../../ISSTA_2027/CEP-TCLE/03_tcle_virtual/TCLE_ambiente_virtual_pt-BR.md), When compared to the staging fixture pt-BR section VIII, Then Controlador and Encarregado wording match the researcher decision.

## Requirements

- **FR-001**: Staging fixture `V6__consent_form_fixture.sql` (pt-BR, en-US, es-ES) MUST set the LGPD controller to Carlos Diego Quirino Lima with e-mail diegoquirino@gmail.com and phone (83) 98818-1876; MUST remove UFCG address/CNPJ from the controller bullet.
- **FR-002**: Replace the “Encarregado institucional da UFCG” bullet with contact via the controller (same e-mail); keep ANPD / CEP paths as already stated in Direitos / XIII.
- **FR-003**: Mirror Controlador + Encarregado in `ISSTA_2027/CEP-TCLE/03_tcle_virtual/TCLE_ambiente_virtual_pt-BR.md`.
- **FR-004**: App version **0.28.0** (`0.<spec>.0`); product **1.0.0** remains gated until **promover a 1.0 e deploy**. No cloud deploy; verify on local staging/dev only (reload staging volumes so fixture reloads).
- **FR-005**: No production Flyway rewrite; staging-only fixture edit is enough for local verification.

## Success Criteria

- **SC-001**: After clean staging reload, consent form text contains the researcher as controller and does not contain UFCG CNPJ in the controller line.
- **SC-002**: CEP md section VIII matches the fixture decision.
- **SC-003**: ROADMAP lists 028 Complete at 0.28.0; 1.0 still gated.
