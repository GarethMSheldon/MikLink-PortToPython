# Verifica codice MikLink

- **Stringhe PDF corrotte e non compilabili**  
  - Evidenza: `app/src/main/java/com/app/miklink/data/pdf/PdfGeneratorIText.kt:176-180` contiene `Paragraph("* Dettaglio Test")` con residui di caratteri binari nel sorgente (commit precedente), e `app/src/main/java/com/app/miklink/data/pdf/PdfDocumentHelper.kt:90-98` mostra sequenze illeggibili nel warning CPU.  
  - Impatto: il modulo PDF non compila in Kotlin e, anche se forzato, produrrebbe testo corrotto nei report.  
  - Soluzione: ripristinare le stringhe in ASCII pulito (es. `"Dettaglio test"`, warning CPU leggibile, footer "Generato il ... con MikLink") e aggiungere un test snapshot per bloccare regressioni.

- **Violazioni SRP/DIP in AppRepository (God object)**  
  - Evidenza: `app/src/main/java/com/app/miklink/data/repository/AppRepository.kt:37-308` accorpa responsabilità di persistenza (Room DAO), chiamate MikroTik REST, logica di configurazione rete, polling stato e business sui report. Nessuna astrazione lato rete/storage: i DAO e la service factory sono usati direttamente.  
  - Impatto: forte accoppiamento, difficile testare a granularità fine e sostituire implementazioni (violazione SOLID: S e D).  
  - Soluzione: estrarre servizi verticali (es. `ProbeNetworkService`, `ReportService`, `NetworkConfigurator`) dietro interfacce; iniettare dipendenze con contratti dedicati per rete e storage; limitare AppRepository a composizione/coordinamento.

- **Network client insicuro e fuori policy**  
  - Evidenza: `app/src/main/java/com/app/miklink/di/NetworkModule.kt:73-93` forza trust-all SSL e disattiva hostname verification; `:98-100` logga i body di ogni richiesta.  
  - Impatto: esposizione a MITM e leak di credenziali base64 nei log; non conforme a principio di sicurezza by default (SOLID/DIP: infrastruttura non sostituibile con configurazione sicura).  
  - Soluzione: rimuovere il trust-all o limitarlo a build di test; abilitare hostname verification; impostare logging a `BASIC`/OFF in release via flag DI o buildConfig; valutare pinning per i certificati noti MikroTik.

- **Migrazioni DB di fatto disabilitate**  
  - Evidenza: `app/src/main/java/com/app/miklink/di/DatabaseModule.kt:38-40` applica le migrazioni ma poi chiama `fallbackToDestructiveMigration()`.  
  - Impatto: ogni mismatch di schema elimina i dati utente nonostante le migrazioni esistenti (debito tecnico su affidabilità).  
  - Soluzione: rimuovere il fallback distruttivo, aggiungere eventuali auto-migrazioni e aggiornare `MigrationTest` per coprire l’attuale version 13.

- **Ripristino backup non transazionale e senza validazione**  
  - Evidenza: `app/src/main/java/com/app/miklink/data/repository/BackupRepository.kt:29-37` cancella tutto e reinserisce il JSON deserializzato fuori da una transazione e senza controlli di schema.  
  - Impatto: un payload corrotto o un crash a metà lascia il DB vuoto/corrotto.  
  - Soluzione: validare il payload (campi obbligatori, versioni) e usare `runInTransaction` per delete+insert atomico; prevedere un backup locale prima di sovrascrivere.

- **Rimozione indiscriminata di route di default**  
  - Evidenza: `app/src/main/java/com/app/miklink/data/repository/AppRepository.kt:154-158` elimina tutte le `0.0.0.0/0` prima di applicare la configurazione statica.  
  - Impatto: possibili interruzioni di connettività di sistema se esistono altre route di default non legate al test; nessun rollback su errore.  
  - Soluzione: filtrare solo le route aggiunte dall’app (tag/descrizione o interface match), o applicare una nuova route senza cancellare quelle esterne; implementare rollback/dry-run.

- **Supporto HTTP/HTTPS per la sonda (adeguato)**  
  - Evidenza: `app/src/main/java/com/app/miklink/data/repository/AppRepository.kt:59-62` ora usa `MikroTikServiceFactory.createService(probe, wifiNetwork?.socketFactory)` che onora `isHttps`, e `data/network/MikroTikServiceFactory.kt:18-43` accetta `SocketFactory` opzionale.  
  - Impatto: la connessione della sonda funziona sia in HTTP che in HTTPS; ridotta duplicazione di configurazione client.  
  - Follow-up: aggiungere test di integrazione (MockWebServer) per verificare handshake su http/https e la gestione delle credenziali.
