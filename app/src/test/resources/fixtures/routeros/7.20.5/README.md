RouterOS: 7.20.5 (stable)

board-name: hAP ax^2

Comandi curl per le fixture:

curl -k --max-time 10 -u "$USER:$PASS" "https://$ROUTER/rest/system/resource?.proplist=architecture-name,board-name,version,uptime,free-memory,total-memory"

curl -k --max-time 10 -u "$USER:$PASS" "https://$ROUTER/rest/ip/neighbor"

curl -k --max-time 10 -u "$USER:$PASS" "https://$ROUTER/rest/interface/ethernet/monitor?interface=ether1&.proplist=rate,status,full-duplex"

curl -k --max-time 10 -u "$USER:$PASS" "https://$ROUTER/rest/interface/ethernet/cable-test?interface=ether1&numbers=1-2"

curl -k --max-time 10 -u "$USER:$PASS" "https://$ROUTER/rest/interface/bridge/host"

curl -k --max-time 10 -u "$USER:$PASS" "https://$ROUTER/rest/interface/bridge/port"

curl -k --max-time 10 -u "$USER:$PASS" "https://$ROUTER/rest/interface/bridge/vlan"

Nota comportamento reale osservato:

POST /interface/ethernet/cable-test:

- su link-ok può tornare solo {name,status} (nessuna misura)
- su no-link può tornare cable-pairs (es. open:4,...)

GET /interface/bridge/vlan nel caso attuale torna [] (quindi VLAN bridge non configurata / non disponibile)

Nota log filtering:

query .query topics~"interface" ha restituito [] nel tuo ambiente → filtro topic lato REST non affidabile, quindi filtro client-side.
