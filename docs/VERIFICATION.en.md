# Timetable verification guide

[简体中文](VERIFICATION.md) | [繁體中文](VERIFICATION.zh-Hant.md) | [繁體粵語](VERIFICATION.zh-HK.md) | [English](VERIFICATION.en.md) | [日本語](VERIFICATION.ja.md) | [한국어](VERIFICATION.ko.md) | [ไทย](VERIFICATION.th.md) | [Bahasa Indonesia](VERIFICATION.id.md) | [Bahasa Melayu](VERIFICATION.ms.md)

This page describes verification coverage in the current source and how to repeat the checks. A test file's presence does not mean it was run during this update. Historical screenshots are interface references, not proof that the current build passed.

## Reproducible checks

Run from the project root:

```sh
swift test
python3 scripts/verify-localizations.py
```

`swift test` runs Foundation core tests, not iOS interface tests. The localization check covers key sets, nonempty text, format placeholders and `.strings` syntax across all nine languages.

| File | Coverage |
| --- | --- |
| [ScheduleImporterTests.swift](../Tests/ScheduleImporterTests.swift) | Format and field errors, references and duplicate IDs, time offsets, fractional seconds, midnight, daylight saving time, complete event updates, templates and demo seeding. |
| [TimelineLayoutTests.swift](../Tests/TimelineLayoutTests.swift) | Touching boundaries, overlap lanes, minimum concurrent lane count, stable sorting, dense column widths and no horizontal collisions. |
| [ScheduleDeletionTests.swift](../Tests/ScheduleDeletionTests.swift) | Event deletion, shared favorites, same name with different IDs, unknown IDs, empty document encoding and reloading. |

## Native interface checks

Open the project in Xcode and run UI tests on an iOS simulator. Main cases are in [OshicueKitUITests.swift](../UITests/OshicueKitUITests.swift), [TimelineReadabilityUITests.swift](../UITests/TimelineReadabilityUITests.swift) and [ScheduleDeletionUITests.swift](../UITests/ScheduleDeletionUITests.swift).

Check single tap details, double tap favorites, merged session types in My Timetable, invalid imports that cannot be confirmed, valid imports replacing the demo, nine language choices and switching, short sessions and concurrent merchandise readability, deletion confirmation, selection fallback and relaunch with an empty timetable. DEBUG UI tests use a separate schedule file with `OSHICUE_UI_TESTS=1`; `OSHICUE_PRESERVE_UI_DATA=1` preserves test data across launches.

## Additional manual checks

Import multiple files and confirm that an invalid file or duplicate event ID across files does not partially apply. Select events in different time zones separately and together, checking the filter bar's zone. Check midnight crossings and ends exactly at midnight, then increase system text size and repeat detail checks. After deletion, confirm ledger and diary snapshots remain. Relaunch and check timetables and favorites.

Simulator checks do not replace checks on physical devices, across system versions, with large text or with the file picker. Record this run’s actual UI testing, physical-device installation and publishing results separately. The existing [screenshot directory](../screenshots/) contains past interface references.

## Retesting 0.4.0 management, favorites and appearance

The following test definitions and retest scope were added for 0.4.0 / build 10. They do not assert that every UI test ran and passed in this update. Record passed, failed or not run from actual execution; palette checks also do not replace manual inspection of each screen.

| Test file | Cases | Added coverage |
| --- | --- | --- |
| [DataManagementUITests.swift](../UITests/DataManagementUITests.swift) | 6 | Filtered purchase selection with Cancel/Confirm, filter changes clearing selection, clearing all currencies, diary batch deletion and backup forms remaining in settings management. Multiple catalog selections, Cancel/Confirm archival and retained purchase history. |
| [DataManagementFailureUITests.swift](../UITests/DataManagementFailureUITests.swift) | 2 | Inject actual persistence failure with `OSHICUE_FAIL_BATCH_DELETE=1` in isolated Debug test data. Verify that original records, diary bodies and the current selection remain, data is unchanged after relaunch, and deletion succeeds after disabling the failure and selecting again. |
| [AppearanceUITests.swift](../UITests/AppearanceUITests.swift) | 5 | The actual UIKit dynamic palette, text and selected-control contrast in both appearances, venue backgrounds, increased contrast and member-color readability; these tests do not operate app screens. |
| [AppearanceScreenUITests.swift](../UITests/AppearanceScreenUITests.swift) | 1 | Operate all four modules, member favorites and selection, editing, management and backup under system light or dark appearance. The app has no forced theme; screen assertions precede 13 actual screenshot attachments. |

```sh
xcodebuild test -project OshicueKit.xcodeproj -scheme OshicueKit -destination 'platform=iOS Simulator,id=<SIMULATOR_ID>' -only-testing:OshicueKitUITests/AppearanceUITests -only-testing:OshicueKitUITests/DataManagementFailureUITests -only-testing:OshicueKitUITests/AppearanceScreenUITests
```

## Additional manual retest steps

- Open both management screens from Settings and check the original import/export under Backup. Combine filters, select part of the results and verify confirmation counts; cancel first, then confirm and relaunch. Purchase confirmation must separate currencies, and changing filters must clear selection.
- Batch archive a shared member and one group; check the scope, other affiliations and historical editing. Clear purchases and verify that catalogs and diary entries remain. Delete diary results by status, date and search; verify unselected bodies and other modules.
- Set a favorite, reopen it through another affiliation and check the shared preference, top chooser section, affiliation prices and new-record default. Check removal, archival, export and reimport. Old JSON without `isFavorite` must mean not favorite and `schemaVersion` must remain `1`.
- Switch system light/dark appearance and inspect all four modules, details, editors, selectors, management and backup screens. Also check larger text, long labels in all nine languages, avatars, member colors, amounts, selected buttons and errors. Record the device and system version; old screenshots cannot replace current results.

---

[Back to README](../README.en.md) · [Import format](IMPORT.en.md) · [Verification guide](VERIFICATION.en.md) · [Display and zoom](TIMETABLE-READABILITY.en.md) · [Delete timetables](SCHEDULE-DELETION.en.md)

Templates: [Timetable](../Templates/timetable-template.json) · [Fixed demo](../Templates/demo-timetable.json) · [Readability checks](../Templates/timetable-readability.json)
