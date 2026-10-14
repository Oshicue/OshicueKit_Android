# Oshicue Kit for Android

[简体中文](README.md) | [English](README.en.md)

原生 Android 应用（Kotlin + Jetpack Compose），用于查看活动时间表、记录物贩消费和整理活动日志。四个页签依次为时间表、手账、日志、设置。当前版本为 **0.4.0（versionCode 10）**，与 iOS 版 Oshicue Kit 0.4.0（build 10）功能对齐，支持 Android 8.0（API 26）及以上。

## 截图

| 时间表 | 我的日程 | 手账日历 |
| --- | --- | --- |
| ![时间表](screenshots/timetable.png) | ![我的日程](screenshots/my-timetable.png) | ![手账日历](screenshots/ledger-calendar.png) |

| 新增物贩 | 手账统计 | 日志 |
| --- | --- | --- |
| ![新增物贩](screenshots/ledger-record-form.png) | ![手账统计](screenshots/ledger-statistics.png) | ![日志](screenshots/diary.png) |

| 新建日志 | 设置 |
| --- | --- |
| ![新建日志](screenshots/diary-editor.png) | ![设置](screenshots/settings.png) |

## 功能

### 时间表

- 全部日程按场馆和 LIVE／物贩分区；日期、活动多选与类型筛选可组合使用。横向查看场馆，纵向查看时间。
- 单击场次查看完整详情；双击收藏或取消收藏团体。收藏按 `artistID` 跨活动共享；「我的日程」合并显示收藏团体的 LIVE 与物贩场次，重叠时并排显示并提示冲突。
- 时间轴提供 50%、70%、100%、150% 缩放（默认 100%，即 4dp／分钟），并发较多时横向扩宽，短场次优先显示名称并自动缩放字号，详情保留完整资料。
- 当前时间线使用真实系统时间，每 15 秒更新；「现在」仅在所选活动有今天排期时切换到今天。
- 多活动统一使用文档顺序中首个所选活动的时区；详情可查看原活动当地时间；跨午夜场次按展示时区拆分到相应日期。
- 导入支持粘贴 JSON 或选择一个／多个 UTF-8 JSON 文件，先校验和预览，再确认应用；相同活动 ID 整体替换该活动，其余保留，同批重复活动 ID 会被拒绝。删除日程表需确认，并清理不再属于任何剩余活动的收藏。

### 手账

- 日历查看每日与每月数量及分币种金额；点选成员卡片编辑物贩，多笔记录先选择再编辑。
- 管理团体、成员、头像、各团体身份的代表色与默认价格、物贩类型和活动模板；兼任成员通过关联已有成员添加身份。
- 记录日期、成员与团体、类型、数量、币种、单价及可选的活动信息，也可从时间表带入活动资料。
- 排行榜按日／月／年／生涯，支持数量或消费、成员合计／成员分团／团体、类型与团体筛选；并列名次为 `1、1、3`；消费选择单一币种，不做汇率换算。
- 历史记录保存名称、颜色、活动与价格快照；删除成员／团体为归档语义，历史记录与统计保留且仍可编辑。
- 成员可标记「最爱成员」，记录成员选择页置顶显示；金额使用整数最小货币单位，支持 `CNY`、`JPY`、`HKD`、`TWD`、`USD`、`KRW`、`THB`、`IDR`、`MYR`。
- 手账管理提供记录管理（按日期范围、成员、团体、类型、币种、活动与文本组合筛选后批量删除、删除全部）、资料清理（归档成员／团体）与 JSON 备份（导出、导入预览、冲突拒绝）。

### 日志

- 新建活动 repo，编辑标题、正文、摘要和标签，标记为「草稿」或「已整理」；按活动日期浏览、按状态筛选、搜索正文与活动资料。
- 可从手账活动带入日期、名称、城市、场馆与地址，再独立修改；日志保存自己的快照。
- 停止编辑约 0.5 秒后自动保存；关闭编辑器或进入后台立即提交；保存失败可重试。
- 「准备发布」可复制 Markdown／纯文本、导出 `.md`／`.txt` 或调用系统分享。
- 日志管理提供筛选、批量删除与 JSON 备份导入导出。

### 设置与数据

- 语言切换（9 种语言 + 跟随系统）即时生效；日期、时间与数字按所选地区格式化。
- 时间表导入、手账管理、日志管理、示例载入与版本信息统一入口。
- 三个模块独立保存在本机 `files/OshicueKit/` 下的 `schedule.json`、`ledger.json`、`diary.json`，原子写入；核心功能离线可用。

## 构建与运行

要求：

- JDK 17 及以上（推荐 Android Studio 自带的 JBR 或 JDK 21+）
- Android SDK Platform 37 与 Build-Tools 36（Android Studio 可自动安装）
- 在仓库根目录创建 `local.properties` 写入 `sdk.dir=/path/to/Android/sdk`，或设置 `ANDROID_HOME` 环境变量
- 首次构建需要网络访问 Maven 仓库；Gradle Wrapper 会自动下载对应版本

```sh
./gradlew :app:assembleDebug        # 构建调试 APK
./gradlew :app:installDebug         # 安装到已连接的设备或模拟器
./gradlew :app:testDebugUnitTest    # 93 项单元测试（与 iOS 单元测试一一对应）
./gradlew :app:lintDebug            # 静态检查
./gradlew :app:assembleRelease      # 构建发布 APK
```

### 发布签名

仓库不包含签名材料。若要产出可安装、可分发的发布 APK，请在仓库根目录创建 `keystore.properties`（该文件与 `*.jks` 已在 `.gitignore` 中排除）：

```sh
keytool -genkeypair -v -keystore oshicue-kit-release.jks -alias oshicue-kit \
  -keyalg RSA -keysize 2048 -validity 10000
```

```properties
storeFile=oshicue-kit-release.jks
storePassword=你的密码
keyAlias=oshicue-kit
keyPassword=你的密码
```

然后运行 `./gradlew :app:assembleRelease`，输出位于 `app/build/outputs/apk/release/app-release.apk`（未配置 `keystore.properties` 时输出未签名 APK）。请妥善备份 `oshicue-kit-release.jks` 与密码，后续版本需使用同一签名。

## 数据格式与兼容性

- 三个模块的 JSON 格式与 iOS 版完全一致（`schemaVersion` 为 `1`），应用内导出／导入的备份文件可与 iOS 版互相使用；本机数据文件结构也相同。
- 时间戳使用带毫秒的 ISO 8601（`yyyy-MM-ddTHH:mm:ss.SSSZ`）；日期为公历 `yyyy-MM-dd`；金额为整数最小货币单位（`unitPriceMinor`）。
- 导入一律先校验、预览，再确认应用；同 ID 不同内容的记录会拒绝整次导入，避免覆盖历史。
- `Templates/` 与 `docs/` 沿用 iOS 版的格式模板与说明文档，可直接参考。

## 本地化

内置简体中文、繁體中文（台灣）、繁體粵語（香港）、English、日本語、한국어、ไทย、Bahasa Indonesia、Bahasa Melayu 共 9 种语言，资源位于 `app/src/main/res/values-*/strings.xml`。

`tools/convert_localization.py` 用于从 iOS 版的 `Localizable.strings` 重新生成本仓库的语言资源（含 `%@` → `%1$s` 位置化参数转换）。如果只维护 Android 版，直接编辑对应的 `strings.xml` 即可。

## 目录结构

| 路径 | 内容 |
| --- | --- |
| `app/src/main/kotlin/com/oshicue/kit/core/` | 模型、JSON 导入导出、统计与排行、时间轴布局算法 |
| `app/src/main/kotlin/com/oshicue/kit/data/` | 三个独立 Store、原子写入与恢复备份 |
| `app/src/main/kotlin/com/oshicue/kit/i18n/` | 语言解析与文案查询 |
| `app/src/main/kotlin/com/oshicue/kit/ui/` | Compose 界面：时间表、手账、日志、设置及全部编辑与备份页面 |
| `app/src/main/res/` | 9 种语言资源、启动图标、Unbounded 字体 |
| `app/src/main/assets/` | 内置导入模板 |
| `app/src/test/` | 单元测试（导入校验、币种精度、排名并列、时间轴算法、UTF-8 处理等） |
| `tools/` | 本地化资源转换脚本 |
| `Templates/`、`docs/` | JSON 模板与格式说明（沿用 iOS 版） |

## 与 iOS 版的差异

- 平台组件：文件导入导出使用系统文件选择器，头像使用系统照片选择器（下采样到 512px、JPEG 0.8、Base64 存储，与 iOS 相同），分享使用系统分享面板。
- 无障碍：iOS 的 VoiceOver 自定义动作对应为 Compose 的轻点／双击手势与语义标签。
- 其余交互流程、边界条件与 JSON 数据格式与 iOS 版保持一致。

## 开源许可

项目沿用原项目的 [MIT License](LICENSE)；第三方组件保留各自许可。内置的 Unbounded 字体使用 [SIL OFL 1.1](THIRD_PARTY_NOTICES)，详见 [第三方声明](THIRD_PARTY_NOTICES)。

Oshicue Kit 原版为原生 SwiftUI iOS 应用（0.4.0，build 10），本仓库是其 Android 平台的完整移植。
