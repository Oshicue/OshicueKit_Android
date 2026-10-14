# 手帳驗證指南

[简体中文](LEDGER-VERIFICATION.md) | [繁體中文](LEDGER-VERIFICATION.zh-Hant.md) | [繁體粵語](LEDGER-VERIFICATION.zh-HK.md) | [English](LEDGER-VERIFICATION.en.md) | [日本語](LEDGER-VERIFICATION.ja.md) | [한국어](LEDGER-VERIFICATION.ko.md) | [ไทย](LEDGER-VERIFICATION.th.md) | [Bahasa Indonesia](LEDGER-VERIFICATION.id.md) | [Bahasa Melayu](LEDGER-VERIFICATION.ms.md)

本頁說明現有測試涵蓋範圍與可重複執行的驗收步驟，依目前原始碼整理。測試檔案存在不代表本次執行通過；舊版本紀錄、暫存結果套件及截圖不作為目前版本通過的證明。實際結果以最近一次執行紀錄為準。

## 核心測試

在專案根目錄執行：

```sh
swift test --filter Ledger
```

[LedgerTests.swift](../Tests/LedgerTests.swift) 包含 16 項測試，[LedgerDeletionTests.swift](../Tests/LedgerDeletionTests.swift) 包含 7 項測試，涵蓋：

- 範例與範本編解碼往返、日期格式、閏日、時區與日／月／年／生涯範圍。
- 每日類型彙總、成員跨團體合計與分計、團體排名、幣別分開累計、競賽名次、穩定排序及零消費占比。
- 最小單位解析、超額小數位拒絕、零價、無效數量／價格，以及單筆與彙總溢位。
- 名稱、顏色與單價快照，目錄更新不改寫歷史紀錄。
- 目錄依編號合併、消費去除重複、同編號不同內容衝突、無效合併的不可分割性。
- 必填欄位、重複編號、所屬關係與價格唯一性、引用、日期、幣別、顏色、JPEG 與檔案大小檢查。
- 舊備份缺省封存欄位、成員全團體封存、團體封存、有效身分篩選、封存備份往返、歷史編輯，以及還原目錄後不重複計數。

## 原生介面測試

在 Xcode 開啟 `OshicueKit.xcodeproj`，選擇 `OshicueKit` scheme 與可用 iPhone 模擬器，執行對應測試類別。需具備專案要求的 Xcode、iOS SDK 與簽署設定。介面測試使用獨立的 `ui-test-ledger.json`；儲存檢查透過結束程序並重新啟動驗證。

| 測試檔案 | 案例數 | 主要範圍 |
| --- | --- | --- |
| [LedgerUITests.swift](../UITests/LedgerUITests.swift) | 6 | 新增與重新啟動儲存、成員合計／分團、消費幣別、無效／有效匯入、快速新增成員與預設價格、九語言標題與日曆入口 |
| [LedgerEditingDeletionUITests.swift](../UITests/LedgerEditingDeletionUITests.swift) | 5 | 單筆編輯、多筆選擇後編輯、消費刪除確認、成員全團體刪除、團體刪除，以及歷史編輯與重新啟動儲存 |

首次範例載入與儲存策略由 [LedgerStore.swift](../OshicueKit/Support/LedgerStore.swift) 實作。核心套件不包含此介面儲存類別；修改策略後，除核心測試外，還應檢查原生介面與重新啟動行為。

## 手動驗收清單

1. 以獨立測試資料啟動新帳本：應顯示範例而無範例標記，建立日數量 10、生涯數量 16。儲存修改並重新啟動應保留修改；既有帳本不能自動補回範例。
2. 為同一成員新增第二個團體，分別設定顏色與價格；合計與分團排名應改變分組，目前範圍總數量維持一致。同名但不同編號的成員應獨立統計。
3. 新增及編輯消費，檢查日期、幣別、預設價格帶入、數量、選填活動資訊與逐筆總額。改選身分／類型／幣別會重新帶入預設價格，儲存前核對實際單價。
4. 單筆成員卡片應直接編輯，多筆卡片應先選擇，儲存只改所選紀錄。取消刪除不改數量；確認後數量與金額扣除，重新啟動仍生效。
5. 刪除兼任成員應隱藏所有身分；刪除一個團體應保留其他團體中的兼任成員。歷史數量與金額不變，歷史紀錄仍可編輯，新增不能選擇已刪除身分。
6. 以 `CNY` 單價 `80.50`、數量 `2` 檢查合計 `161.00`；`JPY`／`KRW` 小數及其他幣別超過兩位小數應報錯。數量統計可合計幣別，消費排名只統計所選幣別。
7. 以 [ledger-template.json](../Templates/ledger-template.json) 檢查貼上匯入、預覽與匯出。未修改範例可取代，已修改範例須合併。重複匯入不重複計數；同編號不同消費阻止整次匯入，無效資料不改原帳本。
8. 用有效備份還原封存目錄，檢查歷史紀錄仍只有一份。以含舊版消費的備份還原已編輯消費，應產生衝突。
9. 切換九種 App 語言，檢查手帳、日曆、統計、成員、編輯、刪除與匯入錯誤文案；查看長名稱、大金額與多幣別版面。
10. 在測試裝置檢查實際相簿選圖、頭像移除、系統多檔選擇與檔案儲存，再驗證備份移轉。自動測試未涵蓋所有系統對話框與真實照片流程。

## 證據紀錄

每次驗證記錄程式版本、執行日期與時區、Xcode／系統／裝置或模擬器版本、指令或測試類別、通過／失敗／未執行項，以及可保留的紀錄或結果套件。`screenshots/ledger-*.png` 可協助理解介面，但靜態截圖不證明目前測試通過。實機安裝、系統檔案流程與發佈驗收需另行記錄。

## 0.4.0 管理、最愛與外觀複測

以下為 0.4.0 / build 10 新增的測試定義與複測範圍，不代表所有 UI 用例本次已執行通過。依實際執行記錄通過、失敗或未執行；色板檢查也不取代各頁面的人工外觀檢查。

| 測試檔案 | 用例數 | 新增涵蓋範圍 |
| --- | --- | --- |
| [LedgerManagementTests.swift](../Tests/LedgerManagementTests.swift) | 6 | 日期邊界與組合篩選、歷史資料比對、無結果及無效範圍、所選筆數／數量／各幣別金額、批次刪除與清空物販保留目錄。 |
| [LedgerCatalogManagementTests.swift](../Tests/LedgerCatalogManagementTests.swift) | 3 | 批次成員全身分封存、團體僅本團體封存、歷史保留及空選取／未知編號／重複操作。 |
| [LedgerFavoritesTests.swift](../Tests/LedgerFavoritesTests.swift) | 8 | 舊備份缺省最愛、JSON 往返與欄位類型、合併、封存排除、兼任價格、穩定排序、新紀錄優先與歷史身分。 |
| [DataManagementUITests.swift](../UITests/DataManagementUITests.swift) | 6 | 物販篩選多選與取消／確認、篩選變更清空選取、清空所有幣別、日誌多選刪除、設定管理入口仍有備份表單。 多選目錄封存、取消與確認，並保留歷史物販。 |
| [DataManagementFailureUITests.swift](../UITests/DataManagementFailureUITests.swift) | 2 | 在 Debug 的獨立測試資料中以 `OSHICUE_FAIL_BATCH_DELETE=1` 注入實際儲存失敗；檢查原紀錄、日誌內文及目前選取保留，重啟後資料不變，解除故障再選取並重試刪除。 |
| [LedgerFavoriteUITests.swift](../UITests/LedgerFavoriteUITests.swift) | 2 | 成員最愛跨團體共用及重啟保留、頂部多個身分快捷選擇、預設最愛與取消最愛。 |
| [AppearanceUITests.swift](../UITests/AppearanceUITests.swift) | 5 | 實際 UIKit 動態色板、淺深色文字及選取控制項對比、場館底色、增強對比與成員代表色的可讀性；不操作 App 頁面。 |
| [AppearanceScreenUITests.swift](../UITests/AppearanceScreenUITests.swift) | 1 | 在系統淺色或深色外觀下實際操作四個模組、成員最愛與選擇、編輯、管理及備份；App 不強制主題，介面斷言後儲存 13 個真實截圖附件。 |

```sh
swift test --filter Ledger
xcodebuild test -project OshicueKit.xcodeproj -scheme OshicueKit -destination 'platform=iOS Simulator,id=<SIMULATOR_ID>' -only-testing:OshicueKitUITests/DataManagementUITests -only-testing:OshicueKitUITests/LedgerFavoriteUITests -only-testing:OshicueKitUITests/AppearanceUITests -only-testing:OshicueKitUITests/DataManagementFailureUITests -only-testing:OshicueKitUITests/AppearanceScreenUITests
```

## 新增人工複測步驟

- 在設定開啟手帳管理與日誌管理，切至備份核對原匯入／匯出；分別組合篩選並選取部分結果，核對確認數量，先取消再確認，重新啟動檢查刪除結果。物販確認須分別列出各幣別；變更篩選必須清空選取。
- 多選封存兼任成員及一個團體，核對刪除範圍、其他身分與歷史編輯；清空物販後目錄與日誌仍在。日誌依狀態、日期、搜尋選擇刪除，核對未選內文與其他模組不受影響。
- 設定最愛並從另一兼任身分重新開啟，核對共用設定、選擇頁頂部專區、各身分價格與新紀錄預設；取消最愛、封存、匯出再匯入後分別核對結果。舊 JSON 缺少 `isFavorite` 時應未最愛，`schemaVersion` 仍為 `1`。
- 在系統切換淺色與深色，逐一檢查四個模組、詳細資料、編輯、選擇、管理及備份；同時檢查大字體、九語言長文字、頭像、代表色、金額、選取按鈕與錯誤提示。記錄測試裝置和系統版本，不以舊截圖取代本次結果。

返回 [README](../README.zh-Hant.md) · [手帳使用與資料格式](LEDGER.zh-Hant.md) · [JSON 範本](../Templates/ledger-template.json)
