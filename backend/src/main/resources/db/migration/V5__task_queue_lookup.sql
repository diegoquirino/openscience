-- Spec 007: speed candidate scans for reserve under FOR UPDATE SKIP LOCKED.
CREATE INDEX ix_task_status_queue ON task (status, queue_position);
