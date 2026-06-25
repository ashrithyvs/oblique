# Oblique / Regretnt — Documentation

Native Android client + Node.js backend for an accountability app: users set daily goals on external platforms (LeetCode, Duolingo), block distracting apps, and validate progress from the device.

> **Note:** Older docs in `docs/oblique_full_context.md` and `docs/spec_1_oblique_working_draft.md` describe a React Native + Postgres aspirational design. The **implemented product** is the Kotlin app under `android/` and the Express + MongoDB API under `backend/`.

---

## Documentation map

| Document | Description |
|----------|-------------|
| [System overview (HLD)](architecture/system-overview.md) | End-to-end architecture, trust boundaries, deployment |
| **Android** | |
| [Android README](android/README.md) | Purpose, setup, usage, module layout |
| [Android architecture](android/architecture.md) | Layers, components, class diagrams, services |
| [UI flows](android/ui-flows.md) | Onboarding, dashboard, settings, PIN flows |
| [Goal validation](android/goal-validation.md) | LeetCode validation pipeline, scheduling, limits |
| [Local data & security](android/data-storage.md) | Prefs, encrypted PIN/token, blocked apps cache |
| **Backend** | |
| [Backend README](../backend/readme.md) | Quick start, env, tests |
| [Backend architecture](backend/architecture.md) | HLD, data model, middleware, diagrams |
| [API reference](backend/api-reference.md) | Routes, payloads, errors, rate limits |

---

## Repository layout

```
oblique/
├── android/          # Kotlin Android app (package: com.example.oblique_android)
├── backend/          # Express + TypeScript + MongoDB API
├── docs/             # This documentation set
└── mobile/           # Legacy RN shell (not the active client)
```

---

## Quick start (developer)

### Backend

```bash
cd backend
npm install
cp .env.example .env   # MONGO_URI, JWT_SECRET
npm run dev            # default :3000
```

### Android

```bash
cd android
cp local.properties.example local.properties
# Set sdk.dir and API_BASE_URL (emulator: http://10.0.2.2:3000/)
./gradlew assembleDebug
```

---

## Product summary

| Capability | Android | Backend |
|------------|---------|---------|
| Account (email/password) | Login, Register | JWT auth |
| Onboarding | Welcome → Permissions → Auth → Goals → Apps → PIN → Dashboard | Stores goals & blocked apps |
| Goal CRUD | GoalsActivity, Settings | `/api/goals` |
| Goal validation | LeetCode GraphQL on device | Stores progress; no platform verify |
| App blocking | MonitoringService + OverlayService | Blocked apps on User doc |
| Temp unlock | PIN → 5 min bypass per app | — |
| Preferences | Settings tab | `/api/user/me/preferences` |

---

## Related legacy docs

- [oblique_full_context.md](oblique_full_context.md) — Original RN/Postgres spec (partially outdated)
- [spec_1_oblique_working_draft.md](spec_1_oblique_working_draft.md) — Working draft spec
