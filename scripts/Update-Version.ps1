param([string]$ProjectRoot = (Split-Path $PSScriptRoot -Parent), [string]$VersionName)
$ErrorActionPreference = 'Stop'
if (-not $VersionName -or $VersionName -notmatch '^\d+\.\d+\.\d+$') {
    throw 'Isi nama versi, misalnya: -Action Bump -VersionName 0.14.1'
}
$path = Join-Path $ProjectRoot 'version.properties'
$text = Get-Content -LiteralPath $path -Raw
if ($text -notmatch '(?m)^versionCode=(\d+)\s*$') { throw 'versionCode tidak valid.' }
$next = [int]$Matches[1] + 1
if ($next -gt 2100000000) { throw 'versionCode melebihi batas Android.' }
[IO.File]::WriteAllText($path, "versionCode=$next`nversionName=$VersionName`n", [Text.Encoding]::ASCII)
Write-Host "Versi diperbarui: $VersionName, versionCode $next. ID aplikasi dan signing key tetap."
