# 手帳驗證指南

[简体中文](LEDGER-VERIFICATION.md) | [繁體中文](LEDGER-VERIFICATION.zh-Hant.md) | [繁體粵語](LEDGER-VERIFICATION.zh-HK.md) | [English](LEDGER-VERIFICATION.en.md) | [日本語](LEDGER-VERIFICATION.ja.md) | [한국어](LEDGER-VERIFICATION.ko.md) | [ไทย](LEDGER-VERIFICATION.th.md) | [Bahasa Indonesia](LEDGER-VERIFICATION.id.md) | [Bahasa Melayu](LEDGER-VERIFICATION.ms.md)

本頁說明現有測試涵蓋範圍同可重複執行嘅驗收步驟，依而家原始碼整理。測試檔案存在唔代表本次執行通過；舊版本紀錄、暫存結果套件同截圖唔當作而家版本通過嘅證明。實際結果以最近一次執行紀錄為準。

## 核心測試

喺專案根目錄執行：

```sh
swift test --filter Ledger
```

[LedgerTests.swift](../Tests/LedgerTests.swift) 包含 16 項測試，[LedgerDeletionTests.swift](../Tests/LedgerDeletionTests.swift) 包含 7 項測試，涵蓋：

- 示範同範本編解碼往返、日期格式、閏日、時區同日／月／年／生涯範圍。
- 每日類型彙總、成員跨團體合計同分計、團體排名、幣種分開累計、競賽名次、穩定排序同零消費占比。
- 最小單位解析、超額小數位拒絕、零價、無效數量／價格，以及單筆同彙總溢位。
- 名稱、顏色同單價快照，目錄更新唔會改寫歷史紀錄。
- 目錄依編號合併、消費去除重複、同編號唔同內容衝突、無效合併失敗時唔會只套用部分修改。
- 必填欄位、重複編號、所屬關係同價格唯一性、引用、日期、幣種、顏色、JPEG 同檔案大小檢查。
- 舊備份缺省封存欄位、成員全團體封存、團體封存、有效身份篩選、封存備份往返、歷史編輯，以及還原目錄後唔會重複計數。

## 原生畫面測試

喺 Xcode 開啟 `OshicueKit.xcodeproj`，選擇 `OshicueKit` scheme 同可用 iPhone 模擬器，執行對應測試類別。需要有專案要求嘅 Xcode、iOS SDK 同簽署設定。畫面測試使用獨立嘅 `ui-test-ledger.json`；儲存檢查透過結束程序並重新開啟驗證。

| 測試檔案 | 案例數 | 主要範圍 |
| --- | --- | --- |
| [LedgerUITests.swift](../UITests/LedgerUITests.swift) | 6 | 新增同重新開啟儲存、成員合計／分團、消費幣種、無效／有效匯入、快速新增成員同預設價格、九語言標題同月曆入口 |
| [LedgerEditingDeletionUITests.swift](../UITests/LedgerEditingDeletionUITests.swift) | 5 | 單筆編輯、多筆選擇後編輯、消費刪除確認、成員全團體刪除、團體刪除，以及歷史編輯同重新開啟儲存 |

首次示範載入同儲存策略由 [LedgerStore.swift](../OshicueKit/Support/LedgerStore.swift) 實作。核心套件唔包含呢個畫面儲存類別；修改策略後，除核心測試外，仲應該檢查原生畫面同重新開啟行為。

## 手動驗收清單

1. 以獨立測試資料啟動新帳本：應該顯示示範而冇示範標記，建立日數量 10、生涯數量 16。儲存修改並重新開啟應該保留修改；已有帳本唔可以自動補回示範。
2. 為同一成員新增第二個團體，分別設定顏色同價格；合計同分團排名應該改變分組，而家範圍總數量維持一致。同名但唔同編號嘅成員應該獨立統計。
3. 新增同編輯消費，檢查日期、幣種、預設價格帶入、數量、選填活動資訊同逐筆總額。改選身份／類型／幣種會重新帶入預設價格，儲存前核對實際單價。
4. 單筆成員卡片應該直接編輯，多筆卡片應該先選擇，儲存只改所選紀錄。取消刪除唔會改數量；確認後數量同金額扣除，重新開啟仍生效。
5. 刪除兼任成員應該隱藏所有身份；刪除一個團體應該保留其他團體中嘅兼任成員。歷史數量同金額唔變，歷史紀錄仍然可以編輯，新增唔可以選擇已刪除身份。
6. 以 `CNY` 單價 `80.50`、數量 `2` 檢查合計 `161.00`；`JPY`／`KRW` 小數同其他幣種超過兩位小數應該報錯。數量統計可以合計幣種，消費排名只統計所選幣種。
7. 以 [ledger-template.json](../Templates/ledger-template.json) 檢查貼上匯入、預覽同匯出。未修改示範可取代，已修改示範要合併。重複匯入唔會重複計數；同編號唔同消費阻止整次匯入，無效資料唔會改原帳本。
8. 用有效備份還原封存目錄，檢查歷史紀錄仍只有一份。以含舊版消費嘅備份還原已編輯消費，應該產生衝突。
9. 切換九種 App 語言，檢查手帳、月曆、統計、成員、編輯、刪除同匯入錯誤文案；查看長名稱、大金額同多幣種版面。
10. 喺測試裝置檢查實際相簿選圖、頭像移除、系統多檔選擇同檔案儲存，再驗證備份移轉。自動測試未涵蓋所有系統對話框同真實照片流程。

## 證據紀錄

每次驗證記錄程式版本、執行日期同時區、Xcode／系統／裝置或模擬器版本、指令或測試類別、通過／失敗／未執行項，以及可保留嘅紀錄或結果套件。`screenshots/ledger-*.png` 可以幫手理解畫面，但靜態截圖唔代表而家測試通過。實機安裝、系統檔案流程同發佈驗收要另外記錄。

## 0.4.0 管理、最愛同外觀重測

以下係 0.4.0 / build 10 新增嘅測試定義同重測範圍，唔代表所有 UI 用例今次已經執行通過。按實際執行記錄通過、失敗或者未執行；色板檢查亦唔可以代替各頁面嘅人工外觀檢查。

| 測試檔案 | 用例數 | 新增範圍 |
| --- | --- | --- |
| [LedgerManagementTests.swift](../Tests/LedgerManagementTests.swift) | 6 | 日期邊界同組合篩選、歷史資料比對、冇結果同無效範圍、所選條數／數量／各貨幣金額、批次刪除同清空物販保留目錄。 |
| [LedgerCatalogManagementTests.swift](../Tests/LedgerCatalogManagementTests.swift) | 3 | 批次成員全身份封存、團體只本團體封存、歷史保留同空選取／未知編號／重複操作。 |
| [LedgerFavoritesTests.swift](../Tests/LedgerFavoritesTests.swift) | 8 | 舊備份缺省最愛、JSON 往返同欄位類型、合併、封存排除、兼任價格、穩定排序、新紀錄優先同歷史身份。 |
| [DataManagementUITests.swift](../UITests/DataManagementUITests.swift) | 6 | 物販篩選多選同取消／確認、改篩選清選取、清空所有貨幣、日誌多選刪除、設定管理入口仍有備份表單。 多選目錄封存、取消同確認，並保留歷史物販。 |
| [DataManagementFailureUITests.swift](../UITests/DataManagementFailureUITests.swift) | 2 | 喺 Debug 獨立測試資料用 `OSHICUE_FAIL_BATCH_DELETE=1` 注入實際儲存失敗；檢查原紀錄、日誌正文同目前選取保留，重啟後資料不變，解除故障再選取同重試刪除。 |
| [LedgerFavoriteUITests.swift](../UITests/LedgerFavoriteUITests.swift) | 2 | 成員最愛跨團體共用同重啟保留、頂部多個身份快捷選擇、預設最愛同取消最愛。 |
| [AppearanceUITests.swift](../UITests/AppearanceUITests.swift) | 5 | 實際 UIKit 動態色板、淺深色文字同選取控制項對比、場館底色、增強對比同成員代表色可讀性；唔操作 App 頁面。 |
| [AppearanceScreenUITests.swift](../UITests/AppearanceScreenUITests.swift) | 1 | 喺系統淺色或者深色外觀下實際操作四個模組、成員最愛同選擇、編輯、管理同備份；App 唔強制主題，介面斷言後保存 13 個真實截圖附件。 |

```sh
swift test --filter Ledger
xcodebuild test -project OshicueKit.xcodeproj -scheme OshicueKit -destination 'platform=iOS Simulator,id=<SIMULATOR_ID>' -only-testing:OshicueKitUITests/DataManagementUITests -only-testing:OshicueKitUITests/LedgerFavoriteUITests -only-testing:OshicueKitUITests/AppearanceUITests -only-testing:OshicueKitUITests/DataManagementFailureUITests -only-testing:OshicueKitUITests/AppearanceScreenUITests
```

## 新增人工重測步驟

- 喺設定開啟手帳管理同日誌管理，轉去備份核對原有匯入／匯出；分別組合篩選同選取部分結果，核對確認數量，先取消再確認，重啟檢查刪除結果。物販確認要分別列出各貨幣；改篩選必須清空選取。
- 多選封存兼任成員同一個團體，核對刪除範圍、其他身份同歷史編輯；清空物販後目錄同日誌仍在。日誌按狀態、日期、搜尋選刪，核對未選正文同其他模組冇受影響。
- 設定最愛，再由另一兼任身份重新開啟，核對共用設定、選擇頁頂部專區、各身份價格同新紀錄預設；取消最愛、封存、匯出再匯入後分別核對結果。舊 JSON 冇 `isFavorite` 應係未最愛，`schemaVersion` 仍然係 `1`。
- 喺系統切換淺色同深色，逐一檢查四個模組、詳情、編輯、選擇、管理同備份；一併檢查大字體、九語言長文字、頭像、代表色、金額、選取按鈕同錯誤提示。記錄測試裝置同系統版本，唔用舊截圖代替今次結果。

返回 [README](../README.zh-HK.md) · [手帳用法同資料格式](LEDGER.zh-HK.md) · [JSON 範本](../Templates/ledger-template.json)
