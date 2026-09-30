# Setup Instructions

## Option A — Docker Compose (recommended, fastest)

### Prerequisites
- Docker Desktop / Docker Engine 24+ and Docker Compose v2
- ~4 GB free RAM for the full stack (8 services + Postgres + RabbitMQ)

### Steps
```bash
git clone <repo-url> social-media-platform
cd social-media-platform
docker-compose up --build
```

Compose brings services up in dependency order (Postgres and RabbitMQ are
health-checked before any Spring Boot service starts, and each service
waits on `service-discovery` being healthy before registering).

First boot takes a few minutes because every service builds its own Maven
image from source. Subsequent `docker-compose up` runs are much faster
(layer caching).

### Verify everything is up
| Check | URL |
|---|---|
| Eureka dashboard — should list 7 registered instances (gateway + 6 services) | http://localhost:8761 |
| API Gateway health | http://localhost:8080/actuator/health |
| RabbitMQ management (guest/guest) | http://localhost:15672 |
| Frontend | http://localhost:3000 |

### Stopping / resetting
```bash
docker-compose down          # stop containers, keep data volumes
docker-compose down -v       # stop and wipe Postgres/media volumes too
```

---

## Option B — Run services locally without Docker (development)

You'll need: **Java 17**, **Maven 3.9+**, **Node.js 20+**, a local
**PostgreSQL 16** instance, and a local **RabbitMQ 3.13** instance.

1. Create the five databases:
   ```sql
   CREATE DATABASE userdb;
   CREATE DATABASE postdb;
   CREATE DATABASE notificationdb;
   CREATE DATABASE chatdb;
   CREATE DATABASE analyticsdb;
   ```
2. Start RabbitMQ (default guest/guest credentials on localhost:5672 work
   out of the box with each service's default `application.yml`).
3. Start services **in this order** (each needs the previous one available):
   ```bash
   cd service-discovery && mvn spring-boot:run          # wait for it to report "started"
   cd api-gateway        && mvn spring-boot:run
   cd user-service       && mvn spring-boot:run
   cd post-service       && mvn spring-boot:run
   cd notification-service && mvn spring-boot:run
   cd chat-service       && mvn spring-boot:run
   cd media-service      && mvn spring-boot:run
   cd analytics-service  && mvn spring-boot:run
   ```
4. Start the frontend:
   ```bash
   cd frontend
   cp .env.example .env
   npm install
   npm start
   ```
5. Open http://localhost:3000.

---

## Option C — Kubernetes (production-style deployment)

### Prerequisites
- A Kubernetes cluster (minikube, kind, or a managed cluster)
- `kubectl` and (optionally) `kustomize` (bundled with recent `kubectl`)
- An image registry the cluster can pull from, or `minikube image load` /
  `kind load docker-image` for local clusters

### Steps
```bash
# 1. Build and tag every service image (repeat per service, or script it)
docker build -t social-media-platform/user-service:latest ./user-service
docker build -t social-media-platform/post-service:latest ./post-service
# ... (same for the remaining services + frontend)

# 2. (local clusters only) load images into the cluster's runtime
minikube image load social-media-platform/user-service:latest
# ... repeat for each image

# 3. Apply all manifests via Kustomize
kubectl apply -k kubernetes/

# 4. Watch rollout
kubectl -n social-media-platform get pods -w

# 5. Access the platform
kubectl -n social-media-platform port-forward svc/frontend 3000:3000
kubectl -n social-media-platform port-forward svc/api-gateway 8080:8080
```

For a real cluster with an Ingress controller installed, use
`kubernetes/ingress.yaml` instead of port-forwarding and point DNS (or
`/etc/hosts`) at the ingress IP for `social-media-platform.local`.

`kubernetes/secrets.yaml` ships with placeholder credentials for
demonstration only — replace it with a Sealed Secret / Vault-injected
secret before deploying anywhere real.

---

## Environment variables reference

| Variable | Used by | Default | Purpose |
|---|---|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | every JPA service | `localhost:5432` / per-service db / `postgres`/`postgres` | Postgres connection |
| `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD` | post/notification/media/analytics services | `localhost:5672` / `guest`/`guest` | Broker connection |
| `EUREKA_HOST` | every service | `localhost` | Service discovery host |
| `MEDIA_STORAGE_PATH` | media-service | `/tmp/social-media-uploads` | Where uploaded files are written |
| `MEDIA_BASE_URL` | media-service | `http://localhost:8085/files` | Public URL prefix returned to clients |
| `REACT_APP_API_BASE_URL` | frontend | `http://localhost:8080/api` | Gateway base URL |
| `REACT_APP_WS_NOTIFICATIONS_URL` | frontend | `ws://localhost:8080/ws/notifications` | Notification WS base |
| `REACT_APP_WS_CHAT_URL` | frontend | `ws://localhost:8080/ws/chat` | Chat WS base |

## Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| A service fails to start with a Eureka connection error | `service-discovery` isn't up yet | Docker Compose handles ordering automatically; locally, start `service-discovery` first and wait for "Started ServiceDiscoveryApplication" |
| 401 on every gateway request | Missing/expired JWT | Log in again via `/api/users/auth/login`; token expires after 24h |
| Notifications never arrive | RabbitMQ not reachable from post-service/notification-service, or the circuit breaker is open | Check RabbitMQ management UI for the `notification.queue`; check `/actuator/circuitbreakers` on both services |
| `docker-compose up` is slow on first run | Every service builds from source inside its own Maven image | Expected; subsequent runs use Docker layer cache |
| Frontend shows CORS errors | Frontend not using the gateway's CORS-enabled origin | Confirm `REACT_APP_API_BASE_URL` points at `:8080`, not directly at a service port |
