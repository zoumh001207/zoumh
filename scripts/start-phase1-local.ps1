param(
  [string]$MavenProfile = "local"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$repo = Resolve-Path (Join-Path $root "..")

Write-Host "Repo: $repo"
Write-Host "Maven profile: $MavenProfile"

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
