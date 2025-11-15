#!/usr/bin/env bash
# Script: esegue i tre comandi curl per MikroTik (Bash / WSL / Git Bash / macOS)
# Modifica le variabili qui sotto prima dell'uso.

MT_IP="192.168.0.251"
MT_USER="admin"
MT_PASS=""   # se password vuota lascia così
IFACE="ether1"
BASE_URL="http://${MT_IP}"

echo "Stampo dhcp-client per interface ${IFACE}..."
curl -v -u "${MT_USER}:${MT_PASS}" -X POST "${BASE_URL}/rest/ip/dhcp-client/print" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode "?.interface=${IFACE}"

echo
echo "Disabilito l'interfaccia ${IFACE}..."
curl -v -u "${MT_USER}:${MT_PASS}" -X POST "${BASE_URL}/rest/interface/disable" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode "numbers=${IFACE}"

echo
echo "Riabilito l'interfaccia ${IFACE}..."
curl -v -u "${MT_USER}:${MT_PASS}" -X POST "${BASE_URL}/rest/interface/enable" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode "numbers=${IFACE}"

echo "Fatto."

