CREATE TABLE projects (
    id          UUID PRIMARY KEY,
    name        VARCHAR(40)  NOT NULL UNIQUE,   -- becomes the subdomain: <name>.yourdomain.com
    repo_url    TEXT         NOT NULL,
    branch      VARCHAR(100) NOT NULL DEFAULT 'main',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE deployments (
    id          UUID PRIMARY KEY,
    project_id  UUID         NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    status      VARCHAR(20)  NOT NULL,          -- QUEUED, BUILDING, READY, FAILED
    commit_sha  VARCHAR(40),
    error       TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Build workers will poll for QUEUED jobs; this keeps that query fast.
CREATE INDEX idx_deployments_status_created ON deployments (status, created_at);
CREATE INDEX idx_deployments_project ON deployments (project_id, created_at DESC);
