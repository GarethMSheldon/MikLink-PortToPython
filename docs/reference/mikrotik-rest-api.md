# MikroTik REST API (RouterOS)

Questa pagina descrive la costruzione del service Retrofit e gli endpoint effettivamente presenti nella codebase.

## Base URL e HTTPS
- La URL base è costruita in `MikroTikServiceFactory.createService(probeConfig)`:
  - `scheme = https` se `ProbeConfig.isHttps = true`
  - `scheme = http` altrimenti
  - `baseUrl = "$scheme://${probe.ipAddress}/"`

## Autenticazione
- Basic Auth via header `Authorization: Basic <base64(user:pass)>`.
- L'header viene aggiunto da un interceptor locale nella factory quando `username` o `password` non sono blank.

## Trust-all (solo in HTTPS)
- Se `isHttps = true` la factory configura un SSLContext e un hostname verifier permissivi **solo per quel client**.
- Il trust-all disabilita la verifica del certificato e dell'hostname. **Non** risolve incompatibilità di protocollo/cipher: un TLS handshake non supportato genera `SSLHandshakeException`.
- In caso di handshake fallito, `ProbeConnectivityRepositoryImpl` tenta un fallback automatico in HTTP; se fallisce anche il fallback, mostra il messaggio: "HTTPS handshake failed: TLS/cipher incompatible or certificate rejected. Install a valid certificate on RouterOS or switch to HTTP."

## Binding Wi-Fi (quando disponibile)
- `MikroTikServiceProviderImpl` prova a trovare una rete Wi-Fi attiva e, se presente, passa la `socketFactory` alla factory.

## Endpoint
Definiti in `data/remote/mikrotik/service/MikroTikApiService.kt`:

| Metodo | Path | Funzione (default params) |
|---|---|---|
| POST | `/rest/system/resource/print` | `getSystemResource(ProplistRequest(["board-name"]))` |
| GET | `/rest/interface/ethernet` | `getEthernetInterfaces(".proplist"="name")` |
| GET | `/rest/ip/dhcp-client` | `getDhcpClientStatus(interface)` |
| POST | `/rest/ip/dhcp-client/add` | `addDhcpClient(DhcpClientAdd)` |
| POST | `/rest/ip/dhcp-client/enable` | `enableDhcpClient(NumbersRequest)` |
| POST | `/rest/ip/dhcp-client/disable` | `disableDhcpClient(NumbersRequest)` |
| GET | `/rest/ip/address` | `getIpAddresses(".proplist"=".id,address,interface")` |
| POST | `/rest/ip/address/add` | `addIpAddress(IpAddressAdd)` |
| POST | `/rest/ip/address/remove` | `removeIpAddress(NumbersRequest)` |
| GET | `/rest/ip/route` | `getRoutes(".proplist"=".id,dst-address,gateway")` |
| POST | `/rest/ip/route/add` | `addRoute(RouteAdd)` |
| POST | `/rest/ip/route/remove` | `removeRoute(NumbersRequest)` |
| POST | `/rest/interface/ethernet/cable-test` | `runCableTest(CableTestRequest)` |
| POST | `/rest/interface/ethernet/monitor` | `getLinkStatus(MonitorRequest)` |
| GET | `/rest/ip/neighbor` | `getIpNeighbors(interface, ".proplist"="identity,interface-name,system-caps-enabled,discovered-by,vlan-id,voice-vlan-id,poe-class,system-description,port-id")` |
| POST | `/rest/ping` | `runPing(PingRequest)` |
| POST | `/rest/tool/speed-test` | `runSpeedTest(SpeedTestRequest)` |

### Board name resilienza
- `/rest/system/resource/print` può restituire più entry e non tutte includono `board-name`.
- `ProbeConnectivityRepositoryImpl` usa `.proplist = ["board-name"]` per ridurre la risposta e prende la prima `board-name` non vuota; se nessuna entry la espone, usa `"Unknown Board"`.
- Coperto dai test `ProbeConnectivityRepositoryContractTest` e `ProbeStatusRepositoryContractTest`.

### HTTPS e TLS
- Il toggle HTTPS abilita il client trust-all (cert/hostname) solo per la sonda.
- Se la stretta di mano TLS fallisce (es. cipher/protocollo non supportato), il repository prova automaticamente HTTP; se anche il fallback fallisce, l'utente vede un messaggio esplicito che suggerisce di installare un certificato valido o usare HTTP. Il trust-all non modifica i cipher né forza protocolli deprecati.
