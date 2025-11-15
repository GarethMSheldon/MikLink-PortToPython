Param()
# Script PowerShell: esegue i tre comandi curl per MikroTik
# Modifica le variabili qui sotto prima dell'uso.

$mtIp  = "192.168.0.251"
$mtUser = "admin"
$mtPass = ""    # se password vuota lascia così
$iface = "ether1"
$baseUrl = "http://$mtIp"

Write-Host "Stampo dhcp-client per interface $iface..."
# Usa curl.exe se disponibile; fallback a Invoke-RestMethod con Header Authorization
if (Get-Command curl.exe -ErrorAction SilentlyContinue) {
    & curl.exe -v -u "$mtUser`:$mtPass" -X POST "$baseUrl/rest/ip/dhcp-client/print" `
        -H "Content-Type: application/x-www-form-urlencoded" `
        --data-urlencode "?.interface=$iface"
} else {
    $auth = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("$mtUser`:$mtPass"))
    Invoke-RestMethod -Uri "$baseUrl/rest/ip/dhcp-client/print" -Method Post `
        -Headers @{ Authorization = "Basic $auth" } `
        -ContentType "application/x-www-form-urlencoded" `
        -Body ( @{ "?.interface" = $iface } )
}

Write-Host ""
Write-Host "Disabilito l'interfaccia $iface..."
if (Get-Command curl.exe -ErrorAction SilentlyContinue) {
    & curl.exe -v -u "$mtUser`:$mtPass" -X POST "$baseUrl/rest/interface/disable" `
        -H "Content-Type: application/x-www-form-urlencoded" `
        --data-urlencode "numbers=$iface"
} else {
    $auth = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("$mtUser`:$mtPass"))
    Invoke-RestMethod -Uri "$baseUrl/rest/interface/disable" -Method Post `
        -Headers @{ Authorization = "Basic $auth" } `
        -ContentType "application/x-www-form-urlencoded" `
        -Body ( @{ numbers = $iface } )
}

Write-Host ""
Write-Host "Riabilito l'interfaccia $iface..."
if (Get-Command curl.exe -ErrorAction SilentlyContinue) {
    & curl.exe -v -u "$mtUser`:$mtPass" -X POST "$baseUrl/rest/interface/enable" `
        -H "Content-Type: application/x-www-form-urlencoded" `
        --data-urlencode "numbers=$iface"
} else {
    $auth = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("$mtUser`:$mtPass"))
    Invoke-RestMethod -Uri "$baseUrl/rest/interface/enable" -Method Post `
        -Headers @{ Authorization = "Basic $auth" } `
        -ContentType "application/x-www-form-urlencoded" `
        -Body ( @{ numbers = $iface } )
}

Write-Host "Fatto."

