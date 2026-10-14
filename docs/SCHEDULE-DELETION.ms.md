# Padam jadual

[简体中文](SCHEDULE-DELETION.md) | [繁體中文](SCHEDULE-DELETION.zh-Hant.md) | [繁體粵語](SCHEDULE-DELETION.zh-HK.md) | [English](SCHEDULE-DELETION.en.md) | [日本語](SCHEDULE-DELETION.ja.md) | [한국어](SCHEDULE-DELETION.ko.md) | [ไทย](SCHEDULE-DELETION.th.md) | [Bahasa Indonesia](SCHEDULE-DELETION.id.md) | [Bahasa Melayu](SCHEDULE-DELETION.ms.md)

## Cara menggunakan

Buka pemilihan acara daripada skrin jadual. Setiap jadual mempunyai butang padam merah serta ikon tong sampah. Apabila diketik, pengesahan memaparkan nama sasaran. Batal mengekalkan data; pengesahan memadamkan acara dan semua sesi LIVE serta jualan barangan. Memilih dan memadamkan acara ialah tindakan berasingan.

Pemadaman menulis fail tempatan sebelum mengemas kini antara muka. Jika penyimpanan gagal, ralat dipaparkan dan jadual asal dikekalkan supaya boleh dicuba lagi. Memadamkan acara yang tidak dipilih mengekalkan pilihan lain. Memadamkan satu-satunya acara terpilih memilih acara yang masih ada secara automatik dan melaraskan tarikh.

## Kegemaran dan data berasingan

Hanya kegemaran kumpulan yang masih diisytiharkan dalam tatasusunan `artists` acara lain dikekalkan. Ini tidak bergantung pada nama paparan atau kewujudan sesi yang tinggal. Kumpulan bernama sama dengan ID berbeza tidak berkongsi kegemaran.

Buku rekod kewangan dan diari menyimpan data serta petikan keadaan acara secara berasingan. Memadamkan jadual tidak memadamkan pembelian, statistik atau draf diari. Memadamkan semua acara menyimpan jadual kosong yang kekal kosong selepas pelancaran semula. Import lagi atau muatkan demo secara sengaja daripada tetapan. Pemuatan demo menggabungkan berdasarkan ID acara demo dan mengekalkan acara yang mempunyai ID lain.

## Ulangi semakan

1. Import dua acara yang berkongsi satu ID kumpulan dan masing-masing mempunyai kumpulan tersendiri. Jadikan kumpulan bersama dan kumpulan tersendiri dalam acara yang akan dipadamkan sebagai kegemaran.
2. Semak nama dalam pengesahan pemadaman, batalkan dahulu dan pastikan acara, sesi serta pilihan kekal.
3. Sahkan pemadaman. Semak bahawa sesi sasaran hilang, kegemaran bersama kekal dan kegemaran yang hanya diisytiharkan dalam acara dipadamkan dikeluarkan. Semak pilihan serta tarikh yang tinggal.
4. Padamkan acara yang tidak dipilih dan semak pilihan lain kekal. Pastikan data buku rekod kewangan dan diari tidak terjejas.
5. Padamkan semua acara dan lancarkan semula untuk memastikan demo tidak dipulihkan. Import lagi dan tambah kegemaran bagi memastikan penggunaan biasa.

Semakan teras terdapat dalam [ScheduleDeletionTests.swift](../Tests/ScheduleDeletionTests.swift); semakan operasi natif terdapat dalam [ScheduleDeletionUITests.swift](../UITests/ScheduleDeletionUITests.swift). Ujian UI tidak dijalankan semula dalam kemas kini ini.

---

[Kembali ke README](../README.ms.md) · [Format import](IMPORT.ms.md) · [Panduan pengesahan](VERIFICATION.ms.md) · [Paparan dan zum](TIMETABLE-READABILITY.ms.md) · [Padam jadual](SCHEDULE-DELETION.ms.md)

Templat: [Jadual](../Templates/timetable-template.json) · [Demo tarikh tetap](../Templates/demo-timetable.json) · [Semakan kebolehbacaan](../Templates/timetable-readability.json)
