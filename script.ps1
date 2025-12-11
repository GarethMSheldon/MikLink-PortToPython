# Esegui dalla radice del repo
$sourceFiles = Get-ChildItem -Path .\app\src\main\java -Recurse -Include *.kt, *.java
$pattern = '^\s*(?:public\s+|internal\s+|private\s+)?(?:sealed\s+class|data\s+class|class|interface|object|enum\s+class)\s+([A-Za-z0-9_]+)'
$out=[]

foreach ($f in $sourceFiles) {
  $content = Get-Content $f.FullName -Raw
  $matches = [regex]::Matches($content, $pattern, 'Multiline')
  foreach ($m in $matches) {
    $symbol = $m.Groups[1].Value
    # Skip common / generic names
    if ($symbol -in @("Success","Error","Idle","Loading","Companion")) { continue }
    $usageStr = git grep -n -w $symbol 2>$null
    $usageCount = 0
    if ($LASTEXITCODE -eq 0 -and $usageStr) { $usageCount = ($usageStr -split "`n").Count }
    $out += [pscustomobject]@{ File = $f.FullName; Symbol = $symbol; Usages = $usageCount }
  }
}
$unused = $out | Where-Object { $_.Usages -le 1 } | Sort-Object Usages, File
$unused | Export-Csv -Path unused-symbols-report.csv -NoTypeInformation