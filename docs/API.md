# PridePin — API Reference

Base URL: `http://localhost:8080/api/v1`

All request and response bodies use `Content-Type: application/json`.
Protected endpoints require `Authorization: Bearer <token>`.

---

## Authentication

### Register

```
POST /auth/register
```

Creates a new user account and returns a JWT.

**Request body**

```json
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "Secure1234"
}
```

| Field    | Type   | Constraints                                   |
|----------|--------|-----------------------------------------------|
| username | string | 3–50 chars, letters/numbers/underscore only   |
| email    | string | Valid email format, max 255 chars             |
| password | string | 8–100 chars                                   |

**Response `201 Created`**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "username": "alice",
    "email": "alice@example.com",
    "role": "USER",
    "createdAt": "2026-03-02T10:00:00"
  }
}
```

**Error responses**

| Status | Condition |
|--------|-----------|
| 400    | Validation failure (see Validation Errors) |
| 409    | Username or email already in use |

---

### Login

```
POST /auth/login
```

Authenticates an existing user and returns a JWT.

**Request body**

```json
{
  "username": "alice",
  "password": "Secure1234"
}
```

**Response `200 OK`**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "username": "alice",
    "email": "alice@example.com",
    "role": "USER",
    "createdAt": "2026-03-02T10:00:00"
  }
}
```

**Error responses**

| Status | Condition |
|--------|-----------|
| 400    | Missing fields |
| 401    | Invalid credentials |

---

## Locations

### List locations

```
GET /locations?category=BAR&tag=TRANS_OWNED&page=0&size=20&sort=createdAt,desc
```

Returns a paginated list of active locations. Public — no auth required.

**Query parameters**

| Parameter | Type   | Required | Description |
|-----------|--------|----------|-------------|
| category  | string | No       | Filter by `LocationCategory` enum value |
| tag       | string | No       | Filter by `SafetyTag` enum value |
| page      | int    | No       | 0-indexed page number (default 0) |
| size      | int    | No       | Items per page (default 20, max 100) |
| sort      | string | No       | Field and direction, e.g. `createdAt,desc` |

Both `category` and `tag` may be combined in a single request.

**LocationCategory values**

`BAR`, `RESTAURANT`, `MEDICAL`, `LEGAL`, `SHELTER`, `COMMUNITY_CENTER`, `SHOPPING`, `SUPPORT_GROUP`, `HEALTHCARE`, `OTHER`

**SafetyTag values**

| Value | Meaning |
|-------|---------|
| `TRANS_OWNED` | Business or space is trans-owned |
| `LGBTQ_OWNED` | Business or space is LGBTQ+-owned |
| `TRANS_AFFIRMING` | Explicitly welcoming to trans people |
| `PRONOUN_FRIENDLY_STAFF` | Staff use correct pronouns |
| `GENDER_NEUTRAL_RESTROOMS` | Gender-neutral / all-gender bathrooms available |
| `TRANS_HEALTHCARE` | Provides trans-specific healthcare |
| `TRANS_YOUTH_SERVICES` | Services specifically for trans youth |
| `NAME_CHANGE_ASSISTANCE` | Legal help with name or gender marker changes |
| `FREE_SERVICES` | Services provided at no cost |
| `CRISIS_SUPPORT` | Crisis or emergency support available |
| `WHEELCHAIR_ACCESSIBLE` | Physically accessible for wheelchair users |

**Response `200 OK`**

```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "name": "The Pride Bar",
      "description": "Welcoming bar with gender-neutral bathrooms",
      "latitude": 51.5074,
      "longitude": -0.1278,
      "category": "BAR",
      "address": "1 Rainbow Street, London",
      "tags": ["TRANS_OWNED", "GENDER_NEUTRAL_RESTROOMS"],
      "addedByUsername": "alice",
      "addedById": "550e8400-e29b-41d4-a716-446655440000",
      "averageRating": 4.5,
      "reviewCount": 12,
      "createdAt": "2026-03-02T10:00:00",
      "updatedAt": "2026-03-02T10:00:00"
    }
  ],
  "pageable": { ... },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "first": true
}
```

---

### Get location by ID

```
GET /locations/{id}
```

Returns a single active location. Public — no auth required.

**Path parameters**

| Parameter | Type | Description |
|-----------|------|-------------|
| id        | UUID | Location ID |

**Response `200 OK`** — same as a single element from the list above.

**Error responses**

| Status | Condition |
|--------|-----------|
| 404    | Location not found or inactive |

---

### Create location

```
POST /locations
Authorization: Bearer <token>
```

Adds a new safe-space location. Requires authentication.

**Request body**

```json
{
  "name": "Trans Health Clinic",
  "description": "Specialist clinic with trans-affirming staff",
  "latitude": 51.5080,
  "longitude": -0.1300,
  "category": "HEALTHCARE",
  "address": "42 Care Lane, London",
  "tags": ["TRANS_AFFIRMING", "TRANS_HEALTHCARE", "PRONOUN_FRIENDLY_STAFF"]
}
```

| Field       | Type         | Required | Constraints |
|-------------|--------------|----------|-------------|
| name        | string       | Yes      | Max 255 chars |
| description | string       | No       | Max 2000 chars |
| latitude    | number       | Yes      | -90.0 to 90.0 |
| longitude   | number       | Yes      | -180.0 to 180.0 |
| category    | string       | Yes      | Valid `LocationCategory` enum value |
| address     | string       | No       | Max 500 chars |
| tags        | string array | No       | Zero or more valid `SafetyTag` enum values |

**Response `201 Created`** — full `LocationResponse` object.

**Error responses**

| Status | Condition |
|--------|-----------|
| 400    | Validation failure |
| 401    | Missing or invalid token |

---

### Update location

```
PUT /locations/{id}
Authorization: Bearer <token>
```

Updates an existing location. Caller must be the location owner or ADMIN.

**Request body** — same structure as Create location (all fields required on update).

**Response `200 OK`** — updated `LocationResponse`.

**Error responses**

| Status | Condition |
|--------|-----------|
| 400    | Validation failure |
| 401    | Missing or invalid token |
| 403    | Not the owner and not ADMIN |
| 404    | Location not found |

---

### Delete location

```
DELETE /locations/{id}
Authorization: Bearer <token>
```

Soft-deletes a location (sets `is_active = false`). Caller must be the location owner or ADMIN.

**Response `200 OK`**

```json
{ "message": "Location deleted successfully" }
```

**Error responses**

| Status | Condition |
|--------|-----------|
| 401    | Missing or invalid token |
| 403    | Not the owner and not ADMIN |
| 404    | Location not found |

---

## Reviews

### List reviews for a location

```
GET /locations/{locationId}/reviews?page=0&size=10
```

Returns paginated reviews for a location. Public — no auth required.

**Path parameters**

| Parameter  | Type | Description |
|------------|------|-------------|
| locationId | UUID | Location ID |

**Response `200 OK`**

```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440002",
      "locationId": "550e8400-e29b-41d4-a716-446655440001",
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "username": "alice",
      "rating": 5,
      "comment": "Incredibly welcoming — felt completely safe",
      "createdAt": "2026-03-02T11:00:00",
      "updatedAt": "2026-03-02T11:00:00"
    }
  ],
  "totalElements": 1,
  "totalPages": 1
}
```

**Error responses**

| Status | Condition |
|--------|-----------|
| 404    | Location not found |

---

### Submit a review

```
POST /locations/{locationId}/reviews
Authorization: Bearer <token>
```

Submits a review for a location. One active review per user per location is enforced.

**Request body**

```json
{
  "rating": 5,
  "comment": "Felt completely safe and welcome here"
}
```

| Field   | Type    | Required | Constraints |
|---------|---------|----------|-------------|
| rating  | integer | Yes      | 1–5 |
| comment | string  | No       | Max 2000 chars |

**Response `201 Created`** — full `ReviewResponse` object.

**Error responses**

| Status | Condition |
|--------|-----------|
| 400    | Validation failure |
| 401    | Missing or invalid token |
| 404    | Location not found |
| 409    | User has already reviewed this location |

---

### Update a review

```
PUT /locations/{locationId}/reviews/{reviewId}
Authorization: Bearer <token>
```

Updates an existing review. Caller must be the review author or ADMIN.

**Request body** — same as Submit review.

**Response `200 OK`** — updated `ReviewResponse`.

**Error responses**

| Status | Condition |
|--------|-----------|
| 400    | Validation failure |
| 401    | Missing or invalid token |
| 403    | Not the author and not ADMIN |
| 404    | Location or review not found |

---

### Delete a review

```
DELETE /locations/{locationId}/reviews/{reviewId}
Authorization: Bearer <token>
```

Soft-deletes a review. Caller must be the review author or ADMIN.

**Response `200 OK`**

```json
{ "message": "Review deleted successfully" }
```

---

## Flags

### Submit a flag

```
POST /locations/{locationId}/flags
Authorization: Bearer <token>
```

Reports a location to admins for review. One open flag per user per location is enforced.

**Path parameters**

| Parameter  | Type | Description |
|------------|------|-------------|
| locationId | UUID | Location to report |

**Request body**

```json
{
  "reason": "NO_LONGER_SAFE",
  "note": "Staff were hostile when I mentioned being trans."
}
```

| Field  | Type   | Required | Constraints |
|--------|--------|----------|-------------|
| reason | string | Yes      | Valid `FlagReason` enum value |
| note   | string | No       | Max 1000 chars |

**FlagReason values**

| Value | Meaning |
|-------|---------|
| `NO_LONGER_SAFE` | Space is no longer safe for trans people |
| `HOSTILE_ENVIRONMENT` | Hostile or unwelcoming environment |
| `INCORRECT_INFO` | Name, address, or description is wrong |
| `PERMANENTLY_CLOSED` | Business or service has closed |
| `WRONG_LOCATION` | Pin is in the wrong place on the map |
| `DUPLICATE` | Same place listed more than once |
| `OTHER` | Any other reason (describe in note) |

**Response `201 Created`** — `FlagResponse` object.

**Error responses**

| Status | Condition |
|--------|-----------|
| 400    | Validation failure or user already has an open flag for this location |
| 401    | Missing or invalid token |
| 404    | Location not found |

---

### List flags *(ADMIN only)*

```
GET /flags/admin?status=OPEN&page=0&size=20
Authorization: Bearer <admin-token>
```

Returns a paginated list of flags filtered by status. Defaults to `OPEN`.

**Query parameters**

| Parameter | Type   | Required | Description |
|-----------|--------|----------|-------------|
| status    | string | No       | `OPEN` (default), `DISMISSED`, or `ACTIONED` |
| page      | int    | No       | 0-indexed page (default 0) |
| size      | int    | No       | Items per page (default 20) |

**Response `200 OK`** — paginated list of `FlagResponse` objects.

```json
{
  "content": [
    {
      "id": "...",
      "locationId": "...",
      "locationName": "The Pride Bar",
      "reporterId": "...",
      "reporterUsername": "alice",
      "reason": "NO_LONGER_SAFE",
      "note": "Staff were hostile when I mentioned being trans.",
      "status": "OPEN",
      "resolvedById": null,
      "resolvedByUsername": null,
      "resolvedAt": null,
      "createdAt": "2026-05-31T10:00:00"
    }
  ],
  "totalElements": 1,
  "totalPages": 1
}
```

**Error responses**

| Status | Condition |
|--------|-----------|
| 403    | Caller does not have ADMIN role |

---

### Resolve a flag *(ADMIN only)*

```
PUT /flags/admin/{flagId}/resolve
Authorization: Bearer <admin-token>
```

Resolves an open flag. `DISMISS` closes the flag with no changes to the location. `DEACTIVATE` closes the flag and soft-deletes the location.

**Path parameters**

| Parameter | Type | Description |
|-----------|------|-------------|
| flagId    | UUID | Flag to resolve |

**Request body**

```json
{ "action": "DEACTIVATE" }
```

| Field  | Type   | Required | Values |
|--------|--------|----------|--------|
| action | string | Yes      | `DISMISS` or `DEACTIVATE` |

**Response `200 OK`** — updated `FlagResponse` with `status` set to `DISMISSED` or `ACTIONED`.

**Error responses**

| Status | Condition |
|--------|-----------|
| 400    | Flag is already resolved |
| 403    | Caller does not have ADMIN role |
| 404    | Flag not found |

---

## Users

All `/users/**` endpoints require authentication.

### Get own profile

```
GET /users/me
Authorization: Bearer <token>
```

**Response `200 OK`**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "username": "alice",
  "email": "alice@example.com",
  "role": "USER",
  "createdAt": "2026-03-02T10:00:00"
}
```

---

### Update own profile

```
PUT /users/me
Authorization: Bearer <token>
```

All fields are optional — only include fields you want to change.

**Request body**

```json
{
  "username": "alice2",
  "email": "alice2@example.com",
  "password": "NewPassword99"
}
```

| Field    | Type   | Required | Constraints |
|----------|--------|----------|-------------|
| username | string | No       | 3–50 chars, alphanumeric + underscore |
| email    | string | No       | Valid email, max 255 chars |
| password | string | No       | 8–100 chars |

**Response `200 OK`** — updated `UserResponse`.

**Error responses**

| Status | Condition |
|--------|-----------|
| 400    | Validation failure |
| 409    | New username or email already in use |

---

### Delete own account

```
DELETE /users/me
Authorization: Bearer <token>
```

Soft-deletes the authenticated user's account.

**Response `200 OK`**

```json
{ "message": "Account deleted successfully" }
```

---

### List all users *(ADMIN only)*

```
GET /users/admin
Authorization: Bearer <admin-token>
```

**Response `200 OK`** — array of `UserResponse` objects.

**Error responses**

| Status | Condition |
|--------|-----------|
| 403    | Caller does not have ADMIN role |

---

### Get user by ID *(ADMIN only)*

```
GET /users/admin/{id}
Authorization: Bearer <admin-token>
```

**Response `200 OK`** — single `UserResponse`.

---

### Delete user by ID *(ADMIN only)*

```
DELETE /users/admin/{id}
Authorization: Bearer <admin-token>
```

Soft-deletes the specified user account.

**Response `200 OK`**

```json
{ "message": "User deleted successfully" }
```

---

## Error Response Format

### Standard error

```json
{
  "status": 404,
  "message": "Location not found with id: '550e8400-...'",
  "timestamp": "2026-03-02T10:00:00"
}
```

### Validation error

```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": {
    "email": "Invalid email format",
    "password": "Password must be 8-100 characters"
  },
  "timestamp": "2026-03-02T10:00:00"
}
```

### HTTP Status Code Summary

| Code | Meaning |
|------|---------|
| 200  | OK |
| 201  | Created |
| 400  | Bad Request / Validation Error |
| 401  | Unauthenticated (missing or invalid token) |
| 403  | Forbidden (authenticated but not authorised) |
| 404  | Resource not found |
| 409  | Conflict (duplicate username, email, or review) |
| 500  | Internal Server Error |
