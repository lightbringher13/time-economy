# TimeEconomy Platform (Monorepo)

This repository contains the full source code for the **TimeEconomy** platform,
including backend services, frontend applications, infrastructure configuration,
and documentation.  
It follows a **monorepo architecture**, similar to structures used by large-scale
organizations (Netflix, Uber, Kakao, Coupang).

---

## 📁 Repository Structure

```
.
├── backend/        # Spring Boot microservices (Auth, User, etc.)
├── frontend/       # React + Vite web application
├── infra/          # Docker Compose, DB, Redis, Traefik, local dev stack
├── docs/           # Architecture diagrams, ADRs, specs, API docs
└── README.md       # You are here
```

---

## 🚀 Tech Stack

### **Backend**
- Java 21+
- Spring Boot 3.x
- Spring Security (JWT + Refresh Token)
- Hexagonal / Clean Architecture
- JPA + Hibernate
- PostgreSQL
- Redis (session + token store)
- Gradle (KTS)

### **Frontend**
- React 18
- Vite
- TypeScript
- Zustand (global state)
- Axios
- React Router v6

### **Infrastructure**
- Docker & Docker Compose
- Traefik (local reverse proxy)
- PostgreSQL, Redis
- (Later) Kubernetes for production

---

## 🧱 Architecture Overview

The platform will evolve toward a modular or microservice architecture:

```
backend/
 ├── auth-service/        # Login, JWT, refresh tokens, sessions
 ├── user-service/        # Profiles, password changes, nickname updates
 └── shared-kernel/       # Shared domain modules (if needed)

frontend/
 └── timeeconomy-web/     # React web app
```

A unified local development environment is provided via `infra/docker-compose.yml`.

---

## 🛠️ Local Development

### **1. Clone the repository**
```bash
git clone https://github.com/<your-name>/timeeconomy.git
cd timeeconomy
```

### **2. Start local infra**
(PostgreSQL, Redis, Traefik)
```bash
cd infra
docker compose up -d
```

### **3. Start backend**
```bash
cd backend/auth-service
./gradlew bootRun
```

### **4. Start frontend**
```bash
cd frontend
npm install
npm run dev
```

### **5. Optional local SMS inbox**

The mock SMS inbox is opt-in, bound to the loopback interface, and never routed
through the API Gateway. Start Tilt with a local token of at least 24 characters:

```bash
export SMS_INBOX_TOKEN="$(openssl rand -hex 32)"
tilt up -- --sms_inbox
```

After requesting a phone verification code, list the available message metadata:

```bash
curl -sS \
  -H "X-Local-Sms-Inbox-Token: ${SMS_INBOX_TOKEN}" \
  http://127.0.0.1:8086/_local/sms-inbox
```

Match the `challengeId` returned by the corresponding `send-otp` request with
the metadata response, then consume that OTP exactly once:

```bash
curl -sS -X POST \
  -H "X-Local-Sms-Inbox-Token: ${SMS_INBOX_TOKEN}" \
  http://127.0.0.1:8086/_local/sms-inbox/CHALLENGE_ID/consume
```

The inbox is in memory, bounded, TTL-aware, and unavailable unless the
`sms-inbox`, `sms-mock`, and local Docker profiles are active together.

---

## ✔️ Code Style & Conventions

- **Backend** uses Clean Architecture (ports & adapters).
- **Frontend** uses feature-based structure (`features/auth`, `features/user`).
- **All services** must return consistent API responses:
  - Success: `200` or `204`
  - Error: `{ "code": "...", "message": "..." }`

---

## 🧪 Testing

### Backend
```bash
./gradlew test
```

### Frontend
```bash
npm run test
```

---

## 📦 Deployment (CI/CD)

Later we will add:
- GitHub Actions for build & test
- Docker image builds
- Deploy to AWS ECS / EKS or GCP GKE
- Automated database migrations (Flyway/Liquibase)

---

## 📝 Documentation

All architecture diagrams, ADRs, and API docs live in the `docs/` folder.

---

## 📄 License

Private project — All rights reserved.

---

If you want, I can also generate:

### ✔ architecture diagrams  
### ✔ API documentation  
### ✔ ADR (Architecture Decision Records)  
### ✔ CI/CD GitHub Actions YAML  
### ✔ Docker Compose YAML for dev stack  

Just tell me:  
👉 **“Generate infra/docker-compose.yml”**  
or  
👉 **“Create the auth-service folder structure”**
