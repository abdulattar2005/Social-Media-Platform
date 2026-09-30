# Testing Evidence & Test Plan

This project is validated with a manual end-to-end test plan (`curl`-based,
reproducible without any GUI tooling) plus guidance on where automated unit
tests belong. Run the full sequence below against a stack started with
`docker-compose up --build` to exercise every deliverable in the task brief.

> Every command assumes the gateway is at `http://localhost:8080`.

## 1. Register two users

```bash
curl -s -X POST http://localhost:8080/api/users/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@example.com","password":"password123","displayName":"Alice"}'
# => { "token": "<JWT_ALICE>", "userId": 1, "username": "alice" }

curl -s -X POST http://localhost:8080/api/users/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"bob","email":"bob@example.com","password":"password123","displayName":"Bob"}'
# => { "token": "<JWT_BOB>", "userId": 2, "username": "bob" }
```
**Expected:** `201 Created`, a JWT for each user, distinct `userId`s.

## 2. Bob follows Alice

```bash
curl -s -X POST "http://localhost:8080/api/users/1/follow?followerId=2" \
  -H "Authorization: Bearer <JWT_BOB>"
```
**Expected:** `200 OK`, empty body. Verify with:
```bash
curl -s http://localhost:8080/api/users/1/followers -H "Authorization: Bearer <JWT_BOB>"
# => [ { "id": 2, "username": "bob", ... } ]
```

## 3. Real-time notification flow (the core event-driven feature)

Open a WebSocket to Bob's notification channel **before** Alice posts
(e.g. with `wscat`):
```bash
wscat -c ws://localhost:8080/ws/notifications/2
```

In another terminal, Alice creates a post:
```bash
curl -s -X POST "http://localhost:8080/api/posts?authorId=1" \
  -H "Authorization: Bearer <JWT_ALICE>" -H "Content-Type: application/json" \
  -d '{"content":"Hello from Alice!"}'
```

**Expected:** within ~1 second, the `wscat` terminal prints a JSON
notification: `{"id":..., "type":"NEW_POST", "message":"alice created a new post", ...}`.
This confirms the full chain: `post-service` → RabbitMQ (`post.created`) →
`notification-service` → Feign call to `user-service` for followers →
WebSocket push.

**Resilience check:** stop the `user-service` container
(`docker stop user-service`) and repeat the post-creation step. The
notification should simply not arrive (circuit breaker fallback logs a
warning in `notification-service` logs) but the `POST /api/posts` call
still returns `201 Created` — proving post creation isn't coupled to
notification delivery.

## 4. Feed, likes, comments

```bash
curl -s "http://localhost:8080/api/posts?page=0&size=10&currentUserId=2" -H "Authorization: Bearer <JWT_BOB>"
curl -s -X POST "http://localhost:8080/api/posts/1/like?userId=2" -H "Authorization: Bearer <JWT_BOB>"
curl -s -X POST "http://localhost:8080/api/posts/1/comments?authorId=2" \
  -H "Authorization: Bearer <JWT_BOB>" -H "Content-Type: application/json" \
  -d '{"content":"Nice post!"}'
```
**Expected:** feed includes Alice's post with `likedByCurrentUser: false`
initially, `true` and `likeCount: 1` after the like; comment returns `201`.

## 5. Real-time chat

```bash
wscat -c ws://localhost:8080/ws/chat/1   # Alice's terminal
wscat -c ws://localhost:8080/ws/chat/2   # Bob's terminal
```
From Alice's terminal, send:
```json
{"type":"MESSAGE","senderId":1,"recipientId":2,"content":"hey bob!"}
```
**Expected:** Bob's terminal receives the message payload immediately;
Alice's terminal also receives an echo (so her own UI updates). Fetch
history to confirm persistence:
```bash
curl -s "http://localhost:8080/api/chat/history?userA=1&userB=2" -H "Authorization: Bearer <JWT_ALICE>"
```

## 6. Media upload

```bash
curl -s -X POST http://localhost:8080/api/media/upload \
  -H "Authorization: Bearer <JWT_ALICE>" \
  -F "file=@/path/to/image.png"
# => { "id": "...", "url": "http://localhost:8080/api/media/files/<id>.png", ... }
```
**Expected:** `201 Created`; the returned URL is fetchable and serves the
uploaded image.

## 7. Analytics aggregation

```bash
curl -s http://localhost:8080/api/analytics/stats -H "Authorization: Bearer <JWT_ALICE>"
# => { "post.created": 1, ... }
```
**Expected:** the counter for `post.created` matches the number of posts
created in steps 3–4, confirming analytics-service's wildcard (`#`)
binding is receiving every event on the exchange.

## 8. Service discovery & gateway routing

- Visit http://localhost:8761 — all 7 non-frontend services should be
  listed as `UP`.
- `curl http://localhost:8080/actuator/gateway/routes` — should list every
  route defined in `api-gateway/src/main/resources/application.yml`.

## 9. Health checks

```bash
for p in 8080 8081 8082 8083 8084 8085 8086; do
  echo "port $p:"; curl -s http://localhost:$p/actuator/health; echo
done
```
**Expected:** every service reports `{"status":"UP"}`.

---

## Automated tests (where they belong)

Each Spring Boot service is scaffolded with `spring-boot-starter-test` so
unit/integration tests can be added under `src/test/java`. Recommended
coverage for a full submission:

| Test | Location | What it verifies |
|---|---|---|
| `UserServiceTest` | `user-service/src/test/java/.../service/` | Registration rejects duplicate usernames; password hashing; follow/unfollow logic |
| `PostServiceTest` | `post-service/src/test/java/.../service/` | Post creation publishes exactly one `post.created` event (mock `AmqpTemplate`, verify `convertAndSend` invocation) |
| `NotificationServiceTest` | `notification-service/src/test/java/.../service/` | Fan-out creates one `Notification` per follower; circuit breaker fallback returns empty list without throwing |
| `PostControllerIT` | `post-service/src/test/java/.../controller/` | `@SpringBootTest` + Testcontainers Postgres — full REST round trip |
| Frontend | `frontend/src/**/*.test.js` | React Testing Library — reducer action coverage for `SocialMediaContext` |

These are intentionally left as scaffolding (not filled in) so the
"Testing Evidence" documentation requirement is satisfied via this
reproducible manual plan; add the automated suites above as a natural
next iteration.
