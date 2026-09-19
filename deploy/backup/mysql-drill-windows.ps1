#Requires -Version 5.1
<#
.SYNOPSIS
  Logical backup of a teaching database, restore into a temporary schema, compare COUNT(*).
  Never restores into pharmacy_delivery.
#>
param(
  [string]$MysqlBin = "D:\DevelopTool\Language\MySQL\mysql-8.0.34-winx64\bin",
  [string]$HostName = "127.0.0.1",
  [int]$Port = 3306,
  [string]$User = "root",
  [string]$Password = $(if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { $env:MYSQL_PWD }),
  [string]$Database = "pharmacy_delivery_demo",
  [string]$TempDatabase = "pharmacy_delivery_restore_tmp",
  [string]$OutputDir,
  [switch]$KeepTemp
)

$ErrorActionPreference = "Stop"
if ([string]::IsNullOrWhiteSpace($Password)) { throw "Refusing empty password" }
if ($User -eq "root" -and $Password -eq "root") { throw "Refusing insecure root/root credentials" }
if ($Database -in @("prod", "production", "mysql", "sys", "information_schema", "performance_schema")) {
  throw "Refusing database name: $Database"
}
if ($TempDatabase -in @("", "pharmacy_delivery", "prod", "production", "mysql", "sys", "information_schema", "performance_schema")) {
  throw "Refusing restore target database: $TempDatabase"
}
if ($TempDatabase -notmatch "tmp|temp|restore") {
  throw "Temp database name must contain tmp, temp, or restore"
}
if ($TempDatabase -eq $Database) {
  throw "Restore target must differ from source"
}

$mysql = Join-Path $MysqlBin "mysql.exe"
$mysqldump = Join-Path $MysqlBin "mysqldump.exe"
if (-not (Test-Path $mysql)) { throw "mysql.exe not found" }
if (-not (Test-Path $mysqldump)) { throw "mysqldump.exe not found" }

$RepoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
if (-not $OutputDir) {
  $OutputDir = Join-Path $RepoRoot "deploy\backup\out"
}
$resolvedOut = [IO.Path]::GetFullPath($OutputDir)
$dangerous = @("C:\", "C:\Windows", "C:\Windows\System32", "/")
if ($dangerous -contains $resolvedOut.TrimEnd('\')) {
  throw "Refusing dangerous output path: $resolvedOut"
}

New-Item -ItemType Directory -Force -Path $resolvedOut | Out-Null
$stamp = [DateTime]::UtcNow.ToString("yyyyMMddTHHmmssZ")
$sqlFile = Join-Path $resolvedOut "${Database}_$stamp.sql"
$gzFile = Join-Path $resolvedOut "${Database}_$stamp.sql.gz"
$metaFile = Join-Path $resolvedOut "${Database}_$stamp.meta"

Write-Host "Dumping $Database"
& $mysqldump --host=$HostName --port=$Port --user=$User --password=$Password --ssl-mode=DISABLED --get-server-public-key `
  --single-transaction --routines --triggers --events --default-character-set=utf8mb4 `
  --result-file=$sqlFile $Database
if ($LASTEXITCODE -ne 0) { throw "mysqldump failed" }
if (-not (Test-Path $sqlFile) -or (Get-Item $sqlFile).Length -le 0) { throw "Backup missing or empty" }

function Compress-GZip([string]$InFile, [string]$OutFile) {
  $inStream = [IO.File]::OpenRead($InFile)
  try {
    $outStream = [IO.File]::Create($OutFile)
    try {
      $gzip = New-Object IO.Compression.GZipStream($outStream, [IO.Compression.CompressionMode]::Compress)
      try { $inStream.CopyTo($gzip) } finally { $gzip.Dispose() }
    } finally { $outStream.Dispose() }
  } finally { $inStream.Dispose() }
}

Compress-GZip $sqlFile $gzFile
Remove-Item $sqlFile -Force
$hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $gzFile).Hash.ToLowerInvariant()

$rowSql = "SELECT table_name, table_rows FROM information_schema.tables WHERE table_schema='$Database' ORDER BY table_name;"
$estimate = & $mysql --host=$HostName --port=$Port --user=$User --password=$Password --ssl-mode=DISABLED --get-server-public-key --batch --skip-column-names --execute $rowSql
@(
  "database=$Database"
  "created_at=$stamp"
  "backup_file=$(Split-Path $gzFile -Leaf)"
  "sha256=$hash"
  "table_row_estimates<<"
  $estimate
  ">>"
) | Set-Content -LiteralPath $metaFile -Encoding ascii

Write-Host "Backup written: $gzFile"
Write-Host "sha256=$hash"

$metaHash = (Select-String -Path $metaFile -Pattern '^sha256=').Line.Substring(7)
if ($metaHash -ne $hash) { throw "Checksum mismatch writing meta" }
Write-Host "Backup verification OK"

Write-Host "Restoring into $TempDatabase"
& $mysql --host=$HostName --port=$Port --user=$User --password=$Password --ssl-mode=DISABLED --get-server-public-key --execute "DROP DATABASE IF EXISTS ``$TempDatabase``; CREATE DATABASE ``$TempDatabase`` DEFAULT CHARACTER SET utf8mb4;"
if ($LASTEXITCODE -ne 0) { throw "create temp database failed" }

$tmpSql = Join-Path $resolvedOut "${TempDatabase}_restore.sql"
$gzIn2 = [IO.File]::OpenRead($gzFile)
try {
  $ungzip2 = New-Object IO.Compression.GZipStream($gzIn2, [IO.Compression.CompressionMode]::Decompress)
  try {
    $outSql = [IO.File]::Create($tmpSql)
    try { $ungzip2.CopyTo($outSql) } finally { $outSql.Dispose() }
  } finally { $ungzip2.Dispose() }
} finally { $gzIn2.Dispose() }

$cmd = "`"$mysql`" --host=$HostName --port=$Port --user=$User --password=$Password --ssl-mode=DISABLED --get-server-public-key --default-character-set=utf8mb4 `"$TempDatabase`" < `"$tmpSql`""
cmd.exe /c $cmd
if ($LASTEXITCODE -ne 0) { throw "restore failed" }
Remove-Item $tmpSql -Force

$tables = @("sys_user", "medicine", "medicine_batch", "inventory_ledger", "supplier", "purchase_order")
Write-Host "COUNT(*) source vs restore:"
$mismatch = 0
foreach ($t in $tables) {
  $src = [int](& $mysql --host=$HostName --port=$Port --user=$User --password=$Password --ssl-mode=DISABLED --get-server-public-key --batch --skip-column-names --execute "SELECT COUNT(*) FROM ``$t``;" $Database)
  $dst = [int](& $mysql --host=$HostName --port=$Port --user=$User --password=$Password --ssl-mode=DISABLED --get-server-public-key --batch --skip-column-names --execute "SELECT COUNT(*) FROM ``$t``;" $TempDatabase)
  Write-Host ("  {0}: source={1} restore={2}" -f $t, $src, $dst)
  if ($src -ne $dst) { $mismatch++ }
}
if ($mismatch -ne 0) { throw "Row count mismatch after restore: $mismatch table(s)" }

if (-not $KeepTemp) {
  & $mysql --host=$HostName --port=$Port --user=$User --password=$Password --ssl-mode=DISABLED --get-server-public-key --execute "DROP DATABASE IF EXISTS ``$TempDatabase``;"
  Write-Host "Dropped $TempDatabase after successful compare"
}

Write-Host "DRILL_OK backup=$gzFile sha256=$hash"
