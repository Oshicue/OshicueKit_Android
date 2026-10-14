# Oshicue Kit for Android

[简体中文](README.md) | [繁體中文](README.zh-Hant.md) | [繁體粵語](README.zh-HK.md) | [English](README.en.md) | [日本語](README.ja.md) | [한국어](README.ko.md) | [ไทย](README.th.md) | [Bahasa Indonesia](README.id.md) | [Bahasa Melayu](README.ms.md)

Aplikasi Android native (Kotlin + Jetpack Compose) untuk jadwal acara, pengeluaran merchandise, dan catatan acara. Empat tabnya adalah Jadwal, Catatan, Catatan Harian, dan Pengaturan. Versi saat ini **0.4.0 (versionCode 10)**, selaras fiturnya dengan aplikasi iOS Oshicue Kit 0.4.0 (build 10), dan mendukung Android 8.0 (API 26) ke atas.

## Tangkapan layar

| Jadwal | Jadwal saya | Kalender Catatan |
| --- | --- | --- |
| ![Jadwal](screenshots/timetable.png) | ![Jadwal saya](screenshots/my-timetable.png) | ![Kalender Catatan](screenshots/ledger-calendar.png) |

| Tambah catatan | Statistik Catatan | Catatan harian |
| --- | --- | --- |
| ![Tambah catatan](screenshots/ledger-record-form.png) | ![Statistik Catatan](screenshots/ledger-statistics.png) | ![Catatan harian](screenshots/diary.png) |

| Catatan baru | Pengaturan |
| --- | --- |
| ![Catatan baru](screenshots/diary-editor.png) | ![Pengaturan](screenshots/settings.png) |

## Fitur

### Jadwal

- Semua sesi dikelompokkan berdasarkan venue dan LIVE／merchandise. Filter tanggal, multi-acara, dan jenis sesi dapat dikombinasikan. Geser horizontal untuk venue dan vertikal untuk waktu.
- Ketuk sesi untuk detail lengkap; ketuk dua kali untuk menandai atau melepas grup dari favorit. Favorit dibagikan antar acara berdasarkan `artistID`; "Jadwal saya" menggabungkan sesi LIVE dan merchandise dari grup favorit, menampilkan tumpang tindih berdampingan dengan peringatan konflik.
- Zoom tersedia 50%, 70%, 100%, dan 150% (default 100%, yaitu 4dp／menit). Tumpang tindih padat melebarkan papan; sesi pendek memprioritaskan nama dengan penyesuaian ukuran huruf otomatis, dan detail lengkap tetap tersedia.
- Garis waktu saat ini memakai waktu sistem asli dan diperbarui setiap 15 detik. "Sekarang" berpindah ke hari ini hanya jika acara terpilih memiliki sesi hari ini.
- Beberapa acara memakai zona waktu acara pertama dalam urutan dokumen; detail dapat menampilkan waktu lokal acara asal; sesi lintas tengah malam dibagi ke tanggal terkait sesuai zona waktu tampilan.
- Impor dengan menempelkan JSON atau memilih satu／beberapa berkas UTF-8 JSON, lalu validasi, pratinjau, dan konfirmasi. ID acara yang sama menggantikan seluruh acara tersebut; ID duplikat dalam satu batch berkas ditolak. Menghapus jadwal memerlukan konfirmasi dan membersihkan favorit yang tidak lagi ada di acara mana pun.

### Catatan

- Kalender menampilkan jumlah harian dan bulanan serta total terpisah per mata uang. Ketuk kartu anggota untuk mengedit pembelian; beberapa catatan memerlukan pemilihan terlebih dahulu.
- Kelola grup, anggota, avatar, warna perwakilan dan harga default per afiliasi, jenis pembelian, serta templat acara. Anggota rangkap ditambahkan dengan menautkan anggota yang sudah ada.
- Catat tanggal, anggota dan grup, jenis, jumlah, mata uang, harga satuan, serta informasi acara opsional yang juga dapat diambil dari jadwal.
- Peringkat mencakup hari／bulan／tahun／seumur hidup, dengan jumlah atau pengeluaran, anggota gabungan／anggota per grup／grup, serta filter jenis dan grup. Seri memakai `1、1、3`; pengeluaran memakai satu mata uang dan tidak mengonversi kurs.
- Catatan lama menyimpan snapshot nama, warna, acara, dan harga; penghapusan anggota／grup bersifat arsip sehingga catatan dan statistik tetap ada dan catatan lama tetap dapat diedit.
- Anggota dapat ditandai sebagai anggota favorit dan ditampilkan di atas pada pemilih anggota; nilai uang memakai satuan minor bilangan bulat untuk `CNY`, `JPY`, `HKD`, `TWD`, `USD`, `KRW`, `THB`, `IDR`, dan `MYR`.
- Pengelolaan Catatan mencakup kelola catatan (filter rentang tanggal, anggota, grup, jenis, mata uang, acara, dan teks lalu hapus massal atau hapus semua), pembersihan katalog (arsip anggota／grup), dan cadangan JSON (ekspor, impor dengan pratinjau, penolakan konflik).

### Catatan harian

- Buat rekap acara dengan judul, isi, ringkasan, dan tag; tandai sebagai draf atau siap, telusuri berdasarkan tanggal acara, filter status, dan cari teks serta informasi acara.
- Bawa tanggal, nama, kota, venue, dan alamat dari acara Catatan lalu edit secara mandiri. Catatan harian menyimpan snapshot sendiri.
- Simpan otomatis berjalan sekitar 0,5 detik setelah berhenti mengedit; menutup editor atau berpindah ke latar belakang segera menyimpan, dan penyimpanan yang gagal bisa dicoba lagi.
- "Siap terbit" dapat menyalin Markdown／teks biasa, mengekspor `.md`／`.txt`, atau memakai lembar berbagi sistem.
- Pengelolaan Catatan Harian menyediakan filter, hapus massal, serta ekspor/impor cadangan JSON.

### Pengaturan dan data

- Pilihan bahasa (9 bahasa plus ikuti sistem) berlaku langsung; tanggal, waktu, dan angka memakai format lokasi terpilih.
- Impor jadwal, kelola Catatan, kelola Catatan Harian, muat contoh, dan informasi versi berada di satu tempat.
- Ketiga modul tersimpan terpisah di perangkat pada `files/OshicueKit/` sebagai `schedule.json`, `ledger.json`, dan `diary.json` dengan penulisan atomik; fitur inti berjalan offline.

## Build dan menjalankan

Kebutuhan:

- JDK 17 ke atas (disarankan JBR bawaan Android Studio atau JDK 21+)
- Android SDK Platform 37 dan Build-Tools 36 (Android Studio dapat memasangnya otomatis)
- Buat `local.properties` di akar repositori berisi `sdk.dir=/path/to/Android/sdk`, atau setel variabel lingkungan `ANDROID_HOME`
- Build pertama memerlukan akses jaringan ke repositori Maven; Gradle Wrapper mengunduh versinya sendiri

```sh
./gradlew :app:assembleDebug        # Build APK debug
./gradlew :app:installDebug         # Pasang ke perangkat/emulator yang terhubung
./gradlew :app:testDebugUnitTest    # 93 pengujian unit (sepadan 1:1 dengan pengujian iOS)
./gradlew :app:lintDebug            # Analisis statis
./gradlew :app:assembleRelease      # Build APK rilis
```

### Penandatanganan rilis

Repositori tidak memuat materi tanda tangan. Untuk menghasilkan APK rilis yang dapat dipasang dan dibagikan, buat `keystore.properties` di akar repositori (berkas ini dan `*.jks` sudah dikecualikan lewat `.gitignore`).

```sh
keytool -genkeypair -v -keystore oshicue-kit-release.jks -alias oshicue-kit \
  -keyalg RSA -keysize 2048 -validity 10000
```

```properties
storeFile=oshicue-kit-release.jks
storePassword=kata-sandi-anda
keyAlias=oshicue-kit
keyPassword=kata-sandi-anda
```

Lalu jalankan `./gradlew :app:assembleRelease`; hasilnya di `app/build/outputs/apk/release/app-release.apk` (tanpa `keystore.properties` akan dihasilkan APK tanpa tanda tangan). Simpan `oshicue-kit-release.jks` dan kata sandinya dengan aman — versi berikutnya harus ditandatangani dengan kunci yang sama.

## Format data dan kompatibilitas

- Format JSON ketiga modul identik dengan aplikasi iOS (`schemaVersion` bernilai `1`); cadangan yang diekspor／diimpor dari aplikasi ini dapat dipakai silang dengan iOS, dan struktur berkas data di perangkat juga sama.
- Cap waktu memakai ISO 8601 dengan milidetik (`yyyy-MM-ddTHH:mm:ss.SSSZ`); tanggal memakai kalender Gregorian `yyyy-MM-dd`; jumlah uang memakai satuan minor bilangan bulat (`unitPriceMinor`).
- Impor selalu divalidasi dan dipratinjau sebelum dikonfirmasi; catatan dengan ID sama tetapi isi berbeda menolak seluruh impor agar riwayat tidak tertimpa diam-diam.
- `Templates/` dan `docs/` melanjutkan templat dan dokumentasi format dari proyek iOS.

## Pelokalan

Sembilan bahasa disertakan — Tionghoa Sederhana, Tionghoa Tradisional (Taiwan), Kanton Tradisional (Hong Kong), English, 日本語, 한국어, ไทย, Bahasa Indonesia, dan Bahasa Melayu — di `app/src/main/res/values-*/strings.xml`.

`tools/convert_localization.py` membuat ulang sumber daya bahasa repositori ini dari `Localizable.strings` iOS (termasuk konversi `%@` → `%1$s` menjadi argumen berposisi). Jika hanya mengelola sisi Android, edit berkas `strings.xml` terkait secara langsung.

## Struktur repositori

| Jalur | Isi |
| --- | --- |
| `app/src/main/kotlin/com/oshicue/kit/core/` | Model, impor/ekspor JSON, statistik dan peringkat, tata letak garis waktu |
| `app/src/main/kotlin/com/oshicue/kit/data/` | Tiga store independen, penulisan atomik, dan cadangan pemulihan |
| `app/src/main/kotlin/com/oshicue/kit/i18n/` | Resolusi bahasa dan pencarian teks |
| `app/src/main/kotlin/com/oshicue/kit/ui/` | Layar Compose: jadwal, Catatan, Catatan Harian, Pengaturan, dan semua halaman edit／cadangan |
| `app/src/main/res/` | Sumber daya 9 bahasa, ikon peluncur, font Unbounded |
| `app/src/main/assets/` | Templat impor bawaan |
| `app/src/test/` | Pengujian unit (validasi impor, presisi mata uang, seri peringkat, tata letak waktu, penanganan UTF-8, dan lainnya) |
| `tools/` | Skrip konversi sumber daya bahasa |
| `Templates/`, `docs/` | Templat JSON dan dokumentasi format (dilanjutkan dari iOS) |

## Perbedaan dari versi iOS

- Komponen platform: impor/ekspor berkas memakai pemilih berkas sistem, avatar memakai pemilih foto sistem (diperkecil ke 512px, JPEG 0.8, disimpan Base64 — sama seperti iOS), dan berbagi memakai lembar berbagi sistem.
- Aksesibilitas: aksi khusus VoiceOver di iOS dipetakan ke gestur ketuk／ketuk dua kali dan semantik Compose.
- Selebihnya — alur interaksi, kasus tepi, dan format data JSON — sama dengan aplikasi iOS.

## Lisensi

Proyek melanjutkan [MIT License](LICENSE) dari proyek asal; komponen pihak ketiga tetap memakai lisensinya masing-masing. Font Unbounded bawaan memakai [SIL OFL 1.1](THIRD_PARTY_NOTICES); lihat [pemberitahuan pihak ketiga](THIRD_PARTY_NOTICES).

Oshicue Kit asli adalah aplikasi iOS native berbasis SwiftUI (0.4.0, build 10); repositori ini adalah porta Android lengkapnya.
