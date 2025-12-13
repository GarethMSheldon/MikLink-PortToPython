# Database

## Stato attuale (Room v1)

- Database: `com.app.miklink.core.data.local.room.v1.AppDatabase`
- Nome file DB: `miklink-db`
- Versione Room: `13`
- `exportSchema = true`

La posizione dello schema è configurata nei Gradle args:

- `room.schemaLocation = app/schemas` (annotation processor)
- `room.schemaLocation = app/schemas` (KSP)

> Buona pratica: versionare la cartella `schemas/` nel VCS (ma non includerla nel packaging dell'app).

### Entities

**Client** (`clients`)

Campi principali (v1):
- `clientId: Long` (PK autogen)
- `companyName: String` (indicizzato)
- `location: String?`
- `notes: String?`
- `networkMode: String` (es. DHCP / Static)
- `staticIp/staticSubnet/staticGateway` (legacy)
- `staticCidr: String?` (preferito)
- Socket ID formatting:
  - `socketPrefix`, `socketSuffix`, `socketSeparator`, `socketNumberPadding`, `nextIdNumber`
- Legacy da rimuovere: `lastFloor`, `lastRoom`
- Speed test (server):
  - `speedTestServerAddress`, `speedTestServerUser`, `speedTestServerPassword`

**ProbeConfig** (`probe_config`)

- `probeId: Long` (PK autogen) — **legacy**: target = sonda unica senza probeId
- `ipAddress`, `username`, `password`
- `testInterface`
- `isOnline`
- `modelName`
- `tdrSupported` (cache)
- `isHttps` (toggle UI: http/https)

**TestProfile** (`test_profiles`)

- `profileId`, `profileName`, `profileDescription`
- flags: `runTdr`, `runLinkStatus`, `runLldp`, `runPing`, `runSpeedTest`
- ping targets: `pingTarget1..3`, `pingCount`

**Report** (`test_reports`)

- `reportId`
- `clientId` (nullable)
- `timestamp`
- `socketName`, `notes`
- `probeName`, `profileName` (stringhe)
- `overallStatus`
- `resultsJson` (payload serializzato)

### Seed iniziale

Nel `DatabaseModule` viene eseguito un seed di profili di default all'`onCreate()` del DB.

## Rebaseline DB (decisione di progetto)

Il progetto è in sviluppo e **non ci sono dati da preservare**: è accettabile rifare il DB “da zero” per:
- rimuovere campi legacy (`probeId`, `lastFloor`, `lastRoom`, ecc.)
- riallineare i layer (Room come infrastruttura in `data/**`)
- semplificare migrazioni (ripartire da versione 1)

**Importante:** prima di implementare la rebase dobbiamo fissare:
1) quali dati devono esistere in v1 (Client / Profile / Probe config / Report / altro)
2) quali relazioni e indici servono (query reali)
3) quali campi sono “fonte di verità” vs “cache”

Le discrepanze e scelte vanno tracciate in ADR e in `docs/DISCREPANCIES.md`.
