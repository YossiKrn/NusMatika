# Pengujian paket lengkap V19

Tanggal: 26 November 2026. Source digabung dari V17 dua game, V18 Petak Umpet, papan susun manual, dan Engklek. Identitas aplikasi id.nusmatika.dakon, versionCode19 / versionName0.14.0.

## Build dan logika

`testLogic assembleDebug lintDebug`: BUILD SUCCESSFUL pada JDK17, Gradle8.11.1, AGP8.9.2, SDK35. Android Lint: 0 error; warning nonfatal bawaan dan alokasi geometri pada pratinjau lipatan masih ada.

Dua belas program uji JVM menjalankan 80.544 pemeriksaan. Termasuk uji lama Dakon/Balap/LKPD, 4.960 pemeriksaan Petak Umpet, 36.830 pemeriksaan papan susun dan 10.088 pemeriksaan Engklek. Engklek mengenali tepat11 pola valid dari35 hexomino bebas, ukuran sisi balok, seluruh alur sesi3N, validasi alasan dan pemulihan sesi.

## Script terminal dan Git

Script diparse dan dijalankan pada PowerShell7.4.6 Linux dengan konstruksi yang tersedia di Windows PowerShell5.1. Belum dieksekusi pada laptop Windows pengguna.

- Uji Git menggunakan remote bare lokal dengan README sebagai commit awal. Publish menghasilkan commit proyek berikutnya; commit README tetap merupakan ancestor, source mencapai remote, dan kunci lokal dikecualikan. Publish kedua memajukan repo yang sama.
- Bump menaikkan versionCode19 menjadi20 pada fixture pengujian; source paket tetap19.
- Setup pada dua folder proyek memakai ulang keystore Gradle yang sama byte demi byte.
- Impor eksplisit menyalin keystore ke folder privat di luar proyek dan mempertahankan seluruh byte kunci.
- Konfigurasi importer Gradle VSCode dihasilkan dengan JDK17.
- Installer diuji dengan jalurADB pengganti dan pembaca sertifikatAPK asli: sertifikat V17 yang tidak cocok dihentikan sebelum install; sertifikat cocok menghasilkan install-r, tanpa uninstall.

## Pemasangan pembaruan pada emulator

Emulator AOSP Android9/API28 terisolasi digunakan. Paket yang diuji adalah APK yang dibangun dari source lengkap ini.

| Skenario | Hasil |
| --- | --- |
| Memasang V17 kiriman asli, kemudian mencoba update dengan sertifikat berbeda | Android menolak UPDATE_INCOMPATIBLE |
| Keadaan setelah update ditolak | V17 masih terpasang; data SharedPreferences uji tetap utuh |
| Salinan V17 khusus pengujian yang ditandatangani ulang dengan kunciQA, kemudian APK V19 dengan kunciQA yang sama | Update berhasil menjadi versionCode19 |
| Data SharedPreferences uji setelah update dengan kunci cocok | Tetap identik tanpa uninstall |
| Jumlah paket NusMatika setelah update berhasil | Tepat satu: id.nusmatika.dakon |
| WelcomeActivity dan menu empat permainan setelah update | Terbuka |
| Log crash pada rangkaian uji | Tidak ditemukan FATAL EXCEPTION |

Penandatanganan ulang salinan V17 dilakukan khusus untuk menguji jalur pembaruan dengan kunci cocok. Ini tidak menyediakan kunci V17 asli dan tidak memungkinkan APK V19 memperbarui V17 kiriman yang sudah terpasang dengan kunci berbeda. Keterbatasan ini dijelaskan pada UPDATE_APLIKASI.md.

Logika dan UI Petak Umpet/Engklek juga telah diuji pada pekerjaan sebelumnya: drag, tempel dan lepas bangun; lipat-bentangkan contoh valid/gagal; pilihan kotak; penilaian alasan; pemulihan proses; serta latihan ulang tanpa menghilangkan tahap terbuka. Uji ini bukan pengujian HP fisik atau seluruh versi Android.
