# Panduan Catatan dan format data

[简体中文](LEDGER.zh-CN.md) | [繁體中文](LEDGER.zh-Hant.md) | [繁體粵語](LEDGER.zh-HK.md) | [English](LEDGER.en.md) | [日本語](LEDGER.ja.md) | [한국어](LEDGER.ko.md) | [ไทย](LEDGER.th.md) | [Bahasa Indonesia](LEDGER.id.md) | [Bahasa Melayu](LEDGER.ms.md)

Catatan merekod pembelian barangan pada kalendar, mengurus kumpulan, ahli, keahlian, warna dan harga lalai, serta memaparkan kedudukan kuantiti dan perbelanjaan. Data disimpan pada peranti. Sandaran Catatan berasingan daripada sandaran Jadual dan Diari.

## Bermula

1. Tambahkan kumpulan dan ahli dalam Ahli & jenis. Jika seorang ahli menyertai beberapa kumpulan, gunakan Pautkan ahli sedia ada untuk menambah keahlian dengan ID ahli yang sama. Nama sama dengan ID berbeza tidak digabungkan secara automatik.
2. Tetapkan warna dan harga seunit lalai bagi setiap keahlian. Jenis lalai ialah Cheki, Cheki lebar dan Foto telefon. Anda boleh menambah jenis serta harga dalam beberapa mata wang bagi jenis yang sama. Nama dan avatar ahli dikongsi antara kumpulan.
3. Ketik ＋, kemudian pilih tarikh, kumpulan ahli, jenis, kuantiti, mata wang dan harga seunit sebenar. Menukar keahlian, jenis atau mata wang akan mengisi semula harga lalai yang sepadan. Jika tiada harga lalai, masukkan harga sendiri. Kuantiti mesti integer positif; harga sifar dibenarkan.
4. Acara, bandar, tempat, alamat dan nota adalah pilihan. Cipta acara Catatan atau pilih acara serta tempat daripada Jadual untuk disalin menjadi acara Catatan. Perubahan Jadual selepas itu tidak disegerakkan secara automatik.
5. Nombor kalendar ialah jumlah unit pada hari tersebut, bukan bilangan rekod. Kad bulanan menunjukkan kuantiti dan perbelanjaan mengikut mata wang. Pilih tarikh untuk melihat ringkasan ahli mengikut kumpulan serta rekod individu.

Kuantiti ialah bilangan unit jenis yang dipilih: foto dikira mengikut keping, manakala jenis tiket tambahan boleh dikira mengikut tiket.

## Menyunting, memadam dan petikan sejarah

Ketik kad ahli di bawah tarikh. Satu pembelian membuka penyunting terus; beberapa pembelian memaparkan pemilih rekod dahulu. Anda juga boleh mengembangkan senarai rekod untuk menyuntingnya. Borang memuatkan tarikh, keahlian, jenis, kuantiti, harga dan maklumat pilihan asal. Simpan hanya mengemas kini rekod terpilih. Padam pembelian ini memerlukan pengesahan; pemadaman menolak kuantiti serta amaunnya.

Dalam Ahli & jenis, ketik ahli untuk menyunting maklumat, atau pensel di sebelah tajuk kumpulan untuk menyunting kumpulan. Kedua-dua pemadaman memerlukan pengesahan:

- Padam ahli: mengarkibkan ahli dan keahliannya dalam semua kumpulan.
- Padam kumpulan: mengarkibkan kumpulan dan keahliannya. Ahli yang turut menyertai kumpulan lain masih boleh digunakan di sana.

Data arkib kekal dalam sandaran. Pembelian, nama, warna, harga dan statistik terdahulu dikekalkan. Rekod baharu dan pertukaran keahlian mesti menggunakan keahlian aktif. Rekod sejarah masih boleh disunting dengan keahlian asal yang dipadam, yang ditandakan Dipadam dalam pemilih. Penyunting katalog tidak menyediakan pemulihan; mengimport sandaran yang mengandungi entri katalog aktif dengan ID sama boleh memulihkannya, tertakluk pada pengesahan gabungan penuh.

Menukar nama, warna atau harga lalai katalog tidak menulis semula pembelian tersimpan. Semasa menyunting pembelian, mengekalkan keahlian mengekalkan petikan nama ahli, kumpulan dan warna; mengekalkan jenis mengekalkan petikan nama jenis. Pertukaran keahlian atau jenis mengambil petikan baharu yang sepadan. Harga sebenar dan maklumat acara disimpan mengikut borang. Seluruh catatan disahkan sebelum disimpan secara atomik pada peranti; kegagalan tidak menerapkan sebahagian perubahan.

## Statistik

Statistik menyokong hari, bulan, tahun dan sepanjang masa, penapis jenis serta kumpulan, dan kedudukan ahli gabungan, ahli mengikut kumpulan atau kumpulan. Bilangan ahli ialah jumlah `memberID` berlainan dalam rekod selepas ditapis. Kumpulan arkib masih tersedia untuk penapisan sejarah.

Kedudukan kuantiti boleh mengira unit merentas semua mata wang, tetapi kedudukan perbelanjaan memerlukan satu mata wang. Amaun mata wang berlainan tidak ditambah bersama atau ditukar secara automatik. Ringkasan menunjukkan kuantiti mengikut jenis dan amaun mengikut mata wang. Kedudukan seri menggunakan bentuk `1、1、3`, dengan susunan stabil berdasarkan ID bagi nilai sama. Bahagian menggunakan jumlah kuantiti atau perbelanjaan satu mata wang dalam skop penapis sebagai penyebut; jumlah sifar memberikan bahagian sifar. Nama dan warna kedudukan menggunakan petikan rekod terkini dalam skop penapis; rekod pada tarikh sama dipilih mengikut susunan ID.

## Catatan contoh

Jika tiada fail tersimpan semasa pelancaran pertama, contoh rekaan dimuatkan dan disimpan terus: 2 kumpulan, 3 ahli, 4 keahlian, 3 jenis, 1 acara dan 5 rekod, berjumlah 16 unit. Tarikh menggunakan hari kalendar `Asia/Shanghai` ketika dicipta serta dua hari sebelumnya; kuantiti hari penciptaan ialah 10. Tarikh tidak ditetapkan kepada tarikh templat. Tiada lencana contoh dipaparkan.

Catatan sedia ada membaca data tersimpan. Pelancaran semula tidak memulihkan contoh yang disunting atau dipadam. Muatkan contoh catatan hanya dibenarkan untuk catatan kosong atau contoh yang belum diubah. Kosong bermaksud tiada kumpulan, ahli, keahlian, acara atau rekod; jenis juga mesti kosong atau sama tepat dengan tiga jenis lalai. Pengarkiban sahaja tidak menjadikan catatan kosong. Import pertama boleh menggantikan contoh yang belum diubah; selepas sebarang suntingan contoh disimpan, import menggunakan gabungan.

## Medan JSON

[ledger-template.json](../Templates/ledger-template.json) ialah contoh boleh import yang berasingan daripada templat Jadual. Gunakan UTF-8 dan tetapkan `schemaVersion` kepada `1`. Keenam-enam tatasusunan berikut wajib wujud; gunakan `[]` jika kosong. `id` mesti unik dalam setiap tatasusunan, dan ID serta nama tidak boleh hanya mengandungi ruang kosong.

| Tatasusunan | Medan wajib | Medan pilihan |
| --- | --- | --- |
| `groups` | `id`, `name` | `isArchived` |
| `members` | `id`, `name` | `avatarJPEGBase64`, `isArchived`、`isFavorite` |
| `memberships` | `id`, `memberID`, `groupID`, `color`, `prices` | `isArchived` |
| `products` | `id`, `name` | Tiada |
| `events` | `id`, `name` | `date`, `city`, `venue`, `address` |
| `records` | Lihat jadual berikut | Lihat jadual berikut |

`memberships` merujuk ahli dan kumpulan dalam fail yang sama. Setiap pasangan ahli/kumpulan hanya boleh mempunyai satu keahlian. `color` ialah RGB enam digit seperti `#F28AAF`. `prices` boleh berupa `[]`. Setiap harga memerlukan `productID`, `currency` dan `unitPriceMinor`, merujuk jenis sedia ada, dan mempunyai gabungan jenis/mata wang unik dalam keahlian itu.

`isArchived` ialah boolean pilihan; hanya `true` bermaksud diarkibkan. Sandaran lama tanpa medan ini masih boleh diimport, dengan versi `1`. Keahlian aktif turut memerlukan ahli dan kumpulan yang tidak diarkibkan. Data arkib masih mesti sah. Jika menyusun sandaran secara manual, kekalkan ahli, kumpulan dan keahlian yang dirujuk oleh pembelian sejarah.

| Medan `records` | Keperluan dan maksud |
| --- | --- |
| `id` | Wajib, ID pembelian unik; pembelian baharu menggunakan ID baharu, UUID disyorkan |
| `date` | Wajib, tarikh Gregorian sah dalam format ketat `yyyy-MM-dd`, seperti `2026-10-14` |
| `memberID`, `groupID`, `productID` | Wajib, merujuk ahli, kumpulan dan jenis dalam fail sama, dengan keahlian sepadan |
| `quantity` | Wajib, kuantiti integer positif |
| `unitPriceMinor` | Wajib, harga seunit integer bukan negatif dalam unit terkecil mata wang |
| `currency` | Wajib, kod mata wang huruf besar dalam jadual di bawah |
| `memberNameSnapshot`, `groupNameSnapshot`, `productNameSnapshot` | Wajib, nama ketika pembelian; tidak boleh hanya ruang kosong |
| `colorSnapshot` | Wajib, warna RGB enam digit ketika pembelian |
| `eventID` | Pilihan, merujuk ID dalam `events` fail yang sama |
| `eventNameSnapshot`, `citySnapshot`, `venueSnapshot`, `addressSnapshot`, `note` | Pilihan, acara, bandar, tempat, alamat dan nota ketika pembelian |

Tarikh pembelian disimpan sebagai hari kalendar dan tidak berubah selepas pertukaran zon waktu. Tarikh acara boleh berbeza daripada tarikh pembelian. Avatar mengandungi data JPEG berpengekodan Base64, bukannya laluan fail.

## Ketepatan amaun dan had

Jumlah ialah `quantity × unitPriceMinor`, menggunakan pengiraan integer. Masukkan harga biasa pada skrin untuk ditukar kepada unit terkecil. Lebihan tempat perpuluhan ditolak dan tidak dibundarkan secara senyap. Gunakan angka dan titik perpuluhan tanpa simbol mata wang atau pemisah ribuan. Amaun JSON mesti integer.

| Mata wang | Tempat perpuluhan | Contoh harga pada skrin | `unitPriceMinor` |
| --- | --- | --- | --- |
| `JPY`, `KRW` | 0 | `2000` | `2000` |
| `CNY` | 2 | `80.50` | `8050` |
| `TWD`, `HKD`, `USD`, `THB`, `IDR`, `MYR` | 2 | `100.00` | `10000` |

Ini ialah ketepatan pelaksanaan aplikasi semasa; `IDR` dan `TWD` juga mengekalkan dua digit unit terkecil. Limpahan integer diperiksa bagi pendaraban setiap pembelian, jumlah kuantiti dan jumlah amaun setiap mata wang. Teks JSON dan catatan selepas pengekodan dihadkan kepada `20 × 1024 × 1024` bait (20 MiB). Setiap avatar import selepas penyahkodan dihadkan kepada `2 × 1024 × 1024` bait (2 MiB) dan struktur JPEG diperiksa. Foto asal daripada pustaka dihadkan kepada 10 MiB, dikecilkan supaya sisi terpanjang maksimum 512 piksel dan ditukar kepada JPEG sebelum disimpan.

## Import, gabungan dan sandaran

Buka “Urus Catatan” di bahagian atas Catatan dan pilih “Sandaran”. Pilih satu atau beberapa fail JSON/teks, atau tampal JSON dan sahkan pratonton, kemudian sahkan import. Beberapa fail digabungkan sebelum pratonton. Fail kosong, dokumen tanpa data, medan tidak sah dan konflik menghalang import. Setiap fail mesti menyertakan semua data rujukan; ia tidak boleh bergantung pada catatan semasa untuk melengkapkan ahli atau kumpulan yang tiada. Menyunting teks yang ditampal membatalkan pratonton terdahulu.

Import sah menggantikan contoh yang belum diubah. Keadaan lain menggunakan peraturan gabungan berikut:

- Kumpulan, ahli, keahlian, jenis dan acara dikemas kini atau ditambah berdasarkan `id`. Data lama yang tiada dalam fail baharu dikekalkan. Import boleh mengemas kini penanda arkib.
- Pembelian dinyahgandakan berdasarkan `id`. Mengimport semula rekod sama tidak mengira dua kali. ID sama dengan kandungan berbeza menyebabkan ralat dan membatalkan seluruh import. Sunting pembelian dalam aplikasi dan gunakan ID baharu untuk pembelian baharu.
- Dokumen penuh disahkan sebelum dan selepas gabungan, termasuk keunikan keahlian, rujukan, tarikh, petikan, amaun dan saiz. Kegagalan mengekalkan catatan asal.

Eksport mengandungi seluruh catatan, data arkib, petikan sejarah, harga dan avatar. Pemulihan menggunakan gabungan dan tidak membatalkan suntingan pembelian sedia ada. Rekod disunting bercanggah dengan rekod sandaran lama yang mempunyai ID sama tetapi kandungan berbeza. Simpan fail eksport untuk pemindahan atau rekod peribadi.

## Pengurusan, ahli kegemaran dan paparan sistem

- Dalam 0.4.0 (build 10), butang atas Catatan dan Tetapan → Pengurusan data membuka “Urus Catatan”. Halaman ini mengandungi “Urus rekod”, “Pembersihan data ahli dan kumpulan” dan “Sandaran”. Import JSON, pratonton dan eksport penuh asal kekal di “Sandaran”. Tetapan turut menyediakan “Urus Diari”.
- Gabungkan penapis pembelian mengikut julat tarikh, ahli, kumpulan, jenis, mata wang, acara dan carian, kemudian pilih berbilang rekod, pilih semua hasil semasa atau kosongkan pilihan. Perubahan penapis mengosongkan pilihan; hasil kosong tidak boleh dipadam. Pengesahan memaparkan bilangan rekod dipilih, jumlah item dan amaun setiap mata wang. Batal mengekalkan data; pengesahan jelas hanya memadam pilihan dan mengemas kini statistik.
- “Padam semua rekod pembelian” meliputi seluruh Catatan tanpa mengikut penapis semasa dan memerlukan pengesahan bilangan rekod, item dan amaun setiap mata wang. Kumpulan, ahli, jenis, katalog acara dan diari kekal untuk rekod baharu.
- “Pembersihan data ahli dan kumpulan” menyokong carian dan pilihan berbilang ahli atau kumpulan. Pemadaman ahli mengarkibkan semua keahliannya; pemadaman kumpulan hanya mengarkibkan kumpulan itu dan keahliannya, sementara keahlian kumpulan lain kekal. Pembelian sejarah, petikan dan statistik dikekalkan dan rekod sejarah masih boleh disunting. Batal tidak mengarkibkan; sandaran sah boleh memulihkan katalog.
- Tetapkan atau nyahpilih “Ahli kegemaran” dalam penyunting ahli di “Ahli dan jenis”. Pilihan terikat pada ID ahli dan dikongsi oleh semua keahlian. Pemilih ahli pembelian memaparkan kegemaran di atas; setiap keahlian mengekalkan warna dan harga sendiri serta senarai kumpulan penuh di bawah. Rekod baharu mengutamakan keahlian kegemaran yang tersedia. Pautan ahli sedia ada juga mendahulukan kegemaran; ahli atau keahlian diarkibkan dikecualikan daripada pintasan.
- `isFavorite` ahli ialah Bool pilihan: hanya `true` bermaksud kegemaran; `false`, tiada medan atau `null` bermaksud bukan kegemaran. JSON lama masih boleh diimport dan `schemaVersion` kekal `1`. Sandaran penuh menyimpan pilihan ini, dan gabungan katalog mengikut ID ahli mengemas kininya.
- Aplikasi mengikut paparan cerah atau gelap sistem untuk jadual, Catatan, diari, tetapan dan semua halaman suntingan, pilihan, pengurusan serta sandaran. Perubahan sistem terus digunakan tanpa tetapan tema berasingan. Warna ahli dan petikan sejarah dikekalkan.

Kembali ke [README](../README.ms.md) · [Panduan pengesahan Catatan](LEDGER-VERIFICATION.ms.md) · [Templat JSON](../Templates/ledger-template.json)
