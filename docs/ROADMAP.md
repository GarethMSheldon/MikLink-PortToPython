# MikLink – Roadmap di Refactor e Feature

## 1. Introduzione e Regole di Ingaggio

Questa roadmap definisce come evolvere MikLink da progetto “funzionante” a prodotto
open source **manutenibile**, **estendibile** e **coerente** dal punto di vista tecnico
(SOLID, Clean Architecture) e dell’esperienza utente (UI/UX consistente, help chiari, multilingua).

Lo scopo è:

- ridurre il debito tecnico accumulato,
- rendere il codice comprensibile anche a contributor esterni,
- introdurre nuove feature (LLDP/VLAN, TDR, Socket ID configurabile, log live, progetti, ecc.)
  senza rompere la UI esistente delle schermate di test,
- allineare il progetto alle best practices Kotlin/Android e alle linee guida Mikrotik.

> ⚠️ **Importante**:  
> Tutte le attività descritte in questa roadmap devono essere eseguite con estrema
> attenzione. Errori di design in questa fase introdurranno problemi difficili da
> rimuovere in futuro.

---

## 1.1 Ruolo richiesto all’implementatore

Chi lavora su questa roadmap (sviluppatore o agent) deve comportarsi come:

> **Senior Kotlin/Android Developer**  
> con esperienza in:
> - Kotlin + coroutines
> - Android (ViewModel, Jetpack Compose)
> - Room
> - Retrofit/Moshi
> - Dependency Injection (es. Hilt)
> - principi SOLID e Clean Architecture

### Comportamento richiesto

- **Non inventare né assumere nulla** che non sia:
  - esplicitamente descritto in questa roadmap,
  - già presente nel codice esistente,
  - oppure documentato chiaramente nella documentazione Mikrotik ufficiale.
- Se per completare un task mancano informazioni:
  - **fermarsi**,
  - raccogliere dubbi specifici,
  - chiedere chiarimenti PRIMA di procedere.
- Non stravolgere il modello di dominio (Client, Probe, TestProfile, Report):  
  le evoluzioni devono essere **incrementali** e motivate.

---

## 1.2 Principi architetturali (SOLID / Clean Architecture)

Tutte le nuove implementazioni devono rispettare i seguenti principi:

1. **Single Responsibility Principle (SRP)**  
   Ogni classe / file ha **una sola responsabilità chiara**.  
   Esempi:
   - un componente che seleziona il neighbor primario LLDP/CDP non deve anche salvare un report;
   - un generatore di Socket ID non deve chiamare direttamente la rete.

2. **Separation of Concerns (Presentation / Domain / Data)**  
   - **Presentation**: UI + ViewModel (stato, navigation, mapping da dominio a UI).  
   - **Domain**: logica di business pura (regole su socket, LLDP, TDR, link, logging, ecc.).  
   - **Data**: accesso a DB, rete, file system (Room, Retrofit, file, backup).

3. **Open/Closed Principle (OCP)**  
   Nuove feature o varianti devono essere aggiunte estendendo componenti esistenti
   (nuove implementazioni, nuovi use case), non aggiungendo “if” in giro per il codice.

4. **Dependency Inversion Principle (DIP)**  
   - Presentation e Domain dipendono da **interfacce**, non da implementazioni concrete.
   - Room/Retrofit/Android SDK devono rimanere confinati negli strati Data / UI.

5. **Niente nuove “God class”**  
   - `AppRepository` e simili sono già sovraccarichi: le epiche serviranno anche a ridurne
     lentamente le responsabilità.
   - Ogni nuova classe deve avere un compito limitato e chiaro.

---

## 1.3 Linee guida Kotlin / Android da seguire

L’implementatore non deve conoscere a memoria tutte le style guide:  
ogni epic/task indicherà i vincoli specifici.  
Tuttavia sono sempre valide queste regole generali:

- **Kotlin official coding conventions**  
  - naming e formattazione standard Kotlin,
  - classi/interfacce in PascalCase,
  - funzioni/variabili in camelCase,
  - niente import inutili.

- **Null-safety e immutabilità**  
  - evitare `!!` (force unwrap) se non assolutamente necessario;
  - preferire `val` a `var` quando la variabile non deve cambiare;
  - preferire collezioni immutabili come default;
  - per risultati complessi, usare **data class dedicate**, non `Pair`/`Triple`.

- **Best practices Android**  
  - ViewModel senza riferimenti diretti a `Context` (se non tramite pattern appropriati);
  - niente logica di business pesante nei Composable;
  - niente operazioni blocccanti nel main thread.

- **Futurice Android best practices (estratto rilevante)**  
  - non committare nel VCS:
    - file di IDE (`.idea`, `.vscode`, ecc.),
    - output di build (`build/`),
    - keystore o file con segreti,
    - `local.properties`;
  - mantenere la struttura standard Gradle/Android (`app/src/main/java`, `res`, ecc.).

Ogni epic includerà una piccola **“Kotlin / Android Style Checklist”**
ad hoc, che l’implementatore deve seguire.

---

## 1.4 Regole di ingaggio generali per le EPIC

1. **Ogni EPIC rappresenta una feature/fix unica e testabile**  
   - Deve coprire l’intera verticale:  
     Data → Domain → Presentation (UI) per quella feature specifica.
   - Deve avere criteri di accettazione chiari.

2. **Backend + Frontend nello stesso contesto**  
   - Non esistono epiche “solo backend” o “solo UI” scollegate:  
     ogni modifica di dominio deve riflettersi nella UI, dove ha senso, e viceversa.

3. **Compatibilità progressiva**  
   - La refactorizzazione avviene per passi:  
     non si riscrive tutto in un colpo.
   - Esisterà del codice `_legacy` che conviverà temporaneamente con il nuovo codice SOLID.

4. **Nessun cambio “di massa” non necessario**  
   - Niente rename globali di classi/cartelle senza bisogno concreto e senza
     un piano di migrazione specifico per l’epic coinvolta.
   - Ogni cambiamento strutturale va motivato e circoscritto.

5. **UI di test invariate nella struttura di base**  
   - Le schermate di test già esistenti (progress, card, pass/fail) non devono essere
     modificate nella loro struttura generale.
   - È consentito **solo**:
     - aggiungere campi informativi in card esistenti,
     - migliorare testi/help,
     - correggere bug visuali.

---

## 1.5 Uso delle API Mikrotik e dei dati di rete

Per tutto ciò che riguarda la sonda Mikrotik:

- Non inventare endpoint REST o parametri non documentati ufficialmente
  o non già presenti nel codice esistente.
- Basarsi sempre su:
  - documentazione Mikrotik ufficiale (RouterOS, REST API, Neighbor discovery),
  - comportamenti osservabili reali della sonda,
  - endpoint effettivamente in uso nel codice corrente.
- Se una certa detection (es. TDR) non è affidabile usando solo API generiche,
  si preferisce:
  - una **lista di compatibilità fissa** documentata,
  - e/o un comportamento chiaramente marcato come “informativo”.

---

## 1.6 In caso di dubbio o informazione insufficiente

Se, durante l’implementazione di una epic:

- non è chiaro cosa debba fare una funzione o classe,
- la documentazione Mikrotik non conferma un’ipotesi,
- la roadmap non specifica esplicitamente un comportamento,

l’implementatore deve:

1. **fermarsi**,  
2. documentare:
   - qual è il dubbio,
   - quali opzioni vede,
   - quali rischi si corrono scegliendo da soli,
3. chiedere istruzioni, **prima** di introdurre codice che “sembra” funzionare
   ma non è allineato alla visione del progetto.

Non è accettabile “indovinare” il comportamento del sistema,
soprattutto per:

- logica di test e certificazione (LLDP, VLAN, TDR, link),
- regole di generazione Socket ID,
- struttura dei dati di report,
- interazione con dispositivi Mikrotik.

---

## 1.7 Definizione di Done (DoD) per ogni EPIC

Un’epic è considerata completata solo se:

- il comportamento funzionale è allineato alla specifica di roadmap;  
- la UI è coerente con le regole di UX (test UI non stravolta);  
- la logica di dominio aggiunta/modificata è:
  - in un componente dedicato,
  - con responsabilità unica,
  - richiamata da Presentation/Data in modo chiaro;
- non ci sono regressioni evidenti (test manuali sulle parti toccate + build pulita);
- eventuali modifiche a DB, backup, PDF sono state aggiornate in modo coerente;
- non sono stati introdotti file di IDE/build/segreti nel repository;
- la **Kotlin / Android Style Checklist** della epic è rispettata.

Solo quando tutti questi punti sono soddisfatti, l’epic può essere considerata “Done” e si può passare alla successiva.


## 2. EPIC A – Pulizia iniziale & Skeleton SOLID

### 2.1 Scopo e contesto

Questa EPIC ha l’obiettivo di:

1. Pulire il repository da file/cartelle di IDE, build o locali che non devono stare nel VCS.
2. Introdurre una struttura di package **SOLID / Clean Architecture** chiara, sotto `com.app.miklink.core` e `com.app.miklink.feature`, pronta ad ospitare la nuova logica.
3. Definire il **DB v2 a livello di design** (schema e naming), senza ancora cambiare il comportamento runtime.
4. Creare i file di dominio e data layer (vuoti o con solo commenti) che descrivano:
   - responsabilità,
   - input,
   - output,
   per le parti chiave (Socket ID, LLDP/neighbor, TDR, link stabilization, logs).
5. Definire una **policy di gestione del codice legacy** (`_legacy`) per le epiche successive.

> ⚠️ Questa EPIC **non** riscrive ancora la logica esistente.  
> La re-implementazione SOLID della logica attuale verrà eseguita in epiche successive
> (LLDP/VLAN, TDR, Socket, Link, Logs, ecc.), in modo incrementale e testabile.

---

### 2.2 A1 – Pulizia del repository (file/cartelle inutili)

**Obiettivo**

Rimuovere dal repository file e cartelle che non devono essere versionati (IDE, log, build, configurazione locale, chiavi), allineandosi alle Futurice Android Best Practices.

**Scope**

Sul progetto uploadato (`MikLink/`), esistono le seguenti cartelle/file non adatti al VCS:

- `MikLink/.idea/` → configurazione IDE Android Studio.
- `MikLink/.kotlin/` → log e sessioni del plugin Kotlin.
- `MikLink/.run/` → run configuration locali.
- `MikLink/.vscode/` → config locale VS Code.
- `MikLink/app/build/` → output di build (file `.compiler.options`, ecc.).
- `MikLink/local.properties` → path SDK locale.
- `MikLink/key` → file chiave generico (da verificare: se contiene keystore o segreti, NON deve stare nel repo).
- Eventuali altri file generati dall’IDE non necessari alla build.

**Attività**

1. Rimuovere dal repository (non solo ignorare) le cartelle:
   - `.idea/`
   - `.kotlin/`
   - `.run/`
   - `.vscode/`
   - `app/build/`
   - `local.properties`
2. Analizzare `MikLink/key`:
   - se è un keystore o contiene segreti → rimuoverlo dal repo, aggiungerlo a `.gitignore`, documentare come gestirlo localmente;
   - se invece è un artefatto necessario e condivisibile (es. chiave pubblica) → documentarlo esplicitamente in `docs/README.md`.
3. Aggiornare `MikLink/.gitignore` per includere tutte queste voci in modo che non vengano più aggiunte al VCS.

**Criteri di accettazione**

- Un clone “pulito” del repository, dopo una build, **non mostra** file `.idea/`, `.kotlin/`, `.run/`, `.vscode/`, `app/build/`, `local.properties`, `key` come modifiche non tracciate.
- Il progetto compila regolarmente senza questi file versionati.

---

### 2.3 A2 – Creazione struttura SOLID (package e cartelle)

**Obiettivo**

Introdurre una struttura chiara per Domain / Data / Presentation, senza toccare ancora la logica esistente, in modo che le future epiche possano spostare codice qui dentro in modo ordinato.

**Stato attuale**

Sotto `MikLink/app/src/main/java/com/app/miklink/` sono presenti, tra le altre, le seguenti cartelle:

- `data/` (db, network, pdf, repository, io)
- `ui/` (dashboard, test, client, probe, profile, history, settings, ecc.)
- `di/` (moduli Hilt)
- `domain/usecase/backup/ImportBackupUseCase.kt`
- `utils/` (Compatibility, NetworkValidator, ecc.)

**Nuova struttura da creare (anche vuota)**

Creare i seguenti package (con almeno un file placeholder/commentato) sotto:

`MikLink/app/src/main/java/com/app/miklink/`:

```text
core/
  domain/
    model/          // Modelli di dominio puri (non Room, non DTO di rete)
    socket/         // Regole per Socket ID (template, generator, stato)
    network/        // Regole per LLDP/CDP, neighbor selection, VLAN/Voice VLAN
    tdr/            // Regole su capability TDR e comportamento
    link/           // Regole di stabilizzazione link
    logs/           // Regole di filtro/aggregazione log
    report/         // Regole di interpretazione e aggregazione risultati test
  data/
    local/
      room/         // DAO/Entity adattati al dominio
    remote/
      mikrotik/     // Client Mikrotik, adattatori tra DTO e dominio
    repository/     // Repository di dominio (interfacce + implementazioni)
  presentation/
    common/         // Eventuale stato/UI contract riusabili

feature/
  dashboard/
  test/
  client/
  probe/
  profile/
  history/
  settings/
  logs/
legacy/
  (per eventuali classi marcate _legacy in epiche successive)
⚠️ In questa EPIC non si sposta ancora codice esistente:
si creano solo i package (directory) con file placeholder/commentati.

Criteri di accettazione

I package sopra esistono nel project tree (core/domain/..., core/data/..., feature/...).

Non ci sono errori di compilazione dovuti alla sola presenza di questi package vuoti.

2.4 A3 – Definizione DB v2 (schema e naming, solo documentazione)
Obiettivo

Definire su carta (documento nel repo) lo schema target del database (Room) e dei model persistenti, includendo:

quali entity esistono (Client, ProbeConfig, TestProfile, Report…),

quali campi sono considerati legacy (es. lastFloor, lastRoom),

come verranno rappresentate le nuove configurazioni (es. Socket ID Template).

Attività

Creare un nuovo file di documentazione:

MikLink/docs/DATABASE_V2.md

In questo documento, descrivere:

Le entity esistenti in com.app.miklink.data.db.model:

Client

ProbeConfig

TestProfile

Report

Per ciascuna:

nome tabella (tableName),

elenco campi attuali (nome + tipo),

campi marcati come “da rimuovere” o “legacy”:

per Client: lastFloor, lastRoom sono da considerare da eliminare in una futura migrazione;

eventuali nuovi campi target (es. un campo socketTemplateConfig: String? per configurazioni di Socket ID), specificando che non viene ancora introdotto in codice in questa EPIC.

Aggiornare docs/ARCHITECTURE.md per:

referenziare DATABASE_V2.md come fonte di verità per lo schema target,

indicare che il DB attuale è v1 e che la migrazione a v2 sarà gestita in una EPIC successiva (dedicata alle modifiche DB).

Criteri di accettazione

Esiste docs/DATABASE_V2.md con una descrizione chiara di:

entity,

campi,

cosa è legacy,

cosa è pianificato per v2.

Nessuna entity Kotlin (es. Client.kt) viene ancora modificata in questa EPIC.

2.5 A4 – Creazione file dominio/dati con responsabilità documentata
Obiettivo

Creare i file chiave del nuovo dominio e data layer (vuoti o quasi), documentando esattamente cosa faranno, cosa riceveranno e cosa restituiranno, senza implementare ancora la logica.

File di dominio da creare (sotto com.app.miklink.core.domain)

core/domain/socket/SocketTemplate.kt

Scopo (commento):

descrivere la struttura di un Socket ID come sequenza di segmenti (testo fisso, numero, lettera, separatore).

Input previsto:

configurazione salvata a livello Client (es. template + stato di incremento).

Output previsto:

rappresentazione immutabile della template (data class di dominio).

core/domain/socket/SocketIdGenerator.kt

Scopo:

generare il valore di Socket ID corrente per un cliente (e opzionalmente stato successivo), basandosi su SocketTemplate.

Input previsto:

SocketTemplate di quel cliente,

stato di incremento attuale,

(opzionale) override manuale dell’utente.

Output previsto:

socket ID calcolato (stringa di dominio),

nuovo stato di incremento da salvare solo a salvataggio report.

core/domain/network/NeighborSelector.kt

Scopo:

scegliere il neighbor primario sulla porta di test tra una lista di neighbor LLDP/CDP/MNDP.

Input previsto:

lista di neighbor di dominio (ad es. mappati da NeighborDetail di rete),

eventuali info aggiuntive (es. host table) in future epiche.

Output previsto:

un oggetto di dominio (es. NeighborSelection) con:

neighbor primario (se esiste),

lista di tutti i neighbor rilevati.

core/domain/tdr/TdrCapabilities.kt

Scopo:

essere l’unica fonte di verità sulle capacità TDR per un determinato modello/board Mikrotik.

Input previsto:

board-name / modelName della sonda.

Output previsto:

uno stato di dominio (es. Supported / NotSupported / Unknown).

core/domain/link/LinkStabilizer.kt

Scopo:

definire le regole per “attendere link stabile” prima di eseguire una suite di test.

Input previsto:

stato link corrente (ottenuto dal layer Data),

parametri di timeout/ritentativo.

Output previsto:

decisione di “link pronto” o “timeout/non pronto”.

core/domain/logs/LogFilter.kt

Scopo:

filtrare una lista di log Mikrotik per topic/severity secondo le preferenze utente.

Input previsto:

lista log di dominio,

configurazione filtri da UserPreferencesRepository.

Output previsto:

lista log filtrata.

core/domain/logs/LogStreamPolicy.kt

Scopo:

descrivere se usare log “streaming” o “polling” in base alle capacità del dispositivo/RouterOS.

Input previsto:

informazioni capacità del dispositivo, versione RouterOS.

Output previsto:

decisione: Streaming, Polling, oppure fallback.

File data layer da creare (sotto com.app.miklink.core.data)

core/data/remote/mikrotik/MikroTikClient.kt

Scopo:

incapsulare MikroTikApiService esistente e offrire metodi di accesso di livello dominio (es. getNeighborsForInterface, getLinkStatus, runCableTest).

In questa EPIC:

solo commenti, nessuna logica.

core/data/local/room/ClientDaoV2.kt (o naming simile)

Scopo:

definire la versione target del DAO per Client in ottica DB v2.

Solo commenti:

descrivere quali query saranno necessarie (es. per socket template e stato incrementale).

core/data/repository/ClientRepository.kt, ProbeRepository.kt, ecc. (placeholder)

Scopo:

interfacce di repository di dominio (non ancora implementate).

Solo commenti:

quali metodi principali esporranno (es. getClientById, updateSocketState, getProbeConfig).

Criteri di accettazione

Tutti i file di dominio/data sopra elencati esistono con:

package corretti,

solo commenti che descrivono chiaramente:

responsabilità unica,

input di alto livello,

output di alto livello.

Nessuna implementazione concreta è stata aggiunta in questi file in questa EPIC.

2.6 A5 – Policy _legacy e mappatura del codice esistente
Obiettivo

Stabilire una policy chiara per gestire il codice storico man mano che viene sostituito da nuove implementazioni SOLID, senza spostare ancora nulla in questa EPIC.

Attività

Aggiornare docs/ARCHITECTURE.md con una sezione “Legacy code policy” che specifichi:

Classi considerate “candidate” legacy:

com.app.miklink.data.repository.AppRepository

porzioni di TestViewModel, DashboardViewModel, ecc., che oggi contengono logica di dominio.

Regola di rinomina:

Quando una nuova implementazione SOLID sostituisce in modo completo una classe o una porzione di logica esistente,
la vecchia classe può essere rinominata in NomeClasse_legacy o spostata sotto com.app.miklink.legacy,
fino alla rimozione definitiva dopo un periodo di stabilizzazione.

Obbligo di:

marcare le classi legacy con annotazione/commento chiaro (@Deprecated se appropriato),

non aggiungere nuova logica a classi marcate _legacy.

Non rinominare né spostare ancora file esistenti in _legacy:
questo avverrà nelle epiche successive, legate a funzionalità specifiche (es. EPIC LLDP, EPIC TDR, EPIC Socket, ecc.).

Criteri di accettazione

docs/ARCHITECTURE.md contiene una sezione chiara sulla policy legacy.

Non ci sono ancora classi rinominate _legacy in questa EPIC (nessun comportamento runtime modificato).

2.7 Kotlin / Android Style Checklist per EPIC A
Per tutte le modifiche di questa EPIC:

Usa le Kotlin official coding conventions per naming e package:

package in lowercase senza underscore (es. com.app.miklink.core.domain.socket);

classi/interfacce in PascalCase (es. SocketTemplate, NeighborSelector);

funzioni/variabili in camelCase.

Non introdurre funzioni top-level non necessarie:

se devi descrivere responsabilità future, fallo in commento all’interno di una classe o in file dedicati (package-info o simili).

Non introdurre logica in core/domain e core/data in questa EPIC:

solo commenti descrittivi.

Adegua il repository alle Android Best Practices Futurice:

nessun file di IDE/build/keystore/versionato (vedi A1).

Se ritieni necessario deviare da queste regole per completare A1–A5,

fermati e chiedi istruzioni invece di decidere da solo.

---

## EPIC A - AVANZAMENTO (stato corrente)

Le attività principali di questa EPIC sono state eseguite con le seguenti note:

- ✅ **A1**: `.gitignore` aggiornato per escludere cartelle di IDE e output di build (`.idea/`, `.kotlin/`, `.run/`, `.vscode/`, `app/build/`, `local.properties`, `key`).
- ✅ **A2**: Creata la struttura `com.app.miklink.core` e `com.app.miklink.feature` con file placeholder per Domain/Data/Presentation.
- ✅ **A3**: `docs/DATABASE_V2.md` creato: schema DB target e chiarimenti su campi legacy (e.g. `lastFloor`, `lastRoom`).
- ✅ **A4**: Placeholder creati per i file di dominio/data (SocketTemplate, Generate, NeighborSelector, TdrCapabilities, LinkStabilizer, LogFilter, etc.).
- ✅ **A5**: `docs/ARCHITECTURE.md` aggiornato con policy "Legacy code" e riferimenti a `DATABASE_V2.md`.
- ✅ **A6**: `docs/CLEANUP_GUIDE.md` aggiunto contenente la procedura consigliata per rimuovere file sensibili dal repository (`key`, `local.properties`) e suggerimenti su secret management.

⚠️ **Azioni manuali (non eseguite automaticamente)**:
- Rimuovere la cartella `key` dalla storia Git con `git rm --cached -r key` e committare; questo passaggio è volontario e richiede consenso del team.
- Verificare che tutti i client locali non necessitino del file `key` in workspace (backup se necessario).

🎯 **Prossimi passi suggeriti**:
1. Creare Issue/PR separati per i seguenti elementi: DB migration plan (v2), implementare `SocketTemplate` / `SocketIdGenerator` e test di integrazione, rimozione sicura della cartella `key` tramite PR dedicata.
2. Pianificare EPIC B per l'implementazione delle regole di business nel domain layer e la migrazione del repository legacy.

