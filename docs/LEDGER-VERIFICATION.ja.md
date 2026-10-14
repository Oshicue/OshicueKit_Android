# 手帳の検証ガイド

[简体中文](LEDGER-VERIFICATION.md) | [繁體中文](LEDGER-VERIFICATION.zh-Hant.md) | [繁體粵語](LEDGER-VERIFICATION.zh-HK.md) | [English](LEDGER-VERIFICATION.en.md) | [日本語](LEDGER-VERIFICATION.ja.md) | [한국어](LEDGER-VERIFICATION.ko.md) | [ไทย](LEDGER-VERIFICATION.th.md) | [Bahasa Indonesia](LEDGER-VERIFICATION.id.md) | [Bahasa Melayu](LEDGER-VERIFICATION.ms.md)

現在のソースコードをもとに、既存テストの範囲と再現可能な受け入れ確認を説明します。テストが存在することは今回の実行成功を意味しません。古いリリースの記録、一時的な結果バンドル、スクリーンショットは現在のバージョンの成功証拠にはなりません。実際の結果は最新の実行ログで確認してください。

## コアテスト

プロジェクトのルートで実行します。

```sh
swift test --filter Ledger
```

[LedgerTests.swift](../Tests/LedgerTests.swift) の 16 件と [LedgerDeletionTests.swift](../Tests/LedgerDeletionTests.swift) の 7 件で、次を確認します。

- サンプルとテンプレートのエンコード往復、日付形式、閏日、タイムゾーン、日・月・年・通算の範囲。
- 日別種類集計、兼任メンバーの合算とグループ別集計、グループ順位、通貨別合計、競技順位、安定した同点順、支出ゼロの割合。
- 最小単位の解析、桁数超過の拒否、ゼロ価格、不正な数量・価格、単一購入と集計のオーバーフロー。
- 名称・カラー・単価のスナップショットと、カタログ更新で履歴が書き換わらないこと。
- ID によるカタログのマージ、購入の重複排除、同じ ID の異なる内容の競合、不正なマージ失敗時に一部だけが反映されないこと。
- 必須フィールド、重複 ID、所属関係と価格の一意性、参照、日付、通貨、カラー、JPEG、ファイルサイズ。
- 古いバックアップのアーカイブフィールド省略、全グループでのメンバーのアーカイブ、グループのアーカイブ、有効な所属関係の抽出、アーカイブの往復、履歴編集、カタログ復元で購入を二重計上しないこと。

## ネイティブ UI テスト

Xcode で `OshicueKit.xcodeproj` を開き、`OshicueKit` scheme と利用可能な iPhone シミュレータを選んで対象のテストクラスを実行します。プロジェクトに必要な Xcode、iOS SDK、署名設定が必要です。UI テストは独立した `ui-test-ledger.json` を使い、プロセスを終了して再起動することで保存を確認します。

| テストファイル | 件数 | 主な範囲 |
| --- | --- | --- |
| [LedgerUITests.swift](../UITests/LedgerUITests.swift) | 6 | 追加・再起動後の保存、メンバー合算／グループ別、支出通貨、不正／有効な読み込み、メンバーの簡単追加と既定価格、9 言語のタイトルとカレンダー入口 |
| [LedgerEditingDeletionUITests.swift](../UITests/LedgerEditingDeletionUITests.swift) | 5 | 単一購入の編集、複数購入から選んで編集、購入削除の確認、全グループでのメンバー削除、グループ削除、履歴編集と再起動後の保存 |

初回のサンプル読み込みと保存方針は [LedgerStore.swift](../OshicueKit/Support/LedgerStore.swift) が実装します。この UI 保存クラスはコアパッケージに含まれません。方針を変更した場合はコアテストに加え、ネイティブ UI と再起動も確認してください。

## 手動確認リスト

1. 独立したテストデータで新しい帳簿を開きます。バッジなしでサンプルが表示され、作成日の数量は 10、通算は 16 になります。編集を保存して再起動しても維持され、既存帳簿にサンプルが自動復活しないことを確認します。
2. 同じメンバーに 2 つ目のグループを追加し、別々のカラーと価格を設定します。合算／グループ別で行のまとまりが変わっても、対象範囲の総数量は同じです。同名で ID が異なるメンバーは別集計になります。
3. 購入を追加・編集し、日付、通貨、既定価格の入力、数量、任意のイベント情報、各購入の総額を確認します。所属関係・種類・通貨を変えると価格が再入力されるため、保存前に実際の単価を確認します。
4. 単一購入のカードは直接編集、複数購入は先に選択となり、保存で選択した記録だけが変わります。削除の取消は合計を維持し、確定は数量と金額を差し引き、再起動後も維持します。
5. 兼任メンバーの削除で全所属関係が非表示になります。グループ削除では他のグループの兼任メンバーを保持します。履歴の数量と金額は不変で編集でき、新規購入には削除済み所属関係を選べません。
6. `CNY` 単価 `80.50`、数量 `2` の合計が `161.00` になることを確認します。`JPY`／`KRW` の小数と、他通貨の小数 2 桁超過はエラーになります。数量は通貨をまたいで数えられ、支出順位は選択通貨のみを使います。
7. [ledger-template.json](../Templates/ledger-template.json) で貼り付け読み込み、プレビュー、書き出しを確認します。未編集サンプルは置換、編集済みサンプルはマージです。再読み込みは二重計上せず、同 ID の異なる購入は読み込み全体を阻止します。不正データで元の帳簿が変わらないことも確認します。
8. 有効なバックアップでアーカイブ済みカタログを復元し、購入が各 1 件のままであることを確認します。編集済み購入に古い内容のバックアップを読み込むと競合になります。
9. アプリの 9 言語を切り替え、手帳、カレンダー、統計、メンバー、編集、削除、読み込みエラーを確認します。長い名称、大きい金額、複数通貨の表示も確認します。
10. テスト端末で実際の写真選択、アイコン削除、システムの複数ファイル選択と保存、バックアップ移行を確認します。自動テストはすべてのシステムダイアログや実写真の操作を網羅しません。

## 証拠の記録

各検証ではコードのリビジョン、実行日時とタイムゾーン、Xcode・OS・端末またはシミュレータのバージョン、コマンドまたはテストクラス、成功・失敗・未実行の項目、保存可能なログや結果バンドルを記録します。`screenshots/ledger-*.png` は UI の参考になりますが、静止画は現在のテスト成功の証拠にはなりません。実機インストール、システムのファイル操作、リリースの受け入れは別途記録してください。

## 0.4.0 の管理・お気に入り・外観の再テスト

以下は 0.4.0 / build 10 で追加したテスト定義と再確認範囲であり、今回すべての UI テストを実行して合格したという意味ではありません。実行結果に基づき合格、不合格、未実行を記録してください。パレット検査も各画面の目視確認を代替しません。

| テストファイル | 件数 | 追加した範囲 |
| --- | --- | --- |
| [LedgerManagementTests.swift](../Tests/LedgerManagementTests.swift) | 6 | 日付境界と複合条件、過去の資料との一致、空結果と無効範囲、選択件数・数量・通貨別金額、一括削除と全購入削除後の資料保持。 |
| [LedgerCatalogManagementTests.swift](../Tests/LedgerCatalogManagementTests.swift) | 3 | メンバー全所属と選択グループのみの一括アーカイブ、履歴保持、空選択・不明 ID・繰り返し操作。 |
| [LedgerFavoritesTests.swift](../Tests/LedgerFavoritesTests.swift) | 8 | 古いバックアップの未設定、JSON 往復と型、マージ、アーカイブ除外、所属別価格、安定順序、新規初期選択と過去の所属。 |
| [DataManagementUITests.swift](../UITests/DataManagementUITests.swift) | 6 | 購入の絞り込みと複数選択、キャンセル／確認、条件変更による選択解除、全通貨削除、日記一括削除、設定管理内のバックアップ。 資料の複数選択、アーカイブのキャンセル・確認、購入履歴の保持。 |
| [DataManagementFailureUITests.swift](../UITests/DataManagementFailureUITests.swift) | 2 | Debug の独立テストデータで `OSHICUE_FAIL_BATCH_DELETE=1` により実際の保存失敗を注入します。元の記録、日記本文、現在の選択が保持され、再起動後もデータが変わらず、障害を解除して再選択すると削除を再試行できることを確認します。 |
| [LedgerFavoriteUITests.swift](../UITests/LedgerFavoriteUITests.swift) | 2 | グループ間で共有するお気に入りと再起動後保持、上部の複数所属ショートカット、優先初期選択と解除。 |
| [AppearanceUITests.swift](../UITests/AppearanceUITests.swift) | 5 | 実際の UIKit 動的パレット、両外観での文字と選択コントロールのコントラスト、会場背景、コントラスト増加、メンバー色の可読性。アプリ画面の操作は行いません。 |
| [AppearanceScreenUITests.swift](../UITests/AppearanceScreenUITests.swift) | 1 | システムのライトまたはダーク外観で、4 機能、メンバーのお気に入りと選択、編集、管理、バックアップを実際に操作します。アプリ側のテーマ強制はなく、画面の検証後に 13 枚の実際のスクリーンショットを添付します。 |

```sh
swift test --filter Ledger
xcodebuild test -project OshicueKit.xcodeproj -scheme OshicueKit -destination 'platform=iOS Simulator,id=<SIMULATOR_ID>' -only-testing:OshicueKitUITests/DataManagementUITests -only-testing:OshicueKitUITests/LedgerFavoriteUITests -only-testing:OshicueKitUITests/AppearanceUITests -only-testing:OshicueKitUITests/DataManagementFailureUITests -only-testing:OshicueKitUITests/AppearanceScreenUITests
```

## 追加の手動再確認手順

- 設定から両管理画面を開き、バックアップの既存読み込み・書き出しを確認します。条件を組み合わせて一部を選択し、確認件数を照合してキャンセル後に削除を確認し、再起動します。購入金額は通貨別に表示し、条件変更で選択を解除する必要があります。
- 兼任メンバーと一つのグループを一括アーカイブし、対象範囲、他所属、過去の編集を確認します。全購入削除後も資料と日記が残ることを確認します。状態・日付・検索で日記を選んで削除し、未選択本文と他機能を確認します。
- お気に入りを設定して他の所属から開き直し、共有設定、選択ページ上部、所属別価格、新規初期選択を確認します。解除、アーカイブ、書き出し、再読み込みも確認します。`isFavorite` のない古い JSON はお気に入り未設定、`schemaVersion` は `1` です。
- システムのライト／ダークを切り替え、4 機能、詳細、編集、選択、管理、バックアップを確認します。大きな文字、9 言語の長いラベル、アイコン、メンバー色、金額、選択ボタン、エラーも確認します。機器と OS バージョンを記録し、古い画像を今回の結果の代わりにしないでください。

[README に戻る](../README.ja.md) · [手帳の使い方とデータ形式](LEDGER.ja.md) · [JSON テンプレート](../Templates/ledger-template.json)
