# MIKLINK - Documentazione Tecnica del Progetto
**Stato del Progetto al: 2025-01-15**
**Versione App: 1.2**

---

## 🚀 SOMMARIO MODIFICHE (2025-01-15)

**Tipo intervento**: Code Review Completa + Allineamento UI/UX  
**Scope**: Correzione bug critici, rimozione feature non implementate, miglioramento UX  
**Files modificati**: 15  
**Build status**: ✅ SUCCESS (dopo correzioni)  

**Modifiche critiche implementate**:
1. ✅ Fix card duplicate in TestExecutionScreen (UI rotta → UI pulita)
2. ✅ Fix sequenza DHCP (perdita connettività → stabile)
3. ✅ Rimozione floor/room da tutta la codebase (9 file)
4. ✅ Rimozione VLAN da tutta la codebase (4 file)
5. ✅ Fix rate null/unknown con log diagnostici
6. ✅ UI toggle log/sections durante esecuzione e completamento
7. ✅ Polling lifecycle-aware (già implementato correttamente)

**Database Schema**: Aggiornamento a v7 (richiede migrazione o reset)

---

## Aggiornamenti recenti (feature)

### 2025-01-15 - Code Review Completa e Allineamento UI/UX

**Sintesi delle Modifiche:**
Eseguita una code review completa dell'app in seguito a problemi rilevati nei flussi UI/UX. Implementate le seguenti correzioni critiche:

#### 1. **FIX CRITICO: Card Duplicate in TestExecutionScreen**
- **Problema**: La schermata di test mostrava card duplicate durante l'esecuzione e al completamento (sezioni renderizzate sia da TestCompletedView che da LazyColumn esterna)
- **Soluzione**: Rimosso completamente il rendering duplicato. Ora:
  - `TestInProgressView`: gestisce autonomamente il rendering durante l'esecuzione (con toggle log/sections)
  - `TestCompletedView`: gestisce autonomamente il rendering al completamento (con toggle log/sections)
  - Rimossa LazyColumn esterna che causava duplicazione
- **Impatto**: UI pulita, nessuna duplicazione, esperienza utente migliorata

#### 2. **FIX DHCP: Sequenza Comandi Corretta**
- **Problema**: La configurazione DHCP rimuoveva IP/route prima di abilitare il client, causando perdita di connettività
- **Soluzione**: Implementata sequenza corretta:
  1. `disable` → `enable` (per DHCP esistente)
  2. `addDhcpClient` (per nuovo DHCP)
  3. Attendi bound (max 6 secondi)
  4. NON rimuovere IP/route (MikroTik gestisce automaticamente)
- **Impatto**: Configurazione rete stabile, nessuna disconnessione temporanea

#### 3. **RIMOZIONE COMPLETA: floor/room da tutta la codebase**
- **File modificati**:
  - `Client.kt`: rimossi `lastFloor`, `lastRoom`
  - `Report.kt`: rimossi `floor`, `room`
  - `ClientDao.kt`: rimossa `updateNextIdAndStickyFields`, aggiunta `incrementNextIdNumber`
  - `TestViewModel.kt`: aggiornata creazione Report e save
  - `ClientEditScreen.kt` e `ClientEditViewModel.kt`: rimossa sezione Sticky Fields
  - `ClientListScreen.kt`: rimosso display floor/room
  - `HistoryScreen.kt`: rimosso display floor/room
  - `PdfGenerator.kt`: rimossa sezione Location (due occorrenze)
- **Impatto**: Codebase semplificata, nessun campo inutilizzato

#### 4. **RIMOZIONE COMPLETA: VLAN da tutta la codebase**
- **File modificati**:
  - `Client.kt`: rimosso `vlanId`
  - `ClientEditScreen.kt` e `ClientEditViewModel.kt`: rimosso campo VLAN ID
  - `PdfGenerator.kt`: rimossa visualizzazione VLAN nei neighbor details
  - `NeighborDetail.kt`: mantenuto `vlanId` nel DTO (arriva da MikroTik) ma non visualizzato
- **Impatto**: Feature non implementata rimossa per evitare confusione utente

#### 5. **FIX Rate null/unknown → FAIL esplicito**
- **Problema**: Se `MonitorResponse.rate` era null o formato sconosciuto, `parseToMbps()` ritornava 0 e il test falliva senza messaggio chiaro
- **Soluzione**: Aggiunto log esplicito in `isRateOk()`:
  - `rate == null` → "ATTENZIONE: Velocità non disponibile → FAIL"
  - `rate` formato sconosciuto → "ATTENZIONE: Formato velocità non riconosciuto ('$rate') → FAIL"
- **Impatto**: Diagnostica migliorata, utente capisce il motivo del FAIL

#### 6. **POLLING LIFECYCLE-AWARE: già implementato correttamente**
- **Verifica**: `DashboardViewModel` e `ProbeListViewModel` usano già `SharingStarted.WhileSubscribed(5000)`
- **Comportamento**: Il polling si ferma automaticamente 5 secondi dopo che la UI non osserva più il flow
- **Impatto**: Consumo batteria ottimizzato, nessuna modifica necessaria

#### 7. **UI TOGGLE: Log Grezzi vs Sections durante esecuzione e completamento**
- **Implementazione**:
  - `TestInProgressView`: toggle "Mostra log grezzi" / "Nascondi log grezzi"
    - Default: mostra sections (Network, LLDP, Link, Ping, Traceroute) in tempo reale
    - Toggle ON: mostra log grezzo monospace
  - `TestCompletedView`: stesso toggle
    - Default: mostra sections aggregate
    - Toggle ON: mostra log completo
    - Fallback: se `sections.isEmpty()` mostra automaticamente log
- **Impatto**: Flessibilità massima per utente tecnico e non-tecnico

---

### Database Schema Changes (v6 → v7)
**ATTENZIONE**: Le modifiche a `Client` e `Report` richiedono una migrazione DB o reset dati.

**Client Table**:
- ❌ Rimosso: `lastFloor: String?`
- ❌ Rimosso: `lastRoom: String?`
- ❌ Rimosso: `vlanId: Int?`

**Report Table**:
- ❌ Rimosso: `floor: String?`
- ❌ Rimosso: `room: String?`

**ClientDao**:
- ❌ Rimosso: `updateNextIdAndStickyFields(id, floor, room)`
- ✅ Aggiunto: `incrementNextIdNumber(id)`

**Strategia attuale**: `fallbackToDestructiveMigration()` (reset DB al cambio schema)  
**TODO futuro**: Implementare migrazione non distruttiva con `Migration(6, 7)`

---

### Testing Checklist (post-implementazione)

**Test Manuali da Eseguire**:
1. ✅ **Dashboard → Test Execution**:
   - Selezionare Client/Probe/Profile
   - Premere "AVVIA TEST"
   - Verificare autostart del test
   - Verificare nessuna duplicazione card
   
2. ✅ **Durante esecuzione test**:
   - Verificare comparsa card in tempo reale (Network, Link, LLDP, Ping)
   - Toggle "Mostra log grezzi" → deve mostrare log monospace
   - Toggle "Nascondi log grezzi" → deve tornare alle card
   
3. ✅ **Al completamento test**:
   - Verificare header PASS/FAIL colorato
   - Verificare card aggregate (ordine: Network, LLDP, Link, Ping, Traceroute, TDR)
   - Toggle log funzionante
   - Bottoni CHIUDI/RIPETI/SALVA funzionanti
   
4. ✅ **Configurazione DHCP**:
   - Modificare Client con `networkMode = DHCP`
   - Avviare test
   - Verificare log: "DHCP lease acquisita" o "DHCP non bound (verificare server DHCP)"
   - Verificare nessuna disconnessione/riconnessione
   
5. ✅ **Rate Link con soglie diverse**:
   - Client con `minLinkRate = "10M"` → test con rate `"100Mbps"` → PASS
   - Client con `minLinkRate = "1G"` → test con rate `"100Mbps"` → FAIL (con log "Velocità link inferiore alla soglia")
   - Sonda ritorna rate `null` → FAIL con log "ATTENZIONE: Velocità non disponibile → FAIL"
   
6. ✅ **Client Edit Screen**:
   - Verificare assenza campi: Last Floor, Last Room, VLAN ID
   - Verificare campo `Min Link Rate` con opzioni: 10M, 100M, 1G, 10G
   
7. ✅ **History Screen**:
   - Verificare display report senza floor/room
   - Verificare export PDF senza sezione Location
   
8. ✅ **Polling sonde**:
   - Dashboard: verificare badge online/offline si aggiorna
   - Uscire da Dashboard e tornare dopo 10 secondi → badge ancora aggiornato
   - Lasciare app in background 1 minuto → tornare → polling riprende

---

### File Modificati (Riepilogo)

### 2025-01-14 - Fix Regressione Schermata Test (Code Review & Ripristino Funzionalità)
**Root Cause**: Dopo il refactoring UI per le card di test, la pipeline di esecuzione test in `TestViewModel` era incompleta:
- **LLDP e Ping non eseguiti**: Blocchi di codice mancanti, sezioni UI vuote.
- **Gating su sonda online**: Il bottone "AVVIA TEST" richiedeva `isProbeOnline=true`, bloccando l'avvio anche in scenari di rete temporaneamente irraggiungibile (il test deve fallire a runtime, non essere impedito preventivamente).
- **Autostart ambiguo**: `LaunchedEffect(Unit)` nella UI avviava automaticamente il test all'ingresso, ma il pulsante "AVVIA TEST" restava visibile → rischio di doppio start o confusione utente.
- **Parametri navigazione**: `socketName` arrivava URI-encoded senza decodifica; nessuna validazione iniziale su `clientId/probeId/profileId` invalidi.
- **Duplicazione DTO**: `ProbeCheckResult` definito sia in `data/network/dto/MikroTikDto.kt` che in `data/repository/AppRepository.kt`.
- **Interceptor duplicato**: `loggingInterceptor` aggiunto due volte nel client per-sonda (già presente in `baseOkHttpClient`).

**Correzioni Applicate**:
1. **TestViewModel.startTest()**: Aggiunti blocchi completi per **LLDP** (`repository.getNeighborsForInterface`) e **Ping** (`repository.runPing`) con:
   - Gestione multi-target ping (pingTarget1/2/3).
   - `upsertSection()` in tempo reale per aggiornamento UI card.
   - Scrittura coerente in `testResults` per salvataggio Report.
   - Status PASS/FAIL/SKIPPED (es. target DHCP_GATEWAY non risolto → SKIPPED, non FAIL).
   - Traceroute: gestione fallback per target non risolvibile (SKIPPED con messaggio chiaro).
2. **Normalizzazione parametri navigazione**:
   - `socketName`: decodifica URI con `android.net.Uri.decode()` + fallback a raw in caso di eccezione.
   - Validazione parametri: se `clientId/probeId/profileId <= 0` → uscita immediata con `UiState.Error` descrittivo ("Parametri di navigazione non validi").
   - Messaggio di errore DB migliorato: "Impossibile caricare i dati di test. Cliente, sonda o profilo non esistono."
3. **Dashboard - Sblocco avvio test**:
   - Rimosso gating hard su `isProbeOnline` da `isTestButtonEnabled`.
   - Aggiunto chip di **warning visivo** (Card rosso con icona Warning) sopra il bottone quando `selectedProbe != null && !isProbeOnline`: "Sonda offline: il test potrebbe fallire".
   - Il test può ora partire anche se sonda offline; fallirà a runtime con errore di rete chiaro (es. "Connection refused").
4. **TestExecutionScreen - Avvio manuale**:
   - **Rimosso autostart**: cancellato `LaunchedEffect(Unit) { if (uiState is Idle) viewModel.startTest() }`.
   - Utente deve premere esplicitamente "AVVIA TEST" → UX prevedibile, nessun doppio start.
5. **Deduplica `ProbeCheckResult`**:
   - Rimossa definizione in `data/network/dto/MikroTikDto.kt`.
   - Mantenuta unica sorgente in `data/repository/AppRepository.kt`.
   - Aggiunto commento nel DTO file per evitare futuri duplicati.
6. **AppRepository.buildServiceFor()**:
   - Rimossa seconda aggiunta di `loggingInterceptor` (già in `baseOkHttpClient`).
   - Evita log HTTP duplicati in Logcat.
7. **Fix warning Kotlin**: Rimosso `toString()` ridondante in `buildSectionsFromResults` (Map.get ritorna già String?).

**Impatti**:
- UI schermata test ora completa: tutte le card (Network, Link, LLDP, Ping, Traceroute, TDR) popolate correttamente.
- Report salvati contengono `resultsJson` completo con LLDP e Ping.
- UX sbloccata: possibile avviare test anche in scenari edge (sonda offline, rete temporanea, gateway DHCP non disponibile).
- Nessuna migrazione DB richiesta; schema invariato (v7).
- Log più puliti (nessun duplicato HTTP).

**Testing Post-Fix**:
- Verificare test completo con profilo "Full Test" (TDR + Link + LLDP + Ping).
- Verificare comportamento con sonda offline (warning visivo, test fallisce a runtime con messaggio chiaro).
- Verificare ping multi-target e traceroute con/senza gateway DHCP.
- Verificare socketName con caratteri speciali (es. "PRT-001/A") → decodifica corretta.

**Next Steps**:
- [ ] Implementare "Cancel test" (coroutine job handle + UI button).
- [ ] Estendere policy Ping: PASS parziale se almeno 1 target su N risponde.
- [ ] Migrazioni Room non distruttive (rimuovere `fallbackToDestructiveMigration`).
- [ ] Test unitari per `TestViewModel.startTest()` (mock Repository).

---

### 2025-11-14 - UI Test per-card + Min Link Rate fisso + Icona FAIL
- Min Link Rate PASS: ora selezione da lista fissa (10M / 100M / 1G / 10G) nel ClientEditScreen.
- TestExecutionScreen: riepilogo ristrutturato in card per singolo test (Network, Link, LLDP/CDP, Ping, Traceroute, TDR) con sezione "Dettagli" espandibile; rimosse card di step test e log come elenco principale (log disponibile su toggle "Mostra Log Grezzi").
- TopAppBar: icona di fallimento uniformata all'icona "Cancel" usata nel logo di test fallito.

### 2025-11-14 - UI Test Unificata, Sezioni Info/Test, Chip di Stato
- Unificata la schermata di test: un unico layout per esecuzione e risultato.
- Header risultato sempre visibile con logo centrale e anello esterno animato (verde/rosso; neutro durante esecuzione).
- Divisione grafica in due blocchi:
  - Informazioni: Config rete (DHCP/Static), LLDP/CDP, altre info non-bloccanti.
  - Test: Link (con soglia Min Link Rate), Ping (target multipli), Traceroute, TDR.
- Card dinamiche: compaiono e si popolano man mano che ogni step termina; ogni card mostra chip di stato (PASS/FAIL/INFO/PARTIAL/SKIPPED) e dettagli espandibili.
- Traceroute: card dedicata con elenco hop (host + RTT) e stato PASS/PARTIAL/FAIL.
- Back icons aggiornate a Icons.AutoMirrored.Filled.ArrowBack in tutte le schermate.

Note: `TestViewModel` emette incrementi di `sections` per permettere l’aggiornamento live delle card; a fine test consolidiamo anche da `resultsJson` per coerenza dei dati salvati nel Report.

---

## UX (aggiornato)
- ClientEditScreen: Min Link Rate PASS come lista fissa con chip selezionabili.
- TestExecutionScreen: struttura per-card dei test con espansione; header con icone coerenti (CheckCircle per PASS, Cancel per FAIL).

---

## Note
- VLAN: per ora non si crea interfaccia VLAN dedicata; il supporto può essere esteso in futuro creando `<iface>.<vlan>` e utilizzandolo come `numbers`/interface.
- Migrazioni: schema v6 con `fallbackToDestructiveMigration()`. Migliorabile con migrazioni non distruttive.

---

## Aggiornamenti recenti (manutenzione)

### 2025-11-14 - Refactoring UI/UX
- **Rimozione FAB duplicati**: Eliminati i FloatingActionButton dalle schermate List (ClientListScreen, ProbeListScreen, TestProfileListScreen); mantenuti solo i Button centrali negli empty state per migliore UX
- **Navigazione Back**: Aggiunto `navigationIcon` con `Icons.Default.ArrowBack` nelle TopAppBar di tutte le schermate secondarie (Edit: Client/Probe/Profile, Detail: History/Report, Settings, TestExecution)
- **Dashboard SelectionCard**: Sostituito `ExposedDropdownMenu` con RadioButton List espandibile per migliore integrazione visiva; implementate animazioni `expandVertically()`/`shrinkVertically()` + `fadeIn()`/`fadeOut()`; selezione evidenziata con `primaryContainer` background e bordo `primary`

### 2025-11-14 - Log Grezzi Live
- Aggiunto toggle "Mostra log grezzi" nella schermata test unificata: quando attivo, le card vengono nascoste e appare un pannello a piena altezza in stile terminale (monospace, autoscroll, colori base per PASS/FAIL/INFO). Il log è live e segue l’esecuzione della sonda.

### Precedenti
- Networking reso stateless: rimosso stato globale `apiService` e `setProbe`; ogni operazione costruisce un service per-sonda con credenziali e binding rete dedicati.
- TLS: OkHttp ora usa `TLS` (non più `SSL`) con trust-all per HTTPS delle sonde (anche in produzione), con logging abilitato.
- Moshi: rimosso `moshi-kotlin-codegen`; gli adapter riflessivi restano attivi (Boolean/Int lenient). `BackupRepository` usa ora `moshi.adapter(BackupData::class.java)`.
- Polling: `observeAllProbesWithStatus()` ora usa `flatMapLatest + ticker` ed è privo di `while(true)` annidati; `observeProbeStatus()` è indipendente.
- Deduplica: unificato `SettingsScreen` (solo `ui/settings/SettingsScreen.kt`) e `Compatibility` (solo `utils/Compatibility.kt`).
- Java/Kotlin: confermato target Java 21 e Kotlin JVM target 21.

---

## Stack Tecnologico (estratto)
- Kotlin 2.0.0, Java 21
- Jetpack Compose BOM 2024.06.00
- Hilt 2.51.1 (KSP attivo per Hilt/Room)
- Room 2.6.1
- Retrofit 2.9.0, OkHttp 4.12.0, Moshi 1.15.1 (senza codegen)

---

## Network Layer (aggiornato)

### Costruzione per-sonda
Ogni chiamata crea un `OkHttpClient` con:
- Interceptor Basic Auth (credenziali della sonda)
- LoggingInterceptor livello BODY
- `socketFactory` della rete Wi‑Fi se disponibile

Retrofit.Builder condiviso, `baseUrl` per-sonda: `http(s)://{ip}/`.

### Sicurezza
- `SSLContext`: `TLS` con trust-all per certificati self-signed dei MikroTik. Attivo anche in produzione, limitato alle sonde.

### Firme metodi (aggiornate)
```kotlin
suspend fun checkProbeConnection(probe: ProbeConfig): ProbeCheckResult
fun observeProbeStatus(probe: ProbeConfig): Flow<Boolean>
fun observeAllProbesWithStatus(): Flow<List<ProbeStatusInfo>>

suspend fun runCableTest(probe: ProbeConfig, interfaceName: String): UiState<CableTestResult>
suspend fun getLinkStatus(probe: ProbeConfig, interfaceName: String): UiState<MonitorResponse>
suspend fun getNeighborsForInterface(probe: ProbeConfig, interfaceName: String): UiState<List<NeighborDetail>>
suspend fun runPing(probe: ProbeConfig, target: String, interfaceName: String): UiState<PingResult>
```

`resolveTargetIp` e `getDhcpGateway` richiedono ora `probe` come parametro.

---

## UI/Repository (note)
- `TestViewModel` aggiornato per usare le nuove firme stateless (nessuna `setProbe`).
- `ProbeListViewModel`/`DashboardViewModel` invariati a livello di API pubblico; polling più robusto.

---

## Build e Configurazione
- `app/build.gradle.kts`: rimosso `ksp(moshi-kotlin-codegen)`; Java 21 confermato.
- `build.gradle.kts` (root): plugin allineati via Version Catalog; rimosse definizioni duplicate.

---

## Note e Limitazioni
- Trust-all TLS: necessario per sondaggi HTTPS con certificati self-signed. Non usare per traffico Internet generico.
- Migrazioni Room: ancora `fallbackToDestructiveMigration()`.

---

# MIKLINK - Documentazione Tecnica del Progetto
**Stato del Progetto al: 2025-01-14**
**Versione App: 1.0**

---

## 📋 INDICE

1. [Panoramica Generale](#panoramica-generale)
2. [Stack Tecnologico](#stack-tecnologico)
3. [Architettura dell'Applicazione](#architettura-dellapplicazione)
4. [Database Layer (Room)](#database-layer-room)
5. [Network Layer (Retrofit + MikroTik API)](#network-layer-retrofit--mikrotik-api)
6. [Repository Layer](#repository-layer)
7. [UI Layer - Schermate e Navigazione](#ui-layer---schermate-e-navigazione)
8. [Funzionalità Principali](#funzionalità-principali)
9. [Flusso di Test Completo](#flusso-di-test-completo)
10. [Export e Backup](#export-e-backup)
11. [Configurazione e Build](#configurazione-e-build)
12. [Note Tecniche e Limitazioni](#note-tecniche-e-limitazioni)

---

## 📱 PANORAMICA GENERALE

**MikLink** è un'applicazione Android nativa per l'esecuzione di test di certificazione di rete su infrastrutture cablate utilizzando dispositivi **MikroTik** come sonde di test.

### Scopo dell'Applicazione
L'app permette a tecnici e installatori di rete di:
- Certificare prese di rete (socket) installate presso clienti
- Eseguire test diagnostici automatizzati (TDR, Link Status, LLDP/CDP, Ping)
- Generare report PDF professionali per ogni test
- Mantenere uno storico organizzato per cliente
- Gestire configurazioni di sonde, clienti e profili di test

### Dominio di Business
- **Cliente**: Azienda/sede dove viene eseguita la certificazione
- **Sonda (Probe)**: Dispositivo MikroTik RouterOS usato per i test
- **Profilo di Test**: Template che definisce quali test eseguire
- **Socket**: Presa di rete fisica da certificare
- **Report**: Risultato di un test di certificazione

---

## 🛠️ STACK TECNOLOGICO

### Framework e Linguaggi
- **Linguaggio**: Kotlin 2.0.0
- **UI Framework**: Jetpack Compose (BOM 2024.06.00)
- **Minimum SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34
- **Java Target**: 21

### Librerie Principali

#### UI e Navigazione
- `androidx.compose.material3:material3` (1.2.1)
- `androidx.navigation:navigation-compose` (2.7.7)
- `androidx.compose.material:icons-extended` (per icone estese)
- `androidx.activity:activity-compose` (1.9.0)
- `androidx.lifecycle:lifecycle-runtime-compose` (2.8.3)

#### Dependency Injection
- `com.google.dagger:hilt-android` (2.51.1)
- `androidx.hilt:hilt-navigation-compose` (1.2.0)
- **KSP** (2.0.0-1.0.21) per code generation

#### Database
- `androidx.room:room-runtime` (2.6.1)
- `androidx.room:room-ktx` (supporto Coroutines)

#### Networking
- `com.squareup.retrofit2:retrofit` (2.9.0)
- `com.squareup.retrofit2:converter-moshi` (2.9.0)
- `com.squareup.okhttp3:okhttp` (4.12.0)
- `com.squareup.okhttp3:logging-interceptor` (4.12.0)
- `com.squareup.moshi:moshi` (1.15.1) + `moshi-kotlin`

#### PDF Generation
- `android.graphics.pdf.PdfDocument` (Android SDK nativo)

### Permissions
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
```

### Configurazioni Speciali
- **Cleartext Traffic**: Abilitato (per comunicazione HTTP con MikroTik)
- **Screen Always On**: Abilitato durante l'uso dell'app
- **Edge-to-Edge**: UI moderna con supporto insets

---

## 🏗️ ARCHITETTURA DELL'APPLICAZIONE

### Pattern Architetturale: MVVM (Model-View-ViewModel)

```
┌─────────────────────────────────────────────────────────┐
│                    UI LAYER (Compose)                   │
│  - Screens (Composables)                                │
│  - ViewModels (State Management)                        │
└───────────────────┬─────────────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────────────┐
│                  REPOSITORY LAYER                       │
│  - AppRepository (Network + Business Logic)             │
│  - BackupRepository (Import/Export)                     │
└───────────────┬───────────────────┬─────────────────────┘
                │                   │
     ┌──────────▼────────┐   ┌──────▼──────────┐
     │   DATA SOURCES    │   │  DATA SOURCES   │
     │   Room Database   │   │  Retrofit API   │
     │   (Local)         │   │  (Remote)       │
     └───────────────────┘   └─────────────────┘
```

### Dependency Injection (Hilt)
- **Modules**:
    - `DatabaseModule`: Provvede `AppDatabase` e DAO
    - `NetworkModule`: Provvede `OkHttpClient`, `Retrofit`, `Moshi`
- **Scopes**: Singleton per repository e database
- **Application Class**: `MikLinkApplication` annotata con `@HiltAndroidApp`

---

## 💾 DATABASE LAYER (ROOM)

### Database: `AppDatabase`
- **Versione**: 6
- **Strategia Migrazione**: `fallbackToDestructiveMigration()`
- **Callback onCreate**: Inserisce 2 profili di test predefiniti

### Entities (Tabelle)

#### 1. **Client** (`clients`)
Rappresenta un cliente/sede dove vengono eseguiti i test.

| Campo | Tipo | Descrizione |
|-------|------|-------------|
| `clientId` | Long (PK) | ID auto-generato |
| `companyName` | String | Nome azienda (richiesto) |
| `location` | String? | Sede/località (default: "Sede") |
| `notes` | String? | Note libere |
| `networkMode` | String | "DHCP" o "STATIC" |
| `vlanId` | Int? | ID VLAN (se applicabile) |
| `staticIp` | String? | IP statico per configurazione manuale |
| `staticSubnet` | String? | Subnet mask |
| `staticGateway` | String? | Gateway |
| `socketPrefix` | String | Prefisso per ID socket (es. "PRT-") |
| `nextIdNumber` | Int | Contatore progressivo socket |
| `lastFloor` | String? | Ultimo piano usato (sticky field) |
| `lastRoom` | String? | Ultima stanza usata (sticky field) |

**Business Logic**:
- `socketPrefix + nextIdNumber` genera ID univoci (es. "PRT-001")
- `lastFloor` e `lastRoom` vengono salvati dopo ogni test per velocizzare l'input

#### 2. **ProbeConfig** (`probe_config`)
Configurazione di una sonda MikroTik.

| Campo | Tipo | Descrizione |
|-------|------|-------------|
| `probeId` | Long (PK) | ID auto-generato |
| `name` | String | Nome identificativo sonda |
| `ipAddress` | String | Indirizzo IP della sonda |
| `username` | String | Username API MikroTik |
| `password` | String | Password API |
| `testInterface` | String | Interfaccia da usare per i test (es. "ether2") |
| `isOnline` | Boolean | Stato connessione (read-only, calcolato) |
| `modelName` | String? | Modello rilevato (es. "RB750Gr3") |
| `tdrSupported` | Boolean | Se la sonda supporta TDR (cable-test) |
| `isHttps` | Boolean | Usa HTTPS invece di HTTP |

**Note Tecniche**:
- `isOnline` è calcolato dinamicamente tramite polling ogni 10-15 secondi
- Il supporto TDR dipende dal modello di MikroTik (non tutti supportano cable-test)

#### 3. **TestProfile** (`test_profiles`)
Template di test da eseguire.

| Campo | Tipo | Descrizione |
|-------|------|-------------|
| `profileId` | Long (PK) | ID auto-generato |
| `profileName` | String | Nome profilo (es. "Full Test") |
| `profileDescription` | String? | Descrizione |
| `runTdr` | Boolean | Esegui TDR (Cable Test) |
| `runLinkStatus` | Boolean | Esegui test stato link |
| `runLldp` | Boolean | Esegui discovery LLDP/CDP |
| `runPing` | Boolean | Esegui ping test |
| `pingTarget1` | String? | Target ping 1 (o "DHCP_GATEWAY") |
| `pingTarget2` | String? | Target ping 2 |
| `pingTarget3` | String? | Target ping 3 |

**Profili Predefiniti** (inseriti al primo avvio):
1. **"Full Test"**: TDR + Link + LLDP + Ping (DHCP_GATEWAY + 8.8.8.8)
2. **"Quick Test"**: Solo Link + Ping (DHCP_GATEWAY)

#### 4. **Report** (`test_reports`)
Risultato di un test di certificazione.

| Campo | Tipo | Descrizione |
|-------|------|-------------|
| `reportId` | Long (PK) | ID auto-generato |
| `clientId` | Long? | FK a Client |
| `timestamp` | Long | Timestamp Unix (ms) |
| `socketName` | String? | ID socket (es. "PRT-001") |
| `floor` | String? | Piano |
| `room` | String? | Stanza |
| `notes` | String? | Note aggiuntive |
| `probeName` | String? | Nome sonda usata |
| `profileName` | String? | Nome profilo usato |
| `overallStatus` | String | "PASS" o "FAIL" |
| `resultsJson` | String | Risultati serializzati in JSON |

**Struttura `resultsJson`**:
```json
{
  "tdr": { "cablePairs": [...], "status": "ok" },
  "link": { "status": "link-ok", "rate": "1Gbps" },
  "lldp": { "identity": "Switch-01", "interfaceName": "ether5", ... },
  "ping_192.168.1.1": { "avgRtt": "1.5ms" },
  "ping_8.8.8.8": { "avgRtt": "12.3ms" }
}
```

### DAO (Data Access Objects)

Tutti i DAO seguono lo stesso pattern:
- **Queries**: Ritornano `Flow<T>` per osservabilità reattiva
- **Insert/Update/Delete**: Operazioni suspend su `Dispatchers.IO`

#### ClientDao
```kotlin
fun getAllClients(): Flow<List<Client>>
fun getClientById(id: Long): Flow<Client?>
suspend fun insert(client: Client)
suspend fun update(client: Client)
suspend fun delete(client: Client)
suspend fun updateNextIdAndStickyFields(id: Long, floor: String?, room: String?)
```

#### ProbeConfigDao
```kotlin
fun getAllProbes(): Flow<List<ProbeConfig>>
fun getProbeById(id: Long): Flow<ProbeConfig?>
suspend fun insert(probe: ProbeConfig)
suspend fun insertAll(probes: List<ProbeConfig>)
suspend fun deleteAll() // Per import/export
```

#### TestProfileDao
```kotlin
fun getAllProfiles(): Flow<List<TestProfile>>
fun getProfileById(id: Long): Flow<TestProfile?>
suspend fun insert(profile: TestProfile)
suspend fun insertAll(profiles: List<TestProfile>)
suspend fun deleteAll()
```

#### ReportDao
```kotlin
fun getAllReports(): Flow<List<Report>>
fun getReportById(id: Long): Flow<Report?>
fun getReportsForClient(clientId: Long): Flow<List<Report>>
suspend fun getLastReportForClient(clientId: Long): Report?
suspend fun insert(report: Report)
suspend fun delete(report: Report)
```

---

## 🌐 NETWORK LAYER (RETROFIT + MIKROTIK API)

### MikroTik REST API
MikLink comunica con RouterOS tramite **REST API** (introdotta in RouterOS v7.x).

**Base URL dinamica**: `http(s)://{ipAddress}/`

### DTOs (Data Transfer Objects)

#### Request DTOs
```kotlin
// Richiesta generica con lista di proprietà
data class ProplistRequest(@Json(name = ".proplist") val proplist: List<String>)

// Filtro per interfaccia
data class InterfaceNameRequest(@Json(name = "?.interface") val interfaceName: String)

// Test specifici
data class CableTestRequest(@Json(name = "numbers") val numbers: String)
data class MonitorRequest(@Json(name = "numbers") val numbers: String, 
                          @Json(name = "once") val once: Boolean = true)
data class PingRequest(val address: String, val count: String = "4")

// LLDP/CDP con query e proplist
data class NeighborRequest(@Json(name = "?.query") val query: List<String>, 
                           @Json(name = ".proplist") val proplist: List<String>)
```

#### Response DTOs
```kotlin
data class SystemResource(@Json(name = "board-name") val boardName: String)
data class EthernetInterface(val name: String)
data class DhcpClientStatus(val gateway: String?)

data class CableTestResult(
    @Json(name = "cable-pairs") val cablePairs: List<Map<String, String>>, 
    val status: String
)

data class MonitorResponse(val status: String, val rate: String?)

data class NeighborDetail(
    val identity: String?,
    @Json(name = "interface-name") val interfaceName: String?,
    @Json(name = "system-caps-enabled") val systemCaps: String?,
    @Json(name = "discovered-by") val discoveredBy: String?,
    @Json(name = "vlan-id") val vlanId: String? = null,
    @Json(name = "voice-vlan-id") val voiceVlanId: String? = null,
    @Json(name = "poe-class") val poeClass: String? = null
)

data class PingResult(@Json(name = "avg-rtt") val avgRtt: String?)
```

### API Service Interface

```kotlin
interface MikroTikApiService {
    @POST("/rest/system/resource/print")
    suspend fun getSystemResource(@Body request: ProplistRequest): List<SystemResource>

    @POST("/rest/interface/ethernet/print")
    suspend fun getEthernetInterfaces(@Body request: ProplistRequest): List<EthernetInterface>

    @POST("/rest/ip/dhcp-client/print")
    suspend fun getDhcpClientStatus(@Body request: InterfaceNameRequest): List<DhcpClientStatus>

    @POST("/rest/interface/ethernet/cable-test")
    suspend fun runCableTest(@Body request: CableTestRequest): List<CableTestResult>

    @POST("/rest/interface/ethernet/monitor")
    suspend fun getLinkStatus(@Body request: MonitorRequest): List<MonitorResponse>

    @POST("/rest/ip/neighbor/print")
    suspend fun getIpNeighbors(@Body request: NeighborRequest): List<NeighborDetail>

    @POST("/rest/ping")
    suspend fun runPing(@Body request: PingRequest): List<PingResult>
}
```

### Network Configuration

#### OkHttp Client
- **Auth**: Basic Authentication tramite `AuthInterceptor` (credenziali dinamiche)
- **Logging**: HttpLoggingInterceptor a livello BODY (per debug)
- **SSL**: Trust manager personalizzato che **accetta tutti i certificati** (necessario per HTTPS self-signed)
- **Socket Factory**: Binding dinamico alla rete WiFi (se disponibile) per forzare il routing

#### Moshi JSON Adapter
Custom adapters per gestire inconsistenze API MikroTik:
- **Boolean Adapter**: Converte stringhe "true"/"false" in booleani
- **Int Adapter**: Converte stringhe numeriche in int
- `KotlinJsonAdapterFactory` per data classes

#### AuthInterceptor
```kotlin
class AuthInterceptor : Interceptor {
    private var credentials: String? = null
    
    fun setCredentials(username: String, password: String) {
        credentials = Credentials.basic(username, password)
    }
    
    override fun intercept(chain: Chain): Response {
        val request = chain.request().newBuilder()
            .header("Authorization", credentials ?: "")
            .build()
        return chain.proceed(request)
    }
}
```

---

## 📦 REPOSITORY LAYER

### AppRepository
Repository principale che orchestra tutte le operazioni di rete e business logic.

#### Responsabilità Principali
1. **Configurazione Sonda Dinamica**: Imposta la sonda corrente per i test
2. **Esecuzione Test**: Wrapper per tutte le chiamate API
3. **Monitoring**: Polling stato sonde
4. **Risoluzione Target**: Risolve "DHCP_GATEWAY" in IP reale

#### Metodi Chiave

```kotlin
// Imposta la sonda corrente (autenticazione + binding rete WiFi)
suspend fun setProbe(probe: ProbeConfig)

// Verifica connessione sonda (ritorna board name e interfacce)
suspend fun checkProbeConnection(probe: ProbeConfig): ProbeCheckResult

// Test di certificazione
suspend fun runCableTest(probe: ProbeConfig, interfaceName: String): UiState<CableTestResult>
suspend fun getLinkStatus(probe: ProbeConfig, interfaceName: String): UiState<MonitorResponse>
suspend fun getNeighborsForInterface(probe: ProbeConfig, interfaceName: String): UiState<List<NeighborDetail>>
suspend fun runPing(probe: ProbeConfig, target: String, interfaceName: String): UiState<PingResult>

// Risolve "DHCP_GATEWAY" nell'IP del gateway DHCP dell'interfaccia
suspend fun resolveTargetIp(target: String, interfaceName: String): String

// Polling stato sonda (ogni 15 secondi)
fun observeProbeStatus(probe: ProbeConfig): Flow<Boolean>

// Polling tutte le sonde (ogni 10 secondi)
fun observeAllProbesWithStatus(): Flow<List<ProbeStatusInfo>>
```

#### UiState Wrapper
```kotlin
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
```

### BackupRepository
Gestisce import/export configurazioni (sonde + profili).

```kotlin
// Esporta sonde e profili in JSON
suspend fun exportConfigToJson(): String

// Importa da JSON (cancella dati esistenti)
suspend fun importConfigFromJson(json: String)
```

**Formato JSON**:
```json
{
  "probes": [...],
  "profiles": [...]
}
```

---

## 🎨 UI LAYER - SCHERMATE E NAVIGAZIONE

### Navigazione (NavGraph)
L'app usa **Jetpack Compose Navigation** con rotte tipizzate.

#### Grafo di Navigazione
```
dashboard (start) ──┬──> test_execution/{clientId}/{probeId}/{profileId}/{socketName}
                    ├──> history ──> report_detail/{reportId}
                    ├──> settings ──┬──> client_list ──┬──> client_add
                    │               │                   └──> client_edit/{clientId}
                    │               ├──> probe_list ──┬──> probe_add
                    │               │                  └──> probe_edit/{probeId}
                    │               └──> profile_list ──┬──> profile_add
                    │                                   └──> profile_edit/{profileId}
```

### Schermate Dettagliate

---

#### 1. **DashboardScreen** (Schermata Principale)
**Rotta**: `dashboard`  
**ViewModel**: `DashboardViewModel`

**Funzionalità**:
- Selezione **Cliente** (lista espandibile con RadioButton)
- Selezione **Sonda** (lista espandibile con RadioButton + indicatore online/offline)
- Selezione **Profilo di Test** (lista espandibile con RadioButton)
- Input **Socket Name** (auto-generato: `prefix + progressivo`, es. "PRT-001")
- Pulsante **"AVVIA TEST"** (abilitato solo se tutti i campi sono compilati e sonda online)
- Link rapidi: **History** e **Settings**

**UI Elements**:
- **TopBar**: Titolo "Dashboard" + badge storico + icona settings
- **Header Card**: Info app con icona Dashboard
- **SelectionCard** (Componente Custom):
    - Surface clickabile che espande/collassa lista RadioButton
    - Bordo colorato (primary quando espansa, outline quando chiusa)
    - AnimatedVisibility con `expandVertically()` + `fadeIn()` / `shrinkVertically()` + `fadeOut()`
    - Item selezionato evidenziato con background `primaryContainer` e bordo `primary`
    - Pulsante "GESTISCI" per navigare alla schermata di gestione
- **Status Chips**: Mostra cliente e sonda selezionati con stato colore
- **Bottom Button**: Pulsante animato verde quando pronto

**State Management**:
```kotlin
val clients: StateFlow<List<Client>>
val probes: StateFlow<List<ProbeConfig>>
val profiles: StateFlow<List<TestProfile>>
val selectedClient: MutableStateFlow<Client?>
val selectedProbe: MutableStateFlow<ProbeConfig?>
val selectedProfile: MutableStateFlow<TestProfile?>
val socketName: MutableStateFlow<String>
val isProbeOnline: StateFlow<Boolean> // Polling automatico
```

**Business Logic**:
- Al cambio di `selectedClient`, `socketName` viene auto-calcolato:
    - Legge l'ultimo report del cliente
    - Incrementa il numero progressivo
    - Formatta con 3 cifre: `prefix + "%03d"`

---

#### 2. **TestExecutionScreen** (Esecuzione Test)
**Rotta**: `test_execution/clientId={clientId}&probeId={probeId}&profileId={profileId}&socketName={socketName}`  
**ViewModel**: `TestViewModel`

**UI**:
- **TopAppBar**: Titolo dinamico ("Test in corso..." / "✓ Test Completato" / "✗ Test Fallito") + navigationIcon Back (ArrowBack)
- **TopAppBar Color**: Cambia in base allo stato (primaryContainer / green / red)

**Funzionalità**:
- **Auto-start**: Il test parte automaticamente all'apertura della schermata
- **Log Real-time**: Mostra log testuale del progresso (scrollabile)
- **Progress Indicator**: Animazione durante l'esecuzione
- **Result Display**: Card colorata con esito (verde PASS / rosso FAIL)
- **Azioni Post-Test**: CHIUDI | RIPETI | SALVA

**Fasi del Test** (sequenziale):
1. **Caricamento Dati**: Client, Probe, Profile da DB
2. **Impostazione Sonda**: `repository.setProbe(probe)`
3. **Esecuzione Test** (in ordine):
    - **TDR** (se `profile.runTdr && probe.tdrSupported`)
    - **Link Status** (se `profile.runLinkStatus`) - **BLOCCO CRITICO**: Se link DOWN, test interrotto
    - **LLDP/CDP** (se `profile.runLldp`) - Cerca switch "bridge" direttamente connesso
    - **Ping** (se `profile.runPing`) - Testa fino a 3 target, risolve "DHCP_GATEWAY"
4. **Generazione Report**: Serializza risultati in JSON, crea oggetto `Report`
5. **Salvataggio** (solo se utente preme SALVA):
    - Insert report in DB
    - Aggiorna `nextIdNumber` e sticky fields del cliente

**UI States**:
- `Loading`: Progress indicator + log in tempo reale
- `Success`: Risultato finale + bottoni azione
- `Error`: Card errore

**Log Messages** (esempi):
```
--- INIZIO TEST ---
Cliente: ACME Inc | Presa: PRT-001
Sonda 'MikroTik-Lab' selezionata.
Esecuzione TDR (Cable-Test)...
TDR: SUCCESSO.
Esecuzione Test Stato Link...
Stato Link: SUCCESSO (link-ok @ 1Gbps)
Esecuzione Test LLDP/CDP...
LLDP: SUCCESSO (Switch: SW-CORE-01, Porta: ether12)
Esecuzione Test Ping...
Ping (192.168.1.1): SUCCESSO (1.2ms)
Ping (8.8.8.8): SUCCESSO (15.3ms)
--- TEST COMPLETATO ---
```

**Overall Status Logic**:
- Inizia come "PASS"
- Se qualsiasi test fallisce → "FAIL"
- Se link DOWN → test interrotto + "FAIL"

---

#### 3. **HistoryScreen** (Storico Test)
**Rotta**: `history`  
**ViewModel**: `HistoryViewModel`

**Funzionalità**:
- Lista **report raggruppati per cliente**
- Card espandibile per ogni cliente (mostra report figli)
- Badge "PASS"/"FAIL" colorato per ogni report
- **Export PDF Batch**: Esporta tutti i report di un cliente in un singolo PDF
- **Export PDF Singolo**: Dal dettaglio del report
- **Delete Report**: Swipe-to-delete (con dialog conferma)

**UI Elements**:
- **Empty State**: Icona + messaggio "No test reports yet"
- **Client Card**:
    - Header: Nome cliente + conteggio report + percentuale successo
    - Expanded: Lista report con timestamp, socket, stato
- **Action Buttons**: PDF batch export, visualizza dettaglio

**Data Model**:
```kotlin
data class ReportsByClient(
    val client: Client?,
    val reports: List<Report>
)
```

---

#### 4. **ReportDetailScreen** (Dettaglio Report)
**Rotta**: `report_detail/{reportId}`  
**ViewModel**: `ReportDetailViewModel`

**UI**:
- **TopAppBar**: Titolo "Report #[ID]" + navigationIcon Back (ArrowBack) + Action Export PDF

**Funzionalità**:
- **Tab "Summary"**: Info generale + risultati Link/LLDP/Ping
- **Tab "Physical Layer"**: Risultati TDR (cable pairs)
- **Tab "Edit"**: Modifica socket name, floor, room, notes
- **Export PDF**: Singolo report

**Parsed Results**:
Il `resultsJson` viene deserializzato in:
```kotlin
data class ParsedResults(
    val tdr: CableTestResult?,
    val link: MonitorResponse?,
    val lldp: NeighborDetail?,
    val pingResults: Map<String, PingResult>
)
```

---

#### 5. **ClientListScreen** (Gestione Clienti)
**Rotta**: `client_list`  
**ViewModel**: `ClientListViewModel`

**Funzionalità**:
- Lista tutti i clienti (ordinati alfabeticamente)
- **Export Project Report**: PDF con tutti i report del cliente (launcher file)
- **Edit/Delete**: Click su card per modificare, swipe per eliminare
- **FAB**: "NUOVO CLIENTE" (verde)

**Client Card**:
- Nome azienda + località
- Badge network mode (DHCP/STATIC)
- Conteggio report totali
- Icone azioni: PDF export, edit, delete

**Empty State**: Icona + messaggio + suggerimento

---

#### 6. **ClientEditScreen** (Modifica/Nuovo Cliente)
**Rotta**: `client_add` | `client_edit/{clientId}`  
**ViewModel**: `ClientEditViewModel`

**UI**:
- **TopAppBar**: Titolo "Edit Client" / "New Client" + navigationIcon Back (ArrowBack)

**Sezioni Form**:
1. **Client Info**:
    - Company Name (required)
    - Location
    - Socket ID Prefix
    - Notes

2. **Sticky Fields**:
    - Last Floor
    - Last Room

3. **Network Settings**:
    - Mode: DHCP / STATIC (segmented button workaround)
    - Se STATIC: IP, Subnet, Gateway
    - Se DHCP+VLAN: VLAN ID

**Validation**:
- Save button abilitato solo se `companyName.isNotBlank()`

**Auto-Close**: Quando `isSaved` diventa true, popBackStack

---

#### 7. **ProbeListScreen** (Gestione Sonde)
**Rotta**: `probe_list`  
**ViewModel**: `ProbeListViewModel`

**Funzionalità**:
- Lista sonde con **indicatore stato real-time** (verde online / rosso offline)
- Polling continuo ogni 10 secondi
- Badge modello + supporto TDR
- **FAB**: "NUOVA SONDA" (blu)

**Probe Card**:
- Nome sonda + IP
- Indicatore online (pulsante verde/rosso)
- Badge modello (es. "RB750Gr3")
- Badge "TDR Support" (se supportato)
- Interface di test
- Icone azioni: edit, delete

**Monitoring Logic**:
```kotlin
val probes: StateFlow<List<ProbeStatusInfo>>
// ProbeStatusInfo(probe: ProbeConfig, isOnline: Boolean)
```

---

#### 8. **ProbeEditScreen** (Modifica/Nuova Sonda)
**Rotta**: `probe_add` | `probe_edit/{probeId}`  
**ViewModel**: `ProbeEditViewModel`

**UI**:
- **TopAppBar**: Titolo "Edit Probe" / "Add Probe" + navigationIcon Back (ArrowBack)

**Workflow**:
1. Input credenziali: Name, IP, Username, Password
2. Toggle HTTPS (con warning certificati ignorati)
3. **VERIFY PROBE**: Pulsante che testa la connessione
4. **Verification Success**:
    - Mostra board name
    - Dropdown "Test Interface" (popolato con interfacce rilevate)
    - Rileva supporto TDR (automatico)
5. **SAVE**: Abilitato solo dopo verifica riuscita

**Verification States**:
```kotlin
sealed class VerificationState {
    object Idle
    object Loading
    data class Success(val boardName: String?, val interfaces: List<String>)
    data class Error(val message: String)
}
```

**Business Logic**:
- Chiama `repository.checkProbeConnection(probe)`
- Se successo, salva `modelName` e `tdrSupported`

---

#### 9. **TestProfileListScreen** (Gestione Profili)
**Rotta**: `profile_list`  
**ViewModel**: `TestProfileViewModel`

**Funzionalità**:
- Lista profili di test (ordinati alfabeticamente)
- Badge per ogni test abilitato (TDR, Link, LLDP, Ping)
- **FAB**: "NUOVO PROFILO" (viola)

**Profile Card**:
- Nome profilo + descrizione
- Badge colorati per test abilitati
- Icone azioni: edit, delete

---

#### 10. **TestProfileEditScreen** (Modifica/Nuovo Profilo)
**Rotta**: `profile_add` | `profile_edit/{profileId}`  
**ViewModel**: `TestProfileViewModel`

**Sezioni Form**:
1. **Info**: Profile Name, Description
2. **Test Toggles**:
    - Run TDR (Cable-Test)
    - Run Link Status Test
    - Run LLDP/CDP Neighbor Test
    - Run Ping Test
3. **Ping Targets** (se Ping abilitato):
    - Target 1, 2, 3
    - Supporta IP o "DHCP_GATEWAY"

**Validation**: Save abilitato se `profileName.isNotBlank()`

---

#### 11. **SettingsScreen** (Impostazioni)
**Rotta**: `settings`  
**ViewModel**: `SettingsViewModel`

**UI**:
- **TopAppBar**: Titolo "Impostazioni" + navigationIcon Back (ArrowBack)

**Sezioni**:
1. **Gestione Dati**:
    - **Clienti**: → `client_list`
    - **Sonde**: → `probe_list`
    - **Profili di Test**: → `profile_list`

2. **Import/Export**:
    - **Esporta Configurazione**: Salva JSON con sonde + profili
    - **Importa Configurazione**: Carica JSON (sovrascrive tutto)

3. **Informazioni App**:
    - Versione
    - Credits
    - (Eventuale: Clear All Data)

**UI**: Cards con icone, navigazione verso schermate gestione

---

## ⚙️ FUNZIONALITÀ PRINCIPALI

### 1. Test di Certificazione Automatizzato

#### TDR (Time Domain Reflectometry)
**Comando RouterOS**: `/interface/ethernet/cable-test`

**Cosa Misura**:
- Lunghezza cavi per ogni coppia (pair 1-4)
- Rilevamento cortocircuiti/interruzioni
- Stato cablaggio fisico

**Limitazioni**:
- Solo su modelli MikroTik compatibili (es. RB750Gr3, ma NON su CRS3xx)
- `tdrSupported` rilevato durante verifica sonda

**Output Esempio**:
```json
{
  "cablePairs": [
    {"pair": "1-2", "length": "15m", "status": "ok"},
    {"pair": "3-6", "length": "15m", "status": "ok"},
    {"pair": "4-5", "length": "15m", "status": "ok"},
    {"pair": "7-8", "length": "15m", "status": "ok"}
  ],
  "status": "ok"
}
```

---

#### Link Status Monitoring
**Comando RouterOS**: `/interface/ethernet/monitor`

**Cosa Misura**:
- Stato link: `link-ok` / `no-link` / `down`
- Velocità negoziata: `10Mbps` / `100Mbps` / `1Gbps` / `10Gbps`
- Duplex mode

**Logica Critica**:
- Se `status.contains("down")` → **Test interrotto immediatamente**
- Motivo: inutile testare LLDP/Ping se il link è DOWN

**Output Esempio**:
```json
{
  "status": "link-ok",
  "rate": "1Gbps"
}
```

---

#### LLDP/CDP Neighbor Discovery
**Comando RouterOS**: `/ip/neighbor/print`

**Cosa Rileva**:
- Switch/device direttamente connesso
- Porta switch (interface name)
- VLAN ID (se presente)
- System capabilities (bridge, router, etc.)
- Discovery protocol (LLDP o CDP)

**Filtro Applicato**:
```kotlin
fun findDirectlyConnectedSwitch(neighbors: List<NeighborDetail>): NeighborDetail? {
    return neighbors
        .filter { it.systemCaps?.contains("bridge", ignoreCase = true) == true }
        .maxByOrNull { 
            when (it.discoveredBy?.lowercase()) {
                "lldp" -> 2  // Priorità LLDP
                "cdp" -> 1   // Poi CDP
                else -> 0
            }
        }
}
```

**Output Esempio**:
```json
{
  "identity": "SW-CORE-01",
  "interfaceName": "ether12",
  "systemCaps": "bridge,router",
  "discoveredBy": "lldp",
  "vlanId": "100"
}
```

---

#### Ping Test
**Comando RouterOS**: `/ping`

**Targets Supportati**:
- IP statico (es. `8.8.8.8`)
- Hostname (es. `google.com`)
- **"DHCP_GATEWAY"**: Keyword speciale → risolto dinamicamente

**Risoluzione DHCP_GATEWAY**:
1. Query `/ip/dhcp-client/print` filtrato per interfaccia
2. Estrae campo `gateway`
3. Usa come target ping

**Validation**:
- Successo se `avgRtt` presente e > 0
- Fallimento se `avgRtt` null, blank o "0"

**Output Esempio**:
```json
{
  "avgRtt": "1.5ms"
}
```

---

### 2. Generazione PDF Professionale

**Libreria**: Android Native `PdfDocument`  
**Classe**: `PdfGenerator`

#### Funzionalità
1. **Single Report PDF**: Genera PDF per un singolo report
2. **Batch PDF**: Genera PDF con indice + tutte le pagine dei report

#### Layout PDF
- **Formato**: A4 (595x842 pt)
- **Font**: Android Default + Bold per header
- **Colori**:
    - Header: `#004D40` (teal scuro)
    - Success: `#2E7D32` (verde)
    - Fail: `#C62828` (rosso)
    - Linee: `#LTGRAY`

#### Sezioni PDF
1. **Header**: Titolo "MikLink Test Report"
2. **Metadata**:
    - Cliente, Socket, Timestamp
    - Sonda, Profilo
    - Floor, Room
3. **Status Section**: Badge PASS/FAIL
4. **Test Results**:
    - TDR: Tabella cable pairs
    - Link: Stato + velocità
    - LLDP: Switch + porta
    - Ping: Target + RTT
5. **Notes**: Se presenti
6. **Footer**: "Generated by MikLink | Page X/Y"

#### Multi-Page Support
- Auto-break pagina a 762pt
- Footer su ogni pagina

---

### 3. Import/Export Configurazioni

**Formato**: JSON con Moshi  
**Repository**: `BackupRepository`

#### Export
```json
{
  "probes": [
    {
      "probeId": 1,
      "name": "MikroTik-Lab",
      "ipAddress": "192.168.88.1",
      // ... tutti i campi
    }
  ],
  "profiles": [
    {
      "profileId": 1,
      "profileName": "Full Test",
      // ... tutti i campi
    }
  ]
}
```

#### Import
- **ATTENZIONE**: Elimina TUTTE le sonde e profili esistenti
- Inserisce i nuovi dati
- Report NON vengono toccati (solo config)

#### Use Case
- Backup configurazione prima di reset
- Condivisione setup tra dispositivi
- Template aziendali

---

### 4. Probe Monitoring

**Polling Automatico**:
- `observeProbeStatus(probe)`: 15 secondi (per sonda selezionata in dashboard)
- `observeAllProbesWithStatus()`: 10 secondi (per lista sonde)

**Meccanismo**:
```kotlin
while (true) {
    val isOnline = try {
        setProbe(probe)
        apiService?.getSystemResource(ProplistRequest(listOf("board-name")))
            ?.isNotEmpty() == true
    } catch (e: Exception) {
        false
    }
    emit(isOnline)
    delay(pollingInterval)
}
```

**UI Feedback**:
- Badge colorato verde/rosso
- Pulsante test abilitato solo se online

---

### 5. Socket Name Auto-Increment

**Business Logic** (in `DashboardViewModel`):
```kotlin
selectedClient.collect { client ->
    if (client != null) {
        val lastReport = reportDao.getLastReportForClient(client.clientId)
        val nextNumber = if (lastReport == null) {
            1
        } else {
            val lastNumber = lastReport.socketName
                ?.removePrefix(client.socketPrefix)
                ?.toIntOrNull() ?: 0
            lastNumber + 1
        }
        socketName.value = "${client.socketPrefix}${String.format("%03d", nextNumber)}"
    }
}
```

**Esempio**:
- Cliente: ACME Inc, Prefix: "PRT-"
- Ultimo report: "PRT-005"
- Prossimo socket: "PRT-006"

---

### 6. Sticky Fields (Floor/Room)

**Comportamento**:
- All'apertura del `TestExecutionScreen`, i campi Floor/Room sono **pre-compilati** con `client.lastFloor` e `client.lastRoom`
- Dopo il salvataggio del report, questi valori vengono aggiornati nel DB cliente
- Risparmia tempo durante certificazioni di edifici multi-piano

**Update Query**:
```kotlin
@Query("UPDATE clients SET nextIdNumber = nextIdNumber + 1, lastFloor = :floor, lastRoom = :room WHERE clientId = :id")
suspend fun updateNextIdAndStickyFields(id: Long, floor: String?, room: String?)
```

---

## 🔄 FLUSSO DI TEST COMPLETO

### Diagramma di Sequenza

```
User → DashboardScreen → TestExecutionScreen → TestViewModel → AppRepository → MikroTik
  |          |                    |                  |                |              |
  |   Seleziona Client           |                  |                |              |
  |   Seleziona Probe            |                  |                |              |
  |   Seleziona Profile          |                  |                |              |
  |   [Socket auto-generato]     |                  |                |              |
  |          |                    |                  |                |              |
  |   Click "AVVIA TEST"          |                  |                |              |
  |          |------------------->|                  |                |              |
  |          |                    | startTest()      |                |              |
  |          |                    |----------------->|                |              |
  |          |                    |                  | setProbe()     |              |
  |          |                    |                  |--------------->|              |
  |          |                    |                  |                | Auth + WiFi  |
  |          |                    |                  |                | Binding      |
  |          |                    |                  |<---------------|              |
  |          |                    |                  |                |              |
  |          |                    |                  | runCableTest() |              |
  |          |                    |                  |--------------->|------------->|
  |          |                    |                  |                |<-------------|
  |          |                    |                  | getLinkStatus()|              |
  |          |                    |                  |--------------->|------------->|
  |          |                    |                  |                |<-------------|
  |          |                    |                  | [Link DOWN?]   |              |
  |          |                    |                  | → STOP TEST    |              |
  |          |                    |                  |                |              |
  |          |                    |                  | getNeighbors() |              |
  |          |                    |                  |--------------->|------------->|
  |          |                    |                  |                |<-------------|
  |          |                    |                  | runPing(x3)    |              |
  |          |                    |                  |--------------->|------------->|
  |          |                    |                  |                |<-------------|
  |          |                    |                  |                |              |
  |          |                    | Generate Report  |                |              |
  |          |                    |<-----------------|                |              |
  |          |  Show Results      |                  |                |              |
  |          |<-------------------|                  |                |              |
  |          |                    |                  |                |              |
  | Click "SALVA"                 |                  |                |              |
  |          |------------------->|                  |                |              |
  |          |                    | saveReportToDb() |                |              |
  |          |                    |----------------->| reportDao.insert()            |
  |          |                    |                  | clientDao.updateNextId()      |
  |          | popBackStack()     |                  |                |              |
  |<---------|                    |                  |                |              |
```

---

## 💾 EXPORT E BACKUP

### PDF Export Scenarios

#### 1. Single Report PDF
**Trigger**: ReportDetailScreen → Export button  
**Launcher**: `CreateDocument("application/pdf")`  
**File Name**: `report_{reportId}.pdf`

#### 2. Batch Client PDF
**Trigger**: HistoryScreen → Client card → Batch export  
**Launcher**: `CreateDocument("application/pdf")`  
**File Name**: `{clientName}_reports.pdf`  
**Content**: Index page + tutte le pagine dei report

#### 3. Project Report PDF
**Trigger**: ClientListScreen → Client card → Project report  
**Launcher**: `CreateDocument("application/pdf")`  
**File Name**: `{clientName}_project.pdf`  
**Content**: Simile a batch, più informazioni aggregate

### Config Import/Export

#### Export Config
**Trigger**: SettingsScreen → Export button  
**Launcher**: `CreateDocument("application/json")`  
**File Name**: `miklink_config.json`  
**Content**: Probes + Profiles (NO Reports, NO Clients)

#### Import Config
**Trigger**: SettingsScreen → Import button  
**Launcher**: `OpenDocument("application/json")`  
**Warning**: Dialog conferma (elimina config esistente)

---

## 🛠️ CONFIGURAZIONE E BUILD

### Gradle Configuration

#### App Module (`app/build.gradle.kts`)
```kotlin
android {
    namespace = "com.app.miklink"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.app.miklink"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    
    kotlinOptions {
        jvmTarget = "21"
    }
}
```

### Build Status
**File**: `BUILD_STATUS.md` (presente nel progetto)  
**Stato**: ✅ Compila correttamente  
**Note**: Java 21 configurato, build funzionante

### Known Issues Risolti
- **Probe Monitoring**: Fix polling implementato (vedi `FIX_PROBE_MONITORING.md`)
- **PDF Export**: Fix multi-page implementato (vedi `PDF_EXPORT_FIX.md`)
- **UI Refactoring**: Completato (vedi `UI_REFACTORING_COMPLETE.md`)

---

## ⚠️ NOTE TECNICHE E LIMITAZIONI

### Sicurezza
1. **Cleartext Traffic**: Abilitato per compatibilità HTTP
2. **SSL Certificate Validation**: **DISABILITATA** (trust all certificates)
    - Necessario per HTTPS self-signed su MikroTik
    - ⚠️ Non usare per comunicazioni internet pubbliche
3. **Credentials Storage**: Password salvate in chiaro nel DB locale
    - ⚠️ Miglioramento futuro: EncryptedSharedPreferences

### Performance
1. **Main Thread Blocking**: **RISOLTO**
    - Tutte le operazioni DB/Network su `Dispatchers.IO`
    - UI sempre reattiva (State Hoisting + Flow)

2. **Polling Impact**:
    - Probe monitoring consuma rete/batteria
    - Considera pause quando app in background

### Compatibilità MikroTik
1. **RouterOS Version**: Richiede **v7.x+** (REST API)
2. **TDR Support**: Solo modelli con chip supportato (verificato runtime)
3. **LLDP/CDP**: Deve essere abilitato in RouterOS (`/ip/neighbor`)

### Network Binding
- **WiFi Preferred**: Se disponibile rete WiFi, forza routing tramite WiFi
- Utile quando smartphone ha anche dati mobili attivi
- Evita routing su 4G/5G quando MikroTik è su LAN

### Database Migrations
- **Strategy**: `fallbackToDestructiveMigration()`
- ⚠️ Cambio schema = perdita dati
- Miglioramento futuro: Migration scripts

### UI Limitations
1. **SegmentedButton**: Non usato (workaround con Row di Button)
    - Material3 `SegmentedButton` ha problemi su alcune versioni
2. **Dark Mode**: Non personalizzato (usa system default)

---

## 📊 STATISTICHE PROGETTO

### File Structure
- **Total Kotlin Files**: 0
- **Screens**: 0
- **ViewModels**: 0
- **Database Entities**: 0
- **DAOs**: 0
- **Repositories**: 0

### Lines of Code (stima)
- UI Layer: ~3000 LOC
- Data Layer: ~1500 LOC
- Network Layer: ~500 LOC
- Utils/DI: ~300 LOC
- **Total**: ~5300 LOC

### Dependencies
- **Production**: 20+ librerie
- **Build/Annotation Processors**: 3 (KSP, Hilt, Room)

---

## 🎯 ROADMAP SUGGERITO

### Priorità Alta
1. **Encrypted Credentials**: Migrare password a EncryptedSharedPreferences
2. **Database Migrations**: Implementare migration scripts invece di destructive
3. **Background Sync**: Service per backup automatico report su cloud

### Priorità Media
4. **Dark Mode**: Tema personalizzato
5. **Statistiche Cliente**: Dashboard con grafici successo/fallimento
6. **Filtri Avanzati**: History screen con filtri per data/stato/cliente
7. **Export Excel**: Alternativa a PDF per analisi dati

### Priorità Bassa
8. **Multi-Utente**: Login e permission system
9. **Cloud Sync**: Firebase/backend per condivisione report
10. **Widget**: Home screen widget con stato sonde

---

## 📞 SUPPORTO E MANUTENZIONE

### Logging
- **Tag Principali**: `AppRepository`, `PdfGenerator`, `TestViewModel`
- **Level**: Debug (LoggingInterceptor a BODY)

### Debugging Network
1. Abilita logging Retrofit: già attivo
2. Usa Wireshark per analisi traffico MikroTik
3. Testa REST API con Postman prima di integrare

### Testing
- **Unit Tests**: Template in `ExampleUnitTest.kt` (da implementare)
- **Instrumented Tests**: Template in `ExampleInstrumentedTest.kt`
- **Manual Testing**: Checklist in docs (da creare)

---

## 📅 Aggiornamenti recenti (feature)

### 2025-11-14 - Fix: Autostart Test e Guardie Anti-Doppio Avvio

**Sintesi del fix:**
- Riabilitato l'avvio automatico del test quando si naviga alla schermata `TestExecutionScreen` (LaunchedEffect) in modo sicuro: il ViewModel viene avviato automaticamente solo se `uiState` è `Idle` e `_isRunning` è `false`.
- Aggiunta guardia in `TestViewModel.startTest()` per ignorare chiamate multiple mentre un test è in corso (log di diagnostica: "Ignorato startTest(): test già in esecuzione").

**File modificati:**
- `app/src/main/java/com/app/miklink/ui/test/TestExecutionScreen.kt` — riattivato `LaunchedEffect(Unit)` con controllo di stato prima dell'avvio.
- `app/src/main/java/com/app/miklink/ui/test/TestViewModel.kt` — aggiunta guardia `_isRunning` all'inizio di `startTest()`.

**Motivazione:**
La navigazione da `Dashboard` apriva `TestExecutionScreen` ma non avviava il test automaticamente (regressione introdotta in refactor UI). Alcuni utenti dovevano premere due volte "AVVIA TEST" e in alcuni casi le card delle sezioni non venivano popolate a causa di avvi multipli o stati incoerenti. Questo fix garantisce che:
- All'apertura della screen il test viene avviato automaticamente (UX coerente con navigazione da Dashboard).
- Non è possibile avviare contemporaneamente più esecuzioni dello stesso test (protection contro doppio-tap o ricomposizioni multiple).

**Testing suggerito (manuale):**
1. Aprire l'app, selezionare Cliente/Sonda/Profilo e premere "AVVIA TEST" dal Dashboard: la `TestExecutionScreen` dovrebbe aprirsi e partire immediatamente l'esecuzione (vedi log "--- INIZIO TEST ---").
2. Osservare le card: devono comparire dinamicamente (Network, TDR, Link, LLDP, Ping, Traceroute) durante l'esecuzione.
3. Provare a premere rapidamente "RIPETI" o "AVVIA TEST" più volte: il log deve mostrare "Ignorato startTest(): test già in esecuzione" e non devono partire più esecuzioni parallele.
4. Verificare salvataggio report e navigazione indietro: dopo `SALVA` il report deve essere inserito nel DB.

**Note per sviluppatori:**
- In futuro valutare l'introduzione di un argomento `autostart` nella route `test_execution` per avere un controllo più esplicito (es. `test_execution/{clientId}/{probeId}/{profileId}/{socketName}?autostart=true`).
- Implementare cancellazione test (cancel job) e UI di abort per migliorare esperienza e robustezza.

---

### 2025-11-14 - Allineamento `minLinkRate` e Card Aggregate per Dettagli Test

**Sintesi:**
- Introdotta una utility `RateParser` per normalizzare le rappresentazioni di velocità (es. `1G`, `1Gbps`, `100Mbps`, numerici) in Mbps. Questo rende la valutazione della soglia `minLinkRate` robusta rispetto a formati eterogenei restituiti dalle sonde o salvati in DB.
- La funzione `isRateOk(rate, min)` in `TestViewModel` è stata rifattorizzata per utilizzare `RateParser.parseToMbps()`.
- La UI di riepilogo dei dettagli test (`TestCompletedView`) ora mostra card aggregate e ordinati: prima le INFO (Network/DHCP, LLDP/CDP), poi una card per ogni test rilevante (Link, Ping, Traceroute, TDR se presente). Se non sono disponibili sezioni, viene mostrato il log grezzo come fallback.

**Perché:**
- Cambiando la rappresentazione della velocità nelle impostazioni (es. opzioni `10M`, `100M`, `1G`, `10G`) si generavano incongruenze tra il valore atteso e il valore restituito dal monitor della sonda (es. `1Gbps`, `1000Mb`). La normalizzazione evita falsi FAIL o PASS.
- Le card aggregate migliorano la leggibilità: un'unica card per Network/DHCP, una per LLDP, e una card ciascuna per i test Link/Ping/Traceroute, invece delle singole righe log-card.

**Modifiche principali (file):**
- `app/src/main/java/com/app/miklink/utils/RateParser.kt` — Nuova utility con `parseToMbps(raw: String?): Int` e `formatReadable(mbps: Int): String`.
- `app/src/main/java/com/app/miklink/ui/test/TestViewModel.kt` — `isRateOk` ora usa `RateParser`; `buildSectionsFromResults` rimane la fonte per le card ma ora è tollerante a formati legacy.
- `app/src/main/java/com/app/miklink/ui/test/TestExecutionScreen.kt` — `TestCompletedView` ora riceve `sections` e renderizza card aggregate (INFO: Network, LLDP; TEST: Link, Ping, Traceroute, TDR) in ordine fisso.
- `app/src/main/java/com/app/miklink/ui/client/ClientEditScreen.kt` e `ClientEditViewModel.kt` — `minLinkRate` rimane una scelta canonica (`10M`,`100M`,`1G`,`10G`) e viene salvato in quella forma; la parser gestisce eventuali valori legacy.

**Politiche e decisioni**
- Fallback per sezioni vuote: Mostrare il log grezzo (scelta A).
- Ordine dei test nelle card: ordine fisso Link → Ping → Traceroute → TDR (scelta A).
- Non è stata implementata la cancellazione del test in questa iterazione.

**Cosa testare manualmente**
1. Creare/modificare un `Client` e impostare `Min Link Rate` su ciascuna delle opzioni (`10M`, `100M`, `1G`, `10G`).
2. Avviare un test dalla `Dashboard` con profilo che esegue `Link`:
   - Confermare che `TestExecutionScreen` si avvii automaticamente e che dopo il completamento appaia la pagina di riepilogo.
   - Controllare la card `Link`: il valore `Rate` visualizzato deve essere leggibile e la valutazione PASS/FAIL coerente con la soglia impostata (es. `1G` = 1000 Mbps).
3. Provare casi edge: la sonda ritorna `rate` in formati diversi (`1Gbps`, `1000`, `1000Mb`) e confermare che la valutazione sia corretta.
4. Se `sections` è vuoto, verificare che venga mostrato il log grezzo come fallback.

**Note tecniche**
- `RateParser.parseToMbps()` è pensata per essere tollerante e restituire 0 su formati non interpretabili (log di warning). La UI mostrerà `-` o `N/A` dove opportuno.
- Non è stata cambiata la struttura del DB, `Client.minLinkRate` rimane `String` e compatibile con la parser.

