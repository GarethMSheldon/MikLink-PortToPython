# Build & Tooling

## Requisiti

- Android Gradle Plugin (AGP): **8.13.2**
- Kotlin: **2.2.21**
- Compile SDK: **36**
- Min SDK: **30**
- Target SDK: **36**

Version catalog: `gradle/libs.versions.toml`.

## Dipendenze principali (snapshot)

- Compose BOM: **2025.12.00**
- Hilt: **2.56.2**
- Room: **2.8.4**
- Retrofit: **2.11.0** + OkHttp **4.12.0**
- Moshi: **1.15.2**
- Coroutines: **1.10.2**
- iText: **7.2.6** (PDF)

## Annotation processing (KSP/KAPT)

- Plugin: `com.google.devtools.ksp` applicato al modulo `app`.
- Processor KSP:
  - `room-compiler` (schema esportato in `app/schemas/**`)
  - `hilt-compiler` (KSP attivo in questo progetto; monitorare stabilità)
- Processor KAPT: _nessuno_ (kapt non applicato al momento).

### Rationale
- Room: KSP è raccomandato da Android (miglior build perf / supporto Kotlin 2/KSP2).
- Hilt/Dagger: KSP support è ancora segnalato come alpha nei docs Dagger, ma è abilitato qui; se emergono problemi, valutare fallback a kapt (applicando `kotlin("kapt")` e sostituendo `ksp(libs.hilt.compiler)` con `kapt(...)`).

### Migrazione/futuro
- Continuare a preferire KSP dove disponibile.
- Se si introduce un processor senza KSP stabile, aggiungere `kapt` solo per quel caso e documentarlo qui.
- Monitorare le note di rilascio:
  - “Migrate from kapt to KSP”: https://developer.android.com/build/migrate-to-ksp
  - Room release notes (KSP/KSP2): https://developer.android.com/jetpack/androidx/releases/room
  - Dagger KSP status: https://dagger.dev/dev-guide/ksp.html

## Comandi utili

```bash
# unit test
./gradlew test

# quality gate (inclusi scan stringhe)
./gradlew test

# build debug
./gradlew assembleDebug

# instrumentation (se presenti test strumentati)
./gradlew connectedAndroidTest
```

## Note

- Lo schema Room viene esportato in `app/schemas/**` (versione attuale: v1).
