# Investigazione Timeout Cable-Test TDR

**Data inizio:** 2025-11-16  
**Stato:** IN CORSO - STEP 1 Completato  
**Priorità:** CRITICA

---

## 📋 Problema Segnalato

**Sintomo:** La funzionalità cable-test TDR mostra timeout nell'UI.

**Incertezze:**
- Non è chiaro se il comando viene effettivamente eseguito sul router
- Non si sa se il problema è un timeout reale o un errore API non gestito
- Pattern API (sincrono vs asincrono) non verificato su hardware reale

---

## 🔍 Analisi Preliminare

### Codice Esistente (Prima delle modifiche)

**AppRepository.kt - runCableTest():**
```kotlin
suspend fun runCableTest(probe: ProbeConfig, interfaceName: String): UiState<CableTestResult> = safeApiCall {
    val api = buildServiceFor(probe)
    val results = api.runCableTest(CableTestRequest(interfaceName))
    results.lastOrNull() ?: throw IllegalStateException("No cable test results returned")
}
```

**Problemi identificati:**
1. ❌ Nessun logging dettagliato
2. ❌ Gestione errori generica (`safeApiCall` cattura tutto)
3. ❌ Non distingue timeout da altri errori
4. ❌ Non gestisce errori HTTP specifici (400, 500)
5. ❌ UI mostra solo "FAIL" senza dettagli

### Timeout Configurati

**NetworkModule.kt:**
```kotlin
.connectTimeout(30, TimeUnit.SECONDS)   // Tempo per stabilire connessione TCP
.writeTimeout(30, TimeUnit.SECONDS)     // Tempo per inviare richiesta
.readTimeout(60, TimeUnit.SECONDS)      // Tempo per leggere risposta
.callTimeout(60, TimeUnit.SECONDS)      // Tempo totale chiamata
```

**Analisi:** Timeout di 60s dovrebbe essere sufficiente per la maggior parte dei cable-test, MA:
- Alcuni modelli potrebbero richiedere più tempo
- Se l'API è asincrona, il timeout non è rilevante

---

## ✅ STEP 1: Logging Dettagliato - COMPLETATO

**Data:** 2025-11-16  
**Commit:** (in attesa di commit)

### Modifiche Implementate

#### 1. AppRepository.kt - runCableTest()

**Aggiunto:**
- ✅ Logging completo con tag `TDR_DEBUG`
- ✅ Misurazione tempo di risposta (elapsed time)
- ✅ Gestione specifica per `SocketTimeoutException`
- ✅ Gestione specifica per `HttpException` (400, 500, altri)
- ✅ Logging del response body in caso di errore
- ✅ Logging dettagliato dei cable pairs nel risultato

**Codice chiave:**
```kotlin
android.util.Log.d("TDR_DEBUG", "=== Cable-Test Request Start ===")
val startTime = System.currentTimeMillis()
// ...chiamata API...
val elapsed = System.currentTimeMillis() - startTime
android.util.Log.d("TDR_DEBUG", "Response received after ${elapsed}ms")
```

#### 2. TestViewModel.kt - Gestione TDR

**Miglioramenti UI:**
- ✅ Stato intermedio "Avvio test in corso..." con nota "Il test può richiedere 10-30 secondi"
- ✅ Analisi automatica dei cable pairs (PASS se tutti "open"/"ok", WARN altrimenti)
- ✅ Dettagli estesi: numero coppie, stato per ogni coppia (pair, status, length)
- ✅ Distinzione tra errori fatali (hardware non supportato → SKIP) e temporanei (→ FAIL)
- ✅ Non bloccare il test se TDR fallisce per incompatibilità hardware

**Codice chiave:**
```kotlin
// Stato iniziale
upsertSection(
    TestSection(
        status = "INFO",
        details = listOf(
            TestDetail("Stato", "Avvio test in corso..."),
            TestDetail("Nota", "Il test può richiedere 10-30 secondi")
        )
    )
)

// Analisi risultati
val allPairsOk = tdrResult.data.cablePairs.all { 
    val status = it["status"]
    status == "open" || status == "ok"
}
val status = if (allPairsOk) "PASS" else "WARN"

// Gestione errori intelligente
val isFatal = tdrResult.message.contains("non supportato", ignoreCase = true)
val status = if (isFatal) "SKIP" else "FAIL"
if (!isFatal) { overallStatus = "FAIL" }
```

### Validazione

**Test da eseguire:**
1. ✅ Compilazione: SUCCESSO (solo warning non bloccanti)
2. ⏳ Build APK: DA ESEGUIRE
3. ⏳ Test su hardware reale: DA ESEGUIRE

---

## ⏳ STEP 2: Test Manuale API - COMPLETATO ✅

**Obiettivo:** Determinare il pattern API (sincrono vs asincrono)

### Script Creato

**File:** `test_cable_test_api.ps1`

**Funzionalità:**
- Esegue chiamata POST a `/rest/interface/ethernet/cable-test`
- Misura tempo di risposta
- Analizza automaticamente il pattern in base al tempo:
  - < 2s → Probabile asincrono
  - 2-15s → Sincrono veloce (OK)
  - > 15s → Sincrono lento (possibile timeout)

**Utilizzo:**
```powershell
# Modificare le variabili nel file:
$RouterIP = "192.168.1.20"
$Username = "dot"
$Password = ""
$Interface = "ether4"

# Eseguire
.\test_cable_test_api.ps1
```

### ✅ Risultato Test Eseguito

**Data:** 2025-11-16  
**Router:** 192.168.1.20  
**Interface:** ether4  

**Risposta ricevuta:**
```json
{"detail":"Session closed","error":400,"message":"Bad Request"}
```

**Tempo:** 60.04 secondi (esattamente 60s → timeout server)

### 🎯 Diagnosi

**SCENARIO IDENTIFICATO:** Timeout "Session closed" - Identico al problema del traceroute

**Causa:** MikroTik REST API chiude la sessione HTTP dopo 60 secondi se il comando è ancora in esecuzione. Il cable-test continua sul router ma la connessione viene chiusa prematuramente.

**Soluzione:** Aggiungere parametro `duration` al request (stesso fix applicato per traceroute)

### ✅ SOLUZIONE IMPLEMENTATA

**File modificato:** `MikroTikApiService.kt`

**Prima:**
```kotlin
data class CableTestRequest(@Json(name = "numbers") val numbers: String)
```

**Dopo:**
```kotlin
data class CableTestRequest(
    @Json(name = "numbers") val numbers: String,
    val duration: String = "40s"  // ESSENZIALE per evitare timeout "Session closed"
)
```

**Benefici:**
- ✅ Limita il tempo totale di esecuzione a 40 secondi
- ✅ Forza MikroTik a ritornare il risultato entro il timeout
- ✅ Evita "Session closed" error
- ✅ Soluzione già validata su traceroute (vedi `API_TESTING_2025-11-15.md`)

---

## ✅ STEP 3: Implementazione Soluzione - COMPLETATO

**Soluzione scelta:** Parametro `duration` (approccio semplice e già validato)

**Modifiche:**
1. ✅ Aggiunto `duration: String = "40s"` a `CableTestRequest`
2. ✅ Aggiornato script di test per includere `duration`
3. ✅ Nessuna modifica a `AppRepository` necessaria (già gestisce timeout correttamente)

**Tempo implementazione:** 15 minuti

**Alternative scartate:**
- ❌ SOLUZIONE A (timeout aumentato): Non risolve il problema "Session closed"
- ❌ SOLUZIONE B (polling asincrono): Eccessivamente complessa, non necessaria

---

## 📊 Checklist Completa

### STEP 1: Logging ✅
- [x] Aggiungere logging dettagliato in `AppRepository.runCableTest()`
- [x] Gestire `SocketTimeoutException` separatamente
- [x] Gestire `HttpException` con codici specifici
- [x] Migliorare UI feedback in `TestViewModel.kt`
- [x] Compilazione senza errori

### STEP 2: Test Manuale ✅
- [x] Creare script PowerShell per test API
- [x] Eseguire test su router reale
- [x] Documentare pattern API rilevato: "Session closed" timeout
- [x] Documentare tempo di risposta effettivo: 60s esatti

### STEP 3: Implementazione Soluzione ✅
- [x] Identificato problema: identico a traceroute (Session closed)
- [x] Implementato parametro `duration` in `CableTestRequest`
- [x] Aggiornato script di test
- [x] Compilazione: DA VERIFICARE

### STEP 4: Validazione Finale ⏳
- [ ] Build APK
- [ ] Rieseguire script di test con duration (verifica fix)
- [ ] Test su app con cable-test TDR
- [ ] Verificare log `TDR_DEBUG` completi
- [ ] Verificare UI mostra dettagli cable pairs

---

## 📝 Log dei Test

### Test #1 - 2025-11-16 - Router MikroTik

**Configurazione:**
- Router: 192.168.1.20
- Username: dot
- Modello: [da verificare con system/resource]
- Interfaccia: ether4
- TDR Supportato: Sì (altrimenti non arriverebbe a timeout)

**Comando eseguito:**
```json
{
    "numbers": "ether4"
}
```

**Risultato:**
```json
{"detail":"Session closed","error":400,"message":"Bad Request"}
```

**Metriche:**
- Tempo risposta: 60.040828s
- HTTP Status: 400
- Time Connect: 0.001062s (connessione OK)

**Pattern rilevato:** Session Closed Timeout (identico a traceroute pre-fix)

**Diagnosi:**
- Il cable-test viene avviato sul router
- Il test impiega >60 secondi per completarsi
- MikroTik chiude la sessione HTTP dopo 60s
- Il client riceve errore 400 "Session closed"
- Il test potrebbe continuare sul router anche dopo la chiusura

**Azione:** ✅ Implementato parametro `duration: "40s"` in `CableTestRequest`

**Prossimo test:** Rieseguire con payload `{"numbers":"ether4","duration":"40s"}`

---

### Test #2 - [DA ESEGUIRE] - Validazione Fix

**Configurazione:**
- Router: 192.168.1.20
- Interfaccia: ether4

**Comando da eseguire:**
```json
{
    "numbers": "ether4",
    "duration": "40s"
}
```

**Risultato atteso:**
```json
[
  {
    "status": "open",
    "cable-pairs": [
      {"pair": "1-2", "status": "open", "length": "XXm"},
      {"pair": "3-6", "status": "open", "length": "XXm"},
      {"pair": "4-5", "status": "open", "length": "XXm"},
      {"pair": "7-8", "status": "open", "length": "XXm"}
    ]
  }
]
```

**Tempo atteso:** 20-40 secondi (entro il limite di duration)

**Comando per test:**
```powershell
.\test_cable_test_api.ps1
```

---

## 🎯 Prossimi Step Immediati

1. **ORA:** Eseguire build APK per verificare che le modifiche compilano
2. **ORA:** Eseguire `test_cable_test_api.ps1` su router reale
3. **DOPO:** In base ai risultati, implementare Soluzione A o B
4. **DOMANI:** Validazione finale su hardware

---

## 📚 Riferimenti

- **Piano completo:** `PIANO_RISOLUZIONE_TDR.md` (presentato all'utente)
- **Script test:** `test_cable_test_api.ps1`
- **Documentazione API MikroTik:** `/docs/API_VALIDATION.md`
- **Test precedenti:** `/docs/testing/API_TESTING_2025-11-15.md`

---

**Ultimo aggiornamento:** 2025-11-16 - STEP 1 completato, STEP 2 in corso

