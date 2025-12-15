# ADR-0004 — Socket ID Lite (formattazione deterministica)

- **Status:** Accepted  
- **Data:** 2025-12-14

## Contesto

Serve un socket-id leggibile e deterministico nei report, senza introdurre un sistema di template complesso.

## Decisione

Implementiamo una versione **Lite** basata su campi semplici nel `Client`:

- `socketPrefix` (String)
- `socketSeparator` (String)
- `socketNumberPadding` (Int)
- `socketSuffix` (String)
- `nextIdNumber` (Int)

### Formattazione

La formattazione è pure function nel dominio:

- `Client.socketNameFor(idNumber: Int): String`

Regola:

- `padded = idNumber` formattato con zero-padding in base a `socketNumberPadding`
- `socketName = prefix + separator + padded + separator + suffix`

> Nota: la funzione concatena sempre entrambi i separatori (anche se prefix/suffix sono vuoti).  
> Fonte: `core/domain/model/Client.kt`.

### Incremento `nextIdNumber`

- Il contatore si incrementa **solo** quando un report salvato ha `overallStatus == "PASS"` ed è salvato dal **flow di run-test**.
- L'incremento è applicato nel use case `SaveTestReportUseCase`; il repository Room rimane CRUD e non muta `Client`.
- Percorsi di duplicazione/import/restore devono usare il repository raw (senza incrementare).

## Conseguenze

- Comportamento deterministico e testabile.
- Nessun side-effect nascosto nei repository; le policy restano nel livello use case.
- Un eventuale “full template” resta fuori scope e richiede ADR dedicato.
