# Ledger guide and data format

[简体中文](LEDGER.zh-CN.md) | [繁體中文](LEDGER.zh-Hant.md) | [繁體粵語](LEDGER.zh-HK.md) | [English](LEDGER.en.md) | [日本語](LEDGER.ja.md) | [한국어](LEDGER.ko.md) | [ไทย](LEDGER.th.md) | [Bahasa Indonesia](LEDGER.id.md) | [Bahasa Melayu](LEDGER.ms.md)

The Ledger records merchandise purchases on a calendar, manages groups, members, affiliations, colors and default prices, and ranks quantities and spending. Data is stored on your device. Ledger backups are separate from Timetable and Diary backups.

## Getting started

1. Add groups and members under Members & types. When a member belongs to several groups, choose an existing member to add another affiliation and keep the same member ID. Members with the same name but different IDs are not merged automatically.
2. Set a color and default unit prices for each affiliation. The default types are Cheki, WIDE Cheki and Phone photo; you can add other types and prices in different currencies for the same type. A member's name and avatar are shared across groups.
3. Tap ＋ and choose the date, member's group, type, quantity, currency and actual unit price. Changing the affiliation, type or currency fills in its matching default price again; enter a price if no default exists. Quantity must be a positive integer; a unit price of zero is allowed.
4. Event, city, venue, address and note are optional. Create a Ledger event or select an event and venue from the Timetable to copy them into a Ledger event. Later Timetable changes do not automatically update the Ledger.
5. Calendar numbers are the day's total quantity, rather than the number of records. Monthly cards show quantity and spending separated by currency. Select a day to see member-by-group summaries and individual records.

Quantity counts units of the chosen type: photos can be counted individually, and ticket types can count tickets.

## Editing, deletion and historical snapshots

Tap a member card below the date. A single purchase opens its editor directly; multiple purchases first show a record chooser. You can also expand the individual records to edit one. The form loads the original date, affiliation, type, quantity, price and optional details. Saving updates only that record. Delete this purchase requires confirmation; deletion subtracts its quantity and amount.

Under Members & types, tap a member to edit their details, or the pencil beside a group heading to edit that group. Both deletions require confirmation:

- Delete member: archives the member and all their affiliations in every group.
- Delete group: archives the group and its affiliations. Members shared with other groups remain available there.

Archived data remains in backups. Existing purchases, names, colors, prices and statistics are preserved. New records and reassigned affiliations must use active affiliations. Historical records can still be edited with their original deleted affiliation, marked Deleted in the picker. The catalog editor has no restore action; importing a backup with active catalog entries using the same IDs can restore them, subject to full merge validation.

Changing catalog names, colors or default prices does not rewrite saved purchases. When editing a purchase, retaining its affiliation preserves the member, group and color snapshots; retaining its type preserves the type-name snapshot. Changing affiliation or type captures the corresponding new snapshots. The actual price and event details are saved from the form. The complete ledger is validated before an atomic local save; a failed save does not publish partial changes.

## Statistics

Statistics support day, month, year and lifetime periods, filters by type and group, and rankings by combined member, member within each group, or group. The member count is the number of distinct `memberID` values in the filtered records. Archived groups remain available for historical filtering.

Quantity rankings can count units across all currencies; spending rankings require one currency. Amounts are never added across currencies or converted automatically. Summaries show quantities by type and amounts by currency. Competition ranks use values such as `1、1、3`, with stable ID ordering for ties. Shares use the filtered quantity or single-currency spending total as the denominator; a zero total gives a zero share. Ranking labels use the latest record's name and color snapshots within the filtered period; records on the same date are ordered by ID.

## Example ledger

When no saved file exists on first launch, fictional examples are loaded and saved immediately: 2 groups, 3 members, 4 affiliations, 3 types, 1 event and 5 records, totaling 16 units. Dates use the creation day's `Asia/Shanghai` calendar date and the preceding two days; the creation day's quantity is 10. They do not use a fixed template date. No example badge appears.

Existing ledgers load their saved data. Restarting does not restore edited or deleted examples. Load examples only accepts an empty ledger or untouched examples. Empty means no groups, members, affiliations, events or records, with either no types or exactly the three default types. Archiving entries alone does not make a ledger empty. An initial import can replace untouched examples; any saved example edit changes subsequent imports to merging.

## JSON fields

[ledger-template.json](../Templates/ledger-template.json) is an importable example separate from the Timetable template. Use UTF-8 and `schemaVersion` set to `1`. All six arrays below are required; use `[]` when empty. IDs must be unique within each array, and IDs and names cannot consist only of whitespace.

| Array | Required fields | Optional fields |
| --- | --- | --- |
| `groups` | `id`, `name` | `isArchived` |
| `members` | `id`, `name` | `avatarJPEGBase64`, `isArchived`、`isFavorite` |
| `memberships` | `id`, `memberID`, `groupID`, `color`, `prices` | `isArchived` |
| `products` | `id`, `name` | None |
| `events` | `id`, `name` | `date`, `city`, `venue`, `address` |
| `records` | See below | See below |

`memberships` reference members and groups declared in the same file. Each member/group pair can have only one affiliation. `color` is a six-digit RGB value, such as `#F28AAF`. `prices` may be `[]`. Each price requires `productID`, `currency` and `unitPriceMinor`, references an existing type, and must have a unique type/currency combination within that affiliation.

`isArchived` is an optional boolean; only `true` means archived. Older backups without this field remain importable, with version `1`. An active affiliation also requires an active member and group. Archived entries must still be valid. When manually organizing a backup, retain the members, groups and affiliations referenced by historical purchases.

| `records` field | Requirement and meaning |
| --- | --- |
| `id` | Required, unique purchase ID; use a new ID for a new purchase, preferably a UUID |
| `date` | Required, valid Gregorian date in strict `yyyy-MM-dd` format, such as `2026-10-14` |
| `memberID`, `groupID`, `productID` | Required, reference the file's member, group and type, with a matching affiliation |
| `quantity` | Required, positive integer quantity |
| `unitPriceMinor` | Required, nonnegative integer unit price in the currency's minor unit |
| `currency` | Required, uppercase currency code from the table below |
| `memberNameSnapshot`, `groupNameSnapshot`, `productNameSnapshot` | Required, names at purchase time; cannot consist only of whitespace |
| `colorSnapshot` | Required, six-digit RGB color at purchase time |
| `eventID` | Optional, references an ID in the file's `events` |
| `eventNameSnapshot`, `citySnapshot`, `venueSnapshot`, `addressSnapshot`, `note` | Optional, event, city, venue, address and note at purchase time |

Purchase dates are stored as calendar days, unaffected by later time-zone changes. Event and purchase dates may differ. Avatars contain Base64-encoded JPEG data, rather than a file path.

## Amount precision and limits

The total is `quantity × unitPriceMinor`, using integer arithmetic. Enter an ordinary price in the UI; it is converted to minor units. Excess decimal places are rejected rather than silently rounded. Use digits and a decimal point, without currency symbols or thousands separators. JSON amounts must be integers.

| Currency | Decimal places | UI price example | `unitPriceMinor` |
| --- | --- | --- | --- |
| `JPY`, `KRW` | 0 | `2000` | `2000` |
| `CNY` | 2 | `80.50` | `8050` |
| `TWD`, `HKD`, `USD`, `THB`, `IDR`, `MYR` | 2 | `100.00` | `10000` |

These are the app's current precision rules; `IDR` and `TWD` also retain two minor-unit digits. Integer overflow is checked for purchase multiplication, aggregate quantity and each currency's total. JSON text and the encoded ledger are limited to `20 × 1024 × 1024` bytes (20 MiB). Each imported avatar is limited to `2 × 1024 × 1024` decoded bytes (2 MiB) and its JPEG structure is checked. Original photos selected from the library are limited to 10 MiB, resized to a maximum dimension of 512 pixels and converted to JPEG before saving.

## Importing, merging and backups

Open “Ledger management” at the top of the Ledger and select “Backup.” Select one or more JSON/text files, or paste JSON and validate its preview, then confirm the import. Multiple files are merged before previewing. Empty files, documents without data, invalid fields and conflicts block importing. Every file must include all referenced entries; it cannot rely on the current ledger to supply missing members or groups. Editing pasted text invalidates the previous preview.

Valid imports replace untouched examples. All other cases use these merge rules:

- Groups, members, affiliations, types and events are updated or added by `id`. Existing entries absent from the incoming file remain. Importing can update archive flags.
- Purchases are deduplicated by `id`. Importing identical records again does not double count. A record with the same ID but different contents cancels the entire import with an error. Edit purchases in the app; use a new ID for a new purchase.
- Complete documents are validated before and after merging, including affiliation uniqueness, references, dates, snapshots, amounts and size. Failure leaves the original ledger intact.

Export includes the entire ledger, archived entries, historical snapshots, prices and avatars. Restoration merges data and does not undo existing purchase edits. An edited record conflicts with an old backup record sharing its ID but containing different data. Keep exported files for migration or your own records.

## Management, favorite members and system appearance

- In 0.4.0 (build 10), the ledger toolbar and Settings → Data management open “Ledger management.” It contains “Record management,” “Catalog cleanup” and “Backup.” Existing JSON import, preview and full export remain under “Backup.” Settings also provides “Diary management.”
- Combine purchase filters for date range, member, group, type, currency, event and search, then select individual records, select all current results or clear the selection. Changing any filter clears the selection; deletion is disabled when no results match. Confirmation shows the selected record count, total quantity and amounts by currency. Cancel keeps the records; explicit confirmation deletes only the selection and updates statistics.
- “Delete all purchase records” applies to the whole ledger regardless of current filters and requires confirmation of its record count, quantity and amounts by currency. Groups, members, types, event catalogs and diary entries remain available for future records.
- “Catalog cleanup” supports search and multiple selection of members or groups. Deleting a member archives all their affiliations; deleting a group archives only that group and its affiliations, keeping memberships in other groups. Historical purchases, snapshots and statistics remain, and historical records stay editable. Cancel does not archive anything; a valid backup can restore catalog entries.
- Set or unset “Favorite member” in the member editor under “Members & types.” The preference belongs to the member ID and is shared across all affiliations. The purchase member chooser puts favorites first, with each group affiliation retaining its own color and prices; the complete group sections remain below. New records prefer an available favorite affiliation. Linking an existing member also lists favorites first; archived members or affiliations are excluded from favorite shortcuts.
- A member’s `isFavorite` is an optional Bool: only `true` means favorite; `false`, omission or `null` means not favorite. Existing JSON still imports and `schemaVersion` remains `1`. Full backups store this preference, and merging catalog entries by member ID updates it.
- The app follows the system’s light or dark appearance across the timetable, ledger, diary, settings and all editing, selection, management and backup screens. System changes update the interface without a separate theme setting. Member colors and historical snapshots are retained.

Back to [README](../README.en.md) · [Ledger verification guide](LEDGER-VERIFICATION.en.md) · [JSON template](../Templates/ledger-template.json)
