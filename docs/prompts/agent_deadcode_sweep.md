You are a Senior Android/Kotlin Maintainer. Perform a proof-driven dead code sweep.

ABSOLUTE RULES
- Do NOT invent. Do NOT assume. Use only repo evidence.
- Do NOT use build caches / generated / ksp / build directories as evidence.
- If you confirm an item is dead, you must DELETE it (no comments, no backups).
- After EACH deletion: STOP and run the full test cycle.

SEARCH SCOPE (only these)
- app/src/main/java
- app/src/test/java
- app/src/androidTest/java

EXCLUDE ALWAYS
- **/build/**
- **/.gradle/**
- **/generated/**
- **/ksp/**

CANDIDATE LIST (verify each item from scratch)
- app/src/main/java/com/app/miklink/core/domain/report/ReportAggregator.kt
- app/src/main/java/com/app/miklink/data/remote/mikrotik/infra/AuthInterceptor.kt
- app/src/main/java/com/app/miklink/utils/ConnectivityProvider.kt
- app/src/main/java/com/app/miklink/utils/RateParser.kt
- app/src/main/java/com/app/miklink/ui/components/InfoCard.kt
- app/src/main/java/com/app/miklink/ui/components/ProbeStatusDot.kt
- app/src/main/java/com/app/miklink/ui/components/StatusBadge.kt
- app/src/main/java/com/app/miklink/ui/components/StepTimeline.kt
- app/src/main/java/com/app/miklink/ui/components/ValueDisplay.kt
- app/src/main/java/com/app/miklink/ui/probe/ProbeEditUiState.kt
- app/src/main/java/com/app/miklink/ui/feature/test_details/TestDetailsContent.kt
- app/src/main/java/com/app/miklink/data/repository/RouteManager.kt (listRoutes)
- app/src/main/java/com/app/miklink/data/remote/mikrotik/service/MikroTikCallExecutor.kt (remove only deprecated API that is truly unused)
- strings: test_toggle_show_raw_logs, test_toggle_hide_raw_logs, test_logs_title (remove only if unused)

TESTS (after each deletion)
- ./gradlew :app:testDebugUnitTest
- ./gradlew :app:lintDebug
- ./gradlew :app:assembleDebug

OUTPUT REQUIRED (for each item)
- Evidence: the exact rg command + summary of matches (paths/count)
- Decision: DELETED or KEPT
- If deleted: test outputs (pass/fail)
- If kept: exact consumer location (file+line) proving real usage
