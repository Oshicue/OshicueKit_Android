# Panduan verifikasi catatan harian dan pengaturan global

[简体中文](DIARY-VERIFICATION.md) | [繁體中文](DIARY-VERIFICATION.zh-Hant.md) | [繁體粵語](DIARY-VERIFICATION.zh-HK.md) | [English](DIARY-VERIFICATION.en.md) | [日本語](DIARY-VERIFICATION.ja.md) | [한국어](DIARY-VERIFICATION.ko.md) | [ไทย](DIARY-VERIFICATION.th.md) | [Bahasa Indonesia](DIARY-VERIFICATION.id.md) | [Bahasa Melayu](DIARY-VERIFICATION.ms.md)

Halaman ini menjelaskan cakupan pengujian dan cara mengulangnya pada repositori saat ini, bukan pernyataan bahwa pengujian telah lulus di lingkungan saat ini. Konfigurasi proyek adalah 0.4.0 / build 10 dengan minimum iOS 17; Swift Package inti juga mendukung macOS 14. Catat commit, versi Xcode, perangkat, dan keluaran untuk setiap pengujian nyata.

## Pengujian inti

[DiaryTests.swift](../Tests/DiaryTests.swift) mendefinisikan 18 pengujian catatan harian. Jalankan dari akar proyek:

```sh
swift test --filter DiaryTests
```

| Cakupan | Pemeriksaan |
| --- | --- |
| Draf dan salinan acara | Catatan kosong dan draf tidak lengkap valid; detail acara mandiri; jika tanggal acara tidak valid atau tidak ada, gunakan tanggal terpilih. |
| Teks dan waktu | Isi Unicode panjang, spasi, dan baris baru dipertahankan saat konversi bolak-balik; deduplikasi berpresisi milidetik; offset zona waktu menunjukkan waktu absolut yang sama. |
| Penggabungan dan konflik | Catatan identik disatukan dan yang baru ditambahkan; perbedaan isi, ringkasan, tag, status, alamat, atau waktu perubahan pada ID yang sama menolak seluruh kumpulan. |
| Validasi data | ID duplikat atau kosong, tanggal dan urutan waktu tidak valid, versi, bidang wajib, tipe bidang, waktu, JSON dan UTF-8 tidak valid, serta batas 20 MiB. |
| Perapian dan ekspor | Pemisahan dan deduplikasi tag menjaga perbedaan bunyi Jepang dan aksen; pencarian beberapa kata lintas bidang; teks biasa dan Markdown mencakup detail acara, ringkasan, isi asli, dan escape judul. |

Perilaku duplikat, konflik, batas penyimpanan, dan ekspor yang diuji mengacu pada kode saat ini. Perbarui halaman ini jika jumlah atau cakupan pengujian berubah.

## Pengujian UI native

[DiaryUITests.swift](../UITests/DiaryUITests.swift) mendefinisikan 5 pengujian:

1. Pilih acara Catatan, masukkan isi beberapa baris, ringkasan dan tag, lalu pindahkan aplikasi ke latar belakang. Setelah dimulai ulang, periksa detail acara dan teks serta pastikan hanya ada satu catatan.
2. Pratinjau dan impor JSON; impor ulang menyatukan duplikat, perubahan isi pada ID yang sama ditolak, dan isi asli tetap ada setelah dimulai ulang.
3. Alur batal dan konfirmasi penghapusan; setelah dimulai ulang catatan tetap terhapus, sedangkan acara Catatan dan catatan pembelian tetap ada.
4. Buka “Kelola catatan harian” lalu pilih “Cadangan” dari tab Pengaturan keempat, beralih ke Inggris lintas modul, dan pertahankan isi catatan asli.
5. Periksa judul catatan harian, tombol catatan baru, tab Pengaturan keempat, dan keadaan kosong pada sembilan bahasa. Ini tidak berarti semua teks UI sudah diperiksa manual.

Tampilkan simulator lokal yang tersedia, lalu ganti `<SIMULATOR_ID>` dengan ID perangkat nyata:

```sh
xcrun simctl list devices available
xcodebuild test -project OshicueKit.xcodeproj -scheme OshicueKit -destination 'platform=iOS Simulator,id=<SIMULATOR_ID>' -only-testing:OshicueKitUITests/DiaryUITests
```

Dalam Debug, pengujian memakai `OSHICUE_UI_TESTS=1` dan file terpisah `ui-test-diary.json`; alur mempertahankan data saat mulai ulang menggunakan `OSHICUE_PRESERVE_UI_DATA=1`. Variabel ini mengisolasi pengujian, bukan kontrol cadangan pengguna.

## Pemeriksaan bahasa, manual, dan bukti

Jalankan skrip berikut di macOS untuk memeriksa kumpulan kunci, nilai tidak kosong, placeholder, dan pembacaan `.strings` pada sembilan bahasa. Skrip tidak menilai makna terjemahan atau tata letak visual:

```sh
python3 scripts/verify-localizations.py
```

Pemeriksaan manual perlu mencakup simpan otomatis dan percobaan ulang setelah gagal, pemilihan acara dan salinan mandiri, pencarian dan filter, batal/konfirmasi hapus, penggabungan beberapa file dan konflik, penolakan cadangan kosong, penyimpanan serta pembacaan ulang `.md`／`.txt`／JSON nyata, pembatalan berbagi sistem, dan teks panjang serta tanggal pada sembilan bahasa. Jangan memilih penerima berbagi tanpa izin.

Gambar [daftar catatan](../screenshots/diary-list.png), [editor](../screenshots/diary-editor.png), [detail acara](../screenshots/diary-editor-event.png), [pratinjau cadangan](../screenshots/diary-backup-preview.png), [pesan konflik](../screenshots/diary-backup-conflict.png), dan [pengaturan global](../screenshots/global-settings.png) yang ada adalah referensi repositori, bukan bukti build saat ini telah lulus. Penerimaan berbagi, pemasangan pada perangkat fisik, publikasi, dan pemulihan kegagalan simpan otomatis tidak bisa dinilai dari pengujian inti saja. Catat hasilnya secara terpisah saat benar-benar dilakukan.

## Uji ulang pengelolaan, favorit dan tampilan 0.4.0

Berikut definisi tes dan lingkup uji ulang yang ditambahkan untuk 0.4.0 / build 10, bukan pernyataan bahwa seluruh tes UI dijalankan dan lulus kali ini. Catat lulus, gagal atau belum dijalankan berdasarkan pelaksanaan nyata. Pemeriksaan palet juga tidak menggantikan inspeksi manual setiap layar.

| File tes | Kasus | Cakupan tambahan |
| --- | --- | --- |
| [DiaryManagementTests.swift](../Tests/DiaryManagementTests.swift) | 4 | Gabungan status/tanggal/pencarian, rentang tidak valid, penghapusan pilihan mempertahankan isi entri lain dan konteks acara, cadangan catatan harian kosong. |
| [DataManagementUITests.swift](../UITests/DataManagementUITests.swift) | 6 | Pilihan pembelian terfilter dengan Batal/Konfirmasi, perubahan filter menghapus pilihan, menghapus semua mata uang, penghapusan batch catatan harian dan formulir cadangan di pengelolaan Pengaturan. Pilihan beberapa data katalog, pembatalan/konfirmasi arsip dan riwayat pembelian tetap ada. |
| [DataManagementFailureUITests.swift](../UITests/DataManagementFailureUITests.swift) | 2 | Suntikkan kegagalan penyimpanan nyata dengan `OSHICUE_FAIL_BATCH_DELETE=1` pada data uji Debug terpisah. Periksa rekaman asli, isi catatan harian dan pilihan saat ini tetap ada, data tidak berubah setelah mulai ulang, lalu pilih kembali dan coba penghapusan setelah menonaktifkan kegagalan. |
| [AppearanceUITests.swift](../UITests/AppearanceUITests.swift) | 5 | Palet dinamis UIKit sebenarnya, kontras teks dan kontrol terpilih dalam kedua tampilan, latar tempat, peningkatan kontras dan keterbacaan warna anggota; tes tidak mengoperasikan layar aplikasi. |

```sh
swift test --filter Diary
xcodebuild test -project OshicueKit.xcodeproj -scheme OshicueKit -destination 'platform=iOS Simulator,id=<SIMULATOR_ID>' -only-testing:OshicueKitUITests/DataManagementUITests -only-testing:OshicueKitUITests/AppearanceUITests -only-testing:OshicueKitUITests/DataManagementFailureUITests
```

## Langkah uji ulang manual tambahan

- Buka kedua layar pengelolaan dari Pengaturan dan periksa impor/ekspor lama di Cadangan. Gabungkan filter, pilih sebagian hasil dan periksa jumlah konfirmasi; batalkan dahulu, lalu konfirmasi dan mulai ulang. Konfirmasi pembelian harus memisahkan mata uang dan perubahan filter harus menghapus pilihan.
- Arsipkan anggota beberapa grup dan satu grup secara batch; periksa lingkup, keanggotaan lain dan edit riwayat. Hapus pembelian dan pastikan katalog serta catatan harian tetap ada. Hapus hasil catatan harian menurut status, tanggal dan pencarian; periksa isi yang tidak dipilih dan modul lain.
- Atur favorit, buka ulang lewat keanggotaan lain dan periksa preferensi bersama, bagian atas pemilih, harga tiap keanggotaan dan pilihan awal rekaman baru. Periksa pembatalan favorit, arsip, ekspor dan impor ulang. JSON lama tanpa `isFavorite` harus bukan favorit dan `schemaVersion` tetap `1`.
- Ganti tampilan sistem terang/gelap dan periksa empat modul, detail, editor, pemilih, pengelolaan dan cadangan. Periksa juga teks besar, label panjang sembilan bahasa, avatar, warna anggota, jumlah uang, tombol terpilih dan kesalahan. Catat perangkat dan versi sistem; gambar lama tidak menggantikan hasil terbaru.

Kembali ke [README](../README.id.md) · Terkait: [Catatan harian](DIARY.id.md) · [Verifikasi keseluruhan](VERIFICATION.id.md) · [Templat catatan harian](../Templates/diary-template.json)
