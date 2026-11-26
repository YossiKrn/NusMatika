# Mengirim seluruh progres ke GitHub

## Repository baru yang sudah berisi README saja

1. Ekstrak ZIP. Buka terminal pada folder `nusmatika-android` yang berisi `nusmatika.ps1`.
2. Jalankan `powershell -NoProfile -ExecutionPolicy Bypass -File .\nusmatika.ps1 -Action Publish`.
3. Tempel URL repository NusMatika. Jika diminta, isi identitas penulis commit dan selesaikan login GitHub.

Script memakai Git yang terpasang, mengambil branch default remote, lalu menempatkan commit README sebagai dasar repo lokal. Semua file proyek yang baru diekstrak tetap dipertahankan sebagai working tree. File-file itu masuk commit berikutnya dan dipush biasa. Riwayat README tidak dihapus dan tidak ada force push.

Jika repository privat, akun Git yang dipakai harus berhak push. Tidak perlu menaruh password atau token ke dalam script, source code, atau URL repository. Login GitHub berjalan melalui mekanisme Git yang tersedia pada laptop.

## Jika repository ternyata sudah berisi kode

Script berhenti pada repo lokal baru agar tidak menghapus file remote yang tidak terdapat dalam ZIP. Clone repo tersebut, lalu salin isi folder proyek dari ZIP ke folder hasil clone; pertahankan folder `.git`, konfigurasi `local.properties`, dan konfigurasi signing lokal. Periksa perubahan menggunakan Source Control VS Code sebelum commit. Setelah itu jalankan Publish dari folder hasil clone.

Jika sedang bekerja pada branch selain branch default, script meminta pemilihan branch tujuan terlebih dahulu. Jika remote mempunyai commit baru yang belum digabung, selesaikan `git pull` sebelum menjalankan Publish kembali. Script tidak menjalankan reset pada repo lokal yang sudah punya commit.

## Isi yang masuk repository

Source Java, aset, resources, wrapper Gradle, tests, version.properties, script dan dokumentasi. Folder build/cache, SDK lokal, APK/ZIP, file keystore dan konfigurasi signing lokal dikecualikan. `.gitignore` tidak menghapus file rahasia yang terlanjur ada pada commit lama; script menolak bila file signing pribadi masih ikut staged.

Referensi: https://docs.github.com/en/migrations/importing-source-code/using-the-command-line-to-import-source-code/adding-locally-hosted-code-to-github
