#Requires -Version 5.1
<#
.SYNOPSIS
  Create pharmacy_delivery_demo, apply Flyway V1-V5, load fictional demo-seed.sql.
.PARAMETER Recreate
  DROP only pharmacy_delivery_demo. Never drops pharmacy_delivery.
#>
param(
  [string]$MysqlBin = "D:\DevelopTool\Language\MySQL\mysql-8.0.34-winx64\bin",
  [string]$HostName = "127.0.0.1",
  [int]$Port = 3306,
  [string]$User = "root",
  [string]$Password = $(if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { $env:MYSQL_PWD }),
  [string]$DemoDatabase = "pharmacy_delivery_demo",
  [string]$VerifyDatabase = "pharmacy_delivery_verify",
  [switch]$Recreate,
  [switch]$SkipVerifyDatabase
)

$ErrorActionPreference = "Stop"
if ([string]::IsNullOrWhiteSpace($Password)) {
  throw "Refusing empty password. Pass -Password or set DB_PASSWORD."
}
if ($User -eq "root" -and $Password -eq "root") {
  throw "Refusing insecure root/root credentials."
}
$forbidden = @("pharmacy_delivery", "prod", "production", "mysql", "sys", "information_schema", "performance_schema")
if ($forbidden -contains $DemoDatabase) {
  throw "Refusing to initialize database name: $DemoDatabase"
}

$mysql = Join-Path $MysqlBin "mysql.exe"
if (-not (Test-Path $mysql)) {
  throw "mysql.exe not found: $mysql"
}

$RepoRoot = Split-Path -Parent $PSScriptRoot
$MigrationDir = Join-Path $RepoRoot "backend\src\main\resources\db\migration"
$SeedFile = Join-Path $PSScriptRoot "demo-seed.sql"

function Invoke-Mysql {
  param([string]$Sql, [string]$Database = "")
  $args = @(
    "--host=$HostName", "--port=$Port", "--user=$User", "--password=$Password",
    "--ssl-mode=DISABLED", "--get-server-public-key", "--default-character-set=utf8mb4"
  )
  if ($Database) { $args += $Database }
  $args += @("--execute", $Sql)
  & $mysql @args
  if ($LASTEXITCODE -ne 0) { throw "mysql failed: $Sql" }
}

function Invoke-MysqlFile {
  param([string]$File, [string]$Database)
  # Byte-redirect so UTF-8 SQL is not re-encoded by PowerShell pipelines.
  $cmd = "`"$mysql`" --host=$HostName --port=$Port --user=$User --password=$Password --ssl-mode=DISABLED --get-server-public-key --default-character-set=utf8mb4 `"$Database`" < `"$File`""
  cmd.exe /c $cmd
  if ($LASTEXITCODE -ne 0) { throw "mysql failed sourcing $File" }
}

Write-Host "Initializing $DemoDatabase on ${HostName}:$Port"

if ($Recreate) {
  Write-Host "Dropping $DemoDatabase (pharmacy_delivery is not touched)"
  Invoke-Mysql "DROP DATABASE IF EXISTS ``$DemoDatabase``;"
}

Invoke-Mysql "CREATE DATABASE IF NOT EXISTS ``$DemoDatabase`` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"

$migrations = @(
  "V1__legacy_baseline.sql",
  "V2__enterprise_upgrade.sql",
  "V3__modulith_event_publication.sql",
  "V4__inventory_count_reconciliation.sql",
  "V5__performance_indexes.sql"
)
foreach ($name in $migrations) {
  $path = Join-Path $MigrationDir $name
  Write-Host "Applying $name"
  Invoke-MysqlFile -File $path -Database $DemoDatabase
}

Invoke-Mysql @"
CREATE TABLE IF NOT EXISTS flyway_schema_history (
  installed_rank INT NOT NULL,
  version VARCHAR(50) NULL,
  description VARCHAR(200) NOT NULL,
  type VARCHAR(20) NOT NULL,
  script VARCHAR(1000) NOT NULL,
  checksum INT NULL,
  installed_by VARCHAR(100) NOT NULL,
  installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  execution_time INT NOT NULL,
  success TINYINT(1) NOT NULL,
  PRIMARY KEY (installed_rank),
  KEY flyway_schema_history_s_idx (success)
) ENGINE=InnoDB;
"@ $DemoDatabase

$already = & $mysql --host=$HostName --port=$Port --user=$User --password=$Password --ssl-mode=DISABLED --get-server-public-key --batch --skip-column-names $DemoDatabase --execute "SELECT COUNT(*) FROM flyway_schema_history;"
if ([int]$already -eq 0) {
  Invoke-Mysql @"
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES
(1,'1','legacy baseline','SQL','V1__legacy_baseline.sql',NULL,'demo-init',0,1),
(2,'2','enterprise upgrade','SQL','V2__enterprise_upgrade.sql',NULL,'demo-init',0,1),
(3,'3','modulith event publication','SQL','V3__modulith_event_publication.sql',NULL,'demo-init',0,1),
(4,'4','inventory count reconciliation','SQL','V4__inventory_count_reconciliation.sql',NULL,'demo-init',0,1),
(5,'5','performance indexes','SQL','V5__performance_indexes.sql',NULL,'demo-init',0,1);
"@ $DemoDatabase
}

$seedCount = & $mysql --host=$HostName --port=$Port --user=$User --password=$Password --ssl-mode=DISABLED --get-server-public-key --batch --skip-column-names $DemoDatabase --execute "SELECT COUNT(*) FROM sys_user WHERE username='admin';"
if ([int]$seedCount -eq 0) {
  Write-Host "Loading demo-seed.sql"
  Invoke-MysqlFile -File $SeedFile -Database $DemoDatabase
} else {
  Write-Host "Seed already present (admin exists); skip demo-seed.sql"
}

Write-Host "Sanity checks:"
Invoke-Mysql @"
SELECT 'sellable_sku' AS k, COUNT(*) AS v FROM medicine m
WHERE m.status=1 AND m.is_deleted=0 AND m.stock>0
UNION ALL
SELECT 'stock_mismatch', COUNT(*) FROM medicine m
WHERE m.stock <> (
  SELECT COALESCE(SUM(b.available_qty),0) FROM medicine_batch b
  WHERE b.medicine_id=m.id AND b.sellable=1 AND b.quality_status='QUALIFIED'
    AND b.expiry_date>CURRENT_DATE AND b.available_qty>0)
UNION ALL
SELECT 'admin_users', COUNT(*) FROM sys_user WHERE username IN ('admin','user01','pharmacist','purchaser','warehouse');
"@ $DemoDatabase

if (-not $SkipVerifyDatabase) {
  Write-Host "Ensuring empty $VerifyDatabase for mvn verify (no demo seed)"
  Invoke-Mysql "CREATE DATABASE IF NOT EXISTS ``$VerifyDatabase`` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
}

Write-Host "Done. Start backend with profile demo, DB_PASSWORD set, Flyway disabled for this database."
