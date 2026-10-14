# Oshicue Kit for Android

[简体中文](README.md) | [繁體中文](README.zh-Hant.md) | [繁體粵語](README.zh-HK.md) | [English](README.en.md) | [日本語](README.ja.md) | [한국어](README.ko.md) | [ไทย](README.th.md) | [Bahasa Indonesia](README.id.md) | [Bahasa Melayu](README.ms.md)

Aplikasi Android natif (Kotlin + Jetpack Compose) untuk jadual acara, perbelanjaan barangan, dan catatan acara. Empat tabnya ialah Jadual, Catatan, Diari, dan Tetapan. Versi semasa ialah **0.4.0 (versionCode 10)**, sejajar cirinya dengan aplikasi iOS Oshicue Kit 0.4.0 (build 10), dan menyokong Android 8.0 (API 26) ke atas.

## Tangkapan skrin

| Jadual | Jadual saya | Kalendar Catatan |
| --- | --- | --- |
| ![Jadual](screenshots/timetable.png) | ![Jadual saya](screenshots/my-timetable.png) | ![Kalendar Catatan](screenshots/ledger-calendar.png) |

| Tambah catatan | Statistik Catatan | Diari |
| --- | --- | --- |
| ![Tambah catatan](screenshots/ledger-record-form.png) | ![Statistik Catatan](screenshots/ledger-statistics.png) | ![Diari](screenshots/diary.png) |

| Catatan baharu | Tetapan |
| --- | --- |
| ![Catatan baharu](screenshots/diary-editor.png) | ![Tetapan](screenshots/settings.png) |

## Ciri-ciri

### Jadual

- Semua sesi dikelompokkan mengikut venue dan LIVE／barangan. Penapis tarikh, berbilang acara, dan jenis sesi boleh digabungkan. Leret mendatar untuk venue dan menegak untuk masa.
- Ketik sesi untuk butiran penuh; ketik dua kali untuk menanda atau menanggalkan kumpulan daripada kegemaran. Kegemaran dikongsi antara acara mengikut `artistID`; "Jadual saya" menggabungkan sesi LIVE dan barangan kumpulan kegemaran, memaparkan pertindihan bersebelahan dengan amaran konflik.
- Zum tersedia pada 50%, 70%, 100%, dan 150% (lalai 100%, iaitu 4dp／minit). Pertindihan padat melebarkan papan; sesi pendek mengutamakan nama dengan penyesuaian saiz huruf automatik, dan butiran penuh kekal tersedia.
- Garis masa semasa menggunakan masa sistem sebenar dan dikemas kini setiap 15 saat. "Sekarang" bertukar ke hari ini hanya jika acara terpilih mempunyai sesi hari ini.
- Berbilang acara menggunakan zon waktu acara pertama dalam susunan dokumen; butiran boleh memaparkan waktu tempatan acara asal; sesi yang melintasi tengah malam dipecahkan kepada tarikh berkaitan mengikut zon waktu paparan.
- Import dengan menampal JSON atau memilih satu／beberapa fail UTF-8 JSON, kemudian sahkan, pratonton, dan sahkan. ID acara yang sama menggantikan keseluruhan acara itu; ID pendua dalam satu kelompok fail ditolak. Memadam jadual memerlukan pengesahan dan membersihkan kegemaran yang tidak lagi wujud dalam mana-mana acara yang tinggal.

### Catatan

- Kalendar memaparkan kuantiti harian dan bulanan serta jumlah berasingan mengikut mata wang. Ketik kad ahli untuk mengedit pembelian; berbilang rekod memerlukan pemilihan dahulu.
- Urus kumpulan, ahli, avatar, warna perwakilan dan harga lalai bagi setiap gabungan, jenis pembelian, serta templat acara. Ahli berbilang kumpulan ditambah dengan memautkan ahli sedia ada.
- Rekod tarikh, ahli dan kumpulan, jenis, kuantiti, mata wang, harga seunit, serta maklumat acara pilihan yang boleh juga diambil daripada jadual.
- Kedudukan merangkumi hari／bulan／tahun／seumur hidup, dengan kuantiti atau perbelanjaan, ahli gabungan／ahli mengikut kumpulan／kumpulan, serta penapis jenis dan kumpulan. Seri menggunakan `1、1、3`; perbelanjaan menggunakan satu mata wang dan tidak menukar kadar.
- Rekod lama menyimpan petikan nama, warna, acara, dan harga; pemadaman ahli／kumpulan bersifat arkib supaya rekod dan statistik kekal dan rekod lama masih boleh diedit.
- Ahli boleh ditandakan sebagai ahli kegemaran dan dipaparkan di atas dalam pemilih ahli; nilai wang menggunakan unit kecil integer bagi `CNY`, `JPY`, `HKD`, `TWD`, `USD`, `KRW`, `THB`, `IDR`, dan `MYR`.
- Pengurusan Catatan merangkumi urus rekod (penapis julat tarikh, ahli, kumpulan, jenis, mata wang, acara, dan teks lalu padam pukal atau padam semua), pembersihan katalog (arkib ahli／kumpulan), dan sandaran JSON (eksport, import dengan pratonton, penolakan konflik).

### Diari

- Cipta rep acara dengan tajuk, isi, ringkasan, dan tag; tandakan sebagai draf atau sedia, layari mengikut tarikh acara, tapis mengikut status, dan cari teks serta maklumat acara.
- Bawa masuk tarikh, nama, bandar, venue, dan alamat daripada acara Catatan lalu edit secara berasingan. Diari menyimpan petikan sendiri.
- Simpan automatik berjalan kira-kira 0.5 saat selepas berhenti mengedit; menutup editor atau berpindah ke latar belakang menyimpan serta-merta, dan simpanan yang gagal boleh dicuba semula.
- "Sedia terbit" boleh menyalin Markdown／teks biasa, mengeksport `.md`／`.txt`, atau menggunakan helaian kongsi sistem.
- Pengurusan Diari menyediakan penapis, padam pukal, serta eksport/import sandaran JSON.

### Tetapan dan data

- Pilihan bahasa (9 bahasa serta ikut sistem) berkuat kuasa serta-merta; tarikh, masa, dan nombor mengikut format lokasi terpilih.
- Import jadual, urus Catatan, urus Diari, muatkan contoh, dan maklumat versi berada di satu tempat.
- Ketiga-tiga modul disimpan secara berasingan dalam peranti pada `files/OshicueKit/` sebagai `schedule.json`, `ledger.json`, dan `diary.json` dengan tulisan atomik; ciri teras berfungsi luar talian.

## Bina dan jalankan

Keperluan:

- JDK 17 ke atas (disyorkan JBR bawaan Android Studio atau JDK 21+)
- Android SDK Platform 37 dan Build-Tools 36 (Android Studio boleh memasangnya secara automatik)
- Cipta `local.properties` di akar repositori dengan `sdk.dir=/path/to/Android/sdk`, atau tetapkan pemboleh ubah persekitaran `ANDROID_HOME`
- Binaan pertama memerlukan akses rangkaian ke repositori Maven; Gradle Wrapper memuat turun versinya sendiri

```sh
./gradlew :app:assembleDebug        # Bina APK nyahpepijat
./gradlew :app:installDebug         # Pasang pada peranti/emulator yang disambung
./gradlew :app:testDebugUnitTest    # 93 ujian unit (sepadan 1:1 dengan ujian iOS)
./gradlew :app:lintDebug            # Analisis statik
./gradlew :app:assembleRelease      # Bina APK keluaran
```

### Tandatangan keluaran

Repositori tidak mengandungi bahan tandatangan. Untuk menghasilkan APK keluaran yang boleh dipasang dan diedarkan, cipta `keystore.properties` di akar repositori (fail ini dan `*.jks` sudah dikecualikan dalam `.gitignore`).

```sh
keytool -genkeypair -v -keystore oshicue-kit-release.jks -alias oshicue-kit \
  -keyalg RSA -keysize 2048 -validity 10000
```

```properties
storeFile=oshicue-kit-release.jks
storePassword=kata-laluan-anda
keyAlias=oshicue-kit
keyPassword=kata-laluan-anda
```

Kemudian jalankan `./gradlew :app:assembleRelease`; hasilnya di `app/build/outputs/apk/release/app-release.apk` (tanpa `keystore.properties` akan terhasil APK tanpa tandatangan). Simpan `oshicue-kit-release.jks` dan kata laluannya dengan selamat — versi seterusnya mesti ditandatangani dengan kunci yang sama.

## Format data dan keserasian

- Format JSON ketiga-tiga modul sama sepenuhnya dengan aplikasi iOS (`schemaVersion` bernilai `1`); sandaran yang dieksport／diimport boleh digunakan silang dengan iOS, dan struktur fail data dalam peranti juga sama.
- Cap masa menggunakan ISO 8601 dengan milisaat (`yyyy-MM-ddTHH:mm:ss.SSSZ`); tarikh menggunakan kalendar Gregorian `yyyy-MM-dd`; jumlah wang menggunakan unit kecil integer (`unitPriceMinor`).
- Import sentiasa disahkan dan dipratonton sebelum disahkan; rekod dengan ID sama tetapi kandungan berbeza menolak seluruh import supaya sejarah tidak ditimpa secara senyap.
- `Templates/` dan `docs/` menyambung templat dan dokumentasi format daripada projek iOS.

## Penyetempatan

Sembilan bahasa disertakan — Cina Ringkas, Cina Tradisional (Taiwan), Kantonis Tradisional (Hong Kong), English, 日本語, 한국어, ไทย, Bahasa Indonesia, dan Bahasa Melayu — di `app/src/main/res/values-*/strings.xml`.

`tools/convert_localization.py` menjana semula sumber bahasa repositori ini daripada `Localizable.strings` iOS (termasuk penukaran `%@` → `%1$s` kepada argumen berkedudukan). Jika hanya menguruskan bahagian Android, edit fail `strings.xml` berkenaan secara terus.

## Struktur repositori

| Laluan | Kandungan |
| --- | --- |
| `app/src/main/kotlin/com/oshicue/kit/core/` | Model, import/eksport JSON, statistik dan kedudukan, susun atur garis masa |
| `app/src/main/kotlin/com/oshicue/kit/data/` | Tiga stor bebas, tulisan atomik, dan sandaran pemulihan |
| `app/src/main/kotlin/com/oshicue/kit/i18n/` | Peleraian bahasa dan carian teks |
| `app/src/main/kotlin/com/oshicue/kit/ui/` | Skrin Compose: jadual, Catatan, Diari, Tetapan, dan semua halaman edit／sandaran |
| `app/src/main/res/` | Sumber 9 bahasa, ikon pelancar, fon Unbounded |
| `app/src/main/assets/` | Templat import terbina dalam |
| `app/src/test/` | Ujian unit (pengesahan import, ketepatan mata wang, seri kedudukan, susun atur garis masa, pengendalian UTF-8, dan lain-lain) |
| `tools/` | Skrip penukaran sumber bahasa |
| `Templates/`, `docs/` | Templat JSON dan dokumentasi format (disambung daripada iOS) |

## Perbezaan daripada versi iOS

- Komponen platform: import/eksport fail menggunakan pemilih fail sistem, avatar menggunakan pemilih foto sistem (dikecilkan kepada 512px, JPEG 0.8, disimpan Base64 — sama seperti iOS), dan perkongsian menggunakan helaian kongsi sistem.
- Kebolehcapaian: tindakan tersuai VoiceOver iOS dipetakan kepada gerak isyarat ketik／ketik dua kali dan semantik Compose.
- Selebihnya — aliran interaksi, kes tepi, dan format data JSON — sama dengan aplikasi iOS.

## Lesen

Projek ini menyambung [MIT License](LICENSE) daripada projek asal; komponen pihak ketiga mengekalkan lesen masing-masing. Fon Unbounded terbina dalam menggunakan [SIL OFL 1.1](THIRD_PARTY_NOTICES); lihat [notis pihak ketiga](THIRD_PARTY_NOTICES).

Oshicue Kit asal ialah aplikasi iOS natif berasaskan SwiftUI (0.4.0, build 10); repositori ini ialah port Android yang lengkap.
