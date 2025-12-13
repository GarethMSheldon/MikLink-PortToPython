# Architettura

## Obiettivo

MikLink è un'app Android (Compose) che comunica con una **sonda MikroTik** via RouterOS REST API, esegue una suite di test (link, ping, TDR, LLDP, speed-test, ecc.) e salva/stampa uno storico (PDF).

Questa documentazione descrive l'architettura **target** (SOLID/Clean) che stiamo consolidando per arrivare a una v1 manutenibile.

## Tassonomia canonica (source of truth)

**Source of truth = contratti + modelli “puri” in `core/`, implementazioni in `data/`, wiring in `di/`, UI in `ui/`.**

### Layer e responsabilità

- `core/domain/**`
  - Modelli di dominio, regole di business, use case, contratti di step/policy.
  - **Puro**: non deve dipendere da Room/Retrofit/OkHttp/Android/UI.

- `core/data/**`
  - Contratti del data layer (repository interfaces, gateway/provider interfaces).
  - Tipi “neutri” (se servono) non legati ad Android/UI.

- `data/**`
  - Implementazioni: Room/Retrofit/OkHttp, repository impl, adapter, mapper.
  - È qui che avviene la trasformazione tra DTO/Entity ↔ Domain models.

- `di/**`
  - Wiring (Hilt). Nessuna logica.

- `ui/**`
  - ViewModel, UI state, Compose screens.
  - Dipende da `core/domain/**` (e, se necessario, dalle interfacce `core/data/repository/**`).

## Regole di dipendenza (hard rules)

- `core/domain/**` **NON** importa:
  - `android.*`
  - `androidx.*` (incluso `androidx.room.*`)
  - `retrofit2.*`, `okhttp3.*`
  - `com.app.miklink.ui.*`
  - `com.app.miklink.core.data.local.*` / `core.data.remote.*` (impl-specific)

- `ui/**` **NON** importa:
  - `core.data.local.room.*.dao.*`
  - `core.data.local.room.*.model.*`
  - `core.data.remote.*`
  - `data.repositoryimpl.*`

- `data/**` **NON** importa `ui/**`.

## Data flow (semplificato)

1. UI (screen) → ViewModel
2. ViewModel → UseCase (domain) **oppure** Repository interface (core/data) in casi CRUD “thin”
3. UseCase/Repo → `data/**` (Room/Retrofit) → mapper → Domain models
4. Domain output → UI state (mapper in `ui/**`)
5. Export PDF: dovrebbe consumare **modelli normalizzati di dominio**, non entity Room e non classi UI.

## Stato attuale

La codebase è in migrazione: alcune dipendenze violano ancora queste regole.
Le discrepanze note vanno tracciate in `docs/DISCREPANCIES.md`.
