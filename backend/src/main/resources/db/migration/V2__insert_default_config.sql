-- Default study configuration (FR-005); no consent form is published here.
INSERT INTO config (id, admin_email, admin_preferred_language, reservation_minutes, block_size, daily_email_limit,
                    study_phase, study_phase_changed_at, updated_at)
VALUES (1, 'diegoquirino@gmail.com', NULL, 20, 10, 450, 'pilot', NULL, UTC_TIMESTAMP(6));
