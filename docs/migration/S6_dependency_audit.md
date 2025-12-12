# S6.1 - Inventario Dipendenze Residue da AppRepository nel Path "Run Test"

## File Analizzati

### Path "Run Test" (definito come):
- `core/domain/usecase/test/RunTestUseCaseImpl.kt`
- `data/teststeps/*StepImpl.kt` (tutti gli step)
- `data/repositoryimpl/NetworkConfigRepositoryImpl.kt`
- `data/repositoryimpl/PingTargetResolverImpl.kt`
- `di/TestRunnerModule.kt`
- `di/RepositoryModule.kt`

## Dipendenze Trovate

### 1. NetworkConfigRepositoryImpl.kt
**File:** `app/src/main/java/com/app/miklink/data/repositoryimpl/NetworkConfigRepositoryImpl.kt`
**Simbolo usato:** `AppRepository` (tipo)
**Motivo:** Bridge verso `AppRepository.applyClientNetworkConfig(probe, client, override)`
**Linee:** 5 (import), 19 (constructor), 27 (chiamata metodo)

**Dettaglio:**
- Usa `appRepository.applyClientNetworkConfig(...)` per delegare tutta la logica di configurazione rete
- Restituisce `NetworkConfigFeedback` convertendo da `UiState<NetworkConfigFeedback>`

### 2. PingTargetResolverImpl.kt
**File:** `app/src/main/java/com/app/miklink/data/repositoryimpl/PingTargetResolverImpl.kt`
**Simbolo usato:** Nessun riferimento diretto ad `AppRepository`
**Motivo:** Costruisce direttamente il service usando `MikroTikServiceFactory` e chiama `api.getDhcpClientStatus(interfaceName)`
**Linee:** 35-37 (buildServiceFor), 40-46 (getDhcpGateway)

**Dettaglio:**
- NON usa AppRepository direttamente
- Ma costruisce il service manualmente usando `MikroTikServiceFactory.createService(probe, wifiNetwork?.socketFactory)`
- Chiama direttamente `api.getDhcpClientStatus(interfaceName).firstOrNull()?.gateway`
- Questo è il "bridge service build + DHCP gateway" menzionato nell'EPIC

### 3. Altri file nel path Run Test
**File:** `core/domain/usecase/test/RunTestUseCaseImpl.kt`
- ✅ Nessun riferimento ad AppRepository

**File:** `data/teststeps/NetworkConfigStepImpl.kt`
- ✅ Usa solo `NetworkConfigRepository` (interfaccia)

**File:** `data/teststeps/PingStepImpl.kt`
- ✅ Usa solo `PingTargetResolver` (interfaccia) e `MikroTikTestRepository`

**File:** Altri step (CableTestStepImpl, LinkStatusStepImpl, ecc.)
- ✅ Nessun riferimento ad AppRepository

**File:** `di/TestRunnerModule.kt`
- ✅ Nessun riferimento ad AppRepository

**File:** `di/RepositoryModule.kt`
- ⚠️ Contiene binding per AppRepository_legacy, ma questo è fuori dal path Run Test (usato da altri ViewModel)

## Riepilogo

### Dipendenze Dirette da AppRepository nel Path Run Test:
1. ✅ **NetworkConfigRepositoryImpl** → `AppRepository.applyClientNetworkConfig`
2. ✅ **PingTargetResolverImpl** → Costruisce service direttamente (non usa AppRepository ma replica la logica)

### Dipendenze Indirette (da eliminare):
- Nessuna altra dipendenza trovata nel path Run Test

## Note

- `PingTargetResolverImpl` non usa AppRepository direttamente, ma replica la logica di `buildServiceFor` e `getDhcpGateway` che sono attualmente in AppRepository_legacy
- Questo è il "bridge service build + DHCP gateway" menzionato nell'EPIC S6
- La soluzione sarà centralizzare la creazione del service (S6.2) e estrarre la logica DHCP (S6.3)

