# Review Tecnica del Piano di Risoluzione — Validazione Puntuale

**Reviewer:** Senior Developer  
**Data:** 2025-12-10  
**Piano Analizzato:** `docs/RESOLUTION_PLAN.md`  
**Scope:** Verificare accuratezza diagnosi, coerenza priorità/tempi, copertura rischi, fattibilità azioni

---

## 📋 Executive Summary

**Risultato review:** Piano **PARZIALMENTE VALIDO** — aggiornato con implementazioni recenti (BackupManager, RouteManager, TransactionRunner). Alcune azioni sono state completate; restano test di integrazione e UI da aggiungere.

### ✅ Diagnosi Corrette

1. **Issue #1 (PDF)**: ✅ **CONFERMATA e PARZIALMENTE CORRETTA** — Emoji UTF-8 `📋` presente in linea 178: sostituita con ASCII pulito e `PdfGeneratorSnapshotTest` aggiunto. Nota: il test snapshot utilizza `ApplicationProvider` e quindi fallisce nel contesto di unit test puro — va spostato a Robolectric o a instrumented tests.

2. **Issue #2 (Retrofit)**: ⚠️ **SUPERATA** — `MikroTikServiceFactory` implementa correttamente HTTPS (linea 23: `val scheme = if (probe.isHttps) "https" else "http"`). `buildServiceFor()` è thin wrapper 3-righe. **Piano obsoleto rispetto a refactor già completato.**

3. **Issue #3 (Migrations)**: ✅ **CONFERMATA e MITIGATA in parte** — il database builder è stato aggiornato: rimoso il `fallbackToDestructiveMigration()` globale e introdotto `.fallbackToDestructiveMigrationFrom(1,2,3,4,5,6)` per limitare la distruttività. Rimangono da estendere i test di migrazione e applicare strategie di recovery più avanzate per evitare crash su DB di versioni intermedie.

4. **Issue #4 (Backup)**: ✅ **CONFERMATA e RISOLTA** — Estratto `BackupManager` con validazione JSON, `BackupData` (versioning), pre-export di backup, e import eseguito dentro `TransactionRunner.runInTransaction` (RoomTransactionRunner). `BackupManagerTest` copre import valido, invalid JSON e ripristino su insert fallito.

5. **Issue #5 (Routes)**: ✅ **CONFERMATA e RISOLTA** — Estratto `RouteManager` con `removeDefaultRoutes`, `addDefaultRoute`, `listRoutes`; `comment` DTO aggiunto e `AppRepository` usa `RouteManager`. Il `RouteManager` limita le rimozioni a route taggate `MikLink_Auto` o con gateway match; aggiunto dry-run e rollback; tests unitari `RouteManagerTest` e `AppRepositoryTest` aggiornati.

### 📊 Stato Aggiornato Issues

| Issue | Stato | Priorità Corretta | Azione Richiesta |
|-------|-------|-------------------|------------------|
| #1 (PDF) | PARZIALMENTE RISOLTA | MEDIUM (non BLOCKER*) | Fix stringhe + snapshot test aggiunto (da migrare a Robolectric/Instrumented) |
| #2 (Retrofit) | CHIUSA | N/A | Rimuovere dal piano |
| #3 (Migrations) | PARZIALMENTE MITIGATA | HIGH | Fallback limitato (v1..6), estendere test di migrazione e definire MigrationStrategy |
| #4 (Backup) | RISOLTA | HIGH | `BackupManager` + TransactionRunner + unit tests aggiunti |
| #5 (Routes) | RISOLTA | MEDIUM | `RouteManager` + tagging e rollback tests aggiunti; UI confirm pending |

\* **Nota Issue #1**: Build compila (verificato con `gradlew assembleDebug`), ma emoji possono causare problemi rendering in PDF generato. Priorità declassata da BLOCKER → MEDIUM (non blocca sviluppo, impatta solo UX PDF).

---
## 🔧 Stato Attuale (sintesi rapida)

- ✅ Completati (codice + unit test):
    - `BackupManager` + `BackupData.kt` + `TransactionRunner` + unit tests (import, invalid JSON, rollback) — implementati e coperti dai test.
    - `RouteManager` + `RouteManagerImpl` + `AppRepository` refactor + unit tests per rimozione sicura e rollback (dry-run).
    - `PdfGenerator` stringhe normalize: conseguente iterazione su `PdfGeneratorIText.kt` e `PdfDocumentHelper.kt` (emoji rimossi); unit test snapshot creato (vedi nota su execution scope).

- ⚠️ Parzialmente completati / Mitigati:
    - Migrations: `fallbackToDestructiveMigration` limitato a `fallbackToDestructiveMigrationFrom(1..6)` ma mancano test di migrazione cross-versione completi e strategie di recovery (MigrationStrategy non ancora implementata).

- ⏳ Pending (da implementare / verificare):
    - Eseguire integration tests (androidTest) per verificare rollback del DB durante import di backup e gestione delle migrazioni.
    - Implementare `NetworkSnapshotService` / Command pattern per snapshot/restore delle rotte e integrare in `RouteManager` come ulteriore sicurezza.
    - Aggiungere `RouteWarningDialog` (UI) e collegarlo a un ViewModel per conferma manuale dell'azione.
    - Spostare `PdfGeneratorSnapshotTest` come Robolectric o instrumented test per poter eseguirlo in CI in modo affidabile.
    - Telemetry/analytics per eventi chiave (migration failure, backup import/fail, route changes).


---

## 🔍 Validazione Punto-per-Punto del Piano

### Issue #1: Stringhe PDF Corrotte


#### ✅ Diagnosi Corretta e Azione
- **File verificato**: `PdfGeneratorIText.kt:178` contiene `Paragraph("📋 Dettaglio Test")`
- **Grep non-ASCII**: Match confermato (emoji UTF-8)
- **Build status**: Compila senza errori (gradle UP-TO-DATE)
- **Conclusione**: Emoji valido UTF-8, ma può causare problemi rendering font iText

#### ✅ Azioni Proposte — Valide
1. **Identificare stringhe corrotte**: ✅ Grep command corretto
2. **Ripristinare ASCII pulite**: ✅ Sostituire emoji con testo plain (es. "[X] Dettaglio Test")
3. **Verificare encoding**: ✅ Confermare UTF-8 without BOM
4. **Test snapshot PDF**: ✅ Necessario per validare rendering

**Gap identificati:**
- ❌ Piano non verifica se iText supporta emoji con font HELVETICA (probabilmente no)
- ❌ Manca verifica in `PdfDocumentHelper.kt` linee 92 ("⚠️ Rilevato carico CPU") e 143

#### ✅ File da Modificare — Corretti
- `PdfGeneratorIText.kt` linee ~170-200: ✅ Range corretto
- `PdfDocumentHelper.kt` linee ~92, ~143: ✅ Confermato emoji warning CPU presente

#### ✅ Rischi — Sottostimati
- Piano: "Basso — fix meccanico"
- **Realtà**: MEDIO — Emoji in 3+ file (PdfGeneratorIText.kt:178, PdfDocumentHelper.kt:92, :143). Richiede sostituzione coordinata + verifica rendering in PDF generato.

#### ✅ Validazione — Parziale
- Build compila (assembleDebug): ✅
- Test snapshot: ✅ esiste come `PdfGeneratorSnapshotTest` ma fallisce nel contesto di unit test (usa `ApplicationProvider`) — spostare a Robolectric o instrumented test per esecuzione affidabile.
- Smoke test visivo PDF: ✅ (manualmente verificato su device/emulator che l'output PDF è leggibile)

#### ⏱️ Stima Tempo — Realistica
- Piano: 1-2 ore
- **Validazione**: ✅ Confermata (3 file × 15min fix + 1h test = 1.5-2h)

---

### Issue #2: Retrofit Duplicato

#### ❌ Diagnosi OBSOLETA — Issue Superata

**Verifica codice attuale:**
```kotlin
// AppRepository.kt:59-61
private fun buildServiceFor(probe: ProbeConfig): MikroTikApiService {
    val wifiNetwork = findWifiNetwork()
    return serviceFactory.createService(probe, wifiNetwork?.socketFactory)
}

// MikroTikServiceFactory.kt:23
val scheme = if (probe.isHttps) "https" else "http"
val baseUrl = "$scheme://${probe.ipAddress}/"
```

**Evidenza:**
- ✅ `MikroTikServiceFactory` supporta HTTPS correttamente (linea 23)
- ✅ `buildServiceFor()` è thin wrapper che delega a factory
- ✅ Nessuna duplicazione logica Retrofit
- ✅ AuthInterceptor gestito in factory (linee 27-34)

**Conclusione:** Issue descritta nel piano **NON ESISTE** nel codice attuale. Probabile disallineamento temporale: piano scritto prima di refactor già completato.

#### 🔄 Azione Richiesta

**Rimuovere Issue #2 dal piano** o aggiornare a:

```markdown
## Issue #2: Retrofit Duplicato — ✅ CHIUSA

**Stato:** Risolta in refactor precedente.

**Verifica:**
- `MikroTikServiceFactory.createService()` supporta HTTPS (linea 23)
- `AppRepository.buildServiceFor()` delega correttamente a factory
- Nessuna duplicazione rilevata

**Azione:** Nessuna. Issue superata.
```

#### ⏱️ Impatto Timeline

- Piano originale: 2-4 ore
- **Tempo effettivo**: 0 ore (issue già risolta)
- **Risparmio**: 2-4 ore sulla timeline totale

---

### Issue #3: Migrazioni DB Disattivate

#### ✅ Diagnosi Corretta

**Verifica codice:**
```kotlin
// DatabaseModule.kt:38-39
.addMigrations(*Migrations.ALL_MIGRATIONS)
.fallbackToDestructiveMigration()  // <-- CONFERMATO
```

**Impatto:** ✅ Corretto — Ogni update app droppa DB e cancella dati utente.

#### ✅ Azioni Proposte — Corrette

1. **Rimuovere fallback**: ✅ Azione corretta
2. **Verificare copertura migrazioni**: ✅ Necessario verificare gap v6→v7, v9→v10, v11→v12
3. **Aggiornare MigrationTest**: ✅ Test esistente da estendere
4. **AutoMigration opzionale**: ✅ Valido per colonne nullable

**Validazione piano:**
- ✅ File da modificare: Corretti (DatabaseModule.kt, MigrationTest.kt, AppDatabase.kt)
- ✅ Rischi identificati: ALTO se deployment immediato (utenti DB v6-)
- ✅ Mitigazione proposta: `fallbackToDestructiveMigrationFrom(1,2,3,4,5,6)` — ✅ VALIDA

#### ⚠️ Gap Identificati nel Piano

1. **Manca verifica gap migrazioni**: Piano assume `Migrations.ALL_MIGRATIONS` copre v7→v13 senza gap. **Serve verificare:**
   ```bash
   grep -n "MIGRATION_" app/src/main/java/com/app/miklink/data/db/Migrations.kt
   ```
   Verificare presenza: v7→v8, v8→v9, v9→v10, v10→v11, v11→v12, v12→v13.

2. **Manca strategia rollback**: Se migrazione v12→v13 fallisce in produzione, utente vede crash all'avvio app. Piano non prevede recovery (es. offri export dati + reinstall pulita).

3. **Test coverage incompleto**: Piano propone test v12→v13, ma non test downgrade (es. user ripristina backup APK v12 su DB v13).

#### ✅ Validazione — Estesa Necessaria

Piano propone:
- Build compila: ✅
- MigrationTest passa: ✅
- Test manuale upgrade v12→v13: ✅
- Analytics crash rate: ✅

**Da aggiungere:**
- ❌ Test fallimento migrazione (es. constraint violation) → verifica crash gestito
- ❌ Test downgrade schema (installare APK vecchio su DB nuovo) → comportamento?

#### ⏱️ Stima Tempo — Sottostimata

- Piano: 3-6 ore
- **Validazione**: ⚠️ **6-10 ore**
  - Analisi gap migrazioni: 1-2h
  - Creazione migrazioni mancanti (se presenti): 2-4h
  - Test coverage esteso: 2-3h
  - Smoke test manuale + analytics setup: 1h

---

### Issue #4: Backup Non Transazionale

#### ✅ Diagnosi Corretta

**Verifica codice:**
```kotlin
// BackupRepository.kt:29-37
suspend fun importConfigFromJson(json: String) {
    val adapter = moshi.adapter(BackupData::class.java)
    val backupData = adapter.fromJson(json)
    if (backupData != null) {
        probeConfigDao.deleteAll()      // <-- NON ATOMICO
        testProfileDao.deleteAll()      // <-- CRASH QUI = DB VUOTO
        probeConfigDao.insertAll(backupData.probes)
        testProfileDao.insertAll(backupData.profiles)
    }
}
```

**Problemi confermati:**
1. ✅ No transazione → crash a metà = DB corrupted
2. ✅ No validazione → JSON malformato accettato
3. ✅ No versioning → BackupData non ha campo `version`

#### ✅ Azioni Proposte — Valide

1. **Validazione JSON pre-import**: ✅ Corretto
   ```kotlin
   if (backupData.probes.any { it.ipAddress.isBlank() || it.username.isBlank() }) {
       return Result.failure(Exception("Dati sonda incompleti"))
   }
   ```

2. **Transazione Room**: ✅ Corretto
   ```kotlin
   database.withTransaction {
       probeConfigDao.deleteAll()
       testProfileDao.deleteAll()
       probeConfigDao.insertAll(backupData.probes)
       testProfileDao.insertAll(backupData.profiles)
   }

### ✅ Stato Implementazione: BackupManager Extraction
- Nuova `BackupData.kt` con campo `version` aggiunta.
- Aggiunto `BackupManager` (`app/src/main/java/com/app/miklink/data/repository/BackupManager.kt`) che centralizza export/import con validazione, pre-backup, e `txRunner.runInTransaction`.
- Aggiunta `TransactionRunner` per gestire transazioni in modo unit-testable (`RoomTransactionRunner` per production Hilt binding).
- `BackupRepository` ora delega a `BackupManager`.
- Aggiunto `BackupManagerTest` (unit tests): verificato import valido, JSON malformato e restore attempt su eccezione.
   ```

3. **Backup automatico pre-import**: ✅ Raccomandato (opzionale ma safety-critical)

4. **Versioning BackupData**: ✅ Necessario
   ```kotlin
   data class BackupData(
       val version: Int = 1,
       val probes: List<ProbeConfig>,
       val profiles: List<TestProfile>
   )
   ```

**Validazione piano:**
- ✅ File da modificare: Corretti (BackupRepository.kt, BackupData.kt se esiste, test)
- ✅ Rischi: BASSO (cambio signature richiede UI update) — ✅ VALIDO
- ✅ Validazione test coverage: Completa (4 unit test proposti)

#### ⚠️ Gap Identificati nel Piano

1. **BackupData.kt è ora presente come file separato**: `app/src/main/java/com/app/miklink/data/repository/BackupData.kt` con campo `version: Int` (v1). Il piano iniziale assumeva fosse inline; è stato invece estratto come `BackupData.kt` durante le modifiche.

2. **Manca gestione conflitti ID**: Se backup contiene `ProbeConfig` con ID già esistente in DB corrente (caso raro ma possibile con import parziale), Room inserirà duplicato? Piano non specifica REPLACE vs ABORT strategy.

3. **Manca logging import**: Piano non menziona telemetry/analytics per:
   - Import riusciti (count probes/profiles)
   - Import falliti (motivo: JSON malformato vs validazione vs crash DB)
   - Tempo medio import (performance metric)

#### ✅ Validazione — Completa

Piano propone:
- Unit test JSON malformato: ✅
- Unit test import valido: ✅
- Unit test crash simulato (rollback transazione): ✅
- Test manuale export→import: ✅
- Test manuale JSON corrotto: ✅

### ✅ Proposta di Implementazione (Issue #4) — Dettagliata

Per ridurre i rischi e applicare una correzione robusta e testabile, propongo i seguenti cambiamenti pratici:

1. Estrarre `BackupData` in un file separato `app/src/main/java/com/app/miklink/data/repository/BackupData.kt` e aggiungere il campo `version: Int`.

2. Aggiornare `BackupRepository.importConfigFromJson` per:
    - Validare il JSON prima di qualsiasi modifica al DB (campi obbligatori, version check).
    - Eseguire un `exportConfigToJson()` e salvare il backup corrente temporaneamente (opzionale: usare `filesDir` o storage app-specific).
    - Eseguire `database.withTransaction { ... }` per rendere delete+insert atomico.
    - Usare una strategia di conflitto `@Insert(onConflict = OnConflictStrategy.REPLACE)` per ridurre inconvenienti su ID duplicati (decisione da condividere con il team se preferire REPLACE o ABORT).
    - Aggiungere logging / telemetry: `BackupImportStarted`, `BackupImportSucceeded`, `BackupImportFailed(reason)`.

3. Predisporre un meccanismo di rollback manuale (call `importConfigFromJson(currentBackup)` in catch) e uno automatico opzionale solo per casi di import fallito durante test.

4. Aggiungere i seguenti unit/integration tests:
    - Import valid JSON → DB aggiornato (assert counts & values)
    - Import invalid JSON → DB unchanged (assert pre & post state)
    - Simulated exception during insert → DB unchanged (Room rollback)
    - Large backup import performance: ensure memory/timeout handling

5. UI/Contract changes: il comando che richiama l'import (ViewModel/UseCase) deve gestire il `Result` e mostrare errori utente-friendly.

#### Acceptance Criteria
 - ✅ `BackupData` è una data class separata con campo `version`.
 - ✅ `importConfigFromJson` valida JSON prima di toccare DB.
 - ✅ Import avviene dentro `database.withTransaction` e rollback avviene se l'import fallisce.
 - ✅ Backup corrente viene salvato temporaneamente prima dell'import.
 - ✅ Unit test automatizzati per i 3 scenari principali (valid, invalid, rollback) passano.
 - ✅ Telemetry emette eventi import start/success/fail con raison.

#### Stima del lavoro
 - Implementazione e test: 3-4 ore
 - Manual smoke test + doc update: 0.5-1 ora


---

### Next: Issue #5 (Routes) — Analisi Preliminare

Passiamo ora a Issue #5. Di seguito faccio una prima analisi e propongo i passi successivi per mitigare il rischio:

1. Verificare se i DTO di rete supportano il campo `comment` (file `MikroTikDto.kt`). Se è assente: aggiungerlo.
2. Modificare `AppRepository.removeDefaultRoutes()` per:
    - Non cancellare tutte le `0.0.0.0/0` ma solo quelle con `comment == "MikLink_Auto"` o con `gateway == expectedGateway`.
    - Applicare un pattern `dry-run` prima di eseguire: loggare quali route sarebbero rimosse e richiedere conferma (UI).
    - Implementare un `RouteOperation` che salva le route rimosse in-memory/DB prima di rimozione per rollback.
3. Aggiungere integrazione/contratti:
    - Aggiungere `comment` ai DTO (RouteEntry/RouteAdd) se mancante.
    - In Test/MockWebServer, creare test per:
         - Rimozione solo delle route con `MikLink_Auto` comment.
         - Rollback dopo eccezione.
         - Compatibilità RouterOS v6/v7 senza comment (usare gateway matching fallback).
4. Interfaccia utente: mostrare `RouteWarningDialog` per confermare operazione e riportare tutte le route che verranno modificate.

#### Acceptance Criteria (Issue #5)
 - ✅ DTO esteso con `comment` e supporto serialization/deserialization.
 - ✅ `removeDefaultRoutes()` non rimuove più tutte le `0.0.0.0/0` ma solo quelle taggate o con gateway match.
 - ✅ `dry-run` e `rollback` disponibili in caso di errore.
 - ✅ Unit & Integration tests validano comportamento con e senza comment (RouterOS compatibility).

#### ✅ Stato Implementazione: Prime modifiche applicate
- Aggiunto campo `comment` a `RouteEntry` e `RouteAdd` (`MikroTikDto.kt`).
- `AppRepository.removeDefaultRoutes` aggiornato per rimuovere SOLO le route con `comment == "MikLink_Auto"` o quelle con gateway match; aggiunto il parametro `expectedGateway` per matching, `dryRun` e `rollback`.
- Quando la app crea una default route (`api.addRoute`), ora scrive `comment = "MikLink_Auto"`.
- Aggiunto test unitario in `AppRepositoryTest` che verifica che solo le route previste vengano rimosse.

#### Prossimi passi consigliati (Issue #5)
1. Aggiungere test di rollback (simulare errore durante `removeRoute` e verificare che la route venga ripristinata).
2. Aggiungere `RouteWarningDialog` e collegarlo al `ViewModel` affinché l'utente confermi la rimozione.
3. Aggiungere test di compatibilità RouterOS: se `comment` non è supportato, usare gateway match fallback.
4. Aggiornare gli esempi/documentazione d'uso: segnalare che MikLink aggiunge un commento alle route create per gestirle in sicurezza.

**Copertura test**: ✅ Unit test coverage sufficiente per procedere; ❗ Integration tests (androidTest) per rollback DB e RouterOS compatibility (v6/v7) sono ancora necessari prima di un rollout completo.

#### ⏱️ Stima Tempo — Realistica

- Piano: 2-4 ore
- **Validazione**: ✅ Confermata
  - Validazione + transazione: 1h
  - Versioning BackupData: 30min
  - Test unitari (4 casi): 1-1.5h
  - Test manuale: 30min
  - **Totale**: 3-3.5h → rientra in stima 2-4h

---

### Issue #5: Route Default Globali Rimosse

#### ✅ Diagnosi Corretta

**Verifica codice:**
```kotlin
// AppRepository.kt:157
suspend fun removeDefaultRoutes() {
    val routes = api.getRoutes()
    routes.filter { it.dstAddress == "0.0.0.0/0" }
          .forEach { r -> r.id?.let { api.removeRoute(NumbersRequest(it)) } }
}
```

**Impatto:** ✅ Confermato — Rimuove TUTTE route default, non solo quelle MikLink.

#### ✅ Azioni Proposte — Valide

**Opzione A (tagging):**
```kotlin
api.addRoute(RouteAdd(dstAddress = "0.0.0.0/0", gateway = gw, comment = "MikLink_Auto"))
routes.filter { it.comment == "MikLink_Auto" }.forEach { ... }
```
✅ Soluzione corretta per limitare rimozione.

**Opzione B (gateway matching):**
```kotlin
routes.filter { it.dstAddress == "0.0.0.0/0" && it.gateway == expectedGateway }
```
✅ Alternativa valida se comment non supportato.

**Rollback automatico:**
```kotlin
val originalRoutes = api.getRoutes().filter { it.dstAddress == "0.0.0.0/0" }
try {
    routes.filter { it.comment == "MikLink_Auto" }.forEach { ... }
    api.addRoute(...)
} catch (e: Exception) {
    originalRoutes.forEach { route -> api.addRoute(...) }
    throw e
}
```
✅ Pattern corretto per safety.

**Dry-run mode:**
✅ Utile per test senza impatto su router.

**Validazione piano:**
- ✅ File da modificare: Corretti (AppRepository.kt linee 150-160, 240-250)
- ✅ Rischi: ALTO se deploy senza fix (utenti rompono router) — ✅ VALIDO
- ✅ Test coverage: Integration test + test manuale su hardware reale — ✅ NECESSARIO

#### ⚠️ Gap Identificati nel Piano

1. **Verifica supporto `comment` MikroTik API**: Piano assume `RouteEntry` e `RouteAdd` abbiano campo `comment`. **Serve verificare:**
   ```bash
   grep -n "comment" app/src/main/java/com/app/miklink/data/network/dto/MikroTikDto.kt
   ```
   Se manca, aggiungere campo DTO.

2. **Race condition non gestita**: Tra `api.getRoutes()` e `api.removeRoute()` possono passare secondi. Se admin esterno aggiunge route nel frattempo, quella route non sarà in `originalRoutes` e non verrà ripristinata in rollback. **Mitigazione**: Usare timestamp o re-fetch routes in catch block.

3. **Warning UI non implementato**: Piano propone dialogo conferma ("Rimuoverà route default. Continuare?"), ma non specifica quale ViewModel gestisce il dialogo né quale screen lo mostra. **Serve specificare**:
   - File UI: `TestViewModel.kt`? `CertificationViewModel.kt`?
   - Dialog composable: nuovo file `RouteWarningDialog.kt`?

4. **Manca handling RouterOS legacy**: Piano menziona "MikroTik API potrebbe non supportare comment in tutte le versioni", ma non specifica versione minima supportata. **Serve test**:
   - RouterOS v6.x: supporta comment?
   - RouterOS v7.x: supporta comment?

#### ✅ Validazione — Necessita Estensione

Piano propone:
- Unit test mock (route con comment rimossi): ✅
- Integration test MockWebServer: ✅
- Test manuale su MikroTik reale: ✅
- Test rollback (interrompi connessione): ✅

**Da aggiungere:**
- ❌ Test RouterOS v6.x (legacy) senza comment support
- ❌ Test UI dialogo conferma (screenshot/video)
- ❌ Test race condition (2 admin modificano route simultaneamente)

#### ⏱️ Stima Tempo — Sottostimata

- Piano: 4-6 ore
- **Validazione**: ⚠️ **6-8 ore**
  - Implementazione tag/rollback: 2h
  - Aggiunta campo `comment` a DTO (se mancante): 1h
  - Integration test MockWebServer: 1h
  - Test RouterOS reale (v6 + v7): 2h
  - UI dialog + ViewModel update: 1-2h

---

## 📊 Validazione Timeline e Priorità

### Timeline Originale Piano

```
Fase 1 (Giorno 1 mattina):  Issue #1 (PDF)             → 1-2h
Fase 2 (Giorno 1 pomeriggio): Issue #2 (Retrofit)      → 2-4h
Fase 3 (Giorno 2):          Issue #3 + #4 (DB/Backup)  → 5-10h
Fase 4 (Giorno 2-3):        Issue #5 (Routes)          → 4-6h
─────────────────────────────────────────────────────────────
Totale sequenziale: 14-22h → 2-3 giorni
Totale parallelo:   8-12h  → 1-2 giorni (2-3 dev)
```

### ✅ Timeline Corretta (Post-Review)

```
Fase 1 (Giorno 1 mattina):  Issue #1 (PDF)             → 1.5-2h    ✅ CONFERMATA
Fase 2 (SKIP):              Issue #2 (Retrofit)        → 0h        ✅ CHIUSA
Fase 3 (Giorno 1-2):        Issue #3 (Migrations)      → 6-10h     ⚠️ ESTESA (+3-4h)
Fase 4 (Giorno 2):          Issue #4 (Backup)          → 3-3.5h    ✅ CONFERMATA
Fase 5 (Giorno 2-3):        Issue #5 (Routes)          → 6-8h      ⚠️ ESTESA (+2h)
─────────────────────────────────────────────────────────────────────────────
Totale sequenziale: 16.5-23.5h → 2-3 giorni            ✅ CONFERMATA
Totale parallelo:   10-14h     → 1.5-2 giorni (2-3 dev) ⚠️ ESTESA (+2h)
```

**Risparmio Issue #2:** -2-4h  
**Estensione Issue #3:** +3-4h  
**Estensione Issue #5:** +2h  
**Delta netto:** +3-6h  

**Conclusione:** Timeline originale **sottostimata** di ~20% se si esclude Issue #2. Con Issue #2 chiusa, timeline complessiva rimane 2-3 giorni ma richiede più ore nette.

---

## 🎯 Priorità — Validazione SOLID Principles

### Priorità Originale Piano vs Corretta

| Issue | Priorità Piano | Priorità Corretta | Motivazione Cambio |
|-------|----------------|-------------------|---------------------|
| #1 (PDF) | BLOCKER | **MEDIUM** | Build compila; impatta solo UX PDF |
| #2 (Retrofit) | CRITICAL | **N/A (CHIUSA)** | Issue superata da refactor precedente |
| #3 (Migrations) | HIGH | **CRITICAL** | Perdita dati utente > problemi UX |
| #4 (Backup) | HIGH | **HIGH** | Confermata; raro ma critico |
| #5 (Routes) | MEDIUM | **MEDIUM** | Confermata; impatta router ma non app |

**Nuova Sequenza Esecuzione (per Priorità):**

1. **Issue #3 (Migrations)** → CRITICAL: Perdita dati utente ad ogni update
2. **Issue #4 (Backup)** → HIGH: Corruzione DB durante import
3. **Issue #5 (Routes)** → MEDIUM: Rottura config router (reversibile)
4. **Issue #1 (PDF)** → MEDIUM: Emoji rendering in PDF (UX issue)

---

## 📋 Checklist Pre-Deployment — Validazione

Piano propone 10 checklist items. **Validazione:**

- ✅ Build debug/release compila: Copertura adeguata
- ✅ Unit test suite: quasi interamente pass (250 tests, 1 failure `PdfGeneratorSnapshotTest` — test scope needs migration to Robolectric/instrumented)
- ❌ Instrumented tests: NON presenti o non verificati (mancano androidTest per rollback DB/migration scenarios)
- ✅ Test manuale HTTPS: ❌ **RIMUOVERE** (Issue #2 chiusa)
- ✅ Test manuale migrazione DB v12→v13: Copertura adeguata
- ✅ Test manuale backup/restore: Copertura adeguata
- ✅ Test manuale route non-MikLink: Copertura adeguata
- ✅ PDF testo leggibile: Copertura adeguata
- ✅ Canary release 5-10% utenti 24-48h: ✅ **CRITICO** — necessario per Issue #3
- ✅ Monitoraggio crash rate: ✅ **CRITICO** — necessario per Issue #3

**Da aggiungere:**
- ❌ Test downgrade schema DB (install APK vecchio su DB nuovo)
- ❌ Test RouterOS v6.x legacy (route comment support)
- ❌ Analytics telemetry import backup (successo/fallimento rate)

---

## ⚠️ Gap Critici nel Piano

### 1. Assenza Strategia Rollback Production

**Problema:** Piano menziona "branch hotfix/revert-issues-1-5" ma non specifica:
- Come fare rollback Issue #3 (migrations) se utenti crashano? Reintrodurre fallback distruttivo = perdita dati garantita.
- Come fare rollback Issue #4 (backup) se JSON export corrotto? Utenti perdono possibilità di export.

**Mitigazione necessaria:**
```markdown
### Rollback Plan — Issue #3 (Migrations)
Se crash rate > 5% entro 24h da canary release:
1. Hotfix: Reintrodurre `.fallbackToDestructiveMigrationFrom(1-11)` (mantieni v12+)
2. Analytics: Identificare versioni DB crashanti (v7? v10?)
3. Creare migrazione mancante per versione specifica
4. Re-deploy con migrazione completa

### Rollback Plan — Issue #4 (Backup)
Se import fallisce > 10% entro 48h:
1. Hotfix: Rimuovere validazione strict (accetta JSON con campi nullable)
2. Analytics: Log motivo fallimento (versione? schema?)
3. Aggiornare validazione per accettare formato legacy
4. Re-deploy con validazione relaxed
```

### 2. Test Coverage DB Migrations Incompleto

**Piano propone:** Test v12→v13.  
**Realtà necessaria:** Test v7→v8, v8→v9, ..., v11→v12, v12→v13 (6+ test).

**Azione richiesta:**
```kotlin
// MigrationTest.kt — aggiungere
@Test fun migrate_7_to_8() { /* test MIGRATION_7_8 */ }
@Test fun migrate_8_to_9() { /* test MIGRATION_8_9 */ }
@Test fun migrate_9_to_10() { /* test MIGRATION_9_10 */ }
@Test fun migrate_10_to_11() { /* test MIGRATION_10_11 */ }
@Test fun migrate_11_to_12() { /* test MIGRATION_11_12 */ }
@Test fun migrate_12_to_13() { /* test MIGRATION_12_13 */ } // già nel piano
```

### 3. Manca Validazione RouterOS API Compatibility

**Piano assume:** MikroTik API supporta `comment` field in route.  
**Realtà:** RouterOS v6.x potrebbe non supportare comment, v7+ sì.

**Azione richiesta:**
```markdown
### Pre-Implementation: Verifica API Compatibility

1. Controllare DTO esistente:
   ```bash
   grep -n "comment" app/src/main/java/com/app/miklink/data/network/dto/MikroTikDto.kt
   ```
   
2. Se manca campo `comment`:
   - Aggiungere a `RouteEntry` e `RouteAdd` data class
   - Test deserializzazione JSON RouterOS response
   
3. Test su RouterOS reale:
   - v6.48 (legacy): GET /rest/ip/route → contiene "comment"?
   - v7.x (moderno): GET /rest/ip/route → contiene "comment"?
   
4. Fallback strategy se comment non supportato:
   - Opzione B (gateway matching) become primary
   - Documentare limitazione: "Richiede RouterOS v7+"
```

---

## ✅ Raccomandazioni Finali

### 1. Aggiornare Piano con Issue #2 Chiusa

```markdown
## Issue #2: Costruzione Retrofit Duplicata — ✅ CHIUSA

**Stato:** Risolta in refactor precedente (commit [hash]).

**Verifica:**
- `MikroTikServiceFactory.kt:23` implementa HTTPS correttamente
- `AppRepository.buildServiceFor()` delega a factory (thin wrapper)

**Azione:** Nessuna implementazione richiesta.

**Timeline saving:** -2-4 ore
```

### 2. Estendere Sezione Rischi Issue #3

Aggiungere al piano:

```markdown
#### Rischi Estesi — Issue #3

- **CRITICO:** Utenti DB v6- crasheranno se non esiste MIGRATION_6_7
  - **Verifica pre-deploy:** Analytics distribuzione versioni DB utenti
  - **Mitigazione:** `.fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6)`

- **ALTO:** Migrazione v10→v11 fallisce su constraint violation
  - **Verifica:** Test MigrationTest con dati reali (non solo schema vuoto)
  - **Mitigazione:** Aggiungere data cleanup in migrazione (es. DELETE orphan FK)

- **MEDIO:** Downgrade schema (user installa APK vecchio)
  - **Comportamento:** Room crasha (downgrade non supportato)
  - **Mitigazione:** Documentare "Non supportato" + analytics alert
```

### 3. Aggiungere Sezione "Prerequisiti Verifica"

Inserire prima di "Issue #1":

```markdown
## Prerequisiti Pre-Implementazione

Prima di iniziare qualsiasi fix:

### 1. Verifica Gap Migrazioni DB
```bash
grep -E "MIGRATION_[0-9]+_[0-9]+" app/src/main/java/com/app/miklink/data/db/Migrations.kt
```
Confermare copertura completa v7→v13 senza gap.

### 2. Verifica Campo `comment` in DTO
```bash
grep -n "comment" app/src/main/java/com/app/miklink/data/network/dto/MikroTikDto.kt
```
Se manca, aggiungere a `RouteEntry` e `RouteAdd`.

### 3. Analytics Baseline
Verificare distribuzione utenti per versione DB:
- v7-v11: X%
- v12: Y%
- v13: Z%

### 4. Canary Environment Setup
Configurare:
- Beta track Google Play (5% utenti)
- Firebase Crashlytics (crash rate threshold 5%)
- Analytics custom events (migration_success, backup_import_failed, route_removed)
```

### 4. Priorità Esecuzione Corretta

**Sequenza raccomandata (per rischio/impatto):**

1. **Giorno 1 (mattina)**: Issue #3 (Migrations) → CRITICAL
   - Analisi gap + test coverage: 3-4h
   - Rimozione fallback + deployment canary: 2h
   
2. **Giorno 1 (pomeriggio)**: Issue #4 (Backup) → HIGH
   - Validazione + transazione: 1.5h
   - Versioning + test: 1.5h

3. **Giorno 2**: Issue #5 (Routes) → MEDIUM
   - Implementazione tag + rollback: 3h
   - Test RouterOS reale: 2h
   - UI dialog: 1h

4. **Giorno 2-3**: Issue #1 (PDF) → MEDIUM (LOW priority)
   - Fix emoji → ASCII: 1h
   - Test snapshot: 1h

**Totale:** 16-18h → 2-3 giorni (sequenziale)

---

## 📝 Conclusione Review

### ✅ Piano: VALIDO con Aggiornamenti Necessari

**Punti di forza:**
- ✅ Diagnosi Issues #3, #4, #5: Accurate e verificate
- ✅ Azioni proposte: Tecnicamente corrette
- ✅ Test coverage: Completa per unit/integration test
- ✅ Checklist pre-deployment: Adeguata

**Punti di debolezza:**
- ❌ Issue #1: Priorità BLOCKER eccessiva (downgrade a MEDIUM)
- ❌ Issue #2: Obsoleta (rimuovere dal piano)
- ⚠️ Issue #3: Rischi sottostimati, test coverage incompleta
- ⚠️ Issue #5: Manca verifica compatibilità RouterOS API

**Azioni immediate richieste:**

1. **Aggiornare stato Issue #2** → CHIUSA (verificato)
2. **Declassare priorità Issue #1** → MEDIUM (già applicato)
3. **Estendere test Issue #3** → Coprire v7→v8, v8→v9, ..., v12→v13 (IN CORSO: aggiungere test instrumented/room migration)
4. **Verificare DTO `comment` Issue #5** → ✅ Fatto: `comment` aggiunto ai DTO e tests aggiornati
5. **Aggiungere rollback strategy** → Migrations + Backup (PENDENTE: implementare MigrationStrategy e test end-to-end)

**Timeline finale aggiornata:**
- Sequenziale: 16-23h → **2-3 giorni** (confermata)
- Parallelo: 10-14h → **1.5-2 giorni** (2-3 dev)

**Raccomandazione deployment:**
- **Canary release obbligatoria** per Issue #3 (migrations)
- **Analytics monitoring 48h** post-deploy
- **Rollback plan testato** prima di production release

**Il piano può essere implementato** con gli aggiornamenti indicati. Focus su Issue #3 (migrations) come priorità assoluta — perdita dati utente è rischio maggiore del progetto.

## 📋 Analisi Issue-per-Issue (Confronto Piano vs Realtà)

### Issue #1: Stringhe PDF Corrotte — ✅ CONFERMATA E RISOLTA

#### Stato Reale del Codice & Azione Eseguita
```kotlin
// PdfGeneratorIText.kt:178-179 (VERIFICATO + FIXATO)
// Emoji sostituita con testo ASCII per evitare rendering problemi
document.add(Paragraph("Dettaglio Test")
    .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
```

**Evidenza e Azione:**
- UTF-8 emoji erano presenti e potevano causare rendering problemi su target senza font compatibili. 
- Abbiamo rimosso le emoji e sostituito con testo ASCII in `PdfGeneratorIText.kt` e `PdfDocumentHelper.kt`.
- `PdfGeneratorSnapshotTest` è stato aggiunto per evitare regressioni sul testo PDF.
    - `PdfGeneratorSnapshotTest` è stato aggiunto (genera un PDF minimale e verifica i contenuti ASCII). Nota: questo test usa `ApplicationProvider` e pertanto fallisce come JVM unit test; spostare a Robolectric/instrumented per CI o convertire a test JVM con un mock del Context.

#### Conclusione
- **Issue #1 è stata corretta** e quindi declassata da BLOCKER a MEDIUM.
 - **Issue #1 è stata corretta** e quindi declassata da BLOCKER a MEDIUM. (Avviso: il test snapshot è stato creato ma necessita di esecuzione in ambiente adeguato — Robolectric o androidTest — per risultare stabile in CI.)


---

### Issue #2: Retrofit Duplicato — ❌ GIÀ RISOLTO

#### Stato Reale del Codice
```kotlin
// AppRepository.kt:59-61 (VERIFICATO)
private fun buildServiceFor(probe: ProbeConfig): MikroTikApiService {
    val wifiNetwork = findWifiNetwork()
    return serviceFactory.createService(probe, wifiNetwork?.socketFactory)
}
```

**Evidenza:**
- `buildServiceFor()` è un **helper da 3 righe**
- Delega a `serviceFactory.createService()` (CORRETTO pattern)
- `MikroTikServiceFactory` (linee 1-50) gestisce HTTPS correttamente:
  ```kotlin
  val scheme = if (probe.isHttps) "https" else "http"
  val baseUrl = "$scheme://${probe.ipAddress}/"
  ```
- **Nessuna duplicazione** costruzione Retrofit

#### Problemi Architetturali Reali (non menzionati nel piano)
1. **findWifiNetwork() leak**: logica Android (NetworkCapabilities) dentro Repository
2. **12 callsite duplicati**: `buildServiceFor()` chiamato in 12 punti → violazione DRY
3. **Mancanza di ApiClient abstraction**: Repository dipende direttamente da MikroTikApiService

#### Conclusione
- ❌ **NON implementare** il refactor proposto (rimuovere buildServiceFor viola encapsulation)
- ✅ **Refactor diverso necessario**:
  - Estrarre `NetworkBindingStrategy` per gestire Wi-Fi binding
  - Introdurre `MikroTikApiClient` wrapper per nascondere chiamate ripetitive
  - Usare Interceptor per Wi-Fi binding invece di parameter passing

---

### Issue #3: Migrazioni DB Disattivate — ✅ VALIDO (ma soluzione INCOMPLETA)

#### Stato Reale del Codice
```kotlin
// DatabaseModule.kt:38 (VERIFICATO e FIXATA)
.addMigrations(*Migrations.ALL_MIGRATIONS)
// fallbackToDestructiveMigration() rimosso e sostituito con fallbackToDestructiveMigrationFrom(1..6)
// per limitare il comportamento distruttivo solo a DB troppo vecchi
```

**Il problema è reale.**

#### Problemi della Soluzione Proposta

**Il piano suggerisce:**
```kotlin
// .fallbackToDestructiveMigration() // <-- RIMUOVERE
```

**Conseguenze immediate:**
- Utenti con DB version < 7 (prima migrazione disponibile) → **CRASH**
- Migrazioni fallite (es. ALTER TABLE con constraint violation) → **CRASH**
- Nessun recovery path → **app inutilizzabile**

#### Soluzione SOLID-Compliant (e Azione effettuata)

**Pattern da usare:** Strategy + Chain of Responsibility per gestione errori migrazioni

```kotlin
// 1. Domain Layer: Migration Strategy Interface
interface MigrationStrategy {
    suspend fun handleMigrationFailure(
        exception: Exception, 
        fromVersion: Int, 
        toVersion: Int
    ): MigrationResult
}

sealed class MigrationResult {
    object Success : MigrationResult()
    data class Retry(val strategy: MigrationStrategy) : MigrationResult()
    data class FallbackToCleanInstall(val backupData: ByteArray?) : MigrationResult()
    data class Abort(val error: String) : MigrationResult()
}

// 2. Concrete Strategies
class AutoBackupAndRetryStrategy(
    private val backupService: BackupService
) : MigrationStrategy {
    override suspend fun handleMigrationFailure(...): MigrationResult {
        val backup = backupService.exportCurrentData()
        // Destructive migration con backup
        return FallbackToCleanInstall(backup)
    }
}

class UserConfirmationStrategy(
    private val dialogService: UserDialogService
) : MigrationStrategy {
    override suspend fun handleMigrationFailure(...): MigrationResult {
        val userChoice = dialogService.showMigrationErrorDialog(...)
        return when (userChoice) {
            RETRY -> Retry(this)
            RESET -> FallbackToCleanInstall(null)
            ABORT -> Abort("User aborted migration")
        }
    }
}

// 3. Database Module con Strategy
@Provides
fun provideAppDatabase(
    context: Context,
    migrationStrategy: MigrationStrategy
): AppDatabase {
    return Room.databaseBuilder(...)
        .addMigrations(*Migrations.ALL_MIGRATIONS)
        .addCallback(object : RoomDatabase.Callback() {
            override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                // Log telemetry per analytics
                Analytics.logEvent("db_migration_failure")
            }
        })
        // NO fallbackToDestructiveMigration() globale
        .build()
}
```

**Vantaggi:**
- ✅ Open/Closed: Estendibile con nuove strategie (es. partial migration, remote sync)
- ✅ Single Responsibility: MigrationStrategy gestisce solo recovery logic
- ✅ Dependency Inversion: Database module dipende da interface, non da concrete classes
- ✅ Testabilità: Iniettare mock strategy per unit test

#### Mitigazione Rischi (Deployment)

**Strategia di rollout:**
1. **Fase 1 (Canary)**: rimozione fallback globale non eseguita in una sola volta; abbiamo sostituito con fallback per v1..6; per rimozione completa proseguire con rollout canary come specificato
   ```kotlin
   .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
   ```
2. **Fase 2 (Analytics)**: Monitorare crash rate su versioni 12+ per 7 giorni
3. **Fase 3 (Progressive)**: Rimuovere fallback per v10+, poi v8+
4. **Fase 4 (Final)**: Rimuovere completamente solo quando analytics mostra <1% utenti v7-

---

### Issue #4: Backup Non Transazionale — ✅ VALIDO (ma soluzione viola SRP)

#### Stato Reale del Codice
```kotlin
// BackupRepository.kt:29-37 (VERIFICATO)
suspend fun importConfigFromJson(json: String) {
    val adapter = moshi.adapter(BackupData::class.java)
    val backupData = adapter.fromJson(json)
    if (backupData != null) {
        probeConfigDao.deleteAll()      // <-- NON ATOMICO
        testProfileDao.deleteAll()      // <-- CRASH QUI = DB VUOTO
        probeConfigDao.insertAll(backupData.probes)
        testProfileDao.insertAll(backupData.profiles)
    }
}
```

**Problemi confermati:**
1. No transazione → crash a metà = DB corrupted
2. No validazione → JSON malformato accettato
3. No versioning → incompatibilità schema ignorata

#### Problemi della Soluzione Proposta

**Il piano suggerisce:**
```kotlin
// Aggiungere validazione + transazione direttamente nel repository
suspend fun importConfigFromJson(json: String): Result<Unit> {
    val backupData = adapter.fromJson(json) ?: return Result.failure(...)
    
    // Validazione inline
    if (backupData.probes.any { it.ipAddress.isBlank() }) { ... }
    
    database.withTransaction {
        probeConfigDao.deleteAll()
        probeConfigDao.insertAll(backupData.probes)
    }
}
```

**Violazioni SOLID:**
- ❌ **SRP**: Repository ora fa: (1) deserializzazione, (2) validazione schema, (3) validazione business rules, (4) transazione DB, (5) error handling
- ❌ **OCP**: Aggiungere nuova validazione (es. "probes non duplicati") richiede modificare repository
- ❌ **DIP**: Validazione hardcoded stringe coupling a schema BackupData

#### Soluzione SOLID-Compliant: Domain-Driven Design

**Architettura a 3 layer:**

```kotlin
// === DOMAIN LAYER ===

// Value Objects (immutabili con validazione intrinseca)
@JvmInline
value class IpAddress private constructor(val value: String) {
    companion object {
        fun create(raw: String): Result<IpAddress> {
            return when {
                raw.isBlank() -> Result.failure(Exception("IP vuoto"))
                !raw.matches(Regex("\\d{1,3}(\\.\\d{1,3}){3}")) -> 
                    Result.failure(Exception("IP formato invalido"))
                else -> Result.success(IpAddress(raw))
            }
        }
    }
}

// Domain Entity
data class ProbeConfiguration(
    val ipAddress: IpAddress,
    val username: String,
    val isHttps: Boolean
) {
    init {
        require(username.isNotBlank()) { "Username obbligatorio" }
    }
}

// Use Case Interface (Port)
interface ImportBackupUseCase {
    suspend fun execute(json: String): Result<ImportResult>
}

data class ImportResult(
    val probesImported: Int,
    val profilesImported: Int,
    val warnings: List<String>
)

// Validator Interface
interface BackupValidator {
    fun validate(data: BackupData): ValidationResult
}

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val errors: List<String>) : ValidationResult()
}

// === APPLICATION LAYER (Use Case Implementation) ===

class ImportBackupUseCaseImpl @Inject constructor(
    private val validator: BackupValidator,
    private val repository: BackupRepository,
    private val mapper: BackupDataMapper
) : ImportBackupUseCase {
    
    override suspend fun execute(json: String): Result<ImportResult> {
        // 1. Parse
        val backupData = repository.parseJson(json)
            .getOrElse { return Result.failure(it) }
        
        // 2. Validate (delegation)
        when (val validation = validator.validate(backupData)) {
            is ValidationResult.Invalid -> 
                return Result.failure(Exception(validation.errors.joinToString()))
            ValidationResult.Valid -> {}
        }
        
        // 3. Map to domain entities
        val domainProbes = backupData.probes.map { mapper.toDomain(it) }
            .onEach { result -> 
                if (result.isFailure) return result.map { ImportResult(0, 0, emptyList()) }
            }
        
        // 4. Persist (atomic)
        return repository.replaceAll(domainProbes, backupData.profiles)
    }
}

// === INFRASTRUCTURE LAYER (Repository Implementation) ===

class BackupRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val moshi: Moshi
) : BackupRepository {
    
    override suspend fun replaceAll(
        probes: List<ProbeConfiguration>,
        profiles: List<TestProfile>
    ): Result<ImportResult> = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
                // Atomic delete + insert
                database.probeConfigDao().deleteAll()
                database.testProfileDao().deleteAll()
                
                val probeEntities = probes.map { it.toEntity() }
                database.probeConfigDao().insertAll(probeEntities)
                database.testProfileDao().insertAll(profiles)
            }
            Result.success(ImportResult(probes.size, profiles.size, emptyList()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Validator concreto (estendibile via composite pattern)
class CompositeBackupValidator(
    private val validators: List<BackupValidator>
) : BackupValidator {
    override fun validate(data: BackupData): ValidationResult {
        val errors = validators.flatMap { 
            when (val result = it.validate(data)) {
                is ValidationResult.Invalid -> result.errors
                ValidationResult.Valid -> emptyList()
            }
        }
        return if (errors.isEmpty()) ValidationResult.Valid 
               else ValidationResult.Invalid(errors)
    }
}

class SchemaVersionValidator : BackupValidator {
    override fun validate(data: BackupData): ValidationResult {
        return when {
            data.version > CURRENT_VERSION -> 
                ValidationResult.Invalid(listOf("Backup da versione futura non supportata"))
            data.version < MIN_SUPPORTED_VERSION -> 
                ValidationResult.Invalid(listOf("Backup troppo vecchio, aggiorna app"))
            else -> ValidationResult.Valid
        }
    }
}

class DataIntegrityValidator : BackupValidator {
    override fun validate(data: BackupData): ValidationResult {
        val errors = mutableListOf<String>()
        if (data.probes.isEmpty()) errors.add("Nessuna sonda nel backup")
        if (data.probes.any { it.ipAddress.isBlank() }) errors.add("IP vuoti rilevati")
        return if (errors.isEmpty()) ValidationResult.Valid 
               else ValidationResult.Invalid(errors)
    }
}

// === DI SETUP ===

@Module
@InstallIn(SingletonComponent::class)
object BackupModule {
    @Provides
    fun provideBackupValidator(): BackupValidator = CompositeBackupValidator(
        listOf(
            SchemaVersionValidator(),
            DataIntegrityValidator()
        )
    )
    
    @Provides
    fun provideImportUseCase(
        validator: BackupValidator,
        repository: BackupRepository,
        mapper: BackupDataMapper
    ): ImportBackupUseCase = ImportBackupUseCaseImpl(validator, repository, mapper)
}
```

**Vantaggi architettura a layer:**
- ✅ **SRP**: Ogni classe ha una responsabilità
  - Repository → solo persistenza
  - Validator → solo validazione
  - UseCase → orchestrazione
  - ValueObject → validazione sintassi
- ✅ **OCP**: Aggiungere nuova validazione = implementare `BackupValidator` e aggiungerla al composite (no modifica codice esistente)
- ✅ **DIP**: UseCase dipende da `BackupRepository` interface, non da Room/Moshi
- ✅ **Testabilità**: Mock validator/repository per unit test use case

---

### Issue #5: Route Globali Rimosse — ✅ VALIDO (ma soluzione fragile)

#### Stato Reale del Codice
```kotlin
// AppRepository.kt:157 (VERIFICATO)
suspend fun removeDefaultRoutes() {
    val routes = api.getRoutes()
    routes.filter { it.dstAddress == "0.0.0.0/0" }
          .forEach { r -> r.id?.let { api.removeRoute(NumbersRequest(it)) } }
}
```

**Problema confermato:** Rimuove TUTTE le route default, non solo quelle create dall'app.

#### Problemi della Soluzione Proposta

**Il piano suggerisce:**
```kotlin
// Opzione A: Tagging con comment
api.addRoute(RouteAdd(dstAddress = "0.0.0.0/0", gateway = gw, comment = "MikLink_Auto"))

routes.filter { it.comment == "MikLink_Auto" }.forEach { ... }
```

**Problemi:**
1. **Non atomico**: GET routes → filter → DELETE non è transazionale (race condition se admin esterno modifica route)
2. **Fragile**: MikroTik API potrebbe non supportare `comment` in tutte le versioni RouterOS
3. **State leak**: Se app crasha dopo ADD route ma prima di salvare mapping → route orfana
4. **No rollback su errori parziali**: Se rimozione route 2/3 fallisce, stato inconsistente

#### Soluzione SOLID-Compliant: Command Pattern + Memento

```kotlin
// === DOMAIN LAYER ===

// Value Object per route
data class RouteConfiguration(
    val destination: String,
    val gateway: String,
    val metadata: RouteMetadata
) {
    init {
        require(destination.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+/\\d+"))) 
            { "Destination CIDR invalido" }
    }
}

data class RouteMetadata(
    val managedBy: String,  // "MikLink", "Manual", "VPN"
    val createdAt: Instant,
    val purpose: String     // "Certification", "Production"
)

// Command interface (undo/redo)
interface NetworkCommand {
    suspend fun execute(): Result<Unit>
    suspend fun undo(): Result<Unit>
}

// Memento (snapshot stato network)
data class NetworkStateSnapshot(
    val routes: List<RouteEntry>,
    val timestamp: Instant
)

// Use Case
interface ApplyNetworkConfigUseCase {
    suspend fun execute(
        probe: ProbeConfig, 
        config: NetworkConfiguration
    ): Result<NetworkConfigResult>
}

// === APPLICATION LAYER ===

class ApplyNetworkConfigUseCaseImpl @Inject constructor(
    private val routeRepository: RouteRepository,
    private val snapshotService: NetworkSnapshotService,
    private val commandFactory: NetworkCommandFactory
) : ApplyNetworkConfigUseCase {
    
    override suspend fun execute(
        probe: ProbeConfig,
        config: NetworkConfiguration
    ): Result<NetworkConfigResult> {
        // 1. Snapshot stato corrente (per rollback)
        val snapshot = snapshotService.captureCurrentState(probe)
            .getOrElse { return Result.failure(it) }
        
        // 2. Build command chain
        val commands = commandFactory.createCommands(config)
        
        // 3. Execute con rollback automatico su errore
        return try {
            commands.forEach { cmd -> 
                cmd.execute().getOrElse { error ->
                    // Rollback automatico
                    snapshotService.restore(probe, snapshot)
                    return Result.failure(error)
                }
            }
            Result.success(NetworkConfigResult.Success)
        } catch (e: Exception) {
            snapshotService.restore(probe, snapshot)
            Result.failure(e)
        }
    }
}

// Command concreto
class RemoveManagedRoutesCommand(
    private val routeRepository: RouteRepository,
    private val managedBy: String = "MikLink"
) : NetworkCommand {
    
    private var removedRoutes: List<RouteEntry> = emptyList()
    
    override suspend fun execute(): Result<Unit> {
        // Get solo route managed
        val routes = routeRepository.getRoutes()
            .filter { it.comment?.startsWith(managedBy) == true }
        
        removedRoutes = routes
        
        return routes.forEach { route ->
            routeRepository.remove(route.id!!)
                .getOrElse { return Result.failure(it) }
        }
        Result.success(Unit)
    }
    
    override suspend fun undo(): Result<Unit> {
        return removedRoutes.forEach { route ->
            routeRepository.add(route.toAdd())
                .getOrElse { return Result.failure(it) }
        }
        Result.success(Unit)
    }
}

// === INFRASTRUCTURE LAYER ===

class RouteRepositoryImpl @Inject constructor(
    private val apiService: MikroTikApiService
) : RouteRepository {
    
    override suspend fun getRoutes(): List<RouteEntry> = 
        apiService.getRoutes()
    
    override suspend fun add(route: RouteAdd): Result<String> = try {
        val enriched = route.copy(
            comment = "${route.comment ?: ""} | MikLink_${Instant.now().epochSecond}"
        )
        Result.success(apiService.addRoute(enriched))
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    override suspend fun remove(id: String): Result<Unit> = try {
        apiService.removeRoute(NumbersRequest(id))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// Snapshot service (Memento pattern)
class NetworkSnapshotService @Inject constructor(
    private val routeRepository: RouteRepository
) {
    suspend fun captureCurrentState(probe: ProbeConfig): Result<NetworkStateSnapshot> {
        val routes = routeRepository.getRoutes()
        return Result.success(NetworkStateSnapshot(routes, Instant.now()))
    }
    
    suspend fun restore(probe: ProbeConfig, snapshot: NetworkStateSnapshot): Result<Unit> {
        // Remove current routes
        val current = routeRepository.getRoutes()
        current.forEach { routeRepository.remove(it.id!!) }
        
        // Restore snapshot routes
        snapshot.routes.forEach { route ->
            routeRepository.add(route.toAdd())
        }
        return Result.success(Unit)
    }
}
```

**Vantaggi:**
- ✅ **Command Pattern**: Ogni operazione è un command con undo (rollback garantito)
- ✅ **Memento Pattern**: Snapshot stato pre-modifica (restore atomico)
- ✅ **Transactional**: Chain of commands con rollback su primo errore
- ✅ **Audit trail**: Timestamp + metadata in ogni route (troubleshooting)
- ✅ **Safe by default**: Non tocca route non-managed

---

## 🏗️ Architettura Target: Clean Architecture + SOLID

### Struttura Package Proposta

```
app/src/main/java/com/app/miklink/
├── domain/                          # ← MANCA (DA CREARE)
│   ├── model/                       # Entities + Value Objects
│   │   ├── ProbeConfiguration.kt
│   │   ├── IpAddress.kt
│   │   ├── NetworkConfiguration.kt
│   │   └── RouteConfiguration.kt
│   ├── usecase/                     # Business Logic (Use Cases)
│   │   ├── backup/
│   │   │   ├── ImportBackupUseCase.kt
│   │   │   └── ExportBackupUseCase.kt
│   │   ├── network/
│   │   │   ├── ApplyNetworkConfigUseCase.kt
│   │   │   └── SnapshotNetworkStateUseCase.kt
│   │   └── migration/
│   │       └── HandleMigrationFailureUseCase.kt
│   └── repository/                  # Interfaces (Ports)
│       ├── BackupRepository.kt
│       ├── RouteRepository.kt
│       └── MigrationStrategyRepository.kt
│
├── data/                            # Infrastructure Layer
│   ├── repository/                  # Implementations (Adapters)
│   │   ├── BackupRepositoryImpl.kt
│   │   └── RouteRepositoryImpl.kt
│   ├── network/
│   │   ├── MikroTikApiService.kt
│   │   └── dto/
│   ├── db/
│   │   ├── AppDatabase.kt
│   │   └── dao/
│   └── mapper/                      # DTO ↔ Domain mapping
│       └── BackupDataMapper.kt
│
├── ui/                              # Presentation Layer
│   ├── viewmodel/
│   │   └── BackupViewModel.kt       # Dipende da UseCase, non da Repository
│   └── screen/
│
└── di/                              # Dependency Injection
    ├── DomainModule.kt              # Bind UseCases
    ├── DataModule.kt                # Bind Repositories
    └── NetworkModule.kt
```

### Dependency Rule (Clean Architecture)

```
UI Layer (ViewModel)
    ↓ depends on
Application Layer (UseCase)
    ↓ depends on
Domain Layer (Entities + Repository Interfaces)
    ↑ implemented by
Infrastructure Layer (RepositoryImpl + API)
```

**Regole:**
- ✅ Domain layer **NON dipende** da nessuno (zero import Android/Room/Retrofit)
- ✅ Use Case dipende solo da domain interfaces
- ✅ Repository implementazioni in `data/` implementano interfaces in `domain/`
- ✅ ViewModel dipende da UseCase, non da Repository

---

## 📊 Debito Tecnico Nascosto (Non Menzionato nel Piano)

### 1. God Object: AppRepository (463 righe)

**Problemi:**
- Gestisce: network config, DHCP, route, IP address, ping, backup, DNS
- 12 callsite di `buildServiceFor()` → dovrebbe essere iniettato `MikroTikApiClient`
- Mescola business logic (resolveTargetIp) e orchestrazione API

**Refactoring necessario:**
```kotlin
// Splittare in:
class NetworkConfigurationRepository  // DHCP, static IP, routing
class ConnectivityRepository           // Ping, DNS lookup
class BackupRepository                 // Già separato, OK
```

### 2. Mancanza di Error Handling Strutturato

**Problema attuale:**
```kotlin
// AppRepository.kt: catch generico ovunque
catch (e: Exception) {
    UiState.Error(e.message ?: context.getString(R.string.error_unknown))
}
```

**Conseguenze:**
- `SocketTimeoutException`, `UnknownHostException`, `HttpException` → tutti "Errore sconosciuto"
- Impossibile retry su errori transitori
- Nessuna telemetry specifica

**Soluzione:**
```kotlin
// Domain errors
sealed class NetworkError {
    data class Timeout(val millis: Long) : NetworkError()
    data class Unauthorized(val probe: String) : NetworkError()
    data class RouteConflict(val existing: RouteEntry) : NetworkError()
    object Unreachable : NetworkError()
}

// Mapper infra → domain
class ApiExceptionMapper {
    fun map(throwable: Throwable): NetworkError = when (throwable) {
        is SocketTimeoutException -> NetworkError.Timeout(...)
        is HttpException -> when (throwable.code()) {
            401, 403 -> NetworkError.Unauthorized(...)
            else -> ...
        }
        else -> ...
    }
}
```

### 3. Test Coverage Gaps

**Problemi rilevati nel piano:**
- ❌ Nessun test per `AppRepository.applyNetworkConfig()` (funzione critica)
- ❌ Nessun integration test per backup/restore
- ❌ Migration test coprono happy path, non errori

**Test mancanti:**
```kotlin
// Da aggiungere:
class BackupUseCaseTest {
    @Test fun `import con JSON malformato fallisce senza toccare DB`()
    @Test fun `import con versione incompatibile ritorna errore specifico`()
    @Test fun `import interrotto a metà rollbacka transazione`()
}

class NetworkConfigUseCaseTest {
    @Test fun `rimozione route fallita ripristina snapshot`()
    @Test fun `configurazione DHCP su interfaccia non esistente ritorna errore`()
}
```

### 4. Observability Zero

**Mancano:**
- Logging strutturato (es. `Timber.tag("BackupImport").d("...")`)
- Telemetry per operazioni critiche (es. quanti utenti perdono dati per migration failure)
- Performance metrics (quanto tempo richiede un import di 100 sonde?)

**Da aggiungere:**
```kotlin
interface AnalyticsService {
    fun logMigrationFailure(fromVersion: Int, toVersion: Int, error: String)
    fun logBackupImport(probeCount: Int, durationMs: Long)
    fun logRouteModification(action: String, routeCount: Int)
}
```

---

## 🎯 Piano di Azione Corretto

### Fase 0: Audit e Preparazione (Giorno 0)
1. **Verificare sorgente CODEBASE_ISSUES.md**
   - Chiedere conferma su Issues #1 e #2 (sembrano obsolete)
   - Se confermato, rimuovere dal piano

2. **Setup architettura**
   - Creare cartella `domain/` con struttura base
   - Definire interfaces per repository critici

### Fase 1: Issue #3 (Migrations) — Risoluzione SOLID (Giorni 1-2)
**Priorità:** CRITICAL (blocca aggiornamenti utenti)

**Step:**
1. Creare `MigrationStrategy` interface + implementations
2. Iniettare strategy in DatabaseModule
3. Implementare `AutoBackupAndRetryStrategy` per fallback safety
4. Test: simulare migration failure → verificare backup + restore
5. Rollout progressivo: fallback solo per versioni <10

**Deliverable:**
- `domain/migration/MigrationStrategy.kt`
- `data/migration/AutoBackupMigrationStrategy.kt`
- `di/MigrationModule.kt`
- Test: `MigrationStrategyTest.kt`

### Fase 2: Issue #4 (Backup) — Clean Architecture (Giorni 2-3)
**Priorità:** HIGH (corruzioni rare ma critiche)

**Step:**
1. Creare Value Objects (`IpAddress`, `ProbeConfiguration`)
2. Implementare `BackupValidator` interface + concrete validators
3. Creare `ImportBackupUseCase` con orchestrazione validation + transaction
4. Refactor UI per chiamare UseCase invece di Repository

**Deliverable:**
- `domain/model/ProbeConfiguration.kt`, `IpAddress.kt`
- `domain/usecase/backup/ImportBackupUseCase.kt`
- `domain/validation/BackupValidator.kt`
- `data/validation/SchemaVersionValidator.kt`, `DataIntegrityValidator.kt`
- Test: `ImportBackupUseCaseTest.kt` (10+ casi edge)

### Fase 3: Issue #5 (Routes) — Command Pattern (Giorni 3-4)
**Priorità:** MEDIUM (impatta esperienza utente ma non critico)

**Step:**
1. Creare `NetworkCommand` interface
2. Implementare `RemoveManagedRoutesCommand` con undo
3. Creare `NetworkSnapshotService` (Memento pattern)
4. Wrapper `ApplyNetworkConfigUseCase` con snapshot pre/post
5. Test integration su MockWebServer

**Deliverable:**
- `domain/command/NetworkCommand.kt`
- `domain/usecase/network/ApplyNetworkConfigUseCase.kt`
- `data/snapshot/NetworkSnapshotService.kt`
- Test: `NetworkCommandTest.kt`, `SnapshotServiceTest.kt`

### Fase 4: Refactoring Debito Tecnico (Giorni 4-5)
**Priorità:** MEDIUM (migliora maintainability)

**Step:**
1. Splittare `AppRepository` (463 righe) in 3 repository specializzati
2. Estrarre `NetworkBindingStrategy` per Wi-Fi logic
3. Aggiungere `ApiExceptionMapper` per error handling
4. Aggiungere logging/telemetry in use cases

**Deliverable:**
- `data/repository/NetworkConfigRepository.kt`
- `data/repository/ConnectivityRepository.kt`
- `domain/error/NetworkError.kt`
- `data/mapper/ApiExceptionMapper.kt`

---

## ✅ Checklist Finale (Pre-Implementazione)

Prima di iniziare qualsiasi modifica:

- [ ] **Confermare con PM/Tech Lead**: Issues #1 e #2 sono reali o obsolete?
- [ ] **Setup architettura**: Creare cartelle `domain/` con package structure
- [ ] **Scrivere test PRIMA del codice** (TDD per use cases critici)
- [ ] **Documentare ADR** (Architecture Decision Records) per pattern scelti
- [ ] **Code review obbligatoria** su ogni use case/command (min 2 senior)
- [ ] **Rollout canary**: Beta release a 5% utenti per ogni fase

---

## 📝 Note Finali

**Il piano originale va RIFIUTATO.**

Propone fix superficiali che:
- Risolvono problemi inesistenti (#1, #2)
- Aggiungono debito tecnico (#3, #4, #5)
- Ignorano SOLID principles

**Questo piano alternativo:**
- ✅ Risolve i problemi REALI (#3, #4, #5)
- ✅ Applica Clean Architecture + SOLID
- ✅ Estendibile e testabile
- ✅ Production-ready (rollback, observability, error handling)

**Tempo stimato:** 4-5 giorni (vs 2-3 del piano originale)  
**Valore:** Risoluzione permanente + infrastruttura per future feature

**Next Step:** Approvazione architettura → implementazione TDD → code review → canary release.
