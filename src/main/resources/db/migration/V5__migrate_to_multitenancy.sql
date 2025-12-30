-- Migration to Multitenancy
-- Robust script for Flyway

-- Extension creation (Flyway handles this in the transaction if the user has permissions)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Roles Table
CREATE TABLE IF NOT EXISTS roles
(
    name VARCHAR(50) PRIMARY KEY
);

INSERT INTO roles (name)
VALUES ('USER'),
       ('ADMIN'),
       ('EDITOR')
ON CONFLICT (name) DO NOTHING;

-- Tenants Table
CREATE TABLE IF NOT EXISTS tenants
(
    id         VARCHAR(255) PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO tenants (id, name)
VALUES ('default-tenant', 'Default Organization')
ON CONFLICT (id) DO NOTHING;

-- Modify Users Table
ALTER TABLE users ADD COLUMN IF NOT EXISTS new_id VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255);

-- Generate UUIDs for existing users (id is currently BIGSERIAL/BIGINT)
UPDATE users
SET new_id = gen_random_uuid()::VARCHAR,
    tenant_id = 'default-tenant'
WHERE new_id IS NULL;

-- Now apply constraints
ALTER TABLE users ALTER COLUMN new_id SET NOT NULL;
ALTER TABLE users ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT uk_users_new_id UNIQUE (new_id);

-- User_Roles Table
CREATE TABLE IF NOT EXISTS user_roles
(
    user_id   VARCHAR(255) NOT NULL,
    role_name VARCHAR(50)  NOT NULL,
    PRIMARY KEY (user_id, role_name),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_name) REFERENCES roles (name) ON DELETE CASCADE
);

-- Assign default USER role to existing users
INSERT INTO user_roles (user_id, role_name)
SELECT new_id, 'USER'
FROM users
ON CONFLICT DO NOTHING;

-- Migrate Documents
ALTER TABLE documents ADD COLUMN IF NOT EXISTS user_id_new VARCHAR(255);
UPDATE documents d
SET user_id_new = u.new_id
FROM users u
WHERE d.user_id = u.id AND d.user_id_new IS NULL;
ALTER TABLE documents ALTER COLUMN user_id_new SET NOT NULL;

-- Migrate Conversations
ALTER TABLE conversations ADD COLUMN IF NOT EXISTS user_id_new VARCHAR(255);
UPDATE conversations c
SET user_id_new = u.new_id
FROM users u
WHERE c.user_id = u.id AND c.user_id_new IS NULL;
ALTER TABLE conversations ALTER COLUMN user_id_new SET NOT NULL;

-- Drop old constraints (named in V1)
ALTER TABLE documents DROP CONSTRAINT IF EXISTS fk_documents_user;
ALTER TABLE conversations DROP CONSTRAINT IF EXISTS fk_conversations_user;

-- Drop old columns and swap to new ones
ALTER TABLE documents DROP COLUMN IF EXISTS user_id;
ALTER TABLE documents RENAME COLUMN user_id_new TO user_id;

ALTER TABLE conversations DROP COLUMN IF EXISTS user_id;
ALTER TABLE conversations RENAME COLUMN user_id_new TO user_id;

-- Update Users Table PK
-- This is the most sensitive part. We will drop the old BIGINT PK and set the new ID as PK.
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_pkey CASCADE;
ALTER TABLE users DROP COLUMN IF EXISTS id;
ALTER TABLE users RENAME COLUMN new_id TO id;
ALTER TABLE users ADD PRIMARY KEY (id);

-- Finish wiring the FKs
ALTER TABLE user_roles ADD CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
ALTER TABLE documents ADD CONSTRAINT fk_documents_user FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE conversations ADD CONSTRAINT fk_conversations_user FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE users ADD CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id);

-- Re-create Indices
CREATE INDEX IF NOT EXISTS idx_users_tenant_id ON users (tenant_id);
CREATE INDEX IF NOT EXISTS idx_documents_user_id ON documents (user_id);
CREATE INDEX IF NOT EXISTS idx_conversations_user_id ON conversations (user_id);


ALTER TABLE users ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
