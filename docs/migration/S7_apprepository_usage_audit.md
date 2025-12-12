# S7-B - Audit Dipendenze Reali verso AppRepository

**Data Audit:** 2025-01-XX  
**Scope:** `app/src/main/java/` - ViewModel e componenti che usano AppRepository

## Metodologia

1. Ricerca file che contengono riferimenti ad `AppRepository`
2. Analisi dettagliata di ogni file per identificare:
   - Dove viene iniettato AppRepository
   - Quali metodi vengono chiamati
   - Classificazione per responsabilità

## File Trovati con Riferimenti ad AppRepository

### ViewModel (UI Layer)

#### 1. DashboardViewModel
**File:** `app/src/main/java/com/app/miklink/ui/dashboard/DashboardViewModel.kt`

**Iniezione:**
- Linea 26: `private val repository: AppRepository` (constructor injection)

**Metodi Chiamati:**

1. **`repository.currentProbe`** (linea 35)
   - Tipo: `Flow<ProbeConfig?>`
   - Uso: `val currentProbe: StateFlow<ProbeConfig?> = repository.currentProbe.stateIn(...)`
   - Responsabilità: **Osservazione sonda corrente** (singola sonda configurata)
   - Note: Usato per mostrare la sonda corrente nella Dashboard

2. **`repository.observeProbeStatus(probe)`** (linea 51)
   - Tipo: `Flow<Boolean>`
   - Firma: `fun observeProbeStatus(probe: ProbeConfig): Flow<Boolean>`
   - Uso: `val isProbeOnline: StateFlow<Boolean> = currentProbe.flatMapLatest { probe -> repository.observeProbeStatus(probe) }`
   - Responsabilità: **Monitoraggio stato online/offline della sonda**
   - Note: Polling periodico dello stato della sonda per aggiornare UI

**Classificazione:** 
- **Probe Observation** (osservazione stato sonda)
- **Probe Status Monitoring** (monitoraggio online/offline)

---

#### 2. ProbeEditViewModel
**File:** `app/src/main/java/com/app/miklink/ui/probe/ProbeEditViewModel.kt`

**Iniezione:**
- Linea 18: `private val repository: AppRepository` (constructor injection)

**Metodi Chiamati:**

1. **`repository.checkProbeConnection(tempProbe)`** (linea 114)
   - Tipo: `suspend fun checkProbeConnection(probe: ProbeConfig): ProbeCheckResult`
   - Uso: `when (val result = repository.checkProbeConnection(tempProbe)) { ... }`
   - Responsabilità: **Verifica connessione e configurazione sonda**
   - Note: 
     - Chiamato quando l'utente clicca "Verify" nella UI di edit probe
     - Ritorna `ProbeCheckResult.Success(boardName, interfaces)` o `ProbeCheckResult.Error(message)`
     - Usato per verificare che le credenziali siano corrette e ottenere informazioni hardware

**Classificazione:**
- **Probe Connectivity Check** (verifica connessione sonda)
- **Probe Configuration Validation** (validazione configurazione)

---

#### 3. ProbeListViewModel
**File:** `app/src/main/java/com/app/miklink/ui/probe/ProbeListViewModel.kt`

**Iniezione:**
- Linea 15: `repository: AppRepository` (constructor injection)

**Metodi Chiamati:**

1. **`repository.observeAllProbesWithStatus()`** (linea 18)
   - Tipo: `fun observeAllProbesWithStatus(): Flow<List<ProbeStatusInfo>>`
   - Uso: `val probes: StateFlow<List<ProbeStatusInfo>> = repository.observeAllProbesWithStatus().stateIn(...)`
   - Responsabilità: **Osservazione lista di tutte le sonde con stato online/offline**
   - Note: 
     - Ritorna `Flow<List<ProbeStatusInfo>>` dove `ProbeStatusInfo = data class(val probe: ProbeConfig, val isOnline: Boolean)`
     - Usato per mostrare lista sonde nella UI (se implementata)

**Classificazione:**
- **Probe List Observation** (osservazione lista sonde)
- **Probe Status Monitoring** (monitoraggio stato multipli)

---

#### 4. TestViewModel
**File:** `app/src/main/java/com/app/miklink/ui/test/TestViewModel.kt`

**Iniezione:**
- Linea 37: `private val repository: AppRepository` (constructor injection)
- **TODO:** `// TODO: Rimuovere quando completamente migrato`

**Metodi Chiamati:**
- **NESSUNO** - Non vengono trovati utilizzi diretti di `repository` nel codice analizzato
- Il ViewModel usa `RunTestUseCase` per eseguire i test (migrato in S5/S6)
- La dipendenza è presente ma non utilizzata

**Classificazione:**
- **Legacy Dependency** (dipendenza legacy non utilizzata)
- **Da rimuovere** dopo verifica completa

---

## Riepilogo Metodi AppRepository Utilizzati

### Metodi Attivamente Utilizzati

1. **`val currentProbe: Flow<ProbeConfig?>`**
   - Usato da: `DashboardViewModel`
   - Responsabilità: Osservazione sonda corrente

2. **`fun observeProbeStatus(probe: ProbeConfig): Flow<Boolean>`**
   - Usato da: `DashboardViewModel`
   - Responsabilità: Monitoraggio stato online/offline singola sonda

3. **`fun observeAllProbesWithStatus(): Flow<List<ProbeStatusInfo>>`**
   - Usato da: `ProbeListViewModel`
   - Responsabilità: Monitoraggio stato online/offline tutte le sonde

4. **`suspend fun checkProbeConnection(probe: ProbeConfig): ProbeCheckResult`**
   - Usato da: `ProbeEditViewModel`
   - Responsabilità: Verifica connessione e validazione configurazione sonda

### Metodi NON Utilizzati (Deprecati da S5/S6)

- `applyClientNetworkConfig` - Migrato a `NetworkConfigRepository` (S6)
- `runCableTest` - Migrato a `RunTestUseCase + CableTestStep` (S5)
- `getLinkStatus` - Migrato a `RunTestUseCase + LinkStatusStep` (S5)
- `getNeighborsForInterface` - Migrato a `RunTestUseCase + NeighborDiscoveryStep` (S5)
- `runPing` - Migrato a `RunTestUseCase + PingStep` (S5)
- `runSpeedTest` - Migrato a `RunTestUseCase + SpeedTestStep` (S5)
- `resolveTargetIp` - Migrato a `PingTargetResolver` (S6)

---

## Classificazione per Responsabilità

### 1. Probe Observation (Osservazione Sonda)
**Componenti:**
- `currentProbe: Flow<ProbeConfig?>` - Sonda corrente (singola)
- `observeAllProbesWithStatus(): Flow<List<ProbeStatusInfo>>` - Lista tutte le sonde

**ViewModel che usano:**
- `DashboardViewModel` (currentProbe)
- `ProbeListViewModel` (observeAllProbesWithStatus)

**Note:**
- Entrambi osservano lo stato delle sonde dal database
- `currentProbe` è probabilmente un Flow che emette la sonda singola configurata
- `observeAllProbesWithStatus` combina dati DB con polling stato online

### 2. Probe Status Monitoring (Monitoraggio Stato Online/Offline)
**Componenti:**
- `observeProbeStatus(probe: ProbeConfig): Flow<Boolean>` - Stato singola sonda
- `observeAllProbesWithStatus(): Flow<List<ProbeStatusInfo>>` - Stato tutte le sonde

**ViewModel che usano:**
- `DashboardViewModel` (observeProbeStatus)
- `ProbeListViewModel` (observeAllProbesWithStatus)

**Note:**
- Polling periodico dello stato online/offline delle sonde
- Probabilmente usa chiamate API MikroTik per verificare connettività

### 3. Probe Connectivity Check (Verifica Connessione)
**Componenti:**
- `checkProbeConnection(probe: ProbeConfig): ProbeCheckResult` - Verifica connessione e ottiene info hardware

**ViewModel che usano:**
- `ProbeEditViewModel` (checkProbeConnection)

**Note:**
- Chiamata one-shot per verificare credenziali e ottenere informazioni hardware
- Ritorna `ProbeCheckResult.Success(boardName, interfaces)` o `Error(message)`

---

## Dipendenze Indirette

### Nessuna dipendenza indiretta trovata
- Tutti i ViewModel accedono direttamente ad AppRepository
- Non ci sono helper o utility che wrappano AppRepository

---

## Analisi Implementazione Attuale (AppRepository_legacy)

Per comprendere come implementare i nuovi repository, è necessario analizzare l'implementazione attuale:

**File:** `app/src/main/java/com/app/miklink/data/repository/AppRepository.kt`

### `currentProbe: Flow<ProbeConfig?>`
- Probabilmente delegato a `probeConfigDao.getSingleProbe()` (Room Flow)

### `observeProbeStatus(probe: ProbeConfig): Flow<Boolean>`
- Polling periodico con `userPreferencesRepository.probePollingInterval`
- Chiama `api.getSystemResource()` per verificare online/offline
- Ritorna `Flow<Boolean>` che emette periodicamente lo stato

### `observeAllProbesWithStatus(): Flow<List<ProbeStatusInfo>>`
- Combina `probeConfigDao.getAllProbes()` con polling periodico
- Per ogni probe chiama `api.getSystemResource()` per verificare stato
- Ritorna `Flow<List<ProbeStatusInfo>>` con stato aggiornato periodicamente

### `checkProbeConnection(probe: ProbeConfig): ProbeCheckResult`
- One-shot: costruisce service, chiama `api.getSystemResource()` e `api.getEthernetInterfaces()`
- Ritorna `ProbeCheckResult.Success(boardName, interfaces)` o `Error(message)`

---

## Prossimi Passi (S7-C)

### Repository da Creare

1. **ProbeObservationRepository**
   - `currentProbe: Flow<ProbeConfig?>` - Osservazione sonda corrente
   - Dipende da: `ProbeRepository` (già esistente in core)

2. **ProbeStatusRepository**
   - `observeProbeStatus(probe: ProbeConfig): Flow<Boolean>` - Monitoraggio stato singola sonda
   - `observeAllProbesWithStatus(): Flow<List<ProbeStatusInfo>>` - Monitoraggio stato tutte le sonde
   - Dipende da: `ProbeRepository`, `MikroTikServiceProvider`, `UserPreferencesRepository`

3. **ProbeConnectivityRepository**
   - `checkProbeConnection(probe: ProbeConfig): ProbeCheckResult` - Verifica connessione
   - Dipende da: `MikroTikServiceProvider`

---

## Note e Domande Aperte

### Domande da Risolvere Prima di Implementare

1. **`currentProbe`**: 
   - È sempre la sonda con `probeId = 1` (singola sonda)?
   - O c'è una logica di selezione?

2. **Polling Interval**:
   - `observeProbeStatus` e `observeAllProbesWithStatus` usano `userPreferencesRepository.probePollingInterval`
   - Questo deve essere mantenuto nei nuovi repository?

3. **Error Handling**:
   - Come gestire errori di rete durante il polling?
   - Ritornare `false` (offline) o propagare errore?

4. **TestViewModel**:
   - La dipendenza non utilizzata può essere rimossa immediatamente?
   - O serve per compatibilità temporanea?

---

## Acceptance S7-B

✅ **Audit completato:**
- Tutti i ViewModel che usano AppRepository identificati
- Tutti i metodi chiamati documentati con firma e uso
- Classificazione per responsabilità completata
- Nessuna dipendenza indiretta trovata
- Prossimi passi (repository da creare) identificati

✅ **Pronto per S7-C:**
- Repository da creare identificati
- Dipendenze necessarie identificate
- Note e domande aperte documentate

