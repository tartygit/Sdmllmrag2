# Software Development Document Environment (SDDE)

Enterprise Software Governance, Document Lifecycle Workflow & Local AI RAG Portal.

---

## 1. Architectural Architecture Design
SDDE is engineered based on **Clean Architecture, Domain-Driven Design (DDD), and SOLID design principles**.
- **Ingestion Pipeline**: ClamAV malware scanner inspects uploads, MinIO/S3 handles file storage, and RabbitMQ schedules asynchronous jobs.
- **AI Local RAG Engine**: High-fidelity extraction powered by Docling, FAISS vector index store, and Ollama (defaulting to `llama3.2:latest` & `nomic-embed-text`).
- **Maker-Checker-Approver workflows**: Strict state-machine logic transitioning uploaded documents through sequential statuses.

---

## 2. Advanced Integration Modules

### 2.1. Unified Notification Center
Unified service (`NotificationService.java`) providing robust:
- **Email Delivery**: Integrates JavaMailSender.
- **SMS Gateway Alerts**: Toggleable text notifications.
- **In-App Logging Alerts**: Dashboard notifications queue.

### 2.2. Distributed OpenTelemetry Tracing
Observed metrics tracing configurations (`OpenTelemetryConfig.java`) providing observability trace span log outputs to Promtheus.

### 2.3. Multi-Language Support (i18n)
Supports internationalization with custom property file resources:
- English: `messages.properties`
- French: `messages_fr.properties`

---

## 3. Deployment & Automation Scripts
- Run `./build.sh` to compile backend packages and frontend Vite chunks.
- Run `./start.sh` to spin up PostgreSQL, MinIO, Redis, RabbitMQ, and Ollama containers.
- Run `./test.sh` to execute JUnit test cases completely offline.
