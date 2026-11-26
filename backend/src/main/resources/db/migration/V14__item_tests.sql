-- Spec 040: per-UC manual test suite texts (Before/After) on each item.
ALTER TABLE item
    ADD COLUMN origin_tests MEDIUMTEXT NOT NULL AFTER target_usecase,
    ADD COLUMN target_tests MEDIUMTEXT NOT NULL AFTER origin_tests;
