# Regretnt Backend

Node.js + TypeScript + Express + MongoDB backend for the Regretnt (Oblique) Android app.

## Full documentation

| Doc | Description |
|-----|-------------|
| [Architecture](../docs/backend/architecture.md) | HLD, data model, layers, diagrams |
| [API reference](../docs/backend/api-reference.md) | All routes, payloads, errors |
| [System overview](../docs/architecture/system-overview.md) | End-to-end client + server |
| [Android client](../docs/android/README.md) | Mobile app docs |

## Features

- JWT authentication (email + password register/login).
- Optional PIN stored server-side on register (device unlock PIN is local on Android).
- Goals CRUD with progress tracking and completion audit history.
- Blocked apps list embedded per user.
- User preferences (display name, platform usernames for LeetCode/Duolingo).
- Aggregated dashboard endpoint (`GET /api/dashboard`).
- Rate limiting on auth and goal completion.
- Client-side goal validation (LeetCode GraphQL runs on Android; backend stores results).

## API overview

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/register` | No | Create account |
| POST | `/api/auth/login` | No | Login with password |
| GET | `/api/user/me` | Yes | Profile + goals summary + platform usernames |
| PUT | `/api/user/me/preferences` | Yes | Update display name / platform usernames |
| GET/PUT/POST/DELETE | `/api/user/me/blocked-apps` | Yes | Blocked app list CRUD |
| GET/POST/PUT/PATCH/DELETE | `/api/goals` | Yes | Goal CRUD + progress + complete |
| GET | `/api/dashboard` | Yes | Goals + blocked apps in one call |

All blocked-app mutations return `[{ "packageName": "..." }]`.

## Requirements

- Node.js >= 18
- MongoDB (local or Atlas)

## Setup

```bash
cd backend
npm install
cp .env.example .env   # set MONGO_URI, JWT_SECRET
npm run dev
```

## Tests

```bash
npm test
```

### Test layout

```
backend/tests/
├── setup/
│   ├── jest.setup.ts
│   ├── global-setup.ts
│   └── global-teardown.ts
└── unit/
    ├── controllers/
    │   ├── auth.controller.spec.ts
    │   ├── dashboard.controller.spec.ts
    │   ├── goals.controller.spec.ts
    │   └── user.controller.spec.ts
    ├── middleware/
    │   └── auth.middleware.spec.ts
    ├── services/
    │   ├── goals.service.spec.ts
    │   └── user.service.spec.ts
    └── utils/
        ├── goalDto.spec.ts
        ├── hash.spec.ts
        ├── jwt.spec.ts
        └── validators.spec.ts
```

Coverage thresholds (see `jest.config.js`):

| Scope | Minimum |
|-------|---------|
| Global | 75% |
| `goals.service.ts` | 90% |
| `goals.controller.ts` | 85% |

## Android integration

- Backend base URL: set `API_BASE_URL` in `android/local.properties` (see `android/local.properties.example`). The app reads it via `BuildConfig.API_BASE_URL` for all build types; there are no hardcoded URLs in Gradle.
- Auth header: `Authorization: Bearer <token>`.

See [docs/android/README.md](../docs/android/README.md) for client setup.
