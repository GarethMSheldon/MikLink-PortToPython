EPIC: UI/UX Consistency + Data Integrity (Profiles/Clients/Reports) + Restore “History Detail v2”

Codename: EPIC-UX-DATA-2025-12
Owner: Kotlin Senior / Maintainer
Target: Stabilize UX, unify formatting, eliminate DB crashes, restore the “ordered and rich” history detail screen, with zero technical debt and SOLID compliance.

Goals

Test Completed Screen: glow behind the PASS/FAIL icon (do not animate the icon), cleaner and more coherent UI.

Details section (Network/Link/Ping/LLDP/CDP): no raw lowercase keys, no ugly raw values, no useless fields like count=1.

Ping: remove LOSS 0% badge in the tab (or make it consistent with others), and improve Targets layout as requested.

DNS: must not be “empty” if retrievable from the routerboard/probe (add robust fallback).

DB crash: eliminate SQLiteConstraintException on:

enabling TDR in the “Full Test” profile

changing probe from DHCP to Static / saving client

Socket ID autoincrement: clear and working behavior (dashboard + after saving test).

History detail screen “nice”: find the branch/commit where it exists, bring it into HEAD with SOLID refactor.

Non-Goals

Total rework of the design system.

Change persisted formats/JSON without a handled migration/resultFormatVersion.

Restore code “as-is” from old branches without adaptation (avoid reintroducing debt).

Guard-Rails Anti-Drift (mandatory)

GR1 — Header comment on every modified file
At the top of every touched file, add a standard commented header:

Purpose

Inputs

Outputs

Notes/Constraints

GR2 — Single source of truth for “label/value formatting”
Forbidden to do .replace()/.capitalize() scattered in the UI.
Create a single formatter (see Story 2) and use it everywhere.

GR3 — No “insert” during edit
In edit (client/profile) never call insert with an existing PK.
Use update or a “Save/Upsert” use-case (see Story 5). This avoids the already observed crashes.





GR4 — Socket-ID policy
Do not duplicate parsing/increment logic: use the centralized policy SocketIdLite.

dump_core_domain

GR5 — Docs-as-code

# EPIC: UI/UX Consistency + Data Integrity (Profiles/Clients/Reports) + Restore “History Detail v2”

## Codename

**EPIC-UX-DATA-2025-12**

**Owner:** Kotlin Senior / Maintainer

**Target:** Stabilize UX, unify formatting, eliminate DB crashes, restore the “ordered and rich” history detail screen, with zero technical debt and SOLID compliance.

---

## Goals

- Test Completed Screen: glow behind the PASS/FAIL icon (do not animate the icon), cleaner and more coherent UI.
- Details section (Network/Link/Ping/LLDP/CDP): no raw lowercase keys, no ugly raw values, no useless fields like count=1.
- Ping: remove LOSS 0% badge in the tab (or make it consistent with others), and improve Targets layout as requested.
- DNS: must not be “empty” if retrievable from the routerboard/probe (add robust fallback).
- DB crash: eliminate SQLiteConstraintException on:
	- enabling TDR in the “Full Test” profile
	- changing probe from DHCP to Static / saving client
- Socket ID autoincrement: clear and working behavior (dashboard + after saving test).
- History detail screen “nice”: find the branch/commit where it exists, bring it into HEAD with SOLID refactor.

---

## Non-Goals

- Total rework of the design system.
- Change persisted formats/JSON without a handled migration/resultFormatVersion.
- Restore code “as-is” from old branches without adaptation (avoid reintroducing debt).

---

## Guard-Rails Anti-Drift (mandatory)

### GR1 — Header comment on every modified file
At the top of every touched file, add a standard commented header:

```
Purpose

Inputs

Outputs

Notes/Constraints
```

### GR2 — Single source of truth for “label/value formatting”

Forbidden to do .replace()/.capitalize() scattered in the UI.
Create a single formatter (see Story 2) and use it everywhere.

### GR3 — No “insert” during edit

In edit (client/profile) never call insert with an existing PK.
Use update or a “Save/Upsert” use-case (see Story 5). This avoids the already observed crashes.

### GR4 — Socket-ID policy

Do not duplicate parsing/increment logic: use the centralized policy `SocketIdLite`.

dump_core_domain

### GR5 — Docs-as-code

Every UI/UX change must update docs in `/docs` + (if needed) ADR (especially for log/UX).

---

## Definition of Done (DoD)

- ✅ No reproducible crash in the reported cases (profiles/clients).
- ✅ No label “mode/interface/status” in lowercase: everything formatted or localized.
- ✅ Ping Targets rendered with the requested layout.
- ✅ DNS populated when available; if not available show consistent placeholder (“—” or “Not available”) without empty strings.
- ✅ Test Completed Screen: glow on the background of the PASS/FAIL icon.
- ✅ Instrumental/UI tests updated or created to cover the changes.
- ✅ Docs updated + changelog/ADR if needed.

---

## Story 1 — Test Completed Screen polish (Glow + Card socket + Logs language)

### Problems

- Glow/animation today is on the icon, requested glow behind.
- Socket card: either make it coherent (adding PASS/FAIL indicators per section) or remove it.
- “Show logs” OK, but text mixed IT/EN.

### Tasks

**Glow background**

- Identify the composable that draws PASS/FAIL icon on completed screen.
- Move animation from scale(icon) to glow behind:
	- use Box with drawBehind { drawCircle(...) } or shadow/blur on a layer behind the icon.
	- animate only alpha/radius of the glow (not the icon size).
- Acceptance: static icon, pulsing glow behind.

**Socket card: decision and implementation**

- Suggestion: keep it, because it gives context (socket) and makes it immediate “which test am I looking at”.
- But it must be “finished”: add a mini-row of indicators for each test/section (e.g., Network, Link, Ping, LLDP, TDR, Speed):
	- each chip: section icon + ✓ (pass) / ✕ (fail) + (optional) color.
	- if a section was not executed: state “—” or “Skipped”.
	- If the row makes the card “too full”, fallback: remove the card and move socket/number into the top summary, but choose one path and update docs.

**Logs: consistent language**

- Set rule: raw logs = English-only (recommended) or “locale-aware”.
- To avoid debt, choose English-only (raw logs = technical diagnostics).
- Audit: find all sources emitting log lines (use case/steps) and unify the text (no mixing).
- Forbidden: use localized UI strings in raw logs: raw logs are not UX copy.

### Test

- UI test: completed PASS and FAIL -> glow present (snapshot/compose test if possible).
- UI test: toggle logs -> content does not contain obvious mix (“Completato”, “Errore”, etc.) if you have a stable fixture set.

### Docs

- Update “Test Completed” screen doc with note about glow + card + log language policy.

### Implementation (current)

- Completed screen now uses a pulsing glow behind a static PASS/FAIL icon (no icon scaling). Raw log fallbacks in `RunTestUseCaseImpl` were normalized to English-only (“Unknown error”, etc.).

---

## Story 2 — Report Detail Screen: label casing, value formatting, removals (count, link-ok, etc.)

### Problems

- “Network” tab: keys in lowercase (mode/interface…).
- “cdp” shows count=1 (noise).
- “Link”: status, link-ok not human friendly.

### Tasks

**Create single formatter**

- New file (UI layer): `ui/format/SectionDetailFormatter.kt`

**Proposed API:**

```
fun formatLabel(section: SectionId, rawKey: String): UiText

fun formatValue(section: SectionId, rawKey: String, rawValue: String): UiText
```

**Rules:**

- mapping known keys → clean (and localized) labels.
- fallback: Title Case + replace -/_ with space.
- values: mapping for known tokens (dhcp→DHCP, link-ok→Link OK, etc.).
- Forbidden: apply scattered transformations in composables.

**Remove or transform “count”**

- CDP/LLDP section: do not show count=X as a row.
- Option: show “Neighbors: X” only if X>0, otherwise do not show.
- Implement in formatter or section renderer (formatter preferred for consistency).

**Unify Tab titles and fields**

- All titles/labels must be consistent (Title Case, or localized).
- Add/reuse strings in `strings.xml` (EN/IT).

### Test

- Unit test formatter:
	- input “mode” => “Mode”
	- “link-ok” => “Link OK”
	- “count” => hidden/handled (assert output “null/empty UiText” if you handle it that way)

### Docs

- Update “History report details” doc indicating UI does not show raw keys.

### Implementation (current)

- Added `ui/format/SectionDetailFormatter.kt` and wired Test Completed/Execution details to it (labels title-cased/localized; LLDP/CDP `count<=1` hidden; `link-ok`/`dhcp` tokens humanized).

---

## Story 3 — Ping UX: remove tab badge + Targets layout as requested

### Problems

- Ping tab has LOSS 0% badge (not consistent with other tabs).
- Targets shown “raw” after targets.

### Tasks

**Ping badge**

- Remove badge from Ping tab or make it consistent with others (but you asked removal → do that).
- Move Loss info into Ping body (summary box).

**Targets: layout**

- Implement rendering per target with blocks:
	- Target #1 → resolvedIp (sourceTarget) where sourceTarget can be “DHCP_GATEWAY” or other.
	- Row below: Loss X%
	- Separator
- Important: do not parse strings “loss=… min=…”.
- Use structured data already present in the report model (if not available, create a mapping in domain/report decode without changing JSON).
- Add mapping “DHCP_GATEWAY” → label “DHCP Gateway” (localized string).

### Test

- Snapshot/compose test Ping section: 2 targets, layout as requested.

### Docs

- Update Ping doc: no tab badge, targets layout.

---

## Story 4 — DNS “empty”: retrieve from routerboard + robust fallback

### Problem

- DNS not populated (in some modes/config).

### Tasks

- Identify where DNS detail is built in the report (step/config/mapper).
- Add DNS retrieval from probe/routerboard:
	- Data layer: add MikroTik endpoint `/ip/dns` (or equivalent used in the project) and minimal DTO.
	- Domain: repository interface `DnsRepository` (SOLID: DIP).
	- Use case/step: if DNS missing in already computed details, fallback to `DnsRepository.getServers()`.
	- UI: if still not available, show placeholder “—” (not empty string).

### Test

- Unit test repository/step with fake response: DNS populated.
- Regression: if endpoint fails, no crash.

### Docs

- Update docs: “DNS source of truth”.

---

## Story 5 — Fix DB crash: Profile edit + Client edit (UNIQUE constraint)

### Problems (reproducible)

- UNIQUE constraint failed: test_profiles.profileId when enabling TDR in full test.
- UNIQUE constraint failed: clients.clientId when switching probe/client to static.

### Root cause (from code)

- In profile edit, an object is built with profileId = entityId but insertProfile(...) is called instead of update.

- PK autogenerate + insert ABORT makes crash inevitable if you insert an id that already exists.

### Tasks

- Introduce use-case “SaveTestProfileUseCase”
	- if (profile.profileId == 0L) insert else update
	- UI calls use-case, not repository directly.
	- Update `TestProfileEditViewModel`
	- Replace `insertProfile(profile)` with `saveTestProfileUseCase(profile)`

- Same for Client
	- Introduce `SaveClientUseCase` with same logic (insert vs update).
	- Update `ClientEditViewModel` (or equivalent).

**DB seeding / Full Test**

- Verify “Full Test” is still `runTdr=true` as per seed
- If user already has “dirty” DB, add a safe routine (non-invasive) like:
	- if a profile named “Full Test” exists and `runTdr=false`, do not overwrite without consent; but in debug/dev you can force or document “reset DB”.

### Test

- Unit test use-case: update does not call insert.
- Instrumented test: edit profile and save → no crash.

### Docs

- Document “save = upsert via use-case” pattern.

### Implementation (current)

- Implemented `SaveClientUseCase` and `SaveTestProfileUseCase` with DI bindings; edit view models call these upsert use cases to avoid PK/UNIQUE crashes.

---

## Story 6 — Socket ID autoincrement: end-to-end verification and fix

### Problem

- After saving test, dashboard does not increment.

### Tasks

- Trace “Save report” flow:
	- UI (button) → ViewModel → UseCase → update client `nextIdNumber` → dashboard observe.
- Add integration test (even “fake repo”):
	- when saving a report (PASS o FAIL) for a client, `nextIdNumber` increments (ADR-0010).
- UI/UX:
	- Esplicitare in dashboard che l'incremento avviene al salvataggio (PASS o FAIL), come da ADR-0010 (helper text opzionale).
- Ensure usage of centralized policy `SocketIdLite` only (no duplications)

dump_core_domain

### Docs

- Update docs: when it increments and why.

---

## Story 7 — Restore “History Detail v2” screen from branch/commit

### Objective

- Recover the “ordered and rich” history detail screen (cards with icon etc.) from a branch (probably develop), reintegrating it into HEAD with SOLID refactor.

### Tasks (git procedure for basic agent)

**Discover where it lived**

```bash
git fetch --all
git branch -a | grep -i develop
```

**Search candidate files:**

```bash
git log --all --name-only -- ui/history | less
git log --all --grep="ReportDetail" --grep="History" --grep="Detail" --oneline
```

**Isolate the “good” commit**

Once found a commit that introduces the “nice” UI:

```bash
git show <hash> --stat
git show <hash>:app/src/main/java/.../ReportDetailScreen*.kt
```

**Clean porting**

- No blind cherry-pick.
- Extract UI components into dedicated package:
	- `ui/history/detail/v2/…`
- Introduce a mapper/presenter if intermediate models are needed (DIP: UI must not know raw details).

**Wiring**

- Hook the new screen from navigation (existing route), keep backward compatibility if needed.

**Remove duplicates**

- If current `ReportDetailScreen` exists, decide:
	- replace, or
	- keep v1 behind feature flag (not recommended if you want zero debt).

### Test

- Compose test: open history detail + render main cards.
- Snapshot: at least 1 report with all sections.

### Docs

- Update docs: new History detail UX.

---

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

**Why it's bad:** drift between Live/History/Report pipelines. Every time a renderer changes, it must be updated in multiple call sites.

### P2 — Section ordering is duplicated in UI (business rule drift)
**Where observed:** `ui/test/TestSectionDisplayPolicy.kt` keeps an `orderedIds` list and sorts sections.

**Why it's bad:** domain/test runner already guarantees deterministic ordering. UI re-ordering creates rule duplication and eventual mismatch.

### P3 — TestExecutionScreen is a UI monolith (spaghetti/hotspot)
**Where observed:** `ui/test/TestExecutionScreen.kt` contains entry/scaffold + running + completed + helpers in one large file.

**Why it's bad:** merge conflicts, review difficulty, accidental coupling; slows down future changes.

---

## 2) Non-goals

- No UX redesign, no new navigation routes
- No broad package renames or architecture rewrites
- No "backup_*.kt" or commented-out code to preserve old logic (Git is the backup)

---

## 3) Guardrails (must follow)

### GR1 — No assumptions
Do not infer usage from build caches, generated sources, or annotations alone.

### GR2 — File header comment required
Every **modified or newly created** Kotlin file must include the standard header comment (see template in this bundle).

### GR3 — Delete means delete
If something is confirmed dead: delete it. Do not comment it out or move to "backup".

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
- Keep only "entry + scaffold + delegation" in `TestExecutionScreen.kt`.

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

---

# EPIC: Eliminate build warnings (KT-73255 + Room/Compose deprecations) with zero tech debt

## Context & problem

The current assembleDebug output shows two classes of warnings:

### Kotlin annotation default target warning (KT-73255)

"This annotation is currently applied to the value parameter only, but in the future it will also be applied to field … use @param: … or -Xannotation-default-target=param-property …"
This is tied to Kotlin's evolving rules for annotation use-site targets (param/property/field). Kotlin 2.2 introduces a preview defaulting rule and recommends explicit use-site targets when you need stable behavior.

### Deprecations in Room + Compose Material3

Room: the no-arg fallbackToDestructiveMigration call is deprecated in favor of the overload with dropAllTables: Boolean.

Compose M3: TabRow deprecated in favor of PrimaryTabRow / SecondaryTabRow.

Compose M3: TopAppBarDefaults.centerAlignedTopAppBarColors deprecated; use topAppBarColors.

## Goal

Zero warnings from the list you posted, without suppressions, without "temporary" flags, and without altering app behavior (UX unchanged except where API semantics require no-op replacements).

## Non-goals

No dependency upgrades.

No refactor beyond what is necessary to remove the warnings cleanly.

No "turn all warnings into errors" unless explicitly requested (too risky as a blanket policy).

## Key decision (anti-tech-debt)
Decision: Fix KT-73255 by using explicit use-site targets (NOT compiler flags)

We will not introduce -Xannotation-default-target=param-property because it's a preview behavior change and can create hidden drift in how annotations are emitted in bytecode across the codebase. Kotlin explicitly documents both approaches and the "preview" nature of the new rule.

Instead:

For DI qualifiers on constructor parameters, we will use @param:....

For Moshi @Json(...) on properties, we will use @field:Json(...) (stable Java-visible placement; Moshi supports honoring annotations on fields/properties; using field: is a well-known safe approach for Kotlin interop).

## Scope (files involved from your warning list)
KT-73255 (annotation default target)

app/src/main/java/com/app/miklink/data/io/AndroidDocumentWriter.kt

app/src/main/java/com/app/miklink/data/pdf/impl/PdfGeneratorIText.kt

app/src/main/java/com/app/miklink/data/remote/mikrotik/MikroTikServiceProviderImpl.kt

app/src/main/java/com/app/miklink/data/remote/mikrotik/dto/MikroTikDto.kt

app/src/main/java/com/app/miklink/data/remote/mikrotik/dto/SpeedTestRequest.kt

app/src/main/java/com/app/miklink/data/remote/mikrotik/dto/SpeedTestResult.kt

app/src/main/java/com/app/miklink/data/repository/mikrotik/MikroTikTestRepositoryRemote.kt

app/src/main/java/com/app/miklink/data/repository/mikrotik/MikroTikProbeConnectivityRepository.kt

Also add a quick search-based sweep for the same pattern in nearby files (e.g., AndroidDocumentReader.kt), so we don't fix only the currently-reported subset and leave landmines for the next build variant.

Room deprecation

app/src/main/java/com/app/miklink/di/DatabaseModule.kt

Compose Material3 deprecations

app/src/main/java/com/app/miklink/ui/history/ReportDetailScreen.kt

app/src/main/java/com/app/miklink/ui/test/TestExecutionScreen.kt

Docs to update (docs-as-code consistency)

Search and update references of:

fallbackToDestructiveMigration no-arg usage

TabRow (deprecated)

annotation target policy (new doc to add)

Implementation plan (step-by-step, agent-proof)
Phase 0 — Guardrails (anti-drift)

Task 0.1: Add a "Forbidden Patterns" quality gate (source-based, deterministic)
Create a small, cross-platform checker that fails CI if deprecated/known-bad patterns reappear.

Preferred approach (portable): Gradle task using Kotlin

Add buildSrc (or existing build-logic) task checkForbiddenPatterns that scans app/src/** and docs/**.

Patterns to fail on:

fallbackToDestructiveMigration (no-arg variant)

TabRow or ScrollableTabRow calls

centerAlignedTopAppBarColors usage

constructor property injection pattern without use-site target:

@ApplicationContext private val

Moshi @Json(name = without use-site target:

(@Json\\() on same line as val / var in primary constructor

Output requirements

Print file path + line number + suggested fix.

Exit non-zero.

Acceptance

Running ./gradlew checkForbiddenPatterns fails on the current code, then passes once all tasks below are completed.

Phase 1 — Fix KT-73255 (DI qualifiers)

Kotlin's docs: use-site targets @param:, @field:, etc.

Task 1.1: Apply @param: to Hilt qualifier annotations on constructor properties
Wherever you have:

@Inject constructor(
    @ApplicationContext private val context: Context
)


Change to:

@Inject constructor(
    @param:ApplicationContext private val context: Context
)


Rules

Use @param: for any constructor parameter qualifier when the parameter is also a property (private val ... / val ...).

Do not change behavior, do not move dependencies, do not add wrappers.

Files

All listed above with @ApplicationContext private val context: Context

Plus any additional hits from the sweep.

Task 1.2: Add/refresh the required top-of-file header comment
You requested this standard previously: every modified file must start with a short header comment describing purpose, inputs, outputs.

If a file already has such header: update it if necessary.

If missing: add it.

Acceptance

The KT-73255 warning lines related to Hilt qualifiers disappear.

No functional changes (app still runs; Hilt graph unchanged).

Phase 2 — Fix KT-73255 (Moshi DTOs)

Moshi + Kotlin often benefits from explicit field targeting for JSON name mapping; Kotlin docs explain why ambiguity exists and how @field: pins it.

Task 2.1: Apply @field:Json in DTO primary constructors
Wherever you have:

data class X(
    @Json(name = "foo") val foo: String
)


Change to:

data class X(
    @field:Json(name = "foo") val foo: String
)


Rules

Only for Moshi com.squareup.moshi.Json.

Do not rename fields.

Do not change defaults/nullability.

Keep formatting consistent.

Files

MikroTikDto.kt

SpeedTestRequest.kt

SpeedTestResult.kt

Also sweep app/src/test/** for Moshi DTOs (your tests/golden parsing likely compile under different tasks; eliminate future warning reintroduction).

Acceptance

All KT-73255 warnings in DTO packages are gone.

Existing parsing/golden tests still pass.

Phase 3 — Fix Room deprecation

Room docs: the no-arg fallbackToDestructiveMigration call is deprecated; use overload with dropAllTables (recommended true).

Task 3.1: Update Room builder call

In DatabaseModule.kt:

Replace:

.fallbackToDestructiveMigration(/* no-arg */)

With:

.fallbackToDestructiveMigration(dropAllTables = true)


Rules

Keep your current "pre-production destructive migration" policy unchanged.

Keep the existing inline comment but update it if it mentions the old signature.

Task 3.2: Update docs references

Search docs/** for the no-arg fallbackToDestructiveMigration mention and update examples to:

fallbackToDestructiveMigration(dropAllTables = true)


Acceptance

Room deprecation warning disappears.

App DB init behavior unchanged (still destructive migration in pre-prod).

Phase 4 — Fix Compose Material3 deprecations

Material3 release notes: TabRow is deprecated in favor of Primary/Secondary variants.
TopAppBarDefaults API: centerAlignedTopAppBarColors deprecated; use topAppBarColors.

Task 4.1: Replace TabRow with PrimaryTabRow

In ReportDetailScreen.kt:

Replace TabRow (selectedTabIndex = ...) { ... }

With PrimaryTabRow (selectedTabIndex = ...) { ... }


Rules

Choose PrimaryTabRow specifically to minimize visual drift from the old default TabRow behavior.

Don't change the Tab contents.

Keep semantics and state logic identical.

Task 4.2: Replace deprecated top app bar colors factory

In TestExecutionScreen.kt:

Replace:

TopAppBarDefaults.centerAlignedTopAppBarColors (...)

With:

TopAppBarDefaults.topAppBarColors(...)


Rules

Keep the same parameter values (container/scrolled/icon/title/action colors).

Do not change any logic that decides which colors to use.

No UI redesign.

Acceptance

Compose deprecation warnings disappear.

Manual smoke check: open affected screens; tabs and top bar look/behave the same.

Verification checklist (Definition of Done)
Build / static

 ./gradlew :app:assembleDebug --warning-mode all shows none of the posted warnings.

 ./gradlew checkForbiddenPatterns passes (new gate).

 No new warnings introduced by the changes.

Tests

 Unit tests pass: ./gradlew testDebugUnitTest

 Instrumentation tests (at least smoke subset) pass: ./gradlew connectedDebugAndroidTest (or your CI equivalent)

Code quality

 No @Suppress("DEPRECATION") added.

 No preview compiler flags added (-Xannotation-default-target=param-property avoided).

 All modified files have the required header comment (purpose / inputs / outputs).

Docs-as-code

 docs/** updated where it references deprecated APIs (fallbackToDestructiveMigration (no-arg) old signature, etc.).

 Add a short new reference doc: docs/reference/annotations-use-site-targets.md describing:

When to use @param: vs @field: (DI vs JSON DTOs)

Rationale referencing Kotlin's use-site target rules (link/citation for future maintainers).

Rollback strategy

All changes are mechanical and localized. If anything unexpected happens:

Revert Phase 2 (DTO annotations) first.

Revert Phase 4 (Compose) next.

Keep Phase 3 (Room) unless Room version is pinned pre-2.7 (but your warning confirms you're on the deprecated API).

Notes for the agent (to prevent "drift by creativity")

Do not refactor architecture, DI modules, or UI structure.

Do not change any runtime logic—only annotation targets and API replacements.

Keep diffs minimal and reviewable.

If you encounter another warning of the same kind, fix it in the same pass (don't leave "we'll do it later").
