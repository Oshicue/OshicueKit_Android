# Oshicue Kit 品牌素材

[简体中文](README.md) | [繁體中文](README.zh-Hant.md) | [繁體粵語](README.zh-HK.md) | [English](README.en.md) | [日本語](README.ja.md) | [한국어](README.ko.md) | [ไทย](README.th.md) | [Bahasa Indonesia](README.id.md) | [Bahasa Melayu](README.ms.md)

品牌字樣沿用 `oshicue.html` 的 Unbounded 字型，字重 `900`，字距 `0.02em`。`OSHI` 與 `KIT` 使用 `#f4f3ee`，`CUE` 使用強調色 `#ff3d6e`，圖示背景使用 `#0a0a0d`；品牌名稱保持 `OSHICUE KIT`。

## 素材與應用程式資源

| 檔案 | 用途 |
| --- | --- |
| [app-icon.png](app-icon.png) | `1024 × 1024`，不透明、方形 iOS 圖示原圖；圓角由系統添加。 |
| [wordmark.png](wordmark.png) | 透明背景完整字標，目前大小 `948 × 133`。淺色字樣適合深色背景。 |
| [wordmark.svg](wordmark.svg) | 由字型輪廓產生的向量字標，無需安裝字型。 |
| [brand.json](brand.json) | 字型路徑、PostScript 名稱、字重、字距、色彩與字標尺寸紀錄。 |

應用程式資源目錄 [Assets.xcassets](../../OshicueKit/Resources/Assets.xcassets) 包含 `AppIcon`、`BrandWordmark` 與 `AccentColor`。SwiftUI 可直接顯示字標：

```swift
Image("BrandWordmark").resizable().scaledToFit()
```

內建字型註冊名稱為 `Unbounded-Regular`，可使用 `Font.custom("Unbounded-Regular", size: …).weight(.black)`。產生腳本明確將 OpenType `wght` 軸設為 `900`；CoreText 傳回實例名稱 `Unbounded-Regular_Black`，註冊字型仍應使用 `Unbounded-Regular`。

## 重新產生

在安裝 Swift 的 macOS 上，從專案根目錄執行：

```sh
CLANG_MODULE_CACHE_PATH=/tmp/oshicue-clang-cache swift scripts/generate_brand.swift
```

[generate_brand.swift](../../scripts/generate_brand.swift) 使用 AppKit、CoreText 與內建 [Unbounded.ttf](../../OshicueKit/Resources/Fonts/Unbounded.ttf)，無需下載或安裝字型。腳本覆寫本目錄的兩個 PNG、SVG 與 `brand.json`，同時更新 `AppIcon.appiconset/AppIcon.png` 與 `BrandWordmark.imageset/wordmark.png`；不產生 `AccentColor` 或資源目錄的 `Contents.json`。

修改規範時同步調整腳本和相關資源，重新產生後檢查圖示尺寸、不透明度、字標透明背景、SVG 輪廓及中繼資料。透明字標應在深色背景上檢查；本頁不宣稱本次已重新產生或驗證素材。

## 字型授權

內建 Unbounded 字型附帶 [SIL Open Font License 1.1](../../OshicueKit/Resources/Fonts/OFL.txt)，版權為 `The Unbounded Project Authors`（2022）。散布字型時保留版權聲明與此授權，並遵守完整授權中的修改、命名和散布條件。字型授權不要求使用字型製作的文件沿用相同授權；本頁不為品牌名稱或品牌素材另行授予授權。

返回 [README](../../README.zh-Hant.md) · 相關：[整體驗證](../VERIFICATION.zh-Hant.md) · [時間表範本](../../Templates/timetable-template.json) · [手帳範本](../../Templates/ledger-template.json) · [日誌範本](../../Templates/diary-template.json)
