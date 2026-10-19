# Oshicue Kit · Android

[简体中文](README.md) | [繁體中文](README.zh-Hant.md) | [繁體粵語](README.zh-HK.md) | [English](README.en.md) | [日本語](README.ja.md) | [한국어](README.ko.md) | [ไทย](README.th.md) | [Bahasa Indonesia](README.id.md) | [Bahasa Melayu](README.ms.md)

一個原生 Android 應用程式，幫你整理演出同行程、查看活動時間表、記錄物販開支同寫活動日誌。用 **Kotlin、Jetpack Compose 同 Material 3** 開發，提供日程、時間表、手帳、日誌、設定五個分頁。

目前版本：**0.5.3（Build 16）**。支援 **Android 8.0（API 26）或以上版本**，應用程式 ID 係 `com.oshicue.kit`。

## 快速開始

1. 安裝已簽署嘅 APK，或者按下面說明由原始碼建置。
2. 第一次開啟會載入虛構嘅時間表同手帳示範資料；日程同日誌一開始係空嘅。
3. 喺設定切換語言，或者入各模組嘅管理頁面匯入自己嘅 JSON 檔案。
4. 定期匯出日程、手帳同日誌備份，並保管原本嘅時間表檔案。

第一次正式匯入時間表會取代示範資料，之後按活動 ID 更新或者新增。手帳第一次正式匯入會取代未改過嘅示範資料；手動改過嘅示範資料會保留並合併。示範入面嘅名稱、日期、航班同地點只供試用，使用前請換成實際資料。

## 主要功能

### 日程

- 喺日曆管理 Live、航班、酒店、餐廳同其他地點，支援指定時間同全日活動、時區、地址同備註。日程同時間表入面「我嘅日程」嘅選擇各自獨立。
- 航班可以分別記錄出發、抵達時間、時區同機場；酒店以入住同退房時間作為行程節點。
- 支援城市同地點搜尋，可以喺外部地圖應用程式查看位置或者路線。搜尋結果唔會自動設定時區，請手動確認。
- 顯示相鄰指定時間活動之間嘅空檔或者重疊；全日活動唔會參與交通區段比較。目前未接入應用程式內嘅路線時間估算或者即時航班查詢。
- 透過 Android 日曆編輯器另存一份到系統日曆；副本同應用程式內嘅記錄分開編輯。
- 提供搜尋、組合篩選、多選刪除、JSON 匯入預覽同全部日程備份。

### 時間表

- 按日期、活動同 LIVE／物販類型篩選，橫向睇場館，縱向睇時間。
- 撳一下場次睇詳情；連撳兩下或者長按就可以加入、移出「我嘅日程」，詳情頁亦有相同操作。
- 重疊場次會並排顯示並提示衝突；支援 50%、70%、100%、150% 縮放。
- 目前時間線每 15 秒更新；跨午夜場次會按顯示時區拆開。多活動檢視會用所選活動當中，喺文件順序排第一個嘅活動時區。
- 支援貼上或者匯入多個檔案、檢查同預覽。相同 ID 嘅活動會整體更新，其他活動會保留；同一批入面重複嘅活動 ID 會被拒絕。
- 刪除活動需要確認，並會清除該活動嘅場次選擇，保留手帳同日誌。目前冇時間表匯出入口，請保留原本匯入嘅檔案。

### 手帳 · Ledger

- 透過日曆查看每日、每月物販數量同各貨幣嘅開支，管理團體、成員、頭像、物販類型同活動資料。
- 同一成員可以加入多個團體，各個身份可以分別設定顏色同預設價格；最愛成員設定由各個身份共用。
- 記錄日期、數量、貨幣、單價同活動快照。新記錄會優先參考所選日期當日或之前，相同成員、團體、類型同貨幣嘅價格，再用記住嘅新增價格、成員預設價或者 0；單價可以手動修改。
- 排行榜支援日、月、年、生涯，以及成員合計、成員分團、團體、數量或者開支等統計方式。
- 支援 `CNY`、`JPY`、`HKD`、`TWD`、`USD`、`KRW`、`THB`、`IDR`、`MYR`；按貨幣分開統計，唔會換算匯率。`JPY`、`KRW` 用整數，其餘用兩位小數；JSON 嘅 `unitPriceMinor` 用整數最小貨幣單位。
- 移除成員或者團體會封存相關資料同身份，保留歷史購買快照同統計。記錄管理支援篩選、批次刪除、完整 JSON 備份同匯入。

### 日誌 · Diary

- 寫標題、內文、摘要同標籤，使用「草稿」「已整理」狀態，按日期瀏覽並搜尋或者篩選。
- 可以由手帳活動帶入日期、名稱、城市、場館同地址；日誌會儲存獨立快照。
- 停止編輯約 0.5 秒後會自動儲存，完成或者關閉編輯器時會提交等緊儲存嘅內容；儲存唔到會提示重試。
- 複製 Markdown／純文字、匯出 `.md`／`.txt`，或者用 Android 系統分享。「已整理」只係本機狀態，分享唔會自動發佈。
- 提供批次管理同完整 JSON 備份。相同 ID 而內容一樣嘅項目會去除重複；相同 ID 但內容有衝突，會拒絕成次匯入。

## JSON 範本同備份

| 內容 | 內置檔案 |
| --- | --- |
| 日程範本 | [agenda-template.json](app/src/main/assets/templates/agenda-template.json) |
| 時間表範本 | [timetable-template.json](app/src/main/assets/templates/timetable-template.json) |
| 示範時間表 | [demo-timetable.json](app/src/main/assets/templates/demo-timetable.json) |
| 手帳範本 | [ledger-template.json](app/src/main/assets/templates/ledger-template.json) |

JSON 檔案用 UTF-8，經對應模組嘅入口匯入，先預覽再確認。各模組嘅 JSON 結構唔同，就算同樣係 `schemaVersion: 1` 都唔可以混用。日誌可以先新增一篇，再匯出備份當格式參考；原始碼冇附獨立嘅日誌範本。

## 本機資料同設定

資料會儲存喺應用程式私人目錄 `filesDir/OshicueKit/` 下面嘅 `agenda.json`、`schedule.json`、`ledger.json`、`diary.json` 同 `settings.json`。語言同上次開啟嘅分頁會喺本機記住；時間表選擇亦會儲存喺本機。

預設跟隨系統語言，唔支援嘅語言會改用英文；可以手動切換九種語言。介面跟隨系統淺色／深色外觀，並喺 Android 14 或以上版本跟隨系統增強對比設定。

核心記錄可以離線使用，唔需要帳戶。目前未提供應用程式內雲端同步、自動雲端備份、推送提醒或者圖片辨識匯入。地點搜尋依賴裝置嘅地理編碼服務；外部地圖、路線同日曆功能取決於已安裝嘅應用程式同服務，相關服務可能需要網絡。解除安裝或者清除應用程式資料之前，請手動匯出備份。

## 由原始碼建置

以下係目前專案設定同今次打包用嘅環境：

| 項目 | 版本 |
| --- | --- |
| 應用程式版本 / `versionCode` | `0.5.3` / `16` |
| 最低 / 目標 / 編譯 Android API | `26` / `37` / `37` |
| Android Gradle Plugin | `9.4.1` |
| Kotlin Compose 外掛 | `2.2.10` |
| Compose BOM | `2026.09.00` |
| Java 原始碼同位元碼目標 | `17` |
| 今次驗證用嘅 Gradle / JBR | `9.8.1` / `25.0.3` |

1. 喺 Android Studio 開啟呢個目錄，安裝 Android SDK Platform 37 同所需建置工具。
2. 使用合適嘅 Gradle 執行環境 JDK；今次驗證用 Android Studio 內置嘅 JBR。Java 編譯目標 `17` 同執行 Gradle 嘅 JDK 版本係兩項設定。
3. 畀 Android Studio 產生 `local.properties`，或者自行將 `sdk.dir` 填成本機 SDK 嘅絕對路徑。唔好提交呢個檔案。
4. 將已安裝嘅 Gradle 加入 `PATH`，喺呢個目錄執行以下命令。第一次解析相依套件需要網絡。

原始碼冇附 Gradle Wrapper，所以以下命令用 `gradle`：

```sh
gradle :app:assembleDebug
gradle :app:testDebugUnitTest :app:lintDebug
```

APK 輸出位置：`app/build/outputs/apk/debug/app-debug.apk`。預設建置用偵錯簽章；目前專案未設定正式發佈簽章，發佈前需要自行設定。

對應原始碼喺今次打包時通過 **166 項單元測試**，APK 簽章同對齊檢查通過。儲存庫冇提供 Android 介面自動化測試套件；地圖、系統日曆、檔案選擇同分享應該喺目標裝置驗證。

上傳 GitHub 時請排除 `local.properties`、`.gradle/`、`.kotlin/`、`build/`、`app/build/`、本機 IDE 檔案同簽署金鑰。

## 專案結構

| 路徑 | 內容 |
| --- | --- |
| `app/src/main/java/com/oshicue/kit/core/` | 資料模型、JSON、匯入、統計同本機儲存 |
| `app/src/main/java/com/oshicue/kit/ui/` | Compose 頁面、編輯器、管理同檔案操作 |
| `app/src/main/assets/localization/` | 九種語言嘅介面文案 |
| `app/src/main/assets/templates/` | 日程、時間表同手帳 JSON 範本 |
| `app/src/main/res/` | 圖示、字型同主題資源 |
| `app/src/test/` | 核心單元測試 |

## 文件語言同字型授權

簡體中文係原稿，其餘八份 README 同本頁內容對應。語言代碼係 `zh-Hans`、`zh-Hant`、`zh-HK`、`en`、`ja`、`ko`、`th`、`id`、`ms`。JSON 鍵、ID、列舉值、命令同路徑唔會翻譯。

內置 Unbounded 字型採用 [SIL Open Font License 1.1](app/src/main/assets/fonts-OFL.txt)。第三方元件遵循各自嘅授權條款。
