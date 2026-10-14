# 時間表驗證指南

[简体中文](VERIFICATION.md) | [繁體中文](VERIFICATION.zh-Hant.md) | [繁體粵語](VERIFICATION.zh-HK.md) | [English](VERIFICATION.en.md) | [日本語](VERIFICATION.ja.md) | [한국어](VERIFICATION.ko.md) | [ไทย](VERIFICATION.th.md) | [Bahasa Indonesia](VERIFICATION.id.md) | [Bahasa Melayu](VERIFICATION.ms.md)

本頁說明目前原始碼的驗證範圍和複測方法。測試檔案的存在不代表本次已經執行；歷史截圖僅供介面參考，不作為目前建置的通過證據。

## 可重複檢查

在專案根目錄執行：

```sh
swift test
python3 scripts/verify-localizations.py
```

`swift test` 執行 Foundation 核心測試，不執行 iOS 介面測試。本地化檢查涵蓋九種語言的鍵集合、非空文字、格式佔位符和 `.strings` 語法。

| 檔案 | 驗證範圍 |
| --- | --- |
| [ScheduleImporterTests.swift](../Tests/ScheduleImporterTests.swift) | 格式與欄位錯誤、參照及重複 ID、時間偏移、小數秒、跨午夜、日光節約時間、完整活動更新、範本及範例種子。 |
| [TimelineLayoutTests.swift](../Tests/TimelineLayoutTests.swift) | 邊界銜接、重疊分欄、最少並行欄數、穩定排序、密集欄寬及橫向無碰撞。 |
| [ScheduleDeletionTests.swift](../Tests/ScheduleDeletionTests.swift) | 活動刪除、共用收藏、同名不同 ID、未知 ID、空表編碼及重新載入。 |

## 原生介面複測

使用 Xcode 開啟專案，在 iOS 模擬器選取 UI 測試執行。主要案例位於 [OshicueKitUITests.swift](../UITests/OshicueKitUITests.swift)、[TimelineReadabilityUITests.swift](../UITests/TimelineReadabilityUITests.swift) 和 [ScheduleDeletionUITests.swift](../UITests/ScheduleDeletionUITests.swift)。

檢查點一下詳情、連點兩下收藏、我的日程合併類型、無效匯入無法確認、有效匯入取代範例、九語選擇與切換、短場次和並行物販可讀性，以及刪除確認、選擇回退和空表重啟。DEBUG UI 測試以 `OSHICUE_UI_TESTS=1` 使用獨立日程檔案；`OSHICUE_PRESERVE_UI_DATA=1` 用於跨啟動保留測試資料。

## 手動補充

匯入多個檔案，確認錯誤檔案或跨檔案重複 ID 不會部分套用；單獨及同時選取不同時區活動，核對篩選列時區；檢查跨午夜與正好午夜結束；放大系統文字並複測詳情。刪除後核對手帳與日誌快照保留；重新啟動核對日程和收藏。

模擬器檢查不能取代實機、各系統版本、大字體和檔案選擇器的實際驗證。UI 測試、實機安裝與發布應分別記錄本次實際執行結果。現有[截圖目錄](../screenshots/)儲存過去的介面參考。

## 0.4.0 管理、最愛與外觀複測

以下為 0.4.0 / build 10 新增的測試定義與複測範圍，不代表所有 UI 用例本次已執行通過。依實際執行記錄通過、失敗或未執行；色板檢查也不取代各頁面的人工外觀檢查。

| 測試檔案 | 用例數 | 新增涵蓋範圍 |
| --- | --- | --- |
| [DataManagementUITests.swift](../UITests/DataManagementUITests.swift) | 6 | 物販篩選多選與取消／確認、篩選變更清空選取、清空所有幣別、日誌多選刪除、設定管理入口仍有備份表單。 多選目錄封存、取消與確認，並保留歷史物販。 |
| [DataManagementFailureUITests.swift](../UITests/DataManagementFailureUITests.swift) | 2 | 在 Debug 的獨立測試資料中以 `OSHICUE_FAIL_BATCH_DELETE=1` 注入實際儲存失敗；檢查原紀錄、日誌內文及目前選取保留，重啟後資料不變，解除故障再選取並重試刪除。 |
| [AppearanceUITests.swift](../UITests/AppearanceUITests.swift) | 5 | 實際 UIKit 動態色板、淺深色文字及選取控制項對比、場館底色、增強對比與成員代表色的可讀性；不操作 App 頁面。 |
| [AppearanceScreenUITests.swift](../UITests/AppearanceScreenUITests.swift) | 1 | 在系統淺色或深色外觀下實際操作四個模組、成員最愛與選擇、編輯、管理及備份；App 不強制主題，介面斷言後儲存 13 個真實截圖附件。 |

```sh
xcodebuild test -project OshicueKit.xcodeproj -scheme OshicueKit -destination 'platform=iOS Simulator,id=<SIMULATOR_ID>' -only-testing:OshicueKitUITests/AppearanceUITests -only-testing:OshicueKitUITests/DataManagementFailureUITests -only-testing:OshicueKitUITests/AppearanceScreenUITests
```

## 新增人工複測步驟

- 在設定開啟手帳管理與日誌管理，切至備份核對原匯入／匯出；分別組合篩選並選取部分結果，核對確認數量，先取消再確認，重新啟動檢查刪除結果。物販確認須分別列出各幣別；變更篩選必須清空選取。
- 多選封存兼任成員及一個團體，核對刪除範圍、其他身分與歷史編輯；清空物販後目錄與日誌仍在。日誌依狀態、日期、搜尋選擇刪除，核對未選內文與其他模組不受影響。
- 設定最愛並從另一兼任身分重新開啟，核對共用設定、選擇頁頂部專區、各身分價格與新紀錄預設；取消最愛、封存、匯出再匯入後分別核對結果。舊 JSON 缺少 `isFavorite` 時應未最愛，`schemaVersion` 仍為 `1`。
- 在系統切換淺色與深色，逐一檢查四個模組、詳細資料、編輯、選擇、管理及備份；同時檢查大字體、九語言長文字、頭像、代表色、金額、選取按鈕與錯誤提示。記錄測試裝置和系統版本，不以舊截圖取代本次結果。

---

[返回 README](../README.zh-Hant.md) · [匯入格式](IMPORT.zh-Hant.md) · [驗證指南](VERIFICATION.zh-Hant.md) · [顯示與縮放](TIMETABLE-READABILITY.zh-Hant.md) · [刪除日程表](SCHEDULE-DELETION.zh-Hant.md)

範本: [時間表](../Templates/timetable-template.json) · [固定範例](../Templates/demo-timetable.json) · [可讀性複測](../Templates/timetable-readability.json)
