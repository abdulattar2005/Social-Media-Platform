# Code Structure

Every Spring Boot service follows the same layered package convention, so
once you understand one, you understand all six:

```
<service>/src/main/java/com/socialmedia/<service>/
├── <Service>Application.java   ← @SpringBootApplication entry point
├── controller/                 ← @RestController — HTTP request handling only
├── service/                    ← business logic, transactions, orchestration
├── repository/                 ← Spring Data JPA interfaces
├── model/                      ← @Entity JPA classes
├── dto/                        ← request/response shapes (never expose entities directly)
├── config/                     ← Spring @Configuration (security, RabbitMQ, WebSocket)
├── messaging/                  ← RabbitMQ event classes + @RabbitListener consumers
└── websocket/                  ← WebSocketHandler implementations (notification/chat only)
```

## Full repository tree

```
social-media-platform/
├── README.md
├── docker-compose.yml
├── docs/
│   ├── ARCHITECTURE.md
│   ├── API_CONTRACTS.md
│   ├── SETUP.md
│   ├── TESTING.md
│   └── CODE_STRUCTURE.md          ← this file
├── scripts/
│   └── init-db.sql                 creates userdb/postdb/notificationdb/chatdb/analyticsdb
│
├── service-discovery/               Eureka Server — port 8761
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/socialmedia/servicediscovery/
│       │   └── ServiceDiscoveryApplication.java
│       └── resources/application.yml
│
├── api-gateway/                      Spring Cloud Gateway — port 8080
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/socialmedia/apigateway/
│       │   ├── ApiGatewayApplication.java
│       │   └── config/JwtAuthenticationFilter.java   ← validates JWT, injects X-Auth-User
│       └── resources/application.yml                  ← route table (see docs/API_CONTRACTS.md)
│
├── user-service/                     port 8081 — userdb
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/socialmedia/userservice/
│       │   ├── UserServiceApplication.java
│       │   ├── controller/  AuthController.java, UserController.java
│       │   ├── service/     UserService.java
│       │   ├── repository/  UserRepository.java, FollowRepository.java
│       │   ├── model/       User.java, Follow.java
│       │   ├── dto/         RegisterRequest, LoginRequest, AuthResponse, UserDTO
│       │   ├── config/      JwtService.java (issues tokens), SecurityConfig.java
│       │   └── exception/   ResourceNotFoundException, BadRequestException, GlobalExceptionHandler
│       └── resources/       application.yml, application-docker.yml
│
├── post-service/                     port 8082 — postdb
│   └── src/main/java/com/socialmedia/postservice/
│       ├── PostServiceApplication.java
│       ├── controller/PostController.java
│       ├── service/PostService.java              ← publishes post.created (circuit-breaker guarded)
│       ├── repository/  PostRepository.java, CommentRepository.java
│       ├── model/       Post.java, Comment.java
│       ├── dto/         CreatePostRequest, CommentRequest, PostDTO
│       ├── messaging/PostCreatedEvent.java        ← event schema published to RabbitMQ
│       └── config/RabbitMQConfig.java             ← declares social-media.exchange
│
├── notification-service/             port 8083 — notificationdb
│   └── src/main/java/com/socialmedia/notificationservice/
│       ├── NotificationServiceApplication.java
│       ├── controller/NotificationController.java
│       ├── service/
│       │   ├── NotificationService.java           ← fan-out logic, circuit-breaker fallback
│       │   └── UserServiceClient.java             ← Feign client → user-service (via Eureka)
│       ├── messaging/
│       │   ├── PostCreatedEvent.java              ← mirrors post-service's event schema
│       │   └── PostCreatedEventConsumer.java       ← @RabbitListener
│       ├── websocket/NotificationWebSocketHandler.java  ← per-user session registry + push
│       ├── repository/NotificationRepository.java
│       ├── model/Notification.java
│       ├── dto/  NotificationDTO, UserDTO (Feign response shape)
│       └── config/ RabbitMQConfig.java, WebSocketConfig.java
│
├── chat-service/                     port 8084 — chatdb
│   └── src/main/java/com/socialmedia/chatservice/
│       ├── ChatServiceApplication.java
│       ├── controller/ChatController.java         ← REST history endpoint
│       ├── service/ChatService.java                ← conversationId hashing + persistence
│       ├── websocket/ChatWebSocketHandler.java      ← MESSAGE / TYPING / READ_RECEIPT / PRESENCE
│       ├── repository/ChatMessageRepository.java
│       ├── model/ChatMessage.java
│       ├── dto/ChatMessageDTO.java
│       └── config/WebSocketConfig.java
│
├── media-service/                     port 8085 — local/object storage
│   └── src/main/java/com/socialmedia/mediaservice/
│       ├── MediaServiceApplication.java
│       ├── controller/MediaController.java         ← upload + serve endpoints
│       ├── service/MediaStorageService.java         ← validation, storage abstraction
│       └── model/MediaFile.java
│
├── analytics-service/                  port 8086 — analyticsdb
│   └── src/main/java/com/socialmedia/analyticsservice/
│       ├── AnalyticsServiceApplication.java
│       ├── controller/AnalyticsController.java      ← GET /stats
│       ├── service/AnalyticsService.java
│       ├── messaging/
│       │   ├── AnalyticsEvent.java
│       │   └── AnalyticsEventConsumer.java          ← @RabbitListener bound to "#" (every event)
│       ├── repository/EventCounterRepository.java
│       ├── model/EventCounter.java
│       └── config/RabbitMQConfig.java               ← binds analytics.queue with key "#"
│
├── frontend/                          React 18 application
│   ├── package.json, .env.example, Dockerfile, nginx.conf
│   └── src/
│       ├── App.js, App.css, index.js, index.css
│       ├── context/
│       │   ├── AuthContext.js          ← login/register/logout, JWT storage
│       │   └── SocialMediaContext.js   ← global feed state: useReducer + WebSocket (per task's sample code)
│       ├── hooks/useChatSocket.js      ← chat WebSocket connection management
│       ├── services/                   ← one file per backend service, all via api.js (axios + JWT interceptor)
│       │   ├── api.js, authService.js, userService.js, postService.js,
│       │   │   notificationService.js, chatService.js, mediaService.js
│       ├── components/
│       │   ├── Layout/Navbar.js
│       │   ├── Auth/  LoginForm.js, RegisterForm.js
│       │   ├── Feed/InfiniteFeed.js     ← infinite scroll via IntersectionObserver
│       │   ├── Post/  PostCard.js, CreatePostForm.js
│       │   ├── Notifications/NotificationList.js
│       │   └── Chat/  ChatWindow.js, ConversationList.js
│       └── pages/  FeedPage.js, NotificationsPage.js, ChatPage.js, ProfilePage.js
│
├── kubernetes/                        Kustomize-based manifests
│   ├── kustomization.yaml
│   ├── namespace.yaml, configmap.yaml, secrets.yaml
│   ├── postgres.yaml, rabbitmq.yaml
│   ├── service-discovery.yaml, api-gateway.yaml
│   ├── user-service.yaml, post-service.yaml, notification-service.yaml,
│   │   chat-service.yaml, media-service.yaml, analytics-service.yaml
│   │   (each = Deployment + Service + HorizontalPodAutoscaler)
│   ├── frontend.yaml
│   └── ingress.yaml
│
└── monitoring/
    ├── prometheus.yml               scrape config for all services' Actuator endpoints
    └── README.md                    how to wire up Prometheus + Grafana
```

## Naming & package conventions used throughout

- **Package root**: `com.socialmedia.<serviceName>` (no dots removed, e.g. `com.socialmedia.postservice`)
- **DTOs never leak JPA entities** over HTTP — every controller returns a `*DTO`/`*Response` built by the service layer's `toDTO()` helper
- **Every cross-service call is either**:
  - a Feign client resolved via Eureka (synchronous, circuit-breaker wrapped), or
  - a RabbitMQ event (asynchronous, fire-and-forget with a durable queue)

  There are **no direct `RestTemplate` calls to hard-coded URLs** anywhere in the codebase — this is what makes the services independently deployable and horizontally scalable.
