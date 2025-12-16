# ADR-0009 — Reintroduce test execution logs (bounded, UI-only)

- **Status:** Accepted  
- **Data:** 2025-12-16  
- **Supersedes:** ADR-0005

## Contesto

L'epic "Reintroduce Show Logs in Test Execution UI" richiede di riportare i log di esecuzione per aiutare debug e field support, evitando però regressioni di sicurezza e performance che avevano portato alla rimozione in ADR-0005.

## Decisione

- Aggiungiamo `TestEvent.LogLine(val message: String)` nel dominio per trasportare log testuali insieme a progress/sections.
- I log sono sanitizzati in un unico punto (`LogSanitizer`): redazione di password/token/authorization e troncamento delle linee troppo lunghe.
- Il buffer è in-memory e limitato (`ExecutionLogBuffer` con `ExecutionLogsConfig.MAX_LINES` e marker di trimming); nessuna persistenza su DB o report JSON.
- La UI espone toggle in esecuzione e a test completato; le composable usano un unico `RawLogsPane` condiviso e semantics tag stabili per i test strumentali.

## Conseguenze

- ADR-0005 è superata: i log tornano ma restano confinati alla UI, sanitizzati e non persistenti.
- L'anti-drift è garantito da handling esaustivo di `TestEvent` e da test di buffer/sanitizer.
- Qualsiasi nuova sorgente di log deve passare dal sanitizer prima dell'emissione per evitare leakage di credenziali.
