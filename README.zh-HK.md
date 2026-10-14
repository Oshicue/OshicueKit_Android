# Oshicue Kit for Android

[简体中文](README.md) | [繁體中文](README.zh-Hant.md) | [繁體粵語](README.zh-HK.md) | [English](README.en.md) | [日本語](README.ja.md) | [한국어](README.ko.md) | [ไทย](README.th.md) | [Bahasa Indonesia](README.id.md) | [Bahasa Melayu](README.ms.md)

原生 Android App（Kotlin + Jetpack Compose），用嚟睇活動時間表、記錄物販開支同整理活動日誌。四個分頁依次係時間表、手帳、日誌、設定。目前版本係 **0.4.0（versionCode 10）**，同 iOS 版 Oshicue Kit 0.4.0（build 10）功能對齊，支援 Android 8.0（API 26）或以上。

## 截圖

| 時間表 | 我嘅日程 | 手帳月曆 |
| --- | --- | --- |
| ![時間表](screenshots/timetable.png) | ![我嘅日程](screenshots/my-timetable.png) | ![手帳月曆](screenshots/ledger-calendar.png) |

| 記一筆 | 手帳統計 | 日誌 |
| --- | --- | --- |
| ![記一筆](screenshots/ledger-record-form.png) | ![手帳統計](screenshots/ledger-statistics.png) | ![日誌](screenshots/diary.png) |

| 新增日誌 | 設定 |
| --- | --- |
| ![新增日誌](screenshots/diary-editor.png) | ![設定](screenshots/settings.png) |

## 功能

### 時間表

- 全部日程按場館同 LIVE／物販分區；日期、活動多選同類型篩選可以組合使用。橫向睇場館，縱向睇時間。
- 單擊場次睇完整詳情；雙擊收藏或者取消收藏團體。收藏按 `artistID` 跨活動共享；「我嘅日程」合併顯示收藏團體嘅 LIVE 同物販場次，重疊時並排顯示並提示衝突。
- 時間軸提供 50%、70%、100%、150% 縮放（預設 100%，即 4dp／分鐘），併發多嘅時候會橫向擴寬，短場次優先顯示名，字級自動縮放，詳情保留完整資料。
- 目前時間線用真實系統時間，每 15 秒更新；「現在」只喺所選活動有今日排期嗰陣先切換到今日。
- 多活動統一用文件順序入面首個所選活動嘅時區；詳情可以睇原活動當地時間；跨午夜場次按展示時區拆到相應日期。
- 匯入支援貼上 JSON 或者揀一個／多個 UTF-8 JSON 檔案，先校驗同預覽，再確認套用；同一個活動 ID 會整體取代該活動，其餘保留，同一批入面重複嘅活動 ID 會被拒絕。刪除時間表要確認，並清理唔再屬於任何剩餘活動嘅收藏。

### 手帳

- 月曆睇每日同每月數量同分貨幣金額；點選成員卡片編輯物販，多筆記錄先揀選再編輯。
- 管理團體、成員、頭像、各團體身份嘅代表色同預設價格、物販類型同活動範本；兼任成員透過關聯已有成員新增身份。
- 記錄日期、成員同團體、類型、數量、貨幣、單價同可選嘅活動資料，亦可以從時間表帶入活動資料。
- 排行榜按日／月／年／生涯，支援數量或者消費、成員合計／成員分團／團體、類型同團體篩選；並列名次係 `1、1、3`；消費揀單一貨幣，唔做匯率換算。
- 歷史記錄保存名、顏色、活動同價格快照；刪除成員／團體屬於封存，歷史記錄同統計保留，仲可以繼續編輯。
- 成員可以標記「最愛成員」，記錄成員揀選頁會置頂顯示；金額用整數最小貨幣單位，支援 `CNY`、`JPY`、`HKD`、`TWD`、`USD`、`KRW`、`THB`、`IDR`、`MYR`。
- 手帳管理提供記錄管理（按日期範圍、成員、團體、類型、貨幣、活動同文字組合篩選之後批次刪除、刪除全部）、資料清理（封存成員／團體）同 JSON 備份（匯出、匯入預覽、衝突拒絕）。

### 日誌

- 新增活動 repo，編輯標題、內文、摘要同標籤，標記做「草稿」或者「已整理」；按活動日期睇、按狀態篩選、搜尋內文同活動資料。
- 可以從手帳活動帶入日期、名、城市、場館同地址，再獨立修改；日誌保存自己嘅快照。
- 停止編輯大約 0.5 秒後自動儲存；關閉編輯器或者入後台即刻提交；儲存失敗可以重試。
- 「準備發布」可以複製 Markdown／純文字、匯出 `.md`／`.txt` 或者叫系統分享。
- 日誌管理提供篩選、批次刪除同 JSON 備份匯入匯出。

### 設定同資料

- 語言切換（9 種語言 + 跟隨系統）即時生效；日期、時間同數字按所選地區格式化。
- 時間表匯入、手帳管理、日誌管理、範例載入同版本資訊統一入口。
- 三個模組獨立保存喺本機 `files/OshicueKit/` 入面嘅 `schedule.json`、`ledger.json`、`diary.json`，原子寫入；核心功能離線都用得。

## 建置同執行

要求：

- JDK 17 或以上（建議用 Android Studio 自帶嘅 JBR 或者 JDK 21+）
- Android SDK Platform 37 同 Build-Tools 36（Android Studio 可以自動安裝）
- 喺儲存庫根目錄建立 `local.properties` 寫 `sdk.dir=/path/to/Android/sdk`，或者設定 `ANDROID_HOME` 環境變數
- 首次建置要連網去 Maven 儲存庫；Gradle Wrapper 會自動下載對應版本

```sh
./gradlew :app:assembleDebug        # 建置除錯 APK
./gradlew :app:installDebug         # 裝落已連接嘅裝置或者模擬器
./gradlew :app:testDebugUnitTest    # 93 項單元測試（同 iOS 單元測試一一對應）
./gradlew :app:lintDebug            # 靜態檢查
./gradlew :app:assembleRelease      # 建置發布 APK
```

### 發布簽名

儲存庫唔包含簽名素材。如果要整可安裝、可散佈嘅發布 APK，請喺儲存庫根目錄建立 `keystore.properties`（呢個檔案同 `*.jks` 已經喺 `.gitignore` 入面排除）：

```sh
keytool -genkeypair -v -keystore oshicue-kit-release.jks -alias oshicue-kit \
  -keyalg RSA -keysize 2048 -validity 10000
```

```properties
storeFile=oshicue-kit-release.jks
storePassword=你嘅密碼
keyAlias=oshicue-kit
keyPassword=你嘅密碼
```

然後執行 `./gradlew :app:assembleRelease`，輸出喺 `app/build/outputs/apk/release/app-release.apk`（冇設定 `keystore.properties` 就輸出未簽名 APK）。記得妥善備份 `oshicue-kit-release.jks` 同密碼，之後嘅版本要用同一個簽名。

## 資料格式同相容性

- 三個模組嘅 JSON 格式同 iOS 版完全一致（`schemaVersion` 係 `1`），App 入面匯出／匯入嘅備份檔案可以同 iOS 版互相使用；本機資料檔案結構都一樣。
- 時間戳用帶毫秒嘅 ISO 8601（`yyyy-MM-ddTHH:mm:ss.SSSZ`）；日期係公曆 `yyyy-MM-dd`；金額係整數最小貨幣單位（`unitPriceMinor`）。
- 匯入一律先校驗、預覽，再確認套用；同 ID 唔同內容嘅記錄會拒絕整次匯入，避免覆寫歷史。
- `Templates/` 同 `docs/` 沿用 iOS 版嘅格式範本同說明文件，可以直接參考。

## 本地化

內建簡體中文、繁體中文（台灣）、繁體粵語（香港）、English、日本語、한국어、ไทย、Bahasa Indonesia、Bahasa Melayu 共 9 種語言，資源喺 `app/src/main/res/values-*/strings.xml`。

`tools/convert_localization.py` 用嚟從 iOS 版嘅 `Localizable.strings` 重新產生本儲存庫嘅語言資源（包含 `%@` → `%1$s` 位置化參數轉換）。如果只係維護 Android 版，直接改對應嘅 `strings.xml` 就得。

## 目錄結構

| 路徑 | 內容 |
| --- | --- |
| `app/src/main/kotlin/com/oshicue/kit/core/` | 模型、JSON 匯入匯出、統計同排行、時間軸版面演算法 |
| `app/src/main/kotlin/com/oshicue/kit/data/` | 三個獨立 Store、原子寫入同復原備份 |
| `app/src/main/kotlin/com/oshicue/kit/i18n/` | 語言解析同文案查詢 |
| `app/src/main/kotlin/com/oshicue/kit/ui/` | Compose 介面：時間表、手帳、日誌、設定同全部編輯與備份頁面 |
| `app/src/main/res/` | 9 種語言資源、啟動圖示、Unbounded 字型 |
| `app/src/main/assets/` | 內建匯入範本 |
| `app/src/test/` | 單元測試（匯入校驗、貨幣精度、排名並列、時間軸演算法、UTF-8 處理等） |
| `tools/` | 本地化資源轉換腳本 |
| `Templates/`、`docs/` | JSON 範本同格式說明（沿用 iOS 版） |

## 同 iOS 版嘅差異

- 平台元件：檔案匯入匯出用系統檔案選擇器，頭像用系統相片選擇器（下採樣到 512px、JPEG 0.8、Base64 儲存，同 iOS 一樣），分享用系統分享面板。
- 無障礙：iOS 嘅 VoiceOver 自訂動作對應做 Compose 嘅輕點／雙擊手勢同語意標籤。
- 其餘互動流程、邊界條件同 JSON 資料格式同 iOS 版保持一致。

## 開源授權

專案沿用原專案嘅 [MIT License](LICENSE)；第三方元件保留各自授權。內建嘅 Unbounded 字型用 [SIL OFL 1.1](THIRD_PARTY_NOTICES)，詳見[第三方聲明](THIRD_PARTY_NOTICES)。

Oshicue Kit 原版係原生 SwiftUI iOS App（0.4.0，build 10），本儲存庫係佢嘅 Android 平台完整移植。
