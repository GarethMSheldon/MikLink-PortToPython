# S7-H - Riduzione AppRepository

**Data:** 2025-01-XX  
**EPIC:** S7 - Rimozione dipendenza da AppRepository dalle feature rimanenti

## Verifica Utilizzo Metodi AppRepository

### Metodi Migrati e Non Più Utilizzati

#### 1. `currentProbe: Flow<ProbeConfig?>`
**Sostituito da:** `ProbeConfigDao.getSingleProbe()`  
**Utilizzato da:** DashboardViewModel (migrato)  
**Stato:** ✅ Non più utilizzato da ViewModel  
**Azione:** Deprecato in AppRepository

#### 2. `observeProbeStatus(probe: ProbeConfig): Flow<Boolean>`
**Sostituito da:** `ProbeStatusRepository.observeProbeStatus(probe)`  
**Utilizzato da:** DashboardViewModel (migrato)  
**Stato:** ✅ Non più utilizzato da ViewModel  
**Azione:** Deprecato in AppRepository

#### 3. `observeAllProbesWithStatus(): Flow<List<ProbeStatusInfo>>`
**Sostituito da:** `ProbeStatusRepository.observeAllProbesWithStatus()`  
**Utilizzato da:** ProbeListViewModel (migrato)  
**Stato:** ✅ Non più utilizzato da ViewModel  
**Azione:** Deprecato in AppRepository

#### 4. `checkProbeConnection(probe: ProbeConfig): ProbeCheckResult`
**Sostituito da:** `ProbeConnectivityRepository.checkProbeConnection(probe)`  
**Utilizzato da:** ProbeEditViewModel (migrato)  
**Stato:** ✅ Non più utilizzato da ViewModel  
**Azione:** Deprecato in AppRepository

### Metodi Già Deprecati (S5/S6)

- `applyClientNetworkConfig` - Migrato a `NetworkConfigRepository` (S6)
- `runCableTest` - Migrato a `RunTestUseCase + CableTestStep` (S5)
- `getLinkStatus` - Migrato a `RunTestUseCase + LinkStatusStep` (S5)
- `getNeighborsForInterface` - Migrato a `RunTestUseCase + NeighborDiscoveryStep` (S5)
- `runPing` - Migrato a `RunTestUseCase + PingStep` (S5)
- `runSpeedTest` - Migrato a `RunTestUseCase + SpeedTestStep` (S5)
- `resolveTargetIp` - Migrato a `PingTargetResolver` (S6)

### Metodi Ancora Utilizzati

**Nessuno** - Tutti i metodi utilizzati da Dashboard/Probe sono stati migrati.

## Azioni Eseguite

### Deprecazione Metodi in AppRepository

I seguenti metodi sono stati marcati come `@Deprecated` in `AppRepository.kt`:

1. `val currentProbe: Flow<ProbeConfig?>` → Usa `ProbeConfigDao.getSingleProbe()`
2. `fun observeProbeStatus(probe: ProbeConfig): Flow<Boolean>` → Usa `ProbeStatusRepository.observeProbeStatus(probe)`
3. `fun observeAllProbesWithStatus(): Flow<List<ProbeStatusInfo>>` → Usa `ProbeStatusRepository.observeAllProbesWithStatus()`
4. `suspend fun checkProbeConnection(probe: ProbeConfig): ProbeCheckResult` → Usa `ProbeConnectivityRepository.checkProbeConnection(probe)`

## Verifica Finale

### Ricerca Riferimenti Residui

```bash
# Verifica ViewModel migrati
grep -r "AppRepository" app/src/main/java/com/app/miklink/ui/dashboard/
grep -r "AppRepository" app/src/main/java/com/app/miklink/ui/probe/
grep -r "AppRepository" app/src/main/java/com/app/miklink/ui/test/
```

**Risultato:** ✅ Nessun riferimento trovato nei ViewModel migrati

### Note

- `AppRepository` può ancora essere utilizzato da altre feature non ancora migrate (se presenti)
- I metodi deprecati rimangono disponibili per compatibilità durante la transizione
- La rimozione completa di `AppRepository` sarà oggetto di future EPIC

