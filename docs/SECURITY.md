# PridePin — Security Documentation

---

## Baseline Audit Snapshot

Pre-remediation OWASP assessment is recorded in:

- `docs/SECURITY-BASELINE-2026-03-03.md`

Use that file as the official "before changes" security record and track remediation against finding IDs (`F-01` to `F-10`).

---

## 1. Threat Model

PridePin stores information about the physical locations people in a marginalised community consider safe. A breach or manipulation of this data could have real-world safety consequences. The threat model reflects this elevated risk.

### Assets

| Asset | Sensitivity | Impact if compromised |
|-------|-------------|----------------------|
| User credentials (passwords) | High | Account takeover |
| User email addresses | High | Targeted harassment / doxxing |
| Location data (safe spaces) | High | Real-world harm if falsified |
| JWT signing secret | Critical | Full authentication bypass |
| Review data | Medium | Reputation damage / misinformation |

### Threat Actors

| Actor | Motivation | Capability |
|-------|-----------|-----------|
| Automated scrapers | Harvest PII | Low–Medium |
| Targeted harassers | Identify trans users | Medium |
| Malicious admins | Abuse elevated access | Medium |
| Unauthenticated attackers | Spam, data pollution | Low–Medium |
| JWT-forging attackers | Bypass auth | Low (requires secret) |

### Attack Surface

```
Public endpoints:   GET /api/v1/locations/**
                    GET /api/v1/locations/{id}/reviews
                    POST /api/v1/auth/register
                    POST /api/v1/auth/login
                    Static files (index.html)

Authenticated:      POST/PUT/DELETE /api/v1/locations/**
                    POST/PUT/DELETE /api/v1/locations/{id}/reviews/**
                    GET/PUT/DELETE /api/v1/users/me

Admin-only:         /api/v1/users/admin/**
```

---

## 2. Security Controls Implemented

### 2.1 Authentication

- **Mechanism:** JSON Web Tokens (JWT), HMAC-SHA256
- **Issuance:** Only on successful `/auth/login` with verified BCrypt password
- **Validation:** `JwtAuthenticationFilter` validates every inbound request before it reaches any controller
- **Expiry:** Configurable TTL (default 24 hours) enforced in every parse
- **Stateless:** No server-side session; the token is the authority

### 2.2 Password Security

- **Algorithm:** BCrypt with default work factor (10 rounds ≈ 100 ms per hash)
- **Storage:** Only the hash is persisted — plaintext is never logged or stored
- **Transmission:** Passwords travel only over HTTPS (enforced by reverse proxy)
- **Minimum length:** 8 characters enforced by `@Size` validation

### 2.3 Authorisation

- **Role-based:** Spring Security `@PreAuthorize` and `SecurityFilterChain` rules enforce USER vs ADMIN boundaries
- **Resource ownership:** Service layer explicitly checks that the requesting user owns a resource before allowing mutation; ADMINs bypass this check
- **Principle of least privilege:** Public read endpoints do not require authentication; all writes do

### 2.4 Input Validation

All inbound request bodies are validated with Jakarta Bean Validation (`@Valid` on every controller parameter):

| Field | Constraint |
|-------|-----------|
| username | 3–50 chars, alphanumeric + underscore only |
| email | valid format, max 255 chars |
| password | 8–100 chars |
| latitude | -90.0 to 90.0 |
| longitude | -180.0 to 180.0 |
| rating | integer 1–5 |
| name / description / comment | max length enforced |

Validation failures return `400 Bad Request` with a structured field-level error map.

### 2.5 Soft Deletes

No user or location data is hard-deleted. `is_active = false` flags records as inactive. This:
- Preserves audit trails
- Allows data recovery if an account is compromised
- Prevents reference integrity issues

### 2.6 Error Handling

`GlobalExceptionHandler` catches all exceptions and returns structured JSON responses. Stack traces are **never** returned to the client. Sensitive exception details are logged server-side only.

### 2.7 CORS

`CorsConfigurationSource` is configured to:
- Allow any origin pattern (suitable for development / portfolio)
- Restrict allowed methods to `GET, POST, PUT, DELETE, OPTIONS`
- Require explicit credential passing

**For production:** restrict `allowedOriginPatterns` to your actual domain.

### 2.8 HTTP Security Headers (add via reverse proxy)

The Spring Boot app does not set security headers directly — add these at the nginx / Caddy layer:

```nginx
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
add_header X-Content-Type-Options "nosniff" always;
add_header X-Frame-Options "DENY" always;
add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline' cdn.jsdelivr.net unpkg.com; style-src 'self' 'unsafe-inline' cdn.jsdelivr.net; img-src 'self' data: tile.openstreetmap.org;" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
```

---

## 3. Known Risks

### 3.1 JWT in localStorage (Frontend)

The single-file frontend stores the JWT in `localStorage`. This is susceptible to XSS attacks if a third-party script is loaded.

**Mitigation implemented:** CSP header (see §2.8) restricts script sources.
**Stronger mitigation:** Use `HttpOnly` cookies for token storage in a production frontend (requires CSRF protection).

### 3.2 No Rate Limiting

There is no built-in rate limiting on `/auth/login` or `/auth/register`. A determined attacker could brute-force credentials or spam registrations.

**Recommended mitigation:** Add `bucket4j-spring-boot-starter` or use a reverse proxy rate limiter (nginx `limit_req`, Cloudflare rate limiting).

### 3.3 No Account Lockout

Failed login attempts do not lock accounts.

**Recommended mitigation:** Track failed attempts in Redis with a TTL; lock after N failures.

### 3.4 Email Not Verified

Registrations accept any well-formed email address without sending a verification email.

**Recommended mitigation:** Add a `verified` flag + verification token sent via SMTP (Spring Mail).

### 3.5 JWT Revocation

JWTs are stateless — a compromised token is valid until expiry. There is no token revocation mechanism.

**Recommended mitigation:** Maintain a Redis-based blocklist of revoked JIDs, checked in `JwtAuthenticationFilter`.

### 3.6 Open CORS Policy

`allowedOriginPatterns("*")` permits any origin. This is acceptable for a local-dev portfolio but is overly permissive in production.

**Recommended mitigation:** Restrict to your actual origin: `allowedOriginPatterns("https://yourapp.com")`.

### 3.7 DDL Auto-Update in Development

`ddl-auto: update` in `application.yml` is convenient for development but can cause irreversible schema changes in production.

**Required action before production:** Set `ddl-auto: validate` and manage schema with Liquibase or Flyway.

---

## 4. Manual Hardening Checklist

Before deploying to a shared or public environment, verify each item:

- [ ] `JWT_SECRET` is a cryptographically random base64 value (minimum 32 bytes)
  ```bash
  openssl rand -base64 64
  ```
- [ ] `DB_PASSWORD` is a strong, unique password (not `pridepin`)
- [ ] `spring.jpa.hibernate.ddl-auto` is set to `validate`
- [ ] TLS is terminated at the reverse proxy; HTTP-to-HTTPS redirect is in place
- [ ] Security headers are set at the reverse proxy (see §2.8)
- [ ] CORS is restricted to your actual domain
- [ ] Application logs do not contain passwords, tokens, or PII
- [ ] The `/v3/api-docs` and `/swagger-ui` endpoints are disabled or protected in production
  ```yaml
  springdoc:
    api-docs:
      enabled: false
    swagger-ui:
      enabled: false
  ```
- [ ] Rate limiting is in place on authentication endpoints
- [ ] Database user has least-privilege (only the `pridepin` database, no superuser)
- [ ] Backups are enabled and tested

---

## 5. Reporting Vulnerabilities

This is a portfolio project. If you discover a vulnerability, please open a GitHub issue marked `[SECURITY]` or contact the maintainer directly. Do not publicly disclose security issues before they are addressed.
