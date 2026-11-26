# Catatan pengembangan Petak Umpet V18

Versi aplikasi: 0.13.0 / versionCode 18. Dasar pengembangan: dokumen pemetaan CP-materi-permainan yang diberikan pengguna, dengan rincian alur Petak Umpet mengikuti arahan terbaru pengguna pada 19 November 2026.

## Pemetaan arahan ke hasil

| Tahap | Arahan | Implementasi awal |
| --- | --- | --- |
| Niteni | Pencarian objek dengan penanda di atas bangun; anak cukup mengetuk | Satu halaman sekolah, enam objek, label Balok/Kubus dan panah. Temuan memberi pengenalan jenis bangun. |
| Nirokke | Pencarian objek tanpa penanda sehingga anak perlu lebih teliti | Enam bangun tanpa label atau panah; lokasi berbeda dari latihan Niteni. Klik berulang tidak menambah koleksi. |
| Nambahi | Isian jumlah bangun untuk menyusun bentuk dari hasil pencarian | Bentuk L dari empat balok dan dua kubus. Dua input angka, Periksa, Susun bangun, animasi penyusunan, lalu Selesai. |

Pola mencari objek mengikuti contoh gameplay yang disebut pengguna. Seluruh gambar halaman sekolah dan bangun dibuat dengan renderer vektor NusMatika; tidak ada gambar, karakter, cerita kriminal, atau kode dari permainan lain.

Niteni merupakan koleksi latihan pengenalan. Inventaris yang dipakai Nambahi berasal dari pencarian Nirokke yang sudah selesai, berjumlah enam benda. Benda latihan Niteni tidak dihitung dua kali. Semua enam benda dari inventaris tersebut digunakan pada contoh L.

## Dasar matematis contoh L

Semua benda mempunyai tebal satu satuan. Empat balok berdimensi 2 x 1 x 1; dua kubus berdimensi 1 x 1 x 1. Dua balok diputar tegak menjadi 1 x 1 x 2, tetapi tetap merupakan balok yang sama. Enam benda berjumlah volume sepuluh kubus satuan.

L mempunyai kaki mendatar lima satuan dan kaki tegak enam satuan, berbagi sudut bawah kiri. Balok horizontal menempati dua bagian dasar; dua balok vertikal mengisi batang tegak; satu kubus menutup bagian paling atas dan satu kubus mengisi ujung dasar. Tidak ada bangun yang saling tumpang tindih atau melayang tanpa penopang.

Kunci jawaban menghitung **jumlah bendanya**, bukan volume, sisi yang tampak, atau jumlah kubus satuan. Kunci empat balok dan dua kubus dihitung dari data Piece yang sama dengan geometri renderer. Garis sambungan pada gambar menunjukkan batas tiap benda.

Jumlah dan susunan tersebut merupakan contoh data awal yang dipilih untuk kerangka ini; pengguna belum menetapkan bank soal atau jumlah variasi final.

## Perilaku aplikasi

- Jalur tiga level memakai LearningPathMapActivity, gembok, dan urutan dari bawah seperti modul lain.
- Level berikutnya terbuka setelah level sebelumnya diselesaikan secara eksplisit.
- Keluar atau menutup aplikasi menyimpan subset benda yang ditemukan serta isian.
- Menjawab salah mempertahankan kedua isian dan semua benda.
- Mengetik jawaban tidak memicu penilaian. Penilaian hanya saat Periksa.
- Susun bangun hanya tersedia setelah kedua hitungan benar.
- Pengulangan tidak menghilangkan unlock yang pernah diperoleh atau inventaris Nirokke yang telah selesai.
- Animasi berhenti di latar belakang. Gerak berkurang menampilkan hasil akhir langsung.
- Jika proses berhenti di tengah animasi, pembukaan berikutnya memperlihatkan susunan akhirnya; siswa tetap menekan Selesai.
- Halaman selesai hanya menampilkan judul selesai, Nara, pengakuan selesai, dan tombol kembali. Arena, soal, isian, inventaris, dan umpan balik sebelumnya disembunyikan.
- Area objek menggunakan tombol Android transparan di atas gambar sehingga dapat diakses lewat sentuhan, keyboard, dan layanan aksesibilitas. Label posisi aksesibilitas tidak menggambar penanda pada Nirokke.

## Batas kerangka

Sudah dibuat: satu arena pencarian dengan dua penempatan untuk Niteni/Nirokke, satu contoh L, dua jenis benda, penyimpanan, animasi penyusunan, integrasi menu/map dan uji logika.

Belum dibuat: variasi arena dan soal, kontrol seret/lepas untuk menyusun, pengecoh berupa bangun tambahan, penilaian jawaban tokoh, serta latihan tampak depan/atas/samping. Isian jumlah dan animasi sesudah jawaban benar merupakan bentuk assemble yang diminta pengguna untuk progres awal ini.

Dokumen CP memuat tujuan geometri yang lebih luas. Kerangka ini belum mengklaim memenuhi seluruh tujuan tersebut; terutama bagian visualisasi tampak masih memerlukan kegiatan tersendiri.

## Berkas lama yang berubah

- GameChooserActivity: entri Petak Umpet dan pemilih yang dapat digulir.
- LearningPathMapActivity: satu hook untuk ikon Petak Umpet; aturan jalur tetap digunakan bersama.
- AndroidManifest.xml dan app/build.gradle: dua Activity baru serta nomor versi 18.
- build.gradle: tugas testLogic untuk seluruh pengujian JVM.
- build.ps1: model dan pengujian Petak Umpet ikut dikompilasi/dijalankan.
- README: cara build dan status terbaru; README V17 disimpan di docs/README_V17.md.
- ID resource untuk kanvas Balap dan kontrol map dirapikan; atribut navigation bar API 27 dipisahkan ke values-v27 agar pemeriksaan Lint sesuai min SDK 26.

Aturan, soal, penyimpanan, renderer belajar Dakon, dan renderer Balap tidak diubah. Perubahan ID view Balap hanya mengganti angka literal dengan ID resource Android. Package aplikasi tetap id.nusmatika.dakon.

## Daftar pemeriksaan manual

1. Buka menu, gulir ke Petak Umpet, periksa ketiga node dan gemboknya.
2. Pada Niteni, periksa enam penanda dan ketuk semuanya sampai Selesai aktif.
3. Pada Nirokke, pastikan benda yang belum ditemukan tidak diberi penanda.
4. Ketuk benda yang sama berulang dan pastikan koleksi tidak bertambah.
5. Keluar setelah beberapa temuan, buka lagi, dan pastikan temuan tetap tersimpan.
6. Selesaikan Nirokke, buka Nambahi, masukkan jawaban salah, lalu perbaiki tanpa kehilangan isian.
7. Masukkan 4 balok dan 2 kubus, Periksa, Susun bangun, dan tunggu susunan L terbentuk.
8. Tutup aplikasi pada setiap langkah, termasuk tengah animasi, lalu periksa kelanjutannya.
9. Ulangi level lama dan pastikan level sesudahnya tetap terbuka.
10. Periksa keyboard, gulir pada viewport 320 dp, mode animasi sistem mati, serta navigasi kembali.
11. Buka Dakon dan Balap untuk memastikan rute lama tetap dapat digunakan.
