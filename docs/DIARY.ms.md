# Menggunakan dan menyandarkan diari

[简体中文](DIARY.zh-CN.md) | [繁體中文](DIARY.zh-Hant.md) | [繁體粵語](DIARY.zh-HK.md) | [English](DIARY.en.md) | [日本語](DIARY.ja.md) | [한국어](DIARY.ko.md) | [ไทย](DIARY.th.md) | [Bahasa Indonesia](DIARY.id.md) | [Bahasa Melayu](DIARY.ms.md)

Diari digunakan untuk merekod pengalaman persembahan, sesi jualan barangan dan perjalanan ke acara. Naskhah asal disimpan pada peranti dan boleh terus disunting, kemudian disalin atau dieksport sebagai teks untuk diterbitkan. Tiada catatan sedia ada pada penggunaan pertama. Contoh rekaan dalam templat perlu diimport sendiri.

## Membuat dan menyusun catatan

1. Ketik butang tambah pada tab ketiga, “Diari”, lalu isi tajuk dan kandungan berbilang baris. Markdown yang ditaip dikekalkan, tetapi penyunting tidak menyediakan pratonton paparan Markdown.
2. Isi “Ringkasan dan perkara utama”. Kad senarai mengutamakan ringkasan; jika kosong, permulaan kandungan dipaparkan, bersama paling banyak tiga tag.
3. Asingkan tag dengan koma, koma ideografik, koma bertitik atau baris baharu. Pemisah separuh lebar dan lebar penuh disokong. Ruang serta `#`／`＃` di awal dan akhir dibuang. Pendua digabungkan tanpa membezakan huruf besar dan kecil, tetapi perbezaan bunyi bersuara bahasa Jepun dan aksen dikekalkan.
4. Dalam “Butiran acara”, pilih acara sedia ada daripada Catatan untuk mengambil nama, tarikh Gregorian, bandar, tempat dan alamat. Jika tiada tarikh sah, tarikh yang sedang dipilih dikekalkan. Anda juga boleh mengisi secara manual atau menambah acara Catatan daripada penyunting.
5. Pilih “Draf” atau “Sedia”. Status ini hanya untuk penyusunan dan penapisan pada peranti, bukan tanda sudah diterbitkan.

Butiran acara ialah salinan keadaan yang dimiliki oleh catatan diari itu sendiri. Mengubah atau memadam acara Catatan tidak mengubah diari yang sudah disimpan. Memilih acara semula akan mengemas kini salinannya. Beralih semula kepada pengisian manual hanya membuang pautan ID acara tanpa mengosongkan butiran yang sudah diambil. Diari boleh digunakan secara berasingan; tempat dan alamat diisi pengguna atau diambil daripada Catatan.

Senarai dikumpulkan mengikut tarikh acara, daripada yang terkini. Dalam hari yang sama, catatan disusun mengikut masa pengubahsuaian terakhir. Carian meliputi tajuk, kandungan, ringkasan, tag, serta nama, tarikh, bandar, tempat dan alamat acara. Semua perkataan carian yang dipisahkan ruang mesti sepadan, walaupun berada dalam medan berbeza. Penapis tersedia untuk semua catatan, draf dan catatan sedia.

## Simpan automatik dan padam

Perubahan disimpan secara automatik kira-kira 0.5 saat selepas anda berhenti menyunting. Mengetik “Selesai” atau “Tutup”, beralih ke latar belakang atau meninggalkan penyunting mencetuskan cubaan simpan serta-merta. Kegagalan memaparkan ralat dan butang cuba lagi. “Selesai” dan “Tutup” keluar hanya selepas simpan berjaya; kegagalan menulis tidak menggantikan data yang sudah disimpan.

Membuka borang kosong atau mengubah tarikh atau status sahaja tidak mencipta catatan baharu. Penyimpanan bermula apabila tajuk, kandungan, ringkasan, tag, nama acara, bandar, tempat atau alamat mempunyai kandungan sebenar. Catatan yang sudah disimpan boleh dikekalkan walaupun teksnya dikosongkan. Setiap catatan sentiasa memerlukan tarikh acara yang sah.

Catatan tersimpan mempunyai butang padam di bahagian bawah penyunting dan memerlukan pengesahan. Pemadaman hanya membuang diari pada peranti, bukan acara Catatan, rekod pembelian atau salinan yang sudah dieksport.

## Eksport teks dan sandaran penuh

“Sediakan untuk penerbitan” menyimpan perubahan semasa sebelum memaparkan teks. Anda boleh menyalin Markdown atau teks biasa, mengeksport `.md`／`.txt`, atau membuka helaian perkongsian iOS. Teks merangkumi tajuk, butiran acara, tag, ringkasan dan kandungan; baris baharu dalam kandungan dan ringkasan dikekalkan. Eksport Markdown melepaskan aksara khas sebaris pada tajuk dan butiran acara, manakala kandungan dan ringkasan kekal seperti asal. Destinasi perkongsian dan tindakan seterusnya dipilih pengguna. Aplikasi tidak menyambung akaun sosial atau menerbitkan secara automatik.

Buka “Urus Diari” dan pilih “Sandaran” dari kanan atas tab diari atau tab keempat, “Tetapan”. “Eksport semua catatan” menyimpan JSON lengkap melalui pemilih fail sistem. JSON mengekalkan ID, status, tatasusunan tag, salinan butiran acara serta masa penciptaan dan pengubahsuaian, sesuai untuk diimport semula. `.md`／`.txt` digunakan untuk membaca dan berkongsi. Sandaran perlu dieksport dan disimpan sendiri; ini bukan sandaran awan automatik dan suntingan seterusnya tidak mengemas kini fail yang sudah dieksport.

## Format JSON

[diary-template.json](../Templates/diary-template.json) ialah contoh rekaan. Gantikan dengan rekod sebenar dan berikan ID unik baharu bagi catatan baharu. Fail menggunakan UTF-8. Medan peringkat teratas yang wajib ialah `schemaVersion` (integer `1`) dan `entries` (tatasusunan).

| Medan | Jenis | Keperluan |
| --- | --- | --- |
| `id` | Rentetan | Wajib, tidak kosong dan unik; UUID disyorkan. |
| `title` | Rentetan | Wajib; draf boleh menggunakan `""`. |
| `body` | Rentetan | Wajib, kandungan penuh; tulis baris baharu JSON sebagai `\n`; boleh `""`. |
| `summary` | Rentetan | Wajib, ringkasan dan perkara utama; boleh `""`. |
| `tags` | Tatasusunan rentetan | Wajib; gunakan `[]` jika tiada tag. |
| `status` | Rentetan | Wajib; hanya `draft` atau `ready`. |
| `event` | Objek | Wajib; lihat medan dalam jadual berikut. |
| `createdAt` | Rentetan | Wajib, masa penciptaan ISO 8601 dengan zon waktu. |
| `updatedAt` | Rentetan | Wajib, masa pengubahsuaian; tidak boleh mendahului `createdAt`. |

| Medan `event` | Jenis | Keperluan |
| --- | --- | --- |
| `ledgerEventID` | Rentetan atau null | Pilihan; boleh ditinggalkan atau `null`. Rentetan mesti tidak kosong; acara tidak perlu masih wujud dalam Catatan. |
| `name` | Rentetan | Wajib, nama acara; boleh `""`. |
| `date` | Rentetan | Wajib, tarikh Gregorian sah dalam format tepat `yyyy-MM-dd`, contohnya `2026-10-14`. |
| `city` | Rentetan | Wajib, bandar; boleh `""`. |
| `venue` | Rentetan | Wajib, tempat; boleh `""`. |
| `address` | Rentetan | Wajib, alamat penuh; boleh `""`. |

`event.date` ialah tahun, bulan dan hari tanpa zon waktu atau jam. Masa penciptaan dan pengubahsuaian mewakili waktu mutlak, dieksport dengan ketepatan milisaat seperti `2026-01-01T00:00:00.000Z`. Import juga menerima ofset UTC yang jelas seperti `2026-01-01T09:00:00.000+09:00`; jangan tinggalkan `Z` atau ofset. Tarikh acara boleh lebih awal daripada masa penciptaan diari yang ditulis kemudian. Sandaran diari tidak memerlukan sandaran ahli, kumpulan atau Catatan.

## Import dan konflik

Dalam “Urus Diari” → “Sandaran”, pilih satu atau beberapa fail UTF-8 JSON, atau tampal JSON kemudian semak dan pratonton. Pratonton menunjukkan bilangan catatan baharu dan sedia ada; data ditulis hanya selepas pengesahan.

- Catatan dengan ID dan semua kandungan yang sama digabungkan untuk membuang pendua; masa penciptaan dan pengubahsuaian dibandingkan pada ketepatan milisaat.
- Kandungan berbeza pada ID yang sama, termasuk kandungan utama, ringkasan, tag, status, butiran acara atau masa, menghentikan keseluruhan import dan mengekalkan data asal. Sunting catatan lama dalam aplikasi; gunakan ID baharu untuk menyimpan versi lain.
- Import hanya menambah catatan baharu. Catatan lama yang tiada dalam fail tetap dikekalkan. Peraturan konflik yang sama digunakan antara beberapa fail yang dipilih.
- Setiap input dan data gabungan dihadkan kepada 20 MiB (`20 * 1024 * 1024` bait). Suntingan manual tertakluk pada had yang sama.
- ID pendua, medan hilang, jenis salah, status, tarikh, masa atau UTF-8 tidak sah memaparkan ralat. Ralat medan menyertakan lokasi; data tidak ditulis jika pengesahan gagal.

Antara muka import menolak koleksi diari kosong. Penyimpanan dalaman dan eksport membenarkan `entries: []`, yang bermaksud tiada catatan buat masa ini.

## Data setempat dan bahasa

Diari disimpan secara berasingan dan atomik dalam `OshicueKit/diary.json` di bawah Application Support, lalu dibaca ketika aplikasi dimulakan semula. Jika fail asal rosak, salinan pemulihan `diary.recovery-<UUID>.json` disimpan dalam direktori yang sama sebelum penggantian pertama yang berjaya. Mekanisme ini tidak menggantikan sandaran pengguna.

Pilihan bahasa dalam Tetapan digunakan bersama untuk jadual waktu, Catatan, diari dan tetapan. Paparan tarikh mengikut bahasa, tetapi kandungan, nama acara, alamat dan kandungan pengguna kekal asal.

## Pengurusan diari dan paparan sistem

- Dalam 0.4.0 (build 10), buka “Urus Diari” dari kanan atas diari atau Tetapan → Pengurusan data. Halaman mengandungi “Urus rekod” dan “Sandaran”; import JSON, pratonton dan eksport semua catatan kekal di “Sandaran”.
- Gabungkan penapis status, julat tarikh acara dan carian, kemudian pilih berbilang catatan, semua hasil semasa atau kosongkan pilihan. Perubahan penapis mengosongkan pilihan. Pengesahan pemadaman menyatakan bilangan catatan serta pemadaman teks dan ringkasan; Batal mengekalkannya. Pengesahan hanya memadam catatan dipilih. Diari lain, Catatan dan jadual dikekalkan dan hasil pemadaman disimpan setempat.
- Senarai diari, penyunting, persediaan penerbitan, pengurusan, sandaran dan tetapan terus mengikut paparan cerah atau gelap sistem. Teks tersimpan dan petikan acara mengekalkan kandungan asal.

Kembali ke [README](../README.ms.md) · Berkaitan: [Catatan](LEDGER.ms.md) · [Pengesahan diari](DIARY-VERIFICATION.ms.md) · [Pengesahan keseluruhan](VERIFICATION.ms.md) · [Templat diari](../Templates/diary-template.json)
