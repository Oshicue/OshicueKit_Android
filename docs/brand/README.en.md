# Oshicue Kit brand assets

[简体中文](README.md) | [繁體中文](README.zh-Hant.md) | [繁體粵語](README.zh-HK.md) | [English](README.en.md) | [日本語](README.ja.md) | [한국어](README.ko.md) | [ไทย](README.th.md) | [Bahasa Indonesia](README.id.md) | [Bahasa Melayu](README.ms.md)

The wordmark follows the Unbounded font in `oshicue.html`, with weight `900` and tracking `0.02em`. `OSHI` and `KIT` use `#f4f3ee`, `CUE` uses the accent `#ff3d6e`, and the icon background uses `#0a0a0d`. The brand name remains `OSHICUE KIT`.

## Assets and app resources

| File | Purpose |
| --- | --- |
| [app-icon.png](app-icon.png) | `1024 × 1024`, opaque, square iOS icon source; the system adds rounded corners. |
| [wordmark.png](wordmark.png) | Complete wordmark on a transparent background, currently `948 × 133`. Its light lettering suits a dark background. |
| [wordmark.svg](wordmark.svg) | Vector wordmark generated from font outlines; no font installation needed. |
| [brand.json](brand.json) | Font path, PostScript names, weight, tracking, colors, and wordmark dimensions. |

The app resource directory [Assets.xcassets](../../OshicueKit/Resources/Assets.xcassets) contains `AppIcon`, `BrandWordmark`, and `AccentColor`. SwiftUI can display the wordmark directly:

```swift
Image("BrandWordmark").resizable().scaledToFit()
```

The bundled font is registered as `Unbounded-Regular`; use `Font.custom("Unbounded-Regular", size: …).weight(.black)`. The generation script explicitly sets the OpenType `wght` axis to `900`. CoreText returns the instance name `Unbounded-Regular_Black`; continue to use `Unbounded-Regular` as the registered font name.

## Regeneration

On macOS with Swift installed, run from the project root:

```sh
CLANG_MODULE_CACHE_PATH=/tmp/oshicue-clang-cache swift scripts/generate_brand.swift
```

[generate_brand.swift](../../scripts/generate_brand.swift) uses AppKit, CoreText, and the bundled [Unbounded.ttf](../../OshicueKit/Resources/Fonts/Unbounded.ttf), without downloading or installing fonts. It overwrites the two PNG files, SVG, and `brand.json` in this directory, and updates `AppIcon.appiconset/AppIcon.png` and `BrandWordmark.imageset/wordmark.png`. It does not generate `AccentColor` or resource `Contents.json` files.

When changing the specifications, update the script and related resources together. After regenerating, check icon dimensions and opacity, the transparent wordmark background, SVG outlines, and metadata. Check the transparent wordmark on a dark background. This page does not claim the assets were regenerated or verified in this run.

## Font license

The bundled Unbounded font includes the [SIL Open Font License 1.1](../../OshicueKit/Resources/Fonts/OFL.txt), with copyright attributed to `The Unbounded Project Authors` (2022). When distributing the font, retain the copyright notice and license and follow the full license's modification, naming, and distribution conditions. The font license does not require documents made with the font to use the same license. This page grants no separate license for the brand name or brand assets.

Back to [README](../../README.en.md) · Related: [Overall verification](../VERIFICATION.en.md) · [Timetable template](../../Templates/timetable-template.json) · [Ledger template](../../Templates/ledger-template.json) · [Diary template](../../Templates/diary-template.json)
