# Rimozioni vendored AndroidX/Compose — MikLink

Data: 21 dicembre 2025

## Sommario
Questo documento riporta le verifiche e le azioni di pulizia chirurgica delle sorgenti AndroidX/Compose vendorizzate trovate nel repository. Tutte le azioni sono basate esclusivamente su ricerche nel codice e su cicli di test locali (unit test, lint, assemble) eseguiti dopo ogni eliminazione.

## Tabella stato
| Item | Confermato? | Azione | Motivo | Comando test eseguito | Esito |
|---|---:|---|---|---|---|
| `commonMain/androidx/compose/material3/BottomSheetScaffold.kt` | Sì | Eliminato ✅ | Vendored, 0 riferimenti nel repo | `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug` | unit: PASS, lint: FAIL (preesistente), assemble: PASS |
| `commonMain/androidx/compose/foundation/layout/WindowInsets.kt` | Sì | Eliminato ✅ | Vendored, 0 riferimenti al di fuori del file | same | unit: PASS, lint: FAIL, assemble: PASS |
| `androidMain/androidx/compose/foundation/layout/WindowInsets.android.kt` | Sì (backup) | Eliminato ✅ (backup in `docs/migration/backup_WindowInsets.android.kt`) | Vendored, backup salvato | same | unit: PASS, lint: FAIL, assemble: PASS |
| `androidMain/androidx/compose/foundation/layout/WindowInsetsPadding.android.kt` | Sì | Eliminato ✅ | Vendored, 0 riferimenti esterni | same | unit: PASS, lint: FAIL, assemble: PASS |
| `androidMain/androidx` (cartella residua) | Sì | **Planned** (to be removed if empty) | residuo non referenziato | (verifica + test after deletion) | (to fill) |

> Nota: `:app:lintDebug` fallisce con errori preesistenti (33 errors, 155 warnings). Ho documentato gli output dettagliati nei log dei comandi (vedi sezione risultati).

## Backup
- `docs/migration/backup_WindowInsets.android.kt` (backup della versione vendorizzata di `WindowInsets` prima della rimozione)

---

## Risultati: passi successivi e comandi suggeriti
- Se sei d'accordo, elimino la cartella residua `androidMain/androidx` (se vuota) e lancio il ciclo di test (unit → lint → assemble).

## Risultati dei cicli di test (aggiornati)

### Eliminazione cartella residua
- `Remove-Item -Recurse -Force -LiteralPath "androidMain\androidx"` → cartella rimossa (era vuota, non referenziata)

### Ciclo test dopo la rimozione
- `./gradlew :app:testDebugUnitTest` → **BUILD SUCCESSFUL** (unit tests pass)
- `./gradlew :app:lintDebug` → **FAILED** (Lint ha riportato 33 errori e 155 warning; problema preesistente, primo errore in `MikroTikProbeConnectivityRepository.kt`)
- `./gradlew :app:assembleDebug` → **BUILD SUCCESSFUL** (assemble OK)

### Build pulita completa
- `./gradlew clean :app:assembleDebug --no-daemon --info --stacktrace` → **BUILD SUCCESSFUL** (con avvisi non bloccanti come mancata strip di alcune librerie native)

> Conclusione: la rimozione della cartella residua non ha introdotto regressioni di compilazione o test; lint già falliva prima ed è rimasto invariato. Per risolvere lint servono interventi dedicati (fix o baseline).


---

File generato automaticamente dall'operazione di pulizia vendored (senza modifiche non necessarie). Se preferisci un altro path per il file checklist, dimmi e lo sposto.
