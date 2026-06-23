# Changelog

All notable changes to PridePin will be documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.3.0] - 2026-06-23

### Added

#### Map Filter UI
- Floating 🔍 Filters button in the top-left of the map
- Filter panel with category chips (all 10 categories) and safety tag chips (all 11 tags)
- Single-select per group — click a chip to activate, click again to deactivate
- Filters can be combined (e.g. category=HEALTHCARE + tag=TRANS_HEALTHCARE)
- Active filter count badge on the Filters button
- "Clear all filters" button resets both filters and reloads all locations
- Panel closes automatically when clicking outside it
- Map markers reload instantly when any filter changes

---

## [1.2.0] - 2026-05-31

### Added

#### Flag / Report System
- New `FlagReason` enum: `NO_LONGER_SAFE`, `HOSTILE_ENVIRONMENT`, `INCORRECT_INFO`, `PERMANENTLY_CLOSED`, `WRONG_LOCATION`, `DUPLICATE`, `OTHER`
- New `FlagStatus` enum: `OPEN`, `DISMISSED`, `ACTIONED`
- New `FlagResolution` enum: `DISMISS`, `DEACTIVATE`
- `Flag` entity with `flags` table — stores reporter, location, reason, note, status, resolver, and resolved timestamp
- `POST /api/v1/locations/{locationId}/flags` — authenticated users can report a location; one open flag per user per location enforced
- `GET /api/v1/flags/admin?status=OPEN` — paginated list of flags filtered by status (ADMIN only)
- `PUT /api/v1/flags/admin/{flagId}/resolve` — resolve a flag with `DISMISS` (no location change) or `DEACTIVATE` (soft-delete location) (ADMIN only)
- Frontend: 🚩 Report button in location sidebar (visible to logged-in non-owners)
- Frontend: Report modal with reason dropdown and optional note field
- Frontend: Admin-only 🚩 Flags button in navbar opens the flag queue modal
- Frontend: Flag queue shows location name (clickable), reporter, reason, note, date with Dismiss / Deactivate actions
- Frontend: Deactivating a location removes its marker from the map immediately

---

## [1.1.0] - 2026-05-31

### Added

#### Safety Tags
- New `SafetyTag` enum with 11 community-meaningful values: `TRANS_OWNED`, `LGBTQ_OWNED`, `TRANS_AFFIRMING`, `PRONOUN_FRIENDLY_STAFF`, `GENDER_NEUTRAL_RESTROOMS`, `TRANS_HEALTHCARE`, `TRANS_YOUTH_SERVICES`, `NAME_CHANGE_ASSISTANCE`, `FREE_SERVICES`, `CRISIS_SUPPORT`, `WHEELCHAIR_ACCESSIBLE`
- `location_safety_tags` join table storing the set of tags for each location
- `tags` field on `LocationRequest` (optional) — accepted on POST and PUT `/api/v1/locations`
- `tags` field on `LocationResponse` — returned in all location list/get/create/update responses
- `?tag=<SafetyTag>` query parameter on `GET /api/v1/locations` for single-tag filtering
- Combined category + tag filtering (`?category=HEALTHCARE&tag=TRANS_HEALTHCARE`)
- Frontend: colour-coded safety tag badges displayed in the sidebar location detail panel
- Frontend: tag picker (toggleable buttons) in the Add Location modal — select all tags that apply
- Frontend: tags cleared correctly when the Add Location form is reset

---

## [1.0.0] - 2026-03-02

### Added

#### Authentication
- `POST /api/v1/auth/register` — username/email/password registration with BCrypt hashing
- `POST /api/v1/auth/login` — credential verification and JWT issuance
- Stateless JWT authentication filter on every protected request
- Role-based access control with `USER` and `ADMIN` roles

#### Locations
- `GET /api/v1/locations` — paginated list of active locations, filterable by category
- `GET /api/v1/locations/{id}` — single location with computed average rating and review count
- `POST /api/v1/locations` — authenticated users can add new locations
- `PUT /api/v1/locations/{id}` — location owner or ADMIN can update
- `DELETE /api/v1/locations/{id}` — soft delete by owner or ADMIN

#### Reviews
- `GET /api/v1/locations/{id}/reviews` — paginated reviews for a location (public)
- `POST /api/v1/locations/{id}/reviews` — submit a 1–5 star review with optional comment
- `PUT /api/v1/locations/{id}/reviews/{reviewId}` — update own review (or ADMIN)
- `DELETE /api/v1/locations/{id}/reviews/{reviewId}` — soft delete (owner or ADMIN)
- One review per user per location enforced at the service layer

#### Users
- `GET /api/v1/users/me` — view own profile
- `PUT /api/v1/users/me` — update username, email, or password
- `DELETE /api/v1/users/me` — soft-delete own account
- `GET /api/v1/users/admin` — list all users (ADMIN only)
- `GET /api/v1/users/admin/{id}` — view any user (ADMIN only)
- `DELETE /api/v1/users/admin/{id}` — soft-delete any user (ADMIN only)

#### Infrastructure
- UUID primary keys on all entities
- Soft deletes via `is_active` column throughout
- JPA auditing (`created_at`, `updated_at`) with `@CreatedDate` / `@LastModifiedDate`
- SpringDoc OpenAPI / Swagger UI at `/swagger-ui.html`
- Global exception handler with structured JSON error responses
- Bean Validation on all request DTOs
- CORS configuration for local and deployed environments

#### Frontend
- Single-page Leaflet.js map application served from Spring Boot static resources
- Category-coloured circle markers
- Sliding sidebar with location details and reviews
- In-map location picker (click map to set coordinates)
- Login, register, and add-location modals
- Star rating display and submission form
- JWT stored in `localStorage` with automatic header injection

[1.0.0]: https://github.com/your-username/pridepin/releases/tag/v1.0.0
