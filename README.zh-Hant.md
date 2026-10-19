# Oshicue Kit · Android

[简体中文](README.md) | [繁體中文](README.zh-Hant.md) | [繁體粵語](README.zh-HK.md) | [English](README.en.md) | [日本語](README.ja.md) | [한국어](README.ko.md) | [ไทย](README.th.md) | [Bahasa Indonesia](README.id.md) | [Bahasa Melayu](README.ms.md)

用於整理演出與旅行行程、查看活動時間表、記錄物販消費及撰寫活動日誌的原生 Android 應用程式。使用 **Kotlin、Jetpack Compose 和 Material 3** 開發，提供日程、時間表、手帳、日誌、設定五個分頁。

目前版本：**0.5.3（Build 16）**。支援 **Android 8.0（API 26）及以上版本**，應用程式 ID 為 `com.oshicue.kit`。

## 快速開始

1. 安裝已簽署的 APK，或依照下方說明從原始碼建置。
2. 首次啟動會載入虛構的時間表與手帳範例；日程和日誌初始為空。
3. 在設定中切換語言，或進入各模組的管理頁面匯入自己的 JSON 檔案。
4. 定期匯出日程、手帳和日誌備份，並妥善保存原始時間表檔案。

首次正式匯入時間表會取代範例，之後依活動 ID 更新或新增。手帳首次正式匯入會取代未經修改的範例；手動修改過的範例會保留並合併。範例中的名稱、日期、航班和地點僅供體驗，使用前請換成實際資料。

## 主要功能

### 日程

- 在行事曆中管理 Live、航班、飯店、餐廳及其他地點，支援指定時間與全天活動、時區、地址及備註。日程與時間表中的「我的日程」選取項目各自獨立。
- 航班可分別記錄出發、抵達時間、時區及機場；飯店以入住和退房時間作為行程節點。
- 支援城市與地點搜尋，可在外部地圖應用程式中查看位置或路線。搜尋結果不會自動設定時區，請手動確認。
- 顯示相鄰指定時間活動之間的空檔或重疊；全天活動不參與交通區段比較。目前尚未整合應用程式內的路線時間估算或即時航班查詢。
- 透過 Android 行事曆編輯器另存系統行事曆副本；副本與應用程式內的紀錄分別編輯。
- 提供搜尋、組合篩選、多選刪除、JSON 匯入預覽及全部日程備份。

### 時間表

- 依日期、活動及 LIVE／物販類型篩選，橫向查看場館，縱向查看時間。
- 點一下場次查看詳細資訊；點兩下或長按可加入、移出「我的日程」，詳細資訊頁面也提供相同操作。
- 重疊場次並排顯示並提示衝突；支援 50%、70%、100%、150% 縮放。
- 目前時間線每 15 秒更新；跨午夜場次依顯示時區拆分。多活動檢視採用所選活動中，在文件順序裡排第一個的活動時區。
- 支援貼上或匯入多個檔案、驗證及預覽。相同 ID 的活動會整體更新，其他活動保留；同批次重複的活動 ID 會被拒絕。
- 刪除活動須確認，並會清除該活動的場次選取項目，保留手帳和日誌。目前沒有時間表匯出入口，請保存原始匯入檔案。

### 手帳 · Ledger

- 透過行事曆查看每日、每月物販數量及各幣別消費，管理團體、成員、頭像、物販類型與活動資料。
- 同一成員可加入多個團體，各身分可分別設定顏色和預設價格；最愛成員設定由各身分共用。
- 記錄日期、數量、幣別、單價及活動快照。新增紀錄會優先參考所選日期當天或之前，相同成員、團體、類型及幣別的價格，再使用記住的新增價格、成員預設價格或 0；單價可手動修改。
- 排行榜支援日、月、年、生涯，以及成員合計、成員分團、團體、數量或消費等統計方式。
- 支援 `CNY`、`JPY`、`HKD`、`TWD`、`USD`、`KRW`、`THB`、`IDR`、`MYR`；依幣別分別統計，不換算匯率。`JPY`、`KRW` 使用整數，其餘使用兩位小數；JSON 的 `unitPriceMinor` 使用整數最小貨幣單位。
- 移除成員或團體會封存相關資料與身分，保留歷史購買快照及統計。紀錄管理支援篩選、批次刪除、完整 JSON 備份及匯入。

### 日誌 · Diary

- 撰寫標題、內文、摘要及標籤，使用「草稿」「已整理」狀態，依日期瀏覽並搜尋或篩選。
- 可從手帳活動帶入日期、名稱、城市、場館及地址；日誌儲存獨立快照。
- 停止編輯約 0.5 秒後自動儲存，完成或關閉編輯器時提交待儲存內容；儲存失敗會提示重試。
- 複製 Markdown／純文字、匯出 `.md`／`.txt`，或使用 Android 系統分享。「已整理」僅為本機狀態，分享不會自動發布。
- 提供批次管理及完整 JSON 備份。相同 ID 且內容相同的項目會去除重複；相同 ID 但內容衝突時，會拒絕整次匯入。

## JSON 範本與備份

| 內容 | 內建檔案 |
| --- | --- |
| 日程範本 | [agenda-template.json](app/src/main/assets/templates/agenda-template.json) |
| 時間表範本 | [timetable-template.json](app/src/main/assets/templates/timetable-template.json) |
| 範例時間表 | [demo-timetable.json](app/src/main/assets/templates/demo-timetable.json) |
| 手帳範本 | [ledger-template.json](app/src/main/assets/templates/ledger-template.json) |

JSON 檔案使用 UTF-8，透過對應模組的入口匯入，先預覽再確認。各模組的 JSON 結構不同，即使同為 `schemaVersion: 1` 也不能混用。日誌可先新增一篇，再匯出備份作為格式參考；原始碼未附獨立的日誌範本。

## 本機資料與設定

資料儲存在應用程式私有目錄 `filesDir/OshicueKit/` 下的 `agenda.json`、`schedule.json`、`ledger.json`、`diary.json` 和 `settings.json`。語言及上次開啟的分頁會在本機記住；時間表選取項目也儲存在本機。

預設跟隨系統語言，不支援的語言會改用英語；可手動切換九種語言。介面跟隨系統淺色／深色外觀，並在 Android 14 及以上版本回應系統增強對比設定。

核心紀錄可離線使用，無須帳號。目前未提供應用程式內雲端同步、自動雲端備份、推播提醒或影像辨識匯入。地點搜尋依賴裝置的地理編碼服務；外部地圖、路線及行事曆功能取決於已安裝的應用程式與服務，相關服務可能需要網路。解除安裝或清除應用程式資料前，請手動匯出備份。

## 從原始碼建置

以下為目前專案設定及本次打包所用的環境：

| 項目 | 版本 |
| --- | --- |
| 應用程式版本 / `versionCode` | `0.5.3` / `16` |
| 最低 / 目標 / 編譯 Android API | `26` / `37` / `37` |
| Android Gradle Plugin | `9.4.1` |
| Kotlin Compose 外掛程式 | `2.2.10` |
| Compose BOM | `2026.09.00` |
| Java 原始碼與位元碼目標 | `17` |
| 本次驗證使用的 Gradle / JBR | `9.8.1` / `25.0.3` |

1. 在 Android Studio 中開啟本目錄，安裝 Android SDK Platform 37 及所需的建置工具。
2. 使用合適的 Gradle 執行階段 JDK；本次驗證使用 Android Studio 內附的 JBR。Java 編譯目標 `17` 與執行 Gradle 的 JDK 版本是兩項設定。
3. 讓 Android Studio 產生 `local.properties`，或自行將 `sdk.dir` 填為本機 SDK 的絕對路徑。請勿提交此檔案。
4. 將已安裝的 Gradle 加入 `PATH`，在本目錄執行以下命令。首次解析相依套件需要網路。

原始碼未附 Gradle Wrapper，因此以下命令使用 `gradle`：

```sh
gradle :app:assembleDebug
gradle :app:testDebugUnitTest :app:lintDebug
```

APK 輸出位置：`app/build/outputs/apk/debug/app-debug.apk`。預設建置使用偵錯簽章；目前專案未設定正式發布簽章，發布前須自行設定。

對應原始碼在本次打包時通過 **166 項單元測試**，APK 簽章與對齊檢查通過。儲存庫未提供 Android 介面自動化測試套件；地圖、系統行事曆、檔案選取及分享應在目標裝置上驗證。

上傳 GitHub 時請排除 `local.properties`、`.gradle/`、`.kotlin/`、`build/`、`app/build/`、本機 IDE 檔案及簽署金鑰。

## 專案結構

| 路徑 | 內容 |
| --- | --- |
| `app/src/main/java/com/oshicue/kit/core/` | 資料模型、JSON、匯入、統計及本機儲存 |
| `app/src/main/java/com/oshicue/kit/ui/` | Compose 頁面、編輯器、管理及檔案操作 |
| `app/src/main/assets/localization/` | 九種語言的介面文案 |
| `app/src/main/assets/templates/` | 日程、時間表及手帳 JSON 範本 |
| `app/src/main/res/` | 圖示、字型及主題資源 |
| `app/src/test/` | 核心單元測試 |

## 文件語言與字型授權

簡體中文為原稿，其餘八份 README 與本頁內容對應。語言代碼為 `zh-Hans`、`zh-Hant`、`zh-HK`、`en`、`ja`、`ko`、`th`、`id`、`ms`。JSON 鍵、ID、列舉值、命令及路徑不翻譯。

內建 Unbounded 字型採用 [SIL Open Font License 1.1](app/src/main/assets/fonts-OFL.txt)。第三方元件遵循各自的授權條款。
