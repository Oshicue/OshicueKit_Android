# Menggunakan dan mencadangkan catatan harian

[简体中文](DIARY.zh-CN.md) | [繁體中文](DIARY.zh-Hant.md) | [繁體粵語](DIARY.zh-HK.md) | [English](DIARY.en.md) | [日本語](DIARY.ja.md) | [한국어](DIARY.ko.md) | [ไทย](DIARY.th.md) | [Bahasa Indonesia](DIARY.id.md) | [Bahasa Melayu](DIARY.ms.md)

Catatan harian digunakan untuk merekam pengalaman konser, sesi penjualan merchandise, dan perjalanan ke acara. Naskah asli disimpan di perangkat dan bisa terus diedit, lalu disalin atau diekspor sebagai teks untuk dibagikan. Saat pertama digunakan, tidak ada catatan bawaan. Contoh fiktif dalam templat perlu diimpor sendiri.

## Membuat dan merapikan

1. Ketuk tombol tambah pada tab ketiga, “Catatan harian”, lalu isi judul dan isi beberapa baris. Markdown yang diketik tetap dipertahankan, tetapi editor tidak menyediakan pratinjau hasil render Markdown.
2. Isi “Ringkasan dan poin penting”. Kartu daftar mengutamakan ringkasan, atau menampilkan bagian awal isi jika ringkasan kosong, serta maksimal tiga tag.
3. Pisahkan tag dengan koma, koma ideografis, titik koma, atau baris baru. Pemisah lebar setengah dan penuh didukung. Spasi serta `#`／`＃` di awal dan akhir dihapus. Duplikat disatukan tanpa membedakan huruf besar dan kecil, tetapi perbedaan bunyi bersuara dalam bahasa Jepang dan aksen tetap dipertahankan.
4. Pada “Detail acara”, pilih acara yang sudah ada dalam Catatan untuk mengambil nama, tanggal Gregorian, kota, tempat, dan alamat. Jika tanggalnya tidak valid atau tidak ada, tanggal yang sedang dipilih tetap digunakan. Anda juga bisa mengisi manual atau menambahkan acara Catatan dari editor.
5. Pilih “Draf” atau “Siap”. Status ini hanya untuk merapikan dan memfilter di perangkat, bukan tanda sudah dipublikasikan.

Detail acara adalah salinan keadaan yang dimiliki catatan harian tersebut. Mengubah atau menghapus acara dalam Catatan tidak mengubah catatan harian yang sudah disimpan. Memilih acara lagi akan memperbarui salinannya. Beralih kembali ke pengisian manual hanya memutus tautan ID acara tanpa menghapus detail yang sudah diambil. Catatan harian bisa digunakan mandiri; tempat dan alamat diisi pengguna atau diambil dari Catatan.

Daftar dikelompokkan menurut tanggal acara, dari terbaru, lalu diurutkan menurut waktu perubahan terakhir untuk hari yang sama. Pencarian mencakup judul, isi, ringkasan, tag, serta nama, tanggal, kota, tempat, dan alamat acara. Semua kata pencarian yang dipisahkan spasi harus cocok, meski berada di bidang berbeda. Filter tersedia untuk semua catatan, draf, dan catatan siap.

## Simpan otomatis dan hapus

Perubahan disimpan otomatis sekitar 0,5 detik setelah Anda berhenti mengedit. Mengetuk “Selesai” atau “Tutup”, masuk ke latar belakang, atau meninggalkan editor segera memicu upaya penyimpanan. Jika gagal, muncul kesalahan dan tombol coba lagi. “Selesai” dan “Tutup” keluar dari editor hanya setelah penyimpanan berhasil; kegagalan penulisan tidak mengganti data yang sudah disimpan.

Membuka formulir kosong atau hanya mengubah tanggal atau status tidak membuat catatan baru. Penyimpanan dimulai saat judul, isi, ringkasan, tag, nama acara, kota, tempat, atau alamat berisi konten nyata. Catatan yang sudah tersimpan tetap bisa dipertahankan setelah teksnya dikosongkan. Setiap catatan selalu membutuhkan tanggal acara yang valid.

Catatan tersimpan memiliki tombol hapus di bagian bawah editor dan perlu konfirmasi. Penghapusan hanya menghapus catatan harian lokal, bukan acara Catatan, catatan pembelian, atau salinan yang sudah diekspor.

## Ekspor teks dan cadangan lengkap

“Siapkan untuk dibagikan” menyimpan perubahan saat ini sebelum menampilkan teks. Anda bisa menyalin Markdown atau teks biasa, mengekspor `.md`／`.txt`, atau membuka lembar berbagi iOS. Teks berisi judul, detail acara, tag, ringkasan, dan isi. Baris baru pada isi dan ringkasan dipertahankan. Ekspor Markdown memberi escape pada karakter khusus sebaris dalam judul dan detail acara; isi dan ringkasan tetap persis seperti aslinya. Tujuan berbagi dan tindakan berikutnya dipilih pengguna. Aplikasi tidak menghubungkan akun sosial atau memublikasikan otomatis.

Buka “Kelola catatan harian” lalu pilih “Cadangan” dari kanan atas tab catatan harian atau tab keempat, “Pengaturan”. “Ekspor semua catatan” menyimpan JSON lengkap melalui pemilih file sistem. JSON mempertahankan ID, status, array tag, salinan detail acara, serta waktu pembuatan dan perubahan sehingga sesuai untuk diimpor kembali. `.md`／`.txt` digunakan untuk membaca dan berbagi. Cadangan harus diekspor dan disimpan sendiri; ini bukan pencadangan cloud otomatis, dan pengeditan selanjutnya tidak memperbarui file yang telah diekspor.

## Format JSON

[diary-template.json](../Templates/diary-template.json) adalah contoh fiktif. Ganti dengan catatan nyata dan tetapkan ID unik baru untuk catatan baru. File menggunakan UTF-8. Bidang tingkat teratas yang wajib adalah `schemaVersion` (bilangan bulat `1`) dan `entries` (array).

| Bidang | Tipe | Ketentuan |
| --- | --- | --- |
| `id` | String | Wajib, tidak kosong, dan unik; UUID disarankan. |
| `title` | String | Wajib; draf boleh memakai `""`. |
| `body` | String | Wajib, isi lengkap; tulis baris baru JSON sebagai `\n`; boleh `""`. |
| `summary` | String | Wajib, ringkasan dan poin penting; boleh `""`. |
| `tags` | Array string | Wajib; tanpa tag gunakan `[]`. |
| `status` | String | Wajib; hanya `draft` atau `ready`. |
| `event` | Objek | Wajib; lihat bidang pada tabel berikut. |
| `createdAt` | String | Wajib, waktu pembuatan ISO 8601 dengan zona waktu. |
| `updatedAt` | String | Wajib, waktu perubahan; tidak boleh sebelum `createdAt`. |

| Bidang `event` | Tipe | Ketentuan |
| --- | --- | --- |
| `ledgerEventID` | String atau null | Opsional; boleh dihilangkan atau `null`. String tidak boleh kosong; acara tidak harus masih ada dalam Catatan. |
| `name` | String | Wajib, nama acara; boleh `""`. |
| `date` | String | Wajib, tanggal Gregorian valid dengan format tepat `yyyy-MM-dd`, misalnya `2026-10-14`. |
| `city` | String | Wajib, kota; boleh `""`. |
| `venue` | String | Wajib, tempat; boleh `""`. |
| `address` | String | Wajib, alamat lengkap; boleh `""`. |

`event.date` adalah tahun, bulan, dan hari tanpa zona waktu atau jam. Waktu pembuatan dan perubahan menunjukkan waktu absolut, diekspor dengan presisi milidetik, misalnya `2026-01-01T00:00:00.000Z`. Impor juga menerima offset UTC eksplisit seperti `2026-01-01T09:00:00.000+09:00`; jangan menghilangkan `Z` atau offset. Tanggal acara bisa lebih awal dari waktu pembuatan catatan yang ditulis belakangan. Cadangan catatan harian tidak membutuhkan cadangan anggota, grup, atau Catatan.

## Impor dan konflik

Pada “Kelola catatan harian” → “Cadangan”, pilih satu atau beberapa file UTF-8 JSON, atau tempel JSON lalu periksa dan lihat pratinjau. Pratinjau menampilkan jumlah catatan baru dan yang sudah ada; penulisan baru dilakukan setelah konfirmasi.

- Catatan dengan ID dan seluruh isi yang sama akan disatukan; waktu pembuatan dan perubahan dibandingkan pada presisi milidetik.
- Isi berbeda pada ID yang sama, termasuk isi utama, ringkasan, tag, status, detail acara, atau waktu, menghentikan seluruh impor dan mempertahankan data asli. Edit catatan lama di aplikasi; gunakan ID baru untuk menyimpan versi lain.
- Impor hanya menambahkan catatan baru. Catatan lama yang tidak ada dalam file tetap dipertahankan. Aturan konflik yang sama berlaku antarfile yang dipilih.
- Setiap masukan dan data gabungan dibatasi 20 MiB (`20 * 1024 * 1024` byte). Pengeditan manual juga mengikuti batas ini.
- ID duplikat, bidang hilang, tipe salah, status, tanggal, waktu, atau UTF-8 tidak valid menampilkan kesalahan. Kesalahan bidang menyertakan lokasi; data tidak ditulis jika validasi gagal.

Antarmuka impor menolak kumpulan catatan kosong. Penyimpanan internal dan ekspor mengizinkan `entries: []`, yang berarti saat ini tidak ada catatan.

## Data lokal dan bahasa

Catatan harian disimpan terpisah secara atomik di `OshicueKit/diary.json` dalam Application Support dan dibaca saat aplikasi dimulai ulang. Jika file asli rusak, salinan pemulihan `diary.recovery-<UUID>.json` disimpan di direktori yang sama sebelum penggantian pertama yang berhasil. Mekanisme ini tidak menggantikan cadangan pengguna.

Pilihan bahasa di Pengaturan berlaku bersama untuk jadwal, Catatan, catatan harian, dan pengaturan. Tampilan tanggal mengikuti bahasa, sedangkan isi, nama acara, alamat, dan konten pengguna tetap sama.

## Pengelolaan catatan harian dan tampilan sistem

- Pada 0.4.0 (build 10), buka “Kelola catatan harian” dari kanan atas catatan harian atau Pengaturan → Pengelolaan data. Halaman berisi “Kelola catatan” dan “Cadangan”; impor JSON, pratinjau dan ekspor semua entri tetap tersedia di “Cadangan”.
- Gabungkan filter status, rentang tanggal acara dan pencarian, lalu pilih beberapa entri, semua hasil saat ini atau hapus pilihan. Perubahan filter menghapus pilihan. Konfirmasi penghapusan menyebut jumlah entri dan penghapusan isi serta ringkasan; Batal mempertahankannya. Konfirmasi hanya menghapus entri terpilih. Entri lain, Catatan dan jadwal tetap ada, dan hasil penghapusan disimpan lokal.
- Daftar catatan harian, editor, persiapan publikasi, pengelolaan, cadangan dan pengaturan langsung mengikuti tampilan terang atau gelap sistem. Isi tersimpan dan snapshot acara mempertahankan konten aslinya.

Kembali ke [README](../README.id.md) · Terkait: [Catatan](LEDGER.id.md) · [Verifikasi catatan harian](DIARY-VERIFICATION.id.md) · [Verifikasi keseluruhan](VERIFICATION.id.md) · [Templat catatan harian](../Templates/diary-template.json)
