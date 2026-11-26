# Identitas dan pembaruan NusMatika

## Ketentuan pembaruan

Pembaruan pada instalasi yang sama memerlukan ID aplikasi yang sama dan sertifikat penandatanganan yang cocok. `versionCode` rilis berikutnya harus meningkat. Nama file ZIP, nama folder proyek, dan nama file APK tidak menentukan identitas aplikasi.

- ID tetap: `id.nusmatika.dakon`.
- V17: versionCode17, versionName0.12.1; dua permainan.
- V19: versionCode19, versionName0.14.0; empat permainan, Petak Umpet dengan papan susun manual, serta Engklek.
- Debug dan release tidak memakai applicationIdSuffix.
- Progres disimpan menggunakan nama SharedPreferences lama; tidak ada rutinitas menghapus data pada update.

Script `Install-App.ps1` memeriksa ID APK, satu perangkat yang siap, APK yang sudah terpasang, sertifikat SHA256, dan nomor versinya. Jika cocok, dipakai `adb install -r` lalu dibuka WelcomeActivity. Jika tidak cocok, script berhenti sebelum install. Script ini tidak menghapus aplikasi maupun datanya.

## Keadaan APK V17 kiriman

APK V17 asli telah diperiksa dari paket serah-terima dua game. Sertifikat SHA256:

```text
b0cf5e3f953bf11b3c45c6a7dbb1939b35c53ed1fd1036a102fd159f2d32ef1c
```

Arsip sumber V17, paket serah-terima, dan arsip V18 yang tersedia tidak menyertakan keystore V17. APK-APK hasil build berikutnya menggunakan sertifikat lain. Kunci yang tersedia pada lingkungan pengembangan juga tidak cocok dengan sertifikat V17. Sertifikat publik di APK hanya bisa digunakan untuk pemeriksaan identitas; kunci privatnya tidak dapat dipulihkan dari sertifikat itu.

Karena itu, paket ini tidak menjanjikan update langsung pada instalasi APK V17 kiriman tanpa keystore aslinya. Bukti uji menunjukkan Android menolak sertifikat berbeda dan tetap mempertahankan aplikasi V17 beserta datanya. Jalur update dengan kunci cocok telah diuji menggunakan salinan V17 khusus pengujian yang ditandatangani ulang dengan kunci QA. Salinan uji itu tidak mengubah sertifikat pada instalasi V17 asli dan bukan solusi untuk mendapatkan kunci lama.

Jika masih memiliki folder build V17 dari komputer pembuat APK, cari `build/debug.keystore` atau cadangan `.keystore`/`.jks`. Script build V17 memakai alias `androiddebugkey` dan password pengembangan `android`; sertifikat tetap perlu dicocokkan, karena file bernama debug.keystore di komputer berbeda dapat mempunyai kunci berbeda.

Impor kunci yang benar secara lokal menggunakan `nusmatika.ps1 -Action Setup -KeystorePath ...`. File diimpor sebagai salinan privat di luar folder proyek. Sesudah itu Run memeriksa kecocokannya dengan aplikasi yang benar-benar terpasang.

## Menjaga pembaruan berikutnya

Setup menyimpan kunci dan metadata lokal di `%USERPROFILE%/.nusmatika/signing`. Jika tersedia kunci Gradle laptop, kunci itu disalin tanpa mengubah byte-nya. Jika belum ada kunci, hanya dibuat satu kunci pengembangan baru. Pindah folder proyek tidak membuat kunci baru karena folder privat digunakan kembali. Kunci yang hilang dari konfigurasi tersimpan menimbulkan error; tidak diam-diam diganti.

Cadangkan seluruh folder privat tersebut. Ketika pindah laptop, pulihkan foldernya dan gunakan Setup dengan KeystorePath bila lokasi kunci berubah. `signing.properties` pada proyek dan `.vscode/settings.json` dihasilkan lokal; keduanya tidak masuk Git. Jangan menyimpan keystore/password ke repository. Build release tersedia dengan konfigurasi lokal yang sama, tetapi keystore debug impor tetap merupakan kunci pengembangan. Persiapan rilis Google Play memerlukan pengelolaan kunci rilis tersendiri.

Gunakan Bump untuk menaikkan nomor versi sebelum membagikan APK berikutnya. Distribusi APK di GitHub Releases dan publikasi source code GitHub adalah dua hal berbeda. Paket ini menyiapkan publikasi source code; tidak mengunggah APK atau membuat release secara otomatis.

Referensi resmi:
- https://developer.android.com/studio/publish/app-signing
- https://developer.android.com/studio/publish/versioning
