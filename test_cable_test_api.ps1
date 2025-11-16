# Script di test manuale per Cable-Test API MikroTik
# Obiettivo: Determinare se l'API è sincrona o asincrona

Write-Host "=== Test Cable-Test API MikroTik ===" -ForegroundColor Cyan
Write-Host ""

# Configurazione (MODIFICARE CON I TUOI DATI)
$RouterIP = "192.168.1.20"
$Username = "dot"
$Password = "Dotroot34!"  # Password vuota se non configurata
$Interface = "ether4"  # Interfaccia da testare

# Encoding credenziali Base64
$credentials = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${Username}:${Password}"))

# Preparare il payload JSON
$payload = @{
    numbers = $Interface
    duration = "40s"  # ESSENZIALE per evitare "Session closed"
} | ConvertTo-Json

Write-Host "Configurazione:" -ForegroundColor Yellow
Write-Host "  Router IP: $RouterIP"
Write-Host "  Username: $Username"
Write-Host "  Interface: $Interface"
Write-Host ""

# Salvare il payload in un file temporaneo (per evitare problemi di escape)
$tempFile = "$env:TEMP\cable-test.json"
$payload | Out-File -FilePath $tempFile -Encoding ASCII -NoNewline

Write-Host "Payload JSON:" -ForegroundColor Yellow
Write-Host $payload
Write-Host ""

Write-Host "Esecuzione cable-test..." -ForegroundColor Green
Write-Host "Endpoint: POST http://${RouterIP}/rest/interface/ethernet/cable-test"
Write-Host ""

# Misurare il tempo di esecuzione
$startTime = Get-Date

try {
    # Eseguire la chiamata API con verbose output
    $response = curl.exe -X POST "http://${RouterIP}/rest/interface/ethernet/cable-test" `
        -H "Authorization: Basic $credentials" `
        -H "Content-Type: application/json" `
        --data-binary "@$tempFile" `
        -w "\n\n--- HTTP Info ---\nHTTP Status: %{http_code}\nTime Total: %{time_total}s\nTime Connect: %{time_connect}s\n" `
        -s -S 2>&1

    $endTime = Get-Date
    $elapsed = ($endTime - $startTime).TotalSeconds

    Write-Host "=== RISPOSTA ===" -ForegroundColor Cyan
    Write-Host $response
    Write-Host ""
    Write-Host "Tempo totale: $elapsed secondi" -ForegroundColor Yellow
    Write-Host ""

    # Analisi del pattern
    Write-Host "=== ANALISI DEL PATTERN ===" -ForegroundColor Magenta

    # Verifica se c'era un errore "Session closed"
    if ($response -match "Session closed") {
        Write-Host "[ERRORE: SESSION CLOSED - RISOLTO]" -ForegroundColor Green
        Write-Host "  - Questo errore si verifica quando il comando impiega >60s"
        Write-Host "  - FIX: Parametro 'duration' aggiunto al payload"
        Write-Host "  - Rieseguire lo script per testare con duration=40s"
    } elseif ($elapsed -lt 2) {
        Write-Host "[PATTERN ASINCRONO PROBABILE]" -ForegroundColor Red
        Write-Host "  - Risposta ricevuta in meno di 2 secondi"
        Write-Host "  - L'API potrebbe ritornare immediatamente e richiedere polling"
        Write-Host "  - Verificare se la risposta contiene un job-id o status='running'"
    } elseif ($elapsed -lt 15) {
        Write-Host "[PATTERN SINCRONO VELOCE]" -ForegroundColor Yellow
        Write-Host "  - Risposta ricevuta in 2-15 secondi"
        Write-Host "  - L'API probabilmente attende il completamento prima di rispondere"
        Write-Host "  - Timeout attuale di 60s dovrebbe essere sufficiente"
    } else {
        Write-Host "[PATTERN SINCRONO LENTO]" -ForegroundColor Yellow
        Write-Host "  - Risposta ricevuta dopo più di 15 secondi"
        Write-Host "  - Con parametro duration=40s, il comando completa correttamente"
    }

    Write-Host ""
    Write-Host "=== PROSSIMI STEP ===" -ForegroundColor Green
    Write-Host "1. Verificare il contenuto della risposta JSON"
    Write-Host "2. Se la risposta contiene i dati completi del cable-test con cable-pairs:"
    Write-Host "   -> FIX IMPLEMENTATO: parametro duration=40s aggiunto"
    Write-Host "   -> Rebuild app e test certificazione completa"
    Write-Host "3. Se ancora errore 'Session closed':"
    Write-Host "   -> Aumentare duration a 60s o 90s"
    Write-Host ""

} catch {
    Write-Host "ERRORE durante l'esecuzione:" -ForegroundColor Red
    Write-Host $_.Exception.Message
    Write-Host ""
    Write-Host "Possibili cause:" -ForegroundColor Yellow
    Write-Host "  - Router non raggiungibile su $RouterIP"
    Write-Host "  - Credenziali errate"
    Write-Host "  - Interfaccia '$Interface' non esiste"
    Write-Host "  - Hardware non supporta cable-test (errore HTTP 500)"
}

# Cleanup
Remove-Item -Path $tempFile -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "Script completato." -ForegroundColor Cyan
Write-Host "Copia l'output completo e analizza la risposta JSON."

