param(
  [string]$Root = ".",
  [string]$OutDir = "docs\_inventory"
)

$ErrorActionPreference = "Stop"
$RootPath = (Resolve-Path $Root).Path
$OutPath = Join-Path $RootPath $OutDir

New-Item -ItemType Directory -Force -Path $OutPath | Out-Null

# Exclusions
$ExcludeFragments = @(
  "\.git\",
  "\.gradle\",
  "\.idea\",
  "\build\",
  "\app\build\",
  "\app\release\"
)

function Should-Exclude([string]$fullPath) {
  foreach ($frag in $ExcludeFragments) {
    if ($fullPath -like "*$frag*") { return $true }
  }
  return $false
}

# ---------- TREE (ASCII) ----------
$TreeFile = Join-Path $OutPath "tree.txt"
Set-Content -LiteralPath $TreeFile -Value ("Tree for: " + $RootPath) -Encoding UTF8

function Write-Tree([string]$path, [string]$prefix) {
  $items = Get-ChildItem -LiteralPath $path | Sort-Object -Property PSIsContainer, Name

  $visible = @()
  foreach ($it in $items) {
    if (-not (Should-Exclude $it.FullName)) { $visible += $it }
  }

  for ($i = 0; $i -lt $visible.Count; $i++) {
    $item = $visible[$i]
    $isLast = ($i -eq ($visible.Count - 1))

    $branch = ""
    if ($isLast) { $branch = "+-- " } else { $branch = "|-- " }

    $nextPrefix = $prefix
    if ($isLast) { $nextPrefix += "    " } else { $nextPrefix += "|   " }

    Add-Content -LiteralPath $TreeFile -Value ($prefix + $branch + $item.Name)

    if ($item.PSIsContainer) {
      Write-Tree -path $item.FullName -prefix $nextPrefix
    }
  }
}

Write-Tree -path $RootPath -prefix ""

# ---------- KOTLIN INVENTORY ----------
$CsvFile = Join-Path $OutPath "kotlin_inventory.csv"

$KtFiles = Get-ChildItem -Path (Join-Path $RootPath "app\src") -Recurse -Filter *.kt |
  Where-Object { -not (Should-Exclude $_.FullName) }

$rows = foreach ($f in $KtFiles) {
  $content = Get-Content -LiteralPath $f.FullName
  $packageLine = ($content | Select-String -Pattern "^\s*package\s+" | Select-Object -First 1)
  $package = ""
  if ($packageLine) { $package = ($packageLine.Line -replace "^\s*package\s+","") }

  $symbols = ($content |
    Select-String -Pattern "^\s*(data\s+)?(class|interface|object)\s+([A-Za-z0-9_]+)" |
    Select-Object -First 3 |
    ForEach-Object { $_.Matches[0].Groups[3].Value }) -join "|"

  [pscustomobject]@{
    Path    = $f.FullName.Substring($RootPath.Length).TrimStart("\")
    Lines   = $content.Count
    Package = $package
    Symbols = $symbols
  }
}

$rows | Export-Csv -LiteralPath $CsvFile -NoTypeInformation -Encoding UTF8

# ---------- VIOLATIONS ----------
$ViolFile = Join-Path $OutPath "violations.txt"
Set-Content -LiteralPath $ViolFile -Value "" -Encoding UTF8

function Scan([string]$basePath, [string]$pattern, [string]$label) {
  Add-Content -LiteralPath $ViolFile -Value ("=== " + $label + " | " + $pattern + " ===")

  if (-not (Test-Path $basePath)) {
    Add-Content -LiteralPath $ViolFile -Value ("(path not found) " + $basePath)
    Add-Content -LiteralPath $ViolFile -Value ""
    return
  }

  $files = Get-ChildItem -Path $basePath -Recurse -Filter *.kt | Where-Object { -not (Should-Exclude $_.FullName) }
  foreach ($f in $files) {
    $hits = Select-String -LiteralPath $f.FullName -Pattern $pattern -ErrorAction SilentlyContinue
    foreach ($h in $hits) {
      $rel = $f.FullName.Substring($RootPath.Length).TrimStart("\")
      Add-Content -LiteralPath $ViolFile -Value ("{0}:{1}:{2}" -f $rel, $h.LineNumber, $h.Line.Trim())
    }
  }

  Add-Content -LiteralPath $ViolFile -Value ""
}

$UiPath     = Join-Path $RootPath "app\src\main\java\com\app\miklink\ui"
$DomainPath = Join-Path $RootPath "app\src\main\java\com\app\miklink\core\domain"
$TestPath   = Join-Path $RootPath "app\src\test\java"

Scan $UiPath     "\.dao\."                     "UI imports DAO"
Scan $UiPath     "core\.data\.local\.room"     "UI imports Room"
Scan $UiPath     "core\.data\.remote\..*dto"   "UI imports remote DTO"
Scan $DomainPath "^import\s+android\."         "Domain imports android.*"
Scan $DomainPath "core\.data\.local\.room"     "Domain imports Room"
Scan $TestPath   "@Ignore"                     "Ignored tests"
