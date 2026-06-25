# Android app — Overview

**Package:** `com.example.oblique_android`  
**Display name:** Regretnt  
**Path:** `android/`

Native Kotlin app that enforces accountability through app blocking, local PIN unlock, and client-side LeetCode goal validation synced to the Oblique backend.

---

## Purpose

- Guide new users through onboarding (permissions, account, goals, app selection, PIN).
- Let users create **daily goals** for LeetCode (and Duolingo in UI only).
- **Block distracting apps** when protection is on.
- **Validate LeetCode progress** and push updates to the backend.
- Manage goals, blocked apps, and platform usernames in **Settings**.

---

## Requirements

- Android SDK (see `android/app/build.gradle.kts` for `compileSdk` / `minSdk`)
- Backend running and reachable from device/emulator
- Permissions: usage access, overlay, notifications (Android 13+)

---

## Setup

1. Copy `android/local.properties.example` → `android/local.properties`.
2. Set `sdk.dir` and `API_BASE_URL` (must end with `/`):

   ```properties
   sdk.dir=/path/to/Android/Sdk
   API_BASE_URL=http://10.0.2.2:3000/
   ```

3. Build and install:

   ```bash
   cd android
   ./gradlew assembleDebug
   ```

4. Install APK from `app/build/outputs/apk/debug/`.

---

## User guide (flows)

| Screen | How to reach | What it does |
|--------|--------------|--------------|
| Welcome | First launch | Intro; continues to permissions |
| Permissions | Onboarding | Grant usage, overlay, notifications |
| Login / Register | After permissions | Email/password auth |
| Goals | Onboarding / Settings → Add | Create platform goals with deadline |
| App list | After goals step | Select apps to block |
| PIN setup | After app list | 6-digit PIN (encrypted locally) |
| Dashboard | After onboarding | Protection toggle, stats, manual validation |
| Settings | Dashboard → gear | Goals, apps, preferences, reset PIN, sign out |

Detailed flows: [UI flows](ui-flows.md).

---

## Project structure

```
android/app/src/main/java/com/example/oblique_android/
├── activity/          # Screens (Activities)
├── adapters/          # RecyclerView adapters
├── models/            # Goal, ViewModels
├── network/           # Retrofit ApiClient, API interfaces
├── prefs/             # TokenManager, PlatformPref
├── repository/        # Auth, Goals, BlockedApps, Dashboard, App
├── services/          # MonitoringService, OverlayService, PINManager, Prefs
├── utils/             # OnboardingRouter, PermissionGuard, PrefsUtils, …
└── validation/        # GoalValidator, LeetCodeValidator, WorkManager
```

---

## Key classes

| Class | Role |
|-------|------|
| `ObliqueApp` | Application; initializes `Prefs`, WorkManager |
| `SplashActivity` | Cold start; `OnboardingRouter.nextSuspend()` |
| `OnboardingRouter` | Single navigation authority for onboarding/resume |
| `ApiClient` | Retrofit + JWT interceptor |
| `GoalsViewModel` | Goals + blocked apps LiveData; dashboard refresh |
| `MonitoringService` | Foreground service: block loop + schedule validation |
| `GoalValidator` | Orchestrates LeetCode check → backend sync |
| `PINManager` | Encrypted local PIN storage |
| `PermissionGuard` | Redirects to `PermissionsActivity` if missing grants |

---

## Networking

- Base URL: `BuildConfig.API_BASE_URL`
- Auth: `Authorization: Bearer <token>` via `TokenManager`
- LeetCode: direct HTTPS to `https://leetcode.com/graphql` (not proxied through backend)

---

## Related docs

- [Architecture & UML](architecture.md)
- [UI flows](ui-flows.md)
- [Goal validation](goal-validation.md)
- [Local data & security](data-storage.md)
- [System HLD](../architecture/system-overview.md)
- [Backend API](../backend/api-reference.md)
