# Oshicue Kit for Android

[简体中文](README.md) | [繁體中文](README.zh-Hant.md) | [繁體粵語](README.zh-HK.md) | [English](README.en.md) | [日本語](README.ja.md) | [한국어](README.ko.md) | [ไทย](README.th.md) | [Bahasa Indonesia](README.id.md) | [Bahasa Melayu](README.ms.md)

A native Android app (Kotlin + Jetpack Compose) for event timetables, merchandise spending, and event recaps. Its four tabs are Timetable, Ledger, Diary, and Settings. The current version is **0.4.0 (versionCode 10)**, feature-aligned with the iOS app Oshicue Kit 0.4.0 (build 10), and supports Android 8.0 (API 26) or later.

## Screenshots

| Timetable | My Timetable | Ledger calendar |
| --- | --- | --- |
| ![Timetable](screenshots/timetable.png) | ![My Timetable](screenshots/my-timetable.png) | ![Ledger calendar](screenshots/ledger-calendar.png) |

| New purchase | Ledger statistics | Diary |
| --- | --- | --- |
| ![New purchase](screenshots/ledger-record-form.png) | ![Ledger statistics](screenshots/ledger-statistics.png) | ![Diary](screenshots/diary.png) |

| New entry | Settings |
| --- | --- |
| ![New entry](screenshots/diary-editor.png) | ![Settings](screenshots/settings.png) |

## Features

### Timetable

- All sessions are grouped by venue and LIVE/merchandise. Combine date, multiple-event, and session-type filters. Scroll horizontally for venues and vertically for time.
- Tap a session for full details; double-tap to favorite or unfavorite its group. Favorites are shared across events by `artistID`; My Timetable merges the favorited groups' LIVE and merchandise sessions, placing overlaps side by side with conflict warnings.
- Zoom options are 50%, 70%, 100%, and 150%; the default is 100% (4dp/minute). Dense overlaps widen the board; short blocks prioritize names with automatic font fitting, and full details remain available.
- The current-time line uses real system time, refreshing every 15 seconds. Now switches to today only if the selected events have sessions today.
- Multiple events share the time zone of the first selected event in document order; details can show the original event's local time; sessions crossing midnight are split across the relevant dates in the display time zone.
- Import by pasting JSON or selecting one or more UTF-8 JSON files, then validate, preview, and confirm. A matching event ID replaces that entire event; duplicate event IDs within one batch of files are rejected. Deleting a timetable requires confirmation and clears favorites absent from every remaining event.

### Ledger

- The calendar shows daily and monthly quantities and separate totals by currency. Tap a member card to edit a purchase; multiple records require selection first.
- Manage groups, members, avatars, affiliation-specific colors and default prices, purchase types, and event templates. Use an existing member to add another group affiliation.
- Record a Gregorian date, member and group, type, quantity, currency, and unit price, plus optional event information that can also be taken from a timetable.
- Rankings cover day, month, year, or lifetime, with quantity/spending, combined members/members by group/groups, and type/group filters. Ties use `1, 1, 3`; spending requires one currency and never converts between currencies.
- Historical records retain name, color, event, and price snapshots; member/group deletion is archival, so records and statistics survive and historical records stay editable.
- Members can be marked as favorite; the purchase member chooser lists favorites first. Amounts use integer minor units with `CNY`, `JPY`, `HKD`, `TWD`, `USD`, `KRW`, `THB`, `IDR`, and `MYR`.
- Ledger management covers record management (combined range/member/group/type/currency/event/text filters, batch delete, delete all), catalog cleanup (archival of members/groups), and JSON backups (export, previewed import, conflict rejection).

### Diary

- Create event recaps with a title, body, summary, and tags; mark them Draft or Ready, browse by event date, filter by status, and search text and event information.
- Bring in a ledger event's date, name, city, venue, and address, then edit independently. The diary keeps its own snapshot.
- Autosave runs about 0.5 seconds after editing stops; closing the editor or moving to the background immediately submits pending changes, and failed saves can be retried.
- Prepare to Publish can copy Markdown/plain text, export `.md`/`.txt`, or use the system share sheet.
- Diary management provides filters, batch deletion, and JSON backup import/export.

### Settings and data

- Language selection (9 languages plus Follow system) applies immediately; dates, times, and numbers use the selected locale.
- Timetable import, ledger and diary management, example loading, and version information live here.
- The three modules save independently under `files/OshicueKit/` as `schedule.json`, `ledger.json`, and `diary.json` with atomic writes; core features work offline.

## Building and running

Requirements:

- JDK 17 or later (the Android Studio JBR or JDK 21+ is recommended)
- Android SDK Platform 37 and Build-Tools 36 (Android Studio can install them)
- Create `local.properties` in the repository root with `sdk.dir=/path/to/Android/sdk`, or set the `ANDROID_HOME` environment variable
- The first build downloads dependencies from Maven repositories; the Gradle Wrapper downloads its own Gradle version

```sh
./gradlew :app:assembleDebug        # Build a debug APK
./gradlew :app:installDebug         # Install to a connected device or emulator
./gradlew :app:testDebugUnitTest    # 93 unit tests (ported 1:1 from the iOS test suite)
./gradlew :app:lintDebug            # Static analysis
./gradlew :app:assembleRelease      # Build a release APK
```

### Release signing

The repository contains no signing material. To produce an installable release APK, create `keystore.properties` in the repository root (both it and `*.jks` are excluded via `.gitignore`):

```sh
keytool -genkeypair -v -keystore oshicue-kit-release.jks -alias oshicue-kit \
  -keyalg RSA -keysize 2048 -validity 10000
```

```properties
storeFile=oshicue-kit-release.jks
storePassword=your-password
keyAlias=oshicue-kit
keyPassword=your-password
```

Then run `./gradlew :app:assembleRelease`; the output lands in `app/build/outputs/apk/release/app-release.apk` (without `keystore.properties` the build produces an unsigned APK). Back up the keystore and password — later versions must be signed with the same key.

## Data formats and compatibility

- All three modules use the same JSON formats as the iOS app (`schemaVersion` is `1`); backups exported from one platform import directly into the other, and on-device files share the same structure.
- Timestamps use ISO 8601 with milliseconds (`yyyy-MM-ddTHH:mm:ss.SSSZ`); dates are Gregorian `yyyy-MM-dd`; amounts are integer minor units (`unitPriceMinor`).
- Imports are always validated and previewed before confirmation; a record with an existing ID but different content rejects the entire import so history is never silently overwritten.
- `Templates/` and `docs/` carry over the iOS project's format templates and reference documentation.

## Localization

Nine languages are bundled — Simplified Chinese, Traditional Chinese (Taiwan), Traditional Cantonese (Hong Kong), English, Japanese, Korean, Thai, Indonesian, and Malay — under `app/src/main/res/values-*/strings.xml`.

`tools/convert_localization.py` regenerates the Android resources from the iOS `Localizable.strings` files (including `%@` → `%1$s` positional argument conversion). If you only maintain the Android side, edit the `strings.xml` files directly.

## Repository layout

| Path | Contents |
| --- | --- |
| `app/src/main/kotlin/com/oshicue/kit/core/` | Models, JSON import/export, statistics and rankings, timeline layout |
| `app/src/main/kotlin/com/oshicue/kit/data/` | The three independent stores, atomic writes, recovery backups |
| `app/src/main/kotlin/com/oshicue/kit/i18n/` | Language resolution and string lookup |
| `app/src/main/kotlin/com/oshicue/kit/ui/` | Compose screens: timetable, ledger, diary, settings, and all editing/backup pages |
| `app/src/main/res/` | Nine language resource sets, launcher icons, Unbounded font |
| `app/src/main/assets/` | Bundled import templates |
| `app/src/test/` | Unit tests (import validation, currency precision, ranked ties, timeline layout, UTF-8 handling, and more) |
| `tools/` | Localization conversion script |
| `Templates/`, `docs/` | JSON templates and format documentation carried over from the iOS project |

## Differences from the iOS version

- Platform components: file import/export uses the system file picker, avatars use the system photo picker (downscaled to 512px, JPEG 0.8, stored as Base64 — same as iOS), and sharing uses the system share sheet.
- Accessibility: iOS VoiceOver custom actions map to Compose tap/double-tap gestures and semantics.
- Everything else — interaction flows, edge cases, and JSON data formats — matches the iOS app.

## License

The project keeps the original project's [MIT License](LICENSE); third-party components retain their own licenses. The bundled Unbounded font uses [SIL OFL 1.1](THIRD_PARTY_NOTICES); see [third-party notices](THIRD_PARTY_NOTICES).

The original Oshicue Kit is a native SwiftUI iOS app (0.4.0, build 10); this repository is its complete Android port.
