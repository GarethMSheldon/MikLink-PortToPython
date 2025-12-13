# U1.6 - Risultato EPIC U1.6: Live Test Cards

## Contesto & Obiettivo
- EPIC U1.6 richiedeva che le card dei singoli step di certificazione venissero popolate e aggiornate in tempo reale durante l'esecuzione dei test.
- Il flusso precedente inviava i `TestSectionResult` solo al completamento del test, costringendo la UI a mostrare placeholder statici.

## Modifiche Principali
- **Dominio (`core/domain`)**
  - `TestEvent` espanso con `SectionsUpdated`, nuovo evento di progressione delle sezioni.
  - `RunTestUseCaseImpl` ora:
    - Costruisce snapshot iniziale delle sezioni (`PENDING` o `SKIP` se disabilitate/unsupported) tramite `buildInitialSections`.
    - Emmette `SectionsUpdated` prima di ogni step, portando la sezione in `RUNNING`, e dopo l'esito finale (`PASS/FAIL/SKIP`).
    - Popola i dettagli di skip usando `TestSkipReason.PROFILE_DISABLED` e `TestSkipReason.HARDWARE_UNSUPPORTED`.
- **UI / ViewModel (`ui/test`)**
  - `TestViewModel` recepisce `SectionsUpdated` aggiornando lo `StateFlow` `sections` mentre il test è in corso; i dati finali continuano ad arrivare da `TestEvent.Completed`.
- **Testing (`app/src/test`)**
  - Nuovo `RunTestUseCaseImplTest` copre l'ordine deterministico delle sezioni e la progressione `PENDING -> RUNNING -> {PASS|FAIL|SKIP}`.
  - Nuovo `TestViewModelTest` verifica l'aggiornamento progressivo delle card e la propagazione dell'outcome finale.
  - Aggiunta la rule condivisa `MainDispatcherRule` per pilotare `Dispatchers.Main` nei test coroutine.

## Stato Funzionale
- Le card in `TestInProgressView` ricevono ora aggiornamenti live e non dipendono più da workaround UI.
- Gli step disabilitati da profilo o hardware sono marcati `SKIP` già nello snapshot iniziale, garantendo coerenza tra dominio e UI.
- I log raw restano disponibili via toggle, senza interferire con la lista delle sezioni.

## Evidenze di Build & Test
- Baseline iniziale:
  - `docs/migration/U1_6_BASELINE_ksp.txt`
  - `docs/migration/U1_6_BASELINE_assemble.txt`
  - `docs/migration/U1_6_BASELINE_tests.txt`
- Verifiche finali post-implementazione:
  - `docs/migration/U1_6_ksp_final.txt`
  - `docs/migration/U1_6_assemble_final.txt`
  - `docs/migration/U1_6_tests_final.txt`

## Note & Warning
- I log mostrano i warning noti di Gradle/Kotlin (`sun.misc.Unsafe`, `SQLiteJDBCLoader`) già presenti prima della modifica; nessun impatto funzionale osservato.

## TODO / Follow-up
- Nessun TODO aperto per l'EPIC U1.6: il requisito "card live" è soddisfatto e coperto da test automatici.
