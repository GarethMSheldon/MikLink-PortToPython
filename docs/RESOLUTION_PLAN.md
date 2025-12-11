# Piano di Risoluzione — Codebase Issues MikLink

**Data creazione:** 2025-12-10  
**Riferimento:** `docs/CODEBASE_ISSUES.md`  
**Responsabile:** Tech Lead / Team Development

---

## Refactor: God Object - `AppRepository` (Cleanup)

### Problema
- `AppRepository` è diventato un oggetto troppo grande (463 righe): gestisce troppi aspetti (DB, networking, route config, DHCP, PDF orchestration) e diventa difficile da mantenere e testare.

### Obiettivo
- Ridurre la responsabilità dell'`AppRepository` suddividendo in componenti specifici: `RouteManager`, `DhcpManager` (se necessario), `NetworkConfigManager`/UseCases e mantenere `AppRepository` come orchestration point che coordina tra i manager.

### Benefici
- Facilità di testing: componenti più piccoli, test unitari più mirati.
- Ridotta superficie di regressione e migliori invarianti di business.
- Telescoping: possibilità di integrare nuove strategie (es. differenti rollback strategies) senza cambiare AppRepository.

### Refactor Plan (Passi)
1. **Estrarre `RouteManager`** (prima attività): un'interfaccia `RouteManager` con implementazione `RouteManagerImpl` che racchiuda tutte le operazioni legate alle route (`removeDefaultRoutes`, `addDefaultRoute`, `listRoutes`).
  - `RouteManager` API minima: `suspend removeDefaultRoutes(api: MikroTikApiService, expectedGateway: String? = null, dryRun: Boolean = false)` e `suspend addDefaultRoute(api: MikroTikApiService, gateway: String)`.
2. **Trasferire la logica** attualmente in `AppRepository.removeDefaultRoutes()` al `RouteManagerImpl`, applicando i test di unit e rollback.
3. **Creare modulo DI** `RepositoryModule` per bindare `RouteManager` a `RouteManagerImpl` con `@Binds`.
4. **Aggiornare `AppRepository`**: rimuovere la porzione `removeDefaultRoutes` e iniettarvi `RouteManager`. Aggiornare i flussi `applyClientNetworkConfig` e `getCurrentInterfaceIpConfig` a usare il manager.
5. **Refactor iterativi**: valutare l'estrazione di altre responsabilità (backup, DB migration helpers, PDF orchestration) come step successivo.

### Test Plan per il Refactor
1. Unit tests per `RouteManagerImpl`:
  - Rimozione solo per `comment` o gateway match;
  - Dry-run non invoca `removeRoute`;
  - Rollback in caso di errore (simulate `removeRoute` throwing exception), `addRoute` ri-crea le rotte rimosse;
2. Unit tests per `AppRepository`:
  - Verifica che AppRepository invochi `RouteManager.removeDefaultRoutes(api, expectedGateway)` con gateway corretto;
  - Assicurarsi che il resto dell'AppRepository non regredisca (run existing tests).
3. Integration tests (MockWebServer) per il nuovo manager:
  - Simulare scenari di risposta e downtime, verificare che i comportamenti siano corretti (rollback, dry-run).

### Stima Tempo
- Implementazione `RouteManager`: 2-3h (incl. tests)
- Aggiornamento `AppRepository` e test: 1-2h
- Smoke tests manuale routerOS: 1-2h

### Acceptance Criteria
- ✅ `RouteManager` esiste con API pubblica e implementazione testata.
- ✅ `AppRepository` inietta e usa `RouteManager` per la gestione di route senza avere la logica implementata direttamente.
- ✅ Unit & Integration tests per la gestione route passano.


## Sommario Esecutivo

Sono stati identificati **5 problemi critici** che bloccano o compromettono funzionalità core del progetto MikLink:

1. **[MEDIUM - RISOLTO]** Stringhe PDF non-ASCII (emoji) - problemi di rendering in alcuni ambienti (ora sostituite)
2. **[CRITICAL]** Costruzione Retrofit duplicata (HTTPS non funzionante)
3. **[HIGH]** Migrazioni DB disattivate (perdita dati utente)
4. **[HIGH]** Backup non transazionale (corruzioni possibili)
5. **[MEDIUM]** Route default globali rimosse (impatto su rete del router)

**Impatto complessivo:** Il sistema può presentare malfunzionamenti a livello di rendering PDF su dispositivi senza font emoji, potenziali problemi nelle connessioni HTTPS, rischio di perdita dati utente e possibili corruzioni di rete del dispositivo sotto test.

**Tempo stimato risoluzione completa:** 2-3 giorni lavorativi (sequenziale) o 1-2 giorni (parallelo con 2-3 sviluppatori)

---

## Classificazione per Priorità e Dipendenze

### Priority 0 — BLOCKER (risoluzione immediata)
// Nessun problema bloccante attivo al momento - priorità spostata verso Data Integrity e Backup

### Priority 1 — CRITICAL (risoluzione entro 24h)
- **Issue #2:** Retrofit duplicato → **HTTPS non funziona, chiamate API falliscono**

### Priority 2 — HIGH (risoluzione entro 48h)
- **Issue #3:** Migrazioni DB disattivate → **aggiornamenti app cancellano dati utente**
- **Issue #4:** Backup non transazionale → **possibili corruzioni durante import/export**

### Priority 3 — MEDIUM (risoluzione entro 72h)
 - **Issue #5:** Route default globali → **impatto su configurazione router, richiede rollback**
 - **Issue #1 (RISOLTO):** Stringhe PDF non-ASCII/emoji → Sostituite con testo ASCII e test aggiunto

---

## Issue #1: Stringhe PDF corrotte (MEDIUM) — RISOLTO

- **File:** `app/src/main/java/com/app/miklink/data/pdf/PdfGeneratorIText.kt:178` (e `PdfDocumentHelper.kt:92, :143`)
- **Evidenza:** La codebase conteneva caratteri non-ASCII (emojis) nel contenuto dei paragrafi per il PDF (`"📋 Dettaglio Test"`, `"⚠️ Rilevato carico CPU"`). Questi caratteri possono causare rendering inatteso su alcune piattaforme se il font usato da iText non supporta emoji.
- **Impatto:** Non blocca la compilazione Kotlin: build locale e CI risultavano UP-TO-DATE; tuttavia il rendering PDF, in assenza di font emoji, può produrre simboli illegibili o fallback grafico. 
- **Root cause:** Corruzione durante editing (encoding errato o copia/incolla da binari)

### Piano di risoluzione

#### Azioni specifiche
1. **Identificare tutte le stringhe non ASCII / emoji** in `PdfGeneratorIText.kt` (linee ~178, e verificare `PdfDocumentHelper.kt` :92, :143)
   ```bash
   grep -n '[^\x00-\x7F]' app/src/main/java/com/app/miklink/data/pdf/PdfGeneratorIText.kt
   ```

2. **Sostituzione e validazione (FASE APPLICATA):**
  - `"📋 Dettaglio Test"` è stato sostituito con `"Dettaglio Test"` in `PdfGeneratorIText.kt`.
  - `"⚠️ Rilevato carico CPU 100%..."` è stato sostituito con `"Attenzione: carico CPU 100%..."` in `PdfDocumentHelper.kt`.
   - Warning CPU/memoria → testo leggibile inglese/italiano (es. "CPU Usage", "Memoria disponibile")
   - Footer → `"Generato il {timestamp} con MikLink v{version}"`

3. **Verificare encoding file:**
   - Assicurarsi che il file sia salvato in **UTF-8 without BOM**
   - Controllare `.editorconfig` / IDE settings per encoding Kotlin

4. **Aggiungere test snapshot PDF (COMPLETATO):**
  - Aggiunto `PdfGeneratorSnapshotTest.kt` che genera un PDF minimale con `PdfGeneratorIText` e verifica:
    - presenza di testo ASCII (es. "Dettaglio Test")
    - assenza di caratteri di controllo non stampabili
  - Il test aiuta ad evitare regressioni in futuro nel rendering del testo nei PDF.
   ```kotlin
   @Test
   fun `generated PDF contains valid UTF-8 strings`() {
       val pdfBytes = pdfGenerator.generatePdfReport(...)
       val pdfText = extractTextFromPdf(pdfBytes)
       assertThat(pdfText).contains("Dettaglio Test")
       assertThat(pdfText).doesNotContainMatch("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]") // no binari
   }
   ```

#### File da modificare
- `app/src/main/java/com/app/miklink/data/pdf/PdfGeneratorIText.kt` (fix stringhe linee ~170-200)
- `app/src/main/java/com/app/miklink/data/pdf/PdfDocumentHelper.kt` (fix linee ~92, ~143 se presenti)
- `app/src/test/java/com/app/miklink/data/pdf/PdfGeneratorSnapshotTest.kt` (nuovo test)

#### Rischi
- **Basso:** Le stringhe erano letterali; la sostituzione è meccanica e non introduce rischi funzionali.
- **NB:** Se si desidera supportare emoji nei PDF, è necessario includere font compatibile (TTF) e applicare embedding con iText; questa è un'operazione opzionale da pianificare.

#### Validazione (COMPLETATO)
- Build Kotlin compila senza errori
- Test `PdfGeneratorSnapshotTest` aggiunto (unit test): verifica il contenuto del PDF e assenza di control chars
- Smoke test manuale: PDF generato mostra testo leggibile

#### Stima tempo
- **Effort realizzato:** ~1.5 ore (fix su 2 file, test aggiunto, esecuzione test)


---

## Issue #2: Costruzione Retrofit duplicata (CRITICAL)

### Problema
- **File:** `app/src/main/java/com/app/miklink/data/repository/AppRepository.kt:62-90` (metodo `buildServiceFor`)
- **Evidenza:** Il repository costruisce manualmente un client Retrofit custom, **ignorando `probe.isHttps`** e duplicando la logica già presente in `MikroTikServiceFactory`
- **Impatto:** 
  - Le chiamate API forzano sempre `http://` anche se la sonda è configurata per HTTPS → connessione fallisce
  - AuthInterceptor e configurazione client divergono tra factory e repository
  - La gestione Wi-Fi network binding è duplicata

### Piano di risoluzione

#### Azioni specifiche
1. **Rimuovere `buildServiceFor` da `AppRepository`**
   - Il metodo duplica `MikroTikServiceFactory.createService()`

2. **Iniettare `MikroTikServiceFactory` in `AppRepository`:**
   ```kotlin
   @Singleton
   class AppRepository @Inject constructor(
       @ApplicationContext private val context: Context,
       private val serviceFactory: MikroTikServiceFactory, // <-- INJECT
       // ...
   ) {
       // Rimuovere buildServiceFor(), usare direttamente serviceFactory
   }
   ```

3. **Aggiornare tutti i callsite che usano `buildServiceFor(probe)`:**
   - Sostituire con: `serviceFactory.createService(probe, wifiNetwork?.socketFactory)`
   - Trovare occorrenze:
     ```bash
     grep -n 'buildServiceFor' app/src/main/java/com/app/miklink/data/repository/AppRepository.kt
     ```

4. **Verificare che `MikroTikServiceFactory` supporti HTTPS:**
   - Controllare che il factory usi `probe.isHttps` per costruire `https://...` o `http://...`
   - Se manca, aggiungere la logica:
     ```kotlin
     val protocol = if (probe.isHttps) "https" else "http"
     val baseUrl = "$protocol://${probe.ipAddress}/rest/"
     ```

5. **Spostare la logica Wi-Fi network binding nel factory (opzionale ma raccomandato):**
   - Portare `findWifiNetwork()` dentro il factory o passarlo come parametro

#### File da modificare
- `app/src/main/java/com/app/miklink/data/repository/AppRepository.kt` (rimuovere `buildServiceFor`, iniettare factory, aggiornare callsite)
- `app/src/main/java/com/app/miklink/data/network/MikroTikServiceFactory.kt` (verificare/aggiungere supporto HTTPS se mancante)
- `app/src/main/java/com/app/miklink/di/NetworkModule.kt` (se necessario aggiornare DI binding per socketFactory)

#### Rischi
- **Medio:** Il factory potrebbe non supportare HTTPS → verificare e aggiungere se necessario
- **Basso:** Cambio signature richiede aggiornamento test mock

#### Validazione
- Build compila
- Test unitari `AppRepositoryTest` passano (aggiornare mock per iniettare factory)
- Test manuale: connessione a sonda HTTPS funziona (verificare con sonda configurata `isHttps=true`)
- Test manuale: connessione HTTP continua a funzionare (backward compatibility)

#### Stima tempo
- **2-4 ore** (refactor + aggiornamento test + smoke test manuale HTTPS)

---

## Issue #3: Migrazioni DB disattivate (HIGH)

### Problema
- **File:** `app/src/main/java/com/app/miklink/di/DatabaseModule.kt:38-40`
- **Evidenza:** Il database builder aggiunge le migrazioni ma subito dopo chiama `.fallbackToDestructiveMigration()`
- **Impatto:** 
  - Ogni aggiornamento app che cambia versione DB **droppa tutte le tabelle** e ricrea il DB da zero
  - Utenti perdono report storici, profili e sonde configurate
  - Le migrazioni esistenti (`MIGRATION_7_8`, `MIGRATION_12_13`, ecc.) non vengono mai eseguite in produzione

### Piano di risoluzione

#### Azioni specifiche
1. **Aggiornamento: sostituito `.fallbackToDestructiveMigration()`**
  - In `DatabaseModule.kt` abbiamo rimosso la chiamata generica a `.fallbackToDestructiveMigration()` e ora usiamo `.fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6)` per limitare i DB distruttivi solo alle versioni molto vecchie.
   ```kotlin
   return Room.databaseBuilder(context, AppDatabase::class.java, "miklink-db")
       .addMigrations(*Migrations.ALL_MIGRATIONS)
       // .fallbackToDestructiveMigration() // <-- RIMUOVERE
       .addCallback(...)
       .build()
   ```

2. **Verificare copertura migrazioni:**
   - Schema attuale in `AppDatabase`: verificare `@Database(version = X)`
   - Migrazioni presenti: `Migrations.ALL_MIGRATIONS` copre v7→v8, v8→v9, ..., v12→v13
   - Verificare che non manchino step intermedi (es. v10→v11 presente?)

3. **Aggiornare `MigrationTest` per coprire tutte le versioni:**
   - Test esistente: `app/src/androidTest/java/com/app/miklink/data/db/MigrationTest.kt`
   - Aggiungere test per migrazioni mancanti (se presenti gap) o confermare copertura completa
   - Esempio test v12→v13:
     ```kotlin
     @Test
     fun migrate_12_to_13() {
         val db = helper.createDatabase(TEST_DB, 12)
         // Insert test data v12
         helper.runMigrationsAndValidate(TEST_DB, 13, true, Migrations.MIGRATION_12_13)
         // Validate indexes created
     }
     ```

4. **Aggiungere autoMigration se ci sono gap semplici (opzionale):**
   - Per migrazioni triviali (solo aggiunta colonne nullable), usare Room autoMigration:
     ```kotlin
     @Database(..., autoMigrations = [AutoMigration(from = X, to = Y)])
     ```

- `app/src/main/java/com/app/miklink/di/DatabaseModule.kt` (aggiornamento fallback to destructive migration for very old versions)
- `app/src/androidTest/java/com/app/miklink/data/db/MigrationTest.kt` (aggiungere test per migrazioni mancanti)
- `app/src/main/java/com/app/miklink/data/db/AppDatabase.kt` (verificare version e eventuale autoMigration)

#### Rischi
- **Alto se deployment immediato:** Utenti con DB v6 o precedenti crasheranno se non c'è `MIGRATION_6_7`
  - **Mitigazione:** Verificare analytics/versione minima utenti; considerare keeping fallback temporaneamente per versioni molto vecchie:
    ```kotlin
    .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6) // solo versioni antiche
    ```
- **Medio:** Test di migrazione potrebbero fallire se ci sono gap → identificare e creare migrazioni mancanti

#### Validazione
- Build compila (verified)
- `MigrationTest` instrumented tests include 7->10 sequences, 11->12 coverage, and 12->13 index validation
- Test manuale: aggiornare app su device con DB v12 → verifica che i dati sopravvivono e schema è v13
- Analytics post-deploy: monitorare crash rate su aggiornamenti (primo rollout a beta/canary)

#### Stima tempo
- **3-6 ore** (analisi gap, aggiornamento test migrazioni, smoke test manuale upgrade)

---

## Issue #4: Backup non transazionale (HIGH)

### Problema
- **File:** `app/src/main/java/com/app/miklink/data/repository/BackupRepository.kt:29-37` (metodo `importConfigFromJson`)
- **Evidenza:** 
  - Il metodo deserializza JSON, **cancella tutte le tabelle**, poi inserisce i nuovi dati
  - Operazioni non sono atomiche (no transazione Room)
  - Nessuna validazione del payload JSON (schema, campi obbligatori, versioni)
- **Impatto:**
  - Un crash a metà import lascia il DB vuoto (dati cancellati ma non ripristinati)
  - JSON malformato o versioni incompatibili corrompono il DB
  - Nessun backup dei dati correnti prima della sovrascrittura (impossibile rollback)

### Piano di risoluzione

#### Azioni specifiche
1. **Validare payload JSON prima di toccare il DB:**
   ```kotlin
   suspend fun importConfigFromJson(json: String): Result<Unit> {
       val adapter = moshi.adapter(BackupData::class.java)
       val backupData = adapter.fromJson(json) ?: return Result.failure(Exception("JSON malformato"))
       
       // Validazione schema
       if (backupData.probes.any { it.ipAddress.isBlank() || it.username.isBlank() }) {
           return Result.failure(Exception("Dati sonda incompleti"))
       }
       if (backupData.profiles.any { it.profileName.isBlank() }) {
           return Result.failure(Exception("Dati profilo incompleti"))
       }
       
       // ... continua con import transazionale
   }
   ```

2. **Usare transazione Room per delete+insert atomico:**
   ```kotlin
   database.withTransaction {
       probeConfigDao.deleteAll()
       testProfileDao.deleteAll()
       probeConfigDao.insertAll(backupData.probes)
       testProfileDao.insertAll(backupData.profiles)
   }
   ```

3. **Creare backup automatico prima di import (opzionale ma raccomandato):**
   ```kotlin
   // Prima di cancellare, esporta stato corrente
   val currentBackup = exportConfigToJson()
   try {
       database.withTransaction { /* import */ }
   } catch (e: Exception) {
       // Opzionale: offri rollback automatico
       // importConfigFromJson(currentBackup)
       throw e
   }
   ```

4. **Aggiungere versioning al formato backup:**
   ```kotlin
   data class BackupData(
       val version: Int = CURRENT_VERSION, // es. 1
       val probes: List<ProbeConfig>,
       val profiles: List<TestProfile>
   )
   ```
   - Validare `backupData.version` prima di procedere; rifiutare versioni troppo vecchie o troppo nuove

#### File da modificare
- `app/src/main/java/com/app/miklink/data/repository/BackupRepository.kt` (aggiungere validazione, transazione, versioning)
 - `app/src/main/java/com/app/miklink/data/repository/BackupRepository.kt` (delegare a `BackupManager` - nuova classe)
 - `app/src/main/java/com/app/miklink/data/repository/BackupManager.kt` (nuova API per import/export con transazione e rollback)
 - `app/src/main/java/com/app/miklink/data/repository/TransactionRunner.kt` (wrapper per Room trasaction)
- `app/src/main/java/com/app/miklink/data/repository/BackupData.kt` (se esiste; aggiungere campo `version`)
- `app/src/test/java/com/app/miklink/data/repository/BackupRepositoryTest.kt` (nuovo test o aggiornare esistente)

#### Rischi
- **Basso:** Cambio signature da `suspend fun` a `suspend fun: Result<Unit>` richiede aggiornamento UI (gestione errori)
- **Medio:** Se backup corrente è grande, doppio export può richiedere memoria → limite dimensione o warning

#### Validazione
- Unit test: import JSON malformato → fallisce senza toccare DB
- Unit test: import valido → DB aggiornato atomicamente
- Unit test: crash simulato a metà transazione → DB intatto (rollback automatico Room)
- Test manuale: export → modify → import → verifica dati caricati correttamente
- Test manuale: export → import JSON corrotto → verifica DB non cancellato

#### Stima tempo
- **2-4 ore** (validazione + transazione + test unitari)

---

## Issue #5: Route default globali rimosse (MEDIUM)

### Problema
- **File:** `app/src/main/java/com/app/miklink/data/repository/AppRepository.kt:157` (circa)
- **Evidenza:** Il metodo `applyStaticConfigToProbe` rimuove **tutte** le route `0.0.0.0/0` prima di applicare la config statica
- **Impatto:**
  - Se il router MikroTik ha route default per altri servizi (VPN, failover, multi-WAN), vengono cancellate
  - Rompe connettività del router oltre al test di certificazione
  - Nessun rollback automatico in caso di errore durante la configurazione

### Piano di risoluzione

#### Azioni specifiche
1. **Limitare rimozione route solo a quelle aggiunte dall'app:**
   - **Opzione A (raccomandato):** Tag/comment le route aggiunte dall'app
     ```kotlin
     // Quando aggiungi route:
     api.addRoute(RouteAdd(dstAddress = "0.0.0.0/0", gateway = gw, comment = "MikLink_Auto"))
     
     // Quando rimuovi, filtra per comment:
     routes.filter { it.comment == "MikLink_Auto" }.forEach { r -> 
         r.id?.let { api.removeRoute(NumbersRequest(it)) } 
     }
     ```
   - **Opzione B:** Verifica l'interfaccia di uscita prima di cancellare
     ```kotlin
     routes.filter { 
         it.dstAddress == "0.0.0.0/0" && it.gateway == expectedGateway 
     }.forEach { ... }
     ```

2. **Implementare rollback automatico in caso di errore:**
   ```kotlin
   val originalRoutes = api.getRoutes().filter { it.dstAddress == "0.0.0.0/0" }
   try {
       // Rimuovi e applica nuova config
       routes.filter { it.comment == "MikLink_Auto" }.forEach { ... }
       api.addRoute(...)
   } catch (e: Exception) {
       // Rollback: ripristina route originali
       originalRoutes.forEach { route ->
           api.addRoute(RouteAdd(dstAddress = route.dstAddress, gateway = route.gateway!!))
       }
       throw e
   }
   ```

3. **Aggiungere dry-run mode (opzionale ma raccomandato):**
   - Permettere test configurazione senza applicare modifiche
   - Logging di cosa verrebbe fatto:
     ```kotlin
     fun applyStaticConfigToProbe(..., dryRun: Boolean = false) {
         // Log planned changes
         if (dryRun) {
             log("Would remove routes: $routesToRemove")
             log("Would add route: 0.0.0.0/0 -> $gateway")
             return
         }
         // ... apply changes
     }
     ```

4. **Aggiungere warning UI prima della rimozione route:**
   - UI deve mostrare: "La configurazione rimuoverà route default del router. Continuare?"
   - Dare opzione "Modalità sicura" che non rimuove route esistenti, solo aggiunge

#### File da modificare
- `app/src/main/java/com/app/miklink/data/repository/AppRepository.kt` (metodo `applyStaticConfigToProbe`, linee ~150-160 e ~240-250)
- `app/src/main/java/com/app/miklink/ui/test/TestViewModel.kt` (se necessario aggiungere conferma UI)
- `app/src/main/java/com/app/miklink/data/network/dto/MikroTikDto.kt` (aggiungere campo `comment` a `RouteEntry` e `RouteAdd` se mancante)

#### Rischi
- **Alto se deployment immediato senza fix:** Utenti continuano a rompere router in produzione
- **Medio:** MikroTik API potrebbe non supportare `comment` in tutte le versioni → testare su RouterOS minimo supportato
- **Basso:** Rollback potrebbe fallire se connessione si interrompe → catch doppio e logging

#### Validazione
- Unit test (mock): route con comment "MikLink_Auto" vengono rimosse, altre no
- Integration test (MockWebServer): simula risposta API con route multiple, verifica solo tag app rimossi
- Test manuale su MikroTik reale: 
  1. Aggiungi route default manuale
  2. Esegui configurazione statica MikLink
  3. Verifica route manuale ancora presente
- Test manuale rollback: interrompi connessione durante config → verifica route ripristinate

#### Stima tempo
- **4-6 ore** (implementazione tag/rollback + test integration + smoke test su device reale)

---

## Timeline e Sequenza di Esecuzione

### Fase 1: Data Integrity & Migrations (Giorno 1, mattina)
- **Issue #3** (Migrazioni DB): 3-6h → garantire integrità dati e rimuovere fallback distruttivo

### Fase 2: Backup & Critical fixes (Giorno 1, pomeriggio)
- **Issue #4** (Backup transazionale): 2-4h → aggiungere transazione e validazione

### Fase 3: Data integrity (Giorno 2)
- **Issue #3** (Migrazioni DB): 3-6h
- **Issue #4** (Backup transazionale): 2-4h
- Totale: 5-10h → può essere parallelizzato (2 sviluppatori)

### Fase 4: Network safety (Giorno 2-3)
- **Issue #5** (Route globali): 4-6h → richiede test manuale su hardware

**Timeline totale:**
- **Sequenziale:** ~14-22 ore → 2-3 giorni lavorativi
- **Parallelo (2-3 dev):** ~8-12 ore → 1-2 giorni lavorativi

---

## Checklist Pre-Deployment

Prima di rilasciare le fix in produzione:

- [ ] Tutte le build (debug/release) compilano senza warning critici
- [ ] Unit test suite: 100% pass
- [ ] Instrumented tests (androidTest): 100% pass su emulator
- [ ] Test manuale HTTPS: connessione a sonda HTTPS funziona
- [ ] Test manuale migrazione DB: upgrade da v12 a v13 preserva dati
- [ ] Test manuale backup/restore: export → import → dati OK
- [ ] Test manuale route: config statica non rimuove route non-MikLink
- [ ] PDF generato: testo leggibile, no caratteri corrotti
- [ ] Beta deployment: canary release a 5-10% utenti per 24-48h
- [ ] Monitoraggio crash rate: nessun picco su aggiornamenti

---

## Risorse e Responsabilità

### Assegnazioni Consigliate
- **Issue #1 (PDF):** Dev Junior/Mid → task meccanico, basso rischio
- **Issue #2 (Retrofit):** Dev Senior → refactor architetturale
- **Issue #3 (Migrazioni):** Dev Senior + QA → test critici
- **Issue #4 (Backup):** Dev Mid → aggiunta validazione e test
- **Issue #5 (Route):** Dev Senior + Network specialist → richiede conoscenza RouterOS

### Code Review
- Ogni fix richiede:
  - [ ] Review codice (almeno 1 senior)
  - [ ] Review test coverage (QA o senior)
  - [ ] Smoke test manuale (tester o PM)

### Rollback Plan
- Tenere branch `hotfix/revert-issues-1-5` pronta
- Ogni fix deve avere feature flag (se possibile) per disabilitazione rapida
- Monitorare analytics prime 48h post-deploy

---

## Note Finali

Questo piano copre la risoluzione completa dei 5 problemi critici identificati. La prioritizzazione riflette:
1. **Sbloccare build** (Issue #1)
2. **Ripristinare funzionalità core** (Issue #2)
3. **Proteggere dati utente** (Issue #3, #4)
4. **Evitare danni a dispositivi di rete** (Issue #5)

Per domande o chiarimenti sul piano, contattare il Tech Lead o aprire una discussione nella PR corrispondente.
