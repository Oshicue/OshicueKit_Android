# Tampilan jadwal dan zoom

[简体中文](TIMETABLE-READABILITY.md) | [繁體中文](TIMETABLE-READABILITY.zh-Hant.md) | [繁體粵語](TIMETABLE-READABILITY.zh-HK.md) | [English](TIMETABLE-READABILITY.en.md) | [日本語](TIMETABLE-READABILITY.ja.md) | [한국어](TIMETABLE-READABILITY.ko.md) | [ไทย](TIMETABLE-READABILITY.th.md) | [Bahasa Indonesia](TIMETABLE-READABILITY.id.md) | [Bahasa Melayu](TIMETABLE-READABILITY.ms.md)

## Linimasa dan nama

Semua Jadwal memisahkan kolom menurut acara, tempat, dan jenis LIVE／penjualan merchandise. Jadwal Saya menggabungkan kedua jenis sesi dari grup favorit. Sesi yang tumpang tindih otomatis berdampingan dengan jumlah lajur minimum untuk setiap kelompok tumpang tindih. Sesi mandiri berikutnya kembali memakai lebar kolom penuh. Gulir horizontal sementara skala waktu di kiri tetap di tempat.

Pada ukuran teks standar, tiap lajur paralel memiliki lebar minimal 180 pt; nama panjang atau sesi pendek dapat memperlebarnya hingga 320 pt. Lebar total kolom tempat bertambah sesuai jumlah sesi serentak. Judul lajur mengulang nama tempat agar mudah dikenali saat digulir. Lebar dan ukuran huruf nama mengikuti ukuran teks sistem.

Nama artis diprioritaskan dan dapat berpindah baris. Ukuran awalnya 12 pt pada Semua Jadwal dan 15 pt pada Jadwal Saya, lalu mengecil hingga 11 pt standar bila diperlukan. Jika ruang terbatas, informasi sekunder seperti waktu, tempat, atau area dalam blok disembunyikan terlebih dahulu. Posisi dan tinggi blok mengikuti waktu nyata dengan celah gambar; sesi tidak diperpanjang demi teks. Sesi sangat pendek, nama sangat panjang, atau teks besar masih dapat terpotong. Ketuk sekali untuk membuka detail dan membaca nama asli lengkap, waktu, tempat, area, serta catatan. VoiceOver juga dapat membacakan nama.

Ketuk dua kali blok untuk menambah／menghapus favorit grup. Detail juga menyediakan tindakan favorit. Ketuk sekali untuk melihat tidak mengubah favorit.

## Zoom dan waktu saat ini

| Skala | Tinggi per menit |
| --- | --- |
| 50% | 2 pt |
| 70% | 2.8 pt |
| 100%（awal） | 4 pt |
| 150% | 6 pt |

Zoom hanya mengubah skala tampilan linimasa. Garis waktu saat ini memakai waktu sistem nyata. Tombol lokasi beralih ke hari ini jika acara saat ini memiliki sesi hari ini, dan menggulir ke sekarang jika waktu saat ini berada dalam rentang yang ditampilkan.

## Pemeriksaan ulang keterbacaan

Impor templat pemeriksaan dari tautan di akhir halaman, lalu pilih `2026-06-14` dan `Asia/Tokyo`. Templat berisi nama Jepang／Inggris panjang, LIVE 5 menit dan 10 menit, serta empat sesi merchandise serentak di satu tempat. Semua jadwal bersifat fiktif.

1. Pada 100% awal, periksa durasi dan nama sesi pendek. Gulir horizontal untuk membaca keempat sesi merchandise dan pastikan tidak saling menutupi.
2. Ketuk sekali untuk melihat detail lengkap. Setelah mengetuk dua kali untuk favorit, periksa Jadwal Saya agar LIVE dan merchandise tampil bersama, lalu periksa petunjuk tumpang tindih.
3. Pilih 50%, 70%, 100%, dan 150% bergantian. Pastikan posisi mengikuti skala waktu dan detail masih dapat dibuka saat tampilan diperkecil.
4. Perbesar ukuran teks sistem, lalu ulangi pemeriksaan gulir, detail, dan favorit.

Lihat panduan verifikasi untuk kasus tata letak inti dan UI. Pengujian UI tidak dijalankan ulang dalam pembaruan ini; langkah teks besar masih menjadi pemeriksaan yang harus dilakukan.

---

[Kembali ke README](../README.id.md) · [Format impor](IMPORT.id.md) · [Panduan verifikasi](VERIFICATION.id.md) · [Tampilan dan zoom](TIMETABLE-READABILITY.id.md) · [Hapus jadwal](SCHEDULE-DELETION.id.md)

Templat: [Jadwal](../Templates/timetable-template.json) · [Demo tanggal tetap](../Templates/demo-timetable.json) · [Pemeriksaan keterbacaan](../Templates/timetable-readability.json)
