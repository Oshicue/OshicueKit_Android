# 日誌同全域設定驗證指南

[简体中文](DIARY-VERIFICATION.md) | [繁體中文](DIARY-VERIFICATION.zh-Hant.md) | [繁體粵語](DIARY-VERIFICATION.zh-HK.md) | [English](DIARY-VERIFICATION.en.md) | [日本語](DIARY-VERIFICATION.ja.md) | [한국어](DIARY-VERIFICATION.ko.md) | [ไทย](DIARY-VERIFICATION.th.md) | [Bahasa Indonesia](DIARY-VERIFICATION.id.md) | [Bahasa Melayu](DIARY-VERIFICATION.ms.md)

本頁講解而家儲存庫嘅測試覆蓋同重現方法，唔代表目前環境已經通過測試。工程設定係 0.4.0 / build 10，最低 iOS 17；核心 Swift Package 亦支援 macOS 14。實際結果應該記錄今次提交、Xcode 版本、執行裝置同測試輸出。

## 核心測試

[DiaryTests.swift](../Tests/DiaryTests.swift) 定義咗 18 項日誌測試。喺專案根目錄執行：

```sh
swift test --filter DiaryTests
```

| 覆蓋範圍 | 檢查內容 |
| --- | --- |
| 草稿同活動快照 | 空日誌同未完整草稿有效；活動資料獨立；冇有效活動日期就用已選日期。 |
| 文字同時間戳 | 長 Unicode 正文、空白同換行往返保真；毫秒精度去除重複；時區偏移代表同一絕對時間。 |
| 合併同衝突 | 相同日誌去除重複，新增日誌增補；同 ID 嘅正文、摘要、標籤、狀態、地址或者修改時間唔同，就拒絕整批。 |
| 資料驗證 | 重複／空 ID、無效日期同時間順序、版本、必填欄位、欄位類型、時間戳、無效 JSON 同 UTF-8、20 MiB 限制。 |
| 整理同匯出 | 標籤分隔同去除重複保留日文清濁音同重音差異；多詞跨欄位搜尋；純文字同 Markdown 包含活動資料、摘要同原正文，標題逸出。 |

測試中嘅重複、衝突、儲存大小限制同匯出行為係按目前程式碼；測試數量或覆蓋範圍改變時要同步更新本頁。

## 原生介面測試

[DiaryUITests.swift](../UITests/DiaryUITests.swift) 定義咗 5 項測試：

1. 由手帳揀活動，輸入多行正文、摘要同標籤之後進入背景；重開後檢查活動資料同文字，確認得一篇日誌。
2. 預覽同匯入 JSON；重複匯入會去除重複，同 ID 改正文會拒絕，重開後原正文保留。
3. 刪除確認嘅取消同確認流程；重開後仍然已刪除，手帳活動同消費記錄保留。
4. 第四個設定頁開啟日誌管理再轉去備份，轉英語之後跨模組生效，原日誌正文保留。
5. 九種語言分別檢查日誌標題、新增按鈕、第四個設定頁籤同空日誌狀態。呢項唔代表所有介面文字都經人工檢查。

先列出本機可用模擬器，再用實際裝置編號取代 `<SIMULATOR_ID>`：

```sh
xcrun simctl list devices available
xcodebuild test -project OshicueKit.xcodeproj -scheme OshicueKit -destination 'platform=iOS Simulator,id=<SIMULATOR_ID>' -only-testing:OshicueKitUITests/DiaryUITests
```

測試喺 Debug 中透過 `OSHICUE_UI_TESTS=1` 使用獨立日誌檔案 `ui-test-diary.json`；重開保留流程用 `OSHICUE_PRESERVE_UI_DATA=1`。呢啲環境變數用嚟隔離測試，唔係用戶備份入口。

## 語言、人工檢查同證據

喺 macOS 執行以下指令，檢查九種語言嘅鍵集合、非空內容、佔位符同 `.strings` 解析；佢唔會判斷翻譯語意或者視覺排版：

```sh
python3 scripts/verify-localizations.py
```

人工檢查應該包括：自動儲存同失敗重試、選擇活動同快照獨立性、搜尋同篩選、刪除取消／確認、多個檔案合併同衝突、拒絕空備份、`.md`／`.txt`／JSON 實際儲存同再讀取、取消系統分享、九語言長文字同日期顯示。未獲授權就唔好揀分享接收對象。

現有 [日誌列表](../screenshots/diary-list.png)、[編輯器](../screenshots/diary-editor.png)、[活動資料](../screenshots/diary-editor-event.png)、[備份預覽](../screenshots/diary-backup-preview.png)、[衝突提示](../screenshots/diary-backup-conflict.png)、[全域設定](../screenshots/global-settings.png) 係儲存庫參考截圖，唔能夠證明目前建置已通過。分享接收、實機安裝、發布流程同自動儲存故障復原，唔可以單憑核心測試判斷；要按實際執行分開記錄結果。

## 0.4.0 管理、最愛同外觀重測

以下係 0.4.0 / build 10 新增嘅測試定義同重測範圍，唔代表所有 UI 用例今次已經執行通過。按實際執行記錄通過、失敗或者未執行；色板檢查亦唔可以代替各頁面嘅人工外觀檢查。

| 測試檔案 | 用例數 | 新增範圍 |
| --- | --- | --- |
| [DiaryManagementTests.swift](../Tests/DiaryManagementTests.swift) | 4 | 狀態／日期／搜尋組合、無效範圍、所選日誌刪除保留其他正文同活動資料、空日誌備份。 |
| [DataManagementUITests.swift](../UITests/DataManagementUITests.swift) | 6 | 物販篩選多選同取消／確認、改篩選清選取、清空所有貨幣、日誌多選刪除、設定管理入口仍有備份表單。 多選目錄封存、取消同確認，並保留歷史物販。 |
| [DataManagementFailureUITests.swift](../UITests/DataManagementFailureUITests.swift) | 2 | 喺 Debug 獨立測試資料用 `OSHICUE_FAIL_BATCH_DELETE=1` 注入實際儲存失敗；檢查原紀錄、日誌正文同目前選取保留，重啟後資料不變，解除故障再選取同重試刪除。 |
| [AppearanceUITests.swift](../UITests/AppearanceUITests.swift) | 5 | 實際 UIKit 動態色板、淺深色文字同選取控制項對比、場館底色、增強對比同成員代表色可讀性；唔操作 App 頁面。 |

```sh
swift test --filter Diary
xcodebuild test -project OshicueKit.xcodeproj -scheme OshicueKit -destination 'platform=iOS Simulator,id=<SIMULATOR_ID>' -only-testing:OshicueKitUITests/DataManagementUITests -only-testing:OshicueKitUITests/AppearanceUITests -only-testing:OshicueKitUITests/DataManagementFailureUITests
```

## 新增人工重測步驟

- 喺設定開啟手帳管理同日誌管理，轉去備份核對原有匯入／匯出；分別組合篩選同選取部分結果，核對確認數量，先取消再確認，重啟檢查刪除結果。物販確認要分別列出各貨幣；改篩選必須清空選取。
- 多選封存兼任成員同一個團體，核對刪除範圍、其他身份同歷史編輯；清空物販後目錄同日誌仍在。日誌按狀態、日期、搜尋選刪，核對未選正文同其他模組冇受影響。
- 設定最愛，再由另一兼任身份重新開啟，核對共用設定、選擇頁頂部專區、各身份價格同新紀錄預設；取消最愛、封存、匯出再匯入後分別核對結果。舊 JSON 冇 `isFavorite` 應係未最愛，`schemaVersion` 仍然係 `1`。
- 喺系統切換淺色同深色，逐一檢查四個模組、詳情、編輯、選擇、管理同備份；一併檢查大字體、九語言長文字、頭像、代表色、金額、選取按鈕同錯誤提示。記錄測試裝置同系統版本，唔用舊截圖代替今次結果。

返回 [README](../README.zh-HK.md) · 相關：[日誌](DIARY.zh-HK.md) · [整體驗證](VERIFICATION.zh-HK.md) · [日誌範本](../Templates/diary-template.json)
