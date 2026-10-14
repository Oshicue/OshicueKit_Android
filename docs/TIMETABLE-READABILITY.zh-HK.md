# 時間表顯示同縮放

[简体中文](TIMETABLE-READABILITY.md) | [繁體中文](TIMETABLE-READABILITY.zh-Hant.md) | [繁體粵語](TIMETABLE-READABILITY.zh-HK.md) | [English](TIMETABLE-READABILITY.en.md) | [日本語](TIMETABLE-READABILITY.ja.md) | [한국어](TIMETABLE-READABILITY.ko.md) | [ไทย](TIMETABLE-READABILITY.th.md) | [Bahasa Indonesia](TIMETABLE-READABILITY.id.md) | [Bahasa Melayu](TIMETABLE-READABILITY.ms.md)

## 時間軸同名稱

全部日程按活動、場館同 LIVE／物販類型分欄；我的日程會合併收藏團體嘅兩種場次。重疊場次自動並排，同一重疊組用最少所需通道；之後獨立嘅場次恢復成整欄寬度。可以橫向捲動，左邊時間刻度保持原位。

標準文字大小下，每條並行通道最少有 180 pt；長名稱或短場次可以令通道擴大到 320 pt。場館總欄寬會隨並行數量增加，通道標題重複顯示場館，方便捲動時辨認。欄寬同名稱字體大小會跟隨系統文字大小。

藝人名優先顯示並容許換行：全部日程預設 12 pt，我的日程預設 15 pt，有需要會縮到標準 11 pt。空間唔夠會先隱藏圖塊入面嘅時間、場館或區域等次要資料。圖塊高度同位置按真實時間繪製，只保留繪製間距，唔會為文字人為拉長場次。極短場次、特長名稱或大字體仍可能截斷；撳一下詳情可以睇完整原始名稱、時間、場館、區域同備註，VoiceOver 亦可以讀取名稱。

連撳兩下方塊收藏／取消收藏團體；詳情亦有收藏操作。撳一下查看唔會改變收藏。

## 縮放同現在時間

| 比例 | 每分鐘高度 |
| --- | --- |
| 50% | 2 pt |
| 70% | 2.8 pt |
| 100%（預設） | 4 pt |
| 150% | 6 pt |

縮放只會改變時間軸顯示比例。現在時間線用真實系統時間；定位按鈕會喺目前活動有今日排期時切到今日，而現在時刻喺可視時間範圍內時就會捲動定位。

## 可讀性重測

匯入文末連結入面嘅重測範本，揀 `2026-06-14`、`Asia/Tokyo`。範本包括日／英文長名、5 分鐘同 10 分鐘 LIVE、同場館四場並行物販，全部都係虛構排期。

1. 喺預設 100% 核對短場次時長同名稱，橫向捲動睇晒四場物販，確認冇互相覆蓋。
2. 撳一下睇完整詳情；連撳兩下收藏後，到我的日程確認 LIVE 同物販一齊出現，再睇重疊提示。
3. 依次揀 50%、70%、100%、150%，確認位置按時間比例變化，縮小後仍然可以睇詳情。
4. 放大系統文字，再檢查捲動、詳情同收藏。

核心布局案例同 UI 案例見驗證指南；今次冇重跑 UI 測試，大字體步驟係有待執行嘅重測要求。

---

[返回 README](../README.zh-HK.md) · [匯入格式](IMPORT.zh-HK.md) · [驗證指南](VERIFICATION.zh-HK.md) · [顯示同縮放](TIMETABLE-READABILITY.zh-HK.md) · [刪除日程表](SCHEDULE-DELETION.zh-HK.md)

範本: [時間表](../Templates/timetable-template.json) · [固定示例](../Templates/demo-timetable.json) · [可讀性重測](../Templates/timetable-readability.json)
