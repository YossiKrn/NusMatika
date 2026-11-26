# Progres gabungan V19

Paket V19 menggabungkan aplikasi dua permainan dari V17 dengan seluruh progres Petak Umpet V18, perubahan papan susun manual, serta Engklek. Paket ini adalah snapshot source lengkap, bukan kumpulan patch yang harus ditempel per class.

- Dakon dan Balap Karung, worksheet JKW, serta aset dan font awal tetap tersedia.
- Petak Umpet memakai lima class papan susun/Activity yang dipisah: semua bangun temuan dapat diseret ke papan terpisah, diputar, disnap antarsisi dan dilepas kembali.
- Engklek dibagi ke sembilan class baru: Activity, MapActivity, Catalog, Net, Session, Store, BoardView, FoldView, Ui. Menu utama menghubungkan keempat permainan.
- Dua program uji tambahan untuk geometri Engklek dan papan susun kini termasuk pada task `testLogic` standar.
- ID aplikasi dipertahankan. Versi dipusatkan pada version.properties, kini19/0.14.0.
- Script Windows menggantikan build manual lama yang membutuhkan folder toolchain tertentu.
- Konfigurasi SDK/JDK workspace dibuat lokal. Kunci penandatanganan disimpan secara persisten di luar folder proyek.
- Pemeriksaan sebelum pemasangan mencegah penggantian aplikasi dengan sertifikat berbeda. Keterbatasan kunci V17 dijelaskan pada UPDATE_APLIKASI.md.
- Script GitHub mendukung repo yang baru berisi README tanpa force push atau hilangnya commit awal.

File source per class tetap tersedia pada app/src/main/java/id/nusmatika/dakon. APK/cache/kunci privat tidak disertakan dalam paket source untuk GitHub.
