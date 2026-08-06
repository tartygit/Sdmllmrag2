-- V2 Security Additions (Failed attempts, Account Locking, MFA, Password Policies)
-- Support Platforms: PostgreSQL, Oracle, MS SQL Server

-- 1. Alter users table to add security policy and MFA support
ALTER TABLE users ADD failed_attempts INTEGER DEFAULT 0 NOT NULL;
ALTER TABLE users ADD lock_time TIMESTAMP;
ALTER TABLE users ADD password_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL;
ALTER TABLE users ADD mfa_secret VARCHAR(100);
ALTER TABLE users ADD mfa_enabled BOOLEAN DEFAULT FALSE NOT NULL;

-- 2. Pre-seed standard Role values
INSERT INTO roles (name, description) VALUES ('ROLE_ADMIN', 'System Administrator with full access');
INSERT INTO roles (name, description) VALUES ('ROLE_MAKER', 'Document creator or editor');
INSERT INTO roles (name, description) VALUES ('ROLE_CHECKER', 'Document reviewer or checker');
INSERT INTO roles (name, description) VALUES ('ROLE_APPROVER', 'Document approver');
INSERT INTO roles (name, description) VALUES ('ROLE_VIEWER', 'ReadOnly viewer');
INSERT INTO roles (name, description) VALUES ('ROLE_AUDITOR', 'Security or Compliance Auditor');

-- 3. Pre-seed default Admin User (Password: "AdminPassword123!")
-- BCrypt Hash: $2a$10$tM2e9.73fF1626jGf9NfHOKO6xG0E8tE7sR7jA.pM.j7t9C9lQ58W
INSERT INTO users (username, password, email, full_name, enabled, failed_attempts, password_updated_at, mfa_enabled)
VALUES ('admin', '$2a$10$tM2e9.73fF1626jGf9NfHOKO6xG0E8tE7sR7jA.pM.j7t9C9lQ58W', 'admin@cth.sdm', 'System Administrator', TRUE, 0, CURRENT_TIMESTAMP, FALSE);

-- 4. Associate admin user with ROLE_ADMIN
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN';
