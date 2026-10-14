# 刪除日程表

[简体中文](SCHEDULE-DELETION.md) | [繁體中文](SCHEDULE-DELETION.zh-Hant.md) | [繁體粵語](SCHEDULE-DELETION.zh-HK.md) | [English](SCHEDULE-DELETION.en.md) | [日本語](SCHEDULE-DELETION.ja.md) | [한국어](SCHEDULE-DELETION.ko.md) | [ไทย](SCHEDULE-DELETION.th.md) | [Bahasa Indonesia](SCHEDULE-DELETION.id.md) | [Bahasa Melayu](SCHEDULE-DELETION.ms.md)

## 操作

開啟時間表的活動選擇頁，每個日程表都有紅色刪除按鈕及垃圾桶圖示。按下後確認提示會顯示目標名稱；取消會保留資料，確認則移除該活動及全部 LIVE、物販場次。選擇活動與刪除活動是各別操作。

刪除先寫入本機檔案，成功後才更新介面；儲存失敗會顯示錯誤並保留原有日程，可重試。刪除未選活動不改變其他選擇；刪除唯一選取活動後自動選取剩餘活動，日期同步調整。

## 收藏與獨立資料

只保留仍在其他活動 `artists` 陣列中宣告的團體收藏，不依顯示名稱或是否還有場次判斷。同名但 ID 不同的團體不會共用收藏。

手帳與日誌使用獨立資料及活動快照，刪除日程表不會刪除消費紀錄、統計或日誌原稿。刪除全部活動後儲存空表，重新啟動仍為空；可重新匯入，也可在設定主動載入範例。載入範例依範例活動 ID 合併，保留其他 ID 的活動。

## 複測

1. 匯入兩個活動，共用一個團體 ID，並各有獨有團體；收藏共用和待刪除活動的獨有團體。
2. 在刪除提示核對名稱，先取消，確認活動、場次和選擇保留。
3. 確認刪除，檢查目標場次消失、共用收藏保留、僅在已刪活動宣告的收藏移除；檢查剩餘選擇與日期。
4. 刪除未選活動，檢查其他選擇保留；檢查手帳與日誌資料不受影響。
5. 刪空後重啟確認沒有恢復範例，再匯入並收藏，確認可正常使用。

核心檢查見 [ScheduleDeletionTests.swift](../Tests/ScheduleDeletionTests.swift)，原生操作檢查見 [ScheduleDeletionUITests.swift](../UITests/ScheduleDeletionUITests.swift)。本次未重跑 UI 測試。

---

[返回 README](../README.zh-Hant.md) · [匯入格式](IMPORT.zh-Hant.md) · [驗證指南](VERIFICATION.zh-Hant.md) · [顯示與縮放](TIMETABLE-READABILITY.zh-Hant.md) · [刪除日程表](SCHEDULE-DELETION.zh-Hant.md)

範本: [時間表](../Templates/timetable-template.json) · [固定範例](../Templates/demo-timetable.json) · [可讀性複測](../Templates/timetable-readability.json)
