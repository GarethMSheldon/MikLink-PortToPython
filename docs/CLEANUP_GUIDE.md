# MikLink — Clean-up & secret removal guide

Questo documento descrive i passi consigliati per rimuovere file sensibili e di configurazione locale dal repository, e per proteggere le chiavi presenti nella cartella `key`.

> Nota: eseguire queste operazioni in modo coordinato con il team, perché rimuovere file dal repo è un'operazione sensibile che cambia la cronologia.

## Raccomandazioni

1. Identificare il contenuto della cartella `key`:
   - Se contiene keystore o secret, NON deve restare nel repo.
2. Aggiungere `key` a `.gitignore` (già fatto in questa EPIC): `key/` o `/key`.
3. Rimuovere la versione tracciata dal repository (da eseguire localmente o da CI con privilegi adeguati):

Nota: per questa repo non eseguire comandi `git` automaticamente da script o agent. Creare un branch dedicato e una PR con i cambiamenti, discutere in team e poi applicare le modifiche.

Esempio di processo manuale consigliato (da eseguire solo con autorizzazione del responsabile repository):

```powershell
# 1) Creare un branch di pulizia
git checkout -b chore/remove-key-files

# 2) Rimuovere i file tracciati (il flag --cached lascia i file locali)
git rm --cached -r key

# 3) Aggiornare .gitignore (se non l'hai già fatto)
git add .gitignore

# 4) Commit dei cambiamenti
git commit -m "chore: remove key files from repo and add to .gitignore"

# 5) Push e aprire PR
git push origin chore/remove-key-files
(*apri PR e richiedi revisione, esegui scansione segreti e policy di approvazione*)
```

4. Conservare il contenuto sensibile in un vault/secret manager:
   - Esempio: Azure KeyVault, AWS Secrets Manager, Google Secret Manager.
   - Per dev locali, mantenere la copia in una directory non tracciata e documentare come usarla (es. `docs/README.md` con configurazione locale).

5. Assicurarsi che `local.properties` non venga committato: `/.gitignore` dovrebbe già gestirlo. Se è stato committato, rimuoverlo con `git rm --cached local.properties`.

6. Se il keystore è usato per la firma dell'app, aggiungere workflow CI per fornire il keystore tramite secrets invece che committarlo.

---

## Verifica e follow-up
- Controllare che il repository, dopo il `git rm --cached`, non contenga più i file in `key` nella cronologia HEAD.
- Eseguire scansioni di segreti su PR/CI (es. truffleHog, GitLeaks) per prevenire commits accidentali.

**Importante**: non eseguire comandi `git` (come rimozione dalla storia) senza autorizzazione del team; l'operazione coinvolgerà la storia condivisa e può richiedere approvazione e passaggi coordinati (force push, ecc.).


