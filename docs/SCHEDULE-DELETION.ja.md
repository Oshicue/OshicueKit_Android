# 時間割の削除

[简体中文](SCHEDULE-DELETION.md) | [繁體中文](SCHEDULE-DELETION.zh-Hant.md) | [繁體粵語](SCHEDULE-DELETION.zh-HK.md) | [English](SCHEDULE-DELETION.en.md) | [日本語](SCHEDULE-DELETION.ja.md) | [한국어](SCHEDULE-DELETION.ko.md) | [ไทย](SCHEDULE-DELETION.th.md) | [Bahasa Indonesia](SCHEDULE-DELETION.id.md) | [Bahasa Melayu](SCHEDULE-DELETION.ms.md)

## 操作

時間割からイベント選択画面を開きます。各時間割に赤い削除ボタンとごみ箱アイコンがあります。タップすると対象名を含む確認が表示されます。キャンセルはデータを保持し、確定するとそのイベントとすべての LIVE・物販の枠を削除します。イベントの選択と削除は別の操作です。

削除はローカルファイルへの書き込みに成功してから画面を更新します。保存に失敗するとエラーを表示し、元の日程を保持するため再試行できます。未選択イベントの削除では他の選択は変わりません。唯一選択していたイベントを削除すると、残るイベントを自動選択し、日付も調整します。

## お気に入りと独立したデータ

他のイベントの `artists` 配列でまだ定義されているグループのお気に入りのみ保持します。表示名や枠の有無では判定しません。同名でも ID が異なるグループはお気に入りを共有しません。

台帳と日記は独立したデータとイベントのスナップショットを保存します。時間割の削除で購入記録、統計、日記の下書きは削除しません。すべてのイベントを削除すると空の時間割を保存し、再起動しても空のままです。再度インポートするか、設定から明示的にサンプルを読み込めます。サンプル読み込みはサンプルのイベント ID で統合し、他の ID のイベントを保持します。

## 再確認

1. 共通のグループ ID が 1 つあり、それぞれ固有のグループもあるイベントを 2 つインポートします。共通グループと削除予定イベントの固有グループをお気に入りにします。
2. 削除確認の名前を確認して先にキャンセルし、イベント、枠、選択が残ることを確認します。
3. 削除を確定し、対象の枠が消え、共通のお気に入りは残り、削除したイベントだけで定義されていたお気に入りが解除されることを確認します。残りの選択と日付も確認します。
4. 未選択イベントを削除し、他の選択が残ることと、台帳・日記が影響を受けないことを確認します。
5. 全件削除後に再起動し、サンプルが復元されないことを確認します。再インポートとお気に入り登録で通常使用できることを確認します。

コア確認は [ScheduleDeletionTests.swift](../Tests/ScheduleDeletionTests.swift)、ネイティブ操作確認は [ScheduleDeletionUITests.swift](../UITests/ScheduleDeletionUITests.swift) にあります。今回 UI テストは再実行していません。

---

[README に戻る](../README.ja.md) · [インポート形式](IMPORT.ja.md) · [検証ガイド](VERIFICATION.ja.md) · [表示とズーム](TIMETABLE-READABILITY.ja.md) · [時間割の削除](SCHEDULE-DELETION.ja.md)

テンプレート: [時間割](../Templates/timetable-template.json) · [固定サンプル](../Templates/demo-timetable.json) · [読みやすさの確認](../Templates/timetable-readability.json)
