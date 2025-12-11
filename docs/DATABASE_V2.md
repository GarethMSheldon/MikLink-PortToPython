# MikLink — DATABASE_V2.md

Questo documento descrive la versione target del database (DB v2) pianificata per il refactor.

Nota: in questa EPIC A si crea solo la documentazione; nessuna modifica runtime ai model o migration viene applicata.

---

## Overview

DB v2 obiettivo: migliorare coerenza dei campi, deprecare campi legacy, aggiungere campi per gestire Socket ID template e stato incremenale in modo robusto.

Entities principali (mappate rispetto al codice attuale v1):
- Client
- ProbeConfig
- TestProfile
- Report
- LogEntry (esistente)

---

## Design decisions (explicit)

- `ProbeConfig.tdrSupported`: kept as a cached boolean in the DB for performance/UI use, but the authoritative logic for TDR capability resides in the domain component `TdrCapabilities` which derives support based on `modelName`/`board-name`.
- `Client.socketTemplateConfig`: stored as an optional `String?` (JSON) field on the `Client` entity in DB v2. The domain layer (`SocketTemplate`/`SocketIdGenerator`) interprets and generates socket IDs from this JSON. Existing socket-related fields will coexist for backward compatibility and to avoid breaking current behavior.
- `Report.resultsJson`: will remain a single JSON column in DB v2. Any decision to split it for analytics will be handled in a separate epic `Report Analytics`.


## Client

- tableName: `clients`

Campi attuali (v1):
- `clientId: Long` PRIMARY_KEY
- `companyName: String`
- `location: String? = "Sede"`
- `notes: String?`
- `networkMode: String` (v1 may be `"DHCP"` or `"Static"`)
- `staticIp: String?`
- `staticSubnet: String?`
- `staticGateway: String?`
- `staticCidr: String? = null` (nuovo — preferire CIDR per la subnet)
- `minLinkRate: String` (nuovo — threshold for PASS like "10M","100M","1G","10G")
- `socketPrefix: String` (prefix for socket ID)
- `socketSuffix: String` (suffix; new configurable formatting)
- `socketSeparator: String` (separator char used between prefix/number/suffix)
- `socketNumberPadding: Int` (e.g. 3 → 001)
- `nextIdNumber: Int` (current state to persist)
- `lastFloor: String?` (LEGACY — to be removed in v2)
- `lastRoom: String?` (LEGACY — to be removed in v2)
- Speed test fields: `speedTestServerAddress: String?`, `speedTestServerUser: String?`, `speedTestServerPassword: String?`

Planned v2 changes and design decisions (Client):
- Mark `lastFloor`/`lastRoom` as legacy and remove in a future migration (fields kept in v1 for compatibility until migration).
- Add `socketTemplateConfig: String?` as an **optional JSON-serialized** field on the `Client` entity. It stores the socket template structure used by the domain layer and is *opt-in* for new features. Existing socket-related fields (`socketPrefix`, `socketSeparator`, `socketNumberPadding`, `nextIdNumber`) will remain for backward compatibility.
- Consider combining `staticIp/staticSubnet/staticGateway` to `staticCidr` + gateway to deprecate legacy fields in a future migration.

Migration notes:
- Existing `nextIdNumber` is compatible; however schema changes requiring a new entity will include migration logic via `Migrations.kt`.

---

## ProbeConfig

- tableName: `probe_config`

Campi attuali (v1):
- `probeId: Long` PRIMARY_KEY
- `ipAddress: String`
- `username: String`
- `password: String`
- `testInterface: String`
- `isOnline: Boolean`
- `modelName: String?`
- `tdrSupported: Boolean`
- `isHttps: Boolean` (default `false`)

Planned v2 changes and design decisions (ProbeConfig):
- Consider normalizing `testInterface` into an explicit interface object in DB, if/when we want to manage multiple test interfaces per probe (future epic; not in v2 scope).
- Add probe capability fields for streaming logs, LLDP support, etc. as optional booleans (future epic).

Design decision (TDR capability):
- `ProbeConfig.tdrSupported` is a **cached boolean** to speed up UI/queries but is **not** the authoritative source of truth for TDR capability.
- The authoritative source is the domain `TdrCapabilities` component which derives capability based on `modelName`/`board-name` and a compatibility list. Implementations should update the cached `tdrSupported` flag as probes are discovered/updated but rely on `TdrCapabilities` when deciding to run TDR.

---

## TestProfile

- tableName: `test_profiles`

Campi attuali (v1):
- `profileId: Long` PRIMARY_KEY
- `profileName: String`
- `profileDescription: String?`
- `runTdr: Boolean`
- `runLinkStatus: Boolean`
- `runLldp: Boolean`
- `runPing: Boolean`
- `pingTarget1: String?`, `pingTarget2: String?`, `pingTarget3: String?`
- `pingCount: Int` (default 4)
- `runSpeedTest: Boolean`

Planned v2 changes:
- If we support more advanced ping/traceroute configs, move ping targets into a subtable or serialized field
- Add TTL/validation for profile versions if profiles are shared across projects in the future

---

## Report

- tableName: `test_reports`

Campi attuali (v1):
- `reportId: Long` PRIMARY_KEY
- `clientId: Long?` (foreign key optional)
- `timestamp: Long`
- `socketName: String?`
- `notes: String?`
- `probeName: String?`
- `profileName: String?`
- `overallStatus: String`
- `resultsJson: String` (full detailed results in JSON)

Planned v2 changes and design decisions (Report):
- `Report.resultsJson` will remain a **single JSON** column in DB v2 to preserve existing flexibility and avoid disruptive migrations at this stage.
- The possibility of splitting `resultsJson` into separate columns to enable per-metric queries is postponed to a dedicated future epic named **"Report Analytics"**, which will include schema evaluation and migration planning.
- `durationMs: Long?` and `version: Int` can be considered later and are not mandatory in v2.

---

## Legacy fields and policy

- `lastFloor` and `lastRoom` in `Client` are considered legacy; they should be removed in v2.
- Avoid adding more "legacy" fields that have no use: document purpose carefully before adding fields.

---

## Implementation Notes

- When implementing DB v2 Migrations:
  - Create `AppDatabase` new version with migration steps in `Migrations.kt`
  - Run migration tests verifying no data loss for existing fields
  - Keep `Client` DAO operations fully compatible during migration by maintaining accessors until migration completes

---

## Open Questions requiring follow-up

- How to migrate `resultsJson` and support querying on structured metrics? (Trade-offs between storage size vs. query ability)
- Should `socketTemplateConfig` be a new table (entity) or a serialized field on `Client`? If it's a table, define a foreign key to `clients`.


---

## References

- See `app/src/main/java/com/app/miklink/data/db/model/` for current model definitions
- Migrations live in `app/src/main/java/com/app/miklink/data/db/Migrations.kt`
