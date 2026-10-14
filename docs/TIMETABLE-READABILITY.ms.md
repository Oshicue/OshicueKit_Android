# Paparan jadual dan zum

[简体中文](TIMETABLE-READABILITY.md) | [繁體中文](TIMETABLE-READABILITY.zh-Hant.md) | [繁體粵語](TIMETABLE-READABILITY.zh-HK.md) | [English](TIMETABLE-READABILITY.en.md) | [日本語](TIMETABLE-READABILITY.ja.md) | [한국어](TIMETABLE-READABILITY.ko.md) | [ไทย](TIMETABLE-READABILITY.th.md) | [Bahasa Indonesia](TIMETABLE-READABILITY.id.md) | [Bahasa Melayu](TIMETABLE-READABILITY.ms.md)

## Garis masa dan nama

Semua Jadual membahagikan lajur mengikut acara, tempat dan jenis LIVE／jualan barangan. Jadual Saya menggabungkan kedua-dua jenis sesi kumpulan kegemaran. Sesi bertindih dipaparkan sebelah-menyebelah secara automatik dengan bilangan lorong minimum bagi setiap kelompok pertindihan. Sesi berasingan yang berikutnya kembali menggunakan lebar lajur penuh. Tatal mendatar sementara skala masa di sebelah kiri kekal di tempatnya.

Pada saiz teks standard, setiap lorong selari selebar sekurang-kurangnya 180 pt; nama panjang atau sesi pendek boleh melebarkannya sehingga 320 pt. Jumlah lebar lajur tempat bertambah mengikut bilangan sesi serentak. Tajuk lorong mengulang nama tempat supaya mudah dikenali semasa menatal. Lebar dan saiz fon nama mengikut saiz teks sistem.

Nama artis diberi keutamaan dan boleh dibalut ke baris seterusnya. Saiz lalai ialah 12 pt dalam Semua Jadual dan 15 pt dalam Jadual Saya, mengecil kepada standard 11 pt jika perlu. Jika ruang tidak mencukupi, maklumat sekunder seperti masa, tempat atau kawasan dalam blok disembunyikan dahulu. Kedudukan dan tinggi blok mengikut masa sebenar dengan sela lukisan; sesi tidak dipanjangkan untuk memuatkan teks. Sesi terlalu pendek, nama terlalu panjang atau teks besar masih boleh terpotong. Ketik sekali untuk membaca nama asal lengkap, masa, tempat, kawasan dan nota dalam butiran. VoiceOver juga boleh membaca nama.

Ketik dua kali blok untuk menambah／mengeluarkan kumpulan daripada kegemaran. Butiran turut menyediakan tindakan kegemaran. Ketik sekali untuk melihat tidak mengubah kegemaran.

## Zum dan masa semasa

| Skala | Tinggi setiap minit |
| --- | --- |
| 50% | 2 pt |
| 70% | 2.8 pt |
| 100%（lalai） | 4 pt |
| 150% | 6 pt |

Zum hanya mengubah skala paparan garis masa. Garisan masa semasa menggunakan waktu sistem sebenar. Butang lokasi beralih ke hari ini jika acara semasa mempunyai sesi hari ini, dan menatal ke sekarang apabila waktu semasa berada dalam julat masa yang dipaparkan.

## Semakan semula kebolehbacaan

Import templat semakan melalui pautan di hujung halaman, kemudian pilih `2026-06-14` dan `Asia/Tokyo`. Templat mengandungi nama Jepun／Inggeris panjang, LIVE 5 minit dan 10 minit serta empat sesi barangan serentak di satu tempat. Semua jadual ialah rekaan.

1. Pada lalai 100%, semak tempoh dan nama sesi pendek. Tatal mendatar untuk membaca kesemua empat sesi barangan dan pastikan tidak saling menutupi.
2. Ketik sekali untuk butiran lengkap. Selepas mengetik dua kali untuk kegemaran, semak Jadual Saya supaya LIVE dan barangan muncul bersama, kemudian lihat petunjuk pertindihan.
3. Pilih 50%, 70%, 100% dan 150% secara bergilir. Pastikan kedudukan mengikut skala masa dan butiran masih boleh dibuka ketika dizum keluar.
4. Besarkan saiz teks sistem, kemudian ulangi semakan tatal, butiran dan kegemaran.

Lihat panduan pengesahan untuk kes susun atur teras dan UI. Ujian UI tidak dijalankan semula dalam kemas kini ini; langkah teks besar masih merupakan semakan yang perlu dilakukan.

---

[Kembali ke README](../README.ms.md) · [Format import](IMPORT.ms.md) · [Panduan pengesahan](VERIFICATION.ms.md) · [Paparan dan zum](TIMETABLE-READABILITY.ms.md) · [Padam jadual](SCHEDULE-DELETION.ms.md)

Templat: [Jadual](../Templates/timetable-template.json) · [Demo tarikh tetap](../Templates/demo-timetable.json) · [Semakan kebolehbacaan](../Templates/timetable-readability.json)
