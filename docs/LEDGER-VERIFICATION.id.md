# Panduan verifikasi Catatan

[简体中文](LEDGER-VERIFICATION.md) | [繁體中文](LEDGER-VERIFICATION.zh-Hant.md) | [繁體粵語](LEDGER-VERIFICATION.zh-HK.md) | [English](LEDGER-VERIFICATION.en.md) | [日本語](LEDGER-VERIFICATION.ja.md) | [한국어](LEDGER-VERIFICATION.ko.md) | [ไทย](LEDGER-VERIFICATION.th.md) | [Bahasa Indonesia](LEDGER-VERIFICATION.id.md) | [Bahasa Melayu](LEDGER-VERIFICATION.ms.md)

Halaman ini menjelaskan cakupan pengujian yang ada dan pemeriksaan penerimaan yang dapat diulang, berdasarkan kode sumber saat ini. Adanya file pengujian tidak berarti pengujian lulus pada eksekusi ini. Laporan versi lama, bundel hasil sementara dan tangkapan layar bukan bukti kelulusan versi sekarang. Hasil sebenarnya mengikuti log eksekusi terbaru.

## Pengujian inti

Jalankan dari direktori utama proyek:

```sh
swift test --filter Ledger
```

[LedgerTests.swift](../Tests/LedgerTests.swift) memuat 16 pengujian dan [LedgerDeletionTests.swift](../Tests/LedgerDeletionTests.swift) memuat 7, mencakup:

- Encoding/decoding ulang contoh dan templat, format tanggal, hari kabisat, zona waktu dan batas hari/bulan/tahun/sepanjang waktu.
- Ringkasan jenis harian, jumlah anggota gabungan dan per grup, peringkat grup, total mata uang terpisah, peringkat seri, urutan stabil dan pangsa pengeluaran nol.
- Parsing unit terkecil, penolakan kelebihan desimal, harga nol, jumlah/harga tidak valid, serta overflow per pembelian dan agregat.
- Snapshot nama, warna dan harga; pembaruan katalog tidak menulis ulang riwayat.
- Penggabungan katalog menurut ID, deduplikasi pembelian, konflik isi berbeda dengan ID sama dan kegagalan penggabungan tanpa perubahan parsial.
- Field wajib, ID duplikat, keunikan keanggotaan dan harga, referensi, tanggal, mata uang, warna, JPEG dan batas ukuran file.
- Field arsip yang tidak ada dalam cadangan lama, pengarsipan anggota di semua grup, pengarsipan grup, filter keanggotaan aktif, encoding ulang arsip, pengeditan riwayat dan pemulihan katalog tanpa menghitung pembelian ulang.

## Pengujian UI native

Buka `OshicueKit.xcodeproj` di Xcode, pilih scheme `OshicueKit` dan simulator iPhone yang tersedia, lalu jalankan kelas pengujian terkait. Xcode, iOS SDK dan konfigurasi signing yang dibutuhkan proyek harus tersedia. Pengujian UI memakai file terpisah `ui-test-ledger.json`; penyimpanan diperiksa dengan menghentikan proses lalu membuka aplikasi kembali.

| File pengujian | Kasus | Cakupan utama |
| --- | --- | --- |
| [LedgerUITests.swift](../UITests/LedgerUITests.swift) | 6 | Penambahan dan penyimpanan setelah buka ulang, anggota gabungan/per grup, mata uang pengeluaran, impor tidak valid/valid, tambah anggota cepat dan harga bawaan, judul dan akses kalender sembilan bahasa |
| [LedgerEditingDeletionUITests.swift](../UITests/LedgerEditingDeletionUITests.swift) | 5 | Edit satu pembelian, pemilihan dan edit dari beberapa pembelian, konfirmasi hapus pembelian, hapus anggota di semua grup, hapus grup, edit riwayat dan penyimpanan setelah buka ulang |

[LedgerStore.swift](../OshicueKit/Support/LedgerStore.swift) menerapkan pemuatan contoh awal dan kebijakan penyimpanan. Kelas penyimpanan UI ini tidak termasuk paket inti. Perubahan kebijakan perlu pemeriksaan UI native dan buka ulang selain pengujian inti.

## Daftar penerimaan manual

1. Mulai catatan baru dengan data uji terpisah. Contoh muncul tanpa lencana; jumlah pada hari pembuatan 10 dan sepanjang waktu 16. Simpan edit lalu buka ulang: perubahan tetap ada. Catatan lama tidak boleh otomatis memulihkan contoh.
2. Tambahkan grup kedua bagi anggota yang sama dengan warna dan harga terpisah. Peringkat gabungan/per grup mengubah pengelompokan tetapi total jumlah pada cakupan yang sama tetap. Nama sama dengan ID berbeda dihitung terpisah.
3. Tambah dan edit pembelian. Periksa tanggal, mata uang, pengisian harga bawaan, jumlah, informasi acara opsional dan total per pembelian. Mengubah keanggotaan/jenis/mata uang mengisi ulang harga; periksa harga sebenarnya sebelum menyimpan.
4. Kartu satu pembelian membuka editor langsung; beberapa pembelian membuka pemilih. Penyimpanan hanya mengubah pilihan. Membatalkan hapus mempertahankan total; konfirmasi mengurangi jumlah dan nilai, termasuk setelah buka ulang.
5. Hapus anggota lintas grup menyembunyikan semua keanggotaannya. Hapus satu grup tetap menjaga anggota di grup lain. Jumlah dan nilai riwayat tidak berubah, riwayat masih dapat diedit, dan pembelian baru tidak bisa memilih keanggotaan yang dihapus.
6. Pastikan `CNY` harga `80.50` dan jumlah `2` menghasilkan `161.00`. Desimal `JPY`/`KRW` dan lebih dari dua digit desimal mata uang lain harus gagal. Jumlah dapat lintas mata uang; peringkat pengeluaran hanya mata uang terpilih.
7. Gunakan [ledger-template.json](../Templates/ledger-template.json) untuk impor tempelan, pratinjau dan ekspor. Contoh belum diubah dapat diganti; contoh sudah diubah harus digabung. Impor ulang tidak menghitung ganda, isi pembelian berbeda dengan ID sama memblokir seluruh impor, dan data tidak valid tidak mengubah catatan asli.
8. Pulihkan katalog arsip dari cadangan valid dan pastikan tiap pembelian tetap satu. Cadangan berisi isi lama dari pembelian yang telah diedit harus menghasilkan konflik.
9. Ganti ke sembilan bahasa aplikasi. Periksa Catatan, Kalender, Statistik, Anggota, edit, hapus dan kesalahan impor, serta tata letak nama panjang, nilai besar dan banyak mata uang.
10. Pada perangkat uji, periksa pemilihan foto nyata, penghapusan avatar, pemilih multi-file dan penyimpanan sistem, lalu migrasi cadangan. Pengujian otomatis tidak mencakup semua dialog sistem dan alur foto nyata.

## Pencatatan bukti

Untuk setiap eksekusi, catat revisi kode, tanggal dan zona waktu, versi Xcode/sistem/perangkat atau simulator, perintah atau kelas pengujian, pemeriksaan lulus/gagal/tidak dijalankan, serta log atau bundel hasil yang disimpan. `screenshots/ledger-*.png` membantu memahami UI, tetapi gambar statis tidak membuktikan pengujian terbaru lulus. Instalasi perangkat nyata, alur file sistem dan penerimaan rilis perlu dicatat terpisah.

## Uji ulang pengelolaan, favorit dan tampilan 0.4.0

Berikut definisi tes dan lingkup uji ulang yang ditambahkan untuk 0.4.0 / build 10, bukan pernyataan bahwa seluruh tes UI dijalankan dan lulus kali ini. Catat lulus, gagal atau belum dijalankan berdasarkan pelaksanaan nyata. Pemeriksaan palet juga tidak menggantikan inspeksi manual setiap layar.

| File tes | Kasus | Cakupan tambahan |
| --- | --- | --- |
| [LedgerManagementTests.swift](../Tests/LedgerManagementTests.swift) | 6 | Batas tanggal dan gabungan filter, kecocokan katalog riwayat, hasil kosong dan rentang tidak valid, jumlah rekaman/item/total per mata uang yang dipilih, penghapusan batch dan semua pembelian dengan katalog tetap ada. |
| [LedgerCatalogManagementTests.swift](../Tests/LedgerCatalogManagementTests.swift) | 3 | Arsip batch semua keanggotaan anggota atau hanya grup terpilih, riwayat tetap ada, pilihan kosong, ID tidak dikenal dan operasi berulang. |
| [LedgerFavoritesTests.swift](../Tests/LedgerFavoritesTests.swift) | 8 | Favorit tidak dicantumkan dalam cadangan lama, JSON bolak-balik dan tipe, penggabungan, arsip dikecualikan, harga keanggotaan, urutan stabil, pilihan awal dan identitas riwayat. |
| [DataManagementUITests.swift](../UITests/DataManagementUITests.swift) | 6 | Pilihan pembelian terfilter dengan Batal/Konfirmasi, perubahan filter menghapus pilihan, menghapus semua mata uang, penghapusan batch catatan harian dan formulir cadangan di pengelolaan Pengaturan. Pilihan beberapa data katalog, pembatalan/konfirmasi arsip dan riwayat pembelian tetap ada. |
| [DataManagementFailureUITests.swift](../UITests/DataManagementFailureUITests.swift) | 2 | Suntikkan kegagalan penyimpanan nyata dengan `OSHICUE_FAIL_BATCH_DELETE=1` pada data uji Debug terpisah. Periksa rekaman asli, isi catatan harian dan pilihan saat ini tetap ada, data tidak berubah setelah mulai ulang, lalu pilih kembali dan coba penghapusan setelah menonaktifkan kegagalan. |
| [LedgerFavoriteUITests.swift](../UITests/LedgerFavoriteUITests.swift) | 2 | Favorit bersama lintas grup dan tetap ada setelah mulai ulang, pintasan atas untuk beberapa keanggotaan, pilihan awal favorit dan penghapusan favorit. |
| [AppearanceUITests.swift](../UITests/AppearanceUITests.swift) | 5 | Palet dinamis UIKit sebenarnya, kontras teks dan kontrol terpilih dalam kedua tampilan, latar tempat, peningkatan kontras dan keterbacaan warna anggota; tes tidak mengoperasikan layar aplikasi. |
| [AppearanceScreenUITests.swift](../UITests/AppearanceScreenUITests.swift) | 1 | Operasikan empat modul, favorit dan pemilihan anggota, editor, pengelolaan serta cadangan dalam tampilan terang atau gelap sistem. Aplikasi tidak memaksakan tema; pemeriksaan layar diikuti 13 lampiran tangkapan layar nyata. |

```sh
swift test --filter Ledger
xcodebuild test -project OshicueKit.xcodeproj -scheme OshicueKit -destination 'platform=iOS Simulator,id=<SIMULATOR_ID>' -only-testing:OshicueKitUITests/DataManagementUITests -only-testing:OshicueKitUITests/LedgerFavoriteUITests -only-testing:OshicueKitUITests/AppearanceUITests -only-testing:OshicueKitUITests/DataManagementFailureUITests -only-testing:OshicueKitUITests/AppearanceScreenUITests
```

## Langkah uji ulang manual tambahan

- Buka kedua layar pengelolaan dari Pengaturan dan periksa impor/ekspor lama di Cadangan. Gabungkan filter, pilih sebagian hasil dan periksa jumlah konfirmasi; batalkan dahulu, lalu konfirmasi dan mulai ulang. Konfirmasi pembelian harus memisahkan mata uang dan perubahan filter harus menghapus pilihan.
- Arsipkan anggota beberapa grup dan satu grup secara batch; periksa lingkup, keanggotaan lain dan edit riwayat. Hapus pembelian dan pastikan katalog serta catatan harian tetap ada. Hapus hasil catatan harian menurut status, tanggal dan pencarian; periksa isi yang tidak dipilih dan modul lain.
- Atur favorit, buka ulang lewat keanggotaan lain dan periksa preferensi bersama, bagian atas pemilih, harga tiap keanggotaan dan pilihan awal rekaman baru. Periksa pembatalan favorit, arsip, ekspor dan impor ulang. JSON lama tanpa `isFavorite` harus bukan favorit dan `schemaVersion` tetap `1`.
- Ganti tampilan sistem terang/gelap dan periksa empat modul, detail, editor, pemilih, pengelolaan dan cadangan. Periksa juga teks besar, label panjang sembilan bahasa, avatar, warna anggota, jumlah uang, tombol terpilih dan kesalahan. Catat perangkat dan versi sistem; gambar lama tidak menggantikan hasil terbaru.

Kembali ke [README](../README.id.md) · [Panduan Catatan dan format data](LEDGER.id.md) · [Templat JSON](../Templates/ledger-template.json)
