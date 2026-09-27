-- S0 baseline: tenancy + audit + async operations
-- Use TIMESTAMP WITH TIME ZONE (not TIMESTAMPTZ alias) for H2 + PostgreSQL compatibility in tests.
CREATE TABLE organizations (
    id              UUID PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(100) NOT NULL,
    status          VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_organizations_slug UNIQUE (slug)
);

CREATE TABLE users (
    id              UUID PRIMARY KEY,
    display_name    VARCHAR(255) NOT NULL,
    email           VARCHAR(320) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE organization_users (
    id               UUID PRIMARY KEY,
    organization_id  UUID NOT NULL REFERENCES organizations(id),
    user_id          UUID NOT NULL REFERENCES users(id),
    role             VARCHAR(64) NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_org_user UNIQUE (organization_id, user_id)
);

CREATE TABLE audit_events (
    id               UUID PRIMARY KEY,
    organization_id  UUID,
    actor_user_id    UUID,
    action           VARCHAR(128) NOT NULL,
    entity_type      VARCHAR(128) NOT NULL,
    entity_id        VARCHAR(128),
    correlation_id   VARCHAR(64),
    old_value        TEXT,
    new_value        TEXT,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_events_org_created ON audit_events (organization_id, created_at DESC);

CREATE TABLE operations (
    id               UUID PRIMARY KEY,
    organization_id  UUID,
    type             VARCHAR(128) NOT NULL,
    status           VARCHAR(32)  NOT NULL,
    progress         INT NOT NULL DEFAULT 0,
    error_code       VARCHAR(128),
    error_message    TEXT,
    result_json      TEXT,
    correlation_id   VARCHAR(64),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at     TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_operations_status ON operations (status);
