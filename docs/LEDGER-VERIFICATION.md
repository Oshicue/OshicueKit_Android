# 手账验证指南

[简体中文](LEDGER-VERIFICATION.md) | [繁體中文](LEDGER-VERIFICATION.zh-Hant.md) | [繁體粵語](LEDGER-VERIFICATION.zh-HK.md) | [English](LEDGER-VERIFICATION.en.md) | [日本語](LEDGER-VERIFICATION.ja.md) | [한국어](LEDGER-VERIFICATION.ko.md) | [ไทย](LEDGER-VERIFICATION.th.md) | [Bahasa Indonesia](LEDGER-VERIFICATION.id.md) | [Bahasa Melayu](LEDGER-VERIFICATION.ms.md)

本页说明现有测试覆盖及可重复执行的验收步骤。内容依据当前源码整理；测试文件存在不等于本次运行通过，旧版本记录、临时结果包及截图不作为当前版本的通过证明。实际结果以最近一次运行日志为准。

## 核心测试

在项目根目录执行：

```sh
swift test --filter Ledger
```

[LedgerTests.swift](../Tests/LedgerTests.swift) 包含 16 项测试，[LedgerDeletionTests.swift](../Tests/LedgerDeletionTests.swift) 包含 7 项测试，覆盖：

- 示例与模板编解码往返、日期格式、闰日、时区及日／月／年／生涯范围。
- 每日类型汇总、成员跨团体合计与分计、团体排名、币种分开累计、竞赛名次、稳定排序及零消费占比。
- 最小单位解析、超额小数位拒绝、零价、无效数量／价格，以及单笔和汇总溢出。
- 名称、颜色与单价快照，目录更新不重写历史记录。
- 目录按编号合并、消费去重、同编号不同内容冲突、无效合并的原子性。
- 必填字段、重复编号、所属关系及价格唯一性、引用、日期、币种、颜色、JPEG 与文件大小校验。
- 旧备份缺省归档字段、成员全团体归档、团体归档、有效身份筛选、归档备份往返、历史编辑及恢复目录后不重复计数。

## 原生界面测试

在 Xcode 打开 `OshicueKit.xcodeproj`，选择 `OshicueKit` scheme 和可用 iPhone 模拟器，运行对应测试类。需具备项目要求的 Xcode、iOS SDK 和签名设置。界面测试使用独立的 `ui-test-ledger.json`；保存检查通过结束进程并重启验证。

| 测试文件 | 用例数 | 主要范围 |
| --- | --- | --- |
| [LedgerUITests.swift](../UITests/LedgerUITests.swift) | 6 | 新增与重启保存、成员合计／分团、消费币种、无效／有效导入、快速新增成员与默认价格、九语言标题与日历入口 |
| [LedgerEditingDeletionUITests.swift](../UITests/LedgerEditingDeletionUITests.swift) | 5 | 单笔编辑、多笔选择后编辑、消费删除确认、成员全团体删除、团体删除及历史编辑与重启保存 |

首次示例载入与保存策略由 [LedgerStore.swift](../OshicueKit/Support/LedgerStore.swift) 实现。核心包不包含这个界面存储类；修改该策略后，除核心测试外还应检查原生界面及重启行为。

## 手工验收清单

1. 使用独立测试资料启动新账本：应显示示例、没有示例标记，示例创建日数量 10，生涯数量 16。保存修改并重启，应保留修改；已有账本不能自动补回示例。
2. 同一成员添加第二个团体，分别设置颜色和价格；合计与分团排名应改变分组，当前范围总数量保持一致。同名但不同编号的成员应独立统计。
3. 新增及编辑消费，检查日期、币种、默认价格带入、数量、可选活动信息及逐笔总额。改选身份／类型／币种会重新带入默认价格，保存前核对实际单价。
4. 单笔成员卡片应直接编辑；多笔卡片应先选择，保存只改所选记录。取消删除不改数量，确认删除后数量和金额扣除，重启仍生效。
5. 删除兼任成员应隐藏其所有身份；删除一个团体应保留其他团体中的兼任成员。历史数量和金额不变，历史记录仍可编辑，新增不能选择已删除身份。
6. 以 `CNY` 单价 `80.50`、数量 `2` 检查合计 `161.00`；`JPY`／`KRW` 小数及其他币种超过两位小数应报错。数量统计可合计币种，消费排名仅统计所选币种。
7. 使用 [ledger-template.json](../Templates/ledger-template.json) 检查粘贴导入、预览和导出。未经修改的示例可替换；修改后的示例须合并。重复导入不重复计数，同编号不同消费阻止整次导入，无效资料不改原账本。
8. 用有效备份恢复归档目录，检查历史记录仍只有一份。用包含旧版本消费的备份恢复已编辑消费，应产生冲突。
9. 切换九种 App 语言，检查手账、日历、统计、成员、编辑、删除及导入错误文案；查看长名称、较大金额及多币种布局。
10. 在测试设备检查真实相册选图、头像移除、系统多文件选择及文件保存，再验证备份迁移。自动测试未覆盖所有系统弹窗与真实照片流程。

## 证据记录

每次验证记录代码版本、执行日期与时区、Xcode／系统／设备或模拟器版本、命令或测试类、通过／失败／未执行项，以及可保留的日志或结果包。`screenshots/ledger-*.png` 可帮助理解界面，但静态截图不证明当前测试通过。真机安装、系统文件流程及发布验收需要单独记录。

## 0.4.0 管理、最爱与外观复测

以下为 0.4.0 / build 10 新增的测试定义与复测范围，不表示所有 UI 用例本轮已经执行通过。按实际运行记录通过、失败或未执行；色板检查也不替代各页面的人工外观检查。

| 测试文件 | 用例数 | 新增覆盖 |
| --- | --- | --- |
| [LedgerManagementTests.swift](../Tests/LedgerManagementTests.swift) | 6 | 日期边界与组合筛选、历史资料匹配、无结果和无效范围、所选条数／数量／分币种金额、批量删除与清空物贩保留目录。 |
| [LedgerCatalogManagementTests.swift](../Tests/LedgerCatalogManagementTests.swift) | 3 | 批量成员全身份归档、团体仅本团体归档、历史保留及空选择／未知编号／重复操作。 |
| [LedgerFavoritesTests.swift](../Tests/LedgerFavoritesTests.swift) | 8 | 旧备份缺省最爱、JSON 往返与字段类型、合并、归档排除、兼任价格、稳定排序、新记录优先及历史身份。 |
| [DataManagementUITests.swift](../UITests/DataManagementUITests.swift) | 6 | 物贩筛选多选与取消／确认、筛选变化清选择、清空所有币种、日志多选删除、设置管理入口仍有备份表单。 多选目录归档、取消与确认，并保留历史物贩。 |
| [DataManagementFailureUITests.swift](../UITests/DataManagementFailureUITests.swift) | 2 | 在 Debug 的独立测试资料中以 `OSHICUE_FAIL_BATCH_DELETE=1` 注入实际保存失败；检查原记录、日志正文和当前选择保留，重启后资料不变，解除故障再选择并重试删除。 |
| [LedgerFavoriteUITests.swift](../UITests/LedgerFavoriteUITests.swift) | 2 | 成员最爱跨团体共享与重启保留、顶部多个身份快捷选择、默认最爱与取消最爱。 |
| [AppearanceUITests.swift](../UITests/AppearanceUITests.swift) | 5 | 实际 UIKit 动态色板、浅深色文字与选中控件对比、场馆底色、增强对比和成员代表色的可读性；不操作 App 页面。 |
| [AppearanceScreenUITests.swift](../UITests/AppearanceScreenUITests.swift) | 1 | 在系统浅色或深色外观下实际操作四个模块、成员最爱与选择、编辑、管理和备份；App 不强制主题，界面断言后保存 13 个真实截图附件。 |

```sh
swift test --filter Ledger
xcodebuild test -project OshicueKit.xcodeproj -scheme OshicueKit -destination 'platform=iOS Simulator,id=<SIMULATOR_ID>' -only-testing:OshicueKitUITests/DataManagementUITests -only-testing:OshicueKitUITests/LedgerFavoriteUITests -only-testing:OshicueKitUITests/AppearanceUITests -only-testing:OshicueKitUITests/DataManagementFailureUITests -only-testing:OshicueKitUITests/AppearanceScreenUITests
```

## 新增人工复测步骤

- 在设置打开手账管理和日志管理，切到备份核对原导入／导出；分别组合筛选并选择部分结果，核对确认数量，先取消再确认，重启检查删除结果。物贩确认需分别列出各币种；更改筛选必须清空选择。
- 多选归档兼任成员及一个团体，核对删除范围、其他身份和历史编辑；清空物贩后目录与日志仍在。日志按状态、日期、搜索选删，核对未选正文及其他模块不受影响。
- 设置最爱并从另一兼任身份重新打开，核对共享设置、选择页顶部专区、各身份价格及新记录默认；取消最爱、归档、导出再导入后分别核对结果。旧 JSON 缺少 `isFavorite` 时应未最爱，`schemaVersion` 仍为 `1`。
- 在系统切换浅色与深色，逐一检查四个模块、详情、编辑、选择、管理和备份；同时检查大字号、九语言长文字、头像、代表色、金额、选中按钮与错误提示。记录测试设备和系统版本，不以旧截图替代本次结果。

返回 [README](../README.md) · [手账使用与数据格式](LEDGER.zh-CN.md) · [JSON 模板](../Templates/ledger-template.json)
