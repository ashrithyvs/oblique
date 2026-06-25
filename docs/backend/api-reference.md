# Backend — API reference

Base path: `/api`  
Default host: `http://localhost:3000`  
Auth header (protected routes): `Authorization: Bearer <JWT>`

---

## Health

### `GET /api/health`

No auth.

**Response 200:**

```json
{ "ok": true }
```

---

## Authentication

### `POST /api/auth/register`

No auth.

**Body:**

```json
{
  "email": "user@example.com",
  "name": "Jane Doe",
  "password": "minimum8chars",
  "pin": "optional-server-pin"
}
```

| Field | Rules |
|-------|-------|
| `email` | Valid email |
| `name` | Min 1 char |
| `password` | Min 8 chars |
| `pin` | Optional, 4–12 chars (stored hashed server-side) |

**Response 200:**

```json
{
  "token": "<jwt>",
  "user": { "id": "...", "email": "...", "name": "..." }
}
```

**Errors:** `400` email in use / invalid payload

---

### `POST /api/auth/login`

No auth.

**Body:**

```json
{
  "email": "user@example.com",
  "password": "minimum8chars"
}
```

**Response 200:** Same shape as register.

**Errors:** `401` invalid credentials, `400` missing password

---

## User (`/api/user` — requires auth)

### `GET /api/user/me`

**Response 200:**

```json
{
  "id": "...",
  "email": "...",
  "name": "...",
  "displayName": "...",
  "platformUsernames": {
    "leetcode": "username",
    "duolingo": "username"
  },
  "hasPin": false,
  "blockedApps": ["com.instagram.android"],
  "goals": [
    {
      "id": "...",
      "platform": "LeetCode",
      "targetValue": 3,
      "unit": "lessons",
      "progress": 1
    }
  ]
}
```

---

### `PUT /api/user/me/preferences`

**Body:**

```json
{
  "displayName": "Jane",
  "usernames": {
    "leetcode": "leetcode_user",
    "duolingo": "duo_user"
  }
}
```

**Response 200:** Updated user object (shape from controller).

---

## Blocked apps

All list/mutation responses use:

```json
[{ "packageName": "com.example.app" }]
```

### `GET /api/user/me/blocked-apps`

Returns current list.

### `POST /api/user/me/blocked-apps`

**Body:** `{ "packageName": "com.example.app" }`  
Adds one package; returns full list.

### `DELETE /api/user/me/blocked-apps/:pkg`

Removes one package; returns full list.

### `PUT /api/user/me/blocked-apps`

**Body:**

```json
{
  "blockedApps": ["com.app.one", "com.app.two"]
}
```

Replaces entire list; returns full list.

---

## Goals (`/api/goals` — requires auth)

### `GET /api/goals`

**Response 200:** Array of goal DTOs (newest first).

### `POST /api/goals`

**Body:**

```json
{
  "platform": "LeetCode",
  "platformUsername": "handle",
  "targetValue": 3,
  "baselineValue": 0,
  "deadline": 1718971200000,
  "title": "Daily LeetCode",
  "unit": "lessons",
  "checkIntervalMs": 3600000,
  "evidence": null
}
```

| Field | Required | Notes |
|-------|----------|-------|
| `targetValue` | Yes | Positive integer |
| `platform` | No | Default `leetcode` in service |
| `baselineValue` | No | Default 0 |
| `checkIntervalMs` | No | Default 3600000 |

**Response 201/200:** Goal DTO.

**Goal DTO fields:**

```json
{
  "id": "...",
  "title": "...",
  "platform": "...",
  "platformUsername": "...",
  "unit": "...",
  "baselineValue": 0,
  "targetValue": 3,
  "progress": 0,
  "status": "active",
  "checkIntervalMs": 3600000,
  "lastCheckedAt": null,
  "completedAt": null,
  "completedByDevice": false,
  "evidence": null,
  "createdAt": "...",
  "updatedAt": "...",
  "deadline": 1718971200000
}
```

---

### `GET /api/goals/:id`

**Response 200:** Single goal DTO or `404`.

---

### `PUT /api/goals/:id`

**Body (at least one field):**

```json
{
  "title": "...",
  "targetValue": 5,
  "deadline": 1718971200000,
  "unit": "...",
  "checkIntervalMs": 3600000
}
```

**Response 200:** Updated goal DTO.

---

### `PATCH /api/goals/:id/progress`

**Body:**

```json
{ "progress": 2 }
```

`progress` is treated as **currentValue**; stored progress = `currentValue - baselineValue`.

Auto-completes if stored progress ≥ `targetValue`.

**Response 200:** Goal DTO (possibly completed).

---

### `POST /api/goals/:id/complete`

Rate limited (default 30/min).

**Body (optional):**

```json
{
  "completedAt": 1718971200000,
  "evidence": { "source": "android" },
  "details": {}
}
```

Note: Service uses server `new Date()` for `completedAt`; client timestamp may be ignored.

**Response 200:** Completed goal DTO. Writes `GoalCheckHistory`.

---

### `DELETE /api/goals/:id`

**Response 200/204:** Goal removed.

---

## Dashboard

### `GET /api/dashboard`

**Response 200:**

```json
{
  "goals": [ /* full GoalDto[] */ ],
  "blockedApps": [ { "packageName": "..." } ]
}
```

Used by Android `DashboardRepository.fetchDashboard()`.

---

## Error format

Typical error body:

```json
{ "message": "Human-readable error" }
```

| Code | When |
|------|------|
| 400 | Validation / bad input |
| 401 | Missing or invalid JWT |
| 404 | Resource not found |
| 429 | Rate limit (goal complete) |
| 500 | Server error |

---

## Android client mapping

| Android class | Endpoint |
|---------------|----------|
| `AuthApi` | `/api/auth/*` |
| `UserApi` | `/api/user/me`, preferences, blocked-apps |
| `GoalsApi` | `/api/goals/*` |
| `DashboardApi` | `/api/dashboard` |
| `LeetCodeValidator` | External — not backend |

---

## Related docs

- [Backend architecture](architecture.md)
- [Android networking](../android/README.md#networking)
