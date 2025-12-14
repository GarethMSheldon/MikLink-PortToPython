# DISCREPANCIES (Docs/ADR vs Codice)

Questo file contiene **solo evidenze** (path/line).  
Niente soluzioni, niente proposte.

---

## D-001 — `probeId` ancora presente (hard rule = 0 occorrenze)

- `app/src/main/java/com/app/miklink/core/domain/test/model/TestPlan.kt:7`
  - contiene la stringa `probeId` (anche se solo in commento)

---

## D-002 — HTTPS trust-all applicato anche quando si usa HTTP

Vincolo: trust-all **solo** quando `isHttps = true` (ADR-0002).

Evidenza:

- `app/src/main/java/com/app/miklink/di/NetworkModule.kt:86-91`
  - `sslSocketFactory(...)` + `hostnameVerifier { _, _ -> true }` applicati **sempre** nel client di default

---

## D-003 — Violazioni Canone A: implementazioni tecnologiche in `core/data/**`

Vincolo: `core/data/**` = solo ports/contratti (no Retrofit/Room/iText/Android).

Stato: **RISOLTO** ✅

Evidenze:

- Le implementazioni iText + Android sono state spostate in `app/src/main/java/com/app/miklink/data/pdf/**`:
  - `app/src/main/java/com/app/miklink/data/pdf/PdfDocumentHelper.kt` (contiene `com.itextpdf.*`).
  - `app/src/main/java/com/app/miklink/data/pdf/impl/PdfGeneratorIText.kt` (contiene `com.itextpdf.*` e `android.content.Context`).

- In `core/data/**` rimangono solo il port e i modelli neutrali (nessuna dipendenza platform-specifica):
  - `app/src/main/java/com/app/miklink/core/data/pdf/PdfGenerator.kt` (interface)
  - `app/src/main/java/com/app/miklink/core/data/pdf/PdfExportConfig.kt`
  - `app/src/main/java/com/app/miklink/core/data/pdf/parser/ParsedResultsParser.kt`

- Verifiche eseguite:
  - `git grep -nE "com\\.itextpdf\\.|android\\." app/src/main/java/**/core/data/**` → **0 occorrenze**
  - `./gradlew testDebugUnitTest` → **BUILD SUCCESSFUL**
  - `./gradlew assembleDebug` → **BUILD SUCCESSFUL**
  - `./gradlew assembleRelease` → **BUILD SUCCESSFUL**

---

## D-004 — Logs ancora presenti (fuori scope)

Stato: **RISOLTO** (EPIC-0001.2).

Evidenze precedenti rimosse:

- UI: toggle + pannello "raw logs" (TestExecutionScreen)
- Domain: evento log (TestEvent.LogLine)
- String resources dedicate ai log

---

## D-005 — DB legacy / migrazioni (EPIC-0002)

Stato: **RISOLTO** ✅

Evidenze (solo comandi/output):

- `git grep -n "miklink-db" app/src/main/java` → **0 occorrenze**
- `git grep -nE "\bv1\b|\bv2\b|Database_v2|Migrations\.kt|Migration\b|AutoMigration" app/src/main/java` → **0 occorrenze**
- `./gradlew testDebugUnitTest` → **BUILD SUCCESSFUL**
- `./gradlew assembleDebug` → **BUILD SUCCESSFUL**
- `./gradlew assembleRelease` → **BUILD SUCCESSFUL**

---

## D-006 — Socket-ID LITE increment gating

Evidenze (implementazione + verifiche anti-drift):

- Increment location (single block):
  - `app/src/main/java/com/app/miklink/data/repositoryimpl/room/RoomReportRepository.kt:40`
    - `val updated = client.copy(nextIdNumber = client.nextIdNumber + 1)`

- Anti-drift grep (only 1 increment block):
  - `git grep -nE "nextIdNumber\s*=\s*.*\+\s*1|nextIdNumber\+\+|incrementNextId|advanceNextId" app/src/main/java` → 1 match (`RoomReportRepository.kt:40`)

- UI check (no UI-side increments found):
  - `git grep -n "nextIdNumber" app/src/main/java/**/ui/**` → matches are only UI read/placeholder usages (no arithmetic/update)

- ProbeId anti-regression (must be 0):
  - `git grep -n "probeId" app/src/main app/src/test` → **0 matches**

- Build gates (after tests & changes):
  - `./gradlew testDebugUnitTest` → **BUILD SUCCESSFUL**
  - `./gradlew assembleDebug` → **BUILD SUCCESSFUL**
  - `./gradlew assembleRelease` → **BUILD SUCCESSFUL**

Stato: **RISOLTO** ✅
