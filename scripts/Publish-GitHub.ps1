param([string]$ProjectRoot = (Split-Path $PSScriptRoot -Parent), [string]$RepositoryUrl)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version 2.0
if (-not (Get-Command git -ErrorAction SilentlyContinue)) { throw 'Git belum tersedia. Instal Git for Windows, lalu buka ulang terminal.' }
if (-not $RepositoryUrl) { $RepositoryUrl = Read-Host 'Tempel URL repository GitHub NusMatika' }
if (-not $RepositoryUrl) { throw 'URL repository wajib diisi.' }
function Invoke-ProjectGit {
    param([string[]]$Arguments)
    & git @Arguments
    if ($LASTEXITCODE -ne 0) { throw ('Git gagal: ' + ($Arguments -join ' ')) }
}
Push-Location -LiteralPath $ProjectRoot
try {
    # ZIP tidak membawa .git. Repository lama yang sudah ada tidak diinisialisasi ulang.
    if (-not (Test-Path -LiteralPath '.git')) { Invoke-ProjectGit -Arguments @('init','-b','main') }
    $remotes = @(& git remote)
    $origin = if ($remotes -contains 'origin') { (& git remote get-url origin).Trim() } else { '' }
    if (-not $origin) { Invoke-ProjectGit -Arguments @('remote','add','origin',$RepositoryUrl) }
    elseif ($origin.Trim().TrimEnd('/') -ne $RepositoryUrl.Trim().TrimEnd('/')) {
        throw 'Origin menunjuk repository berbeda. Periksa git remote -v; remote tidak diganti otomatis.'
    }
    Invoke-ProjectGit -Arguments @('fetch','origin')
    $remoteHead = (& git ls-remote --symref origin HEAD) -join "`n"
    if ($LASTEXITCODE -ne 0) { throw 'Repository tidak dapat dibaca. Periksa URL dan login GitHub.' }
    $branch = 'main'
    if ($remoteHead -match 'ref: refs/heads/([^\s]+)\s+HEAD') { $branch = $Matches[1] }
    & git rev-parse --verify --quiet HEAD *> $null
    $hasLocalCommit = $LASTEXITCODE -eq 0
    & git show-ref --verify --quiet ('refs/remotes/origin/' + $branch)
    $hasRemoteBranch = $LASTEXITCODE -eq 0
    if (-not $hasLocalCommit) {
        Invoke-ProjectGit -Arguments @('symbolic-ref','HEAD',('refs/heads/' + $branch))
        if ($hasRemoteBranch) {
            $remoteFiles = @(& git ls-tree -r --name-only ('origin/' + $branch))
            if ($LASTEXITCODE -ne 0) { throw 'Isi repository tidak dapat dibaca.' }
            $unknown = @($remoteFiles | Where-Object { $_ -notmatch '^(?i:readme(?:\.[^/]+)?|license(?:\.[^/]+)?|\.gitignore)$' })
            if ($unknown.Count -gt 0) {
                throw 'Repository berisi kode selain README/license/gitignore. Clone repository itu, salin proyek ini ke dalamnya, lalu jalankan Publish lagi.'
            }
            # Mengambil riwayat README awal tanpa mengganti file proyek yang sudah diekstrak.
            Invoke-ProjectGit -Arguments @('reset','--mixed',('origin/' + $branch))
        }
    } else {
        $localBranch = (& git symbolic-ref --short HEAD).Trim()
        if ($LASTEXITCODE -ne 0) { throw 'Pilih branch terlebih dahulu; HEAD sedang detached.' }
        if ($localBranch -ne $branch) { throw "Branch lokal $localBranch berbeda dari $branch. Pilih branch tujuan sebelum Publish." }
        if ($hasRemoteBranch) {
            & git merge-base --is-ancestor ('origin/' + $branch) HEAD
            if ($LASTEXITCODE -ne 0) { throw 'Ada commit remote yang belum ada lokal. Lakukan git pull dan selesaikan penggabungan dahulu.' }
        }
    }
    $author = & git config user.name
    if (-not $author) {
        $author = Read-Host 'Nama penulis commit'
        if (-not $author) { throw 'Nama commit wajib diisi.' }
        Invoke-ProjectGit -Arguments @('config','--local','user.name',$author)
    }
    $email = & git config user.email
    if (-not $email) {
        $email = Read-Host 'Email commit GitHub, boleh email noreply GitHub'
        if (-not $email) { throw 'Email commit wajib diisi.' }
        Invoke-ProjectGit -Arguments @('config','--local','user.email',$email)
    }
    Invoke-ProjectGit -Arguments @('add','.')
    $staged = @(& git diff --cached --name-only)
    $privateFiles = @($staged | Where-Object { $_ -match '(^|/)(signing|keystore)\.properties$|\.(keystore|jks|p12|pfx|pem|key)$|(^|/)local\.properties$' })
    if ($privateFiles.Count -gt 0) { throw 'Ada konfigurasi/kunci pribadi yang ikut staged. Keluarkan dari Git sebelum publish.' }
    if ($staged.Count -gt 0) {
        Invoke-ProjectGit -Arguments @('commit','-m','Integrate four NusMatika games and consistent app update configuration')
    }
    Invoke-ProjectGit -Arguments @('push','-u','origin',$branch)
    Write-Host 'Seluruh kode proyek berhasil dipush. Riwayat README awal tetap dipertahankan.'
} finally { Pop-Location }
