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
	- when saving a “PASS” report for a client, `nextIdNumber` increments.
- UI/UX:
	- If by design it increments only in some cases (e.g., PASS), make it explicit (helper text in dashboard).
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
