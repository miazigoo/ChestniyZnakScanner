# CODEX TASK 01: TSD Reliability Implementation Report

Date: 2026-07-12
Android repo baseline SHA: 557fbdccd1e47f65e2a8987e66da8b3409e1e4d8
Backend repo baseline SHA: 38bbb27ce40755e303d7fad448c9ed38dac960f8

## Baseline

- `git pull --ff-only` completed in all known project repos; all were already up to date before changes.
- Android baseline: `./gradlew testDebugUnitTest lintDebug assembleDebug` passed before implementation.
- Android i18n baseline: `python3 scripts/check_i18n.py` passed, 509 keys across 3 languages.
- Backend baseline: full `.venv/bin/pytest` passed, 234 tests.
- Backend quality baseline was not clean before this task: black/isort/flake8/mypy reported pre-existing issues in backend files unrelated to this implementation.

## Implemented

- Removed release demo seed data: `seed/chestniy_znak_codes.json` moved from `main` assets to `debug` assets, and `ensureSeedData` is gated by `BuildConfig.DEBUG`.
- Disabled Android app backup: `android:allowBackup="false"` and WorkManager startup override added correctly.
- Added explicit `API_MODE` build config and same-origin checks for auth/TSD URL rewrite.
- Restricted bearer auth to same API origin and non-public paths only.
- Added encrypted bearer token storage using Android Keystore AES/GCM with one-time migration from old plaintext SharedPreferences value.
- Hardened token refresh with single-flight synchronization and no token clearing on network/5xx transient failures.
- Stopped treating 403 as logout in Android repositories; only 401 invalidates session.
- Added cached SaaS auth session restore so transient bootstrap failures keep the operator authenticated locally when a previous valid session exists.
- Added separate unauthenticated APK download OkHttp client and same-origin validation for APK download URLs.
- Added `package_uuid` to Android DTO/domain/UI mapping and unit coverage.
- Added Room DB v3:
  - `scopeKey` on `marking_codes` and `scan_logs`.
  - unique code index changed from global `rawCodeSha256` to `(scopeKey, rawCodeSha256)`.
  - `sync_events` table.
  - `local_scopes` table.
  - manual migration `2 -> 3` and exported schema `3.json`.
- Scoped local code pools, verification, pending packing, duplicate checks, commit/remove operations by current plant/supplier/client scope.
- Made local packing scan/remove atomic from the UI perspective: UI no longer shows success if Room update failed.
- Preserved current box UI on transient current-box refresh failures instead of clearing it to null.
- Added durable local sync event insertion for local `code.scan` and local remove audit events.
- Added WorkManager + Hilt Worker to send ready `code.scan` events to `/api/v1/tsd/sync/events` with retry/backoff and terminal status handling.
- Added backend contract assertions that legacy TSD current/list/print responses include `package_uuid`.

## Verification

- Android: `./gradlew testDebugUnitTest` passed.
- Android: `./gradlew lintDebug` passed.
- Android: `./gradlew assembleDebug` passed.
- Android i18n: `python3 scripts/check_i18n.py` passed.
- Android whitespace: `git diff --check` passed.
- Backend targeted: `.venv/bin/pytest tests/test_tsd_bootstrap.py tests/test_tsd_sync.py` passed, 4 tests.
- Backend whitespace: `git diff --check` passed.

## Remaining Risks / Follow-up

- Backend has no `code.remove` sync event type today. Android stores local remove events for durability/audit and marks unsupported events terminal in the worker instead of retrying forever. If true offline remove sync is required, backend schema/service must add a supported remove event.
- Pending/conflict counters are stored in `sync_events`, but not yet surfaced in the TSD UI chrome.
- `local_scopes` table exists for the Room contract; current active scope source is SharedPreferences-backed `LocalScopeStore` because it is needed before Room access during auth restore.
- Full backend quality suite still has pre-existing black/isort/flake8/mypy issues unrelated to this task.
