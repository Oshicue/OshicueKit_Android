# 時間表驗證指南

[简体中文](VERIFICATION.md) | [繁體中文](VERIFICATION.zh-Hant.md) | [繁體粵語](VERIFICATION.zh-HK.md) | [English](VERIFICATION.en.md) | [日本語](VERIFICATION.ja.md) | [한국어](VERIFICATION.ko.md) | [ไทย](VERIFICATION.th.md) | [Bahasa Indonesia](VERIFICATION.id.md) | [Bahasa Melayu](VERIFICATION.ms.md)

本頁交代目前原始碼嘅驗證範圍同重測方法。有測試檔案唔代表今次已經執行；舊截圖只供介面參考，唔當作目前版本嘅通過證據。

## 可重複檢查

喺工程根目錄執行：

```sh
swift test
python3 scripts/verify-localizations.py
```

`swift test` 會執行 Foundation 核心測試，唔會執行 iOS 介面測試。本地化檢查涵蓋九種語言嘅鍵集合、非空文字、格式佔位符同 `.strings` 語法。

| 檔案 | 驗證範圍 |
| --- | --- |
| [ScheduleImporterTests.swift](../Tests/ScheduleImporterTests.swift) | 格式同欄位錯誤、引用同重複 ID、時間偏移、小數秒、跨午夜、夏令時間、完整活動更新、範本同示例種子。 |
| [TimelineLayoutTests.swift](../Tests/TimelineLayoutTests.swift) | 邊界銜接、重疊分欄、最少並行欄數、穩定排序、密集欄寬同橫向冇碰撞。 |
| [ScheduleDeletionTests.swift](../Tests/ScheduleDeletionTests.swift) | 活動刪除、共用收藏、同名唔同 ID、未知 ID、空表編碼同重新載入。 |

## 原生介面重測

用 Xcode 打開工程，喺 iOS 模擬器揀 UI 測試執行。主要案例喺 [OshicueKitUITests.swift](../UITests/OshicueKitUITests.swift)、[TimelineReadabilityUITests.swift](../UITests/TimelineReadabilityUITests.swift) 同 [ScheduleDeletionUITests.swift](../UITests/ScheduleDeletionUITests.swift)。

檢查撳一下詳情、連撳兩下收藏、我的日程合併類型、無效匯入唔能夠確認、有效匯入取代示例、九語選擇同切換、短場次同並行物販可讀性，以及刪除確認、選擇回退同空表重啟。DEBUG UI 測試以 `OSHICUE_UI_TESTS=1` 使用獨立日程檔案；`OSHICUE_PRESERVE_UI_DATA=1` 用嚟喺重新啟動後保留測試資料。

## 手動補充

匯入多個檔案，確認有問題嘅檔案或跨檔案重複 ID 唔會部分套用；單獨同同時揀唔同時區活動，核對篩選列時區；檢查跨午夜同啱啱午夜結束；放大系統文字再重測詳情。刪除後核對手帳同日誌快照保留；重新啟動核對日程同收藏。

模擬器檢查唔可以代替真機、各系統版本、大字體同檔案選擇器嘅實際驗證。UI 測試、真機安裝同發布要分別記錄今次實際執行結果。現有[截圖目錄](../screenshots/)保存咗過去嘅介面參考。

## 0.4.0 管理、最愛同外觀重測

以下係 0.4.0 / build 10 新增嘅測試定義同重測範圍，唔代表所有 UI 用例今次已經執行通過。按實際執行記錄通過、失敗或者未執行；色板檢查亦唔可以代替各頁面嘅人工外觀檢查。

| 測試檔案 | 用例數 | 新增範圍 |
| --- | --- | --- |
| [DataManagementUITests.swift](../UITests/DataManagementUITests.swift) | 6 | 物販篩選多選同取消／確認、改篩選清選取、清空所有貨幣、日誌多選刪除、設定管理入口仍有備份表單。 多選目錄封存、取消同確認，並保留歷史物販。 |
| [DataManagementFailureUITests.swift](../UITests/DataManagementFailureUITests.swift) | 2 | 喺 Debug 獨立測試資料用 `OSHICUE_FAIL_BATCH_DELETE=1` 注入實際儲存失敗；檢查原紀錄、日誌正文同目前選取保留，重啟後資料不變，解除故障再選取同重試刪除。 |
| [AppearanceUITests.swift](../UITests/AppearanceUITests.swift) | 5 | 實際 UIKit 動態色板、淺深色文字同選取控制項對比、場館底色、增強對比同成員代表色可讀性；唔操作 App 頁面。 |
| [AppearanceScreenUITests.swift](../UITests/AppearanceScreenUITests.swift) | 1 | 喺系統淺色或者深色外觀下實際操作四個模組、成員最愛同選擇、編輯、管理同備份；App 唔強制主題，介面斷言後保存 13 個真實截圖附件。 |

```sh
xcodebuild test -project OshicueKit.xcodeproj -scheme OshicueKit -destination 'platform=iOS Simulator,id=<SIMULATOR_ID>' -only-testing:OshicueKitUITests/AppearanceUITests -only-testing:OshicueKitUITests/DataManagementFailureUITests -only-testing:OshicueKitUITests/AppearanceScreenUITests
```

## 新增人工重測步驟

- 喺設定開啟手帳管理同日誌管理，轉去備份核對原有匯入／匯出；分別組合篩選同選取部分結果，核對確認數量，先取消再確認，重啟檢查刪除結果。物販確認要分別列出各貨幣；改篩選必須清空選取。
- 多選封存兼任成員同一個團體，核對刪除範圍、其他身份同歷史編輯；清空物販後目錄同日誌仍在。日誌按狀態、日期、搜尋選刪，核對未選正文同其他模組冇受影響。
- 設定最愛，再由另一兼任身份重新開啟，核對共用設定、選擇頁頂部專區、各身份價格同新紀錄預設；取消最愛、封存、匯出再匯入後分別核對結果。舊 JSON 冇 `isFavorite` 應係未最愛，`schemaVersion` 仍然係 `1`。
- 喺系統切換淺色同深色，逐一檢查四個模組、詳情、編輯、選擇、管理同備份；一併檢查大字體、九語言長文字、頭像、代表色、金額、選取按鈕同錯誤提示。記錄測試裝置同系統版本，唔用舊截圖代替今次結果。

---

[返回 README](../README.zh-HK.md) · [匯入格式](IMPORT.zh-HK.md) · [驗證指南](VERIFICATION.zh-HK.md) · [顯示同縮放](TIMETABLE-READABILITY.zh-HK.md) · [刪除日程表](SCHEDULE-DELETION.zh-HK.md)

範本: [時間表](../Templates/timetable-template.json) · [固定示例](../Templates/demo-timetable.json) · [可讀性重測](../Templates/timetable-readability.json)
