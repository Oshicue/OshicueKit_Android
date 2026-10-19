# Oshicue Kit · Android

[简体中文](README.md) | [繁體中文](README.zh-Hant.md) | [繁體粵語](README.zh-HK.md) | [English](README.en.md) | [日本語](README.ja.md) | [한국어](README.ko.md) | [ไทย](README.th.md) | [Bahasa Indonesia](README.id.md) | [Bahasa Melayu](README.ms.md)

A native Android app for organizing concert and travel plans, viewing event timetables, tracking merchandise purchases, and writing event diaries. Built with **Kotlin, Jetpack Compose, and Material 3**, it has five tabs: Agenda, Timetable, Ledger, Diary, and Settings.

Current version: **0.5.3 (Build 16)**. Supports **Android 8.0 (API 26) and later**. The application ID is `com.oshicue.kit`.

## Quick start

1. Install the signed APK, or build from source as described below.
2. The first launch loads fictional timetable and ledger examples; Agenda and Diary start empty.
3. Change the language in Settings, or open a module's management page to import your own JSON files.
4. Export agenda, ledger, and diary backups regularly, and keep your original timetable files.

The first import of your own timetable replaces the examples; later imports update or add events by ID. The first import of your own ledger replaces untouched examples; examples you have edited are retained and merged. Names, dates, flights, and places in the examples are for trying the app. Replace them with your actual information before use.

## Main features

### Agenda

- Manage Live events, flights, hotels, restaurants, and other places in a calendar, with timed and all-day activities, time zones, addresses, and notes. Agenda is independent of the timetable's “My schedule” selections.
- Record departure and arrival times, time zones, and airports separately for flights; hotels use check-in and checkout times as itinerary milestones.
- Search for cities and places, then view locations or routes in an external map app. Search results do not set the time zone automatically; check it manually.
- See gaps or overlaps between consecutive timed activities. All-day activities are excluded from travel segment comparisons. In-app route duration estimates and live flight lookups are currently unavailable.
- Save a separate copy to the system calendar through the Android calendar editor; edit the copy and the app's record independently.
- Search, combine filters, delete multiple entries, preview JSON imports, and back up the complete agenda.

### Timetable

- Filter by date, event, and LIVE/merchandise type; scroll horizontally through venues and vertically through time.
- Tap a session for details. Double-tap or long-press to add it to or remove it from “My schedule”; the detail page offers the same action.
- Overlapping sessions appear side by side with conflict indicators. Zoom options are 50%, 70%, 100%, and 150%.
- The current-time line refreshes every 15 seconds. Sessions crossing midnight are split according to the display time zone. A multi-event view uses the time zone of the first selected event in document order.
- Paste JSON or import multiple files, with validation and a preview. An event with the same ID is replaced in full while other events remain; duplicate event IDs within one batch are rejected.
- Deleting an event requires confirmation and clears its session selections while preserving Ledger and Diary. There is currently no timetable export action; keep your original import files.

### Ledger

- View daily and monthly merchandise quantities and spending by currency in a calendar. Manage groups, members, avatars, merchandise types, and event details.
- A member can belong to multiple groups, with a separate color and default price for each identity; favorite-member status is shared across identities.
- Record the date, quantity, currency, unit price, and an event snapshot. For a new record, the app first looks for a price on or before the selected date for the same member, group, type, and currency, then uses the remembered price for new entries, the member's default price, or 0. You can edit the unit price manually.
- Rankings support daily, monthly, yearly, and lifetime views, with totals by member, member within each group, or group, ranked by quantity or spending.
- Supports `CNY`, `JPY`, `HKD`, `TWD`, `USD`, `KRW`, `THB`, `IDR`, and `MYR`. Totals are calculated separately for each currency, without exchange-rate conversion. `JPY` and `KRW` use whole numbers; the others use two decimal places. JSON's `unitPriceMinor` stores integer amounts in the currency's smallest unit.
- Removing a member or group archives the relevant details and identities while keeping historical purchase snapshots and statistics. Record management includes filters, bulk deletion, complete JSON backups, and imports.

### Diary

- Write titles, body text, summaries, and tags; use the “Drafts” and “Ready” statuses, browse by date, and search or filter.
- Bring in the date, name, city, venue, and address from a ledger event; the diary keeps its own snapshot.
- Changes save automatically about 0.5 seconds after you stop editing. Finishing or closing the editor submits pending changes; a failed save prompts you to retry.
- Copy Markdown or plain text, export `.md` or `.txt`, or use Android system sharing. “Ready” is a local status; sharing does not publish automatically.
- Manage entries in bulk and create complete JSON backups. Identical content with the same ID is deduplicated; conflicting content with the same ID causes the entire import to be rejected.

## JSON templates and backups

| Content | Bundled file |
| --- | --- |
| Agenda template | [agenda-template.json](app/src/main/assets/templates/agenda-template.json) |
| Timetable template | [timetable-template.json](app/src/main/assets/templates/timetable-template.json) |
| Example timetable | [demo-timetable.json](app/src/main/assets/templates/demo-timetable.json) |
| Ledger template | [ledger-template.json](app/src/main/assets/templates/ledger-template.json) |

JSON files use UTF-8. Import them through the corresponding module, preview them, and then confirm. Each module has a different JSON structure; files are not interchangeable even when they share `schemaVersion: 1`. For Diary, create an entry and export a backup as a format reference; the source does not include a separate diary template.

## Local data and settings

Data is stored in the app's private `filesDir/OshicueKit/` directory as `agenda.json`, `schedule.json`, `ledger.json`, `diary.json`, and `settings.json`. The language and last-opened tab are remembered on the device, as are timetable selections.

The app follows the system language by default and falls back to English for unsupported languages; you can manually choose from nine languages. The interface follows the system's light or dark appearance and responds to the system's enhanced contrast setting on Android 14 and later.

Core records work offline and require no account. In-app cloud sync, automatic cloud backups, push reminders, and image-based imports are not currently available. Place search depends on the device's geocoding service. External maps, routes, and calendar features depend on installed apps and services, which may require a network connection. Export backups manually before uninstalling the app or clearing its data.

## Build from source

The following are the current project settings and the environment used for this package:

| Item | Version |
| --- | --- |
| App version / `versionCode` | `0.5.3` / `16` |
| Minimum / target / compile Android API | `26` / `37` / `37` |
| Android Gradle Plugin | `9.4.1` |
| Kotlin Compose plugin | `2.2.10` |
| Compose BOM | `2026.09.00` |
| Java source and bytecode target | `17` |
| Gradle / JBR used for this verification | `9.8.1` / `25.0.3` |

1. Open this directory in Android Studio, then install Android SDK Platform 37 and the required build tools.
2. Use a suitable JDK to run Gradle. This verification used Android Studio's bundled JBR. The Java compilation target `17` and the JDK version that runs Gradle are separate settings.
3. Let Android Studio generate `local.properties`, or set `sdk.dir` to the absolute path of your local SDK. Do not commit this file.
4. Add your installed Gradle to `PATH` and run the following commands from this directory. Resolving dependencies for the first time requires a network connection.

The source does not include a Gradle Wrapper, so these commands use `gradle`:

```sh
gradle :app:assembleDebug
gradle :app:testDebugUnitTest :app:lintDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`. The default build uses debug signing. The project currently has no production release signing configuration; configure it yourself before release.

At packaging, the corresponding source passed **166 unit tests**, and the APK passed signature and alignment checks. The repository does not include an Android UI automation test suite. Verify maps, the system calendar, file selection, and sharing on your target devices.

When uploading to GitHub, exclude `local.properties`, `.gradle/`, `.kotlin/`, `build/`, `app/build/`, local IDE files, and signing keys.

## Project structure

| Path | Contents |
| --- | --- |
| `app/src/main/java/com/oshicue/kit/core/` | Data models, JSON, imports, statistics, and local persistence |
| `app/src/main/java/com/oshicue/kit/ui/` | Compose screens, editors, management, and file operations |
| `app/src/main/assets/localization/` | Interface text in nine languages |
| `app/src/main/assets/templates/` | Agenda, timetable, and ledger JSON templates |
| `app/src/main/res/` | Icons, fonts, and theme resources |
| `app/src/test/` | Core unit tests |

## Documentation languages and font license

Simplified Chinese is the source document; the other eight READMEs correspond to this page. The language codes are `zh-Hans`, `zh-Hant`, `zh-HK`, `en`, `ja`, `ko`, `th`, `id`, and `ms`. JSON keys, IDs, enum values, commands, and paths are not translated.

The bundled Unbounded font uses the [SIL Open Font License 1.1](app/src/main/assets/fonts-OFL.txt). Third-party components retain their respective licenses.
