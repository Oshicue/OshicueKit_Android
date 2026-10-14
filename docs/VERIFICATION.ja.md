# 時間割の検証ガイド

[简体中文](VERIFICATION.md) | [繁體中文](VERIFICATION.zh-Hant.md) | [繁體粵語](VERIFICATION.zh-HK.md) | [English](VERIFICATION.en.md) | [日本語](VERIFICATION.ja.md) | [한국어](VERIFICATION.ko.md) | [ไทย](VERIFICATION.th.md) | [Bahasa Indonesia](VERIFICATION.id.md) | [Bahasa Melayu](VERIFICATION.ms.md)

現在のソースでの検証範囲と再確認方法を説明します。テストファイルがあるだけでは、今回実行したことにはなりません。過去のスクリーンショットは画面の参考であり、現在のビルドが合格した証拠ではありません。

## 再現可能な確認

プロジェクトのルートで実行します：

```sh
swift test
python3 scripts/verify-localizations.py
```

`swift test` は Foundation のコアテストを実行し、iOS の画面テストは実行しません。ローカライズ確認では 9 言語のキー集合、空でない文字列、書式プレースホルダー、`.strings` の構文を確認します。

| ファイル | 検証範囲 |
| --- | --- |
| [ScheduleImporterTests.swift](../Tests/ScheduleImporterTests.swift) | 形式とフィールドのエラー、参照と重複 ID、時刻オフセット、小数秒、深夜、夏時間、イベント全体の更新、テンプレートと初期サンプル。 |
| [TimelineLayoutTests.swift](../Tests/TimelineLayoutTests.swift) | 接する時間境界、重複の分割、最小の並行レーン数、安定した並び順、密集列の幅、横方向の衝突防止。 |
| [ScheduleDeletionTests.swift](../Tests/ScheduleDeletionTests.swift) | イベント削除、共通のお気に入り、同名で異なる ID、未知の ID、空の文書のエンコードと再読み込み。 |

## ネイティブ画面の再確認

Xcode でプロジェクトを開き、iOS シミュレーターで UI テストを実行します。主なケースは [OshicueKitUITests.swift](../UITests/OshicueKitUITests.swift)、[TimelineReadabilityUITests.swift](../UITests/TimelineReadabilityUITests.swift)、[ScheduleDeletionUITests.swift](../UITests/ScheduleDeletionUITests.swift) にあります。

シングルタップでの詳細、ダブルタップでのお気に入り、自分の時間割での種類統合、無効なインポートの確定防止、有効なインポートによるサンプル置換、9 言語の選択と切り替え、短い枠と同時物販の読みやすさ、削除確認、選択の切り替え、空の時間割での再起動を確認します。DEBUG UI テストでは `OSHICUE_UI_TESTS=1` により専用の日程ファイルを使用し、`OSHICUE_PRESERVE_UI_DATA=1` により再起動後もテストデータを保持します。

## 手動での補足確認

複数ファイルをインポートし、無効なファイルやファイル間のイベント ID 重複が一部だけ適用されないことを確認します。異なるタイムゾーンのイベントを個別および同時に選び、フィルター欄のゾーンを確認します。深夜をまたぐ枠と深夜ちょうどに終了する枠を確認し、システムの文字サイズを大きくして詳細を再確認します。削除後も台帳と日記のスナップショットが残ること、再起動後の日程とお気に入りも確認します。

シミュレーターの確認は、実機、各 OS バージョン、大きな文字、ファイル選択画面の実際の検証を代替しません。UI テスト、実機インストール、公開は今回の実行結果を個別に記録してください。既存の[スクリーンショットディレクトリ](../screenshots/)には過去の画面参考を保存しています。

## 0.4.0 の管理・お気に入り・外観の再テスト

以下は 0.4.0 / build 10 で追加したテスト定義と再確認範囲であり、今回すべての UI テストを実行して合格したという意味ではありません。実行結果に基づき合格、不合格、未実行を記録してください。パレット検査も各画面の目視確認を代替しません。

| テストファイル | 件数 | 追加した範囲 |
| --- | --- | --- |
| [DataManagementUITests.swift](../UITests/DataManagementUITests.swift) | 6 | 購入の絞り込みと複数選択、キャンセル／確認、条件変更による選択解除、全通貨削除、日記一括削除、設定管理内のバックアップ。 資料の複数選択、アーカイブのキャンセル・確認、購入履歴の保持。 |
| [DataManagementFailureUITests.swift](../UITests/DataManagementFailureUITests.swift) | 2 | Debug の独立テストデータで `OSHICUE_FAIL_BATCH_DELETE=1` により実際の保存失敗を注入します。元の記録、日記本文、現在の選択が保持され、再起動後もデータが変わらず、障害を解除して再選択すると削除を再試行できることを確認します。 |
| [AppearanceUITests.swift](../UITests/AppearanceUITests.swift) | 5 | 実際の UIKit 動的パレット、両外観での文字と選択コントロールのコントラスト、会場背景、コントラスト増加、メンバー色の可読性。アプリ画面の操作は行いません。 |
| [AppearanceScreenUITests.swift](../UITests/AppearanceScreenUITests.swift) | 1 | システムのライトまたはダーク外観で、4 機能、メンバーのお気に入りと選択、編集、管理、バックアップを実際に操作します。アプリ側のテーマ強制はなく、画面の検証後に 13 枚の実際のスクリーンショットを添付します。 |

```sh
xcodebuild test -project OshicueKit.xcodeproj -scheme OshicueKit -destination 'platform=iOS Simulator,id=<SIMULATOR_ID>' -only-testing:OshicueKitUITests/AppearanceUITests -only-testing:OshicueKitUITests/DataManagementFailureUITests -only-testing:OshicueKitUITests/AppearanceScreenUITests
```

## 追加の手動再確認手順

- 設定から両管理画面を開き、バックアップの既存読み込み・書き出しを確認します。条件を組み合わせて一部を選択し、確認件数を照合してキャンセル後に削除を確認し、再起動します。購入金額は通貨別に表示し、条件変更で選択を解除する必要があります。
- 兼任メンバーと一つのグループを一括アーカイブし、対象範囲、他所属、過去の編集を確認します。全購入削除後も資料と日記が残ることを確認します。状態・日付・検索で日記を選んで削除し、未選択本文と他機能を確認します。
- お気に入りを設定して他の所属から開き直し、共有設定、選択ページ上部、所属別価格、新規初期選択を確認します。解除、アーカイブ、書き出し、再読み込みも確認します。`isFavorite` のない古い JSON はお気に入り未設定、`schemaVersion` は `1` です。
- システムのライト／ダークを切り替え、4 機能、詳細、編集、選択、管理、バックアップを確認します。大きな文字、9 言語の長いラベル、アイコン、メンバー色、金額、選択ボタン、エラーも確認します。機器と OS バージョンを記録し、古い画像を今回の結果の代わりにしないでください。

---

[README に戻る](../README.ja.md) · [インポート形式](IMPORT.ja.md) · [検証ガイド](VERIFICATION.ja.md) · [表示とズーム](TIMETABLE-READABILITY.ja.md) · [時間割の削除](SCHEDULE-DELETION.ja.md)

テンプレート: [時間割](../Templates/timetable-template.json) · [固定サンプル](../Templates/demo-timetable.json) · [読みやすさの確認](../Templates/timetable-readability.json)
