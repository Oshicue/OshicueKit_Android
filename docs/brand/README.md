# Oshicue Kit 品牌素材

[简体中文](README.md) | [繁體中文](README.zh-Hant.md) | [繁體粵語](README.zh-HK.md) | [English](README.en.md) | [日本語](README.ja.md) | [한국어](README.ko.md) | [ไทย](README.th.md) | [Bahasa Indonesia](README.id.md) | [Bahasa Melayu](README.ms.md)

品牌字样沿用 `oshicue.html` 的 Unbounded 字体，字重 `900`，字距 `0.02em`。`OSHI` 与 `KIT` 使用 `#f4f3ee`，`CUE` 使用强调色 `#ff3d6e`，图标背景使用 `#0a0a0d`；品牌名称保持 `OSHICUE KIT`。

## 素材与应用资源

| 文件 | 用途 |
| --- | --- |
| [app-icon.png](app-icon.png) | `1024 × 1024`，不透明、方形 iOS 图标源图；圆角由系统添加。 |
| [wordmark.png](wordmark.png) | 透明背景完整字标，当前大小 `948 × 133`。浅色字样适合深色背景。 |
| [wordmark.svg](wordmark.svg) | 由字体轮廓生成的矢量字标，无需安装字体。 |
| [brand.json](brand.json) | 字体路径、PostScript 名称、字重、字距、颜色与字标尺寸记录。 |

应用资源目录 [Assets.xcassets](../../OshicueKit/Resources/Assets.xcassets) 包含 `AppIcon`、`BrandWordmark` 与 `AccentColor`。SwiftUI 可直接显示字标：

```swift
Image("BrandWordmark").resizable().scaledToFit()
```

内置字体注册名为 `Unbounded-Regular`，可使用 `Font.custom("Unbounded-Regular", size: …).weight(.black)`。生成脚本显式将 OpenType `wght` 轴设为 `900`；CoreText 返回该实例名 `Unbounded-Regular_Black`，注册字体仍应使用 `Unbounded-Regular`。

## 重新生成

在装有 Swift 的 macOS 上，从项目根目录执行：

```sh
CLANG_MODULE_CACHE_PATH=/tmp/oshicue-clang-cache swift scripts/generate_brand.swift
```

[generate_brand.swift](../../scripts/generate_brand.swift) 使用 AppKit、CoreText 及内置 [Unbounded.ttf](../../OshicueKit/Resources/Fonts/Unbounded.ttf)，无需下载或安装字体。脚本覆盖本目录的两个 PNG、SVG 和 `brand.json`，同时更新 `AppIcon.appiconset/AppIcon.png` 与 `BrandWordmark.imageset/wordmark.png`；它不生成 `AccentColor` 或资源目录的 `Contents.json`。

修改规范时同步调整脚本和相关资源，重新生成后检查图标尺寸、不透明度、字标透明背景、SVG 轮廓及元数据。透明字标应在深色背景上检查；本页不宣称本次已重新生成或验证素材。

## 字体许可

内置 Unbounded 字体附带 [SIL Open Font License 1.1](../../OshicueKit/Resources/Fonts/OFL.txt)，版权为 `The Unbounded Project Authors`（2022）。分发字体时保留版权声明和该许可，并遵守完整许可中的修改、命名和分发条件。字体许可不要求使用字体制作的文档沿用同一许可；本页不为品牌名称或品牌素材另行授予许可。

返回 [README](../../README.md) · 相关：[总体验证](../VERIFICATION.md) · [时间表模板](../../Templates/timetable-template.json) · [手账模板](../../Templates/ledger-template.json) · [日志模板](../../Templates/diary-template.json)
