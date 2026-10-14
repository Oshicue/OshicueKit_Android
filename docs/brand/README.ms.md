# Aset jenama Oshicue Kit

[简体中文](README.md) | [繁體中文](README.zh-Hant.md) | [繁體粵語](README.zh-HK.md) | [English](README.en.md) | [日本語](README.ja.md) | [한국어](README.ko.md) | [ไทย](README.th.md) | [Bahasa Indonesia](README.id.md) | [Bahasa Melayu](README.ms.md)

Tanda perkataan menggunakan fon Unbounded seperti dalam `oshicue.html`, dengan ketebalan `900` dan jarak huruf `0.02em`. `OSHI` dan `KIT` menggunakan `#f4f3ee`, `CUE` menggunakan warna aksen `#ff3d6e`, dan latar ikon menggunakan `#0a0a0d`. Nama jenama kekal `OSHICUE KIT`.

## Aset dan sumber aplikasi

| Fail | Kegunaan |
| --- | --- |
| [app-icon.png](app-icon.png) | Sumber ikon iOS segi empat sama legap, `1024 × 1024`; sistem menambah bucu bulat. |
| [wordmark.png](wordmark.png) | Tanda perkataan lengkap berlatar lutsinar, kini `948 × 133`. Huruf cerah sesuai untuk latar gelap. |
| [wordmark.svg](wordmark.svg) | Tanda perkataan vektor daripada garis luar fon; tidak perlu memasang fon. |
| [brand.json](brand.json) | Laluan fon, nama PostScript, ketebalan, jarak huruf, warna dan dimensi tanda perkataan. |

Direktori sumber aplikasi [Assets.xcassets](../../OshicueKit/Resources/Assets.xcassets) mengandungi `AppIcon`, `BrandWordmark` dan `AccentColor`. SwiftUI boleh memaparkan tanda perkataan secara terus:

```swift
Image("BrandWordmark").resizable().scaledToFit()
```

Nama berdaftar fon terbina ialah `Unbounded-Regular`; gunakan `Font.custom("Unbounded-Regular", size: …).weight(.black)`. Skrip penjana menetapkan paksi OpenType `wght` kepada `900` secara jelas. CoreText memulangkan nama tika `Unbounded-Regular_Black`, tetapi nama fon berdaftar masih perlu menggunakan `Unbounded-Regular`.

## Jana semula

Pada macOS dengan Swift dipasang, jalankan dari akar projek:

```sh
CLANG_MODULE_CACHE_PATH=/tmp/oshicue-clang-cache swift scripts/generate_brand.swift
```

[generate_brand.swift](../../scripts/generate_brand.swift) menggunakan AppKit, CoreText dan [Unbounded.ttf](../../OshicueKit/Resources/Fonts/Unbounded.ttf) terbina, tanpa memuat turun atau memasang fon. Skrip menulis ganti dua PNG, SVG dan `brand.json` dalam direktori ini serta mengemas kini `AppIcon.appiconset/AppIcon.png` dan `BrandWordmark.imageset/wordmark.png`. Ia tidak menjana `AccentColor` atau fail sumber `Contents.json`.

Apabila spesifikasi berubah, kemas kini skrip dan sumber berkaitan bersama-sama. Selepas menjana semula, semak dimensi dan kelegapan ikon, latar lutsinar tanda perkataan, garis luar SVG dan metadata. Semak tanda perkataan lutsinar pada latar gelap. Halaman ini tidak menyatakan aset sudah dijana semula atau disahkan dalam tugasan ini.

## Lesen fon

Fon Unbounded terbina menyertakan [SIL Open Font License 1.1](../../OshicueKit/Resources/Fonts/OFL.txt), dengan hak cipta `The Unbounded Project Authors` (2022). Semasa mengedarkan fon, kekalkan notis hak cipta dan lesen serta patuhi syarat pengubahsuaian, penamaan dan pengedaran dalam lesen penuh. Lesen fon tidak mewajibkan dokumen yang dibuat dengan fon menggunakan lesen yang sama. Halaman ini tidak memberikan lesen berasingan untuk nama jenama atau aset jenama.

Kembali ke [README](../../README.ms.md) · Berkaitan: [Pengesahan keseluruhan](../VERIFICATION.ms.md) · [Templat jadual waktu](../../Templates/timetable-template.json) · [Templat Catatan](../../Templates/ledger-template.json) · [Templat diari](../../Templates/diary-template.json)
