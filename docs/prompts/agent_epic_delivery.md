You are a Senior Android/Kotlin Maintainer.

TASK
Implement the EPIC “De-spaghetti Test Execution + Deduplicate Renderer Registry + Remove UI Ordering Drift” exactly as described in docs/EPIC-UI-TestExecution-Despaghettify-DedupRenderers.md.

NON-NEGOTIABLE RULES
- Do NOT invent or assume. Every decision must be backed by repo evidence.
- Minimum-change only. No stylistic refactors, no redesign.
- Do NOT comment out code or keep "backup_*.kt" files. If confirmed dead/unneeded, DELETE.
- After each structural step, STOP and run tests.

MANDATORY TESTS (after each step)
- ./gradlew :app:testDebugUnitTest
- ./gradlew :app:assembleDebug

STEPS (in this exact order)
1) Split:
   - Modify: ui/test/TestExecutionScreen.kt (entry + scaffold + delegation only)
   - Create: ui/test/TestExecutionRunningContent.kt (running UI moved mechanically)
   - Create: ui/test/TestExecutionCompletedContent.kt (completed UI moved mechanically)
2) Create default registry provider:
   - Create: ui/feature/test_details/DefaultSectionRendererRegistry.kt
   - Provide: @Composable rememberDefaultSectionRendererRegistry(): SectionRendererRegistry
3) Replace inline registry usage:
   - Modify: ui/history/ReportDetailScreen.kt to use rememberDefaultSectionRendererRegistry()
   - Modify: ui/test/TestExecutionScreen.kt to use rememberDefaultSectionRendererRegistry()
4) Remove UI ordering drift:
   - Modify: ui/test/TestSectionDisplayPolicy.kt to remove orderedIds/ordered() sorting
   - Update call sites to render snapshot.sections as-is
5) Update docs-as-code:
   - Modify: docs/reference/ui-architecture.md to reference the new registry file and reuse points

OUTPUT REQUIRED
- List each file changed/created and what moved.
- Show proof that inline registries are gone (search output summary).
- Report test results after each step.
