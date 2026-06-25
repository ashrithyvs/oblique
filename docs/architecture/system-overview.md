# System overview (HLD)

High-level design for the **Regretnt / Oblique** stack: native Android client and Node.js API.

---

## Purpose

Help users stay accountable by:

1. **Setting daily goals** tied to learning platforms (LeetCode, Duolingo in UI).
2. **Blocking selected apps** when “protection” is enabled.
3. **Validating LeetCode progress** on the device and syncing results to the cloud.
4. **Temporary unlock** via a local 6-digit PIN (5 minutes per blocked app).

The backend is the **system of record** for users, goals, blocked apps, and preferences. Platform activity verification runs **only on Android** (LeetCode GraphQL today).

---

## Context diagram

```mermaid
flowchart TB
    subgraph Client["Android app"]
        UI[Activities / ViewModels]
        Block[MonitoringService + OverlayService]
        Validate[GoalValidator + LeetCodeValidator]
        Local[Prefs / Encrypted PIN & JWT]
    end

    subgraph Backend["Oblique API"]
        API[Express REST]
        DB[(MongoDB)]
    end

    subgraph External["External"]
        LC[leetcode.com/graphql]
    end

    User((User)) --> UI
    UI --> API
    Validate --> LC
    Validate --> API
    Block --> Local
    UI --> Local
    API --> DB
```

---

## Trust boundaries

| Data / action | Source of truth | Verified by |
|---------------|-----------------|-------------|
| User credentials | Backend (bcrypt password) | Login/register |
| JWT session | Backend issues; Android `TokenManager` stores encrypted | `requireAuth` middleware |
| Goals & progress | Backend MongoDB | Client reports progress; **not** re-verified server-side |
| LeetCode solves | LeetCode API | Android `LeetCodeValidator` |
| Blocked app list | Backend User doc + local `PrefsUtils` cache | Sync on confirm/save |
| Device unlock PIN | Android `PINManager` (EncryptedSharedPreferences) | Local only |
| Temp unlock window | Android `TempUnlockManager` | Local only |

---

## Deployment topology

```mermaid
flowchart LR
    Phone[Android device]
    API[Backend :3000]
    Mongo[(MongoDB)]
    LC[LeetCode GraphQL]

    Phone -->|HTTPS REST JWT| API
    Phone -->|HTTPS no auth| LC
    API --> Mongo
```

**Android API URL:** `BuildConfig.API_BASE_URL` from `android/local.properties` (`API_BASE_URL`).

**Emulator → host:** `http://10.0.2.2:3000/`  
**Physical device:** `http://<LAN-IP>:3000/`

---

## Major subsystems

### Android

| Subsystem | Entry points | Responsibility |
|-----------|--------------|----------------|
| Onboarding | `SplashActivity`, `OnboardingRouter` | Route user to first incomplete step |
| Auth | `LoginActivity`, `RegisterActivity`, `AuthRepository` | JWT acquisition |
| Permissions | `PermissionsActivity`, `PermissionGuard` | Usage stats, overlay, notifications |
| Goals UI | `GoalsActivity`, `SettingsActivity` | Create/edit goals |
| Dashboard | `DashboardActivity`, `GoalsViewModel` | Stats, protection toggle, manual poll |
| App blocking | `MonitoringService`, `OverlayService`, `PinUnlockActivity` | Foreground detection, overlay, PIN unlock |
| Goal validation | `GoalValidationWorker`, `GoalValidator` | Scheduled + manual LeetCode checks |

See [Android architecture](../android/architecture.md).

### Backend

| Subsystem | Routes | Responsibility |
|-----------|--------|------------------|
| Auth | `/api/auth/*` | Register, login, JWT |
| User | `/api/user/me/*` | Profile, preferences, blocked apps |
| Goals | `/api/goals/*` | CRUD, progress, complete |
| Dashboard | `/api/dashboard` | Aggregated goals + blocked apps |

See [Backend architecture](../backend/architecture.md).

---

## Key integration flows

### 1. Registration & onboarding

```mermaid
sequenceDiagram
    participant A as Android
    participant B as Backend

    A->>A: Welcome → Permissions
    A->>B: POST /api/auth/register
    B-->>A: JWT + user
    A->>B: POST /api/goals
    A->>B: PUT /api/user/me/blocked-apps
    A->>A: Set local PIN
    A->>A: Dashboard
```

### 2. Goal validation (LeetCode)

```mermaid
sequenceDiagram
    participant A as Android GoalValidator
    participant LC as LeetCode GraphQL
    participant B as Backend

    A->>LC: recentAcSubmissionList
    LC-->>A: submissions in time window
    alt progress >= target
        A->>B: POST /api/goals/:id/complete
    else progress changed
        A->>B: PATCH /api/goals/:id/progress
    end
```

### 3. App blocking (independent of goals)

```mermaid
sequenceDiagram
    participant MS as MonitoringService
    participant OS as OverlayService
    participant PU as PinUnlockActivity

    loop every 10s
        MS->>MS: foreground app in blocklist?
        alt blocked and not temp-unlocked
            MS->>OS: show overlay
            OS->>PU: user enters PIN
            PU->>PU: TempUnlockManager 5 min
        end
    end
```

**Important:** Goal completion does **not** automatically unblock apps in the current implementation.

---

## Non-goals (current MVP)

- Server-side LeetCode/Duolingo verification
- Duolingo validation on Android
- Goal-gated app blocking (blocking uses PIN only)
- iOS client
- React Native active client (`mobile/` is not wired to this backend flow)

---

## Glossary

| Term | Meaning |
|------|---------|
| Protection | User-enabled monitoring via `MonitoringService` foreground service |
| Temp unlock | 5-minute bypass for one blocked app after correct PIN |
| Baseline | `baselineValue` on goal; progress = reported value − baseline |
| App selection done | Local pref set when user confirms blocked apps in onboarding |
