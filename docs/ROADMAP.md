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


EPIC S6 — Eliminare i bridge verso AppRepository nel percorso “Run Test” (NetworkConfig + DHCP/Gateway)

Copia/incolla in ROADMAP. Super-dettagliata, con stop condition anti-drift.

Obiettivo

Rendere il percorso di esecuzione test (RunTestUseCase → Steps → Repositories → MikroTik REST) indipendente da AppRepository / legacy, eliminando:

il bridge di NetworkConfigRepository verso AppRepository

il bridge “service build + DHCP gateway” dentro PingTargetResolverImpl (oggi basato su buildServiceFor(probe) + api.getDhcpClientStatus(interfaceName))

Nota: questa EPIC riguarda solo il percorso “Run Test”. AppRepository può restare per feature non ancora migrate.

S6.0 — Baseline (obbligatorio)

Eseguire e salvare output:

./gradlew :app:kspDebugKotlin → salvare in docs/migration/S6_ksp_baseline.txt

./gradlew assembleDebug → salvare in docs/migration/S6_assemble_baseline.txt

./gradlew testDebugUnitTest → salvare in docs/migration/S6_tests_baseline.txt

Stop condition: se uno fallisce, NON procedere con S6.

S6.1 — Inventario dipendenze residue da AppRepository nel path “Run Test”
Target

core/domain/usecase/test/RunTestUseCaseImpl.kt

data/teststeps/*StepImpl.kt

data/repositoryimpl/* (in particolare NetworkConfigRepositoryImpl, PingTargetResolverImpl)

di/TestRunnerModule.kt, di/RepositoryModule.kt

Azione

Cercare nel codice (PowerShell o IDE) riferimenti a:

AppRepository / AppRepository_legacy

package com.app.miklink.legacy.*

Produrre lista (solo testo) in docs/migration/S6_dependency_audit.md con:

file path

simbolo usato (es. tipo, metodo)

motivo (es. “applyClientNetworkConfig”, “buildServiceFor”)

Stop condition: se il path Run Test usa ancora legacy in più punti oltre quelli già noti, segnalarli nel file e includerli negli step successivi (non inventare fix).

Checkpoint: ./gradlew :app:kspDebugKotlin PASS

S6.2 — Centralizzare la creazione del service MikroTik in una dipendenza DI (no factory sparsa)
Problema attuale (dato)

PingTargetResolverImpl richiede probe per chiamare buildServiceFor(probe) e poi api.getDhcpClientStatus(interfaceName).

Target nuovo (SOLID)

Creare un’astrazione unica (in core) per ottenere il service REST:

File da creare

app/src/main/java/com/app/miklink/core/data/remote/mikrotik/service/MikroTikServiceProvider.kt

package com.app.miklink.core.data.remote.mikrotik.service

import com.app.miklink.core.data.local.room.v1.model.ProbeConfig

interface MikroTikServiceProvider {
    fun build(probe: ProbeConfig): MikroTikApiService
}


Implementazione (in data):
app/src/main/java/com/app/miklink/data/remote/mikrotik/MikroTikServiceProviderImpl.kt

Deve usare l’infrastruttura già esistente (MikroTikServiceFactory o equivalente) senza cambiare logica.

Se oggi esiste già una factory DI-friendly, usarla.

Se la factory è statica/companion, wrappare.

DI

Aggiornare di/NetworkModule.kt (o modulo corretto) per bindare:

MikroTikServiceProvider → MikroTikServiceProviderImpl

Refactor immediato

Aggiornare:

PingTargetResolverImpl per dipendere da MikroTikServiceProvider (non chiamare factory direttamente)

Qualsiasi altro componente “Run Test path” che costruisce il service direttamente

Checkpoint:

./gradlew :app:kspDebugKotlin PASS

S6.3 — Estrarre “DHCP Gateway resolution” in un repository dedicato (rimuovere conoscenza DHCP da PingTargetResolver)
Target nuovo

PingTargetResolver deve risolvere target “logici”, ma la logica DHCP (API, parsing DTO, fallback) deve stare in data layer dedicato.

File da creare

app/src/main/java/com/app/miklink/core/data/repository/test/DhcpGatewayRepository.kt

package com.app.miklink.core.data.repository.test

import com.app.miklink.core.data.local.room.v1.model.ProbeConfig

interface DhcpGatewayRepository {
    suspend fun getGatewayForInterface(
        probe: ProbeConfig,
        interfaceName: String
    ): String?
}


Implementazione:
app/src/main/java/com/app/miklink/data/repositoryimpl/mikrotik/DhcpGatewayRepositoryImpl.kt

Deve usare:

MikroTikServiceProvider.build(probe)

MikroTikApiService.getDhcpClientStatus(interfaceName) (esattamente come oggi)

Deve gestire:

risposta senza gateway → ritorna null

errori rete/API → ritorna null oppure propaga eccezione (scegliere 1 comportamento e documentarlo in KDoc; NON inventare “magie”)

Nessun testo localizzato qui.

Aggiornare PingTargetResolverImpl

Dipendenze:

DhcpGatewayRepository

eventuale altra logica già presente

Rimuovere chiamate dirette a api.getDhcpClientStatus(...)

DI

Aggiornare di/RepositoryModule.kt:

bind DhcpGatewayRepository → DhcpGatewayRepositoryImpl

Checkpoint:

./gradlew :app:kspDebugKotlin PASS

./gradlew testDebugUnitTest PASS

Stop condition (dati mancanti):
Se il DTO/response di getDhcpClientStatus non è chiaramente determinabile dal codice esistente, fermarsi e chiedere output di un curl “dhcp-client print/monitor” (NO assunzioni).

S6.4 — Eliminare il bridge NetworkConfigRepository -> AppRepository (core feature di S6)
Obiettivo

NetworkConfigRepositoryImpl deve eseguire direttamente le stesse operazioni che oggi fa AppRepository.applyClientNetworkConfig(...), ma senza chiamarlo.

Step 1 — Congelare comportamento attuale (anti-regressione)

Identificare nel codice AppRepository.applyClientNetworkConfig(...):

file path esatto

firma esatta

sequenza chiamate MikroTik (endpoint/metodi MikroTikApiService)

eventuali scritture DB/Report

Scrivere in docs/migration/S6_network_config_behavior.md:

elenco chiamate in ordine (nome metodo service)

condizioni (es. DHCP vs Static, override etc.)

side effects (scritture DB)

Stop condition: se la logica è troppo “incollata” alla UI o dipende da state globale non riproducibile, fermarsi e chiedere istruzioni (non inventare).

Checkpoint: ./gradlew :app:kspDebugKotlin PASS

Step 2 — Implementazione deterministica in NetworkConfigRepositoryImpl
Target

app/src/main/java/com/app/miklink/data/repositoryimpl/NetworkConfigRepositoryImpl.kt

Azione

Rimuovere qualsiasi dipendenza da AppRepository.

Dipendenze consentite:

MikroTikServiceProvider

repository Room v1 (ClientRepository / ProbeRepository se necessari)

eventuale RouteManager solo se già esiste e non è legacy (altrimenti implementare le chiamate route direttamente come fa AppRepository, senza creare nuovi layer “a caso”)

Replicare la sequenza chiamate documentata nello step precedente.

Pulizia contratto

Rimuovere @Deprecated da:

core/data/repository/test/NetworkConfigRepository.kt

NetworkConfigRepositoryImpl.kt

Aggiornare KDoc: ora è implementazione reale, non bridge.

Checkpoint:

./gradlew :app:kspDebugKotlin PASS

./gradlew assembleDebug PASS

./gradlew testDebugUnitTest PASS

S6.5 — “Run Test path” deve essere legacy-free (verifica automatica)
Verifica

Ricerca testuale (PowerShell) in soli file coinvolti nel run test:

RunTestUseCaseImpl

data/teststeps/*

NetworkConfigRepositoryImpl

PingTargetResolverImpl

DhcpGatewayRepositoryImpl
per:

AppRepository

legacy.

Salvare output in:

docs/migration/S6_legacy_free_audit.txt

Acceptance: nessuna occorrenza nel path Run Test.

Checkpoint: ./gradlew :app:kspDebugKotlin PASS

S6.6 — Test minimi (senza UI automation)
Obiettivo

Aggiungere test focalizzati sulla nuova architettura senza introdurre UI test complessi.

A) Unit test (mock-driven) per PingTargetResolver + DhcpGatewayRepository

Creare test in:

app/src/test/java/com/app/miklink/core/data/repository/test/DhcpGatewayRepositoryContractTest.kt

app/src/test/java/com/app/miklink/core/data/repository/test/PingTargetResolverContractTest.kt

Linee guida:

mock di MikroTikServiceProvider e MikroTikApiService

verificare:

se gateway mancante → null

se PING_NO_TARGETS generato correttamente già coperto dai test del runner (se presenti)

B) Golden parsing (solo se serve)

Se in S6.3 si è dovuto introdurre/aggiornare DTO per getDhcpClientStatus:

aggiungere fixture reale in:

app/src/test/resources/mikrotik/7.20.5/<nome_fixture_dhcp_status>.json

aggiungere golden test in:

app/src/test/java/com/app/miklink/core/data/remote/mikrotik/golden/DhcpStatusGoldenParsingTest.kt

Stop condition (dati mancanti): se non c’è payload reale, fermarsi e chiedere al maintainer di fornire output curl.

Checkpoint finale: ./gradlew testDebugUnitTest PASS

S6.7 — Documentazione finale + log

Creare/aggiornare:

docs/migration/S6_BASELINE.md

docs/migration/S6_RESULT.md

In S6_RESULT.md includere:

elenco file creati/modificati (path completi)

conferma rimozione @Deprecated su NetworkConfigRepository

conferma: Run Test path legacy-free

esito comandi finali:

:app:kspDebugKotlin

assembleDebug

testDebugUnitTest

Acceptance Criteria EPIC S6

✅ NetworkConfigRepositoryImpl non dipende da AppRepository e NetworkConfigRepository non è più deprecato.

✅ PingTargetResolverImpl non costruisce direttamente il service e non chiama direttamente DHCP API: usa MikroTikServiceProvider + DhcpGatewayRepository.

✅ Nessun riferimento a AppRepository / legacy.* nel path Run Test.

✅ Build + unit test PASS con log salvati in docs/migration/.

---

## EPIC S7 — Rimozione dipendenza da AppRepository dalle feature rimanenti (Dashboard / Probe) + Repository SOLID dedicati

**STATO:** ✅ **COMPLETATA**

**Obiettivo:** Rendere le feature Dashboard e Probe indipendenti da AppRepository, creando repository SOLID dedicati.

**Risultato:** 
- ✅ Creati `ProbeStatusRepository` e `ProbeConnectivityRepository`
- ✅ Migrati `DashboardViewModel`, `ProbeEditViewModel`, `ProbeListViewModel`
- ✅ Rimossa dipendenza non utilizzata da `TestViewModel`
- ✅ Tutti i metodi AppRepository utilizzati da Dashboard/Probe sono stati deprecati
- ✅ Build e test PASS (7 test S7, tutti PASSED)

**Documentazione:** 
- `docs/migration/S7_RESULT.md` - Report completo
- `docs/migration/S7_AUDIT_FINAL.md` - Audit finale di verifica
- `docs/migration/S7_viewmodel_dependency_matrix.md` - Matrice dipendenze ViewModel
- `docs/migration/S7_repository_inventory.md` - Inventario repository
- `docs/migration/S7_tests_inventory.md` - Inventario test