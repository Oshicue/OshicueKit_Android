# 删除日程表

[简体中文](SCHEDULE-DELETION.md) | [繁體中文](SCHEDULE-DELETION.zh-Hant.md) | [繁體粵語](SCHEDULE-DELETION.zh-HK.md) | [English](SCHEDULE-DELETION.en.md) | [日本語](SCHEDULE-DELETION.ja.md) | [한국어](SCHEDULE-DELETION.ko.md) | [ไทย](SCHEDULE-DELETION.th.md) | [Bahasa Indonesia](SCHEDULE-DELETION.id.md) | [Bahasa Melayu](SCHEDULE-DELETION.ms.md)

## 操作

打开时间表的活动选择页，每个日程表都有红色删除按钮及垃圾桶图标。点击后确认提示会显示目标名称；取消保留资料，确认移除该活动及全部 LIVE、物贩场次。选择活动与删除活动是分别操作。

删除先写入本机文件，成功后才更新界面；保存失败会显示错误并保留原有日程，可重试。删除未选活动不改变其他选择；删除唯一选中活动后自动选择剩余活动，日期同步调整。

## 收藏与独立资料

只保留仍在其他活动 `artists` 数组中声明的团体收藏，不按显示名称或是否还有场次判断。同名但 ID 不同的团体互不共享收藏。

手账与日志使用独立资料及活动快照，删除日程表不删除消费记录、统计或日志原稿。删除全部活动后保存空表，重新启动仍为空；可重新导入，也可在设置主动载入示例。载入示例按示例活动 ID 合并，保留其他 ID 的活动。

## 复测

1. 导入两个活动，共享一个团体 ID，并各有独有团体；收藏共享和待删除活动的独有团体。
2. 删除提示中核对名称，先取消，确认活动、场次和选择保留。
3. 确认删除，检查目标场次消失、共享收藏保留、仅在已删活动声明的收藏移除；检查剩余选择与日期。
4. 删除未选活动，检查其他选择保留；检查手账及日志资料不受影响。
5. 删空后重启确认没有恢复示例，再导入并收藏，确认可正常使用。

核心检查见 [ScheduleDeletionTests.swift](../Tests/ScheduleDeletionTests.swift)，原生操作检查见 [ScheduleDeletionUITests.swift](../UITests/ScheduleDeletionUITests.swift)。本轮未重跑 UI 测试。

---

[返回 README](../README.md) · [导入格式](IMPORT.zh-CN.md) · [验证指南](VERIFICATION.md) · [显示与缩放](TIMETABLE-READABILITY.md) · [删除日程表](SCHEDULE-DELETION.md)

模板：[时间表](../Templates/timetable-template.json) · [固定示例](../Templates/demo-timetable.json) · [可读性复测](../Templates/timetable-readability.json)
