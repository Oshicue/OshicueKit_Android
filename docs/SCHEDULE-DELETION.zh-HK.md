# 刪除日程表

[简体中文](SCHEDULE-DELETION.md) | [繁體中文](SCHEDULE-DELETION.zh-Hant.md) | [繁體粵語](SCHEDULE-DELETION.zh-HK.md) | [English](SCHEDULE-DELETION.en.md) | [日本語](SCHEDULE-DELETION.ja.md) | [한국어](SCHEDULE-DELETION.ko.md) | [ไทย](SCHEDULE-DELETION.th.md) | [Bahasa Indonesia](SCHEDULE-DELETION.id.md) | [Bahasa Melayu](SCHEDULE-DELETION.ms.md)

## 操作

打開時間表嘅活動選擇頁，每個日程表都有紅色刪除按鈕同垃圾桶圖示。撳咗之後，確認提示會顯示目標名稱；取消會保留資料，確認就會移除嗰個活動同全部 LIVE、物販場次。揀活動同刪除活動係分開操作。

刪除會先寫入本機檔案，成功先更新介面；儲存失敗會顯示錯誤，保留原有日程，可以再試。刪除未揀活動唔會改變其他選擇；刪除唯一已揀活動後，會自動揀返剩低嘅活動，日期一齊調整。

## 收藏同獨立資料

只保留仍然喺其他活動 `artists` 陣列聲明嘅團體收藏，唔會按顯示名稱或係咪仲有場次判斷。同名但 ID 唔同嘅團體唔會共用收藏。

手帳同日誌用獨立資料同活動快照，刪除日程表唔會刪除消費紀錄、統計或日誌原稿。刪除全部活動後會儲存空表，重新啟動仍然係空；可以重新匯入，亦可以喺設定主動載入示例。載入示例會按示例活動 ID 合併，保留其他 ID 嘅活動。

## 重測

1. 匯入兩個活動，共用一個團體 ID，亦各自有獨有團體；收藏共用團體同待刪除活動嘅獨有團體。
2. 喺刪除提示核對名稱，先取消，確認活動、場次同選擇保留。
3. 確認刪除，檢查目標場次消失、共用收藏保留、只喺已刪活動聲明嘅收藏移除；檢查剩低選擇同日期。
4. 刪除未揀活動，檢查其他選擇保留；檢查手帳同日誌資料冇受影響。
5. 刪空後重新啟動，確認冇恢復示例，再匯入同收藏，確認可以正常用。

核心檢查見 [ScheduleDeletionTests.swift](../Tests/ScheduleDeletionTests.swift)，原生操作檢查見 [ScheduleDeletionUITests.swift](../UITests/ScheduleDeletionUITests.swift)。今次冇重跑 UI 測試。

---

[返回 README](../README.zh-HK.md) · [匯入格式](IMPORT.zh-HK.md) · [驗證指南](VERIFICATION.zh-HK.md) · [顯示同縮放](TIMETABLE-READABILITY.zh-HK.md) · [刪除日程表](SCHEDULE-DELETION.zh-HK.md)

範本: [時間表](../Templates/timetable-template.json) · [固定示例](../Templates/demo-timetable.json) · [可讀性重測](../Templates/timetable-readability.json)
