# V1 — Obiettivi e confini

Questa pagina serve solo per mantenere allineati “noi” e gli agent durante il refactor.

## Obiettivo v1

Rilasciare una v1 che sia:

- architetturalmente **pulita** (regole di layering esplicite e rispettate)
- **migrata** dal legacy (il legacy non deve rimanere in repo se non temporaneamente durante la migrazione)
- con una suite di test **anti-regressione**:
  - Golden parsing (RouterOS fixtures + Moshi)
  - Quality (hardcoded strings + copertura IT)
- compatibile con il contesto d'uso “cantiere”:
  - supporto a device anche datati (minSdk da rivedere in base ai vincoli reali)
  - comunicazione HTTP o HTTPS (trust-all in HTTPS: scelta consapevole)

## Non obiettivi (post-v1)

- Refactor “completo” del socket-id (campi/separatori/ordine/auto-increment) → dopo v1
- Contract test completi per feature future → placeholder per ora
