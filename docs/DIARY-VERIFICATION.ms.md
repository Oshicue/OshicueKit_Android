# Panduan pengesahan diari dan tetapan global

[简体中文](DIARY-VERIFICATION.md) | [繁體中文](DIARY-VERIFICATION.zh-Hant.md) | [繁體粵語](DIARY-VERIFICATION.zh-HK.md) | [English](DIARY-VERIFICATION.en.md) | [日本語](DIARY-VERIFICATION.ja.md) | [한국어](DIARY-VERIFICATION.ko.md) | [ไทย](DIARY-VERIFICATION.th.md) | [Bahasa Indonesia](DIARY-VERIFICATION.id.md) | [Bahasa Melayu](DIARY-VERIFICATION.ms.md)

Halaman ini menerangkan liputan ujian dan cara mengulangnya bagi repositori semasa, bukan pernyataan bahawa ujian sudah lulus dalam persekitaran semasa. Konfigurasi projek ialah 0.4.0 / build 10 dengan minimum iOS 17; Swift Package teras turut menyokong macOS 14. Rekodkan komit, versi Xcode, peranti dan output bagi setiap ujian sebenar.

## Ujian teras

[DiaryTests.swift](../Tests/DiaryTests.swift) mentakrifkan 18 ujian diari. Jalankan dari akar projek:

```sh
swift test --filter DiaryTests
```

| Liputan | Pemeriksaan |
| --- | --- |
| Draf dan salinan acara | Diari kosong dan draf tidak lengkap sah; butiran acara berasingan; jika tiada tarikh acara sah, gunakan tarikh yang dipilih. |
| Teks dan masa | Kandungan Unicode panjang, ruang dan baris baharu dikekalkan selepas penukaran pergi balik; nyahpendua pada ketepatan milisaat; ofset zon waktu mewakili waktu mutlak yang sama. |
| Penggabungan dan konflik | Catatan sama dinyahpendua dan catatan baharu ditambah; perbezaan kandungan, ringkasan, tag, status, alamat atau masa pengubahsuaian pada ID sama menolak seluruh kumpulan. |
| Pengesahan data | ID pendua atau kosong, tarikh dan susunan masa tidak sah, versi, medan wajib, jenis medan, masa, JSON dan UTF-8 tidak sah serta had 20 MiB. |
| Penyusunan dan eksport | Pemisahan dan nyahpendua tag mengekalkan perbezaan bunyi Jepun dan aksen; carian beberapa perkataan merentas medan; teks biasa dan Markdown merangkumi butiran acara, ringkasan, kandungan asal dan pelepasan aksara tajuk. |

Tingkah laku pendua, konflik, had penyimpanan dan eksport yang diuji berdasarkan kod semasa. Kemas kini halaman ini jika bilangan atau liputan ujian berubah.

## Ujian UI natif

[DiaryUITests.swift](../UITests/DiaryUITests.swift) mentakrifkan 5 ujian:

1. Pilih acara Catatan, masukkan kandungan berbilang baris, ringkasan dan tag, kemudian alihkan aplikasi ke latar belakang. Selepas dimulakan semula, semak butiran acara dan teks serta pastikan hanya satu catatan wujud.
2. Pratonton dan import JSON; import semula membuang pendua, perubahan kandungan pada ID sama ditolak dan kandungan asal kekal selepas dimulakan semula.
3. Aliran batal dan sahkan pemadaman; selepas dimulakan semula catatan tetap dipadam, sementara acara Catatan dan rekod pembelian kekal.
4. Buka “Urus Diari” dan pilih “Sandaran” daripada tab Tetapan keempat, tukar kepada bahasa Inggeris merentas modul dan kekalkan kandungan diari asal.
5. Semak tajuk diari, butang catatan baharu, tab Tetapan keempat dan keadaan kosong dalam kesemua sembilan bahasa. Ini tidak bermaksud setiap teks UI sudah disemak secara manual.

Senaraikan simulator setempat yang tersedia, kemudian gantikan `<SIMULATOR_ID>` dengan ID peranti sebenar:

```sh
xcrun simctl list devices available
xcodebuild test -project OshicueKit.xcodeproj -scheme OshicueKit -destination 'platform=iOS Simulator,id=<SIMULATOR_ID>' -only-testing:OshicueKitUITests/DiaryUITests
```

Dalam Debug, ujian menggunakan `OSHICUE_UI_TESTS=1` dan fail berasingan `ui-test-diary.json`; aliran pengekalan semasa mula semula menggunakan `OSHICUE_PRESERVE_UI_DATA=1`. Pemboleh ubah ini mengasingkan ujian, bukan kawalan sandaran pengguna.

## Semakan bahasa, manual dan bukti

Jalankan skrip berikut pada macOS untuk menyemak set kunci, nilai tidak kosong, ruang letak dan penghuraian `.strings` dalam sembilan bahasa. Skrip tidak menilai makna terjemahan atau susun atur visual:

```sh
python3 scripts/verify-localizations.py
```

Semakan manual perlu meliputi simpan automatik dan cubaan semula selepas gagal, pemilihan acara dan salinan berasingan, carian dan penapis, batal/sahkan padam, penggabungan beberapa fail dan konflik, penolakan sandaran kosong, penyimpanan dan bacaan semula `.md`／`.txt`／JSON sebenar, pembatalan perkongsian sistem serta teks panjang dan tarikh dalam sembilan bahasa. Jangan pilih penerima perkongsian tanpa kebenaran.

Imej [senarai diari](../screenshots/diary-list.png), [penyunting](../screenshots/diary-editor.png), [butiran acara](../screenshots/diary-editor-event.png), [pratonton sandaran](../screenshots/diary-backup-preview.png), [mesej konflik](../screenshots/diary-backup-conflict.png) dan [tetapan global](../screenshots/global-settings.png) sedia ada ialah rujukan repositori, bukan bukti binaan semasa sudah lulus. Penerimaan perkongsian, pemasangan pada peranti fizikal, penerbitan dan pemulihan kegagalan simpan automatik tidak boleh dinilai melalui ujian teras sahaja. Rekodkan hasilnya secara berasingan apabila benar-benar dilaksanakan.

## Uji semula pengurusan, kegemaran dan paparan 0.4.0

Berikut ialah definisi ujian dan skop uji semula yang ditambah untuk 0.4.0 / build 10, bukan pengesahan bahawa semua ujian UI dijalankan dan lulus kali ini. Rekod lulus, gagal atau belum dijalankan berdasarkan pelaksanaan sebenar. Semakan palet juga tidak menggantikan pemeriksaan manual setiap halaman.

| Fail ujian | Kes | Liputan tambahan |
| --- | --- | --- |
| [DiaryManagementTests.swift](../Tests/DiaryManagementTests.swift) | 4 | Gabungan status/tarikh/carian, julat tidak sah, pemadaman pilihan mengekalkan teks lain dan konteks acara, sandaran diari kosong. |
| [DataManagementUITests.swift](../UITests/DataManagementUITests.swift) | 6 | Pilihan pembelian ditapis dengan Batal/Sahkan, perubahan penapis mengosongkan pilihan, memadam semua mata wang, pemadaman kelompok diari dan borang sandaran dalam pengurusan Tetapan. Pilihan berbilang data katalog, batal/sahkan arkib dan sejarah pembelian dikekalkan. |
| [DataManagementFailureUITests.swift](../UITests/DataManagementFailureUITests.swift) | 2 | Suntik kegagalan penyimpanan sebenar dengan `OSHICUE_FAIL_BATCH_DELETE=1` pada data ujian Debug berasingan. Semak rekod asal, teks diari dan pilihan semasa dikekalkan, data tidak berubah selepas mula semula, kemudian pilih semula dan cuba pemadaman selepas menyahaktifkan kegagalan. |
| [AppearanceUITests.swift](../UITests/AppearanceUITests.swift) | 5 | Palet dinamik UIKit sebenar, kontras teks dan kawalan dipilih dalam dua paparan, latar tempat, peningkatan kontras dan kebolehbacaan warna ahli; ujian tidak mengendalikan halaman aplikasi. |

```sh
swift test --filter Diary
xcodebuild test -project OshicueKit.xcodeproj -scheme OshicueKit -destination 'platform=iOS Simulator,id=<SIMULATOR_ID>' -only-testing:OshicueKitUITests/DataManagementUITests -only-testing:OshicueKitUITests/AppearanceUITests -only-testing:OshicueKitUITests/DataManagementFailureUITests
```

## Langkah uji semula manual tambahan

- Buka kedua-dua halaman pengurusan dari Tetapan dan semak import/eksport asal di Sandaran. Gabungkan penapis, pilih sebahagian hasil dan semak bilangan pengesahan; batal dahulu, kemudian sahkan dan mula semula. Pengesahan pembelian mesti mengasingkan mata wang dan perubahan penapis mesti mengosongkan pilihan.
- Arkibkan ahli berbilang kumpulan dan satu kumpulan secara kelompok; semak skop, keahlian lain dan suntingan sejarah. Padam pembelian dan pastikan katalog serta diari kekal. Padam hasil diari mengikut status, tarikh dan carian; semak teks tidak dipilih dan modul lain.
- Tetapkan kegemaran, buka semula melalui keahlian lain dan semak pilihan dikongsi, bahagian atas pemilih, harga keahlian dan lalai rekod baharu. Semak nyahpilih, arkib, eksport dan import semula. JSON lama tanpa `isFavorite` mesti bukan kegemaran dan `schemaVersion` kekal `1`.
- Tukar paparan sistem cerah/gelap dan semak empat modul, butiran, penyunting, pemilih, pengurusan dan sandaran. Semak teks besar, label panjang sembilan bahasa, avatar, warna ahli, amaun, butang dipilih dan ralat. Rekod peranti dan versi sistem; gambar lama tidak menggantikan hasil semasa.

Kembali ke [README](../README.ms.md) · Berkaitan: [Diari](DIARY.ms.md) · [Pengesahan keseluruhan](VERIFICATION.ms.md) · [Templat diari](../Templates/diary-template.json)
