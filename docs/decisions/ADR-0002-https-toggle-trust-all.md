# ADR-0002 — HTTP/HTTPS toggle con trust-all in HTTPS

- **Status:** Accepted
- **Data:** 2025-12-13

## Contesto

La sonda (MikroTik) è spesso un dispositivo “solo strumento” in cantiere.
Quando si usa HTTPS, nel 99% dei casi non c'è un certificato valido (CA pubblica / hostname).
L'utente deve poter scegliere **esplicitamente** tra HTTP e HTTPS.

## Decisione

Nella UI di configurazione sonda esiste un toggle:

- **OFF** → usa `http://<ip>`  
- **ON** → usa `https://<ip>` con:
  - **nessuna verifica** del certificato
  - **nessuna verifica** dell'hostname

Questa è una scelta consapevole (trade-off accettato per il contesto d'uso).

## Conseguenze

- Implementare trust-all **solo** quando `isHttps = true`.
- Tenere la scelta nel model di configurazione sonda (`ProbeConfig.isHttps` o equivalente dominio futuro).
- Documentare chiaramente la scelta (questo ADR) per evitare regressioni “security hardening” non desiderate.
