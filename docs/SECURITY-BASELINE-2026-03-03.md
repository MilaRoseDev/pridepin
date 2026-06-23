# PridePin Security Baseline Report (Pre-Remediation)

**Project:** PridePin  
**Assessment Date:** 2026-03-03  
**Assessor:** Internal Security Review Team  
**Standard:** OWASP Top 10 (2021)  
**Version Scope:** Current `main` codebase before remediation changes

---

## 1) Purpose

This document is the official baseline record of security weaknesses and gaps identified **before** implementing hardening changes.

It is intended to:
- establish an auditable "before" state,
- map findings to OWASP Top 10 categories,
- prioritize remediation work.

---

## 2) Method and Scope

### In Scope
- Backend code under `src/main/java`
- Runtime configuration in `src/main/resources/application.yml`
- Dependency declaration in `pom.xml`
- API behavior documented in `docs/API.md`

### Out of Scope
- Dynamic penetration testing
- Infrastructure/cloud configuration not represented in repo
- Frontend runtime hardening beyond what is documented in backend files

### Confidence
Moderate. Findings are based on static code/config review and best-practice alignment, not exploit simulation.

---

## 3) Executive Summary

PridePin has a good base security architecture (JWT auth, BCrypt password hashing, service-layer authorization checks, validation, and structured error handling).  
However, it is **not yet fully aligned** with OWASP Top 10 for production readiness.

High-priority weaknesses are concentrated in:
- Security misconfiguration,
- Authentication abuse protections (rate limiting/lockout),
- Token lifecycle controls,
- Operational security controls (dependency scanning/monitoring policy).

---

## 4) OWASP Top 10 Status Matrix (2021)

| OWASP ID | Category | Status | Notes |
|---|---|---|---|
| A01 | Broken Access Control | **Partial (Strong)** | Route auth + role/ownership checks present; needs broader authorization test coverage |
| A02 | Cryptographic Failures | **Partial** | BCrypt + signed JWTs present; insecure defaults still allowed in config |
| A03 | Injection | **Mostly Addressed** | JPA/repository usage reduces SQLi risk; client-side XSS handling not guaranteed |
| A04 | Insecure Design | **Partial** | Verification flow exists; abuse controls (rate-limit/lockout) missing |
| A05 | Security Misconfiguration | **Needs Remediation** | CORS too permissive, Swagger public, dev-friendly defaults in runtime config |
| A06 | Vulnerable/Outdated Components | **Unknown/Partial** | Modern versions appear used; no repo-visible automated vulnerability gate |
| A07 | Identification & Authentication Failures | **Partial** | JWT auth flow present; no revocation, no refresh rotation, no lockout |
| A08 | Software & Data Integrity Failures | **Partial** | No obvious unsafe deserialization; no explicit SBOM/signature/integrity process in repo |
| A09 | Security Logging & Monitoring Failures | **Partial** | Basic logs exist; no formal audit event policy/alerting evidence |
| A10 | SSRF | **Low Apparent Risk** | No user-controlled server-side URL fetch feature detected |

---

## 5) Detailed Findings Register

## F-01: Overly Permissive CORS With Credentials
- **Severity:** High
- **OWASP:** A05 Security Misconfiguration
- **Evidence:** `src/main/java/com/pridepin/pridepin/config/SecurityConfig.java`
- **Observation:** CORS allows `*` origin patterns while credentials are enabled.
- **Risk:** Increases cross-origin abuse risk and weakens browser boundary assumptions.
- **Recommended Fix:** Restrict origins to trusted frontend domains per environment; disable credentials if not needed.

## F-02: Development JWT Secret Fallback Present in Runtime Config
- **Severity:** High
- **OWASP:** A02 Cryptographic Failures, A05 Security Misconfiguration
- **Evidence:** `src/main/resources/application.yml`
- **Observation:** `JWT_SECRET` has a default fallback value for local convenience.
- **Risk:** Misdeployment can lead to predictable/known signing secret and token forgery risk.
- **Recommended Fix:** Remove sensitive defaults; require secret injection from environment/secret manager.

## F-03: Email Verification Disabled by Default
- **Severity:** Medium
- **OWASP:** A07 Identification & Authentication Failures, A05
- **Evidence:** `src/main/resources/application.yml`, `src/main/java/com/pridepin/pridepin/service/AuthService.java`
- **Observation:** `app.email-verification.enabled` defaults to `false`.
- **Risk:** Low assurance identity onboarding in environments that accidentally inherit defaults.
- **Recommended Fix:** Default to `true` outside dev profile; split config by profile.

## F-04: Swagger/OpenAPI Endpoints Publicly Exposed
- **Severity:** Medium
- **OWASP:** A05 Security Misconfiguration
- **Evidence:** `src/main/java/com/pridepin/pridepin/config/SecurityConfig.java`
- **Observation:** `/swagger-ui/**` and `/v3/api-docs/**` are publicly permitted.
- **Risk:** Increases reconnaissance surface for attackers.
- **Recommended Fix:** Disable or protect docs in production (profile/role/IP based control).

## F-05: No Rate Limiting on Authentication Endpoints
- **Severity:** High
- **OWASP:** A04 Insecure Design, A07 Authentication Failures
- **Evidence:** `src/main/java/com/pridepin/pridepin/controller/AuthController.java` (no throttling layer present)
- **Observation:** Login/register/resend endpoints have no request throttling.
- **Risk:** Credential stuffing, brute-force, and registration spam exposure.
- **Recommended Fix:** Apply API gateway/proxy limits and/or application-level bucket-based limits.

## F-06: No Account Lockout / Progressive Delays
- **Severity:** Medium
- **OWASP:** A07 Identification & Authentication Failures
- **Evidence:** `src/main/java/com/pridepin/pridepin/service/AuthService.java` (no failed-attempt tracking logic)
- **Observation:** Repeated failed logins are not tracked for lockout/backoff.
- **Risk:** Facilitates brute-force attempts over time.
- **Recommended Fix:** Add per-account/IP failed-attempt tracking with cooldown/lock thresholds.

## F-07: Stateless JWT Without Revocation or Rotation Model
- **Severity:** Medium
- **OWASP:** A07
- **Evidence:** `src/main/java/com/pridepin/pridepin/security/JwtUtil.java`, `src/main/java/com/pridepin/pridepin/security/JwtAuthenticationFilter.java`
- **Observation:** Access tokens are validated by signature/expiry only; no revocation list or token versioning.
- **Risk:** Stolen tokens remain valid until expiration.
- **Recommended Fix:** Add refresh-token rotation and revocation strategy (`jti` denylist or token version).

## F-08: Schema Auto-Update Enabled in Main Runtime Config
- **Severity:** Medium
- **OWASP:** A05 Security Misconfiguration
- **Evidence:** `src/main/resources/application.yml` (`spring.jpa.hibernate.ddl-auto: update`)
- **Observation:** Runtime schema mutation is enabled by default.
- **Risk:** Uncontrolled schema drift and unsafe production changes.
- **Recommended Fix:** Use `validate` in production; manage migrations via Flyway/Liquibase.

## F-09: No In-Repo Evidence of Security Headers/TLS Enforcement at App Layer
- **Severity:** Medium
- **OWASP:** A05
- **Evidence:** `src/main/java/com/pridepin/pridepin/config/SecurityConfig.java`, `docs/SECURITY.md`
- **Observation:** Security headers and HTTPS enforcement are expected at reverse proxy, not guaranteed by app config.
- **Risk:** If proxy is misconfigured, browser protections may be absent.
- **Recommended Fix:** Document mandatory proxy baseline and add deployment checks.

## F-10: No Repo-Visible Automated Dependency Vulnerability Gate
- **Severity:** Medium
- **OWASP:** A06 Vulnerable and Outdated Components
- **Evidence:** `pom.xml` (no dependency-check plugin configuration in repo)
- **Observation:** No explicit in-repo CI gate for known vulnerable dependencies.
- **Risk:** Vulnerable transitive dependencies may enter releases unnoticed.
- **Recommended Fix:** Add dependency scanning (e.g., OWASP Dependency-Check/Dependabot/Snyk) with fail thresholds.

---

## 6) Controls Confirmed as Present (Strengths)

- JWT-based authentication and signature validation (`JwtUtil`, `JwtAuthenticationFilter`)
- BCrypt password hashing (`SecurityConfig`, `AuthService`, `UserService`)
- Role and ownership authorization checks (`UserController`, `LocationService`, `ReviewService`)
- Request validation with structured validation error responses (`@Valid`, `GlobalExceptionHandler`)
- Generic response pattern for resend verification (helps reduce email enumeration)

---

## 7) Priority Remediation Plan

### Priority 0 (Immediate)
1. Lock down CORS by environment
2. Remove default JWT secret for non-dev use
3. Add rate limiting to auth endpoints

### Priority 1 (Near Term)
4. Disable/protect Swagger in production
5. Switch production schema mode to `validate` + migrations
6. Add login lockout/backoff

### Priority 2 (Planned)
7. Implement token revocation/refresh model
8. Add dependency vulnerability scanning gate
9. Formalize security monitoring and alerting policy

---

## 8) Approval and Change Control

This baseline is approved as the **pre-remediation security state** for PridePin as of 2026-03-03.

Any remediation PR should reference:
- finding IDs from this document (`F-01` ... `F-10`),
- affected OWASP category,
- verification steps and tests added.

