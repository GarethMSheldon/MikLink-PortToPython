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
2) Default registry provider (Superseded — Won’t Do):
   - Motivo: il pattern attuale funziona ed è la verità del codice; la deduplica è rinviata a un refactor esplicito futuro.
   - Oggi: la registry è costruita localmente.
     - In app/src/main/java/com/app/miklink/ui/test/TestExecutionScreen.kt tramite funzione @Composable `rememberRendererRegistry()` con `remember { SectionRendererRegistry(mapOf(...)) }`.
     - In app/src/main/java/com/app/miklink/ui/history/ReportDetailScreen.kt inline con `remember { SectionRendererRegistry(mapOf(...)) }`.
     - Renderer inclusi (sintesi): NetworkSectionRenderer, LinkSectionRenderer, TdrSectionRenderer, NeighborsSectionRenderer, PingSectionRenderer, SpeedSectionRenderer.
3) Replace inline registry usage (Superseded — Won’t Do):
   - Non sostituire: mantenere l’uso locale come da codice attuale.
4) Remove UI ordering drift:
   - Modify: ui/test/TestSectionDisplayPolicy.kt to remove orderedIds/ordered() sorting
   - Update call sites to render snapshot.sections as-is
5) Update docs-as-code:
   - Modify: docs/reference/ui-architecture.md per riferire i punti reali di costruzione della registry (vedi file sopra) senza introdurre file centralizzati inesistenti

OUTPUT REQUIRED
- List each file changed/created and what moved.
- Mostrare conteggio aggiornato dei riferimenti nei docs (nessun riferimento a file centralizzati inesistenti; SectionRendererRegistry punta a file reali).
- Report test results after each step.
