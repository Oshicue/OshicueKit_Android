# Diary and global settings verification guide

[简体中文](DIARY-VERIFICATION.md) | [繁體中文](DIARY-VERIFICATION.zh-Hant.md) | [繁體粵語](DIARY-VERIFICATION.zh-HK.md) | [English](DIARY-VERIFICATION.en.md) | [日本語](DIARY-VERIFICATION.ja.md) | [한국어](DIARY-VERIFICATION.ko.md) | [ไทย](DIARY-VERIFICATION.th.md) | [Bahasa Indonesia](DIARY-VERIFICATION.id.md) | [Bahasa Melayu](DIARY-VERIFICATION.ms.md)

This page describes the current repository's test coverage and reproduction steps; it does not claim that tests have passed in the current environment. The project is configured as 0.4.0 / build 10 with a minimum of iOS 17; the core Swift Package also supports macOS 14. Record the commit, Xcode version, device, and test output for each actual run.

## Core tests

[DiaryTests.swift](../Tests/DiaryTests.swift) defines 18 diary tests. Run from the project root:

```sh
swift test --filter DiaryTests
```

| Coverage | Checks |
| --- | --- |
| Drafts and event snapshots | Empty diaries and incomplete drafts are valid; event details are independent; missing valid event dates fall back to the selected date. |
| Text and timestamps | Long Unicode bodies, whitespace, and line breaks survive round trips; deduplication uses millisecond precision; time zone offsets represent the same instant. |
| Merging and conflicts | Identical entries are deduplicated and new entries added; different bodies, summaries, tags, statuses, addresses, or modification times under one ID reject the whole batch. |
| Data validation | Duplicate or empty IDs, invalid dates and chronological order, versions, required fields, field types, timestamps, invalid JSON and UTF-8, and the 20 MiB limit. |
| Organization and export | Tag splitting and deduplication preserve Japanese voicing and accent differences; multiple search words match across fields; plain text and Markdown include event details, summary, and original body, with escaped titles. |

The duplicate, conflict, storage size, and export behavior under test reflects current code. Update this page when the test count or coverage changes.

## Native UI tests

[DiaryUITests.swift](../UITests/DiaryUITests.swift) defines 5 tests:

1. Choose a ledger event, enter multiline text, a summary, and tags, then background the app. After relaunch, check event details and text and ensure there is only one entry.
2. Preview and import JSON; repeat import deduplicates, changing the body under the same ID is rejected, and the original body survives relaunch.
3. Cancel and confirm diary deletion; deletion survives relaunch while ledger events and purchase records remain.
4. Open “Diary management” and select “Backup” from the fourth Settings tab, switch to English across modules, and preserve the original diary body.
5. Check the diary title, new-entry button, fourth Settings tab, and empty diary state in each of nine languages. This does not mean every UI string has been reviewed manually.

List the available local simulators, then replace `<SIMULATOR_ID>` with a real device ID:

```sh
xcrun simctl list devices available
xcodebuild test -project OshicueKit.xcodeproj -scheme OshicueKit -destination 'platform=iOS Simulator,id=<SIMULATOR_ID>' -only-testing:OshicueKitUITests/DiaryUITests
```

In Debug, tests use `OSHICUE_UI_TESTS=1` and the separate file `ui-test-diary.json`; relaunch preservation uses `OSHICUE_PRESERVE_UI_DATA=1`. These environment variables isolate tests and are not user backup controls.

## Language checks, manual checks, and evidence

Run this script on macOS to check the key sets, nonempty values, placeholders, and `.strings` parsing across nine languages. It does not assess translation meaning or visual layout:

```sh
python3 scripts/verify-localizations.py
```

Manual checks should include autosave and retry after failure, event selection and independent snapshots, search and filters, deletion cancellation and confirmation, multiple-file merging and conflicts, empty-backup rejection, actual `.md`／`.txt`／JSON saving and rereading, cancellation of system sharing, and long text and dates in all nine languages. Do not select a share recipient without authorization.

The existing [diary list](../screenshots/diary-list.png), [editor](../screenshots/diary-editor.png), [event details](../screenshots/diary-editor-event.png), [backup preview](../screenshots/diary-backup-preview.png), [conflict message](../screenshots/diary-backup-conflict.png), and [global settings](../screenshots/global-settings.png) images are repository reference screenshots and do not prove the current build has passed. Share delivery, installation on physical devices, publication, and autosave failure recovery cannot be established by core tests alone; record their results separately when actually performed.

## Retesting 0.4.0 management, favorites and appearance

The following test definitions and retest scope were added for 0.4.0 / build 10. They do not assert that every UI test ran and passed in this update. Record passed, failed or not run from actual execution; palette checks also do not replace manual inspection of each screen.

| Test file | Cases | Added coverage |
| --- | --- | --- |
| [DiaryManagementTests.swift](../Tests/DiaryManagementTests.swift) | 4 | Combined status/date/search filters, invalid ranges, deletion preserving other entry bodies and event context, and empty diary backups. |
| [DataManagementUITests.swift](../UITests/DataManagementUITests.swift) | 6 | Filtered purchase selection with Cancel/Confirm, filter changes clearing selection, clearing all currencies, diary batch deletion and backup forms remaining in settings management. Multiple catalog selections, Cancel/Confirm archival and retained purchase history. |
| [DataManagementFailureUITests.swift](../UITests/DataManagementFailureUITests.swift) | 2 | Inject actual persistence failure with `OSHICUE_FAIL_BATCH_DELETE=1` in isolated Debug test data. Verify that original records, diary bodies and the current selection remain, data is unchanged after relaunch, and deletion succeeds after disabling the failure and selecting again. |
| [AppearanceUITests.swift](../UITests/AppearanceUITests.swift) | 5 | The actual UIKit dynamic palette, text and selected-control contrast in both appearances, venue backgrounds, increased contrast and member-color readability; these tests do not operate app screens. |

```sh
swift test --filter Diary
xcodebuild test -project OshicueKit.xcodeproj -scheme OshicueKit -destination 'platform=iOS Simulator,id=<SIMULATOR_ID>' -only-testing:OshicueKitUITests/DataManagementUITests -only-testing:OshicueKitUITests/AppearanceUITests -only-testing:OshicueKitUITests/DataManagementFailureUITests
```

## Additional manual retest steps

- Open both management screens from Settings and check the original import/export under Backup. Combine filters, select part of the results and verify confirmation counts; cancel first, then confirm and relaunch. Purchase confirmation must separate currencies, and changing filters must clear selection.
- Batch archive a shared member and one group; check the scope, other affiliations and historical editing. Clear purchases and verify that catalogs and diary entries remain. Delete diary results by status, date and search; verify unselected bodies and other modules.
- Set a favorite, reopen it through another affiliation and check the shared preference, top chooser section, affiliation prices and new-record default. Check removal, archival, export and reimport. Old JSON without `isFavorite` must mean not favorite and `schemaVersion` must remain `1`.
- Switch system light/dark appearance and inspect all four modules, details, editors, selectors, management and backup screens. Also check larger text, long labels in all nine languages, avatars, member colors, amounts, selected buttons and errors. Record the device and system version; old screenshots cannot replace current results.

Back to [README](../README.en.md) · Related: [Diary](DIARY.en.md) · [Overall verification](VERIFICATION.en.md) · [Diary template](../Templates/diary-template.json)
