# Oshicue Kit ブランド素材

[简体中文](README.md) | [繁體中文](README.zh-Hant.md) | [繁體粵語](README.zh-HK.md) | [English](README.en.md) | [日本語](README.ja.md) | [한국어](README.ko.md) | [ไทย](README.th.md) | [Bahasa Indonesia](README.id.md) | [Bahasa Melayu](README.ms.md)

ワードマークには `oshicue.html` と同じ Unbounded フォントを使い、ウェイトは `900`、字間は `0.02em` です。`OSHI` と `KIT` は `#f4f3ee`、`CUE` はアクセントカラー `#ff3d6e`、アイコンの背景は `#0a0a0d` です。ブランド名は `OSHICUE KIT` のまま使用します。

## 素材とアプリのリソース

| ファイル | 用途 |
| --- | --- |
| [app-icon.png](app-icon.png) | `1024 × 1024` の不透明な正方形 iOS アイコン原画像。角丸はシステムが適用します。 |
| [wordmark.png](wordmark.png) | 透明背景の完全なワードマーク。現在のサイズは `948 × 133`。明るい文字色のため暗い背景に適しています。 |
| [wordmark.svg](wordmark.svg) | フォントのアウトラインから生成したベクターワードマーク。フォントのインストールは不要です。 |
| [brand.json](brand.json) | フォントのパス、PostScript 名、ウェイト、字間、色、ワードマークの寸法。 |

アプリのリソースディレクトリ [Assets.xcassets](../../OshicueKit/Resources/Assets.xcassets) には `AppIcon`、`BrandWordmark`、`AccentColor` があります。SwiftUI では直接表示できます。

```swift
Image("BrandWordmark").resizable().scaledToFit()
```

同梱フォントの登録名は `Unbounded-Regular` で、`Font.custom("Unbounded-Regular", size: …).weight(.black)` を使えます。生成スクリプトでは OpenType の `wght` 軸を明示的に `900` に設定しています。CoreText はインスタンス名 `Unbounded-Regular_Black` を返しますが、登録フォント名には引き続き `Unbounded-Regular` を使ってください。

## 再生成

Swift が入った macOS で、プロジェクトのルートから実行します。

```sh
CLANG_MODULE_CACHE_PATH=/tmp/oshicue-clang-cache swift scripts/generate_brand.swift
```

[generate_brand.swift](../../scripts/generate_brand.swift) は AppKit、CoreText、同梱の [Unbounded.ttf](../../OshicueKit/Resources/Fonts/Unbounded.ttf) を使い、フォントのダウンロードやインストールは不要です。このディレクトリの 2 つの PNG、SVG、`brand.json` を上書きし、`AppIcon.appiconset/AppIcon.png` と `BrandWordmark.imageset/wordmark.png` も更新します。`AccentColor` やリソースの `Contents.json` は生成しません。

仕様を変更する際はスクリプトと関連リソースを合わせて更新してください。再生成後はアイコンの寸法と不透明性、ワードマークの透明背景、SVG の輪郭、メタデータを確認します。透明ワードマークは暗い背景で確認してください。このページは今回の再生成や検証の実施を示すものではありません。

## フォントのライセンス

同梱 Unbounded フォントには [SIL Open Font License 1.1](../../OshicueKit/Resources/Fonts/OFL.txt) が付属し、著作権者は `The Unbounded Project Authors`（2022）です。フォントの配布時は著作権表記とライセンスを保持し、全文の改変・命名・配布条件に従ってください。フォントで作成した文書に同じライセンスを使うことは求められていません。このページはブランド名やブランド素材に別のライセンスを与えるものではありません。

[README に戻る](../../README.ja.md) · 関連：[全体の検証](../VERIFICATION.ja.md) · [タイムテーブルテンプレート](../../Templates/timetable-template.json) · [手帳テンプレート](../../Templates/ledger-template.json) · [日記テンプレート](../../Templates/diary-template.json)
