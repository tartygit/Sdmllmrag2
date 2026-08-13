# Software Development Document Environment (SDDE) - Architecture Design Document

## 1. Introduction & Executive Summary
The Software Development Document Environment (SDDE) is an enterprise-grade, high-availability Document Management and Governance System. It manages software engineering documents (e.g., specifications, architectural designs, compliance checklists) through a structured, multi-phase Software Development Lifecycle (SDLC).

SDDE features:
- **Pluggable Async Ingestion Pipeline**: Monitors directories, SFTP, REST, and Message Queues to automatically ingest documents (Word, Excel, PPT, PDF, XML, ZIP, etc.).
- **Antivirus Scanning**: Integrates ClamAV to inspect uploaded files.
- **Asynchronous Document Processing**: RabbitMQ distributes load.
- **High-Fidelity Document Parsing**: Driven by Python-based `Docling` microservice.
- **AI Core (Local RAG)**: Powered by local LLMs via Ollama (e.g. `llama3.2` and `nomic-embed-text`) and FAISS vector indices, producing instant summaries, quality/compliance scoring, and streaming semantic search.
- **State-Machine Workflow Engine**: Implements rigid Maker-Checker-Approver workflows.
- **Dynamic SDLC Mapping**: Displays documents over a customizable 7-phase lifecycle.
- **Audit Logging**: Captures comprehensive system audits (timestamp, username, IP, operation, old value, new value).

---

## 2. Technology Stack
- **Backend**: Java 21, Spring Boot 3.3.x, Spring Security (JWT, LDAP, MFA), Spring Data JPA, Liquibase (Database Migrations), MapStruct, Lombok, RabbitMQ (Messaging), MinIO/S3 (Object Storage), Redis (Caching), Prometheus & Grafana (Monitoring).
- **AI/RAG Service**: Python 3.12, Gunicorn, Flask, Docling, LangChain, FAISS, Ollama.
- **Frontend**: React 18, Vite, TypeScript, Tailwind CSS, Material Design UI.
- **Infrastructure**: Docker, Docker Compose, ClamAV, PostgreSQL (Primary/Configurable to Oracle & SQL Server).
