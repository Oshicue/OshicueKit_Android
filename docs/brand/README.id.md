# Aset merek Oshicue Kit

[简体中文](README.md) | [繁體中文](README.zh-Hant.md) | [繁體粵語](README.zh-HK.md) | [English](README.en.md) | [日本語](README.ja.md) | [한국어](README.ko.md) | [ไทย](README.th.md) | [Bahasa Indonesia](README.id.md) | [Bahasa Melayu](README.ms.md)

Wordmark mengikuti font Unbounded pada `oshicue.html`, dengan ketebalan `900` dan jarak huruf `0.02em`. `OSHI` dan `KIT` memakai `#f4f3ee`, `CUE` memakai warna aksen `#ff3d6e`, dan latar ikon memakai `#0a0a0d`. Nama merek tetap `OSHICUE KIT`.

## Aset dan sumber daya aplikasi

| File | Kegunaan |
| --- | --- |
| [app-icon.png](app-icon.png) | Sumber ikon iOS persegi tanpa transparansi, `1024 × 1024`; sistem menambahkan sudut membulat. |
| [wordmark.png](wordmark.png) | Wordmark lengkap dengan latar transparan, saat ini `948 × 133`. Huruf terang cocok untuk latar gelap. |
| [wordmark.svg](wordmark.svg) | Wordmark vektor dari kontur font; tidak perlu memasang font. |
| [brand.json](brand.json) | Jalur font, nama PostScript, ketebalan, jarak huruf, warna, dan ukuran wordmark. |

Direktori sumber daya aplikasi [Assets.xcassets](../../OshicueKit/Resources/Assets.xcassets) memuat `AppIcon`, `BrandWordmark`, dan `AccentColor`. SwiftUI bisa menampilkan wordmark langsung:

```swift
Image("BrandWordmark").resizable().scaledToFit()
```

Nama terdaftar font bawaan adalah `Unbounded-Regular`; gunakan `Font.custom("Unbounded-Regular", size: …).weight(.black)`. Skrip pembuat menetapkan sumbu OpenType `wght` ke `900` secara eksplisit. CoreText mengembalikan nama instans `Unbounded-Regular_Black`, tetapi nama font terdaftar tetap harus menggunakan `Unbounded-Regular`.

## Membuat ulang

Pada macOS dengan Swift terpasang, jalankan dari akar proyek:

```sh
CLANG_MODULE_CACHE_PATH=/tmp/oshicue-clang-cache swift scripts/generate_brand.swift
```

[generate_brand.swift](../../scripts/generate_brand.swift) menggunakan AppKit, CoreText, dan [Unbounded.ttf](../../OshicueKit/Resources/Fonts/Unbounded.ttf) bawaan, tanpa mengunduh atau memasang font. Skrip menimpa dua PNG, SVG, dan `brand.json` di direktori ini, serta memperbarui `AppIcon.appiconset/AppIcon.png` dan `BrandWordmark.imageset/wordmark.png`. Skrip tidak membuat `AccentColor` atau file sumber daya `Contents.json`.

Saat mengubah spesifikasi, perbarui skrip dan sumber daya terkait bersama-sama. Setelah dibuat ulang, periksa ukuran dan opasitas ikon, latar transparan wordmark, kontur SVG, dan metadata. Periksa wordmark transparan pada latar gelap. Halaman ini tidak menyatakan aset telah dibuat ulang atau diverifikasi pada pengerjaan ini.

## Lisensi font

Font Unbounded bawaan menyertakan [SIL Open Font License 1.1](../../OshicueKit/Resources/Fonts/OFL.txt), dengan hak cipta `The Unbounded Project Authors` (2022). Saat mendistribusikan font, pertahankan pemberitahuan hak cipta dan lisensi serta ikuti syarat modifikasi, penamaan, dan distribusi dalam lisensi lengkap. Lisensi font tidak mewajibkan dokumen yang dibuat dengan font memakai lisensi yang sama. Halaman ini tidak memberikan lisensi terpisah atas nama merek atau aset merek.

Kembali ke [README](../../README.id.md) · Terkait: [Verifikasi keseluruhan](../VERIFICATION.id.md) · [Templat jadwal](../../Templates/timetable-template.json) · [Templat Catatan](../../Templates/ledger-template.json) · [Templat catatan harian](../../Templates/diary-template.json)
