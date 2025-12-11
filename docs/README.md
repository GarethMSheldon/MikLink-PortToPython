# MikLink - Indice Documentazione

**Ultima Revisione**: 2025-11-15  
**Versione App**: 2.0

---

## 📐 Architettura e Design

### [ARCHITECTURE.md](ARCHITECTURE.md)
**Documentazione tecnica completa dell'architettura MikLink**

- Diagramma architetturale (Presentation → Domain → Data Layer)
- Schema Database Room v8 con tutte le entità
- Descrizione dettagliata di `Client`, `ProbeConfig`, `TestProfile`, `Report`
- Business rules e constraint per ogni entità
- Pattern MVVM + Repository

**Target**: Sviluppatori senior, architetti software

---

### [UX_UI_SPEC.md](UX_UI_SPEC.md)
**Design System completo (stile Ubiquiti)**

- Palette colori (primari, semantici, light/dark theme)
**MikLink** è l'app Android per il testing e la certificazione di infrastrutture di rete basate su MikroTik. È pensata per tecnici di campo e team di installazione: esegue test (TDR, Link Status, LLDP, Ping, Traceroute), produce report certificati (PDF) e mantiene uno storico dei test.

Questo repository contiene codice sorgente, test e documentazione tecnica. I contenuti della documentazione sono stati consolidati sotto `docs/` (README, ROADMAP, ARCHITECTURE, IMPLEMENTATION_SUMMARY).

**Struttura principale della documentazione (ora canonica in `docs/`):**
- `docs/README.md` - Overview e guida rapida (questo file)
- `docs/ROADMAP.md` - Roadmap, priorità e azioni raccomandate
- `docs/ARCHITECTURE.md` - Documentazione tecnica approfondita
- `docs/IMPLEMENTATION_SUMMARY.md` - Riepilogo delle modifiche, audit e report

---

## Quick Start

Prerequisiti:

Build and run (PowerShell):
```
.\gradlew clean build
.\gradlew installDebug
```
PDF generation: the project now uses a SOLID design for PDF output — a `PdfGenerator` interface with an iText implementation (`PdfGeneratorIText`) bound via Hilt. Legacy HTML/WebView templates were removed and replaced by the iText flow.

If you hit build issues related to PDF generation, check `app/src/main/java/com/app/miklink/data/pdf/PdfGeneratorIText.kt` and ensure CI reflects the latest changes. Local builds and unit tests are currently green.
Se la build fallisce nella fase KSP/compilazione (errori legati a `PdfGeneratorIText.kt`), risolvere il file indicato o usare la branch di emergenza che ripristina un wrapper `PdfGenerator`.

---

## Dove cercare le informazioni tecniche
- Architettura e database: `docs/ARCHITECTURE.md`
- Roadmap e azioni: `docs/ROADMAP.md`
- Audit / Implementation summary: `docs/IMPLEMENTATION_SUMMARY.md`

---

## Note sulla migrazione della documentazione
I contenuti dalle vecchie note, report e piani (es. `CODEBASE_ANALYSIS_REPORT.md`, `MASTER_PLAN_v2.0.md`, `API_VALIDATION.md`, `VALIDATION_REPORT_v2.0.md`, `DUPLICATES_CLEANUP.md`) sono stati consolidati in questi file in `docs/`. I file originali nella root e in `docs/archive/` sono stati rimossi per evitare duplicazione, dopo verifica che le informazioni siano state migrate.

Se qualcosa sembra mancare o desideri che includa o preservi specifiche sezioni dettagliate in appendice, dimmi quali e lo inserisco esplicitamente.
---


