# DISCREPANCIES (Docs vs Codice)

Questo file serve per tracciare **evidenze** (non proposte) quando la documentazione non riflette lo stato reale della codebase o viceversa.

## Regole

- Una discrepanza = un blocco.
- Nessuna proposta di fix qui (quelle vanno in issue/epic).
- Includere sempre path e simboli (classi/funzioni) coinvolti.

---

## Discrepanze note (iniziali)

### 1) UI dipende da Room (DAO/entity)

- **Doc dice:** `ui/**` non importa `core.data.local.room.*`
- **Codice mostra:** diversi file in `ui/**` importano `ClientDao`, `ReportDao`, `ProbeConfig`, `TestProfile`, ecc.
- **Impatto:** alto (rompe layering)

Esempi (non esaustivi):
- `app/src/main/java/com/app/miklink/ui/client/ClientEditViewModel.kt`
- `app/src/main/java/com/app/miklink/ui/history/HistoryViewModel.kt`

### 2) Domain non è ancora “puro”

- **Doc dice:** `core/domain/**` non importa Room/Retrofit/Moshi
- **Codice mostra:** import da Room model e/o Moshi/DTO in alcuni file domain/usecase
- **Impatto:** alto

Esempi:
- `core/domain/test/model/TestExecutionContext.kt`
- `core/domain/usecase/test/RunTestUseCaseImpl.kt`

### 3) probeId ancora presente nel flow UI

- **Doc/ADR dice:** sonda unica, `probeId` legacy
- **Codice mostra:** route/args ancora includono `probeId`
- **Impatto:** medio/alto

Esempio:
- `ui/NavGraph.kt` (route `test_execution/{clientId}/{probeId}/{profileId}/{socketName}`)

### 4) PDF dipende da UI e Room entity

- **Doc dice:** PDF dovrebbe consumare modelli normalizzati di dominio
- **Codice mostra:** `PdfGeneratorIText` importa `ui.history.model.ParsedResults` e entity Room v1
- **Impatto:** medio

Esempio:
- `core/data/pdf/impl/PdfGeneratorIText.kt`

---

## Discrepanze aperte (template)

### X) [DOC] <file.md>#<sezione>
- **Doc dice:** ...
- **Codice mostra:** (path + simbolo)
- **Impatto:** basso/medio/alto
- **Note:** ...
