# Panduan pengesahan jadual

[简体中文](VERIFICATION.md) | [繁體中文](VERIFICATION.zh-Hant.md) | [繁體粵語](VERIFICATION.zh-HK.md) | [English](VERIFICATION.en.md) | [日本語](VERIFICATION.ja.md) | [한국어](VERIFICATION.ko.md) | [ไทย](VERIFICATION.th.md) | [Bahasa Indonesia](VERIFICATION.id.md) | [Bahasa Melayu](VERIFICATION.ms.md)

Halaman ini menerangkan skop pengesahan dalam sumber semasa dan cara mengulangi semakan. Kewujudan fail ujian tidak bermakna ujian telah dijalankan dalam kemas kini ini. Tangkapan skrin lama ialah rujukan antara muka, bukan bukti bahawa binaan semasa lulus.

## Semakan yang boleh diulang

Jalankan dari direktori akar projek:

```sh
swift test
python3 scripts/verify-localizations.py
```

`swift test` menjalankan ujian teras Foundation, bukan ujian antara muka iOS. Semakan penyetempatan meliputi set kunci, teks tidak kosong, pemegang tempat format dan sintaks `.strings` bagi kesemua sembilan bahasa.

| Fail | Skop |
| --- | --- |
| [ScheduleImporterTests.swift](../Tests/ScheduleImporterTests.swift) | Ralat format dan medan, rujukan dan ID pendua, ofset masa, pecahan saat, tengah malam, waktu musim panas, kemas kini acara lengkap, templat dan penjanaan demo awal. |
| [TimelineLayoutTests.swift](../Tests/TimelineLayoutTests.swift) | Sempadan masa bersambung, lorong pertindihan, bilangan lorong serentak minimum, susunan stabil, lebar lajur padat dan tiada perlanggaran mendatar. |
| [ScheduleDeletionTests.swift](../Tests/ScheduleDeletionTests.swift) | Pemadaman acara, kegemaran bersama, nama sama dengan ID berbeza, ID tidak dikenali, pengekodan dokumen kosong dan pemuatan semula. |

## Semakan semula antara muka natif

Buka projek dalam Xcode dan jalankan ujian UI pada simulator iOS. Kes utama terdapat dalam [OshicueKitUITests.swift](../UITests/OshicueKitUITests.swift), [TimelineReadabilityUITests.swift](../UITests/TimelineReadabilityUITests.swift) dan [ScheduleDeletionUITests.swift](../UITests/ScheduleDeletionUITests.swift).

Semak ketik sekali untuk butiran, ketik dua kali untuk kegemaran, gabungan jenis sesi dalam Jadual Saya, import tidak sah yang tidak boleh disahkan, import sah yang menggantikan demo, pemilihan dan penukaran sembilan bahasa, kebolehbacaan sesi pendek dan jualan barangan serentak, pengesahan pemadaman, pelarasan pilihan dan pelancaran semula dengan jadual kosong. Ujian UI DEBUG menggunakan fail jadual berasingan melalui `OSHICUE_UI_TESTS=1`; `OSHICUE_PRESERVE_UI_DATA=1` mengekalkan data ujian antara pelancaran.

## Semakan manual tambahan

Import berbilang fail dan pastikan fail tidak sah atau ID acara pendua merentas fail tidak digunakan sebahagian. Pilih acara berlainan zon waktu secara berasingan dan bersama, kemudian semak zon pada bar penapis. Semak sesi merentas tengah malam dan sesi tamat tepat pada tengah malam, kemudian besarkan teks sistem dan ulangi semakan butiran. Selepas pemadaman, pastikan petikan keadaan buku rekod kewangan dan diari kekal. Lancarkan semula dan semak jadual serta kegemaran.

Semakan simulator tidak menggantikan pengesahan sebenar pada peranti fizikal, pelbagai versi sistem, teks besar atau pemilih fail. Rekod hasil sebenar ujian UI, pemasangan peranti fizikal dan penerbitan kali ini secara berasingan. [Direktori tangkapan skrin](../screenshots/) sedia ada menyimpan rujukan antara muka terdahulu.

## Uji semula pengurusan, kegemaran dan paparan 0.4.0

Berikut ialah definisi ujian dan skop uji semula yang ditambah untuk 0.4.0 / build 10, bukan pengesahan bahawa semua ujian UI dijalankan dan lulus kali ini. Rekod lulus, gagal atau belum dijalankan berdasarkan pelaksanaan sebenar. Semakan palet juga tidak menggantikan pemeriksaan manual setiap halaman.

| Fail ujian | Kes | Liputan tambahan |
| --- | --- | --- |
| [DataManagementUITests.swift](../UITests/DataManagementUITests.swift) | 6 | Pilihan pembelian ditapis dengan Batal/Sahkan, perubahan penapis mengosongkan pilihan, memadam semua mata wang, pemadaman kelompok diari dan borang sandaran dalam pengurusan Tetapan. Pilihan berbilang data katalog, batal/sahkan arkib dan sejarah pembelian dikekalkan. |
| [DataManagementFailureUITests.swift](../UITests/DataManagementFailureUITests.swift) | 2 | Suntik kegagalan penyimpanan sebenar dengan `OSHICUE_FAIL_BATCH_DELETE=1` pada data ujian Debug berasingan. Semak rekod asal, teks diari dan pilihan semasa dikekalkan, data tidak berubah selepas mula semula, kemudian pilih semula dan cuba pemadaman selepas menyahaktifkan kegagalan. |
| [AppearanceUITests.swift](../UITests/AppearanceUITests.swift) | 5 | Palet dinamik UIKit sebenar, kontras teks dan kawalan dipilih dalam dua paparan, latar tempat, peningkatan kontras dan kebolehbacaan warna ahli; ujian tidak mengendalikan halaman aplikasi. |
| [AppearanceScreenUITests.swift](../UITests/AppearanceScreenUITests.swift) | 1 | Kendalikan empat modul, kegemaran dan pilihan ahli, suntingan, pengurusan serta sandaran dalam paparan cerah atau gelap sistem. Aplikasi tidak memaksa tema; semakan halaman diikuti 13 lampiran tangkapan skrin sebenar. |

```sh
xcodebuild test -project OshicueKit.xcodeproj -scheme OshicueKit -destination 'platform=iOS Simulator,id=<SIMULATOR_ID>' -only-testing:OshicueKitUITests/AppearanceUITests -only-testing:OshicueKitUITests/DataManagementFailureUITests -only-testing:OshicueKitUITests/AppearanceScreenUITests
```

## Langkah uji semula manual tambahan

- Buka kedua-dua halaman pengurusan dari Tetapan dan semak import/eksport asal di Sandaran. Gabungkan penapis, pilih sebahagian hasil dan semak bilangan pengesahan; batal dahulu, kemudian sahkan dan mula semula. Pengesahan pembelian mesti mengasingkan mata wang dan perubahan penapis mesti mengosongkan pilihan.
- Arkibkan ahli berbilang kumpulan dan satu kumpulan secara kelompok; semak skop, keahlian lain dan suntingan sejarah. Padam pembelian dan pastikan katalog serta diari kekal. Padam hasil diari mengikut status, tarikh dan carian; semak teks tidak dipilih dan modul lain.
- Tetapkan kegemaran, buka semula melalui keahlian lain dan semak pilihan dikongsi, bahagian atas pemilih, harga keahlian dan lalai rekod baharu. Semak nyahpilih, arkib, eksport dan import semula. JSON lama tanpa `isFavorite` mesti bukan kegemaran dan `schemaVersion` kekal `1`.
- Tukar paparan sistem cerah/gelap dan semak empat modul, butiran, penyunting, pemilih, pengurusan dan sandaran. Semak teks besar, label panjang sembilan bahasa, avatar, warna ahli, amaun, butang dipilih dan ralat. Rekod peranti dan versi sistem; gambar lama tidak menggantikan hasil semasa.

---

[Kembali ke README](../README.ms.md) · [Format import](IMPORT.ms.md) · [Panduan pengesahan](VERIFICATION.ms.md) · [Paparan dan zum](TIMETABLE-READABILITY.ms.md) · [Padam jadual](SCHEDULE-DELETION.ms.md)

Templat: [Jadual](../Templates/timetable-template.json) · [Demo tarikh tetap](../Templates/demo-timetable.json) · [Semakan kebolehbacaan](../Templates/timetable-readability.json)
