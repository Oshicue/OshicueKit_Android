# Import jadual JSON

[简体中文](IMPORT.zh-CN.md) | [繁體中文](IMPORT.zh-Hant.md) | [繁體粵語](IMPORT.zh-HK.md) | [English](IMPORT.en.md) | [日本語](IMPORT.ja.md) | [한국어](IMPORT.ko.md) | [ไทย](IMPORT.th.md) | [Bahasa Indonesia](IMPORT.id.md) | [Bahasa Melayu](IMPORT.ms.md)

Gunakan JSON berpengekodan UTF-8. Buka halaman import daripada jadual, kemudian tampal seluruh teks atau pilih satu atau beberapa fail `.json` atau teks biasa. Selepas pengesahan berjaya, bilangan acara dan sesi dipaparkan; data hanya digunakan setelah disahkan. Teks pratonton yang diubah perlu disahkan semula.

## Kemas kini dan kegemaran

Pada pelancaran pertama, aplikasi menjana dan menyimpan dua jadual rekaan berasaskan permulaan jam semasa di Tokyo, termasuk sesi hari berikutnya. Dua kumpulan pertama dijadikan kegemaran awal. Tajuk tidak memaparkan label demo. Pelancaran seterusnya membaca data tersimpan. Import sebenar pertama menggantikan jadual yang masih dalam keadaan demo dan mengosongkan kegemaran demo. Import seterusnya mengekalkan acara lain dan menggantikan keseluruhan kandungan acara yang mempunyai `id` sama. Untuk kemas kini, sediakan acara lengkap, bukan sebahagian sesinya sahaja.

`id` kumpulan ialah identiti kegemaran bersama merentas acara. Mengekalkan ID memastikan kegemaran kekal selepas nama ditukar. Ketik dua kali blok LIVE atau jualan barangan untuk menukar status kegemaran kumpulan. Jadual Saya menggabungkan kedua-dua jenis sesi kumpulan kegemaran mengikut acara dan tarikh yang dipilih. Masa bertindih dibenarkan dan dipaparkan sebelah-menyebelah dengan petunjuk pertindihan. Petunjuk hanya membandingkan masa, tanpa menilai perjalanan atau kebolehlaksanaan menghadiri kedua-duanya.

## Medan

| Lokasi | Medan | Peraturan |
| --- | --- | --- |
| Aras utama | `schemaVersion` | Integer wajib, sentiasa `1`. |
| Aras utama | `events` | Tatasusunan acara wajib. Halaman import memerlukan sekurang-kurangnya satu acara dan satu sesi dalam seluruh kelompok. |
| Acara | `id`, `name`, `city` | Rentetan wajib yang tidak kosong. ID acara mesti unik dalam seluruh kelompok. |
| Acara | `timeZone` | Zon waktu IANA yang sah diperlukan, seperti `Asia/Tokyo`. |
| Acara | `venues`, `artists`, `sessions` | Tatasusunan wajib; acara tertentu boleh tiada sesi. |
| Tempat | `id`, `name`, `color` | ID dan nama tidak kosong; warna mesti berbentuk `#RRGGBB`, seperti `#78C9EB`. |
| Kumpulan | `id`, `name` | Rentetan wajib yang tidak kosong. Kekalkan ID kumpulan merentas acara. |
| Sesi | `id`, `artistID`, `venueID` | ID sesi tidak kosong; rujukan mesti menunjuk kepada kumpulan dan tempat yang diisytiharkan dalam acara ini. |
| Sesi | `kind` | Hanya `live` atau `goods`. |
| Sesi | `startsAt`, `endsAt` | Tarikh dan masa ISO 8601 lengkap dengan saat serta `Z` atau ofset nyata; tamat mesti selepas mula. |
| Sesi | `area`, `notes` | Rentetan pilihan; boleh ditinggalkan atau bernilai `null`. |

ID tempat, kumpulan dan sesi masing-masing mesti unik dalam acaranya. Rentetan wajib tidak boleh mengandungi ruang kosong sahaja. JSON tidak menyokong komen atau koma selepas item terakhir. Medan tidak dikenali diterima tetapi tidak dipaparkan oleh aplikasi atau dikekalkan apabila dikodkan semula.

## Pengesahan dan import kelompok

Ralat memaparkan laluan medan, seperti `$.events[0].sessions[2].endsAt`. Versi, jenis data, tarikh, zon waktu, warna, ID pendua, rujukan atau tempoh yang tidak sah menghentikan pengesahan seluruh input. Berbilang fail digabungkan sebelum disahkan bersama; ID acara pendua merentas fail juga menyebabkan kegagalan. Kegagalan mana-mana fail menghalang import sebahagian. Halaman import tidak boleh menggunakan tatasusunan acara kosong atau kelompok tanpa sesi. Format teras membenarkan `events: []` untuk menyimpan keadaan kosong dalaman selepas pemadaman.

## Zon waktu dan merentas tengah malam

Nilai yang diterima termasuk `2026-10-14T12:00:00+09:00`, `2026-10-14T03:00:00Z` dan pecahan saat. `12:00`, nilai tanpa saat atau nilai tanpa zon waktu tidak sah. Ofset menentukan detik mutlak, manakala `timeZone` acara menentukan tarikh tempatan. Isikan zon waktu sebenar bagi setiap sesi. Jam berulang semasa waktu musim panas, `2026-11-01T01:30:00-04:00` dan `2026-11-01T01:30:00-05:00`, mewakili dua detik berbeza.

Bagi sesi merentas tengah malam, gunakan tarikh hari berikutnya pada waktu tamat: `2026-10-14T23:40:00+09:00` → `2026-10-15T00:20:00+09:00`. Sesi muncul pada kedua-dua tarikh yang diliputi, dengan blok dipotong mengikut hari. Tamat tepat pada `00:00:00` tidak menambah tarikh kosong. Tiada had tempoh maksimum buatan.

Apabila acara berlainan zon waktu dipilih bersama, antara muka semasa menggunakan zon waktu acara terpilih yang pertama mengikut susunan dokumen untuk tarikh, masa dan butiran. Bar penapis menunjukkan zon tersebut. Pilih satu acara sahaja untuk melihat waktu tempatannya sendiri. Garisan masa semasa menggunakan waktu sistem sebenar; tarikh demo tetap mungkin bukan hari ini.

## Menyunting templat

Salin templat jadual melalui pautan di hujung halaman. Kekalkan `schemaVersion` dan `events`, isikan ID yang konsisten, kemudian salin sesi dan ubah rujukan, jenis serta masanya. Semua acara, kumpulan dan jadual dalam templat ialah rekaan. Templat tidak diimport secara automatik.

---

[Kembali ke README](../README.ms.md) · [Format import](IMPORT.ms.md) · [Panduan pengesahan](VERIFICATION.ms.md) · [Paparan dan zum](TIMETABLE-READABILITY.ms.md) · [Padam jadual](SCHEDULE-DELETION.ms.md)

Templat: [Jadual](../Templates/timetable-template.json) · [Demo tarikh tetap](../Templates/demo-timetable.json) · [Semakan kebolehbacaan](../Templates/timetable-readability.json)
