# 时间表显示与缩放

[简体中文](TIMETABLE-READABILITY.md) | [繁體中文](TIMETABLE-READABILITY.zh-Hant.md) | [繁體粵語](TIMETABLE-READABILITY.zh-HK.md) | [English](TIMETABLE-READABILITY.en.md) | [日本語](TIMETABLE-READABILITY.ja.md) | [한국어](TIMETABLE-READABILITY.ko.md) | [ไทย](TIMETABLE-READABILITY.th.md) | [Bahasa Indonesia](TIMETABLE-READABILITY.id.md) | [Bahasa Melayu](TIMETABLE-READABILITY.ms.md)

## 时间轴与名称

全部日程按活动、场馆和 LIVE／物贩类型分列；我的日程把收藏团体的两种场次合并。重叠场次自动并排，同一重叠组采用最少所需通道；独立的后续场次恢复整列宽度。可以横向滑动，左侧时间刻度保持原位。

标准文字大小下，每条并行通道至少为 180 pt；长名称或短场次可使通道扩大至 320 pt。场馆总列宽随并发数量扩大，通道标题重复显示场馆，方便滑动辨认。列宽和名称字号会跟随系统文字大小。

艺人名优先显示并允许换行：全部日程默认 12 pt，我的日程默认 15 pt，必要时缩至标准 11 pt。空间不足先隐藏块内时间、场馆或区域等次要信息。图块高度与位置按真实时间绘制，只保留绘制间距，不为文字人为拉长场次。极短场次、特长名称或大字号仍可能截断；单击详情可读完整原始名称、时间、场馆、区域及备注，VoiceOver 也可读取名称。

双击方块收藏／取消收藏团体；详情中也有收藏操作。单击查看不会改变收藏。

## 缩放与现在时间

| 比例 | 每分钟高度 |
| --- | --- |
| 50% | 2 pt |
| 70% | 2.8 pt |
| 100%（默认） | 4 pt |
| 150% | 6 pt |

缩放只改变时间轴显示比例。现在时间线取真实系统时间；定位按钮在当前活动有今天排期时切到今天，并在现在时刻位于可视时间范围内时滚动定位。

## 可读性复测

导入文末链接中的复测模板，选择 `2026-06-14`、`Asia/Tokyo`。它含日／英文长名、5 分钟及 10 分钟 LIVE、同场馆四场并发物贩，均为虚构排期。

1. 在默认 100% 核对短场次时长和名称，横向滑动读完四场物贩，确认没有相互覆盖。
2. 单击查看完整详情；双击收藏后到我的日程确认 LIVE 与物贩一起出现，并查看重叠提示。
3. 依次选择 50%、70%、100%、150%，确认位置随时间比例变化，缩小后仍能查看详情。
4. 调大系统文字，重复滑动、详情与收藏检查。

核心布局用例及 UI 用例见验证指南；本轮未重跑 UI 测试，大字号步骤是待执行的复测要求。

---

[返回 README](../README.md) · [导入格式](IMPORT.zh-CN.md) · [验证指南](VERIFICATION.md) · [显示与缩放](TIMETABLE-READABILITY.md) · [删除日程表](SCHEDULE-DELETION.md)

模板：[时间表](../Templates/timetable-template.json) · [固定示例](../Templates/demo-timetable.json) · [可读性复测](../Templates/timetable-readability.json)
