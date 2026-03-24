param(
  [string]$MavenProfile = "local",
  [string]$NacosHost = "zoumh.com",
  [int]$NacosPort = 8848,
  [string]$RedisHost = "zoumh.com",
  [int]$RedisPort = 6379,
  [string]$MysqlHost = "zoumh.com",
  [int]$MysqlPort = 3306
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$repo = Resolve-Path (Join-Path $root "..")

function Test-TcpPort {
  param(
    [string]$TargetHost,
    [int]$Port
  )
  try {
    $ok = Test-NetConnection -ComputerName $TargetHost -Port $Port -WarningAction SilentlyContinue
    return [bool]$ok.TcpTestSucceeded
  } catch {
    return $false
  }
}

Write-Host "Repo: $repo"
Write-Host "Maven profile: $MavenProfile"

if (-not (Test-TcpPort -TargetHost $NacosHost -Port $NacosPort)) {
  throw "Nacos not reachable: $NacosHost`:$NacosPort"
}
if (-not (Test-TcpPort -TargetHost $RedisHost -Port $RedisPort)) {
  throw "Redis not reachable: $RedisHost`:$RedisPort"
}
if (-not (Test-TcpPort -TargetHost $MysqlHost -Port $MysqlPort)) {
  throw "MySQL not reachable: $MysqlHost`:$MysqlPort"
}

$services = @(
  @{ Name = "ruoyi-system"; Path = "ruoyi-modules/ruoyi-system" },
  @{ Name = "ruoyi-auth"; Path = "ruoyi-auth" },
  @{ Name = "ruoyi-gateway"; Path = "ruoyi-gateway" },
  @{ Name = "ruoyi-file"; Path = "ruoyi-modules/ruoyi-file" }
)

foreach ($svc in $services) {
  $svcDir = Join-Path $repo $svc.Path
  Write-Host "Starting $($svc.Name) in $svcDir"
  Start-Process -FilePath "mvn" -ArgumentList "spring-boot:run","-P$MavenProfile","-DskipTests" -WorkingDirectory $svcDir
  Start-Sleep -Seconds 2
}

Write-Host "All services started in separate processes."
