-- V3 Add document_number column to documents table
-- Support Platforms: PostgreSQL, Oracle, MS SQL Server

ALTER TABLE documents ADD document_number VARCHAR(100);
CREATE UNIQUE INDEX idx_doc_number ON documents(document_number);
