# Impor jadwal JSON

[简体中文](IMPORT.zh-CN.md) | [繁體中文](IMPORT.zh-Hant.md) | [繁體粵語](IMPORT.zh-HK.md) | [English](IMPORT.en.md) | [日本語](IMPORT.ja.md) | [한국어](IMPORT.ko.md) | [ไทย](IMPORT.th.md) | [Bahasa Indonesia](IMPORT.id.md) | [Bahasa Melayu](IMPORT.ms.md)

Gunakan JSON dengan encoding UTF-8. Buka halaman impor dari jadwal, lalu tempel seluruh teks atau pilih satu atau beberapa berkas `.json` atau teks biasa. Setelah validasi berhasil, jumlah acara dan sesi ditampilkan; data diterapkan hanya setelah konfirmasi. Teks pratinjau yang diubah harus divalidasi ulang.

## Pembaruan dan favorit

Saat pertama dijalankan, aplikasi membuat dan menyimpan dua jadwal fiktif dengan acuan awal jam saat peluncuran menurut waktu Tokyo, termasuk sesi untuk hari berikutnya. Dua grup pertama menjadi favorit awal. Judul tidak menampilkan label demo. Peluncuran berikutnya membaca data tersimpan. Impor nyata pertama mengganti jadwal yang masih berstatus demo dan menghapus favorit demo. Impor berikutnya mempertahankan acara lain dan mengganti seluruh isi acara dengan `id` yang sama. Untuk pembaruan, sediakan acara lengkap, bukan hanya sebagian sesinya.

`id` grup merupakan identitas favorit bersama di berbagai acara. Mempertahankan ID menjaga favorit saat nama berubah. Ketuk dua kali blok LIVE atau penjualan merchandise untuk mengubah favorit grup. Jadwal Saya menggabungkan kedua jenis sesi dari grup favorit berdasarkan acara dan tanggal yang dipilih. Waktu yang bertumpang tindih diperbolehkan dan ditampilkan berdampingan dengan petunjuk benturan. Petunjuk hanya membandingkan waktu, tanpa menilai perjalanan atau kemungkinan menghadiri keduanya.

## Bidang

| Lokasi | Bidang | Aturan |
| --- | --- | --- |
| Tingkat utama | `schemaVersion` | Bilangan bulat wajib, selalu `1`. |
| Tingkat utama | `events` | Larik acara wajib. Halaman impor mensyaratkan sedikitnya satu acara dan satu sesi dalam seluruh kumpulan. |
| Acara | `id`, `name`, `city` | String wajib yang tidak kosong. ID acara harus unik dalam seluruh kumpulan. |
| Acara | `timeZone` | Zona waktu IANA yang valid wajib diisi, misalnya `Asia/Tokyo`. |
| Acara | `venues`, `artists`, `sessions` | Larik wajib; suatu acara boleh tidak memiliki sesi. |
| Tempat | `id`, `name`, `color` | ID dan nama tidak kosong; warna harus berbentuk `#RRGGBB`, misalnya `#78C9EB`. |
| Grup | `id`, `name` | String wajib yang tidak kosong. Gunakan ID grup yang tetap di berbagai acara. |
| Sesi | `id`, `artistID`, `venueID` | ID sesi tidak kosong; referensi harus menunjuk grup dan tempat yang dideklarasikan dalam acara ini. |
| Sesi | `kind` | Hanya `live` atau `goods`. |
| Sesi | `startsAt`, `endsAt` | Tanggal dan waktu ISO 8601 lengkap, dengan detik serta `Z` atau offset eksplisit; akhir harus setelah awal. |
| Sesi | `area`, `notes` | String opsional; boleh dihilangkan atau berupa `null`. |

ID tempat, grup, dan sesi masing-masing harus unik di dalam acaranya. String wajib tidak boleh hanya berisi spasi. JSON tidak mendukung komentar atau koma setelah elemen terakhir. Bidang yang tidak dikenal diterima, tetapi tidak ditampilkan aplikasi atau dipertahankan saat dienkode ulang.

## Validasi dan impor beberapa berkas

Kesalahan menunjukkan jalur bidang, seperti `$.events[0].sessions[2].endsAt`. Versi, tipe, tanggal, zona waktu, warna, ID ganda, referensi, atau durasi yang tidak valid menghentikan validasi seluruh masukan. Beberapa berkas digabung lalu divalidasi bersama; ID acara ganda antarberkas juga menyebabkan kegagalan. Kegagalan satu berkas mencegah impor sebagian. Halaman impor tidak dapat menerapkan larik acara kosong atau kumpulan tanpa sesi. Format inti mengizinkan `events: []` untuk menyimpan keadaan kosong internal setelah penghapusan.

## Zona waktu dan lintas tengah malam

Nilai yang diterima mencakup `2026-10-14T12:00:00+09:00`, `2026-10-14T03:00:00Z`, dan pecahan detik. `12:00`, nilai tanpa detik, atau nilai tanpa zona waktu tidak valid. Offset menentukan waktu absolut, sedangkan `timeZone` acara menentukan tanggal lokal. Isi sesuai zona waktu sebenarnya untuk setiap sesi. Jam berulang pada waktu musim panas, `2026-11-01T01:30:00-04:00` dan `2026-11-01T01:30:00-05:00`, menunjukkan dua waktu berbeda.

Untuk sesi lintas tengah malam, gunakan tanggal berikutnya pada waktu akhir: `2026-10-14T23:40:00+09:00` → `2026-10-15T00:20:00+09:00`. Sesi muncul pada kedua tanggal yang dicakup, dengan blok dipotong sesuai hari. Akhir tepat pada `00:00:00` tidak menambahkan tanggal kosong. Tidak ada batas durasi maksimum buatan.

Saat acara dari zona waktu berbeda dipilih bersama, antarmuka saat ini menggunakan zona waktu acara terpilih yang pertama dalam urutan dokumen untuk tanggal, waktu, dan detail. Bilah filter menampilkan zona tersebut. Pilih satu acara saja untuk melihat waktu lokalnya sendiri. Garis waktu saat ini memakai waktu sistem nyata; tanggal demo yang tetap belum tentu hari ini.

## Mengedit templat

Salin templat jadwal dari tautan di akhir halaman. Pertahankan `schemaVersion` dan `events`, isi ID yang konsisten, lalu salin sesi dan ubah referensi, jenis, serta waktunya. Semua acara, grup, dan jadwal dalam templat bersifat fiktif. Templat tidak diimpor secara otomatis.

---

[Kembali ke README](../README.id.md) · [Format impor](IMPORT.id.md) · [Panduan verifikasi](VERIFICATION.id.md) · [Tampilan dan zoom](TIMETABLE-READABILITY.id.md) · [Hapus jadwal](SCHEDULE-DELETION.id.md)

Templat: [Jadwal](../Templates/timetable-template.json) · [Demo tanggal tetap](../Templates/demo-timetable.json) · [Pemeriksaan keterbacaan](../Templates/timetable-readability.json)
