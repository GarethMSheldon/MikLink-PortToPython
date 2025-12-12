# S7-G - Inventario Test S7

**Data Audit:** 2025-01-XX  
**Comando eseguito:** `./gradlew testDebugUnitTest` → salvato in `docs/migration/S7_tests_final.txt`

## Test Creati per S7

### 1. ProbeStatusRepositoryContractTest
**Path:** `app/src/test/java/com/app/miklink/core/data/repository/probe/ProbeStatusRepositoryContractTest.kt`  
**Package:** `com.app.miklink.core.data.repository.probe`

**Cosa Valida:**
- Comportamento di `observeProbeStatus()` quando la sonda è online
- Comportamento di `observeProbeStatus()` quando la sonda è offline
- Comportamento di `observeAllProbesWithStatus()` quando non ci sono sonde
- Comportamento di `observeAllProbesWithStatus()` con sonde online/offline miste

**Dipendenze Mockate:**
- `ProbeConfigDao` (mockk)
- `MikroTikServiceProvider` (mockk)
- `MikroTikApiService` (mockk)
- `UserPreferencesRepository` (mockk)

**Test Cases:**
1. `observeProbeStatus returns true when probe is online` - ✅ PASSED
2. `observeProbeStatus returns false when probe is offline` - ✅ PASSED
3. `observeAllProbesWithStatus returns empty list when no probes exist` - ✅ PASSED
4. `observeAllProbesWithStatus returns list with online status` - ✅ PASSED

**Compila:** ✅ Sì  
**Passa:** ✅ Sì (4/4 test passati)

---

### 2. ProbeConnectivityRepositoryContractTest
**Path:** `app/src/test/java/com/app/miklink/core/data/repository/probe/ProbeConnectivityRepositoryContractTest.kt`  
**Package:** `com.app.miklink.core.data.repository.probe`

**Cosa Valida:**
- Comportamento di `checkProbeConnection()` quando la connessione è riuscita
- Comportamento di `checkProbeConnection()` quando la connessione fallisce
- Comportamento di `checkProbeConnection()` quando board-name è mancante

**Dipendenze Mockate:**
- `Context` (mockk, relaxed)
- `MikroTikServiceProvider` (mockk)
- `MikroTikApiService` (mockk)

**Test Cases:**
1. `checkProbeConnection returns Success with boardName and interfaces` - ✅ PASSED
2. `checkProbeConnection returns Error when connection fails` - ✅ PASSED
3. `checkProbeConnection returns Success with Unknown Board when boardName is missing` - ✅ PASSED

**Compila:** ✅ Sì  
**Passa:** ✅ Sì (3/3 test passati)

---

## Risultato Test Finale

**Comando:** `./gradlew testDebugUnitTest`  
**Output:** `docs/migration/S7_tests_final.txt`

**Risultato:** ✅ **BUILD SUCCESSFUL**

**Test S7:**
- `ProbeStatusRepositoryContractTest` - 4 test PASSED
- `ProbeConnectivityRepositoryContractTest` - 3 test PASSED
- **Totale test S7:** 7 test, tutti PASSED

**Test Totali:**
- 33 test completati
- 7 test skipped (pre-esistenti, non S7)
- 0 test failed

**Note:**
- Nessun test `@Ignore` aggiunto per far passare i test
- Tutti i test S7 sono contract test con mock (no UI test)
- Verificano mapping null/errore, policy eccezioni, casi "probe offline" come richiesto

