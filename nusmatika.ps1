param(
    [ValidateSet('Setup','Apk','Run','Check','Publish','Bump')][string]$Action = 'Apk',
    [string]$JdkHome, [string]$SdkRoot, [string]$KeystorePath,
    [string]$KeyAlias = 'androiddebugkey', [string]$StorePassword, [string]$KeyPassword,
    [string]$DeviceSerial, [string]$RepositoryUrl, [string]$VersionName
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version 2.0

if ($Action -eq 'Publish') {
    & "$PSScriptRoot/scripts/Publish-GitHub.ps1" -ProjectRoot $PSScriptRoot -RepositoryUrl $RepositoryUrl
    exit 0
}
if ($Action -eq 'Bump') {
    & "$PSScriptRoot/scripts/Update-Version.ps1" -ProjectRoot $PSScriptRoot -VersionName $VersionName
    exit 0
}
. "$PSScriptRoot/scripts/Setup-Project.ps1"
$settings = Initialize-NusMatika -ProjectRoot $PSScriptRoot -JdkHome $JdkHome -SdkRoot $SdkRoot `
    -KeystorePath $KeystorePath -KeyAlias $KeyAlias -StorePassword $StorePassword -KeyPassword $KeyPassword
if ($Action -eq 'Setup') { exit 0 }

Push-Location -LiteralPath $PSScriptRoot
try {
    $tasks = if ($Action -eq 'Check') { @('testLogic','assembleDebug','lintDebug') } else { @('assembleDebug') }
    & "$PSScriptRoot/gradlew.bat" --console=plain @tasks
    if ($LASTEXITCODE -ne 0) { throw 'Build gagal. Periksa error Gradle di atas.' }
    $apk = Join-Path $PSScriptRoot 'app/build/outputs/apk/debug/app-debug.apk'
    Write-Host "APK: $apk"
    if ($Action -eq 'Run') {
        . "$PSScriptRoot/scripts/Install-App.ps1"
        Install-NusMatika -ProjectRoot $PSScriptRoot -SdkRoot $settings.SdkRoot `
            -JdkHome $settings.JdkHome -ApkPath $apk -DeviceSerial $DeviceSerial
    }
} finally { Pop-Location }
