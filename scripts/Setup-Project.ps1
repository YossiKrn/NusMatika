# Pengaturan hanya pada proses ini dan workspace NusMatika; konfigurasi Flutter global tetap.
function ConvertTo-NusMatikaProperty([string]$Value) {
    $encoded = New-Object Text.StringBuilder
    foreach ($character in $Value.ToCharArray()) {
        $code = [int]$character
        if ($code -lt 32 -or $code -gt 126) { [void]$encoded.Append(('\u{0:x4}' -f $code)) }
        elseif ('\:=#! '.Contains([string]$character)) { [void]$encoded.Append('\').Append($character) }
        else { [void]$encoded.Append($character) }
    }
    return $encoded.ToString()
}

function Initialize-NusMatika {
    param([string]$ProjectRoot, [string]$JdkHome, [string]$SdkRoot, [string]$KeystorePath,
        [string]$KeyAlias = 'androiddebugkey', [string]$StorePassword, [string]$KeyPassword)
    $ErrorActionPreference = 'Stop'
    $candidates = @($JdkHome, $env:JAVA_HOME)
    if ($env:ProgramFiles) {
        $microsoft = Join-Path $env:ProgramFiles 'Microsoft'
        if (Test-Path -LiteralPath $microsoft) {
            $candidates += @(Get-ChildItem -LiteralPath $microsoft -Directory -Filter 'jdk-17*' |
                Sort-Object Name -Descending | ForEach-Object { $_.FullName })
        }
    }
    $chosenJdk = $null
    foreach ($candidate in $candidates) {
        if (-not $candidate) { continue }
        $java = Join-Path $candidate 'bin/java.exe'
        if (-not (Test-Path -LiteralPath $java)) { continue }
        # Invoke through ProcessStartInfo so Java's normal stderr version output is not a PS error.
        $process = New-Object Diagnostics.Process
        $process.StartInfo = New-Object Diagnostics.ProcessStartInfo($java, '-version')
        $process.StartInfo.UseShellExecute = $false
        $process.StartInfo.RedirectStandardError = $true
        $process.StartInfo.RedirectStandardOutput = $true
        [void]$process.Start()
        $version = $process.StandardError.ReadToEnd() + $process.StandardOutput.ReadToEnd()
        $process.WaitForExit()
        if ($process.ExitCode -eq 0 -and $version -match 'version "17[.\"]') { $chosenJdk = $candidate; break }
    }
    if (-not $chosenJdk) { throw 'JDK 17 tidak ditemukan. Gunakan -JdkHome "C:\Program Files\Microsoft\jdk-17...".' }
    $env:JAVA_HOME = [IO.Path]::GetFullPath($chosenJdk)
    $env:PATH = (Join-Path $env:JAVA_HOME 'bin') + [IO.Path]::PathSeparator + $env:PATH
    if (-not $SdkRoot) { $SdkRoot = $env:ANDROID_HOME }
    if (-not $SdkRoot) { $SdkRoot = $env:ANDROID_SDK_ROOT }
    if (-not $SdkRoot -and $env:LOCALAPPDATA) { $SdkRoot = Join-Path $env:LOCALAPPDATA 'Android/Sdk' }
    if (-not $SdkRoot -or -not (Test-Path -LiteralPath $SdkRoot)) {
        throw 'Android SDK tidak ditemukan. Gunakan -SdkRoot "C:\Users\YOSSI\AppData\Local\Android\Sdk".'
    }
    $SdkRoot = [IO.Path]::GetFullPath($SdkRoot)
    $env:ANDROID_HOME = $SdkRoot
    [IO.File]::WriteAllText((Join-Path $ProjectRoot 'local.properties'),
        'sdk.dir=' + (ConvertTo-NusMatikaProperty $SdkRoot.Replace('\','/')) + "`n", [Text.Encoding]::ASCII)

    $profileRoot = $env:USERPROFILE
    if (-not $profileRoot) { $profileRoot = [Environment]::GetFolderPath('UserProfile') }
    $privateRoot = Join-Path $profileRoot '.nusmatika/signing'
    [void][IO.Directory]::CreateDirectory($privateRoot)
    $record = Join-Path $privateRoot 'signing.json'
    $explicitImport = [bool]$KeystorePath
    if (-not $KeystorePath -and (Test-Path -LiteralPath $record)) {
        $key = Get-Content -LiteralPath $record -Raw | ConvertFrom-Json
    } else {
        if ($KeystorePath) {
            $KeystorePath = (Resolve-Path -LiteralPath $KeystorePath).Path
            if (-not $StorePassword) { $StorePassword = 'android' }
            if (-not $KeyPassword) { $KeyPassword = $StorePassword }
        } else {
            $gradleKey = Join-Path $profileRoot '.android/debug.keystore'
            $KeystorePath = Join-Path $privateRoot 'nusmatika.keystore'
            if (Test-Path -LiteralPath $KeystorePath) {
                throw "Key ada tetapi catatan password hilang: $record. Pulihkan cadangan atau berikan -KeystorePath dan password."
            }
            if (Test-Path -LiteralPath $gradleKey) {
                Copy-Item -LiteralPath $gradleKey -Destination $KeystorePath
                $KeyAlias = 'androiddebugkey'; $StorePassword = 'android'; $KeyPassword = 'android'
                Write-Host 'Kunci Gradle laptop dipertahankan dalam folder pribadi NusMatika.'
            } else {
                $KeyAlias = 'nusmatika'
                $StorePassword = [Guid]::NewGuid().ToString('N') + [Guid]::NewGuid().ToString('N')
                $KeyPassword = $StorePassword
                & (Join-Path $env:JAVA_HOME 'bin/keytool.exe') -genkeypair -storetype PKCS12 `
                    -keystore $KeystorePath -storepass $StorePassword -keypass $KeyPassword -alias $KeyAlias `
                    -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=NusMatika Development,O=NusMatika,C=ID'
                if ($LASTEXITCODE -ne 0) { throw 'Pembuatan kunci gagal.' }
                Write-Host 'Kunci baru dibuat untuk build berikutnya. Kunci ini bukan kunci APK V17 kiriman.'
            }
        }
        $key = [PSCustomObject]@{ storeFile=$KeystorePath; storePassword=$StorePassword; keyAlias=$KeyAlias; keyPassword=$KeyPassword }
        & (Join-Path $env:JAVA_HOME 'bin/keytool.exe') -list -keystore $key.storeFile `
            -storepass $key.storePassword -alias $key.keyAlias | Out-Null
        if ($LASTEXITCODE -ne 0) { throw 'Kunci tidak dapat dibaca. Periksa password dan alias.' }
        if ($explicitImport) {
            $digest = (Get-FileHash -LiteralPath $key.storeFile -Algorithm SHA256).Hash.ToLowerInvariant()
            $persistentKey = Join-Path $privateRoot ('imported-' + $digest.Substring(0,16) + '.keystore')
            if (-not (Test-Path -LiteralPath $persistentKey)) {
                Copy-Item -LiteralPath $key.storeFile -Destination $persistentKey
            }
            $key.storeFile = $persistentKey
        }
        if (Test-Path -LiteralPath $record) {
            Copy-Item -LiteralPath $record -Destination ($record + '.backup-' + (Get-Date -Format 'yyyyMMddHHmmssfff'))
        }
        [IO.File]::WriteAllText($record, ($key | ConvertTo-Json), [Text.Encoding]::UTF8)
    }
    if (-not (Test-Path -LiteralPath $key.storeFile)) { throw 'Kunci penandatanganan hilang. Pulihkan cadangan; jangan membuat pengganti.' }
    $lines = foreach ($name in @('storeFile','storePassword','keyAlias','keyPassword')) {
        $value = [string]$key.$name
        if ($name -eq 'storeFile') { $value = $value.Replace('\','/') }
        $name + '=' + (ConvertTo-NusMatikaProperty $value)
    }
    [IO.File]::WriteAllText((Join-Path $ProjectRoot 'signing.properties'), ($lines -join "`n") + "`n", [Text.Encoding]::ASCII)
    $editorRoot = Join-Path $ProjectRoot '.vscode'
    [void][IO.Directory]::CreateDirectory($editorRoot)
    $editorFile = Join-Path $editorRoot 'settings.json'
    $editor = if (Test-Path -LiteralPath $editorFile) { Get-Content -LiteralPath $editorFile -Raw | ConvertFrom-Json } else { [PSCustomObject]@{} }
    $editor | Add-Member NoteProperty 'java.import.gradle.java.home' $env:JAVA_HOME -Force
    if (-not $editor.PSObject.Properties['terminal.integrated.env.windows']) {
        $editor | Add-Member NoteProperty 'terminal.integrated.env.windows' ([PSCustomObject]@{})
    }
    $editor.'terminal.integrated.env.windows' | Add-Member NoteProperty JAVA_HOME $env:JAVA_HOME -Force
    [IO.File]::WriteAllText($editorFile, ($editor | ConvertTo-Json -Depth 10), [Text.Encoding]::UTF8)
    Write-Host "JDK 17: $env:JAVA_HOME"
    Write-Host "SDK: $SdkRoot"
    Write-Host "Cadangkan seluruh folder kunci pribadi: $privateRoot"
    return [PSCustomObject]@{ JdkHome=$env:JAVA_HOME; SdkRoot=$SdkRoot; ProjectRoot=$ProjectRoot }
}
