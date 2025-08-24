# SPEC-1-Oblique (Segregated Structure, package.json, and Retained Architecture)

## Background

Oblique is a cross‑platform mobile product that enforces a user’s self‑development goals by restricting access to distracting apps (primarily on Android) and using persuasive prompts on iOS. The app caches today’s and upcoming goals for offline continuity, persists partial progress, and syncs profiles, goals, progress, and blocking configurations to a cloud backend. Shared React Native UI; platform‑specific native services (Android Accessibility/overlays; iOS push/limited overlays).

## Architecture (PlantUML)

```plantuml
@startuml
skinparam componentStyle rectangle
package "Mobile App (React Native)" {
  [UI Layer]
  [State & Domain]
  [Sync Manager]
  [Local DB (Realm)]
  [Policy Engine]
  [Bridge: Android Native]
  [Bridge: iOS Native]
}
package "Android Native" {
  [Accessibility Service]
  [Overlay Service]
  [App Usage Monitor]
  [Foreground Service]
}
package "iOS Native" {
  [Notification Scheduler]
  [In-App Overlay]
}
package "Backend (Express.js)" {
  [Auth API (Firebase)]
  [Goals API]
  [Progress API]
  [Policy API]
  [Postgres DB]
}
[UI Layer] --> [State & Domain]
[State & Domain] --> [Policy Engine]
[State & Domain] --> [Local DB (Realm)]
[Sync Manager] --> [Auth API (Firebase)]
[Sync Manager] --> [Goals API]
[Sync Manager] --> [Progress API]
[Policy Engine] --> [Bridge: Android Native]
[Policy Engine] --> [Bridge: iOS Native]
[Bridge: Android Native] --> [Accessibility Service]
@enduml
```

## ER Diagram (PlantUML)

```plantuml
@startuml
entity User {
  *id: uuid
  email: varchar
  role: enum
  created_at: timestamp
}
entity Device {
  *id: uuid
  user_id: uuid
  platform: enum
  push_token: text
}
entity Goal {
  *id: uuid
  user_id: uuid
  title: varchar
  metric: enum
  target_value: int
  schedule_cron: varchar
}
entity DailyGoal {
  *id: uuid
  goal_id: uuid
  date: date
  target_value: int
  status: enum
}
entity ProgressEntry {
  *id: uuid
  daily_goal_id: uuid
  delta: int
  source: enum
  created_at: timestamp
}
entity BlockList {
  *id: uuid
  user_id: uuid
  package_name: varchar
}
entity AuditEvent {
  *id: uuid
  device_id: uuid
  event_type: varchar
  meta: json
  created_at: timestamp
}
User ||--o{ Device
User ||--o{ Goal
Goal ||--o{ DailyGoal
DailyGoal ||--o{ ProgressEntry
User ||--o{ BlockList
Device ||--o{ AuditEvent
@enduml
```

## Key Flows (short)

**App Blocking (Android)**: Accessibility detects app → Blocklist check → Goal status check → Show overlay → Log audit. **Goal Lifecycle**: Materialize daily goals at midnight → Save to Realm → Sync when online → Mark complete.

## Folder Structure

```
/oblique-monorepo
├── apps
│   ├── mobile
│   │   ├── android                # Native Android code
│   │   ├── ios                     # Native iOS code (placeholder)
│   │   ├── src
│   │   │   ├── assets
│   │   │   ├── components          # Pure UI components
│   │   │   ├── modules
│   │   │   │   ├── device-api      # Platform bridges
│   │   │   │   │   ├── android
│   │   │   │   │   └── ios
│   │   │   │   ├── local-db        # Realm schemas & repositories
│   │   │   │   └── cloud-sync
│   │   │   ├── services
│   │   │   ├── store               # Zustand stores
│   │   │   ├── hooks
│   │   │   ├── navigation
│   │   │   ├── utils
│   │   │   ├── App.tsx
│   │   │   └── index.js
│   │   ├── package.json
├── backend
│   ├── src
│   │   ├── controllers
│   │   ├── routes
│   │   ├── services
│   │   ├── db
│   │   ├── utils
│   │   └── index.js
│   ├── package.json
├── libs
│   ├── api-client
│   └── types
├── infra
│   └── docker-compose.yml
└── scripts
```

## package.json — Mobile

```json
{
  "name": "oblique-mobile",
  "version": "1.0.0",
  "private": true,
  "scripts": {
    "android": "react-native run-android",
    "start": "react-native start",
    "test": "jest",
    "lint": "eslint ."
  },
  "dependencies": {
    "@react-native-async-storage/async-storage": "^1.21.0",
    "@react-native-firebase/app": "^19.0.1",
    "@react-native-firebase/auth": "^19.0.1",
    "@react-native-firebase/messaging": "^19.0.1",
    "@react-navigation/native": "^6.1.9",
    "@react-navigation/stack": "^6.3.20",
    "axios": "^1.6.8",
    "react": "18.2.0",
    "react-hook-form": "^7.51.3",
    "react-native": "0.81.0",
    "react-native-gesture-handler": "^2.14.0",
    "react-native-permissions": "^3.10.1",
    "react-native-reanimated": "^3.6.2",
    "react-native-safe-area-context": "^4.9.0",
    "react-native-screens": "^3.31.1",
    "realm": "^12.6.0",
    "yup": "^1.3.2",
    "zustand": "^4.5.2"
  }
}
```

## package.json — Backend

```json
{
  "name": "oblique-backend",
  "version": "1.0.0",
  "main": "src/index.js",
  "type": "module",
  "scripts": {
    "dev": "nodemon src/index.js",
    "start": "node src/index.js"
  },
  "dependencies": {
    "axios": "^1.6.8",
    "cors": "^2.8.5",
    "dotenv": "^16.4.0",
    "express": "^4.19.2",
    "firebase-admin": "^12.4.0",
    "helmet": "^7.1.0",
    "morgan": "^1.10.0",
    "pg": "^8.11.3",
    "prisma": "^5.9.1"
  }
}
```

## Notes

- Realm DB only for mobile app.
- Device-specific APIs under `device-api/android` and `device-api/ios`.
- UI components are platform-agnostic.
- Backend is Express.js + Postgres + Firebase Auth verification.

