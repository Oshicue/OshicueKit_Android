# Panduan verifikasi jadwal

[简体中文](VERIFICATION.md) | [繁體中文](VERIFICATION.zh-Hant.md) | [繁體粵語](VERIFICATION.zh-HK.md) | [English](VERIFICATION.en.md) | [日本語](VERIFICATION.ja.md) | [한국어](VERIFICATION.ko.md) | [ไทย](VERIFICATION.th.md) | [Bahasa Indonesia](VERIFICATION.id.md) | [Bahasa Melayu](VERIFICATION.ms.md)

Halaman ini menjelaskan cakupan verifikasi dalam sumber saat ini dan cara mengulang pemeriksaan. Adanya berkas pengujian tidak berarti pengujian dijalankan dalam pembaruan ini. Tangkapan layar lama menjadi referensi antarmuka, bukan bukti bahwa build saat ini lulus.

## Pemeriksaan yang dapat diulang

Jalankan dari direktori utama proyek:

```sh
swift test
python3 scripts/verify-localizations.py
```

`swift test` menjalankan pengujian inti Foundation, bukan pengujian antarmuka iOS. Pemeriksaan pelokalan mencakup kumpulan kunci, teks tidak kosong, placeholder format, dan sintaks `.strings` untuk kesembilan bahasa.

| Berkas | Cakupan |
| --- | --- |
| [ScheduleImporterTests.swift](../Tests/ScheduleImporterTests.swift) | Kesalahan format dan bidang, referensi dan ID ganda, offset waktu, pecahan detik, tengah malam, waktu musim panas, pembaruan acara lengkap, templat, dan pembuatan demo awal. |
| [TimelineLayoutTests.swift](../Tests/TimelineLayoutTests.swift) | Batas waktu yang bersambung, lajur tumpang tindih, jumlah lajur serentak minimum, urutan stabil, lebar kolom padat, dan tidak ada benturan horizontal. |
| [ScheduleDeletionTests.swift](../Tests/ScheduleDeletionTests.swift) | Penghapusan acara, favorit bersama, nama sama dengan ID berbeda, ID tidak dikenal, encoding dokumen kosong, dan pemuatan ulang. |

## Pemeriksaan ulang antarmuka native

Buka proyek di Xcode dan jalankan pengujian UI pada simulator iOS. Kasus utama terdapat di [OshicueKitUITests.swift](../UITests/OshicueKitUITests.swift), [TimelineReadabilityUITests.swift](../UITests/TimelineReadabilityUITests.swift), dan [ScheduleDeletionUITests.swift](../UITests/ScheduleDeletionUITests.swift).

Periksa ketuk sekali untuk detail, ketuk dua kali untuk favorit, penggabungan jenis sesi di Jadwal Saya, impor tidak valid yang tidak dapat dikonfirmasi, impor valid yang mengganti demo, pilihan dan pergantian sembilan bahasa, keterbacaan sesi pendek dan penjualan merchandise serentak, konfirmasi penghapusan, penyesuaian pilihan, serta peluncuran ulang dengan jadwal kosong. Pengujian UI DEBUG memakai berkas jadwal terpisah dengan `OSHICUE_UI_TESTS=1`; `OSHICUE_PRESERVE_UI_DATA=1` mempertahankan data pengujian saat aplikasi diluncurkan ulang.

## Pemeriksaan manual tambahan

Impor beberapa berkas dan pastikan berkas tidak valid atau ID acara ganda antarberkas tidak diterapkan sebagian. Pilih acara dari zona waktu berbeda secara terpisah dan bersama, lalu periksa zona pada bilah filter. Periksa sesi lintas tengah malam dan sesi yang berakhir tepat tengah malam, lalu perbesar teks sistem dan ulangi pemeriksaan detail. Setelah penghapusan, pastikan snapshot buku catatan keuangan dan jurnal tetap ada. Luncurkan ulang dan periksa jadwal serta favorit.

Pemeriksaan simulator tidak menggantikan verifikasi nyata pada perangkat fisik, berbagai versi sistem, teks besar, atau pemilih berkas. Catat hasil nyata tes UI, pemasangan pada perangkat fisik dan publikasi untuk pelaksanaan ini secara terpisah. [Direktori tangkapan layar](../screenshots/) yang tersedia menyimpan referensi antarmuka sebelumnya.

## Uji ulang pengelolaan, favorit dan tampilan 0.4.0

Berikut definisi tes dan lingkup uji ulang yang ditambahkan untuk 0.4.0 / build 10, bukan pernyataan bahwa seluruh tes UI dijalankan dan lulus kali ini. Catat lulus, gagal atau belum dijalankan berdasarkan pelaksanaan nyata. Pemeriksaan palet juga tidak menggantikan inspeksi manual setiap layar.

| File tes | Kasus | Cakupan tambahan |
| --- | --- | --- |
| [DataManagementUITests.swift](../UITests/DataManagementUITests.swift) | 6 | Pilihan pembelian terfilter dengan Batal/Konfirmasi, perubahan filter menghapus pilihan, menghapus semua mata uang, penghapusan batch catatan harian dan formulir cadangan di pengelolaan Pengaturan. Pilihan beberapa data katalog, pembatalan/konfirmasi arsip dan riwayat pembelian tetap ada. |
| [DataManagementFailureUITests.swift](../UITests/DataManagementFailureUITests.swift) | 2 | Suntikkan kegagalan penyimpanan nyata dengan `OSHICUE_FAIL_BATCH_DELETE=1` pada data uji Debug terpisah. Periksa rekaman asli, isi catatan harian dan pilihan saat ini tetap ada, data tidak berubah setelah mulai ulang, lalu pilih kembali dan coba penghapusan setelah menonaktifkan kegagalan. |
| [AppearanceUITests.swift](../UITests/AppearanceUITests.swift) | 5 | Palet dinamis UIKit sebenarnya, kontras teks dan kontrol terpilih dalam kedua tampilan, latar tempat, peningkatan kontras dan keterbacaan warna anggota; tes tidak mengoperasikan layar aplikasi. |
| [AppearanceScreenUITests.swift](../UITests/AppearanceScreenUITests.swift) | 1 | Operasikan empat modul, favorit dan pemilihan anggota, editor, pengelolaan serta cadangan dalam tampilan terang atau gelap sistem. Aplikasi tidak memaksakan tema; pemeriksaan layar diikuti 13 lampiran tangkapan layar nyata. |

```sh
xcodebuild test -project OshicueKit.xcodeproj -scheme OshicueKit -destination 'platform=iOS Simulator,id=<SIMULATOR_ID>' -only-testing:OshicueKitUITests/AppearanceUITests -only-testing:OshicueKitUITests/DataManagementFailureUITests -only-testing:OshicueKitUITests/AppearanceScreenUITests
```

## Langkah uji ulang manual tambahan

- Buka kedua layar pengelolaan dari Pengaturan dan periksa impor/ekspor lama di Cadangan. Gabungkan filter, pilih sebagian hasil dan periksa jumlah konfirmasi; batalkan dahulu, lalu konfirmasi dan mulai ulang. Konfirmasi pembelian harus memisahkan mata uang dan perubahan filter harus menghapus pilihan.
- Arsipkan anggota beberapa grup dan satu grup secara batch; periksa lingkup, keanggotaan lain dan edit riwayat. Hapus pembelian dan pastikan katalog serta catatan harian tetap ada. Hapus hasil catatan harian menurut status, tanggal dan pencarian; periksa isi yang tidak dipilih dan modul lain.
- Atur favorit, buka ulang lewat keanggotaan lain dan periksa preferensi bersama, bagian atas pemilih, harga tiap keanggotaan dan pilihan awal rekaman baru. Periksa pembatalan favorit, arsip, ekspor dan impor ulang. JSON lama tanpa `isFavorite` harus bukan favorit dan `schemaVersion` tetap `1`.
- Ganti tampilan sistem terang/gelap dan periksa empat modul, detail, editor, pemilih, pengelolaan dan cadangan. Periksa juga teks besar, label panjang sembilan bahasa, avatar, warna anggota, jumlah uang, tombol terpilih dan kesalahan. Catat perangkat dan versi sistem; gambar lama tidak menggantikan hasil terbaru.

---

[Kembali ke README](../README.id.md) · [Format impor](IMPORT.id.md) · [Panduan verifikasi](VERIFICATION.id.md) · [Tampilan dan zoom](TIMETABLE-READABILITY.id.md) · [Hapus jadwal](SCHEDULE-DELETION.id.md)

Templat: [Jadwal](../Templates/timetable-template.json) · [Demo tanggal tetap](../Templates/demo-timetable.json) · [Pemeriksaan keterbacaan](../Templates/timetable-readability.json)
