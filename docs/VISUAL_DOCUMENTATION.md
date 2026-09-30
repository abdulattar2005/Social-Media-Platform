# Visual Documentation

This file is the checklist for the screenshot set that should accompany a
submission (the task's "Visual Documentation" requirement: *"Screenshots
demonstrating functionality"*). Capture each of these once the stack is
running via `docker-compose up --build` (see `docs/SETUP.md`), and drop the
image files into `docs/screenshots/`, linking them below.

## Required screenshots

1. **Eureka dashboard** (`http://localhost:8761`) showing all 7 backend
   instances registered as `UP`.
2. **RabbitMQ management UI** (`http://localhost:15672`) showing
   `notification.queue` and `analytics.queue` bound to `social-media.exchange`.
3. **Registration / login screen** of the React frontend.
4. **Feed page** with at least one created post, showing like count and
   the comment box expanded.
5. **Real-time notification** arriving in the Notifications page
   immediately after a followed user posts (two browser windows side by
   side is the clearest way to demonstrate this).
6. **Chat window** with an active conversation, showing the typing
   indicator and the online/offline presence dot.
7. **Docker Compose** terminal output showing all containers healthy
   (`docker-compose ps`).
8. **Kubernetes** `kubectl -n social-media-platform get pods` output
   showing every deployment's pods `Running` (if the K8s path was tested).

## Suggested folder layout once captured

```
docs/screenshots/
├── 01-eureka-dashboard.png
├── 02-rabbitmq-queues.png
├── 03-login.png
├── 04-feed.png
├── 05-realtime-notification.png
├── 06-chat.png
├── 07-docker-compose-ps.png
└── 08-k8s-pods.png
```

Then embed them here, e.g.:
```markdown
![Eureka dashboard](screenshots/01-eureka-dashboard.png)
```

This file is intentionally a checklist rather than pre-filled images,
since screenshots must be captured from an actual running deployment on
the machine/cluster being used for submission.
