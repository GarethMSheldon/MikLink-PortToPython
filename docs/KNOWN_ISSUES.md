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

