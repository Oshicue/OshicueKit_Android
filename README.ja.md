# Oshicue Kit for Android

[简体中文](README.md) | [繁體中文](README.zh-Hant.md) | [繁體粵語](README.zh-HK.md) | [English](README.en.md) | [日本語](README.ja.md) | [한국어](README.ko.md) | [ไทย](README.th.md) | [Bahasa Indonesia](README.id.md) | [Bahasa Melayu](README.ms.md)

イベントのタイムテーブル、物販の支出、イベントレポを管理するネイティブ Android アプリです（Kotlin + Jetpack Compose）。タブはタイムテーブル、手帳、日記、設定の順に並びます。現在のバージョンは **0.4.0（versionCode 10）** で、iOS 版 Oshicue Kit 0.4.0（build 10）と機能が揃っています。Android 8.0（API 26）以降に対応しています。

## スクリーンショット

| タイムテーブル | マイタイムテーブル | 手帳カレンダー |
| --- | --- | --- |
| ![タイムテーブル](screenshots/timetable.png) | ![マイタイムテーブル](screenshots/my-timetable.png) | ![手帳カレンダー](screenshots/ledger-calendar.png) |

| 物販を記録 | 手帳の統計 | 日記 |
| --- | --- | --- |
| ![物販を記録](screenshots/ledger-record-form.png) | ![手帳の統計](screenshots/ledger-statistics.png) | ![日記](screenshots/diary.png) |

| 日記を作成 | 設定 |
| --- | --- |
| ![日記を作成](screenshots/diary-editor.png) | ![設定](screenshots/settings.png) |

## 機能

### タイムテーブル

- すべての日程は会場と LIVE／物販で分かれます。日付・イベントの複数選択・種類のフィルターは組み合わせて使えます。会場は横に、時間は縦にスクロールします。
- セッションをタップすると詳細を表示、ダブルタップでグループを登録／解除できます。お気に入りは `artistID` 単位でイベント間で共有され、「マイタイムテーブル」はお気に入りグループの LIVE と物販をまとめて表示します。重なりは並べて表示し、時間の重複を通知します。
- 時間軸は 50%、70%、100%、150% のズームに対応（既定は 100%、4dp／分）。重なりが多いときは横に広がり、短いセッションは名前を優先して文字サイズを自動調整します。詳細では完全な情報を確認できます。
- 現在時刻の線は実際のシステム時刻を使い、15 秒ごとに更新されます。「現在」は選択中のイベントに今日の日程がある場合のみ今日へ切り替えます。
- 複数イベントでは、ドキュメント順で最初の選択イベントのタイムゾーンに統一します。詳細では元イベントの現地時刻も確認できます。日付をまたぐセッションは表示タイムゾーンの各日に分割されます。
- 読み込みは JSON の貼り付け、または 1 つ／複数の UTF-8 JSON ファイルに対応。検証とプレビューの後に確認して適用します。同じイベント ID はそのイベント全体を置き換え、同じバッチ内の重複 ID は拒否されます。タイムテーブルの削除は確認が必要で、残るイベントに存在しないアーティストのお気に入りも整理されます。

### 手帳

- カレンダーで日別・月別の枚数と通貨別の金額を確認できます。メンバーカードをタップして物販を編集し、複数ある場合はまず記録を選択します。
- グループ、メンバー、アイコン、グループごとの代表色と既定価格、物販の種類、イベントテンプレートを管理します。兼任メンバーは既存メンバーの関連付けで追加できます。
- 日付、メンバーとグループ、種類、枚数、通貨、単価、任意のイベント情報を記録でき、タイムテーブルのイベント情報も取り込めます。
- ランキングは日／月／年／生涯に対応し、枚数または支出、メンバー合計／メンバー×グループ／グループ、種類とグループで絞り込めます。同点は `1、1、3`。支出は 1 通貨のみで、通貨間の換算は行いません。
- 履歴には名前・色・イベント・価格のスナップショットが保存されます。メンバー／グループの削除はアーカイブ扱いで、履歴と統計は保持され、過去の記録も引き続き編集できます。
- メンバーは「お気に入りメンバー」に設定でき、記録時のメンバー選択では上部に表示されます。金額は整数の最小通貨単位で、`CNY`、`JPY`、`HKD`、`TWD`、`USD`、`KRW`、`THB`、`IDR`、`MYR` に対応します。
- 手帳の管理では、記録の管理（期間・メンバー・グループ・種類・通貨・イベント・テキストの組み合わせで絞り込み、一括削除や全削除）、カタログ整理（メンバー／グループのアーカイブ）、JSON バックアップ（書き出し、プレビュー付き読み込み、競合の拒否）を提供します。

### 日記

- イベントレポを作成し、タイトル、本文、要約、タグを編集、下書き／整理済みを切り替えます。イベント日付での閲覧、状態での絞り込み、本文とイベント情報の検索に対応します。
- 手帳のイベントから日付・名前・都市・会場・住所を取り込み、その後は独立して編集できます。日記は独自のスナップショットを保存します。
- 編集をやめて約 0.5 秒後に自動保存。エディタを閉じるときやバックグラウンド移行時は即座に保存します。失敗時は再試行できます。
- 「公開の準備」で Markdown／プレーンテキストのコピー、`.md`／`.txt` の書き出し、システム共有ができます。
- 日記の管理では、絞り込み、一括削除、JSON バックアップの入出力を提供します。

### 設定とデータ

- 言語の切り替え（9 言語 + システムに従う）は即時反映されます。日付・時刻・数値は選択した地域の形式で表示されます。
- タイムテーブルの読み込み、手帳の管理、日記の管理、サンプルの読み込み、バージョン情報をまとめています。
- 3 つのモジュールは端末内 `files/OshicueKit/` の `schedule.json`、`ledger.json`、`diary.json` に独立して保存され、アトミック書き込みされます。コア機能はオフラインで動作します。

## ビルドと実行

要件：

- JDK 17 以降（Android Studio 同梱の JBR または JDK 21+ を推奨）
- Android SDK Platform 37 と Build-Tools 36（Android Studio で自動インストール可能）
- リポジトリ直下に `local.properties` を作成し `sdk.dir=/path/to/Android/sdk` を記述するか、環境変数 `ANDROID_HOME` を設定
- 初回ビルドには Maven リポジトリへのネットワークアクセスが必要です。Gradle Wrapper が対応バージョンを自動ダウンロードします

```sh
./gradlew :app:assembleDebug        # デバッグ APK をビルド
./gradlew :app:installDebug         # 接続中の端末／エミュレーターへインストール
./gradlew :app:testDebugUnitTest    # 単体テスト 93 件（iOS のテストと 1:1 対応）
./gradlew :app:lintDebug            # 静的解析
./gradlew :app:assembleRelease      # リリース APK をビルド
```

### リリース署名

リポジトリに署名素材は含まれません。配布可能なリリース APK を作るには、リポジトリ直下に `keystore.properties` を作成します（このファイルと `*.jks` は `.gitignore` で除外済み）。

```sh
keytool -genkeypair -v -keystore oshicue-kit-release.jks -alias oshicue-kit \
  -keyalg RSA -keysize 2048 -validity 10000
```

```properties
storeFile=oshicue-kit-release.jks
storePassword=パスワード
keyAlias=oshicue-kit
keyPassword=パスワード
```

その後 `./gradlew :app:assembleRelease` を実行すると `app/build/outputs/apk/release/app-release.apk` が出力されます（`keystore.properties` がない場合は未署名 APK）。`oshicue-kit-release.jks` とパスワードは必ず保管してください。今後のバージョンは同じ署名が必要です。

## データ形式と互換性

- 3 モジュールの JSON 形式は iOS 版と完全に同一です（`schemaVersion` は `1`）。アプリで書き出した／読み込むバックアップは iOS 版と相互に利用でき、端末内のデータファイル構造も同じです。
- タイムスタンプはミリ秒付き ISO 8601（`yyyy-MM-ddTHH:mm:ss.SSSZ`）、日付はグレゴリオ暦の `yyyy-MM-dd`、金額は整数の最小通貨単位（`unitPriceMinor`）です。
- 読み込みは必ず検証とプレビューを経てから適用します。同じ ID で内容が異なる記録はバッチ全体を拒否し、履歴を上書きしません。
- `Templates/` と `docs/` は iOS 版のテンプレートとドキュメントを引き継いでいます。

## ローカライズ

簡体字中国語、繁体字中国語（台湾）、繁体字広東語（香港）、English、日本語、한국어、ไทย、Bahasa Indonesia、Bahasa Melayu の 9 言語を内蔵しています。リソースは `app/src/main/res/values-*/strings.xml` にあります。

`tools/convert_localization.py` は iOS 版の `Localizable.strings` から本リポジトリの言語リソースを再生成するスクリプトです（`%@` → `%1$s` の位置引数変換を含む）。Android 版のみを保守する場合は、対応する `strings.xml` を直接編集してください。

## ディレクトリ構成

| パス | 内容 |
| --- | --- |
| `app/src/main/kotlin/com/oshicue/kit/core/` | モデル、JSON 入出力、統計とランキング、時間軸レイアウト |
| `app/src/main/kotlin/com/oshicue/kit/data/` | 3 つの独立したストア、アトミック書き込みと復旧バックアップ |
| `app/src/main/kotlin/com/oshicue/kit/i18n/` | 言語解決と文字列参照 |
| `app/src/main/kotlin/com/oshicue/kit/ui/` | Compose 画面：タイムテーブル、手帳、日記、設定とすべての編集・バックアップ画面 |
| `app/src/main/res/` | 9 言語のリソース、ランチャーアイコン、Unbounded フォント |
| `app/src/main/assets/` | 同梱の読み込みテンプレート |
| `app/src/test/` | 単体テスト（読み込み検証、通貨の精度、同点ランキング、時間軸レイアウト、UTF-8 処理など） |
| `tools/` | ローカライズ変換スクリプト |
| `Templates/`、`docs/` | JSON テンプレートと形式ドキュメント（iOS 版から引き継ぎ） |

## iOS 版との違い

- プラットフォーム部品：ファイル入出力はシステムのファイルピッカー、アイコンはシステムのフォトピッカー（512px に縮小し JPEG 0.8、Base64 で保存 — iOS と同じ）、共有はシステムの共有シートを使用します。
- アクセシビリティ：iOS の VoiceOver カスタムアクションは、Compose のタップ／ダブルタップ操作とセマンティクスに対応します。
- それ以外の操作フロー、境界条件、JSON データ形式は iOS 版と一致しています。

## ライセンス

本プロジェクトは元プロジェクトの [MIT License](LICENSE) を引き継ぎます。サードパーティコンポーネントはそれぞれのライセンスに従います。同梱の Unbounded フォントは [SIL OFL 1.1](THIRD_PARTY_NOTICES) です。詳細は[サードパーティに関する表示](THIRD_PARTY_NOTICES)をご覧ください。

Oshicue Kit のオリジナルは SwiftUI 製のネイティブ iOS アプリ（0.4.0、build 10）で、本リポジトリはその Android 版への完全移植です。
