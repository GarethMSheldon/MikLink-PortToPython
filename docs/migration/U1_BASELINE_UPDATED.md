# U1 Baseline - Updated

This file contains an updated summary of the baseline and the changes made in the U1 EPIC iteration.

Baseline Summary:
- Data: 2025-12-12
- Branch locale: Non visibile
- Commands executed (baseline):
  - .\gradlew.bat :app:kspDebugKotlin — Esito: PASS — Log: docs/migration/U1_ksp_baseline.txt
  - .\gradlew.bat assembleDebug — Esito: PASS — Log: docs/migration/U1_assemble_baseline.txt
  - .\gradlew.bat testDebugUnitTest — Esito: PASS — Log: docs/migration/U1_tests_baseline.txt

Updates since baseline:
- Fixed unbalanced Kotlin braces in `TestProfileEditScreen.kt` blocking KSP.
- Restored file from HEAD after accidental duplication and re-applied scoped i18n changes.
- Implemented `TestSectionTitleMapper` and mapped titles for `TestExecutionScreen` and `ReportDetailScreen`.
- Added localized strings for raw log toggles and update code to use `stringResource(...)` for toggles.
- Added navigation icons and localized `back` content description for `profile_list`, `client_list`, `probe_list` screens, as well as `profile_edit`, `client_edit`, and `probe_edit` screens.
- Added instrumentation UI tests for toggle behavior: `TestExecutionToggleTest.kt`.

Next steps:
- Re-run KSP/Assemble/Unit Tests and record logs in `docs/migration/`.
- Run instrumentation tests on device/emulator (connectedAndroidTest) to validate toggle behavior on actual UI.
- Expand localization coverage in subsequent steps for more UI labels.

