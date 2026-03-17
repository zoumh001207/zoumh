param(
  [string]$Server = 'http://127.0.0.1:8848',
  [string]$Username = 'nacos',
  [string]$Password = 'zoumh',
  [string]$Group = 'DEFAULT_GROUP',
  [string]$Namespace = '',
  [switch]$Insecure
)

$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
if ($Insecure) {
  [System.Net.ServicePointManager]::ServerCertificateValidationCallback = { $true }
}
$baseDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$configDir = Join-Path $baseDir '..\nacos\phase1'
$serverBase = $Server.TrimEnd('/')
if ($serverBase.EndsWith('/nacos')) {
  $apiBase = $serverBase
} else {
  $apiBase = "$serverBase/nacos"
}

$items = @(
  @{ DataIds = @('application-dev.yml'); File = 'application-dev.yml'; Type = 'yaml' },
  @{ DataIds = @('gateway', 'gateway.yaml', 'gateway-dev.yaml', 'ruoyi-gateway-dev.yml'); File = 'gateway.yaml'; Type = 'yaml' },
  @{ DataIds = @('zoumh-auth', 'zoumh-auth.yml', 'zoumh-auth-dev.yml', 'ruoyi-auth-dev.yml'); File = 'zoumh-auth.yml'; Type = 'yaml' },
  @{ DataIds = @('system', 'system.yml', 'system-dev.yml'); File = 'system.yml'; Type = 'yaml' },
  @{ DataIds = @('zoumh-tools', 'zoumh-tools.yml', 'zoumh-tools-dev.yml'); File = 'zoumh-tools.yml'; Type = 'yaml' },
  @{ DataIds = @('zoumh-hotel-monitor', 'zoumh-hotel-monitor.yml', 'zoumh-hotel-monitor-dev.yml'); File = 'zoumh-hotel-monitor.yml'; Type = 'yaml' },
  @{ DataIds = @('ruoyi-gen', 'ruoyi-gen.yml', 'ruoyi-gen-dev.yml'); File = 'ruoyi-gen.yml'; Type = 'yaml' },
  @{ DataIds = @('ruoyi-file', 'ruoyi-file.yml', 'ruoyi-file-dev.yml'); File = 'ruoyi-file.yml'; Type = 'yaml' }
)

function Publish-Config {
  param([string]$DataId, [string]$FileName, [string]$Type)

  $path = Join-Path $configDir $FileName
  $args = @(
    '-sS',
    '-X', 'POST',
    "$apiBase/v1/cs/configs",
    '--data-urlencode', "dataId=$DataId",
    '--data-urlencode', "group=$Group",
    '--data-urlencode', "type=$Type",
    '--data-urlencode', "username=$Username",
    '--data-urlencode', "password=$Password",
    '--data-urlencode', "content@$path"
  )

  if (-not [string]::IsNullOrWhiteSpace($Namespace)) {
    $args += @('--data-urlencode', "tenant=$Namespace")
  }

  if ($apiBase.StartsWith('https://')) {
    $args = @('--ssl-no-revoke') + $args
  }

  $resp = & curl.exe @args
  if ($LASTEXITCODE -ne 0) {
    throw "curl push failed for dataId=$DataId"
  }
  $r = ($resp | Out-String).Trim()
  if ($r -ne "true") {
    throw "nacos rejected dataId=$DataId, response=$r"
  }
  Write-Host "[OK] $DataId <= $FileName :: $resp"
}

foreach ($item in $items) {
  foreach ($id in $item.DataIds) {
    Publish-Config -DataId $id -FileName $item.File -Type $item.Type
  }
}

Write-Host 'Done.'
