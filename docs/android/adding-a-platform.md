# Adding a platform validator

Checklist for integrating a new goal platform (e.g. Duolingo) into the validation pipeline.

## 1. Constants and catalog

- [ ] Add platform key to `utils/PlatformConstants.kt` (`KEY_*`, `iconMap`)
- [ ] Add entry to `utils/PlatformCatalog.kt` (key, displayName, icon)
- [ ] Add any platform-specific error codes to `utils/ValidationConstants.kt`

## 2. Platform validator

- [ ] Create `validation/platform/<Name>PlatformValidator.kt` implementing `PlatformGoalValidator`
- [ ] Return structured results: `Success`, `NoChange`, or `Failure(code)`
- [ ] Redact usernames in logs; never log raw credentials
- [ ] Handle network errors with retries where appropriate
- [ ] Register in `PlatformValidatorRegistry.defaultValidators()`

## 3. Scheduling

- [ ] Update `GoalValidationScheduleManager.isSchedulablePlatform()` if the platform supports automated checks
- [ ] Confirm `DefaultPlatformSchedulingPolicy` offsets are appropriate for the platform's deadline model

## 4. Backend

- [ ] Confirm `PATCH /api/goals/:id/progress` accepts evidence JSON from the validator
- [ ] Document expected `evidence` shape in API reference
- [ ] Optional: add `GoalCheckHistory` entries for no-change/error outcomes (see architecture doc)

## 5. UI

- [ ] Platform appears in `PlatformsAdapter` via `PlatformCatalog`
- [ ] Username field in Settings → Preferences via `PlatformPrefsAdapter`
- [ ] Dashboard icon via `PlatformCatalog.iconFor()`

## 6. Tests

- [ ] Unit tests for the new validator (mock HTTP where needed)
- [ ] Registry lookup test in `PlatformValidatorRegistryTest`
- [ ] Integration test path in `GoalValidationServiceTest` with mocked validator

## 7. Documentation

- [ ] Update `docs/android/goal-validation.md` platform matrix
- [ ] Update `docs/architecture/goal-validation.md` sequence diagrams if flow differs
