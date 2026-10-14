# Oshicue Kit 品牌素材

[简体中文](README.md) | [繁體中文](README.zh-Hant.md) | [繁體粵語](README.zh-HK.md) | [English](README.en.md) | [日本語](README.ja.md) | [한국어](README.ko.md) | [ไทย](README.th.md) | [Bahasa Indonesia](README.id.md) | [Bahasa Melayu](README.ms.md)

品牌字樣沿用 `oshicue.html` 嘅 Unbounded 字體，字重 `900`，字距 `0.02em`。`OSHI` 同 `KIT` 用 `#f4f3ee`，`CUE` 用強調色 `#ff3d6e`，圖示背景用 `#0a0a0d`；品牌名稱維持 `OSHICUE KIT`。

## 素材同應用程式資源

| 檔案 | 用途 |
| --- | --- |
| [app-icon.png](app-icon.png) | `1024 × 1024`，不透明、方形 iOS 圖示原圖；系統會加圓角。 |
| [wordmark.png](wordmark.png) | 透明背景完整字標，目前大小 `948 × 133`。淺色字樣適合深色背景。 |
| [wordmark.svg](wordmark.svg) | 由字體輪廓生成嘅向量字標，唔使安裝字體。 |
| [brand.json](brand.json) | 記錄字體路徑、PostScript 名稱、字重、字距、顏色同字標尺寸。 |

應用程式資源目錄 [Assets.xcassets](../../OshicueKit/Resources/Assets.xcassets) 包含 `AppIcon`、`BrandWordmark` 同 `AccentColor`。SwiftUI 可以直接顯示字標：

```swift
Image("BrandWordmark").resizable().scaledToFit()
```

內置字體註冊名係 `Unbounded-Regular`，可以用 `Font.custom("Unbounded-Regular", size: …).weight(.black)`。生成腳本會明確將 OpenType `wght` 軸設為 `900`；CoreText 回傳嘅實例名係 `Unbounded-Regular_Black`，註冊字體仍然應該用 `Unbounded-Regular`。

## 重新生成

喺裝有 Swift 嘅 macOS 上，由專案根目錄執行：

```sh
CLANG_MODULE_CACHE_PATH=/tmp/oshicue-clang-cache swift scripts/generate_brand.swift
```

[generate_brand.swift](../../scripts/generate_brand.swift) 用 AppKit、CoreText 同內置 [Unbounded.ttf](../../OshicueKit/Resources/Fonts/Unbounded.ttf)，唔使下載或者安裝字體。腳本會覆寫本目錄兩個 PNG、SVG 同 `brand.json`，並更新 `AppIcon.appiconset/AppIcon.png` 同 `BrandWordmark.imageset/wordmark.png`；唔會生成 `AccentColor` 或資源目錄嘅 `Contents.json`。

修改規範時要同步調整腳本同相關資源，重新生成後檢查圖示尺寸、不透明度、字標透明背景、SVG 輪廓同中繼資料。透明字標應該喺深色背景上檢查；本頁唔宣稱今次已重新生成或者驗證素材。

## 字體授權

內置 Unbounded 字體附有 [SIL Open Font License 1.1](../../OshicueKit/Resources/Fonts/OFL.txt)，版權屬於 `The Unbounded Project Authors`（2022）。分發字體時要保留版權聲明同呢份授權，並遵守完整授權中嘅修改、命名同分發條件。字體授權唔要求用字體製作嘅文件沿用相同授權；本頁唔會另外授予品牌名稱或者品牌素材嘅使用許可。

返回 [README](../../README.zh-HK.md) · 相關：[整體驗證](../VERIFICATION.zh-HK.md) · [時間表範本](../../Templates/timetable-template.json) · [手帳範本](../../Templates/ledger-template.json) · [日誌範本](../../Templates/diary-template.json)
