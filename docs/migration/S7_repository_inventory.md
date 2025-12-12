# S7-C/D - Inventario Repository S7

**Data Audit:** 2025-01-XX  
**Comando eseguito:** Analisi manuale file repository

## Repository Creati in S7

### 1. ProbeStatusRepository (Interfaccia)
**Path:** `app/src/main/java/com/app/miklink/core/data/repository/probe/ProbeStatusRepository.kt`  
**Package:** `com.app.miklink.core.data.repository.probe`  
**Tipo:** Interface

**Firme Funzioni:**
```kotlin
fun observeProbeStatus(probe: ProbeConfig): Flow<Boolean>
fun observeAllProbesWithStatus(): Flow<List<ProbeStatusInfo>>
```

**Dipendenze Principali (dichiarate in KDoc):**
- `ProbeConfig` (input)
- `ProbeStatusInfo` (output)
- `Flow` (Kotlin coroutines)

**KDoc:** ✅ Presente con Input/Output/Error policy

---

### 2. ProbeConnectivityRepository (Interfaccia)
**Path:** `app/src/main/java/com/app/miklink/core/data/repository/probe/ProbeConnectivityRepository.kt`  
**Package:** `com.app.miklink.core.data.repository.probe`  
**Tipo:** Interface

**Firme Funzioni:**
```kotlin
suspend fun checkProbeConnection(probe: ProbeConfig): ProbeCheckResult
```

**Dipendenze Principali (dichiarate in KDoc):**
- `ProbeConfig` (input)
- `ProbeCheckResult` (output)

**KDoc:** ✅ Presente con Input/Output/Error policy

---

### 3. ProbeStatusRepositoryImpl (Implementazione)
**Path:** `app/src/main/java/com/app/miklink/data/repositoryimpl/mikrotik/ProbeStatusRepositoryImpl.kt`  
**Package:** `com.app.miklink.data.repositoryimpl.mikrotik`  
**Tipo:** Class

**Dipendenze Iniettate (Constructor):**
- `probeConfigDao: ProbeConfigDao` (Room v1)
- `serviceProvider: MikroTikServiceProvider` (S6)
- `userPreferencesRepository: UserPreferencesRepository`

**Implementazioni:**
- `observeProbeStatus(probe: ProbeConfig): Flow<Boolean>` - Linee 30-47
- `observeAllProbesWithStatus(): Flow<List<ProbeStatusInfo>>` - Linee 49-69

**Note:**
- Usa `MikroTikServiceProvider.build(probe)` per costruire il service
- Usa `userPreferencesRepository.probePollingInterval` per polling interval
- Gestisce errori ritornando `false` (offline) senza propagare eccezioni

---

### 4. ProbeConnectivityRepositoryImpl (Implementazione)
**Path:** `app/src/main/java/com/app/miklink/data/repositoryimpl/mikrotik/ProbeConnectivityRepositoryImpl.kt`  
**Package:** `com.app.miklink.data.repositoryimpl.mikrotik`  
**Tipo:** Class

**Dipendenze Iniettate (Constructor):**
- `context: Context` (@ApplicationContext)
- `serviceProvider: MikroTikServiceProvider` (S6)

**Implementazioni:**
- `checkProbeConnection(probe: ProbeConfig): ProbeCheckResult` - Linee 25-43

**Note:**
- Usa `MikroTikServiceProvider.build(probe)` per costruire il service
- Chiama `api.getSystemResource()` e `api.getEthernetInterfaces()`
- Gestisce errori convertendoli in `ProbeCheckResult.Error`

---

## Binding DI (Hilt)

**File:** `app/src/main/java/com/app/miklink/di/RepositoryModule.kt`

**Binding Presenti:**
- Linea 101: `@Binds abstract fun bindProbeStatusRepository(impl: ProbeStatusRepositoryImpl): ProbeStatusRepository`
- Linea 105: `@Binds abstract fun bindProbeConnectivityRepository(impl: ProbeConnectivityRepositoryImpl): ProbeConnectivityRepository`

**Verifica:**
- ✅ Binding presenti e corretti
- ✅ Singleton scope applicato
- ✅ Nessun binding duplicato

---

## Verifica Esistenza File

| File | Path | Esiste | Package Corretto |
|------|------|--------|------------------|
| ProbeStatusRepository.kt | `core/data/repository/probe/` | ✅ Sì | ✅ Sì |
| ProbeConnectivityRepository.kt | `core/data/repository/probe/` | ✅ Sì | ✅ Sì |
| ProbeStatusRepositoryImpl.kt | `data/repositoryimpl/mikrotik/` | ✅ Sì | ✅ Sì |
| ProbeConnectivityRepositoryImpl.kt | `data/repositoryimpl/mikrotik/` | ✅ Sì | ✅ Sì |

---

## Conformità Architettura

✅ **Interfacce in core/** - Corretto  
✅ **Implementazioni in data/** - Corretto  
✅ **Uso Room v1** - Corretto (`ProbeConfigDao`)  
✅ **Uso MikroTikServiceProvider** - Corretto (S6)  
✅ **Nessuna dipendenza da AppRepository** - Corretto  
✅ **KDoc completo** - Presente con Input/Output/Error policy

