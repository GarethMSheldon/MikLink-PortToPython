# Known Issues - MikLink

**Ultimo aggiornamento:** 2025-12-12  
**EPIC:** S8 - Sunset definitivo di AppRepository

## S8 - Known Issues (Posticipati ma Tracciati)

### S8-001: Commenti Storici nei KDoc

**Descrizione:** Alcuni file contengono riferimenti ad AppRepository solo in commenti KDoc storici.

**File Coinvolti:**
- `app/src/main/java/com/app/miklink/core/data/repository/test/NetworkConfigRepository.kt` - Linea 11
- `app/src/main/java/com/app/miklink/data/repositoryimpl/NetworkConfigRepositoryImpl.kt` - Linee 19-20
- `app/src/main/java/com/app/miklink/core/data/repository/test/PingTargetResolver.kt` - Linea 8
- `app/src/main/java/com/app/miklink/data/repositoryimpl/mikrotik/MikroTikTestRepositoryImpl.kt` - Linea 22

**Impatto:** Nessuno - solo commenti storici, non bloccanti

**Riproduzione:** N/A - commenti KDoc

**Area:** Documentazione codice

**Severità:** Bassa

**Workaround:** Nessuno necessario - può essere aggiornato in futuro per pulizia

**Note:** Questi commenti possono essere aggiornati per rimuovere riferimenti ad AppRepository, ma non sono bloccanti per la funzionalità.

---

### S8-002: Riferimento Storico in ARCHITECTURE.md

**Descrizione:** ARCHITECTURE.md contiene ancora un riferimento storico ad AppRepository in un esempio di codice (linea 442).

**File Coinvolti:**
- `docs/ARCHITECTURE.md` - Linea 442

**Impatto:** Nessuno - solo esempio storico

**Riproduzione:** N/A - documentazione

**Area:** Documentazione

**Severità:** Bassa

**Workaround:** Nessuno necessario - può essere aggiornato in futuro per pulizia

**Note:** Questo riferimento può essere aggiornato per riflettere l'architettura attuale senza AppRepository.

---

## Note Generali

Questi known issues sono stati tracciati durante EPIC S8 ma non sono bloccanti per la funzionalità. Possono essere risolti in futuro per migliorare la pulizia del codice e della documentazione.


## UI/UX & Localizzazione

- In italiano la scheda dà ancora "Add Probe"
  - Stato: IN PROGRESS
  - Severità: P1
  - Epic target: U1
- Se non ho connessione alla sonda: errore + consente inserire interfaccia a mano (UI/UX da migliorare)
  - Stato: OPEN
  - Severità: P1
  - Epic target: U2
- Se clicco "use https": errore SSL "openssl handshake failure on client hello"
  - Stato: OPEN
  - Severità: P0
  - Epic target: U2
- Slider intervallo aggiornamento sonda e intensità glow non piacciono
  - Stato: OPEN
  - Severità: P2
  - Epic target: U2
- Temi non piacciono
  - Stato: OPEN
  - Severità: P2
  - Epic target: U2
  - Stato: RESOLVED
  - Severità: P1
  - Epic target: U1
- Testi dei test sempre in inglese anche con italiano
  - Stato: IN PROGRESS
  - Severità: P1
  - Epic target: U1
- Quick fill funziona ma è brutto
  - Stato: OPEN
  - Severità: P2
  - Epic target: U2
  - Stato: RESOLVED (secondary screens now include a back action)
  - Severità: P1
  - Epic target: U1
- In gestisci clienti: clienti non visualizzati come profili (inconsistenza)
  - Stato: OPEN
  - Severità: P2
  - Epic target: U2
- Campo cerca presente in clienti ma non in profili (consistenza)
  - Stato: OPEN
  - Severità: P2
  - Epic target: U2
- Tasto esporta pdf sulla linea del cliente inutile
  - Stato: OPEN
  - Severità: P2
  - Epic target: U2
- Speedtest: quickfill con dhcp_gateway + capire se possibile far fare un "test" per verificare speedtest attivo
  - Stato: OPEN
  - Severità: P1
  - Epic target: U2
  - Stato: RESOLVED (back button added to ClientList)
  - Severità: P1
  - Epic target: U1
- Pagina Tema in impostazioni male strutturata
  - Stato: OPEN
  - Severità: P2
  - Epic target: U2
- In Preferenze rapporto PDF: "Includi test vuoti" vs "nascondi colonne vuote" confonde
  - Stato: OPEN
  - Severità: P2
  - Epic target: U2
- Colonne da stampare: default dovrebbe nascondere colonne senza dati
  - Stato: OPEN
  - Severità: P2
  - Epic target: U2
- Strategia Numerazione da rivedere (socket ID refactor)
  - Stato: OPEN
  - Severità: P2
  - Epic target: U2
- Storico test: filtri "solo pass/fail" da rendere consistenti con icona + pass/fail e "tutto"
  - Stato: OPEN
  - Severità: P2
  - Epic target: U2
  - Stato: RESOLVED (toggle behavior and cards rendering verified and UI tests added)
  - Severità: P1
  - Epic target: U1
- In quasi tutti i temi, bottone "salva" poco visibile (cambia colore con tema)
  - Stato: OPEN
  - Severità: P2
  - Epic target: U2
- Animazione logo centrale pass/fail non piace: icona ferma + glow dietro
  - Stato: OPEN
  - Severità: P2
  - Epic target: U2
- Mini-card presa sotto logo "triste": valutare eliminarla o aggiungere info
  - Stato: OPEN
  - Severità: P2
  - Epic target: U2

