# NusMatika — Android

Prototipe native Android offline versi 0.12.1. Halaman pembuka memakai ilustrasi dan animasi 2D dengan tombol **Mulai** menuju pilihan **Dakon** dan **Balap Karung**. Dakon membuka empat level dan Balap Karung tiga level, masing-masing pada map dari bawah ke atas. Android 8.0/API 26 atau lebih baru.

## Memasang

Salin `outputs/nusmatika-v17-jkw-rapi.apk` ke HP, buka berkasnya, lalu ikuti pemasangan Android. Bila diminta, izinkan pemasangan dari aplikasi pengelola berkas yang dipakai. APK ini ditandatangani dengan kunci pengembangan yang sama dan dapat memperbarui versi sebelumnya.

## Map Dakon

Map menampilkan jalur dari bawah ke atas. Level pertama berada paling bawah. Setelah tugas pada satu level diselesaikan, level berikutnya terbuka. Level yang selesai dapat diulang tanpa mengunci kembali level lainnya. Jika latihan ulang ditinggalkan, tombol Lanjutkan memulihkan pekerjaan yang belum selesai.

1. **Niteni:** anak mengetuk lubang, memilih dan menaruh biji, memakai Hapus, Urungkan, lalu Kosongkan. Setiap tugas memiliki satu arahan singkat dan sorotan alat atau lubang yang diperlukan.
2. **Nirokke:** empat contoh terbimbing: KPK 3 dan 4; FPB 12 dan 18; KPK 2, 3 dan 4; FPB 6, 12 dan 18. Penempatan biji disertai perkalian atau pembagian tanpa sisa. Anak mengamati lubang yang dimiliki bersama, lalu memilih kelipatan bersama terkecil atau faktor bersama terbesar. Pilihan salah mendapat petunjuk dan tidak menghapus susunan biji.
3. **Nambahi dua bilangan:** empat soal total: satu soal KPK dan satu FPB acak menggunakan papan kosong, lalu dua LKPD cerita tanpa papan.
4. **Nambahi tiga bilangan:** empat soal total dengan susunan yang sama untuk tiga bilangan. Biji biru, koral dan ungu menjadi penanda masing-masing bilangan.

Di kedua level Nambahi, tombol **Bantuan** di atas soal membuka petunjuk singkat tanpa mengisikan jawaban. Anak mengisi jawaban akhir dan boleh menuliskan cara menghitung pada LKPD. Pemeriksaan dilakukan setelah Periksa; susunan biji bebas tidak wajib menyerupai contoh.

Setelah soal terakhir selesai, halaman **Selesai** hanya menampilkan pengakuan selesai dan tombol kembali. Cerita, kolom Caramu dan jawaban lama tidak digambar lagi; data pekerjaan tetap tersimpan.

Semua bilangan dan hasil KPK pada soal papan dapat diwakili lubang 1–24. Papan tetap berisi 24 lubang; tidak ada soal dengan angka di atas rentang ini. Soal, urutan, biji, riwayat urungkan, isian dan catatan tersimpan ketika aplikasi ditinggalkan. Jawaban salah mempertahankan pekerjaan. Map hanya memiliki empat bulatan; level 4 terbuka setelah level 3 selesai.

Pembaruan mempertahankan status level v12 serta memigrasikan pekerjaan ke susunan soal baru. Contoh Nirokke tiga bilangan dapat dilanjutkan dari sesi lama yang telah menyelesaikan dua contoh. Progresnya memakai penyimpanan tersendiri; berkas progres Dakon versi lama dan Balap Karung dipertahankan. Tombol Kembali menuju map, lalu pemilih permainan. Animasi menghormati pengaturan gerak sistem.

## Buku pembuka

Buku tertutup selama 0,6 detik, lalu sampul dan lembarannya membuka ke sudut tiga perempat. Empat karakter muncul sekali: Bima dalam pose berputar, Raka melompat, Tari menunjuk, serta Nara sebagai tokoh utama di dalam karung. Wajah, ekspresi, proporsi, dan siluet keempatnya berbeda. Rig bahu–siku–tangan serta pinggul–lutut–sepatu menjaga anggota tubuh tetap menyatu. Ikonnya meliputi dakon, kelereng dengan sudut 60°, jaring-jaring kubus untuk Engklek, dan susunan bangun ruang untuk Petak Umpet. Tiga rumus jarak, waktu, dan kecepatan ikut mengambang.

Pembukaan terjadi sekali. Sesudahnya gerak tubuh, kedipan, halaman buku, dan benda mengambang mengikuti loop 12 detik. Karakter memiliki jeda pose dengan perpindahan smootherstep, gerak kepala/badan yang mengikuti tangan dengan selang 280 ms, dan track tatapan/mulut tersendiri. Seluruh adegan pembuka digambar dengan vektor native dan diperbarui mengikuti vsync layar; tidak menggunakan atlas 12 fps milik layar Dakon. Tombol **Mulai** dapat ditekan sejak awal. Tombol Kembali dari pemilih permainan melanjutkan buku yang sudah terbuka. Animasi berhenti ketika aplikasi berada di latar belakang. Jika animasi sistem dinonaktifkan, buku dan seluruh isi langsung tampil diam.

Ikon permainan ketiga sampai kelima hanya ilustrasi pembuka; permainan yang tersedia tetap Dakon dan Balap Karung.

Pada v10, Raka melayang menyamping, satu kaki terentang ke kanan dengan sepatu tegak dan satu terlipat di bawah pinggul. Ujung sepatu kaki yang terlipat terlihat di sebelah kiri. Tari memakai tunik berlengan panjang sampai pergelangan, dengan hanya tangan yang terbuka; wajahnya tersenyum ceria dan menatap ke arah tangan yang menunjuk. Bentuk tetap berupa bidang warna 2D. Animasi tubuh, kedipan dan pergantian tatapan tetap mengikuti loop 12 detik. Bima, Nara dan ikon dakon memakai susunan v8.

Versi 3D v7 disimpan terpisah pada APK, arsip sumber, dan video bernama v7 di `outputs/`. Kode/aset 3D tidak masuk APK atau sumber aktif v17. Hash arsipnya tercatat pada `outputs/nusmatika-versi-3d-v7-checkpoint.json`.

## Balap Karung

Pemilih permainan sekarang membuka map tiga level dengan bentuk bulatan, jalur biru, banner hijau, dan gembok yang sama seperti map Dakon. Setiap level berhenti pada halaman selesai dan tombol **Kembali ke jalur**. Menyelesaikan tahap membuka level berikutnya. Level yang selesai dapat diulang tanpa mengunci level lain; pekerjaan latihan ulang dapat dilanjutkan.

Sesi dan pilihan suara BK1 dibaca dari penyimpanan Balap lama, lalu dimigrasikan ke penyimpanan map tersendiri (`nusmatika-balap-learning`). Progres lama tidak dihapus atau ditimpa. Level yang sudah terlewati tetap terbuka dan jawaban pada tahap yang belum selesai dipertahankan.

Arena menggunakan warna tanah/pasir, garis kapur, dan detail batu kecil yang datar. Terdapat ruang putih 16 dp antara tepi bawah arena dengan kartu jarak, waktu, dan kecepatan.

- **Niteni:** Garis merah mendatar untuk pembagian dikenalkan terlebih dahulu, lalu garis biru tegak untuk perkalian. Garis, panah dan nama operasi muncul dengan animasi. Tiga contoh J/K/W menyusul dengan huruf yang dicari langsung tidak ditampilkan sejak awal karena sedang dicari. Penjelasan bawah diketik bertahap: besaran yang dicari, operasi pada dua huruf yang tersisa, lalu perhitungan. Panah operasi bergerak sesuai contoh. Rumus dan substitusi angka disusun dalam dua baris. Sebutan besaran memakai kata dicari; seluruh tulisan Dikali tetap utuh saat redup. Setelah itu, mulai, jeda, atau ulangi balapan empat peserta pada lintasan 12 m.
- **Nirokke:** Tiga latihan terbimbing untuk mencari jarak, kecepatan, dan waktu. Rumus serta angka yang diketahui tersedia. Periksa jawaban untuk menjalankan simulasi sesuai data; Lanjut tersedia setelah Nara mencapai garis akhir.
- **Nambahi:** Tiga soal cerita dalam bentuk LKPD tanpa arena. Anak boleh menulis cara menghitung, lalu mengisi satu jawaban akhir beserta satuan yang ditampilkan. Catatan cara menghitung disimpan, tanpa penilaian otomatis terhadap teks bebas. Jawaban salah tidak menghapus pekerjaan.

Posisi pelari berasal dari data jarak/kecepatan dan detik simulasi. Timer di bawah nama pemain mengikuti pemain terpilih dan berhenti saat pemain itu mencapai garis akhir; pemain lain dapat terus melaju. Jeda, waktu berpikir, atau jawaban salah tidak mengubah angka pengukuran. Kecepatan gerak lompat ilustratif tidak dianggap kecepatan sesaat yang sedang diukur. Mode gerak berkurang menampilkan keadaan akhir tanpa lompatan.

Segitiga kecil langsung terlihat di kanan atas Nirokke dan Nambahi; gambar ini bukan tombol. Hanya huruf J/K/W dan garis warna yang dipakai supaya tetap terbaca, dengan besaran yang dicari sudah disembunyikan. Niteni menggunakan segitiga besar di dalam penjelasannya. Halaman selesai tidak menampilkan segitiga bantuan.

Progres Niteni memakai BK4: dua pengenalan garis, tiga contoh, lalu pengamatan balapan. BK3 versi sebelumnya dimigrasikan sesuai contoh yang sedang dikerjakan; level yang selesai tetap selesai. Sesi BK2 tingkat lain dan catatan LKPD tetap dipertahankan. Animasi berhenti ketika aplikasi di latar belakang; mode gerak berkurang langsung menampilkan penjelasan lengkap.

### Cara mengerjakan Nambahi

Tulis langkah pada **Caramu menghitung**, lalu masukkan angka akhirnya pada **Jawaban**. Satuan sudah disediakan di samping input. Tekan **Periksa**, kemudian **Lanjut** setelah benar. Teks langkah menghitung disimpan untuk ditinjau; hanya jawaban angka yang diperiksa otomatis.

| Soal | Contoh langkah | Angka pada Jawaban |
| --- | --- | --- |
| Jarak Nara, kecepatan 2 m/detik selama 8 detik | `2 x 8 = 16 m` | `16` |
| Kecepatan rata-rata Bima, 18 m dalam 9 detik | `18 / 9 = 2 m/detik` | `2` |
| Waktu Tari, 24 m dengan kecepatan 3 m/detik | `24 / 3 = 8 detik` | `8` |

## Sesi Dakon versi lama

Sumber sesi KPK 3 dan 4 dari versi sebelumnya tetap disertakan sebagai acuan. Jalur baru memakai DakonLearningActivity. Seluruh papan, model biji dan animasi Nara berasal dari aset dan renderer yang telah disetujui. Aplikasi berjalan offline tanpa izin internet atau WebView.

## Struktur sumber

- `WelcomeActivity.java`: halaman pembuka native, safe area, tombol Mulai, dan lifecycle animasi.
- `WelcomeMotion.java`: waktu pembukaan, kemunculan bertahap, easing, dan loop.
- `WelcomeBookArtwork.java`, `WelcomeCharacterArtwork.java`, `WelcomeIcons.java`: sampul/halaman buku, empat karakter, dan ikon vektor.
- `GameChooserActivity.java`: pilihan dua permainan, tanpa menyentuh progres masing-masing.
- `LearningPathMapActivity.java` dan `LearningPathStore.java`: renderer dan kontrak progres map bersama.
- `DakonMapActivity.java` dan `BalapKarungMapActivity.java`: metadata empat level Dakon dan tiga level Balap.
- `DakonMapCatalog.java`: label materi, ketersediaan sesi, dan pembacaan tahap yang tersimpan.
- `MapPadlockArtwork.java`: satu gambar gembok vektor untuk semua pos yang belum tersedia.
- `DakonLearningSession.java`: tugas pengenalan, empat contoh terbimbing, soal acak dan snapshot pekerjaan.
- `DakonLearningStore.java`: progres per level, pembukaan berurutan dan sesi latihan ulang.
- `DakonLearningActivity.java` dan `DakonLearningView.java`: interaksi native, papan, petunjuk, input jawaban dan LKPD.
- `JkwTriangleArtwork.java`: gambar vektor JKW besar dengan garis/panah animasi dan versi kecil untuk latihan.
- `JkwTutorialMotion.java`: urutan waktu garis, panah, ketikan Unicode, perhitungan dan kesiapan tombol.
- `BalapKarungSession.java`: data lomba, tiga tahap, enam soal, pemeriksaan jawaban, snapshot setiap halaman.
- `BalapLearningProgress.java` dan `BalapLearningStore.java`: sesi per level, migrasi BK1, pembukaan berurutan dan latihan ulang.
- `BalapKarungActivity.java` dan `BalapKarungView.java`: arena native, waktu simulasi, input angka, LKPD, keyboard dan aksesibilitas.
- `SackRacerArtwork.java`: empat karakter vektor dengan gerak tubuh dan pegangan karung yang menyatu.
- `DakonSession.java`: aturan 3N, isi papan, urungkan, pemeriksaan jawaban, snapshot sesi.
- `BoardGeometry.java`: koordinat dan area sentuh yang dipakai bersama dengan gambar.
- `WorksheetState.java`: isian LKPD, pemeriksaan eksplisit, dan snapshot pekerjaan.
- `WorksheetLayout.java`: posisi kolom native dan latar gambar yang sama.
- `DakonArtwork.java` dan `DakonView.java`: gambar vektor native serta interaksi.
- `NaraRenderer.java`: animasi transparan 20 detik, 12 fps, maksimal dua atlas dalam memori.
- `MainActivity.java`: sepuluh input angka native, penyimpanan lokal, keyboard, safe area, lifecycle.
- `tests/`: pengujian JVM tanpa dependensi eksternal.

## Build yang digunakan

Dari PowerShell di folder proyek utama:

```powershell
.\android\build.ps1
```

Script memakai toolchain portable di `work/android-toolchain`: Microsoft OpenJDK 17, platform Android 35, dan Build Tools 35.0.0. Script menjalankan kompilasi, pemeriksaan sesi/koordinat, DEX, zipalign, penandatanganan APK, dan verifikasi tanda tangan. Hasil ada di `outputs/nusmatika-dakon-debug.apk`.

Toolchain tidak dimasukkan ke arsip kode sumber karena ukurannya besar. Pada komputer lain, sediakan direktori toolchain dengan struktur yang sama atau sesuaikan jalur JDK/SDK di `build.ps1`. Toolchain dan emulator lokal tersimpan terpisah dari kode aplikasi.

Proyek juga memuat konfigurasi Android Studio: buka direktori `android/`. Gradle Wrapper 8.11.1, Android Gradle Plugin 8.9.2, dan JDK 17 dipakai dengan SDK 35. Dari terminal yang sudah mengatur `JAVA_HOME` dan `ANDROID_HOME`, jalankan `gradlew.bat --no-daemon assembleDebug`. Jalur ini menghasilkan `app/build/outputs/apk/debug/app-debug.apk`.

Build native `build.ps1` dan build Gradle `assembleDebug` sudah berhasil dijalankan. APK pada folder `outputs` adalah hasil build native yang sama dengan APK yang dipasang dan diuji di emulator.

Referensi build: [AAPT2](https://developer.android.com/tools/aapt2), [D8](https://developer.android.com/tools/d8), [zipalign](https://developer.android.com/tools/zipalign), [apksigner](https://developer.android.com/tools/apksigner).

Font Nunito berasal dari [Google Fonts](https://github.com/google/fonts/tree/main/ofl/nunito), dengan lisensi OFL di `app/src/main/assets/fonts/OFL.txt`. Karakter Nara dan papan merupakan aset NusMatika yang telah disetujui dalam proses desain.

## Batas versi ini

Dakon memuat empat level untuk pengenalan alat, contoh KPK/FPB dan latihan dua serta tiga bilangan pada papan 1–24. Balap Karung memuat satu sesi pengenalan, tiga latihan terbimbing, dan tiga soal cerita awal. Bank soal lanjutan, tiga permainan lain, dashboard guru, dan distribusi Play Store belum masuk versi ini. Pengujian runtime menggunakan emulator Android 9/API 28; pengujian di HP fisik belum dilakukan.
