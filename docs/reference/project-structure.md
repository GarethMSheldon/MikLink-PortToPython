# Struttura del progetto

Questa pagina descrive la struttura **attuale** dei package principali e la struttura **target** (quella su cui basare ogni nuovo codice).

## Moduli Gradle

- Un solo modulo applicativo: `app/`

## Package principali

Base package: `com.app.miklink`

- `core/`
  - `core/domain/**` — dominio puro (regole + use case + modelli)
  - `core/data/**` — contratti (repository/gateway/provider) e tipi neutri
  - `core/presentation/**` — contratti/pattern UI-agnostic (minimo, se serve)

- `data/`
  - `data/repositoryimpl/**` — implementazioni dei repository (es. Room, MikroTik)
  - (target) ulteriori adapter/mapper infra-only

- `di/`
  - Moduli Hilt (Database, Network, Repository, PDF, ecc.)

- `ui/`
  - Compose + ViewModel + state e mapper UI

## Cartelle presenti ma “in migrazione”

- `legacy/`  
- `domain/` (top-level, fuori dal canone `core/domain/**`)
- `feature/` (struttura parallela a `ui/`)

**Regola pratica:** nessun nuovo codice dovrebbe nascere qui; si migra verso i layer canonici e poi si elimina.

## Convenzioni

- **Domain models**: in `core/domain/**/model`
- **Use cases**: in `core/domain/usecase/**`
- **Repository interfaces**: in `core/data/repository/**`
- **Repository implementations**: in `data/repositoryimpl/**`
- **Room**: è considerata infrastruttura ⇒ deve vivere in `data/**` (target).
