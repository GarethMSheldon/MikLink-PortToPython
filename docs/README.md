# MikLink — Documentazione

Questa cartella contiene **la documentazione “di riferimento”** per MikLink.
L'obiettivo è ridurre il drift tipico del vibe-coding: pochi documenti, aggiornati, con decisioni tracciate.

## Navigazione

- [Architettura](explanation/architecture.md)
- [V1 — Obiettivi e confini](explanation/v1-scope.md)
- [Struttura del progetto](reference/project-structure.md)
- [Database](reference/database.md)
- [MikroTik REST API](reference/mikrotik-rest-api.md)
- [Testing](reference/testing.md)
- [Fixtures](reference/fixtures.md)
- [Dipendenze](reference/dependencies.md)
- How-to:
  - [Aggiungere un golden test](howto/add-golden-test.md)
- Decisioni (ADR):
  - [ADR-0001 — Sonda unica](decisions/ADR-0001-single-probe.md)
  - [ADR-0002 — HTTP/HTTPS + trust-all](decisions/ADR-0002-https-toggle-trust-all.md)
- [Discrepanze Docs vs Codice](DISCREPANCIES.md)

## Come eseguire

### Build & test (da CLI)

```bash
./gradlew test
./gradlew connectedAndroidTest
```

### Info toolchain (dallo stato attuale della codebase)

- **AGP**: 8.13.1
- **Kotlin**: 2.1.0
- **compileSdk / targetSdk / minSdk**: 36 / 36 / 26

> Nota: i numeri qui sopra sono quelli presenti *oggi* nei file Gradle della repo.

## Regole anti-drift

- Le decisioni architetturali vivono in `docs/decisions/` (ADR).
- Le regole di layering stanno in `docs/explanation/architecture.md`.
- Se qualcosa non torna tra docs e codice: aggiornare `docs/DISCREPANCIES.md` (e poi sistemare).
