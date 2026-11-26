function Get-NusMatikaCertificate([string]$Java, [string]$Signer, [string]$Apk) {
    $output = & $Java -jar $Signer verify --print-certs $Apk
    if ($LASTEXITCODE -ne 0) { throw "Tanda tangan APK tidak valid: $Apk" }
    $match = [regex]::Match(($output -join "`n"), 'Signer #1 certificate SHA-256 digest: ([a-fA-F0-9]+)')
    if (-not $match.Success) { throw 'Sertifikat APK tidak dapat dibaca.' }
    return $match.Groups[1].Value.ToLowerInvariant()
}

function Install-NusMatika {
    param([string]$ProjectRoot, [string]$SdkRoot, [string]$JdkHome, [string]$ApkPath, [string]$DeviceSerial)
    $ErrorActionPreference = 'Stop'
    $package = 'id.nusmatika.dakon'
    $adb = Join-Path $SdkRoot 'platform-tools/adb.exe'
    $tools = Join-Path $SdkRoot 'build-tools/35.0.0'
    $aapt = Join-Path $tools 'aapt2.exe'
    $signer = Join-Path $tools 'lib/apksigner.jar'
    $java = Join-Path $JdkHome 'bin/java.exe'
    foreach ($required in @($adb,$aapt,$signer,$java,$ApkPath)) {
        if (-not (Test-Path -LiteralPath $required)) { throw "File diperlukan: $required" }
    }
    $badging = (& $aapt dump badging $ApkPath) -join "`n"
    if ($LASTEXITCODE -ne 0) { throw 'Identitas APK tidak dapat dibaca.' }
    if ($badging -notmatch "package: name='id\.nusmatika\.dakon' versionCode='(\d+)'") {
        throw 'Identitas aplikasi berubah. Build harus tetap memakai id.nusmatika.dakon.'
    }
    $newVersion = [int]$Matches[1]
    $devices = @(& $adb devices | Where-Object { $_ -match '^([^\s]+)\s+device$' } |
        ForEach-Object { ($_ -split '\s+')[0] })
    if (-not $DeviceSerial) {
        if ($devices.Count -ne 1) { throw 'Hubungkan tepat satu HP/emulator, atau tambahkan -DeviceSerial dari adb devices.' }
        $DeviceSerial = $devices[0]
    }
    if ($devices -notcontains $DeviceSerial) { throw 'Perangkat belum siap. Aktifkan USB debugging dan setujui izin di HP.' }
    $newCertificate = Get-NusMatikaCertificate $java $signer $ApkPath
    $installed = @(& $adb -s $DeviceSerial shell pm path $package | Where-Object { $_ -match '^package:' })
    if ($LASTEXITCODE -ne 0) { throw 'Status instalasi tidak dapat diperiksa; pemasangan dibatalkan.' }
    if ($installed.Count -gt 0) {
        $remoteApk = ($installed | Where-Object { $_ -match '/base\.apk\s*$' } | Select-Object -First 1)
        if (-not $remoteApk) { $remoteApk = $installed[0] }
        $remoteApk = $remoteApk.Substring('package:'.Length).Trim()
        $inspectionRoot = Join-Path $ProjectRoot 'build/update-check'
        [void][IO.Directory]::CreateDirectory($inspectionRoot)
        $oldApk = Join-Path $inspectionRoot 'installed.apk'
        & $adb -s $DeviceSerial pull $remoteApk $oldApk | Out-Host
        if ($LASTEXITCODE -ne 0) { throw 'APK terpasang tidak dapat diperiksa; pemasangan dibatalkan.' }
        $oldCertificate = Get-NusMatikaCertificate $java $signer $oldApk
        if ($newCertificate -ne $oldCertificate) {
            throw ("Kunci APK berbeda; aplikasi lama tetap dipertahankan. Sertifikat terpasang: " +
                $oldCertificate + ". Diperlukan keystore asli APK tersebut. " +
                'Baca docs/UPDATE_APLIKASI.md dan jalankan Setup dengan -KeystorePath. Tidak ada uninstall yang dijalankan.')
        }
        $oldBadging = (& $aapt dump badging $oldApk) -join "`n"
        if ($LASTEXITCODE -ne 0) { throw 'Versi aplikasi terpasang tidak dapat diperiksa.' }
        if ($oldBadging -notmatch "versionCode='(\d+)'") { throw 'Nomor versi aplikasi lama tidak ditemukan.' }
        if ([int]$Matches[1] -gt $newVersion) { throw 'Versi di HP lebih baru. Naikkan versionCode dengan -Action Bump sebelum build.' }
        Write-Host 'ID dan sertifikat cocok. Memasang pembaruan pada aplikasi yang sama.'
    }
    & $adb -s $DeviceSerial install -r $ApkPath
    if ($LASTEXITCODE -ne 0) { throw 'Pemasangan ditolak Android. Instalasi lama tidak dihapus.' }
    & $adb -s $DeviceSerial shell am start -n ($package + '/.WelcomeActivity')
    if ($LASTEXITCODE -ne 0) { throw 'APK terpasang, tetapi halaman awal tidak dapat dibuka.' }
}
