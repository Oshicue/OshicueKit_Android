# Hapus jadwal

[简体中文](SCHEDULE-DELETION.md) | [繁體中文](SCHEDULE-DELETION.zh-Hant.md) | [繁體粵語](SCHEDULE-DELETION.zh-HK.md) | [English](SCHEDULE-DELETION.en.md) | [日本語](SCHEDULE-DELETION.ja.md) | [한국어](SCHEDULE-DELETION.ko.md) | [ไทย](SCHEDULE-DELETION.th.md) | [Bahasa Indonesia](SCHEDULE-DELETION.id.md) | [Bahasa Melayu](SCHEDULE-DELETION.ms.md)

## Cara menggunakan

Buka pilihan acara dari layar jadwal. Setiap jadwal memiliki tombol hapus merah dan ikon tempat sampah. Ketukan menampilkan konfirmasi dengan nama sasaran. Batal mempertahankan data; konfirmasi menghapus acara dan seluruh sesi LIVE serta penjualan merchandise. Memilih dan menghapus acara adalah tindakan terpisah.

Penghapusan menulis berkas lokal terlebih dahulu sebelum memperbarui antarmuka. Jika penyimpanan gagal, pesan kesalahan muncul dan jadwal lama tetap ada sehingga dapat dicoba lagi. Menghapus acara yang tidak dipilih mempertahankan pilihan lain. Menghapus satu-satunya acara terpilih otomatis memilih acara yang tersisa dan menyesuaikan tanggal.

## Favorit dan data terpisah

Hanya favorit grup yang masih dideklarasikan pada larik `artists` acara lain yang dipertahankan. Penilaian tidak bergantung pada nama tampilan atau apakah masih ada sesi. Grup bernama sama dengan ID berbeda tidak berbagi favorit.

Buku catatan keuangan dan jurnal menyimpan data serta snapshot acara secara terpisah. Menghapus jadwal tidak menghapus pembelian, statistik, atau draf jurnal. Menghapus semua acara menyimpan jadwal kosong yang tetap kosong setelah peluncuran ulang. Impor lagi atau muat demo secara sengaja dari pengaturan. Pemuatan demo menggabungkan berdasarkan ID acara demo dan mempertahankan acara dengan ID lain.

## Ulangi pemeriksaan

1. Impor dua acara yang berbagi satu ID grup dan masing-masing memiliki grup tersendiri. Favoritkan grup bersama dan grup tersendiri pada acara yang akan dihapus.
2. Periksa nama pada konfirmasi penghapusan, batalkan dulu, lalu pastikan acara, sesi, dan pilihan tetap ada.
3. Konfirmasikan penghapusan. Periksa bahwa sesi sasaran hilang, favorit bersama tetap ada, dan favorit yang hanya dideklarasikan dalam acara terhapus dihapus. Periksa pilihan serta tanggal yang tersisa.
4. Hapus acara yang tidak dipilih dan periksa pilihan lain tetap ada. Pastikan data buku catatan keuangan dan jurnal tidak terpengaruh.
5. Hapus semua acara dan luncurkan ulang untuk memastikan demo tidak kembali. Impor lagi dan favoritkan grup untuk memastikan penggunaan normal.

Pemeriksaan inti ada di [ScheduleDeletionTests.swift](../Tests/ScheduleDeletionTests.swift); pemeriksaan operasi native ada di [ScheduleDeletionUITests.swift](../UITests/ScheduleDeletionUITests.swift). Pengujian UI tidak dijalankan ulang dalam pembaruan ini.

---

[Kembali ke README](../README.id.md) · [Format impor](IMPORT.id.md) · [Panduan verifikasi](VERIFICATION.id.md) · [Tampilan dan zoom](TIMETABLE-READABILITY.id.md) · [Hapus jadwal](SCHEDULE-DELETION.id.md)

Templat: [Jadwal](../Templates/timetable-template.json) · [Demo tanggal tetap](../Templates/demo-timetable.json) · [Pemeriksaan keterbacaan](../Templates/timetable-readability.json)
