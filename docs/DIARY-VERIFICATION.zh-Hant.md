# 日誌與全域設定驗證指南

[简体中文](DIARY-VERIFICATION.md) | [繁體中文](DIARY-VERIFICATION.zh-Hant.md) | [繁體粵語](DIARY-VERIFICATION.zh-HK.md) | [English](DIARY-VERIFICATION.en.md) | [日本語](DIARY-VERIFICATION.ja.md) | [한국어](DIARY-VERIFICATION.ko.md) | [ไทย](DIARY-VERIFICATION.th.md) | [Bahasa Indonesia](DIARY-VERIFICATION.id.md) | [Bahasa Melayu](DIARY-VERIFICATION.ms.md)

本頁說明目前儲存庫的測試涵蓋範圍與重現方式，不代表目前環境已通過測試。工程設定為 0.4.0 / build 10，最低 iOS 17；核心 Swift Package 亦支援 macOS 14。實際結果應記錄本次提交、Xcode 版本、執行裝置及測試輸出。

## 核心測試

[DiaryTests.swift](../Tests/DiaryTests.swift) 定義 18 項日誌測試。在專案根目錄執行：

```sh
swift test --filter DiaryTests
```

| 範圍 | 檢查內容 |
| --- | --- |
| 草稿與活動快照 | 空日誌及不完整草稿有效；活動資料獨立；缺少有效活動日期時使用所選日期。 |
| 文字與時間戳記 | 長 Unicode 內文、空白及換行往返保真；毫秒精度去除重複；時區位移表示相同絕對時間。 |
| 合併與衝突 | 相同日誌去除重複，新日誌增補；相同 ID 的內文、摘要、標籤、狀態、地址或修改時間不同時拒絕整批。 |
| 資料驗證 | 重複／空 ID、無效日期及時間順序、版本、必填欄位、欄位型別、時間戳記、無效 JSON 與 UTF-8、20 MiB 限制。 |
| 整理與匯出 | 標籤分隔及去除重複保留日文清濁音與重音差異；多詞跨欄位搜尋；純文字及 Markdown 包含活動資料、摘要及原內文，標題逸出。 |

測試中的重複、衝突、儲存大小限制及匯出行為依據目前程式碼；測試數量或涵蓋範圍改變時應同步更新本頁。

## 原生介面測試

[DiaryUITests.swift](../UITests/DiaryUITests.swift) 定義 5 項測試：

1. 從手帳選擇活動，輸入多行內文、摘要及標籤後進入背景；重新啟動後檢查活動資料及文字，確認只有一篇日誌。
2. 預覽並匯入 JSON；重複匯入去除重複，相同 ID 更改內文時拒絕，重新啟動後原內文保留。
3. 刪除確認中的取消與確認流程；重新啟動後仍維持刪除，手帳活動及消費紀錄保留。
4. 第四個設定頁開啟日誌管理並切換至備份，切換英語後跨模組生效，原日誌內文保留。
5. 九種語言分別檢查日誌標題、新增按鈕、第四個設定頁籤及空日誌狀態。此項不代表所有介面文字均經人工檢查。

先列出本機可用模擬器，再將 `<SIMULATOR_ID>` 替換為實際裝置編號：

```sh
xcrun simctl list devices available
xcodebuild test -project OshicueKit.xcodeproj -scheme OshicueKit -destination 'platform=iOS Simulator,id=<SIMULATOR_ID>' -only-testing:OshicueKitUITests/DiaryUITests
```

測試在 Debug 中透過 `OSHICUE_UI_TESTS=1` 使用獨立日誌檔案 `ui-test-diary.json`；重新啟動保留流程使用 `OSHICUE_PRESERVE_UI_DATA=1`。這些環境變數用於測試隔離，不是使用者備份入口。

## 語言、人工檢查與證據

在 macOS 執行以下指令，檢查九種語言的鍵集合、非空內容、預留位置及 `.strings` 解析；它不判斷翻譯語意或視覺排版：

```sh
python3 scripts/verify-localizations.py
```

人工檢查應包含：自動儲存與失敗重試、選擇活動與快照獨立性、搜尋與篩選、刪除取消／確認、多檔案合併與衝突、空備份拒絕、`.md`／`.txt`／JSON 實際儲存與重新讀取、取消系統分享、九語言長文字與日期顯示。未經授權時不要選擇分享接收對象。

既有 [日誌清單](../screenshots/diary-list.png)、[編輯器](../screenshots/diary-editor.png)、[活動資料](../screenshots/diary-editor-event.png)、[備份預覽](../screenshots/diary-backup-preview.png)、[衝突提示](../screenshots/diary-backup-conflict.png)、[全域設定](../screenshots/global-settings.png) 是儲存庫參考截圖，不能證明目前建置已通過。分享接收、實機安裝、發布流程及自動儲存故障復原不能僅憑核心測試判斷；須依實際執行分別記錄結果。

## 0.4.0 管理、最愛與外觀複測

以下為 0.4.0 / build 10 新增的測試定義與複測範圍，不代表所有 UI 用例本次已執行通過。依實際執行記錄通過、失敗或未執行；色板檢查也不取代各頁面的人工外觀檢查。

| 測試檔案 | 用例數 | 新增涵蓋範圍 |
| --- | --- | --- |
| [DiaryManagementTests.swift](../Tests/DiaryManagementTests.swift) | 4 | 狀態／日期／搜尋組合、無效範圍、所選日誌刪除保留其他內文與活動資料、空日誌備份。 |
| [DataManagementUITests.swift](../UITests/DataManagementUITests.swift) | 6 | 物販篩選多選與取消／確認、篩選變更清空選取、清空所有幣別、日誌多選刪除、設定管理入口仍有備份表單。 多選目錄封存、取消與確認，並保留歷史物販。 |
| [DataManagementFailureUITests.swift](../UITests/DataManagementFailureUITests.swift) | 2 | 在 Debug 的獨立測試資料中以 `OSHICUE_FAIL_BATCH_DELETE=1` 注入實際儲存失敗；檢查原紀錄、日誌內文及目前選取保留，重啟後資料不變，解除故障再選取並重試刪除。 |
| [AppearanceUITests.swift](../UITests/AppearanceUITests.swift) | 5 | 實際 UIKit 動態色板、淺深色文字及選取控制項對比、場館底色、增強對比與成員代表色的可讀性；不操作 App 頁面。 |

```sh
swift test --filter Diary
xcodebuild test -project OshicueKit.xcodeproj -scheme OshicueKit -destination 'platform=iOS Simulator,id=<SIMULATOR_ID>' -only-testing:OshicueKitUITests/DataManagementUITests -only-testing:OshicueKitUITests/AppearanceUITests -only-testing:OshicueKitUITests/DataManagementFailureUITests
```

## 新增人工複測步驟

- 在設定開啟手帳管理與日誌管理，切至備份核對原匯入／匯出；分別組合篩選並選取部分結果，核對確認數量，先取消再確認，重新啟動檢查刪除結果。物販確認須分別列出各幣別；變更篩選必須清空選取。
- 多選封存兼任成員及一個團體，核對刪除範圍、其他身分與歷史編輯；清空物販後目錄與日誌仍在。日誌依狀態、日期、搜尋選擇刪除，核對未選內文與其他模組不受影響。
- 設定最愛並從另一兼任身分重新開啟，核對共用設定、選擇頁頂部專區、各身分價格與新紀錄預設；取消最愛、封存、匯出再匯入後分別核對結果。舊 JSON 缺少 `isFavorite` 時應未最愛，`schemaVersion` 仍為 `1`。
- 在系統切換淺色與深色，逐一檢查四個模組、詳細資料、編輯、選擇、管理及備份；同時檢查大字體、九語言長文字、頭像、代表色、金額、選取按鈕與錯誤提示。記錄測試裝置和系統版本，不以舊截圖取代本次結果。

返回 [README](../README.zh-Hant.md) · 相關：[日誌](DIARY.zh-Hant.md) · [整體驗證](VERIFICATION.zh-Hant.md) · [日誌範本](../Templates/diary-template.json)
