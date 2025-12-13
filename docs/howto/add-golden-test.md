# How-to — Aggiungere un nuovo golden parsing test

Questo how-to descrive i passi minimi per aggiungere un golden test per un nuovo endpoint RouterOS.

## 1) Salvare una fixture JSON

- Mettere il JSON in `app/src/test/resources/fixtures/routeros/<version>/`
- Usare un nome descrittivo.

## 2) Caricare la fixture nel test

Usare `FixtureLoader.load("fixtures/routeros/<version>/<file>.json")`.

## 3) Parse con Moshi

- Usare `TestMoshiProvider` per ottenere un `Moshi` consistente con la produzione.
- Parsare la fixture in DTO (o in model “neutro” se previsto).

## 4) Assert

- Verificare campi “essenziali” (non tutto il payload se è fragile).
- Se ci sono normalizzazioni, assertare l'output normalizzato.

## 5) Naming

- `*GoldenParsingTest.kt` sotto `app/src/test/java/.../golden/`
