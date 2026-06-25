# Backend — Architecture

Express + TypeScript + MongoDB API for the Regretnt Android client.

**Path:** `backend/src/`  
**Entry:** `index.ts` → `app.ts` → routes under `/api`

---

## HLD

```mermaid
flowchart TB
    Client[Android client]
    Express[Express app]
    AuthMW[requireAuth JWT]
    Ctrl[Controllers]
    Svc[Services]
    Models[Mongoose models]
    Mongo[(MongoDB)]

    Client -->|REST JSON| Express
    Express --> AuthMW
    AuthMW --> Ctrl
    Ctrl --> Svc
    Svc --> Models
    Models --> Mongo
```

---

## Layer responsibilities

| Layer | Path | Role |
|-------|------|------|
| Routes | `routes/*.routes.ts` | HTTP mapping, rate limits |
| Controllers | `controllers/*.controller.ts` | Request/response, status codes |
| Services | `services/*.service.ts` | Business logic, DB access |
| Models | `models/*.model.ts` | Mongoose schemas |
| Middleware | `middleware/auth.ts`, `errorHandler.ts` | JWT verify, errors |
| Utils | `utils/validators.ts`, `goalDto.ts`, `jwt.ts`, `hash.ts` | Zod, DTO mapping, crypto |

---

## Request lifecycle

```mermaid
sequenceDiagram
    participant C as Client
    participant E as Express
    participant A as requireAuth
    participant X as Controller
    participant S as Service
    participant M as MongoDB

    C->>E: HTTP request
    alt /api/auth/*
        E->>X: no auth
    else protected
        E->>A: verify JWT
        A->>X: req.user
    end
    X->>S: domain call
    S->>M: query
    M-->>S: document
    S-->>X: DTO
    X-->>C: JSON response
```

---

## Data model (ER)

```mermaid
erDiagram
    User ||--o{ Goal : owns
    User ||--o{ GoalCheckHistory : owns
    Goal ||--o{ GoalCheckHistory : audited_by

    User {
        ObjectId _id
        string email
        string name
        string displayName
        string[] blockedApps
        object platformUsernames
        string hashedPassword
        string hashedPin
        boolean onboardingCompleted
    }

    Goal {
        ObjectId _id
        ObjectId user
        string title
        string platform
        string platformUsername
        string unit
        number baselineValue
        number targetValue
        number progress
        string status
        number deadline
        number checkIntervalMs
        date lastCheckedAt
        date completedAt
        boolean completedByDevice
        mixed evidence
    }

    GoalCheckHistory {
        ObjectId _id
        ObjectId goal
        ObjectId user
        date checkedAt
        string result
        mixed details
        mixed evidence
    }
```

**Relationships:**

- Goals reference `user` ObjectId.
- Blocked apps are embedded on **User** (`blockedApps: string[]`), not a separate collection.
- `GoalCheckHistory` rows created on **completion** (`markComplete`), not on every progress PATCH.

---

## Authentication

- **Register:** `POST /api/auth/register` — bcrypt password, optional server PIN hash.
- **Login:** `POST /api/auth/login` — email + password required.
- **JWT:** `signJwt({ sub: userId, email })`, default expiry `7d` (`JWT_SECRET`, `JWT_EXPIRES_IN`).
- **Middleware:** `requireAuth` reads `Authorization: Bearer <token>`, attaches `req.user`.

Device unlock PIN used by Android is **local** (`PINManager`); server `hashedPin` is optional at register and not used by current Android unlock flow.

---

## Goal lifecycle (server)

```mermaid
stateDiagram-v2
    [*] --> active: POST /api/goals
    active --> active: PATCH progress
    active --> completed: POST complete OR progress >= target
    completed --> [*]
    active --> [*]: DELETE
```

**Progress update** (`updateGoalProgress`):

```
computedProgress = max(0, currentValue - baselineValue)
if computedProgress >= targetValue → markComplete(via: progressUpdate)
else → save progress
```

**Complete** (`markComplete`):

- Sets `status = completed`, `completedAt`, bumps `progress` to at least `targetValue`
- Writes `GoalCheckHistory` with `result: 'completed'`

---

## Validation philosophy

| Validated server-side | Not validated server-side |
|----------------------|---------------------------|
| JWT, Zod on create/register | LeetCode/Duolingo activity |
| ObjectId format | Truth of client-reported progress |
| Numeric progress type | Deadline enforcement |
| Rate limits on complete | Platform username correctness |

Client-side LeetCode GraphQL validation is **by design**; backend persists trusted snapshots.

---

## Rate limiting

| Endpoint | Config | Default |
|----------|--------|---------|
| Auth routes | `AUTH_RATE_LIMIT_*` | 20 / 15 min |
| `POST /api/goals/:id/complete` | `GOAL_COMPLETE_RATE_LIMIT_*` | 30 / 1 min |

Progress PATCH is not rate-limited.

---

## Configuration

Environment (`.env`):

| Variable | Purpose |
|----------|---------|
| `MONGO_URI` | MongoDB connection |
| `JWT_SECRET` | Signing key |
| `JWT_EXPIRES_IN` | Token TTL |
| `PORT` | Server port (default 3000) |
| `ALLOWED_ORIGINS` | CORS |
| `BCRYPT_SALT_ROUNDS` | Password hashing |
| `GOAL_COMPLETE_RATE_LIMIT_*` | Complete endpoint throttle |

---

## File map

```
backend/src/
├── app.ts                 # Express setup, CORS, routes mount
├── index.ts               # Listen, DB connect
├── config.ts              # Env constants
├── controllers/
│   ├── auth.controller.ts
│   ├── goals.controller.ts
│   ├── user.controller.ts
│   └── dashboard.controller.ts
├── services/
│   ├── goals.service.ts
│   └── user.service.ts
├── models/
│   ├── user.model.ts
│   ├── goal.model.ts
│   └── goalCheckHistory.model.ts
├── routes/
│   ├── index.ts           # /api mount
│   ├── auth.routes.ts
│   ├── goals.routes.ts
│   ├── user.routes.ts
│   └── dashboard.routes.ts
├── middleware/
│   ├── auth.ts
│   └── errorHandler.ts
└── utils/
    ├── validators.ts      # Zod schemas
    ├── goalDto.ts           # Response mappers
    ├── jwt.ts
    └── hash.ts
```

---

## Dashboard aggregation

`GET /api/dashboard`:

```javascript
{
  goals: await goalSvc.listGoalsForUser(userId),
  blockedApps: toBlockedAppDtos(await userSvc.getBlockedApps(userId))
}
```

Single round-trip for Android `DashboardRepository`.

---

## Known gaps / TODOs

| Item | Notes |
|------|-------|
| `completeGoalSchema` | Defined in validators, not wired in controller |
| `lastCheckedAt` | Field exists; never updated in services |
| `completedByDevice` | Supported in model; Android does not send |
| Progress audit | No `GoalCheckHistory` on PATCH |
| Server-side platform verify | Out of scope for MVP |

---

## Related docs

- [API reference](api-reference.md)
- [Backend README](../../backend/readme.md)
- [System overview](../architecture/system-overview.md)
- [Android goal validation](../android/goal-validation.md)
