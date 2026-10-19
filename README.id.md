# Oshicue Kit · Android

[简体中文](README.md) | [繁體中文](README.zh-Hant.md) | [繁體粵語](README.zh-HK.md) | [English](README.en.md) | [日本語](README.ja.md) | [한국어](README.ko.md) | [ไทย](README.th.md) | [Bahasa Indonesia](README.id.md) | [Bahasa Melayu](README.ms.md)

Aplikasi Android native untuk mengatur jadwal pertunjukan dan perjalanan, melihat jadwal acara, mencatat pengeluaran merchandise, serta menulis catatan harian kegiatan. Dibangun dengan **Kotlin, Jetpack Compose, dan Material 3**, dengan lima tab: Agenda, Jadwal, Catatan, Catatan harian, dan Pengaturan.

Versi saat ini: **0.5.3 (Build 16)**. Mendukung **Android 8.0 (API 26) ke atas**, dengan ID aplikasi `com.oshicue.kit`.

## Mulai menggunakan

1. Instal APK yang sudah ditandatangani, atau bangun aplikasi dari kode sumber dengan petunjuk di bawah.
2. Saat pertama dibuka, aplikasi memuat contoh jadwal acara dan catatan fiktif; agenda dan catatan harian masih kosong.
3. Ganti bahasa melalui Pengaturan, atau buka halaman pengelolaan setiap modul untuk mengimpor berkas JSON Anda.
4. Ekspor cadangan agenda, catatan, dan catatan harian secara berkala, serta simpan berkas jadwal acara asli.

Impor jadwal acara pertama akan menggantikan contoh. Setelah itu, acara diperbarui atau ditambahkan berdasarkan ID. Impor catatan pertama akan menggantikan contoh yang belum diubah; contoh yang sudah diedit secara manual tetap disimpan dan digabungkan. Nama, tanggal, penerbangan, dan lokasi dalam contoh hanya untuk mencoba aplikasi. Ganti dengan data sebenarnya sebelum digunakan.

## Fitur utama

### Agenda

- Kelola Live, penerbangan, hotel, restoran, dan lokasi lain di kalender, dengan dukungan kegiatan berwaktu dan sepanjang hari, zona waktu, alamat, serta catatan. Agenda ini terpisah dari pilihan “Jadwal Saya” pada modul Jadwal.
- Untuk penerbangan, catat waktu, zona waktu, serta bandara keberangkatan dan kedatangan secara terpisah. Untuk hotel, waktu check-in dan check-out menjadi titik dalam rencana perjalanan.
- Cari kota dan tempat, lalu buka lokasi atau rute di aplikasi peta eksternal. Hasil pencarian tidak mengatur zona waktu secara otomatis; pastikan secara manual.
- Tampilkan jeda atau waktu yang tumpang tindih di antara kegiatan berwaktu yang berurutan. Kegiatan sepanjang hari tidak disertakan dalam perbandingan segmen perjalanan. Saat ini belum tersedia perkiraan durasi rute di dalam aplikasi atau informasi penerbangan waktu nyata.
- Simpan salinan ke kalender sistem melalui editor kalender Android. Salinan dan catatan di dalam aplikasi diedit secara terpisah.
- Tersedia pencarian, kombinasi filter, penghapusan beberapa pilihan, pratinjau impor JSON, dan cadangan seluruh agenda.

### Jadwal

- Filter berdasarkan tanggal, acara, serta jenis LIVE／penjualan merchandise. Lihat tempat secara horizontal dan waktu secara vertikal.
- Ketuk satu kali untuk melihat detail sesi; ketuk dua kali atau tekan lama untuk menambahkan atau menghapusnya dari “Jadwal Saya”. Tindakan yang sama juga tersedia di halaman detail.
- Sesi yang tumpang tindih ditampilkan berdampingan dengan penanda benturan. Mendukung skala 50%, 70%, 100%, 150%.
- Garis waktu saat ini diperbarui setiap 15 detik; sesi yang melewati tengah malam dipisahkan menurut zona waktu tampilan. Tampilan beberapa acara menggunakan zona waktu acara pertama dalam urutan dokumen di antara acara yang dipilih.
- Mendukung tempel teks atau impor beberapa berkas, validasi, dan pratinjau. Acara dengan ID yang sama diperbarui secara menyeluruh, sedangkan acara lain tetap disimpan. ID acara yang berulang dalam satu kelompok impor akan ditolak.
- Penghapusan acara memerlukan konfirmasi dan menghapus pilihan sesi untuk acara tersebut, sementara catatan dan catatan harian tetap disimpan. Saat ini belum ada menu ekspor jadwal; simpan berkas asli yang digunakan untuk impor.

### Catatan · Ledger

- Lihat jumlah pembelian merchandise harian dan bulanan serta pengeluaran per mata uang melalui kalender. Kelola grup, anggota, avatar, jenis merchandise, dan data acara.
- Satu anggota dapat bergabung dengan beberapa grup, dengan warna dan harga bawaan tersendiri untuk setiap keanggotaan. Pengaturan anggota favorit berlaku untuk seluruh keanggotaannya.
- Catat tanggal, jumlah, mata uang, harga satuan, dan salinan data acara saat pembelian. Catatan baru mengutamakan harga dari anggota, grup, jenis, dan mata uang yang sama pada atau sebelum tanggal yang dipilih, lalu harga penambahan terakhir yang diingat, harga bawaan anggota, atau 0. Harga satuan dapat diubah secara manual.
- Peringkat mendukung rentang harian, bulanan, tahunan, dan sepanjang waktu, dengan tampilan total anggota, anggota per grup, grup, jumlah, atau pengeluaran.
- Mendukung `CNY`, `JPY`, `HKD`, `TWD`, `USD`, `KRW`, `THB`, `IDR`, `MYR`; dihitung terpisah untuk setiap mata uang tanpa konversi kurs. `JPY`, `KRW` menggunakan bilangan bulat, sedangkan mata uang lain menggunakan dua angka desimal. `unitPriceMinor` dalam JSON berupa bilangan bulat dalam satuan mata uang terkecil.
- Menghapus anggota atau grup akan mengarsipkan data dan keanggotaan terkait, serta mempertahankan salinan data pembelian lama dan statistiknya. Pengelolaan catatan mendukung filter, penghapusan sekaligus, cadangan JSON lengkap, dan impor.

### Catatan harian · Diary

- Tulis judul, isi, ringkasan, dan tag, dengan status “Draf” atau “Siap”. Telusuri berdasarkan tanggal, cari, atau gunakan filter.
- Ambil tanggal, nama, kota, tempat acara, dan alamat dari acara di Catatan; catatan harian menyimpan salinan data tersendiri.
- Perubahan disimpan otomatis sekitar 0.5 detik setelah berhenti mengedit. Perubahan yang masih tertunda disimpan saat menekan Selesai atau menutup editor. Jika penyimpanan gagal, tersedia pemberitahuan untuk mencoba lagi.
- Salin sebagai Markdown／teks biasa, ekspor `.md`／`.txt`, atau bagikan melalui sistem Android. “Siap” hanya status lokal; berbagi tidak menerbitkan catatan harian secara otomatis.
- Tersedia pengelolaan beberapa catatan dan cadangan JSON lengkap. ID dengan isi yang sama dihapus duplikatnya; jika ID yang sama memiliki isi berbeda, seluruh impor akan ditolak.

## Templat JSON dan cadangan

| Isi | Berkas bawaan |
| --- | --- |
| Templat agenda | [agenda-template.json](app/src/main/assets/templates/agenda-template.json) |
| Templat jadwal | [timetable-template.json](app/src/main/assets/templates/timetable-template.json) |
| Contoh jadwal | [demo-timetable.json](app/src/main/assets/templates/demo-timetable.json) |
| Templat catatan | [ledger-template.json](app/src/main/assets/templates/ledger-template.json) |

Berkas JSON menggunakan UTF-8 dan diimpor melalui modul yang sesuai, dengan pratinjau sebelum konfirmasi. Struktur JSON setiap modul berbeda dan tidak dapat saling dipertukarkan meskipun sama-sama menggunakan `schemaVersion: 1`. Untuk catatan harian, buat satu catatan lalu ekspor cadangannya sebagai referensi format; kode sumber tidak menyertakan templat catatan harian terpisah.

## Data lokal dan pengaturan

Data disimpan dalam direktori privat aplikasi `filesDir/OshicueKit/` sebagai `agenda.json`, `schedule.json`, `ledger.json`, `diary.json`, dan `settings.json`. Bahasa dan tab yang terakhir dibuka diingat secara lokal; pilihan sesi jadwal juga disimpan di perangkat.

Secara bawaan aplikasi mengikuti bahasa sistem, dengan bahasa Inggris sebagai pengganti untuk bahasa yang tidak didukung. Anda dapat memilih salah satu dari sembilan bahasa secara manual. Antarmuka mengikuti mode terang／gelap sistem dan menyesuaikan pengaturan kontras yang ditingkatkan pada Android 14 ke atas.

Catatan utama dapat digunakan secara offline tanpa akun. Saat ini belum tersedia sinkronisasi cloud di dalam aplikasi, pencadangan cloud otomatis, pengingat push, atau impor dari gambar. Pencarian tempat bergantung pada layanan geocoding perangkat. Peta, rute, dan kalender eksternal bergantung pada aplikasi serta layanan yang terpasang, yang mungkin memerlukan internet. Ekspor cadangan secara manual sebelum mencopot aplikasi atau menghapus datanya.

## Membangun dari kode sumber

Berikut konfigurasi proyek saat ini dan lingkungan yang digunakan untuk membuat paket ini:

| Komponen | Versi |
| --- | --- |
| Versi aplikasi / `versionCode` | `0.5.3` / `16` |
| Android API minimum / target / kompilasi | `26` / `37` / `37` |
| Android Gradle Plugin | `9.4.1` |
| Plugin Kotlin Compose | `2.2.10` |
| Compose BOM | `2026.09.00` |
| Target kode sumber dan bytecode Java | `17` |
| Gradle / JBR yang digunakan untuk verifikasi ini | `9.8.1` / `25.0.3` |

1. Buka direktori ini di Android Studio, lalu instal Android SDK Platform 37 dan alat build yang diperlukan.
2. Gunakan JDK yang sesuai untuk menjalankan Gradle. Verifikasi ini menggunakan JBR bawaan Android Studio. Target kompilasi Java `17` dan versi JDK untuk menjalankan Gradle merupakan dua pengaturan yang berbeda.
3. Biarkan Android Studio membuat `local.properties`, atau isi `sdk.dir` dengan jalur absolut SDK di perangkat Anda. Jangan commit berkas ini.
4. Tambahkan Gradle yang sudah terpasang ke `PATH`, lalu jalankan perintah berikut dari direktori ini. Resolusi dependensi pertama memerlukan internet.

Kode sumber tidak menyertakan Gradle Wrapper, sehingga perintah berikut menggunakan `gradle`:

```sh
gradle :app:assembleDebug
gradle :app:testDebugUnitTest :app:lintDebug
```

Lokasi keluaran APK: `app/build/outputs/apk/debug/app-debug.apk`. Build bawaan menggunakan tanda tangan debug; proyek saat ini belum mengatur penandatanganan rilis, sehingga perlu dikonfigurasi sebelum penerbitan.

Kode sumber terkait telah lulus **166 pengujian unit** saat pembuatan paket ini, dan APK lulus pemeriksaan tanda tangan serta alignment. Repositori belum menyediakan rangkaian pengujian antarmuka Android otomatis; peta, kalender sistem, pemilihan berkas, dan berbagi perlu diperiksa pada perangkat tujuan.

Saat mengunggah ke GitHub, kecualikan `local.properties`, `.gradle/`, `.kotlin/`, `build/`, `app/build/`, berkas IDE lokal, dan kunci penandatanganan.

## Struktur proyek

| Jalur | Isi |
| --- | --- |
| `app/src/main/java/com/oshicue/kit/core/` | Model data, JSON, impor, statistik, dan penyimpanan lokal |
| `app/src/main/java/com/oshicue/kit/ui/` | Halaman Compose, editor, pengelolaan, dan operasi berkas |
| `app/src/main/assets/localization/` | Teks antarmuka dalam sembilan bahasa |
| `app/src/main/assets/templates/` | Templat JSON agenda, jadwal, dan catatan |
| `app/src/main/res/` | Sumber daya ikon, font, dan tema |
| `app/src/test/` | Pengujian unit bagian inti |

## Bahasa dokumentasi dan lisensi font

Bahasa Mandarin Sederhana adalah naskah sumber; delapan README lainnya memiliki isi yang bersesuaian dengan halaman ini. Kode bahasanya adalah `zh-Hans`, `zh-Hant`, `zh-HK`, `en`, `ja`, `ko`, `th`, `id`, `ms`. Kunci JSON, ID, nilai enum, perintah, dan jalur tidak diterjemahkan.

Font Unbounded yang disertakan menggunakan [SIL Open Font License 1.1](app/src/main/assets/fonts-OFL.txt). Komponen pihak ketiga mengikuti lisensi masing-masing.
