# 時間表顯示與縮放

[简体中文](TIMETABLE-READABILITY.md) | [繁體中文](TIMETABLE-READABILITY.zh-Hant.md) | [繁體粵語](TIMETABLE-READABILITY.zh-HK.md) | [English](TIMETABLE-READABILITY.en.md) | [日本語](TIMETABLE-READABILITY.ja.md) | [한국어](TIMETABLE-READABILITY.ko.md) | [ไทย](TIMETABLE-READABILITY.th.md) | [Bahasa Indonesia](TIMETABLE-READABILITY.id.md) | [Bahasa Melayu](TIMETABLE-READABILITY.ms.md)

## 時間軸與名稱

全部日程依活動、場館及 LIVE／物販類型分欄；我的日程將收藏團體的兩種場次合併。重疊場次自動並排，同一重疊組採用最少所需通道；獨立的後續場次恢復整欄寬度。可橫向捲動，左側時間刻度保持原位。

標準文字大小下，每條並行通道至少為 180 pt；長名稱或短場次可使通道擴大至 320 pt。場館總欄寬隨並行數量擴大，通道標題重複顯示場館，方便捲動辨認。欄寬與名稱字級會跟隨系統文字大小。

藝人名優先顯示並允許換行：全部日程預設 12 pt，我的日程預設 15 pt，必要時縮至標準 11 pt。空間不足時先隱藏圖塊內時間、場館或區域等次要資訊。圖塊高度與位置依真實時間繪製，只保留繪製間距，不會為文字人工延長場次。極短場次、特長名稱或大字體仍可能截斷；點一下詳情可讀完整原始名稱、時間、場館、區域及備註，VoiceOver 也可讀取名稱。

連點兩下方塊收藏／取消收藏團體；詳情中也有收藏操作。點一下查看不會改變收藏。

## 縮放與現在時間

| 比例 | 每分鐘高度 |
| --- | --- |
| 50% | 2 pt |
| 70% | 2.8 pt |
| 100%（預設） | 4 pt |
| 150% | 6 pt |

縮放只改變時間軸顯示比例。現在時間線採用真實系統時間；定位按鈕會在目前活動有今天排程時切至今天，並在現在時刻位於可視時間範圍內時捲動定位。

## 可讀性複測

匯入文末連結中的複測範本，選取 `2026-06-14`、`Asia/Tokyo`。其中包含日／英文長名、5 分鐘及 10 分鐘 LIVE、同場館四場並行物販，均為虛構排程。

1. 在預設 100% 核對短場次時長和名稱，橫向捲動讀完四場物販，確認沒有互相覆蓋。
2. 點一下查看完整詳情；連點兩下收藏後到我的日程確認 LIVE 與物販一起出現，並查看重疊提示。
3. 依序選取 50%、70%、100%、150%，確認位置隨時間比例變化，縮小後仍能查看詳情。
4. 放大系統文字，重複捲動、詳情與收藏檢查。

核心布局案例及 UI 案例見驗證指南；本次未重跑 UI 測試，大字體步驟是待執行的複測要求。

---

[返回 README](../README.zh-Hant.md) · [匯入格式](IMPORT.zh-Hant.md) · [驗證指南](VERIFICATION.zh-Hant.md) · [顯示與縮放](TIMETABLE-READABILITY.zh-Hant.md) · [刪除日程表](SCHEDULE-DELETION.zh-Hant.md)

範本: [時間表](../Templates/timetable-template.json) · [固定範例](../Templates/demo-timetable.json) · [可讀性複測](../Templates/timetable-readability.json)
