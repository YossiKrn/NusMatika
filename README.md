# NusMatika

Aplikasi Android native Java untuk pembelajaran matematika SD melalui permainan tradisional dan pendekatan Niteni, Nirokke, Nambahi. Paket lengkap V19 / 0.14.0 berisi seluruh progres sebelumnya, dengan empat permainan yang terhubung pada menu utama.

| Permainan | Isi yang tersedia |
| --- | --- |
| Dakon | Kelipatan, faktor, KPK dan FPB; kegiatan 3N dan penyimpanan progres |
| Balap Karung | Jarak, kecepatan rata-rata dan waktu; balapan serta latihan JKW |
| Petak Umpet | Pencarian kubus/balok dan penyusunan bangun di papan terpisah; drag, putar, tempel antarsisi, dan lepaskan kembali |
| Engklek | Jaring-jaring kubus/balok, lipat-bentangkan, melengkapi/menguraikan, pola alternatif, penilaian alasan, dan soal kotak kado |

Engklek memiliki 14 kegiatan dalam tiga tahap. Pemeriksaan geometrinya mengenali 11 pola jaring-jaring kubus dan memeriksa ukuran bidang untuk balok 3 × 2 × 1. Cakupan ini belum mencakup seluruh permainan dan seluruh variasi materi dalam dokumen pemetaan CP.

## Mulai dari terminal VS Code Windows

Buka folder yang langsung berisi `gradlew.bat`, `app`, `scripts`, dan `nusmatika.ps1`.

```powershell
# Menyiapkan JDK17, lokasi SDK, konfigurasi workspace dan kunci lokal; lalu membuat APK.
powershell -NoProfile -ExecutionPolicy Bypass -File .\nusmatika.ps1 -Action Apk

# Pemeriksaan lengkap: 12 program uji JVM, APK, dan Android Lint.
powershell -NoProfile -ExecutionPolicy Bypass -File .\nusmatika.ps1 -Action Check

# Dengan satu emulator/HP siap, periksa kompatibilitas, pasang, lalu buka aplikasi.
powershell -NoProfile -ExecutionPolicy Bypass -File .\nusmatika.ps1 -Action Run
```

JDK 17 dan Android SDK yang sudah terpasang digunakan kembali. Script mencari Microsoft JDK17 pada Program Files, SDK dari ANDROID_HOME / ANDROID_SDK_ROOT atau `%LOCALAPPDATA%\Android\Sdk`. Jika lokasinya berbeda:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\nusmatika.ps1 -Action Apk -JdkHome "C:\Program Files\Microsoft\jdk-17.0.21.102-hotspot" -SdkRoot "C:\Users\YOSSI\AppData\Local\Android\Sdk"
```

Pengaturan JDK berlaku untuk proses script dan workspace NusMatika. Konfigurasi Java global dan konfigurasi Flutter tidak diubah. Pada build pertama Gradle dapat mengunduh dependensi dan komponen SDK yang diperlukan.

APK berada di `app\build\outputs\apk\debug\app-debug.apk`. Emulator tidak diperlukan untuk membuat APK.

## Repository GitHub yang sudah berisi README

Jalankan dari folder proyek yang baru diekstrak:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\nusmatika.ps1 -Action Publish
```

Tempel URL repository GitHub saat diminta, misalnya `https://github.com/NAMA_AKUN/NusMatika.git`. Login melalui alur Git jika diminta. Script mengambil commit README awal, menjadikannya dasar riwayat lokal tanpa mengganti file proyek, menambahkan seluruh progres sebagai commit berikutnya, lalu melakukan push ke branch default repository. Tidak memakai force push atau menghapus riwayat. Isi README terbaru menggantikan README awal pada versi kerja; README awal tetap dapat dilihat pada commit sebelumnya.

Nama/email commit diminta hanya bila Git belum dikonfigurasi; konfigurasi penulis disimpan lokal pada repository ini. Git untuk Windows harus sudah tersedia. Repository yang telah berisi kode selain README/license/gitignore dihentikan agar tidak tertimpa. Baca [panduan GitHub](docs/GITHUB.md) untuk keadaan itu.

Cache, build output, APK, konfigurasi SDK dan kunci privat dikecualikan dari Git. File `gradlew`, `gradlew.bat`, dan `gradle/wrapper` tetap disertakan.

## Pembaruan aplikasi dan batasan APK V17

ID tetap **id.nusmatika.dakon**, sama seperti V17. Nomor versi sekarang **19 / 0.14.0** dan tidak memakai suffix ID untuk debug/release. Kunci penandatanganan yang sama dipakai kembali pada build berikutnya.

**APK V17 kiriman lama mempunyai sertifikat berbeda, sedangkan keystore aslinya tidak ditemukan dalam arsip yang tersedia. Tanpa keystore itu, APK baru tidak bisa memperbarui instalasi V17 kiriman secara langsung. Mengganti nama APK atau menaikkan versi tidak menyelesaikan perbedaan kunci.** Script Run memeriksa sertifikat sebelum memasang dan berhenti jika tidak cocok, dengan instalasi/data lama tetap utuh. Script tidak menjalankan uninstall.

Jika keystore V17 asli tersedia, impor lokal melalui Setup; kuncinya tetap di komputer pemilik:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\nusmatika.ps1 -Action Setup -KeystorePath "D:\Cadangan\V17\debug.keystore" -KeyAlias androiddebugkey -StorePassword android -KeyPassword android
powershell -NoProfile -ExecutionPolicy Bypass -File .\nusmatika.ps1 -Action Run
```

Contoh password/alias di atas mengikuti script build pengembangan V17. Untuk kunci lain, gunakan password/alias kunci tersebut. Keystore pengganti yang baru dibuat tidak dapat menyamai sertifikat V17. Untuk mencoba seluruh progres sekarang, gunakan emulator/perangkat uji yang belum terpasang APK V17 kiriman.

Script Setup mempertahankan kunci Gradle laptop bila tersedia. Jika belum ada, dibuat kunci pengembangan baru sekali. Kunci dan catatannya disimpan di `%USERPROFILE%\.nusmatika\signing`, di luar proyek. **Cadangkan seluruh folder tersebut secara privat** agar build di komputer baru memakai kunci yang sama. Kunci privat dan password tidak disertakan dalam ZIP sumber/GitHub. Baca [panduan pembaruan](docs/UPDATE_APLIKASI.md).

## Membuat versi berikutnya

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\nusmatika.ps1 -Action Bump -VersionName 0.14.1
powershell -NoProfile -ExecutionPolicy Bypass -File .\nusmatika.ps1 -Action Apk
```

Perintah Bump menaikkan `versionCode` satu angka pada `version.properties`. Nomor versi hanya diatur di file tersebut; ID dan kunci tidak berubah. Pasang APK baru menggunakan Run atau kirim APK yang dibangun dengan kunci yang sama kepada pengguna. Ini pembaruan melalui APK, bukan layanan unduhan otomatis di aplikasi.

## Struktur kode

- `app/src/main/java/id/nusmatika/dakon`: class per permainan; state/model, penyimpanan, renderer, view dan activity terpisah.
- `app/src/main/assets`: font Nunito beserta lisensi OFL dan atlas karakter Nara.
- `tests/id/nusmatika/dakon`: pengujian logika tanpa dependensi JUnit Android.
- `scripts`: pengaturan lingkungan, pemeriksaan pemasangan, publikasi GitHub dan peningkatan versi.
- `docs`: panduan, catatan perubahan dan hasil pengujian. Laporan V18 adalah catatan historis.

Toolchain: JDK17, Gradle Wrapper 8.11.1, Android Gradle Plugin 8.9.2, compileSdk/targetSdk35, minSdk26 (Android8). Android Studio bisa memakai wrapper yang sama; untuk importer VS Code gunakan konfigurasi workspace yang dihasilkan Setup.
