# S7-B - Matrice Dipendenze ViewModel Migrati

**Data Audit:** 2025-01-XX  
**Comando eseguito:** Analisi manuale file ViewModel

## ViewModel Analizzati

### 1. DashboardViewModel
**File:** `app/src/main/java/com/app/miklink/ui/dashboard/DashboardViewModel.kt`

**Dipendenze Iniettate (Constructor):**
- `clientDao: ClientDao`
- `testProfileDao: TestProfileDao`
- `reportDao: ReportDao`
- `probeConfigDao: ProbeConfigDao` ✅ **NUOVO** (aggiunto in S7)
- `probeStatusRepository: ProbeStatusRepository` ✅ **NUOVO** (aggiunto in S7)
- `userPreferencesRepository: UserPreferencesRepository`

**Metodi Usati:**
- `probeConfigDao.getSingleProbe()` - Linea 37 (sostituisce `repository.currentProbe`)
- `probeStatusRepository.observeProbeStatus(probe)` - Linea 53 (sostituisce `repository.observeProbeStatus(probe)`)

**AppRepository:**
- ✅ **RIMOSSO** - Nessun riferimento trovato nel file
- ✅ **MIGRATO** - Usa `ProbeConfigDao` e `ProbeStatusRepository` invece

---

### 2. ProbeEditViewModel
**File:** `app/src/main/java/com/app/miklink/ui/probe/ProbeEditViewModel.kt`

**Dipendenze Iniettate (Constructor):**
- `probeConfigDao: ProbeConfigDao`
- `probeConnectivityRepository: ProbeConnectivityRepository` ✅ **NUOVO** (aggiunto in S7)
- `savedStateHandle: SavedStateHandle`

**Metodi Usati:**
- `probeConfigDao.getSingleProbe()` - Linea 64 (caricamento sonda)
- `probeConfigDao.getProbeById(id)` - Linea 99 (caricamento per editing)
- `probeConfigDao.upsertSingle(probe)` - Linea 142 (salvataggio)
- `probeConnectivityRepository.checkProbeConnection(tempProbe)` - Linea 114 (sostituisce `repository.checkProbeConnection(tempProbe)`)

**AppRepository:**
- ✅ **RIMOSSO** - Nessun riferimento trovato nel file
- ✅ **MIGRATO** - Usa `ProbeConnectivityRepository` invece

---

### 3. ProbeListViewModel
**File:** `app/src/main/java/com/app/miklink/ui/probe/ProbeListViewModel.kt`

**Dipendenze Iniettate (Constructor):**
- `probeStatusRepository: ProbeStatusRepository` ✅ **NUOVO** (aggiunto in S7)

**Metodi Usati:**
- `probeStatusRepository.observeAllProbesWithStatus()` - Linea 18 (sostituisce `repository.observeAllProbesWithStatus()`)

**AppRepository:**
- ✅ **RIMOSSO** - Nessun riferimento trovato nel file
- ✅ **MIGRATO** - Usa `ProbeStatusRepository` invece

---

### 4. TestViewModel
**File:** `app/src/main/java/com/app/miklink/ui/test/TestViewModel.kt`

**Dipendenze Iniettate (Constructor):**
- `savedStateHandle: SavedStateHandle`
- `clientDao: ClientDao`
- `probeDao: ProbeConfigDao`
- `profileDao: TestProfileDao`
- `reportDao: ReportDao`
- `moshi: Moshi`
- `runTestUseCase: RunTestUseCase`

**Metodi Usati:**
- Nessun metodo AppRepository utilizzato (dipendenza già non utilizzata)

**AppRepository:**
- ✅ **RIMOSSO** - Nessun riferimento trovato nel file (dipendenza già non utilizzata prima di S7)

---

## Riepilogo

| ViewModel | AppRepository Prima | AppRepository Dopo | Stato |
|-----------|---------------------|-------------------|-------|
| DashboardViewModel | ✅ Usato (`currentProbe`, `observeProbeStatus`) | ❌ Rimosso | ✅ Migrato |
| ProbeEditViewModel | ✅ Usato (`checkProbeConnection`) | ❌ Rimosso | ✅ Migrato |
| ProbeListViewModel | ✅ Usato (`observeAllProbesWithStatus`) | ❌ Rimosso | ✅ Migrato |
| TestViewModel | ⚠️ Presente ma non utilizzato | ❌ Rimosso | ✅ Pulito |

## Verifica Finale

**Comando eseguito:**
```powershell
Get-ChildItem -Path "app\src\main\java\com\app\miklink\ui\dashboard","app\src\main\java\com\app\miklink\ui\probe","app\src\main\java\com\app\miklink\ui\test" -Recurse -File | Select-String -Pattern "AppRepository"
```

**Risultato:** ✅ **Nessun riferimento trovato** nei ViewModel migrati

**Conclusione:** Tutti i ViewModel target di S7 sono stati migrati con successo e non dipendono più da AppRepository.

