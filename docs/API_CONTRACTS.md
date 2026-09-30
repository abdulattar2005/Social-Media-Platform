# API Contracts

All requests go through the **API Gateway** at `http://localhost:8080`.
The gateway strips `/api/<service>` before forwarding, so paths below are
shown both as the public gateway path and the internal service path.

Authenticated endpoints require `Authorization: Bearer <token>` — the
gateway validates the JWT and forwards the resolved username as
`X-Auth-User` to the downstream service.

## Gateway routing table

| Gateway path | Routed to | Notes |
|---|---|---|
| `/api/users/**` | `user-service` | `/auth/register`, `/auth/login` are public |
| `/api/posts/**` | `post-service` | JWT required |
| `/api/notifications/**` | `notification-service` | JWT required |
| `/ws/notifications/**` | `notification-service` (WebSocket) | Upgrade request |
| `/api/chat/**` | `chat-service` | JWT required |
| `/ws/chat/**` | `chat-service` (WebSocket) | Upgrade request |
| `/api/media/**` | `media-service` | JWT required |
| `/api/analytics/**` | `analytics-service` | JWT required |

---

## user-service

| Method | Gateway path | Internal path | Body / Params | Description |
|---|---|---|---|---|
| POST | `/api/users/auth/register` | `/auth/register` | `{username, email, password, displayName}` | Create account, returns JWT |
| POST | `/api/users/auth/login` | `/auth/login` | `{username, password}` | Returns JWT |
| GET | `/api/users/{id}` | `/{id}` | — | Get user by id |
| GET | `/api/users/username/{username}` | `/username/{username}` | — | Get user by username |
| GET | `/api/users/search?q=` | `/search?q=` | — | Search users |
| POST | `/api/users/{id}/follow?followerId=` | `/{id}/follow?followerId=` | — | Follow a user |
| DELETE | `/api/users/{id}/follow?followerId=` | `/{id}/follow?followerId=` | — | Unfollow a user |
| GET | `/api/users/{id}/followers` | `/{id}/followers` | — | Used internally by notification-service |
| GET | `/api/users/{id}/following` | `/{id}/following` | — | |

## post-service

| Method | Gateway path | Internal path | Body / Params | Description |
|---|---|---|---|---|
| POST | `/api/posts?authorId=` | `?authorId=` | `{content, mediaUrl}` | Create post → publishes `post.created` |
| GET | `/api/posts?page=&size=&currentUserId=` | same | — | Paginated feed |
| GET | `/api/posts/author/{authorId}` | `/author/{authorId}` | — | Posts by one author |
| POST | `/api/posts/{id}/like?userId=` | `/{id}/like?userId=` | — | Like a post |
| DELETE | `/api/posts/{id}/like?userId=` | `/{id}/like?userId=` | — | Unlike |
| POST | `/api/posts/{id}/comments?authorId=` | `/{id}/comments?authorId=` | `{content}` | Add comment |
| GET | `/api/posts/{id}/comments` | `/{id}/comments` | — | List comments |

## notification-service

| Method | Gateway path | Internal path | Description |
|---|---|---|---|
| GET | `/api/notifications/{userId}` | `/{userId}` | List notifications |
| GET | `/api/notifications/{userId}/unread-count` | `/{userId}/unread-count` | `{unread: n}` |
| PUT | `/api/notifications/{id}/read` | `/{id}/read` | Mark as read |
| WS | `/ws/notifications/{userId}` | — | Live push channel |

## chat-service

| Method | Gateway path | Internal path | Description |
|---|---|---|---|
| GET | `/api/chat/history?userA=&userB=` | `/history?userA=&userB=` | Conversation history |
| WS | `/ws/chat/{userId}` | — | Live chat channel |

**WebSocket chat frame shapes** (JSON, sent both directions):
```json
{ "type": "MESSAGE", "senderId": 1, "recipientId": 2, "content": "hey!" }
{ "type": "TYPING", "senderId": 1, "recipientId": 2 }
{ "type": "READ_RECEIPT", "senderId": 2, "recipientId": 1 }
{ "type": "PRESENCE", "userId": 1, "online": true }
```

## media-service

| Method | Gateway path | Internal path | Description |
|---|---|---|---|
| POST | `/api/media/upload` (multipart `file`) | `/upload` | Upload image/video, returns `{id, url, ...}` |
| GET | `/api/media/files/{filename}` | `/files/{filename}` | Serve stored file |

## analytics-service

| Method | Gateway path | Internal path | Description |
|---|---|---|---|
| GET | `/api/analytics/stats` | `/stats` | `{ "post.created": 42, "user.followed": 10, ... }` |

---

## RabbitMQ event contracts

All events are published to the **topic exchange** `social-media.exchange`.

| Routing key | Publisher | Consumer(s) | Payload |
|---|---|---|---|
| `post.created` | post-service | notification-service, analytics-service | `{postId, authorId, authorUsername, contentPreview, createdAt}` |
| `user.followed` | *(reserved — see docs/TESTING.md for how to extend)* | analytics-service | `{followerId, followingId}` |
| `message.sent` | *(reserved, chat is currently WS-only)* | analytics-service | `{senderId, recipientId}` |
| `notification.created` | *(reserved)* | analytics-service | `{userId, type}` |
| `media.uploaded` | *(reserved)* | analytics-service | `{mediaId, uploaderId}` |
| `analytics.event` | *(generic bucket)* | analytics-service | `{eventType, userId, occurredAt}` |

Only `post.created` is wired end-to-end in this implementation (matching
the task's own sample code); the other routing keys are documented and the
analytics-service queue is already bound with wildcard key `#`, so they
are consumed automatically the moment any other service starts publishing
them — no analytics-service changes needed.

**Queue/exchange topology:**

```
social-media.exchange (topic)
 ├── notification.queue  ← binding key "post.created"
 └── analytics.queue     ← binding key "#"  (everything)
```

## Error response shape

All services return errors in this shape (see `GlobalExceptionHandler` in
user-service, mirrored in the others):

```json
{
  "timestamp": "2026-09-20T10:15:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Username already taken"
}
```
