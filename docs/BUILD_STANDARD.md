# GenSolar Pro Engineering — mandatory build and code-completion standard

This standard applies to every subsequent implementation task on the `main` branch of `VEERA0007/GenSolar-Pro-Engineering`. The deliverable is one integrated Android application, not separate feature demos or disconnected APKs.

## Focused implementation window
- Target at least five minutes of focused implementation work for each substantial build task when the execution environment permits.
- Five minutes is a minimum work target, not a timer, a guarantee of continuous execution, or a reason to pad a change. If execution limits prevent it, report the actual work completed and continue in the next task.
- Prefer meaningful vertical slices: implementation, integration, tests, documentation and verification. Do not spend the window on cosmetic-only changes or repeated status updates.

## Language and architecture
- Android application: Kotlin 2.0.21, Jetpack Compose Material 3, Gradle 8.9, AGP 8.7.3, JDK 17; retain the existing application ID and minimum SDK unless a documented compatibility decision requires change.
- Use Kotlin for Android UI, domain models, persistence, lifecycle, permissions and platform integrations.
- Use JavaScript for the bundled EPC engineering engine and browser workspace where it is the best fit; expose Android integration only through the controlled `GenSolarEpcBridge`.
- Use Python for build tooling, validation, data preparation and CI automation, not as a substitute for native Android runtime code.
- Use established, maintained libraries where they materially improve reliability. Pin versions, document licences and avoid speculative dependencies.
- Keep the app offline-first. All workspaces must use the same active project and explicit data contracts.

## Definition of complete code
- No TODO, FIXME, placeholder, mock-only result, dead button, fake progress, unhandled required branch or disconnected screen may be counted as complete.
- Every user-facing action must perform its stated operation, validate inputs, surface errors and persist successful changes.
- Engineering calculations must define units, assumptions, valid ranges, source/provenance and calculation version. Invalid or missing inputs must return REVIEW/INCOMPLETE, never an invented PASS.
- Shared project edits must invalidate dependent results; stale outputs must not be presented as current.
- Persisted data and exports must be revision-aware, validated and safe against malformed input and spreadsheet formula injection.
- Avoid claiming full AutoCAD, STAAD, PVsyst, DWG/SKP conversion, ray tracing or AR scanning unless the actual implementation and tests support that exact capability.
- Structural/electrical outputs remain preliminary until independently reviewed and signed off by qualified professionals.

## Required verification for every relevant change
1. Inspect the current source and latest `main` commit before editing.
2. Run formatting/static checks available in the repository.
3. Run Python modernization validator and all affected Node engineering regression suites.
4. Run Kotlin unit tests with `gradle testDebugUnitTest --stacktrace`.
5. Run HTML/embedded JavaScript syntax and unique-ID checks.
6. Build with `gradle assembleDebug --stacktrace`.
7. Verify APK package/application ID and signature with `apksigner` and `aapt`.
8. Inspect the uploaded APK artifact, record exact byte size and SHA-256, and link the exact successful Actions run.
9. Where a check cannot run, state it as pending; never infer success from a commit or empty status response.
10. Add regression tests for the defect/feature and keep CI assertions synchronized with the implementation.

## Integration and release discipline
- All features remain within the single repository and single Android application. Preserve the native navigation and shared project bridge.
- Do not count docs-only changes, validator-only changes, or disconnected screens as product feature progress.
- Do not increase the full application completion percentage until the relevant implementation, integration and tests are evidenced. Report feature-scope completion separately from CI/APK verification and physical-device/release readiness.
- Every implementation report includes: changed files, commit SHA/link, tests actually run, CI run/conclusion, APK artifact details when available, current full-app percentage, and remaining work.
- Work only on `main` unless the user explicitly authorizes a different branch. Never switch to the older `gensolar-pro` or `Roka-One` repositories.
