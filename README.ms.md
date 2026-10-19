# Oshicue Kit · Android

[简体中文](README.md) | [繁體中文](README.zh-Hant.md) | [繁體粵語](README.zh-HK.md) | [English](README.en.md) | [日本語](README.ja.md) | [한국어](README.ko.md) | [ไทย](README.th.md) | [Bahasa Indonesia](README.id.md) | [Bahasa Melayu](README.ms.md)

Aplikasi Android natif untuk menyusun jadual persembahan dan perjalanan, melihat jadual acara, merekod perbelanjaan barangan, serta menulis diari aktiviti. Dibangunkan dengan **Kotlin, Jetpack Compose dan Material 3**, dengan lima tab: Agenda, Jadual, Catatan, Diari dan Tetapan.

Versi semasa: **0.5.3 (Build 16)**. Menyokong **Android 8.0 (API 26) dan ke atas**, dengan ID aplikasi `com.oshicue.kit`.

## Mula menggunakan

1. Pasang APK yang telah ditandatangani, atau bina daripada kod sumber mengikut panduan di bawah.
2. Pada pelancaran pertama, aplikasi memuatkan contoh jadual acara dan catatan rekaan; agenda dan diari bermula dalam keadaan kosong.
3. Tukar bahasa dalam Tetapan, atau buka halaman pengurusan setiap modul untuk mengimport fail JSON anda sendiri.
4. Eksport sandaran agenda, catatan dan diari secara berkala, serta simpan fail jadual acara asal.

Import jadual acara sebenar yang pertama akan menggantikan contoh. Selepas itu, acara dikemas kini atau ditambah mengikut ID. Import catatan sebenar yang pertama akan menggantikan contoh yang belum diubah; contoh yang telah disunting secara manual akan dikekalkan dan digabungkan. Nama, tarikh, penerbangan dan lokasi dalam contoh hanya untuk mencuba aplikasi. Gantikannya dengan maklumat sebenar sebelum digunakan.

## Ciri utama

### Agenda

- Urus Live, penerbangan, hotel, restoran dan lokasi lain dalam kalendar, dengan sokongan aktiviti berwaktu dan sepanjang hari, zon waktu, alamat serta nota. Agenda ini berasingan daripada pilihan “Jadual Saya” dalam modul Jadual.
- Bagi penerbangan, rekod waktu, zon waktu serta lapangan terbang berlepas dan ketibaan secara berasingan. Bagi hotel, waktu daftar masuk dan daftar keluar menjadi titik dalam rancangan perjalanan.
- Cari bandar dan tempat, kemudian lihat lokasi atau laluan dalam aplikasi peta luaran. Hasil carian tidak menetapkan zon waktu secara automatik; sila sahkannya secara manual.
- Paparkan sela masa atau pertindihan antara aktiviti berwaktu yang berturutan. Aktiviti sepanjang hari tidak diambil kira dalam perbandingan segmen perjalanan. Anggaran tempoh laluan dalam aplikasi dan semakan penerbangan masa nyata belum tersedia.
- Simpan salinan ke kalendar sistem melalui penyunting kalendar Android. Salinan itu dan rekod dalam aplikasi disunting secara berasingan.
- Menyediakan carian, gabungan penapis, pemadaman berbilang pilihan, pratonton import JSON dan sandaran keseluruhan agenda.

### Jadual

- Tapis mengikut tarikh, acara dan jenis LIVE／jualan barangan. Lihat tempat secara mendatar dan waktu secara menegak.
- Ketik sekali untuk melihat butiran sesi; ketik dua kali atau tekan lama untuk menambahkannya ke atau mengeluarkannya daripada “Jadual Saya”. Tindakan yang sama juga tersedia pada halaman butiran.
- Sesi yang bertindih dipaparkan bersebelahan dengan penanda pertembungan. Menyokong zum 50%, 70%, 100%, 150%.
- Garis waktu semasa dikemas kini setiap 15 saat; sesi yang melintasi tengah malam dibahagikan mengikut zon waktu paparan. Paparan berbilang acara menggunakan zon waktu acara pertama dalam susunan dokumen daripada acara yang dipilih.
- Menyokong tampalan teks atau import berbilang fail, pengesahan dan pratonton. Acara dengan ID yang sama dikemas kini sepenuhnya, manakala acara lain dikekalkan. ID acara yang berulang dalam satu kelompok import akan ditolak.
- Pemadaman acara memerlukan pengesahan dan mengosongkan pilihan sesi acara tersebut, sementara catatan dan diari dikekalkan. Pada masa ini tiada menu eksport jadual; sila simpan fail asal yang diimport.

### Catatan · Ledger

- Lihat kuantiti pembelian barangan harian dan bulanan serta perbelanjaan mengikut mata wang melalui kalendar. Urus kumpulan, ahli, avatar, jenis barangan dan maklumat acara.
- Seorang ahli boleh menyertai beberapa kumpulan, dengan warna dan harga lalai berasingan bagi setiap keahlian. Tetapan ahli kegemaran dikongsi oleh semua keahliannya.
- Rekod tarikh, kuantiti, mata wang, harga seunit dan salinan maklumat acara semasa pembelian. Rekod baharu mengutamakan harga bagi ahli, kumpulan, jenis dan mata wang yang sama pada atau sebelum tarikh yang dipilih, kemudian harga penambahan terkini yang diingati, harga lalai ahli atau 0. Harga seunit boleh diubah secara manual.
- Kedudukan menyokong tempoh harian, bulanan, tahunan dan sepanjang masa, dengan paparan jumlah ahli, ahli mengikut kumpulan, kumpulan, kuantiti atau perbelanjaan.
- Menyokong `CNY`, `JPY`, `HKD`, `TWD`, `USD`, `KRW`, `THB`, `IDR`, `MYR`; jumlah dikira secara berasingan bagi setiap mata wang tanpa penukaran kadar tukaran. `JPY`, `KRW` menggunakan nombor bulat, manakala mata wang lain menggunakan dua tempat perpuluhan. `unitPriceMinor` dalam JSON ialah integer dalam unit mata wang terkecil.
- Mengeluarkan ahli atau kumpulan akan mengarkibkan maklumat dan keahlian berkaitan, serta mengekalkan salinan maklumat pembelian terdahulu dan statistik. Pengurusan rekod menyokong penapis, pemadaman berkelompok, sandaran JSON lengkap dan import.

### Diari · Diary

- Tulis tajuk, isi, ringkasan dan tag, dengan status “Draf” atau “Sedia”. Semak mengikut tarikh, cari atau tapis catatan.
- Ambil tarikh, nama, bandar, tempat acara dan alamat daripada acara dalam Catatan; diari menyimpan salinan maklumatnya sendiri.
- Perubahan disimpan secara automatik kira-kira 0.5 saat selepas penyuntingan berhenti. Perubahan yang masih belum disimpan akan disimpan apabila menekan Selesai atau menutup penyunting. Jika penyimpanan gagal, aplikasi memaparkan arahan untuk mencuba lagi.
- Salin sebagai Markdown／teks biasa, eksport `.md`／`.txt`, atau kongsi melalui sistem Android. “Sedia” hanyalah status setempat; perkongsian tidak menerbitkan diari secara automatik.
- Menyediakan pengurusan berbilang catatan dan sandaran JSON lengkap. Rekod dengan ID dan kandungan yang sama dinyahduplikasi; jika ID yang sama mempunyai kandungan bercanggah, keseluruhan import akan ditolak.

## Templat JSON dan sandaran

| Kandungan | Fail terbina dalam |
| --- | --- |
| Templat agenda | [agenda-template.json](app/src/main/assets/templates/agenda-template.json) |
| Templat jadual | [timetable-template.json](app/src/main/assets/templates/timetable-template.json) |
| Contoh jadual | [demo-timetable.json](app/src/main/assets/templates/demo-timetable.json) |
| Templat catatan | [ledger-template.json](app/src/main/assets/templates/ledger-template.json) |

Fail JSON menggunakan UTF-8 dan diimport melalui modul yang sepadan, dengan pratonton sebelum pengesahan. Struktur JSON setiap modul berbeza dan tidak boleh saling digunakan walaupun semuanya menggunakan `schemaVersion: 1`. Untuk diari, cipta satu catatan kemudian eksport sandarannya sebagai rujukan format; kod sumber tidak menyertakan templat diari berasingan.

## Data setempat dan tetapan

Data disimpan dalam direktori peribadi aplikasi `filesDir/OshicueKit/` sebagai `agenda.json`, `schedule.json`, `ledger.json`, `diary.json` dan `settings.json`. Bahasa dan tab yang terakhir dibuka diingati secara setempat; pilihan sesi jadual juga disimpan pada peranti.

Secara lalai, aplikasi mengikut bahasa sistem dan menggunakan bahasa Inggeris jika bahasa itu tidak disokong. Anda boleh memilih antara sembilan bahasa secara manual. Antara muka mengikut mod cerah／gelap sistem dan bertindak balas kepada tetapan kontras dipertingkat sistem pada Android 14 dan ke atas.

Rekod utama boleh digunakan di luar talian tanpa akaun. Pada masa ini, aplikasi belum menyediakan penyegerakan awan dalam aplikasi, sandaran awan automatik, peringatan push atau import daripada imej. Carian tempat bergantung pada perkhidmatan geokod peranti. Peta, laluan dan kalendar luaran bergantung pada aplikasi serta perkhidmatan yang dipasang, yang mungkin memerlukan internet. Eksport sandaran secara manual sebelum menyahpasang aplikasi atau mengosongkan datanya.

## Membina daripada kod sumber

Berikut ialah konfigurasi projek semasa dan persekitaran yang digunakan untuk menghasilkan pakej ini:

| Komponen | Versi |
| --- | --- |
| Versi aplikasi / `versionCode` | `0.5.3` / `16` |
| Android API minimum / sasaran / kompilasi | `26` / `37` / `37` |
| Android Gradle Plugin | `9.4.1` |
| Pemalam Kotlin Compose | `2.2.10` |
| Compose BOM | `2026.09.00` |
| Sasaran kod sumber dan kod bait Java | `17` |
| Gradle / JBR yang digunakan untuk pengesahan ini | `9.8.1` / `25.0.3` |

1. Buka direktori ini dalam Android Studio, kemudian pasang Android SDK Platform 37 dan alat binaan yang diperlukan.
2. Gunakan JDK yang sesuai untuk menjalankan Gradle. Pengesahan ini menggunakan JBR yang disertakan dengan Android Studio. Sasaran kompilasi Java `17` dan versi JDK untuk menjalankan Gradle ialah dua tetapan berbeza.
3. Biarkan Android Studio menjana `local.properties`, atau isi `sdk.dir` dengan laluan mutlak SDK pada peranti anda. Jangan commit fail ini.
4. Tambahkan Gradle yang telah dipasang ke dalam `PATH`, kemudian jalankan arahan berikut dari direktori ini. Penyelesaian kebergantungan kali pertama memerlukan internet.

Kod sumber tidak menyertakan Gradle Wrapper, maka arahan berikut menggunakan `gradle`:

```sh
gradle :app:assembleDebug
gradle :app:testDebugUnitTest :app:lintDebug
```

Lokasi output APK: `app/build/outputs/apk/debug/app-debug.apk`. Binaan lalai menggunakan tandatangan nyahpepijat; projek semasa belum menetapkan tandatangan keluaran rasmi, jadi anda perlu mengkonfigurasinya sebelum penerbitan.

Kod sumber yang sepadan telah lulus **166 ujian unit** semasa pakej ini dihasilkan, dan APK lulus pemeriksaan tandatangan serta penjajaran. Repositori belum menyediakan suite ujian antara muka Android automatik; peta, kalendar sistem, pemilihan fail dan perkongsian perlu disahkan pada peranti sasaran.

Apabila memuat naik ke GitHub, kecualikan `local.properties`, `.gradle/`, `.kotlin/`, `build/`, `app/build/`, fail IDE setempat dan kunci tandatangan.

## Struktur projek

| Laluan | Kandungan |
| --- | --- |
| `app/src/main/java/com/oshicue/kit/core/` | Model data, JSON, import, statistik dan penyimpanan setempat |
| `app/src/main/java/com/oshicue/kit/ui/` | Halaman Compose, penyunting, pengurusan dan operasi fail |
| `app/src/main/assets/localization/` | Teks antara muka dalam sembilan bahasa |
| `app/src/main/assets/templates/` | Templat JSON agenda, jadual dan catatan |
| `app/src/main/res/` | Sumber ikon, fon dan tema |
| `app/src/test/` | Ujian unit bahagian teras |

## Bahasa dokumentasi dan lesen fon

Bahasa Cina Ringkas ialah naskhah sumber; lapan README lain mempunyai kandungan yang sepadan dengan halaman ini. Kod bahasanya ialah `zh-Hans`, `zh-Hant`, `zh-HK`, `en`, `ja`, `ko`, `th`, `id`, `ms`. Kunci JSON, ID, nilai enum, arahan dan laluan tidak diterjemahkan.

Fon Unbounded yang disertakan menggunakan [SIL Open Font License 1.1](app/src/main/assets/fonts-OFL.txt). Komponen pihak ketiga tertakluk pada lesen masing-masing.
