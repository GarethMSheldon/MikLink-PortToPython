# Fixtures RouterOS (Golden parsing)

Le fixture JSON usate dai golden test vivono in:

- `app/src/test/resources/fixtures/routeros/`

Attualmente esiste almeno una versione:

- `7.20.5/` (con `README.md` e varie risposte REST salvate)

## Regole

- Le fixture devono essere **immutabili**: se cambia un endpoint o la struttura, si aggiunge una nuova fixture (nuovo file o nuova cartella versione).
- Ogni nuova fixture deve avere:
  - nome file esplicativo (es. `ethernet_monitor_ether1_link_ok_1gbps.json`)
  - un test golden che la carica via `FixtureLoader`
