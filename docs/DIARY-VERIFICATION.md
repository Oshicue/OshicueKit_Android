# 日志与全局设置验证指南

[简体中文](DIARY-VERIFICATION.md) | [繁體中文](DIARY-VERIFICATION.zh-Hant.md) | [繁體粵語](DIARY-VERIFICATION.zh-HK.md) | [English](DIARY-VERIFICATION.en.md) | [日本語](DIARY-VERIFICATION.ja.md) | [한국어](DIARY-VERIFICATION.ko.md) | [ไทย](DIARY-VERIFICATION.th.md) | [Bahasa Indonesia](DIARY-VERIFICATION.id.md) | [Bahasa Melayu](DIARY-VERIFICATION.ms.md)

本页描述当前仓库的测试覆盖和复现方法，不作为当前环境已通过测试的声明。工程配置为 0.4.0 / build 10，最低 iOS 17；核心 Swift Package 还支持 macOS 14。实际结果应记录本次提交、Xcode 版本、运行设备和测试输出。

## 核心测试

[DiaryTests.swift](../Tests/DiaryTests.swift) 定义 18 项日志测试。在项目根目录运行：

```sh
swift test --filter DiaryTests
```

| 覆盖 | 检查内容 |
| --- | --- |
| 草稿与活动快照 | 空日志和不完整草稿合法；活动资料独立；缺少有效活动日期时使用已选日期。 |
| 文本与时间戳 | 长 Unicode 正文、空白和换行往返保真；毫秒精度去重；时区偏移表示相同绝对时刻。 |
| 合并与冲突 | 相同日志去重，新日志增补；同 ID 的正文、摘要、标签、状态、地址或修改时刻不同会拒绝整批。 |
| 数据校验 | 重复／空 ID、非法日期及时间顺序、版本、必填字段、字段类型、时间戳、无效 JSON 与 UTF-8、20 MiB 限制。 |
| 整理与导出 | 标签分隔和去重保留日文清浊音与重音差异；多词跨字段搜索；纯文本和 Markdown 含活动资料、摘要和原正文，标题转义。 |

测试中的重复、冲突、存储大小限制与导出行为依据当前代码；测试数量或覆盖改变时应同步更新本页。

## 原生界面测试

[DiaryUITests.swift](../UITests/DiaryUITests.swift) 定义 5 项测试：

1. 从手账选择活动，输入多行正文、摘要与标签后进入后台；重启后检查活动资料和文字，确认只有一篇日志。
2. 预览并导入 JSON；重复导入去重，同 ID 改正文时拒绝，重启后原正文保留。
3. 删除确认中的取消与确认流程；重启后删除仍生效，手账活动与消费记录保留。
4. 第四个设置页打开日志管理并切换至备份，切换英语后跨模块生效，原日志正文保留。
5. 九种语言分别检查日志标题、新建按钮、第四个设置页签及空日志状态。该项不等于所有界面文本均已人工检查。

先列出本机可用模拟器，再把 `<SIMULATOR_ID>` 换成实际设备编号：

```sh
xcrun simctl list devices available
xcodebuild test -project OshicueKit.xcodeproj -scheme OshicueKit -destination 'platform=iOS Simulator,id=<SIMULATOR_ID>' -only-testing:OshicueKitUITests/DiaryUITests
```

测试在 Debug 中通过 `OSHICUE_UI_TESTS=1` 使用独立日志文件 `ui-test-diary.json`；重启保留流程使用 `OSHICUE_PRESERVE_UI_DATA=1`。这些环境变量用于测试隔离，不是用户备份入口。

## 语言、人工检查与证据

在 macOS 上运行以下脚本，检查九种语言的键集合、非空内容、占位符及 `.strings` 解析；它不判断翻译语义或视觉排版：

```sh
python3 scripts/verify-localizations.py
```

人工检查应包括：自动保存及失败重试、选择活动与快照独立性、搜索和筛选、删除取消／确认、多个文件的合并及冲突、空备份拒绝、`.md`／`.txt`／JSON 实际保存和重新读取、系统分享取消、九语言长文本和日期显示。不要在未经授权时选择分享接收方。

现有 [日志列表](../screenshots/diary-list.png)、[编辑器](../screenshots/diary-editor.png)、[活动资料](../screenshots/diary-editor-event.png)、[备份预览](../screenshots/diary-backup-preview.png)、[冲突提示](../screenshots/diary-backup-conflict.png)、[全局设置](../screenshots/global-settings.png) 是仓库参考截图，不证明当前构建已通过。分享接收、真机安装、发布流程及自动保存故障恢复不能仅凭核心测试判断；需按实际执行单独记录结果。

## 0.4.0 管理、最爱与外观复测

以下为 0.4.0 / build 10 新增的测试定义与复测范围，不表示所有 UI 用例本轮已经执行通过。按实际运行记录通过、失败或未执行；色板检查也不替代各页面的人工外观检查。

| 测试文件 | 用例数 | 新增覆盖 |
| --- | --- | --- |
| [DiaryManagementTests.swift](../Tests/DiaryManagementTests.swift) | 4 | 状态／日期／搜索组合、无效范围、所选日志删除保留其他正文与活动资料、空日志备份。 |
| [DataManagementUITests.swift](../UITests/DataManagementUITests.swift) | 6 | 物贩筛选多选与取消／确认、筛选变化清选择、清空所有币种、日志多选删除、设置管理入口仍有备份表单。 多选目录归档、取消与确认，并保留历史物贩。 |
| [DataManagementFailureUITests.swift](../UITests/DataManagementFailureUITests.swift) | 2 | 在 Debug 的独立测试资料中以 `OSHICUE_FAIL_BATCH_DELETE=1` 注入实际保存失败；检查原记录、日志正文和当前选择保留，重启后资料不变，解除故障再选择并重试删除。 |
| [AppearanceUITests.swift](../UITests/AppearanceUITests.swift) | 5 | 实际 UIKit 动态色板、浅深色文字与选中控件对比、场馆底色、增强对比和成员代表色的可读性；不操作 App 页面。 |

```sh
swift test --filter Diary
xcodebuild test -project OshicueKit.xcodeproj -scheme OshicueKit -destination 'platform=iOS Simulator,id=<SIMULATOR_ID>' -only-testing:OshicueKitUITests/DataManagementUITests -only-testing:OshicueKitUITests/AppearanceUITests -only-testing:OshicueKitUITests/DataManagementFailureUITests
```

## 新增人工复测步骤

- 在设置打开手账管理和日志管理，切到备份核对原导入／导出；分别组合筛选并选择部分结果，核对确认数量，先取消再确认，重启检查删除结果。物贩确认需分别列出各币种；更改筛选必须清空选择。
- 多选归档兼任成员及一个团体，核对删除范围、其他身份和历史编辑；清空物贩后目录与日志仍在。日志按状态、日期、搜索选删，核对未选正文及其他模块不受影响。
- 设置最爱并从另一兼任身份重新打开，核对共享设置、选择页顶部专区、各身份价格及新记录默认；取消最爱、归档、导出再导入后分别核对结果。旧 JSON 缺少 `isFavorite` 时应未最爱，`schemaVersion` 仍为 `1`。
- 在系统切换浅色与深色，逐一检查四个模块、详情、编辑、选择、管理和备份；同时检查大字号、九语言长文字、头像、代表色、金额、选中按钮与错误提示。记录测试设备和系统版本，不以旧截图替代本次结果。

返回 [README](../README.md) · 相关：[日志](DIARY.zh-CN.md) · [总体验证](VERIFICATION.md) · [日志模板](../Templates/diary-template.json)
