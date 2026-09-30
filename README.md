# Social-Media-Platform

# 1. Project Overview

SocialSphere is a full, working reference implementation of a microservices-based social network. A user can register/log in, publish posts with optional media, like and comment on posts, follow other users, receive real-time notifications when someone they follow posts, and exchange real-time direct messages — all served through a single API Gateway backed by independently deployable Spring Boot services.

The goal of this project (per the task brief) was to practice and demonstrate:

Microservices architecture patterns
React advanced state management (Context API + useReducer)
API gateway and service discovery (Spring Cloud Gateway + Netflix Eureka)
Message brokers and event-driven architecture (RabbitMQ)
Containerization with Docker (and Kubernetes for production deployment)
Cloud-native development practices (health checks, circuit breakers, horizontal scaling)

# 2. Architecture at a glance

                         ┌─────────────┐
                         │   Frontend   │  React 18 + Context API + WebSocket
                         │  (port 3000) │
                         └──────┬───────┘
                                │ HTTPS / WSS
                         ┌──────▼───────┐
                         │  API Gateway  │  Spring Cloud Gateway, JWT validation,
                         │  (port 8080)  │  routing, StripPrefix, rate limiting
                         └──────┬───────┘
                                │ load-balanced via Eureka
        ┌───────┬──────────────┼───────────────┬───────────┬────────────┐
        ▼        ▼              ▼               ▼           ▼            ▼
   ┌────────┐┌─────────┐ ┌──────────────┐ ┌───────────┐┌──────────┐┌────────────┐
   │  User  ││  Post   │ │ Notification │ │   Chat    ││  Media   ││ Analytics  │
   │Service ││ Service │ │   Service    │ │  Service  ││ Service  ││  Service   │
   │ :8081  ││  :8082  │ │    :8083     │ │   :8084   ││  :8085   ││   :8086    │
   └───┬────┘└────┬────┘ └──────┬───────┘ └─────┬─────┘└──────────┘└─────┬──────┘
       │          │  post.created│  WS push      │ WS chat                │ all events
       │          └─────────────►│◄──────────────┘                       │ (routing key #)
       │                          RabbitMQ  (social-media.exchange) ◄─────┘
       │
       └── Postgres (userdb) ── each service owns its own database (postdb, notificationdb, chatdb, analyticsdb)

                  All services register with:  Eureka Service Discovery (:8761)

See docs/ARCHITECTURE.md for the full breakdown, sequence diagrams, and design rationale (CAP theorem trade-offs, eventual consistency, resilience patterns).

# 3. Tech stack
Layer	Technology
Frontend	React 18, React Router, Context API + useReducer, native WebSocket, Axios
API Gateway	Spring Cloud Gateway, JJWT (JWT validation)
Service Discovery	Netflix Eureka
Microservices	Spring Boot 3.2, Spring Data JPA, Spring Security, Spring WebSocket
Messaging	RabbitMQ (topic exchange, event-driven fan-out)
Resilience	Resilience4j (circuit breakers)
Data	PostgreSQL (database-per-service)
Containerization	Docker, Docker Compose
Orchestration	Kubernetes manifests + HorizontalPodAutoscaler
Monitoring	Spring Boot Actuator, Prometheus scrape config

# 4. Repository structure
social-media-platform/
├── README.md                  ← you are here
├── docker-compose.yml          Full local stack
├── docs/                       Architecture, API contracts, setup, testing docs
├── scripts/init-db.sql         Creates one database per service
├── frontend/                   React application
├── api-gateway/                Spring Cloud Gateway
├── service-discovery/          Eureka server
├── user-service/                User management microservice
├── post-service/                Post management microservice
├── notification-service/        Notification microservice
├── chat-service/                 Real-time chat microservice
├── media-service/                 Media microservice
├── analytics-service/             Analytics microservice
├── kubernetes/                    K8s manifests (Deployments, Services, HPA, Ingress)
└── monitoring/                    Prometheus scrape config

Full annotated tree: docs/CODE_STRUCTURE.md.

# 5. Quick start (Docker Compose — recommended)
bash
git clone <this-repo-url> social-media-platform
cd social-media-platform
docker-compose up --build

Then open:

Frontend: http://localhost:3000
API Gateway: http://localhost:8080
Eureka dashboard: http://localhost:8761
RabbitMQ management UI: http://localhost:15672 (guest/guest)

Full step-by-step instructions, environment variables, and troubleshooting: docs/SETUP.md.

# 6. API contracts & message events

All REST endpoints (grouped by service) and every RabbitMQ event schema (post.created, user.followed, message.sent, notification.created, media.uploaded, analytics.event) are documented in docs/API_CONTRACTS.md.

# 7. Testing

Manual test plan, sample curl requests, and a Postman-style walkthrough covering every user flow (register → post → follow → real-time notify → chat → like/comment) are in docs/TESTING.md.

# 8. Deliverables checklist
 Complete microservices architecture (6 business services + gateway + discovery)
 React frontend with real-time features (WebSocket notifications & chat)
 6+ Spring Boot microservices
 API Gateway implementation (Spring Cloud Gateway, JWT-secured)
 Service discovery setup (Eureka)
 Message queue integration (RabbitMQ, topic exchange, 3 consumers)
 Docker containerization (per-service Dockerfile + docker-compose.yml)
 Distributed tracing groundwork (correlation via Actuator; see docs/ARCHITECTURE.md)
 Resilience patterns (Resilience4j circuit breakers on cross-service calls)
 Cloud deployment ready (Kubernetes manifests + HPA + Ingress)
 
# 9. Weekly build log (per the task's step-by-step guide)
Week	Focus	Status
1	Architecture & Setup — design microservices, set up Spring Cloud components	✅
2	Core Services — implement user, post, media services	✅
3	Frontend Integration — build React frontend with state management	✅
4	Real-time Features — add WebSocket chat, notifications	✅
5	Resilience & Observability — add circuit breakers, logging, tracing	✅
6	Containerization & Deployment — Dockerize services, deploy to cloud	✅

# 10. Notes on scope & production hardening

This is a learning/portfolio-grade implementation, built to satisfy the task's technical requirements end-to-end and to actually run via docker-compose up. Before real production use you would additionally want: JWT refresh tokens, rate limiting at the gateway, object storage (S3) instead of local disk for media, distributed tracing (Zipkin/Jaeger via Micrometer Tracing), centralized log aggregation (ELK/Loki), database read replicas for analytics, and secrets management (Vault/Sealed Secrets) instead of the plain secrets.yaml included here for demonstration.
