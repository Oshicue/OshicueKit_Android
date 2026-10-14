# Panduan pengesahan Catatan

[简体中文](LEDGER-VERIFICATION.md) | [繁體中文](LEDGER-VERIFICATION.zh-Hant.md) | [繁體粵語](LEDGER-VERIFICATION.zh-HK.md) | [English](LEDGER-VERIFICATION.en.md) | [日本語](LEDGER-VERIFICATION.ja.md) | [한국어](LEDGER-VERIFICATION.ko.md) | [ไทย](LEDGER-VERIFICATION.th.md) | [Bahasa Indonesia](LEDGER-VERIFICATION.id.md) | [Bahasa Melayu](LEDGER-VERIFICATION.ms.md)

Halaman ini menerangkan liputan ujian sedia ada dan semakan penerimaan yang boleh diulang, berdasarkan kod sumber semasa. Kewujudan fail ujian tidak bermaksud ujian lulus dalam pelaksanaan ini. Laporan versi lama, himpunan hasil sementara dan tangkapan skrin bukan bukti versi semasa lulus. Gunakan log pelaksanaan terkini untuk hasil sebenar.

## Ujian teras

Jalankan dari direktori akar projek:

```sh
swift test --filter Ledger
```

[LedgerTests.swift](../Tests/LedgerTests.swift) mempunyai 16 ujian dan [LedgerDeletionTests.swift](../Tests/LedgerDeletionTests.swift) mempunyai 7, meliputi:

- Pengekodan dan penyahkodan semula contoh/templat, format tarikh, hari lompat, zon waktu dan sempadan hari/bulan/tahun/sepanjang masa.
- Ringkasan jenis harian, jumlah ahli gabungan dan mengikut kumpulan, kedudukan kumpulan, jumlah mata wang berasingan, kedudukan seri, susunan stabil dan bahagian perbelanjaan sifar.
- Penghuraian unit terkecil, penolakan lebihan perpuluhan, harga sifar, kuantiti/harga tidak sah, serta limpahan pembelian dan agregat.
- Petikan nama, warna dan harga; kemas kini katalog tidak menulis semula sejarah.
- Gabungan katalog mengikut ID, penyahgandaan pembelian, konflik kandungan berbeza dengan ID sama dan kegagalan gabungan tanpa perubahan separa.
- Medan wajib, ID pendua, keunikan keahlian dan harga, rujukan, tarikh, mata wang, warna, JPEG dan had saiz fail.
- Medan arkib yang tiada dalam sandaran lama, pengarkiban ahli dalam semua kumpulan, pengarkiban kumpulan, penapis keahlian aktif, pengekodan semula arkib, suntingan sejarah dan pemulihan katalog tanpa mengira pembelian dua kali.

## Ujian UI natif

Buka `OshicueKit.xcodeproj` dalam Xcode, pilih scheme `OshicueKit` serta simulator iPhone yang tersedia, kemudian jalankan kelas ujian berkaitan. Xcode, iOS SDK dan tetapan penandatanganan yang diperlukan projek mesti tersedia. Ujian UI menggunakan fail berasingan `ui-test-ledger.json`; penyimpanan diperiksa dengan menamatkan proses dan melancarkan semula aplikasi.

| Fail ujian | Kes | Liputan utama |
| --- | --- | --- |
| [LedgerUITests.swift](../UITests/LedgerUITests.swift) | 6 | Penambahan dan penyimpanan selepas pelancaran semula, ahli gabungan/mengikut kumpulan, mata wang perbelanjaan, import tidak sah/sah, tambah ahli pantas dan harga lalai, tajuk serta akses kalendar sembilan bahasa |
| [LedgerEditingDeletionUITests.swift](../UITests/LedgerEditingDeletionUITests.swift) | 5 | Sunting satu pembelian, pilih dan sunting antara beberapa pembelian, pengesahan padam pembelian, padam ahli semua kumpulan, padam kumpulan, sunting sejarah dan penyimpanan selepas pelancaran semula |

[LedgerStore.swift](../OshicueKit/Support/LedgerStore.swift) melaksanakan pemuatan contoh awal dan dasar penyimpanan. Kelas penyimpanan UI ini tidak termasuk pakej teras. Perubahan dasar memerlukan semakan UI natif dan pelancaran semula selain ujian teras.

## Senarai penerimaan manual

1. Mulakan catatan baharu dengan data ujian berasingan. Contoh dipaparkan tanpa lencana; kuantiti hari penciptaan 10 dan sepanjang masa 16. Simpan suntingan dan lancarkan semula: perubahan mesti kekal. Catatan sedia ada tidak boleh mengembalikan contoh secara automatik.
2. Tambah kumpulan kedua untuk ahli sama dengan warna dan harga berasingan. Kedudukan gabungan/mengikut kumpulan mengubah pengelompokan tetapi jumlah kuantiti dalam skop sama kekal. Nama sama dengan ID berbeza dikira berasingan.
3. Tambah dan sunting pembelian. Periksa tarikh, mata wang, pengisian harga lalai, kuantiti, maklumat acara pilihan dan jumlah setiap pembelian. Menukar keahlian/jenis/mata wang mengisi harga semula; semak harga sebenar sebelum menyimpan.
4. Kad satu pembelian membuka penyunting terus; beberapa pembelian membuka pemilih dahulu. Simpan hanya mengubah rekod terpilih. Membatalkan pemadaman mengekalkan jumlah; pengesahan menolak kuantiti dan amaun, termasuk selepas pelancaran semula.
5. Memadam ahli merentas kumpulan menyembunyikan semua keahliannya. Memadam satu kumpulan mengekalkan ahli dalam kumpulan lain. Jumlah kuantiti dan amaun sejarah tidak berubah, sejarah masih boleh disunting, dan pembelian baharu tidak boleh memilih keahlian dipadam.
6. Pastikan `CNY` harga `80.50` dan kuantiti `2` memberikan `161.00`. Perpuluhan `JPY`/`KRW` dan lebih dua tempat perpuluhan mata wang lain mesti gagal. Kuantiti boleh merentas mata wang; kedudukan perbelanjaan hanya menggunakan mata wang terpilih.
7. Gunakan [ledger-template.json](../Templates/ledger-template.json) untuk import tampalan, pratonton dan eksport. Contoh belum diubah boleh diganti; contoh disunting mesti digabung. Import semula tidak mengira dua kali, pembelian berbeza dengan ID sama menghalang seluruh import, dan data tidak sah tidak mengubah catatan asal.
8. Pulihkan katalog arkib dengan sandaran sah dan pastikan setiap pembelian masih satu. Sandaran yang mengandungi versi lama pembelian disunting mesti melaporkan konflik.
9. Tukar antara sembilan bahasa aplikasi. Periksa Catatan, Kalendar, Statistik, Ahli, suntingan, pemadaman dan ralat import, serta susun atur nama panjang, amaun besar dan berbilang mata wang.
10. Pada peranti ujian, periksa pemilihan foto sebenar, pembuangan avatar, pemilih berbilang fail dan penyimpanan sistem, kemudian pemindahan sandaran. Ujian automatik tidak meliputi semua dialog sistem dan aliran foto sebenar.

## Merekod bukti

Bagi setiap pelaksanaan, catat semakan kod, tarikh dan zon waktu, versi Xcode/sistem/peranti atau simulator, arahan atau kelas ujian, semakan lulus/gagal/tidak dilaksanakan, serta log atau himpunan hasil yang disimpan. `screenshots/ledger-*.png` membantu memahami UI, tetapi gambar statik tidak membuktikan ujian semasa lulus. Pemasangan peranti sebenar, aliran fail sistem dan penerimaan keluaran perlu direkodkan berasingan.

## Uji semula pengurusan, kegemaran dan paparan 0.4.0

Berikut ialah definisi ujian dan skop uji semula yang ditambah untuk 0.4.0 / build 10, bukan pengesahan bahawa semua ujian UI dijalankan dan lulus kali ini. Rekod lulus, gagal atau belum dijalankan berdasarkan pelaksanaan sebenar. Semakan palet juga tidak menggantikan pemeriksaan manual setiap halaman.

| Fail ujian | Kes | Liputan tambahan |
| --- | --- | --- |
| [LedgerManagementTests.swift](../Tests/LedgerManagementTests.swift) | 6 | Sempadan tarikh dan gabungan penapis, padanan katalog sejarah, hasil kosong dan julat tidak sah, bilangan rekod/item/amaun setiap mata wang dipilih, pemadaman kelompok dan semua pembelian dengan katalog dikekalkan. |
| [LedgerCatalogManagementTests.swift](../Tests/LedgerCatalogManagementTests.swift) | 3 | Arkib kelompok semua keahlian ahli atau hanya kumpulan dipilih, sejarah dikekalkan, pilihan kosong, ID tidak diketahui dan operasi berulang. |
| [LedgerFavoritesTests.swift](../Tests/LedgerFavoritesTests.swift) | 8 | Medan kegemaran tiada dalam sandaran lama, pergi balik JSON dan jenis, gabungan, pengecualian arkib, harga keahlian, susunan stabil, pilihan lalai dan identiti sejarah. |
| [DataManagementUITests.swift](../UITests/DataManagementUITests.swift) | 6 | Pilihan pembelian ditapis dengan Batal/Sahkan, perubahan penapis mengosongkan pilihan, memadam semua mata wang, pemadaman kelompok diari dan borang sandaran dalam pengurusan Tetapan. Pilihan berbilang data katalog, batal/sahkan arkib dan sejarah pembelian dikekalkan. |
| [DataManagementFailureUITests.swift](../UITests/DataManagementFailureUITests.swift) | 2 | Suntik kegagalan penyimpanan sebenar dengan `OSHICUE_FAIL_BATCH_DELETE=1` pada data ujian Debug berasingan. Semak rekod asal, teks diari dan pilihan semasa dikekalkan, data tidak berubah selepas mula semula, kemudian pilih semula dan cuba pemadaman selepas menyahaktifkan kegagalan. |
| [LedgerFavoriteUITests.swift](../UITests/LedgerFavoriteUITests.swift) | 2 | Kegemaran dikongsi merentas kumpulan dan kekal selepas mula semula, pintasan atas bagi berbilang keahlian, lalai kegemaran dan membuang kegemaran. |
| [AppearanceUITests.swift](../UITests/AppearanceUITests.swift) | 5 | Palet dinamik UIKit sebenar, kontras teks dan kawalan dipilih dalam dua paparan, latar tempat, peningkatan kontras dan kebolehbacaan warna ahli; ujian tidak mengendalikan halaman aplikasi. |
| [AppearanceScreenUITests.swift](../UITests/AppearanceScreenUITests.swift) | 1 | Kendalikan empat modul, kegemaran dan pilihan ahli, suntingan, pengurusan serta sandaran dalam paparan cerah atau gelap sistem. Aplikasi tidak memaksa tema; semakan halaman diikuti 13 lampiran tangkapan skrin sebenar. |

```sh
swift test --filter Ledger
xcodebuild test -project OshicueKit.xcodeproj -scheme OshicueKit -destination 'platform=iOS Simulator,id=<SIMULATOR_ID>' -only-testing:OshicueKitUITests/DataManagementUITests -only-testing:OshicueKitUITests/LedgerFavoriteUITests -only-testing:OshicueKitUITests/AppearanceUITests -only-testing:OshicueKitUITests/DataManagementFailureUITests -only-testing:OshicueKitUITests/AppearanceScreenUITests
```

## Langkah uji semula manual tambahan

- Buka kedua-dua halaman pengurusan dari Tetapan dan semak import/eksport asal di Sandaran. Gabungkan penapis, pilih sebahagian hasil dan semak bilangan pengesahan; batal dahulu, kemudian sahkan dan mula semula. Pengesahan pembelian mesti mengasingkan mata wang dan perubahan penapis mesti mengosongkan pilihan.
- Arkibkan ahli berbilang kumpulan dan satu kumpulan secara kelompok; semak skop, keahlian lain dan suntingan sejarah. Padam pembelian dan pastikan katalog serta diari kekal. Padam hasil diari mengikut status, tarikh dan carian; semak teks tidak dipilih dan modul lain.
- Tetapkan kegemaran, buka semula melalui keahlian lain dan semak pilihan dikongsi, bahagian atas pemilih, harga keahlian dan lalai rekod baharu. Semak nyahpilih, arkib, eksport dan import semula. JSON lama tanpa `isFavorite` mesti bukan kegemaran dan `schemaVersion` kekal `1`.
- Tukar paparan sistem cerah/gelap dan semak empat modul, butiran, penyunting, pemilih, pengurusan dan sandaran. Semak teks besar, label panjang sembilan bahasa, avatar, warna ahli, amaun, butang dipilih dan ralat. Rekod peranti dan versi sistem; gambar lama tidak menggantikan hasil semasa.

Kembali ke [README](../README.ms.md) · [Panduan Catatan dan format data](LEDGER.ms.md) · [Templat JSON](../Templates/ledger-template.json)
