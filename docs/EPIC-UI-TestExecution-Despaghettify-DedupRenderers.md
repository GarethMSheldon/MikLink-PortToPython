# EPIC — De-spaghetti Test Execution + Deduplicate Renderer Registry + Remove UI Ordering Drift

**Codename:** EPIC-UI-TestExecution-Despaghettify-DedupRenderers-2025-12  
**Owner:** Kotlin Senior / Maintainer  
**Scope:** UI test execution (live), history report detail, renderer wiring, docs-as-code update  
**Strict mode:** minimum-change, proof-driven, delete-means-delete

---

## 0) Mandatory reading (before touching code)

- `.github/copilot-instructions.md` (no assumptions, no drift)
- `docs/reference/ui-architecture.md` (feature-first targets + renderer reuse)
- `docs/decisions/ADR-0011-typed-test-execution-contract.md` (typed snapshot + single renderer approach)
- `docs/decisions/ADR-0007-package-structure-and-naming.md` (package/structure guardrails)

---

## 1) Problems (with concrete evidence)

### P1 — Renderer registry is duplicated inline (drift risk)
**Where observed:** `ui/history/ReportDetailScreen.kt` builds `SectionRendererRegistry(renderers = mapOf(...))` inline.

**Why it’s bad:** drift between Live/History/Report pipelines. Every time a renderer changes, it must be updated in multiple call sites.

### P2 — Section ordering is duplicated in UI (business rule drift)
**Where observed:** `ui/test/TestSectionDisplayPolicy.kt` keeps an `orderedIds` list and sorts sections.

**Why it’s bad:** domain/test runner already guarantees deterministic ordering. UI re-ordering creates rule duplication and eventual mismatch.

### P3 — TestExecutionScreen is a UI monolith (spaghetti/hotspot)
**Where observed:** `ui/test/TestExecutionScreen.kt` contains entry/scaffold + running + completed + helpers in one large file.

**Why it’s bad:** merge conflicts, review difficulty, accidental coupling; slows down future changes.

---

## 2) Non-goals

- No UX redesign, no new navigation routes
- No broad package renames or architecture rewrites
- No “backup_*.kt” or commented-out code to preserve old logic (Git is the backup)

---

## 3) Guardrails (must follow)

### GR1 — No assumptions
Do not infer usage from build caches, generated sources, or annotations alone.

### GR2 — File header comment required
Every **modified or newly created** Kotlin file must include the standard header comment (see template in this bundle).

### GR3 — Delete means delete
If something is confirmed dead: delete it. Do not comment it out or move to “backup”.

### GR4 — Test after each elimination / structural change
After each deletion or structural step:
- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:assembleDebug`
(Optionally run lint at the end if lint is known to be pre-broken; otherwise include lint per team standard.)

---

## 4) Stories (specific file plan)

### Story 1 — Split TestExecutionScreen into coherent files (no behavior changes)

**Modify**
- `app/src/main/java/com/app/miklink/ui/test/TestExecutionScreen.kt`

**Create**
- `app/src/main/java/com/app/miklink/ui/test/TestExecutionRunningContent.kt`
- `app/src/main/java/com/app/miklink/ui/test/TestExecutionCompletedContent.kt`

**Rules**
- Keep public entry API unchanged (route, parameters).
- Move code mechanically: no UI changes, no logic changes.
- Keep only “entry + scaffold + delegation” in `TestExecutionScreen.kt`.

**Expected responsibilities**
- `TestExecutionScreen.kt`: collects state, owns scaffold/top bar, delegates to running/completed content.
- `TestExecutionRunningContent.kt`: contains the running UI and any helpers used only by running.
- `TestExecutionCompletedContent.kt`: contains the completed UI and any helpers used only by completed.

**Acceptance**
- Build and unit tests pass.
- No changes in runtime behavior.

---

### Story 2 — Centralize the default renderer registry (deduplicate wiring)

**Create**
- `app/src/main/java/com/app/miklink/ui/feature/test_details/DefaultSectionRendererRegistry.kt`

**Implementation**
- Provide: `@Composable fun rememberDefaultSectionRendererRegistry(): SectionRendererRegistry`
- Use `remember { SectionRendererRegistry(mapOf(...)) }`
- The map must match the current set of renderers used in `ReportDetailScreen.kt`.

**Modify**
- `app/src/main/java/com/app/miklink/ui/history/ReportDetailScreen.kt`: replace inline registry creation with `rememberDefaultSectionRendererRegistry()`.
- `app/src/main/java/com/app/miklink/ui/test/TestExecutionScreen.kt`: use the same provider instead of any inline creation.

**Acceptance**
- No inline `SectionRendererRegistry(mapOf(...))` remains in screens.
- Build + unit tests pass.

---

### Story 3 — Remove UI ordering drift (stop duplicating business rules)

**Modify**
- `app/src/main/java/com/app/miklink/ui/test/TestSectionDisplayPolicy.kt`

**Change**
- Remove `orderedIds` and `ordered(...)` sorting.
- Update call sites (running/completed content) to render `snapshot.sections` as-is.

**Acceptance**
- UI shows domain-provided order.
- Build + unit tests pass.

---

### Story 4 — Docs-as-code update (must be true and concrete)

**Modify**
- `docs/reference/ui-architecture.md`

**Add/update**
- Mention that the default registry lives in:
  `ui/feature/test_details/DefaultSectionRendererRegistry.kt`
- Mention reuse from:
  - `TestExecutionScreen` (live)
  - `ReportDetailScreen` (history)

**Acceptance**
- Doc references real, existing paths.
- No aspirational statements.

---

## 5) Definition of Done (DoD)

- Registry deduplicated via `rememberDefaultSectionRendererRegistry()`
- TestExecutionScreen split into 3 files as specified
- UI section ordering drift removed (no sorting list in UI policy)
- Docs updated to reflect real code
- All touched/created Kotlin files have required header comment
- Build + unit tests pass
