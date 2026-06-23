# PridePin — System Architecture

---

## 1. System Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         Internet / Browser                       │
└────────────────────────────┬────────────────────────────────────┘
                             │  HTTPS
                             ▼
                 ┌───────────────────────┐
                 │   Reverse Proxy / TLS  │
                 │  (nginx / cloud LB)    │
                 └───────────┬───────────┘
                             │  HTTP
                             ▼
          ┌──────────────────────────────────────┐
          │          Spring Boot 4 Application    │
          │                                      │
          │  ┌─────────────┐  ┌────────────────┐ │
          │  │ Static Files │  │  REST API      │ │
          │  │ /index.html  │  │  /api/v1/**    │ │
          │  │ (Leaflet.js) │  │                │ │
          │  └─────────────┘  └───────┬────────┘ │
          │                           │           │
          │  ┌────────────────────────▼─────────┐ │
          │  │      Spring Security Filter Chain │ │
          │  │   JWT validation on every request  │ │
          │  └────────────────────────┬─────────┘ │
          │                           │           │
          │  ┌────────────────────────▼─────────┐ │
          │  │         Service Layer             │ │
          │  │  AuthService / LocationService /  │ │
          │  │  ReviewService / UserService       │ │
          │  └────────────────────────┬─────────┘ │
          │                           │           │
          │  ┌────────────────────────▼─────────┐ │
          │  │    Spring Data JPA Repositories   │ │
          │  └────────────────────────┬─────────┘ │
          └───────────────────────────┼───────────┘
                                      │  JDBC / HikariCP
                                      ▼
                          ┌───────────────────────┐
                          │     PostgreSQL 15+     │
                          │  pridepin database     │
                          └───────────────────────┘
```

---

## 2. HTTP Request Flow

```
Browser / API Client
        │
        │  1. HTTP Request (with or without Authorization header)
        ▼
┌──────────────────────────┐
│   DispatcherServlet       │
└──────────┬───────────────┘
           │
           ▼
┌──────────────────────────┐
│  JwtAuthenticationFilter │  ◄── OncePerRequestFilter
│  - Reads Authorization   │
│    Bearer token          │
│  - Validates JWT         │
│  - Sets SecurityContext  │
└──────────┬───────────────┘
           │
           ▼
┌──────────────────────────┐
│  SecurityFilterChain     │
│  - Checks permitAll      │
│    routes (public GET)   │
│  - Checks authenticated  │
│    routes                │
│  - Checks role-based     │
│    routes (ADMIN)        │
└──────────┬───────────────┘
           │  (access granted)
           ▼
┌──────────────────────────┐
│    @RestController        │
│  - Deserialises request   │
│  - Runs @Valid validation │
│  - Calls service layer   │
└──────────┬───────────────┘
           │
           ▼
┌──────────────────────────┐
│      Service Layer        │
│  - Business logic        │
│  - Ownership checks      │
│  - Soft-delete handling  │
└──────────┬───────────────┘
           │
           ▼
┌──────────────────────────┐
│   JPA Repository / DB    │
│  - JPQL queries          │
│  - UUID PKs              │
│  - Audit timestamps      │
└──────────┬───────────────┘
           │
           ▼
┌──────────────────────────┐
│   DTO mapping → JSON     │
│   ResponseEntity<T>      │
└──────────────────────────┘
           │
           ▼
     HTTP Response
```

---

## 3. Database Schema

```
┌─────────────────────────────────────────────────────────────┐
│                          users                               │
├──────────────┬──────────────────────────────────────────────┤
│ id           │ UUID PRIMARY KEY                             │
│ username     │ VARCHAR(50) UNIQUE NOT NULL                  │
│ email        │ VARCHAR(255) UNIQUE NOT NULL                 │
│ password     │ VARCHAR(255) NOT NULL  (BCrypt hash)         │
│ role         │ VARCHAR(20) NOT NULL   (USER | ADMIN)        │
│ is_active    │ BOOLEAN NOT NULL DEFAULT TRUE                │
│ created_at   │ TIMESTAMP NOT NULL                          │
│ updated_at   │ TIMESTAMP NOT NULL                          │
└──────────────┴──────────────────────────────────────────────┘
        │ 1
        │
        │ N                         N
        ▼                           ▼
┌──────────────────────────┐  ┌─────────────────────────────┐
│        locations          │  │          reviews             │
├─────────────┬────────────┤  ├─────────────┬───────────────┤
│ id          │ UUID PK    │  │ id          │ UUID PK       │
│ name        │ VARCHAR    │  │ location_id │ UUID FK       │
│ description │ TEXT       │  │ user_id     │ UUID FK       │
│ latitude    │ FLOAT8     │  │ rating      │ INTEGER 1–5   │
│ longitude   │ FLOAT8     │  │ comment     │ TEXT          │
│ category    │ VARCHAR    │  │ is_active   │ BOOLEAN       │
│ address     │ VARCHAR    │  │ created_at  │ TIMESTAMP     │
│ added_by    │ UUID FK ───┼──► updated_at  │ TIMESTAMP     │
│ is_active   │ BOOLEAN    │  └─────────────┴───────────────┘
│ created_at  │ TIMESTAMP  │         │            │
│ updated_at  │ TIMESTAMP  │         │            │
└─────────────┴────────────┘         │ N          │ N
        ▲ 1                           │            │
        │                            └────────────┘
        │                              both FK to
        └──────────────────────────── locations and users
```

```
┌──────────────────────────────────────────────────┐
│              location_safety_tags                  │
├──────────────────┬───────────────────────────────┤
│ location_id      │ UUID FK → locations.id         │
│ tag              │ VARCHAR(50) NOT NULL            │
└──────────────────┴───────────────────────────────┘
```

Tags are stored as a `@ElementCollection` in a join table. A location may carry zero or more tags; each tag is an enum value stored as a plain string column.

```
┌──────────────────────────────────────────────────────────────┐
│                            flags                              │
├──────────────────┬───────────────────────────────────────────┤
│ id               │ UUID PRIMARY KEY                          │
│ location_id      │ UUID FK → locations.id                    │
│ reporter_id      │ UUID FK → users.id                        │
│ reason           │ VARCHAR(50) NOT NULL                       │
│ note             │ TEXT                                       │
│ status           │ VARCHAR(20) NOT NULL  (OPEN | DISMISSED | ACTIONED) │
│ resolved_by      │ UUID FK → users.id (nullable)             │
│ resolved_at      │ TIMESTAMP (nullable)                       │
│ is_active        │ BOOLEAN NOT NULL DEFAULT TRUE             │
│ created_at       │ TIMESTAMP NOT NULL                        │
│ updated_at       │ TIMESTAMP NOT NULL                        │
└──────────────────┴───────────────────────────────────────────┘
```

### Relationships

| Relationship             | Cardinality | Notes                                        |
|--------------------------|-------------|----------------------------------------------|
| User → Locations         | 1 : N       | via `added_by` FK                            |
| User → Reviews           | 1 : N       | via `user_id` FK                             |
| Location → Reviews       | 1 : N       | via `location_id` FK                         |
| User + Location → Review | N : 1       | One active review per pair (app-level)       |
| Location → SafetyTags    | 1 : N       | via `location_safety_tags` join table        |
| Location → Flags         | 1 : N       | via `location_id` FK                         |
| User → Flags (reporter)  | 1 : N       | via `reporter_id` FK                         |
| User → Flags (resolver)  | 1 : N       | via `resolved_by` FK (nullable)              |

---

## 4. Security Flow

```
  Client
    │
    │  POST /api/v1/auth/login
    │  { username, password }
    ▼
┌────────────────────────────┐
│      AuthController        │
│  authService.login(req)    │
└───────────┬────────────────┘
            │
            ▼
┌────────────────────────────┐
│  AuthenticationManager     │
│  DaoAuthenticationProvider │
│  BCryptPasswordEncoder     │
│  (verifies password hash)  │
└───────────┬────────────────┘
            │  credentials valid
            ▼
┌────────────────────────────┐
│        JwtUtil             │
│  generateToken(userDetails)│
│  - sub: username           │
│  - iat: now                │
│  - exp: now + 24h          │
│  - signed with HMAC-SHA256 │
└───────────┬────────────────┘
            │
            ▼
  { token: "eyJ...", user: {...} }
            │
            │  Client stores token in localStorage
            │
   ── subsequent requests ──────────────────────────────
            │
            │  GET /api/v1/locations
            │  Authorization: Bearer eyJ...
            ▼
┌────────────────────────────┐
│  JwtAuthenticationFilter   │
│  1. Extract token          │
│  2. Parse & verify sig     │
│  3. Check expiry           │
│  4. Load UserDetails       │
│  5. Set SecurityContext    │
└───────────┬────────────────┘
            │  authenticated
            ▼
        Controller / Service
```

---

## 5. Project Folder Structure

```
pridepin/
├── CHANGELOG.md
├── README.md
├── docs/
│   ├── API.md
│   ├── ARCHITECTURE.md
│   └── SECURITY.md
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/pridepin/pridepin/
    │   │   ├── PridepinApplication.java          # @SpringBootApplication + @EnableJpaAuditing
    │   │   │
    │   │   ├── config/
    │   │   │   ├── OpenApiConfig.java             # SpringDoc / Swagger UI config
    │   │   │   └── SecurityConfig.java            # SecurityFilterChain, CORS, password encoder
    │   │   │
    │   │   ├── controller/
    │   │   │   ├── AuthController.java            # /api/v1/auth/**
    │   │   │   ├── FlagController.java            # /api/v1/locations/{id}/flags, /api/v1/flags/admin/**
    │   │   │   ├── LocationController.java        # /api/v1/locations/**
    │   │   │   ├── ReviewController.java          # /api/v1/locations/{id}/reviews/**
    │   │   │   └── UserController.java            # /api/v1/users/**
    │   │   │
    │   │   ├── dto/
    │   │   │   ├── request/
    │   │   │   │   ├── FlagRequest.java
    │   │   │   │   ├── LoginRequest.java
    │   │   │   │   ├── LocationRequest.java
    │   │   │   │   ├── RegisterRequest.java
    │   │   │   │   ├── ResolveFlagRequest.java
    │   │   │   │   ├── ReviewRequest.java
    │   │   │   │   └── UpdateUserRequest.java
    │   │   │   └── response/
    │   │   │       ├── AuthResponse.java
    │   │   │       ├── FlagResponse.java
    │   │   │       ├── LocationResponse.java
    │   │   │       ├── MessageResponse.java
    │   │   │       ├── ReviewResponse.java
    │   │   │       └── UserResponse.java
    │   │   │
    │   │   ├── entity/
    │   │   │   ├── Flag.java
    │   │   │   ├── Location.java
    │   │   │   ├── Review.java
    │   │   │   └── User.java
    │   │   │
    │   │   ├── enums/
    │   │   │   ├── FlagReason.java
    │   │   │   ├── FlagResolution.java
    │   │   │   ├── FlagStatus.java
    │   │   │   ├── LocationCategory.java
    │   │   │   ├── Role.java
    │   │   │   └── SafetyTag.java
    │   │   │
    │   │   ├── exception/
    │   │   │   ├── GlobalExceptionHandler.java
    │   │   │   ├── ResourceNotFoundException.java
    │   │   │   ├── UnauthorizedException.java
    │   │   │   └── UserAlreadyExistsException.java
    │   │   │
    │   │   ├── repository/
    │   │   │   ├── FlagRepository.java
    │   │   │   ├── LocationRepository.java
    │   │   │   ├── ReviewRepository.java
    │   │   │   └── UserRepository.java
    │   │   │
    │   │   ├── security/
    │   │   │   ├── JwtAuthenticationFilter.java
    │   │   │   ├── JwtUtil.java
    │   │   │   └── UserDetailsServiceImpl.java
    │   │   │
    │   │   └── service/
    │   │       ├── AuthService.java
    │   │       ├── FlagService.java
    │   │       ├── LocationService.java
    │   │       ├── ReviewService.java
    │   │       └── UserService.java
    │   │
    │   └── resources/
    │       ├── application.yml
    │       └── static/
    │           └── index.html                    # Leaflet.js single-page frontend
    │
    └── test/
        └── java/com/pridepin/pridepin/
            └── PridepinApplicationTests.java
```

---

## 6. Key Design Decisions

| Decision | Choice | Reason |
|----------|--------|--------|
| Primary keys | UUID | Avoids enumerable IDs in URLs, better for distributed systems |
| Delete strategy | Soft delete (`is_active`) | Preserves data for audit trails; nothing is lost |
| Token storage | JWT in `Authorization: Bearer` header | Stateless; no server-side session storage needed |
| Password storage | BCrypt (default strength 10) | Adaptive hashing; resistant to brute force |
| Pagination | Spring Data `Pageable` | Prevents unbounded result sets |
| Average rating | Computed at query time | Always accurate; avoids stale denormalised data |
| One review per user | Enforced in service layer | Allows deleted reviews to be re-submitted |
| Safety tags storage | `@ElementCollection` join table | Simple many-values-per-entity pattern; no dedicated entity needed for a fixed enum set |
| Safety tags filtering | Single-tag `?tag=` query param | Covers the primary use case cleanly; multi-tag AND/OR can be added without breaking the API |
| Flag uniqueness | One open flag per (reporter, location) — service layer | Prevents spam; allows re-flagging after a flag is resolved |
| Flag resolution | Two outcomes: DISMISS or DEACTIVATE | Covers the two realistic admin responses without over-engineering a complex workflow |
