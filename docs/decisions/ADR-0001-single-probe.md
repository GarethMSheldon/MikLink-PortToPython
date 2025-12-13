# ADR-0001 — Sonda unica (rimozione probeId)

- **Status:** Accepted
- **Data:** 2025-12-13

## Contesto

In una prima fase l'app prevedeva più sonde (da qui la presenza storica di `probeId` in DB, UI routes e report).
La direzione attuale è semplificare: **una sola sonda configurabile**.

## Decisione

- L'app supporta **una sola sonda**.
- `probeId` è considerato **legacy** e va rimosso completamente:
  - da DB schema
  - da UI navigation/routes
  - dai repository contracts e dagli use case

## Conseguenze

- Le schermate “probe list” diventano “probe config” (singola configurazione).
- Il repository della sonda espone semantica “getSingle()/upsertSingle()” invece di CRUD multi-record.
- Ogni riferimento a `probeId` rimane solo come compatibilità temporanea durante migrazione (da tracciare in `DISCREPANCIES.md`).
