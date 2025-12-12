# S8.1 - Audit Deterministico: Dove viene ancora usato AppRepository

**Data Audit:** 2025-12-12  
**EPIC:** S8 - Sunset definitivo di AppRepository  
**Scope:** `app/src/main/java/**` (solo sorgenti main, no test)

## Metodologia

1. Ricerca testuale su sorgenti main:
   - Pattern: `AppRepository`
   - Pattern: `@Inject constructor(... AppRepository`
   - Pattern: `import .*AppRepository`
   - Pattern: `: AppRepository` (type annotation)
   - Pattern: `AppRepository)` (constructor parameter)

2. Analisi dettagliata di ogni file per identificare:
   - Dove viene iniettato AppRepository
   - Quali metodi vengono chiamati
   - Feature impattata (UI, data, domain)
   - Se è una dipendenza non usata (da rimuovere)

## File Trovati con Riferimenti ad AppRepository

### 1. Interfaccia Bridge (Core)
**File:** `app/src/main/java/com/app/miklink/core/data/repository/AppRepository.kt`

**Tipo:** Interfaccia bridge deprecata  
**Stato:** ✅ Legittimo - Interfaccia da rimuovere in S8.5

**Metodi Definiti (tutti deprecati):**
- `val currentProbe: Flow<ProbeConfig?>` - Deprecato S7 → Usa `ProbeConfigDao.getSingleProbe()`
- `suspend fun applyClientNetworkConfig(...)` - Deprecato S6 → Usa `NetworkConfigRepository.applyClientNetworkConfig()`
- `suspend fun runCableTest(...)` - Deprecato S5 → Usa `RunTestUseCase + CableTestStep`
- `suspend fun getLinkStatus(...)` - Deprecato S5 → Usa `RunTestUseCase + LinkStatusStep`
- `suspend fun getNeighborsForInterface(...)` - Deprecato S5 → Usa `RunTestUseCase + NeighborDiscoveryStep`
- `suspend fun resolveTargetIp(...)` - Deprecato S6 → Usa `PingTargetResolver.resolve()`
- `suspend fun runPing(...)` - Deprecato S5 → Usa `RunTestUseCase + PingStep`
- `suspend fun runSpeedTest(...)` - Deprecato S5 → Usa `RunTestUseCase + SpeedTestStep`
- `fun observeAllProbesWithStatus()` - Deprecato S7 → Usa `ProbeStatusRepository.observeAllProbesWithStatus()`
- `fun observeProbeStatus(probe)` - Deprecato S7 → Usa `ProbeStatusRepository.observeProbeStatus(probe)`
- `suspend fun checkProbeConnection(probe)` - Deprecato S7 → Usa `ProbeConnectivityRepository.checkProbeConnection(probe)`

**Classificazione:** Interfaccia bridge legacy - da rimuovere completamente

---

### 2. Implementazione Legacy (Data)
**File:** `app/src/main/java/com/app/miklink/data/repository/AppRepository.kt`

**Tipo:** Classe implementazione legacy `AppRepository_legacy`  
**Stato:** ✅ Legittimo - Implementazione da rimuovere in S8.5

**Implementa:** `com.app.miklink.core.data.repository.AppRepository`

**Metodi Implementati:**
- Tutti i metodi dell'interfaccia `AppRepository` (tutti deprecati)
- `suspend fun saveProbe(probe: ProbeConfig)` - Metodo interno non esposto nell'interfaccia
  - **Uso:** Verificato - NON usato da nessun componente esterno
  - **Nota:** Metodo di convenienza che delega a `probeConfigDao.upsertSingle(probe)`

**Dipendenze:**
- `Context` (ApplicationContext)
- `ClientDao`
- `ProbeConfigDao`
- `TestProfileDao`
- `ReportDao`
- `MikroTikServiceFactory`
- `RouteManager`
- `UserPreferencesRepository`

**Classificazione:** Implementazione legacy - da rimuovere completamente

---

### 3. Dependency Injection Module
**File:** `app/src/main/java/com/app/miklink/di/RepositoryModule.kt`

**Tipo:** Provider DI per AppRepository legacy  
**Stato:** ⚠️ Da rimuovere in S8.5

**Provider Definiti:**

1. **`provideAppRepositoryLegacy`** (linee 116-136)
   - **Tipo:** `@Provides @Singleton`
   - **Ritorna:** `com.app.miklink.data.repository.AppRepository_legacy`
   - **Dipendenze:** Context, ClientDao, ProbeConfigDao, TestProfileDao, ReportDao, MikroTikServiceFactory, RouteManager, UserPreferencesRepository
   - **Uso:** Crea istanza di `AppRepository_legacy`

2. **`provideAppRepositoryBridge`** (linee 138-140)
   - **Tipo:** `@Provides @Singleton`
   - **Ritorna:** `com.app.miklink.core.data.repository.AppRepository`
   - **Dipendenze:** `AppRepository_legacy`
   - **Uso:** Bridge che espone l'interfaccia `AppRepository` delegando a `AppRepository_legacy`

**Classificazione:** Provider DI legacy - da rimuovere completamente

---

### 4. Riferimenti Storici (Commenti KDoc)
**File trovati con riferimenti in commenti:**

1. **`app/src/main/java/com/app/miklink/core/data/repository/test/NetworkConfigRepository.kt`**
   - Linea 11: Commento KDoc che menziona "senza dipendere da AppRepository (EPIC S6)"
   - **Stato:** ✅ Legittimo - Solo commento storico

2. **`app/src/main/java/com/app/miklink/data/repositoryimpl/NetworkConfigRepositoryImpl.kt`**
   - Linee 19-20: Commenti KDoc che menzionano AppRepository
   - **Stato:** ✅ Legittimo - Solo commenti storici

3. **`app/src/main/java/com/app/miklink/core/data/repository/test/PingTargetResolver.kt`**
   - Linea 8: Commento KDoc che menziona "Temporary bridge used to resolve ping targets (e.g. DHCP gateway) outside AppRepository"
   - **Stato:** ✅ Legittimo - Solo commento storico

4. **`app/src/main/java/com/app/miklink/data/repositoryimpl/mikrotik/MikroTikTestRepositoryImpl.kt`**
   - Linea 22: Commento KDoc che menziona "Replica la logica di AppRepository_legacy"
   - **Stato:** ✅ Legittimo - Solo commento storico

**Classificazione:** Commenti storici - possono essere aggiornati ma non bloccanti

---

## Verifica ViewModel e Componenti UI

### ViewModel Verificati

| ViewModel | AppRepository Prima S7 | AppRepository Dopo S7 | Repository Usati Ora | Stato |
|-----------|------------------------|----------------------|---------------------|-------|
| DashboardViewModel | ✅ Usato | ❌ Rimosso | `ProbeConfigDao`, `ProbeStatusRepository` | ✅ Migrato |
| ProbeEditViewModel | ✅ Usato | ❌ Rimosso | `ProbeConfigDao`, `ProbeConnectivityRepository` | ✅ Migrato |
| ProbeListViewModel | ✅ Usato | ❌ Rimosso | `ProbeStatusRepository` | ✅ Migrato |
| TestViewModel | ⚠️ Presente non usato | ❌ Rimosso | `RunTestUseCase` | ✅ Pulito |

**Conclusione:** ✅ Nessun ViewModel usa più AppRepository. Tutti migrati in S7.

---

## Verifica UseCase e Domain Layer

**Ricerca eseguita:** Pattern `AppRepository` in `app/src/main/java/com/app/miklink/core/domain/**`

**Risultato:** ✅ Nessun riferimento trovato

**Conclusione:** ✅ Nessun UseCase o componente Domain Layer usa AppRepository.

---

## Verifica Altri Componenti

**Ricerca eseguita:** Pattern `AppRepository` in tutti i file `app/src/main/java/**`

**Risultato:** Solo i file già elencati sopra

**Conclusione:** ✅ Nessun altro componente usa AppRepository.

---

## Riepilogo Metodi AppRepository

### Metodi Attualmente Utilizzati

**NESSUNO** - Tutti i metodi sono deprecati e migrati a repository dedicati.

### Metodi NON Utilizzati (Deprecati e Migrati)

| Metodo AppRepository | Repository/UseCase Sostitutivo | EPIC Migrazione |
|----------------------|-------------------------------|-----------------|
| `currentProbe` | `ProbeConfigDao.getSingleProbe()` | S7 |
| `observeProbeStatus(probe)` | `ProbeStatusRepository.observeProbeStatus(probe)` | S7 |
| `observeAllProbesWithStatus()` | `ProbeStatusRepository.observeAllProbesWithStatus()` | S7 |
| `checkProbeConnection(probe)` | `ProbeConnectivityRepository.checkProbeConnection(probe)` | S7 |
| `applyClientNetworkConfig(...)` | `NetworkConfigRepository.applyClientNetworkConfig(...)` | S6 |
| `runCableTest(...)` | `RunTestUseCase + CableTestStep` | S5 |
| `getLinkStatus(...)` | `RunTestUseCase + LinkStatusStep` | S5 |
| `getNeighborsForInterface(...)` | `RunTestUseCase + NeighborDiscoveryStep` | S5 |
| `runPing(...)` | `RunTestUseCase + PingStep` | S5 |
| `runSpeedTest(...)` | `RunTestUseCase + SpeedTestStep` | S5 |
| `resolveTargetIp(...)` | `PingTargetResolver.resolve(...)` | S6 |

### Metodo Interno Non Esposto

| Metodo | Uso | Note |
|--------|-----|------|
| `saveProbe(probe)` | ❌ NON usato | Metodo interno di convenienza, non esposto nell'interfaccia |

---

## Classificazione per Responsabilità

### 1. Interfaccia Bridge (Core)
**File:** `core/data/repository/AppRepository.kt`  
**Stato:** Da rimuovere completamente  
**Responsabilità:** Interfaccia bridge deprecata, tutti i metodi migrati

### 2. Implementazione Legacy (Data)
**File:** `data/repository/AppRepository.kt` (classe `AppRepository_legacy`)  
**Stato:** Da rimuovere completamente  
**Responsabilità:** Implementazione legacy, tutti i metodi migrati

### 3. Dependency Injection
**File:** `di/RepositoryModule.kt`  
**Stato:** Da rimuovere completamente  
**Responsabilità:** Provider DI per AppRepository legacy

### 4. Commenti Storici
**File:** Vari repository (NetworkConfigRepository, PingTargetResolver, ecc.)  
**Stato:** Opzionale - possono essere aggiornati ma non bloccanti  
**Responsabilità:** Solo commenti KDoc che menzionano AppRepository per contesto storico

---

## Dipendenze Indirette

**Ricerca eseguita:** Pattern `AppRepository` in helper, utility, extension, ecc.

**Risultato:** ✅ Nessuna dipendenza indiretta trovata

**Conclusione:** ✅ Nessun componente wrapper o helper usa AppRepository.

---

## Analisi Metodo `saveProbe`

**File:** `app/src/main/java/com/app/miklink/data/repository/AppRepository.kt`  
**Linea:** 437-439

**Definizione:**
```kotlin
suspend fun saveProbe(probe: ProbeConfig) {
    probeConfigDao.upsertSingle(probe)
}
```

**Uso Verificato:**
- ✅ NON usato da nessun ViewModel
- ✅ NON usato da nessun UseCase
- ✅ NON usato da nessun altro componente

**Conclusione:** Metodo interno di convenienza non esposto nell'interfaccia. Può essere rimosso insieme a `AppRepository_legacy`.

---

## Prossimi Passi (S8.2-S8.5)

### File da Rimuovere

1. **`app/src/main/java/com/app/miklink/core/data/repository/AppRepository.kt`**
   - Interfaccia bridge deprecata
   - Tutti i metodi migrati

2. **`app/src/main/java/com/app/miklink/data/repository/AppRepository.kt`**
   - Implementazione legacy `AppRepository_legacy`
   - Tutti i metodi migrati

### Provider DI da Rimuovere

**File:** `app/src/main/java/com/app/miklink/di/RepositoryModule.kt`

1. **`provideAppRepositoryLegacy`** (linee 116-136)
2. **`provideAppRepositoryBridge`** (linee 138-140)

### Commenti Storici da Aggiornare (Opzionale)

- `NetworkConfigRepository.kt` - Commento KDoc linea 11
- `NetworkConfigRepositoryImpl.kt` - Commenti KDoc linee 19-20
- `PingTargetResolver.kt` - Commento KDoc linea 8
- `MikroTikTestRepositoryImpl.kt` - Commento KDoc linea 22

---

## Acceptance Criteria S8.1

✅ **Audit completato:**
- Tutti i file che contengono riferimenti ad AppRepository identificati
- Tutti i metodi chiamati documentati con stato migrazione
- Classificazione per responsabilità completata
- Verifica ViewModel completata (nessun uso trovato)
- Verifica UseCase/Domain completata (nessun uso trovato)
- Verifica altri componenti completata (nessun uso trovato)
- Analisi metodo `saveProbe` completata (non usato)

✅ **Pronto per S8.2:**
- Nessun repository target da creare (tutti già creati in S5-S7)
- File da rimuovere identificati
- Provider DI da rimuovere identificati
- Commenti storici identificati (opzionali)

---

## Note Finali

**Stato Attuale:**
- ✅ Tutti i ViewModel sono stati migrati in S7
- ✅ Tutti i metodi AppRepository sono stati migrati a repository dedicati
- ✅ Nessun componente attivo usa AppRepository
- ✅ AppRepository esiste solo come interfaccia bridge e implementazione legacy

**Conclusione:**
AppRepository può essere rimosso completamente senza impattare funzionalità esistenti. Tutti i metodi sono già migrati e nessun componente dipende più da AppRepository.

