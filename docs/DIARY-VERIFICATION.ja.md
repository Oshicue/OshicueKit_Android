# 日記と全体設定の検証ガイド

[简体中文](DIARY-VERIFICATION.md) | [繁體中文](DIARY-VERIFICATION.zh-Hant.md) | [繁體粵語](DIARY-VERIFICATION.zh-HK.md) | [English](DIARY-VERIFICATION.en.md) | [日本語](DIARY-VERIFICATION.ja.md) | [한국어](DIARY-VERIFICATION.ko.md) | [ไทย](DIARY-VERIFICATION.th.md) | [Bahasa Indonesia](DIARY-VERIFICATION.id.md) | [Bahasa Melayu](DIARY-VERIFICATION.ms.md)

このページは現在のリポジトリのテスト範囲と再現手順を説明するもので、現在の環境でテストに合格したことを示すものではありません。プロジェクト設定は 0.4.0 / build 10、最低対応は iOS 17 です。コアの Swift Package は macOS 14 にも対応しています。実際の結果にはコミット、Xcode のバージョン、実行端末、テスト出力を記録してください。

## コアテスト

[DiaryTests.swift](../Tests/DiaryTests.swift) に 18 件の日記テストが定義されています。プロジェクトのルートで実行します。

```sh
swift test --filter DiaryTests
```

| 対象 | 確認内容 |
| --- | --- |
| 下書きとイベントスナップショット | 空の日記と未完成の下書きが有効であること、イベント情報が独立していること、有効なイベント日がなければ選択中の日付を使うこと。 |
| テキストとタイムスタンプ | 長い Unicode 本文、空白、改行の往復保持、ミリ秒精度の重複判定、タイムゾーンオフセットによる同じ絶対時刻の表現。 |
| 統合と競合 | 同一日記の重複除去と新規追加、同じ ID の本文、要約、タグ、状態、住所、更新時刻の違いによる全件拒否。 |
| データ検証 | 重複・空の ID、無効な日付と時刻の順序、バージョン、必須項目、フィールド型、タイムスタンプ、無効な JSON と UTF-8、20 MiB 制限。 |
| 整理と書き出し | 清音・濁音やアクセントの違いを残すタグ分割と重複除去、複数語の項目横断検索、イベント情報・要約・元本文を含むテキストと Markdown、タイトルのエスケープ。 |

重複、競合、保存容量、書き出しに関するテストは現在のコードに基づきます。テスト数や範囲が変わった場合はこのページも更新してください。

## ネイティブ UI テスト

[DiaryUITests.swift](../UITests/DiaryUITests.swift) に 5 件のテストが定義されています。

1. 手帳イベントを選び、複数行の本文、要約、タグを入力してバックグラウンドへ移行。再起動後にイベント情報と文字を確認し、日記が 1 件だけであることを確認します。
2. JSON をプレビューしてインポート。再インポートで重複を除去し、同じ ID の本文変更を拒否し、再起動後も元の本文を保持します。
3. 削除確認のキャンセルと確定。再起動後も削除されたままで、手帳イベントと購入記録は残ります。
4. 4 番目の設定タブから「日記の管理」を開いて「バックアップ」を選び、英語への切り替えを各機能に適用し、元の日記本文を保持します。
5. 9 言語ごとに日記タイトル、新規作成ボタン、4 番目の設定タブ、空の日記状態を確認します。すべての UI 文言の手動確認を意味するものではありません。

利用可能なローカルシミュレーターを一覧表示し、`<SIMULATOR_ID>` を実際の端末 ID に置き換えます。

```sh
xcrun simctl list devices available
xcodebuild test -project OshicueKit.xcodeproj -scheme OshicueKit -destination 'platform=iOS Simulator,id=<SIMULATOR_ID>' -only-testing:OshicueKitUITests/DiaryUITests
```

Debug でのテストは `OSHICUE_UI_TESTS=1` により独立した `ui-test-diary.json` を使用します。再起動での保持は `OSHICUE_PRESERVE_UI_DATA=1` を使用します。これらはテスト分離用の環境変数で、利用者のバックアップ操作ではありません。

## 言語確認、手動確認、証拠

macOS で次のスクリプトを実行すると、9 言語のキー集合、空でない値、プレースホルダー、`.strings` の解析を確認できます。翻訳の意味や見た目のレイアウトは判定しません。

```sh
python3 scripts/verify-localizations.py
```

手動確認には、自動保存と失敗時の再試行、イベント選択と独立したスナップショット、検索と絞り込み、削除のキャンセル・確定、複数ファイルの統合と競合、空バックアップの拒否、`.md`／`.txt`／JSON の実際の保存と再読込、システム共有のキャンセル、9 言語の長文と日付表示を含めてください。許可なく共有先を選ばないでください。

既存の [日記一覧](../screenshots/diary-list.png)、[エディター](../screenshots/diary-editor.png)、[イベント情報](../screenshots/diary-editor-event.png)、[バックアッププレビュー](../screenshots/diary-backup-preview.png)、[競合表示](../screenshots/diary-backup-conflict.png)、[全体設定](../screenshots/global-settings.png) は参考画像であり、現在のビルドの合格を証明しません。共有先への送信、実機インストール、公開手順、自動保存の障害復旧はコアテストだけでは判断できません。実行したときに結果を別途記録してください。

## 0.4.0 の管理・お気に入り・外観の再テスト

以下は 0.4.0 / build 10 で追加したテスト定義と再確認範囲であり、今回すべての UI テストを実行して合格したという意味ではありません。実行結果に基づき合格、不合格、未実行を記録してください。パレット検査も各画面の目視確認を代替しません。

| テストファイル | 件数 | 追加した範囲 |
| --- | --- | --- |
| [DiaryManagementTests.swift](../Tests/DiaryManagementTests.swift) | 4 | 状態・日付・検索の組合せ、無効範囲、削除後の他本文とイベント情報保持、空の日記バックアップ。 |
| [DataManagementUITests.swift](../UITests/DataManagementUITests.swift) | 6 | 購入の絞り込みと複数選択、キャンセル／確認、条件変更による選択解除、全通貨削除、日記一括削除、設定管理内のバックアップ。 資料の複数選択、アーカイブのキャンセル・確認、購入履歴の保持。 |
| [DataManagementFailureUITests.swift](../UITests/DataManagementFailureUITests.swift) | 2 | Debug の独立テストデータで `OSHICUE_FAIL_BATCH_DELETE=1` により実際の保存失敗を注入します。元の記録、日記本文、現在の選択が保持され、再起動後もデータが変わらず、障害を解除して再選択すると削除を再試行できることを確認します。 |
| [AppearanceUITests.swift](../UITests/AppearanceUITests.swift) | 5 | 実際の UIKit 動的パレット、両外観での文字と選択コントロールのコントラスト、会場背景、コントラスト増加、メンバー色の可読性。アプリ画面の操作は行いません。 |

```sh
swift test --filter Diary
xcodebuild test -project OshicueKit.xcodeproj -scheme OshicueKit -destination 'platform=iOS Simulator,id=<SIMULATOR_ID>' -only-testing:OshicueKitUITests/DataManagementUITests -only-testing:OshicueKitUITests/AppearanceUITests -only-testing:OshicueKitUITests/DataManagementFailureUITests
```

## 追加の手動再確認手順

- 設定から両管理画面を開き、バックアップの既存読み込み・書き出しを確認します。条件を組み合わせて一部を選択し、確認件数を照合してキャンセル後に削除を確認し、再起動します。購入金額は通貨別に表示し、条件変更で選択を解除する必要があります。
- 兼任メンバーと一つのグループを一括アーカイブし、対象範囲、他所属、過去の編集を確認します。全購入削除後も資料と日記が残ることを確認します。状態・日付・検索で日記を選んで削除し、未選択本文と他機能を確認します。
- お気に入りを設定して他の所属から開き直し、共有設定、選択ページ上部、所属別価格、新規初期選択を確認します。解除、アーカイブ、書き出し、再読み込みも確認します。`isFavorite` のない古い JSON はお気に入り未設定、`schemaVersion` は `1` です。
- システムのライト／ダークを切り替え、4 機能、詳細、編集、選択、管理、バックアップを確認します。大きな文字、9 言語の長いラベル、アイコン、メンバー色、金額、選択ボタン、エラーも確認します。機器と OS バージョンを記録し、古い画像を今回の結果の代わりにしないでください。

[README に戻る](../README.ja.md) · 関連：[日記](DIARY.ja.md) · [全体の検証](VERIFICATION.ja.md) · [日記テンプレート](../Templates/diary-template.json)
