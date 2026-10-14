# Panduan Catatan dan format data

[简体中文](LEDGER.zh-CN.md) | [繁體中文](LEDGER.zh-Hant.md) | [繁體粵語](LEDGER.zh-HK.md) | [English](LEDGER.en.md) | [日本語](LEDGER.ja.md) | [한국어](LEDGER.ko.md) | [ไทย](LEDGER.th.md) | [Bahasa Indonesia](LEDGER.id.md) | [Bahasa Melayu](LEDGER.ms.md)

Catatan merekam pembelian merchandise pada kalender, mengelola grup, anggota, keanggotaan, warna dan harga bawaan, serta menampilkan peringkat jumlah dan pengeluaran. Data disimpan di perangkat. Cadangan Catatan terpisah dari cadangan Jadwal dan Diari.

## Memulai

1. Tambahkan grup dan anggota di Anggota & jenis. Jika seorang anggota bergabung dalam beberapa grup, gunakan Tautkan anggota yang ada untuk menambahkan keanggotaan dengan ID anggota yang sama. Nama yang sama dengan ID berbeda tidak digabung otomatis.
2. Atur warna dan harga satuan bawaan untuk setiap keanggotaan. Jenis bawaan adalah Cheki, Cheki wide dan Foto ponsel. Anda dapat menambah jenis dan menetapkan harga dalam beberapa mata uang untuk jenis yang sama. Nama dan avatar anggota digunakan bersama di semua grup.
3. Ketuk ＋, lalu pilih tanggal, grup anggota, jenis, jumlah, mata uang dan harga satuan sebenarnya. Mengubah keanggotaan, jenis atau mata uang mengisi ulang harga bawaan yang sesuai. Jika tidak ada harga bawaan, masukkan harga sendiri. Jumlah harus bilangan bulat positif; harga nol diperbolehkan.
4. Acara, kota, tempat, alamat dan catatan bersifat opsional. Buat acara Catatan atau pilih acara dan tempat dari Jadwal untuk disalin menjadi acara Catatan. Perubahan Jadwal berikutnya tidak disinkronkan otomatis.
5. Angka kalender menunjukkan jumlah unit pada hari itu, bukan banyaknya rekaman. Kartu bulanan menampilkan jumlah dan pengeluaran per mata uang. Pilih tanggal untuk melihat ringkasan anggota per grup dan rekaman satu per satu.

Jumlah adalah unit untuk jenis yang dipilih: foto dihitung per lembar, sedangkan jenis tiket yang ditambahkan dapat dihitung per tiket.

## Mengedit, menghapus dan snapshot riwayat

Ketuk kartu anggota di bawah tanggal. Satu pembelian langsung membuka editor; beberapa pembelian membuka pemilih rekaman terlebih dahulu. Anda juga dapat membuka daftar rekaman untuk mengeditnya. Formulir memuat tanggal, keanggotaan, jenis, jumlah, harga dan informasi opsional asli. Penyimpanan hanya memperbarui rekaman yang dipilih. Hapus pembelian ini memerlukan konfirmasi; penghapusan mengurangi jumlah dan nilainya.

Di Anggota & jenis, ketuk anggota untuk mengedit data, atau pensil di sebelah judul grup untuk mengedit grup. Keduanya meminta konfirmasi penghapusan:

- Hapus anggota: mengarsipkan anggota dan keanggotaannya dalam semua grup.
- Hapus grup: mengarsipkan grup beserta keanggotaannya. Anggota yang juga berada dalam grup lain tetap tersedia di sana.

Data arsip tetap ada dalam cadangan. Pembelian, nama, warna, harga dan statistik lama dipertahankan. Rekaman baru dan perubahan keanggotaan harus memakai keanggotaan aktif. Rekaman riwayat masih dapat diedit dengan keanggotaan asli yang dihapus, yang ditandai Dihapus pada pemilih. Editor katalog tidak menyediakan pemulihan; mengimpor cadangan dengan entri katalog aktif ber-ID sama dapat memulihkannya, tetapi harus lolos validasi penggabungan menyeluruh.

Mengubah nama, warna atau harga bawaan katalog tidak menulis ulang pembelian tersimpan. Saat mengedit pembelian, mempertahankan keanggotaan menjaga snapshot nama anggota, grup dan warna; mempertahankan jenis menjaga snapshot nama jenis. Jika keanggotaan atau jenis berubah, snapshot terkait diambil ulang. Harga sebenarnya dan informasi acara disimpan sesuai formulir. Seluruh catatan divalidasi sebelum disimpan secara atomik ke perangkat. Kegagalan tidak menerapkan sebagian perubahan.

## Statistik

Statistik mendukung hari, bulan, tahun dan sepanjang waktu, filter jenis dan grup, serta peringkat anggota gabungan, anggota per grup atau grup. Jumlah anggota dihitung dari nilai `memberID` berbeda setelah filter. Grup yang diarsipkan tetap dapat dipilih untuk memfilter riwayat.

Peringkat jumlah dapat menjumlahkan unit dari semua mata uang, tetapi peringkat pengeluaran memerlukan satu mata uang. Nilai lintas mata uang tidak dijumlahkan atau dikonversi otomatis. Ringkasan menampilkan jumlah per jenis dan nilai per mata uang. Peringkat seri mengikuti bentuk `1、1、3`, dengan urutan stabil berdasarkan ID jika nilainya sama. Pangsa memakai total jumlah atau pengeluaran satu mata uang dalam cakupan filter sebagai penyebut; total nol menghasilkan pangsa nol. Nama dan warna peringkat memakai snapshot rekaman terbaru dalam cakupan filter; rekaman pada tanggal yang sama diurutkan menurut ID.

## Catatan contoh

Jika belum ada file tersimpan saat pertama dibuka, contoh fiktif dimuat dan langsung disimpan: 2 grup, 3 anggota, 4 keanggotaan, 3 jenis, 1 acara dan 5 rekaman, dengan total 16 unit. Tanggal mengikuti hari kalender `Asia/Shanghai` saat pembuatan dan dua hari sebelumnya; hari pembuatan berjumlah 10 unit. Tanggal tidak dikunci ke tanggal templat. Tidak ada lencana contoh.

Catatan yang sudah ada membaca data tersimpan. Membuka ulang tidak memulihkan contoh yang telah diedit atau dihapus. Muat contoh catatan hanya diizinkan untuk catatan kosong atau contoh yang belum diubah. Kosong berarti tanpa grup, anggota, keanggotaan, acara dan rekaman, serta jenis kosong atau tepat sama dengan tiga jenis bawaan. Mengarsipkan data saja tidak membuat catatan kosong. Impor pertama dapat mengganti contoh yang belum diubah; setelah mengedit dan menyimpan contoh, impor berikutnya menggunakan penggabungan.

## Field JSON

[ledger-template.json](../Templates/ledger-template.json) adalah contoh impor terpisah dari templat Jadwal. Gunakan UTF-8 dan `schemaVersion` bernilai `1`. Keenam array berikut wajib ada; gunakan `[]` jika kosong. `id` harus unik dalam setiap array. ID dan nama tidak boleh hanya berisi spasi.

| Array | Field wajib | Field opsional |
| --- | --- | --- |
| `groups` | `id`, `name` | `isArchived` |
| `members` | `id`, `name` | `avatarJPEGBase64`, `isArchived`、`isFavorite` |
| `memberships` | `id`, `memberID`, `groupID`, `color`, `prices` | `isArchived` |
| `products` | `id`, `name` | Tidak ada |
| `events` | `id`, `name` | `date`, `city`, `venue`, `address` |
| `records` | Lihat tabel berikut | Lihat tabel berikut |

`memberships` merujuk anggota dan grup dalam file yang sama. Setiap pasangan anggota/grup hanya boleh memiliki satu keanggotaan. `color` adalah RGB enam digit, misalnya `#F28AAF`. `prices` boleh `[]`. Setiap harga memerlukan `productID`, `currency` dan `unitPriceMinor`, merujuk jenis yang ada, dan memiliki kombinasi jenis/mata uang unik dalam keanggotaan tersebut.

`isArchived` adalah boolean opsional; hanya `true` berarti diarsipkan. Cadangan lama tanpa field ini tetap dapat diimpor dengan versi `1`. Keanggotaan aktif juga memerlukan anggota dan grup yang tidak diarsipkan. Data arsip tetap harus valid. Jika merapikan cadangan secara manual, pertahankan anggota, grup dan keanggotaan yang dirujuk oleh pembelian riwayat.

| Field `records` | Persyaratan dan arti |
| --- | --- |
| `id` | Wajib, ID pembelian unik; pembelian baru memakai ID baru, UUID disarankan |
| `date` | Wajib, tanggal Gregorian valid dalam format ketat `yyyy-MM-dd`, misalnya `2026-10-14` |
| `memberID`, `groupID`, `productID` | Wajib, merujuk anggota, grup dan jenis dalam file yang sama, dengan keanggotaan sesuai |
| `quantity` | Wajib, jumlah berupa bilangan bulat positif |
| `unitPriceMinor` | Wajib, harga satuan bilangan bulat nonnegatif dalam unit terkecil mata uang |
| `currency` | Wajib, kode mata uang huruf besar dari tabel di bawah |
| `memberNameSnapshot`, `groupNameSnapshot`, `productNameSnapshot` | Wajib, nama saat pembelian; tidak boleh hanya spasi |
| `colorSnapshot` | Wajib, warna RGB enam digit saat pembelian |
| `eventID` | Opsional, merujuk ID dalam `events` di file yang sama |
| `eventNameSnapshot`, `citySnapshot`, `venueSnapshot`, `addressSnapshot`, `note` | Opsional, acara, kota, tempat, alamat dan catatan saat pembelian |

Tanggal pembelian disimpan sebagai hari kalender, sehingga perubahan zona waktu berikutnya tidak memindahkan rekaman. Tanggal acara boleh berbeda dari tanggal pembelian. Avatar berisi data JPEG dengan encoding Base64, bukan lokasi file.

## Presisi nilai dan batas

Total adalah `quantity × unitPriceMinor`, dengan perhitungan bilangan bulat. Masukkan harga biasa di layar untuk dikonversi ke unit terkecil. Kelebihan digit desimal ditolak, bukan dibulatkan diam-diam. Gunakan angka dan titik desimal tanpa simbol mata uang atau pemisah ribuan. Nilai JSON harus bilangan bulat.

| Mata uang | Digit desimal | Contoh harga di layar | `unitPriceMinor` |
| --- | --- | --- | --- |
| `JPY`, `KRW` | 0 | `2000` | `2000` |
| `CNY` | 2 | `80.50` | `8050` |
| `TWD`, `HKD`, `USD`, `THB`, `IDR`, `MYR` | 2 | `100.00` | `10000` |

Ini adalah presisi implementasi aplikasi saat ini; `IDR` dan `TWD` juga menyimpan dua digit unit terkecil. Overflow diperiksa pada perkalian per pembelian, total jumlah dan total tiap mata uang. Teks JSON dan catatan setelah encoding dibatasi `20 × 1024 × 1024` byte (20 MiB). Setiap avatar impor setelah decoding dibatasi `2 × 1024 × 1024` byte (2 MiB), dengan pemeriksaan struktur JPEG. Foto asli dari galeri dibatasi 10 MiB, diperkecil hingga sisi terpanjang maksimal 512 piksel dan dikonversi menjadi JPEG sebelum disimpan.

## Impor, penggabungan dan cadangan

Buka “Kelola Catatan” di bagian atas Catatan lalu pilih “Cadangan”. Pilih satu atau beberapa file JSON/teks, atau tempel JSON dan validasi pratinjau, lalu konfirmasi impor. Beberapa file digabung sebelum pratinjau. File kosong, dokumen tanpa data, field tidak valid dan konflik menghalangi impor. Tiap file harus menyertakan semua data yang dirujuk; anggota atau grup yang hilang tidak diambil dari catatan saat ini. Mengedit teks tempelan membatalkan pratinjau sebelumnya.

Impor valid mengganti contoh yang belum diubah. Kasus lainnya memakai aturan berikut:

- Grup, anggota, keanggotaan, jenis dan acara diperbarui atau ditambahkan berdasarkan `id`. Data lama yang tidak tercantum dalam file baru tetap ada. Impor dapat memperbarui penanda arsip.
- Pembelian dideduplikasi berdasarkan `id`. Impor ulang rekaman identik tidak menghitung dua kali. ID sama dengan isi berbeda menghasilkan kesalahan dan membatalkan seluruh impor. Edit pembelian di aplikasi dan gunakan ID baru untuk pembelian baru.
- Dokumen lengkap divalidasi sebelum dan sesudah penggabungan, termasuk keunikan keanggotaan, referensi, tanggal, snapshot, nilai dan ukuran. Kegagalan mempertahankan catatan asli.

Ekspor berisi seluruh catatan, data arsip, snapshot riwayat, harga dan avatar. Pemulihan adalah penggabungan dan tidak membatalkan edit pembelian yang sudah ada. Rekaman yang telah diedit akan konflik dengan rekaman cadangan lama ber-ID sama tetapi isi berbeda. Simpan file ekspor untuk migrasi atau arsip pribadi.

## Pengelolaan, anggota favorit dan tampilan sistem

- Pada 0.4.0 (build 10), tombol atas Catatan dan Pengaturan → Pengelolaan data membuka “Kelola Catatan”. Halaman ini berisi “Kelola catatan”, “Bersihkan data anggota dan grup” dan “Cadangan”. Impor JSON, pratinjau dan ekspor lengkap tetap tersedia di “Cadangan”. Pengaturan juga menyediakan “Kelola catatan harian”.
- Gabungkan filter pembelian berdasarkan rentang tanggal, anggota, grup, jenis, mata uang, acara dan pencarian, lalu pilih beberapa rekaman satu per satu, pilih semua hasil saat ini atau hapus pilihan. Perubahan filter menghapus pilihan; hasil kosong tidak dapat dihapus. Konfirmasi menampilkan jumlah rekaman terpilih, jumlah item dan total per mata uang. Batal mempertahankan data; konfirmasi jelas menghapus hanya pilihan dan memperbarui statistik.
- “Hapus semua rekaman pembelian” berlaku untuk seluruh Catatan tanpa mengikuti filter saat ini dan meminta konfirmasi jumlah rekaman, item dan total per mata uang. Grup, anggota, jenis, katalog acara dan catatan harian tetap tersedia untuk rekaman baru.
- “Bersihkan data anggota dan grup” mendukung pencarian dan pilihan beberapa anggota atau grup. Menghapus anggota mengarsipkan semua keanggotaannya; menghapus grup hanya mengarsipkan grup tersebut dan keanggotaannya, sementara keanggotaan grup lain tetap tersedia. Riwayat pembelian, snapshot dan statistik tetap ada dan riwayat dapat diedit. Batal tidak mengarsipkan; cadangan valid dapat memulihkan data katalog.
- Atur atau hapus “Anggota favorit” di editor anggota dalam “Anggota dan jenis”. Preferensi terikat pada ID anggota dan dibagikan ke semua keanggotaannya. Pemilih anggota pembelian menampilkan favorit di atas; setiap keanggotaan tetap memiliki warna dan harga sendiri, dengan daftar grup lengkap di bawah. Rekaman baru mendahulukan keanggotaan favorit yang aktif. Menghubungkan anggota lama juga mendahulukan favorit; anggota atau keanggotaan yang diarsipkan tidak masuk pintasan favorit.
- `isFavorite` anggota adalah Bool opsional: hanya `true` berarti favorit; `false`, tidak dicantumkan atau `null` berarti bukan favorit. JSON lama tetap dapat diimpor dan `schemaVersion` tetap `1`. Cadangan lengkap menyimpan preferensi ini; penggabungan katalog berdasarkan ID anggota memperbaruinya.
- Aplikasi mengikuti tampilan terang atau gelap sistem pada jadwal, Catatan, catatan harian, pengaturan serta semua layar edit, pilihan, pengelolaan dan cadangan. Perubahan sistem langsung diterapkan tanpa pengaturan tema terpisah. Warna anggota dan snapshot riwayat tetap disimpan.

Kembali ke [README](../README.id.md) · [Panduan verifikasi Catatan](LEDGER-VERIFICATION.id.md) · [Templat JSON](../Templates/ledger-template.json)
