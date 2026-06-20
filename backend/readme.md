# Regretnt Backend

Node.js + TypeScript + Express + MongoDB backend for the Regretnt (Oblique) Android app.

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
| GET/PUT | `/api/user/me/blocked-apps` | Yes | List or replace blocked apps |
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

## Android integration

- Backend base URL: set `API_BASE_URL` in `android/local.properties` (see `android/local.properties.example`). The app reads it via `BuildConfig.API_BASE_URL` for all build types; there are no hardcoded URLs in Gradle.
- Auth header: `Authorization: Bearer <token>`.
