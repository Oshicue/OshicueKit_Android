# Oshicue Kit · Android

[简体中文](README.md) | [繁體中文](README.zh-Hant.md) | [繁體粵語](README.zh-HK.md) | [English](README.en.md) | [日本語](README.ja.md) | [한국어](README.ko.md) | [ไทย](README.th.md) | [Bahasa Indonesia](README.id.md) | [Bahasa Melayu](README.ms.md)

用于整理演出与出行日程、查看活动时间表、记录物贩消费和撰写活动日志的原生 Android 应用。使用 **Kotlin、Jetpack Compose 和 Material 3** 开发，提供日程、时间表、手账、日志、设置五个页签。

当前版本：**0.5.3（Build 16）**。支持 **Android 8.0（API 26）及以上**，应用 ID 为 `com.oshicue.kit`。

## 快速开始

1. 安装已签名的 APK，或按照下方说明从源码构建。
2. 首次启动会载入虚构的时间表和手账示例；日程与日志初始为空。
3. 在设置切换语言，或进入各模块的管理页面导入自己的 JSON 文件。
4. 定期导出日程、手账和日志备份，并保管原始时间表文件。

首次正式导入时间表会替换示例，此后按活动 ID 更新或追加。手账首次正式导入会替换未经修改的示例；手动修改过的示例会保留并合并。示例中的名称、日期、航班和地点仅供体验，使用前请换成实际资料。

## 主要功能

### 日程

- 在日历中管理 Live、航班、酒店、餐厅和其他地点，支持定时与全天活动、时区、地址和备注。日程独立于时间表中的「我的日程」选择。
- 航班可分别记录出发、到达时间、时区及机场；酒店以入住和退房时间作为行程节点。
- 支持城市和地点搜索，在外部地图应用中查看位置或路线。搜索结果不会自动设置时区，请手动确认。
- 显示相邻定时活动之间的空档或重叠；全天活动不参与交通区段比较。当前未接入应用内路线耗时估算或实时航班查询。
- 通过 Android 日历编辑器另存系统日历副本；副本与应用内记录分别编辑。
- 提供搜索、组合筛选、多选删除、JSON 导入预览和全部日程备份。

### 时间表

- 按日期、活动和 LIVE／物贩类型筛选，横向查看场馆，纵向查看时间。
- 单击场次查看详情；双击或长按加入、移出「我的日程」，详情页也提供相同操作。
- 重叠场次并排显示并提示冲突；支持 50%、70%、100%、150% 缩放。
- 当前时间线每 15 秒刷新；跨午夜场次按展示时区拆分。多活动视图采用所选活动在文档顺序中的第一个活动的时区。
- 支持粘贴或多文件导入、校验与预览。同 ID 活动整体更新，其他活动保留；同批重复活动 ID 会被拒绝。
- 删除活动需确认，清除该活动的场次选择，保留手账和日志。当前没有时间表导出入口，请保留原始导入文件。

### 手账 · Ledger

- 通过日历查看每日、每月物贩数量和分币种消费，管理团体、成员、头像、物贩类型与活动资料。
- 同一成员可加入多个团体，各身份分别设置颜色和默认价格；最爱成员设置由各身份共享。
- 记录日期、数量、币种、单价和活动快照。新记录优先参考所选日期当天或之前的同成员、团体、类型及币种价格，再使用记住的新增价格、成员默认价或 0；单价可手动修改。
- 排行榜支持日、月、年、生涯，以及成员合计、成员分团、团体、数量或消费等维度。
- 支持 `CNY`、`JPY`、`HKD`、`TWD`、`USD`、`KRW`、`THB`、`IDR`、`MYR`；按币种分别统计，不换算汇率。`JPY`、`KRW` 使用整数，其余使用两位小数；JSON 的 `unitPriceMinor` 使用整数最小货币单位。
- 移除成员或团体会归档相关资料与身份，保留历史购买快照和统计。记录管理支持筛选、批量删除、完整 JSON 备份和导入。

### 日志 · Diary

- 撰写标题、正文、摘要与标签，使用「草稿」「已整理」状态，按日期浏览并搜索或筛选。
- 可从手账活动带入日期、名称、城市、场馆与地址；日志保存独立快照。
- 停止编辑约 0.5 秒后自动保存，完成或关闭编辑器时提交待保存内容；保存失败会提示重试。
- 复制 Markdown／纯文本，导出 `.md`／`.txt`，或使用 Android 系统分享。「已整理」仅为本地状态，分享不会自动发布。
- 提供批量管理和完整 JSON 备份。同 ID 同内容去重，同 ID 内容冲突会拒绝整次导入。

## JSON 模板与备份

| 内容 | 内置文件 |
| --- | --- |
| 日程模板 | [agenda-template.json](app/src/main/assets/templates/agenda-template.json) |
| 时间表模板 | [timetable-template.json](app/src/main/assets/templates/timetable-template.json) |
| 示例时间表 | [demo-timetable.json](app/src/main/assets/templates/demo-timetable.json) |
| 手账模板 | [ledger-template.json](app/src/main/assets/templates/ledger-template.json) |

JSON 文件使用 UTF-8，通过对应模块入口导入，先预览再确认。各模块的 JSON 结构不同，即使同为 `schemaVersion: 1` 也不能混用。日志可先新建一篇，再导出备份作为格式参考；源码未附单独的日志模板。

## 本地数据与设置

数据保存在应用私有目录 `filesDir/OshicueKit/` 下的 `agenda.json`、`schedule.json`、`ledger.json`、`diary.json` 和 `settings.json`。语言和上次打开的页签会在本机记住；时间表选择也保存在本机。

默认跟随系统语言，不支持的语言回退到英语；可手动切换九种语言。界面跟随系统浅色／深色外观，并在 Android 14 及以上响应系统增强对比度设置。

核心记录可离线使用，无需账户。当前未提供应用内云同步、自动云备份、推送提醒或识图导入。地点搜索依赖设备的地理编码服务；外部地图、路线和日历功能取决于已安装的应用及服务，相关服务可能需要网络。卸载或清除应用数据前，请手动导出备份。

## 从源码构建

以下为当前工程配置及本次打包所用环境：

| 项目 | 版本 |
| --- | --- |
| 应用版本 / `versionCode` | `0.5.3` / `16` |
| 最低 / 目标 / 编译 Android API | `26` / `37` / `37` |
| Android Gradle Plugin | `9.4.1` |
| Kotlin Compose 插件 | `2.2.10` |
| Compose BOM | `2026.09.00` |
| Java 源码与字节码目标 | `17` |
| 本次验证使用的 Gradle / JBR | `9.8.1` / `25.0.3` |

1. 在 Android Studio 中打开本目录，安装 Android SDK Platform 37 和所需构建工具。
2. 使用合适的 Gradle 运行时 JDK；本次验证使用 Android Studio 自带的 JBR。Java 编译目标 `17` 与运行 Gradle 的 JDK 版本是两个设置。
3. 让 Android Studio 生成 `local.properties`，或自行填写 `sdk.dir` 为本机 SDK 绝对路径。不要提交该文件。
4. 将已安装的 Gradle 加入 `PATH`，在本目录运行以下命令。首次解析依赖需要网络。

源码未附带 Gradle Wrapper，因此以下命令使用 `gradle`：

```sh
gradle :app:assembleDebug
gradle :app:testDebugUnitTest :app:lintDebug
```

APK 输出位置：`app/build/outputs/apk/debug/app-debug.apk`。默认构建使用调试签名；当前工程未配置正式发布签名，发布前需自行配置。

对应源码在本次打包时通过 **166 项单元测试**，APK 签名与对齐校验通过。仓库未提供 Android 界面自动化测试套件；地图、系统日历、文件选择和分享应在目标设备上验证。

上传 GitHub 时排除 `local.properties`、`.gradle/`、`.kotlin/`、`build/`、`app/build/`、本机 IDE 文件及签名密钥。

## 项目结构

| 路径 | 内容 |
| --- | --- |
| `app/src/main/java/com/oshicue/kit/core/` | 数据模型、JSON、导入、统计与本地保存 |
| `app/src/main/java/com/oshicue/kit/ui/` | Compose 页面、编辑器、管理和文件操作 |
| `app/src/main/assets/localization/` | 九种语言的界面文案 |
| `app/src/main/assets/templates/` | 日程、时间表与手账 JSON 模板 |
| `app/src/main/res/` | 图标、字体与主题资源 |
| `app/src/test/` | 核心单元测试 |

## 文档语言与字体许可

简体中文为源稿，其余八份 README 与本页内容对应。语言代码为 `zh-Hans`、`zh-Hant`、`zh-HK`、`en`、`ja`、`ko`、`th`、`id`、`ms`。JSON 键、ID、枚举值、命令和路径不翻译。

内置 Unbounded 字体采用 [SIL Open Font License 1.1](app/src/main/assets/fonts-OFL.txt)。第三方组件遵循各自的许可。
