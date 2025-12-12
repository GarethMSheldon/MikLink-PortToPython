# S6.4 Step 1 - Comportamento Attuale di applyClientNetworkConfig

## File Analizzato
**File:** `app/src/main/java/com/app/miklink/data/repository/AppRepository.kt`  
**Metodo:** `applyClientNetworkConfig(probe: ProbeConfig, client: Client, override: Client?)`  
**Linee:** 129-246

## Firma Esatta
```kotlin
override suspend fun applyClientNetworkConfig(
    probe: ProbeConfig,
    client: Client,
    override: Client? // usa un Client temporaneo per override per-singolo-test
): UiState<NetworkConfigFeedback>
```

## Sequenza Chiamate MikroTik (in ordine)

### Setup Iniziale
1. Determina `effective = override ?: client`
2. Costruisce `api = buildServiceFor(probe)` (usa WiFi network binding)
3. Estrae `iface = probe.testInterface`

### Helper Function: removeStaticAddressesOnInterface()
- `api.getIpAddresses()` → ottiene tutti gli indirizzi IP
- Filtra per `it.iface == iface`
- Per ogni entry: `api.removeIpAddress(NumbersRequest(entry.id))`

### Caso DHCP (`effective.networkMode.equals("DHCP", true)`)

#### Step 1: Verifica stato esistente
- `api.getDhcpClientStatus(iface).firstOrNull()` → ottiene stato DHCP corrente

#### Step 2: Se già configurato e bound
- Condizione: `existingDhcp != null && existingDhcp.disabled == "false" && existingDhcp.status?.equals("bound", ignoreCase = true) == true`
- **Azione:** Ritorna immediatamente `NetworkConfigFeedback` con dati esistenti
- **Nessuna chiamata API aggiuntiva**

#### Step 3: Se client esiste ma non è bound/disabilitato
- Se `existingDhcp.disabled == "true"`: `api.enableDhcpClient(NumbersRequest(dhcpId))`
- Altrimenti (abilitato ma non bound): 
  - `api.disableDhcpClient(NumbersRequest(dhcpId))`
  - `delay(500)`
  - `api.enableDhcpClient(NumbersRequest(dhcpId))`

#### Step 4: Se client non esiste
- `api.addDhcpClient(DhcpClientAdd(interface = iface))`
- Gestione race condition: se errore contiene "already exists":
  - `delay(500)`
  - `api.getDhcpClientStatus(iface).firstOrNull()?.id`
  - Se id trovato: `api.enableDhcpClient(NumbersRequest(existingId))`

#### Step 5: Attesa lease (max 6 secondi)
- Loop `repeat(6)`:
  - `api.getDhcpClientStatus(iface).firstOrNull()`
  - Se `status.equals("bound", true)`: esce dal loop
  - Altrimenti: `delay(1000)`
- Dopo loop: `api.getDhcpClientStatus(iface).firstOrNull()` (ultimo tentativo)
- Ritorna `NetworkConfigFeedback` con dati del lease (o null se non bound)

### Caso STATIC (`else`)

#### Step 1: Disabilita DHCP se presente
- `api.getDhcpClientStatus(iface).firstOrNull()?.id`
- Se `dhcpId != null`: `api.disableDhcpClient(NumbersRequest(dhcpId))`

#### Step 2: Rimuovi IP statici esistenti
- Chiama `removeStaticAddressesOnInterface()` (vedi sopra)

#### Step 3: Rimuovi route default
- `routeManager.removeDefaultRoutes(api, expectedGateway)` dove `expectedGateway = effective.staticGateway`
- RouteManager filtra route con `dstAddress == "0.0.0.0/0"` e (`comment == "MikLink_Auto"` OR `gateway == expectedGateway`)
- Per ogni candidato: `api.removeRoute(NumbersRequest(route.id))`

#### Step 4: Costruisci CIDR
- Se `effective.staticCidr` non è null: usa quello
- Altrimenti: costruisce da `effective.staticIp` e `effective.staticSubnet` → formato `"$ip/$mask"`
- Validazione: `require(!cidr.isNullOrBlank())` → errore se mancante

#### Step 5: Aggiungi IP statico
- `api.addIpAddress(IpAddressAdd(address = cidr, interface = iface))`

#### Step 6: Aggiungi route default
- `api.addRoute(RouteAdd(dstAddress = "0.0.0.0/0", gateway = gw, comment = "MikLink_Auto"))`
- Dove `gw = effective.staticGateway ?: error(...)` → errore se mancante

#### Step 7: Ritorna feedback
- `NetworkConfigFeedback(mode = "STATIC", interfaceName = iface, address = cidr, gateway = gw, dns = null, message = ...)`

## Condizioni e Side Effects

### Condizioni DHCP
- Se già bound: skip configurazione
- Se disabilitato: enable
- Se abilitato ma non bound: disable/enable per refresh
- Se non esiste: crea nuovo client

### Condizioni STATIC
- Richiede `staticCidr` O (`staticIp` + `staticSubnet`)
- Richiede `staticGateway` (obbligatorio)
- Rimuove route default esistenti prima di aggiungere nuova route

### Side Effects (Scritture DB)
- **Nessuna scrittura DB diretta** in questo metodo
- Il metodo è puro networking (solo chiamate API MikroTik)
- Eventuali scritture DB avvengono a livello superiore (UseCase/ViewModel)

### Gestione Errori
- Wrappato in `safeApiCall { ... }` che ritorna `UiState<NetworkConfigFeedback>`
- Errori vengono convertiti in `UiState.Error(message)`
- Race condition su `addDhcpClient` gestita con retry

## Dipendenze Esterne
- `RouteManager` (iniettato nel constructor di AppRepository_legacy)
- `context: Context` (per stringhe localizzate)
- `serviceFactory: MikroTikServiceFactory` (per costruire il service)

## Note per Implementazione NetworkConfigRepositoryImpl
- Deve replicare esattamente questa sequenza
- Deve usare `MikroTikServiceProvider` invece di `buildServiceFor`
- Deve usare `RouteManager` (già disponibile via DI)
- Deve gestire le stringhe localizzate (serve Context o un modo per ottenere le stringhe)
- Deve ritornare `NetworkConfigFeedback` direttamente (non `UiState<NetworkConfigFeedback>`)

