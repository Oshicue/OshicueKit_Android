# 时间表验证指南

[简体中文](VERIFICATION.md) | [繁體中文](VERIFICATION.zh-Hant.md) | [繁體粵語](VERIFICATION.zh-HK.md) | [English](VERIFICATION.en.md) | [日本語](VERIFICATION.ja.md) | [한국어](VERIFICATION.ko.md) | [ไทย](VERIFICATION.th.md) | [Bahasa Indonesia](VERIFICATION.id.md) | [Bahasa Melayu](VERIFICATION.ms.md)

本页说明当前源码的验证范围和复测方法。测试文件的存在不代表本轮已经运行；历史截图只作界面参考，不作为当前构建的通过证据。

## 可重复检查

在工程根目录运行：

```sh
swift test
python3 scripts/verify-localizations.py
```

`swift test` 运行 Foundation 核心测试，不运行 iOS 界面测试。本地化检查覆盖九种语言的键集合、非空文本、格式占位符和 `.strings` 语法。

| 文件 | 核验范围 |
| --- | --- |
| [ScheduleImporterTests.swift](../Tests/ScheduleImporterTests.swift) | 格式与字段错误、引用及重复 ID、时间偏移、小数秒、跨午夜、夏令时、完整活动更新、模板及示例种子。 |
| [TimelineLayoutTests.swift](../Tests/TimelineLayoutTests.swift) | 边界接续、重叠分栏、最小并发列数、稳定排序、密集列宽及横向无碰撞。 |
| [ScheduleDeletionTests.swift](../Tests/ScheduleDeletionTests.swift) | 活动删除、共享收藏、同名不同 ID、未知 ID、空表编码及重新载入。 |

## 原生界面复测

用 Xcode 打开工程，在 iOS 模拟器选择 UI 测试运行。主要用例位于 [OshicueKitUITests.swift](../UITests/OshicueKitUITests.swift)、[TimelineReadabilityUITests.swift](../UITests/TimelineReadabilityUITests.swift) 和 [ScheduleDeletionUITests.swift](../UITests/ScheduleDeletionUITests.swift)。

检查单击详情、双击收藏、我的日程合并类型、无效导入无法确认、有效导入替换示例、九语选择与切换、短场次和并发物贩可读性，以及删除确认、选择回退和空表重启。DEBUG UI 测试以 `OSHICUE_UI_TESTS=1` 使用独立日程文件；`OSHICUE_PRESERVE_UI_DATA=1` 用于跨启动保留测试资料。

## 手动补充

导入多个文件，确认坏文件或跨文件重复 ID 不会部分应用；单独及同时选择不同时区活动，核对筛选栏时区；检查跨午夜与刚好午夜结束；放大系统文字并复测详情。删除后核对手账与日志快照保留；重新启动核对日程和收藏。

模拟器检查不能替代真机、各系统版本、大字号和文件选择器的实际验证。UI 测试、真机安装和发布应分别记录本次实际执行结果。现有 [截图目录](../screenshots/) 保存过去的界面参考。

## 0.4.0 管理、最爱与外观复测

以下为 0.4.0 / build 10 新增的测试定义与复测范围，不表示所有 UI 用例本轮已经执行通过。按实际运行记录通过、失败或未执行；色板检查也不替代各页面的人工外观检查。

| 测试文件 | 用例数 | 新增覆盖 |
| --- | --- | --- |
| [DataManagementUITests.swift](../UITests/DataManagementUITests.swift) | 6 | 物贩筛选多选与取消／确认、筛选变化清选择、清空所有币种、日志多选删除、设置管理入口仍有备份表单。 多选目录归档、取消与确认，并保留历史物贩。 |
| [DataManagementFailureUITests.swift](../UITests/DataManagementFailureUITests.swift) | 2 | 在 Debug 的独立测试资料中以 `OSHICUE_FAIL_BATCH_DELETE=1` 注入实际保存失败；检查原记录、日志正文和当前选择保留，重启后资料不变，解除故障再选择并重试删除。 |
| [AppearanceUITests.swift](../UITests/AppearanceUITests.swift) | 5 | 实际 UIKit 动态色板、浅深色文字与选中控件对比、场馆底色、增强对比和成员代表色的可读性；不操作 App 页面。 |
| [AppearanceScreenUITests.swift](../UITests/AppearanceScreenUITests.swift) | 1 | 在系统浅色或深色外观下实际操作四个模块、成员最爱与选择、编辑、管理和备份；App 不强制主题，界面断言后保存 13 个真实截图附件。 |

```sh
xcodebuild test -project OshicueKit.xcodeproj -scheme OshicueKit -destination 'platform=iOS Simulator,id=<SIMULATOR_ID>' -only-testing:OshicueKitUITests/AppearanceUITests -only-testing:OshicueKitUITests/DataManagementFailureUITests -only-testing:OshicueKitUITests/AppearanceScreenUITests
```

## 新增人工复测步骤

- 在设置打开手账管理和日志管理，切到备份核对原导入／导出；分别组合筛选并选择部分结果，核对确认数量，先取消再确认，重启检查删除结果。物贩确认需分别列出各币种；更改筛选必须清空选择。
- 多选归档兼任成员及一个团体，核对删除范围、其他身份和历史编辑；清空物贩后目录与日志仍在。日志按状态、日期、搜索选删，核对未选正文及其他模块不受影响。
- 设置最爱并从另一兼任身份重新打开，核对共享设置、选择页顶部专区、各身份价格及新记录默认；取消最爱、归档、导出再导入后分别核对结果。旧 JSON 缺少 `isFavorite` 时应未最爱，`schemaVersion` 仍为 `1`。
- 在系统切换浅色与深色，逐一检查四个模块、详情、编辑、选择、管理和备份；同时检查大字号、九语言长文字、头像、代表色、金额、选中按钮与错误提示。记录测试设备和系统版本，不以旧截图替代本次结果。

---

[返回 README](../README.md) · [导入格式](IMPORT.zh-CN.md) · [验证指南](VERIFICATION.md) · [显示与缩放](TIMETABLE-READABILITY.md) · [删除日程表](SCHEDULE-DELETION.md)

模板：[时间表](../Templates/timetable-template.json) · [固定示例](../Templates/demo-timetable.json) · [可读性复测](../Templates/timetable-readability.json)
