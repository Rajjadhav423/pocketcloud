ALTER TABLE deployments
    ADD COLUMN app_type      VARCHAR(20),   -- STATIC or NODE, known after framework detection
    ADD COLUMN start_command TEXT,          -- e.g. "npm start"; NULL for static sites
    ADD COLUMN artifact_path TEXT,          -- folder the runtime will serve or run from
    ADD COLUMN started_at    TIMESTAMPTZ,
    ADD COLUMN finished_at   TIMESTAMPTZ;
