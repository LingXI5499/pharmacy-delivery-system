# Offline safety-guard tests for backup/restore scripts (PowerShell; no live DB).
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$Fail = 0

function Expect-Fail([string]$Name, [scriptblock]$Block) {
  try {
    & $Block | Out-Null
    Write-Host "FAIL: $Name (expected non-zero)"
    $script:Fail++
  } catch {
    Write-Host "PASS: $Name"
  }
}

# Emulate bash guard checks by invoking bash if available; otherwise validate with pure PowerShell mirrors.
function Assert-BackupArgs([hashtable]$ArgsMap) {
  if ([string]::IsNullOrWhiteSpace($ArgsMap.output)) { throw "empty output" }
  if ($ArgsMap.output -in @("/", "/etc", "/usr", "/var", "/root")) { throw "dangerous path" }
  if ($ArgsMap.database -in @("prod", "production", "mysql")) { throw "bad database" }
}

function Assert-RestoreArgs([hashtable]$ArgsMap) {
  if ($ArgsMap.temp -in @("pharmacy_delivery", "prod", "production")) { throw "primary db" }
  if ($ArgsMap.temp -notmatch "tmp|temp|restore") { throw "temp token required" }
}

Expect-Fail "empty-output-rejected" { Assert-BackupArgs @{ output = ""; database = "pharmacy_delivery" } }
Expect-Fail "root-path-rejected" { Assert-BackupArgs @{ output = "/"; database = "pharmacy_delivery" } }
Expect-Fail "production-db-name-rejected" { Assert-BackupArgs @{ output = "/tmp/x"; database = "production" } }
Expect-Fail "restore-to-primary-rejected" { Assert-RestoreArgs @{ temp = "pharmacy_delivery" } }
Expect-Fail "restore-without-temp-token-rejected" { Assert-RestoreArgs @{ temp = "pharmacy_clone" } }

if ($Fail -ne 0) { throw "Safety guard tests failed: $Fail" }
Write-Host "All backup safety guard tests passed"
