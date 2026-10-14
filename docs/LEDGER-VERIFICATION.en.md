# Ledger verification guide

[简体中文](LEDGER-VERIFICATION.md) | [繁體中文](LEDGER-VERIFICATION.zh-Hant.md) | [繁體粵語](LEDGER-VERIFICATION.zh-HK.md) | [English](LEDGER-VERIFICATION.en.md) | [日本語](LEDGER-VERIFICATION.ja.md) | [한국어](LEDGER-VERIFICATION.ko.md) | [ไทย](LEDGER-VERIFICATION.th.md) | [Bahasa Indonesia](LEDGER-VERIFICATION.id.md) | [Bahasa Melayu](LEDGER-VERIFICATION.ms.md)

This page describes existing test coverage and repeatable acceptance checks. It is based on the current source code. The presence of tests does not mean they passed in this run; old release reports, temporary result bundles and screenshots are not evidence that the current version passed. Use the latest execution logs for actual results.

## Core tests

Run from the project root:

```sh
swift test --filter Ledger
```

[LedgerTests.swift](../Tests/LedgerTests.swift) contains 16 tests and [LedgerDeletionTests.swift](../Tests/LedgerDeletionTests.swift) contains 7, covering:

- Example/template encoding round trips, date formats, leap days, time zones and day/month/year/lifetime boundaries.
- Daily type summaries, combined and per-group member totals, group rankings, separate currency totals, competition ranks, stable ties and zero-spending shares.
- Minor-unit parsing, rejection of excess decimal places, zero prices, invalid quantity/price and purchase/aggregate overflow.
- Name, color and unit-price snapshots; catalog updates do not rewrite history.
- Catalog merges by ID, purchase deduplication, conflicting records with the same ID and atomic failure of invalid merges.
- Required fields, duplicate IDs, unique affiliations and prices, references, dates, currencies, colors, JPEG data and size limits.
- Missing archive fields in older backups, archiving a member across groups, archiving groups, active-affiliation filtering, archive round trips, historical editing and restoring the catalog without duplicating purchases.

## Native UI tests

Open `OshicueKit.xcodeproj` in Xcode, select the `OshicueKit` scheme and an available iPhone simulator, and run the relevant test classes. The required Xcode, iOS SDK and signing settings must be available. UI tests use a separate `ui-test-ledger.json`; persistence is checked by terminating and relaunching the app.

| Test file | Cases | Main coverage |
| --- | --- | --- |
| [LedgerUITests.swift](../UITests/LedgerUITests.swift) | 6 | Adding and relaunch persistence, combined/per-group members, spending currencies, invalid/valid imports, quick member creation and default prices, titles and calendar entry in nine languages |
| [LedgerEditingDeletionUITests.swift](../UITests/LedgerEditingDeletionUITests.swift) | 5 | Single-purchase editing, choosing among multiple purchases, confirmed purchase deletion, deleting a member across groups, group deletion, historical editing and relaunch persistence |

[LedgerStore.swift](../OshicueKit/Support/LedgerStore.swift) implements first-launch example loading and persistence. This UI storage class is outside the core package. Changes to its policy need native UI and restart checks alongside the core tests.

## Manual acceptance checklist

1. Start a new ledger with isolated test data. Examples should appear without an example badge; the creation day's quantity is 10 and lifetime quantity is 16. Save an edit and restart: it should persist. Existing ledgers must not automatically regain examples.
2. Add a second group for the same member with its own color and price. Combined and per-group rankings should regroup rows while preserving the filtered total quantity. Members sharing a name but having different IDs remain separate.
3. Add and edit purchases. Check dates, currencies, default-price filling, quantity, optional event details and purchase totals. Switching affiliation/type/currency fills defaults again; verify the actual price before saving.
4. A single-purchase member card opens editing directly; multiple purchases first show a chooser. Saving changes only the chosen record. Cancelling deletion preserves totals; confirming subtracts quantity and spending, including after relaunch.
5. Deleting a shared member hides all their affiliations. Deleting one group retains shared members in other groups. Historical totals remain unchanged, historical purchases stay editable, and new purchases cannot select deleted affiliations.
6. Verify `CNY` price `80.50` and quantity `2` total `161.00`. Decimal prices in `JPY`/`KRW`, and more than two decimal places in other currencies, should fail. Quantity can count across currencies; spending rankings use only the selected currency.
7. Use [ledger-template.json](../Templates/ledger-template.json) to check pasted import, preview and export. Untouched examples can be replaced; edited examples must merge. Reimporting must not double count. Different purchase data with the same ID blocks the entire import. Invalid data must not change the saved ledger.
8. Restore an archived catalog using a valid backup and check that each purchase appears once. Restoring an edited purchase from a backup containing its older contents should report a conflict.
9. Switch among all nine app languages. Check Ledger, Calendar, Statistics, Members, editing, deletion and import errors, plus layouts with long names, large amounts and multiple currencies.
10. On a test device, check actual photo selection, avatar removal, system multi-file selection and saving, then backup migration. Automated tests do not cover every system dialog or real-photo workflow.

## Recording evidence

For each verification run, record the code revision, date and time zone, Xcode/system/device or simulator versions, command or test classes, passed/failed/not-run checks, and retained logs or result bundles. `screenshots/ledger-*.png` can illustrate the UI, but static screenshots do not establish a current test pass. Device installation, system file workflows and release acceptance need separate records.

## Retesting 0.4.0 management, favorites and appearance

The following test definitions and retest scope were added for 0.4.0 / build 10. They do not assert that every UI test ran and passed in this update. Record passed, failed or not run from actual execution; palette checks also do not replace manual inspection of each screen.

| Test file | Cases | Added coverage |
| --- | --- | --- |
| [LedgerManagementTests.swift](../Tests/LedgerManagementTests.swift) | 6 | Date boundaries and combined filters, historical catalog matching, empty and invalid ranges, selected counts/quantities/amounts by currency, batch deletion and clearing purchases while keeping catalogs. |
| [LedgerCatalogManagementTests.swift](../Tests/LedgerCatalogManagementTests.swift) | 3 | Batch archival of every member affiliation or only the selected groups, retained history, empty selections, unknown IDs and repeated operations. |
| [LedgerFavoritesTests.swift](../Tests/LedgerFavoritesTests.swift) | 8 | Missing favorite fields in old backups, JSON round trips and types, merge, archive exclusions, affiliation prices, stable order, new-record defaults and historical identities. |
| [DataManagementUITests.swift](../UITests/DataManagementUITests.swift) | 6 | Filtered purchase selection with Cancel/Confirm, filter changes clearing selection, clearing all currencies, diary batch deletion and backup forms remaining in settings management. Multiple catalog selections, Cancel/Confirm archival and retained purchase history. |
| [DataManagementFailureUITests.swift](../UITests/DataManagementFailureUITests.swift) | 2 | Inject actual persistence failure with `OSHICUE_FAIL_BATCH_DELETE=1` in isolated Debug test data. Verify that original records, diary bodies and the current selection remain, data is unchanged after relaunch, and deletion succeeds after disabling the failure and selecting again. |
| [LedgerFavoriteUITests.swift](../UITests/LedgerFavoriteUITests.swift) | 2 | Favorites shared across groups and persisted on relaunch, top shortcuts for multiple affiliations, preferred defaults and removing favorites. |
| [AppearanceUITests.swift](../UITests/AppearanceUITests.swift) | 5 | The actual UIKit dynamic palette, text and selected-control contrast in both appearances, venue backgrounds, increased contrast and member-color readability; these tests do not operate app screens. |
| [AppearanceScreenUITests.swift](../UITests/AppearanceScreenUITests.swift) | 1 | Operate all four modules, member favorites and selection, editing, management and backup under system light or dark appearance. The app has no forced theme; screen assertions precede 13 actual screenshot attachments. |

```sh
swift test --filter Ledger
xcodebuild test -project OshicueKit.xcodeproj -scheme OshicueKit -destination 'platform=iOS Simulator,id=<SIMULATOR_ID>' -only-testing:OshicueKitUITests/DataManagementUITests -only-testing:OshicueKitUITests/LedgerFavoriteUITests -only-testing:OshicueKitUITests/AppearanceUITests -only-testing:OshicueKitUITests/DataManagementFailureUITests -only-testing:OshicueKitUITests/AppearanceScreenUITests
```

## Additional manual retest steps

- Open both management screens from Settings and check the original import/export under Backup. Combine filters, select part of the results and verify confirmation counts; cancel first, then confirm and relaunch. Purchase confirmation must separate currencies, and changing filters must clear selection.
- Batch archive a shared member and one group; check the scope, other affiliations and historical editing. Clear purchases and verify that catalogs and diary entries remain. Delete diary results by status, date and search; verify unselected bodies and other modules.
- Set a favorite, reopen it through another affiliation and check the shared preference, top chooser section, affiliation prices and new-record default. Check removal, archival, export and reimport. Old JSON without `isFavorite` must mean not favorite and `schemaVersion` must remain `1`.
- Switch system light/dark appearance and inspect all four modules, details, editors, selectors, management and backup screens. Also check larger text, long labels in all nine languages, avatars, member colors, amounts, selected buttons and errors. Record the device and system version; old screenshots cannot replace current results.

Back to [README](../README.en.md) · [Ledger guide and data format](LEDGER.en.md) · [JSON template](../Templates/ledger-template.json)
