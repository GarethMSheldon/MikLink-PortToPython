# Testing

## Standard attuale (baseline v1)

Per la v1, consideriamo **standard** e “bussola”:

1) **Golden parsing tests** (fixture RouterOS + Moshi)  
2) **Quality tests**:
   - scan hardcoded strings
   - coverage italiano delle stringhe

Altri test presenti (contract/placeholder, Compose, migration) restano nel repo, ma:
- i contract test `@Ignore` sono **placeholder per feature future**;
- i test di migrazione DB possono diventare irrilevanti se scegliamo un “DB rebaseline” (reset).

## Dove sono i test

- Unit test (JVM): `app/src/test/...`
  - `core/data/remote/mikrotik/golden/*GoldenParsingTest.kt`
  - `quality/*`
- Instrumentation (device): `app/src/androidTest/...`
  - test Compose e (attualmente) test migrazione Room

## Come eseguire

```bash
# unit test (include golden + quality)
./gradlew test

# instrumentation
./gradlew connectedAndroidTest
```

## Linee guida

- I golden test devono rimanere deterministici:
  - fixture JSON versionate
  - parsing con Moshi (provider test dedicato)
- I quality test devono fallire se:
  - compaiono stringhe hardcoded in UI invece di `strings.xml`
  - mancano traduzioni IT dove richiesto
