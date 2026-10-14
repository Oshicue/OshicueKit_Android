# Using and backing up your diary

[简体中文](DIARY.zh-CN.md) | [繁體中文](DIARY.zh-Hant.md) | [繁體粵語](DIARY.zh-HK.md) | [English](DIARY.en.md) | [日本語](DIARY.ja.md) | [한국어](DIARY.ko.md) | [ไทย](DIARY.th.md) | [Bahasa Indonesia](DIARY.id.md) | [Bahasa Melayu](DIARY.ms.md)

The diary records concerts, merchandise sessions, and trips. Originals stay on your device so you can keep editing them, then copy or export text for publication. There are no preloaded entries on first use; importing the fictional template example is optional and requires your action.

## Creating and organizing entries

1. Tap the plus button in the third tab, “Diary,” and enter a title and multiline body. Markdown you type is preserved; the editor does not render a Markdown preview.
2. Add a summary and key points. List cards show the summary first, or the beginning of the body when the summary is empty, along with up to three tags.
3. Separate tags with commas, the ideographic comma, semicolons, or line breaks, including half-width and full-width separators. Leading and trailing whitespace and `#`／`＃` are removed. Deduplication ignores case but preserves differences in Japanese voicing and accents.
4. Under event details, choose an existing ledger event to bring in its name, Gregorian date, city, venue, and address. If the event has no valid date, the currently selected date is kept. You can also enter details manually or add a ledger event from the editor.
5. Choose “Drafts” or “Ready.” These states organize and filter local entries; they do not mean the entry has been published.

Event details are a snapshot owned by the diary entry. Editing or deleting the ledger event does not rewrite saved entries; selecting the event again refreshes the snapshot. Switching back to manual entry only removes the event ID link and keeps the imported details. The diary works independently, with venue and address supplied by you or brought in from the ledger.

The list groups entries by event date, newest first, and sorts entries on the same day by their latest modification time. Search includes the title, body, summary, tags, and event name, date, city, venue, and address. Every whitespace-separated search word must match, potentially in different fields. Filters show all entries, drafts, or ready entries.

## Autosave and deletion

Changes save automatically about 0.5 seconds after you stop editing. Tapping “Done” or “Close,” moving to the background, or leaving the editor triggers an immediate save attempt. A failed save shows an error and retry control. “Done” and “Close” leave the editor only after a successful save; a failed write does not replace saved data.

Opening an empty form or changing only the date or status does not create an entry. Saving starts when the title, body, summary, tags, event name, city, venue, or address contains meaningful content. An already saved entry can remain after its text is cleared. Every entry always needs a valid event date.

Saved entries have a deletion button at the bottom of the editor and require confirmation. Deletion removes only the local diary entry, leaving ledger events, purchase records, and previously exported copies intact.

## Text exports and full backups

“Prepare to publish” saves the current changes before showing the text. Copy Markdown or plain text, export `.md`／`.txt`, or open the iOS share sheet. Text includes the title, event details, tags, summary, and body; body and summary line breaks are preserved. Markdown export escapes inline special characters in the title and event details, while preserving the body and summary verbatim. You choose the share destination and subsequent actions; the app does not connect to social accounts or publish automatically.

Open “Diary management” and select “Backup” from the top right of the diary tab or the fourth tab, “Settings.” Exporting all entries saves a complete JSON file through the system file picker. JSON preserves IDs, statuses, tag arrays, event snapshots, and creation and modification times for reimporting. `.md`／`.txt` are for reading and sharing. You must export and keep backups yourself; this is not automatic cloud backup, and further edits do not update exported files.

## JSON format

[diary-template.json](../Templates/diary-template.json) is a fictional example. Replace it with actual records and assign a new unique ID to each new entry. Files use UTF-8. Required top-level fields are `schemaVersion` (integer `1`) and `entries` (an array).

| Field | Type | Requirement |
| --- | --- | --- |
| `id` | String | Required, nonempty, and unique; UUID is recommended. |
| `title` | String | Required; a draft may use `""`. |
| `body` | String | Required, complete body; write JSON line breaks as `\n`; may be `""`. |
| `summary` | String | Required, summary and key points; may be `""`. |
| `tags` | Array of strings | Required; use `[]` when there are no tags. |
| `status` | String | Required; only `draft` or `ready`. |
| `event` | Object | Required; see the fields below. |
| `createdAt` | String | Required, ISO 8601 creation timestamp with a time zone. |
| `updatedAt` | String | Required, modification timestamp; must not precede `createdAt`. |

| `event` field | Type | Requirement |
| --- | --- | --- |
| `ledgerEventID` | String or null | Optional; omit or use `null`. A string must be nonempty; the event need not still exist in the ledger. |
| `name` | String | Required, event name; may be `""`. |
| `date` | String | Required, valid Gregorian date strictly in `yyyy-MM-dd` format, such as `2026-10-14`. |
| `city` | String | Required, city; may be `""`. |
| `venue` | String | Required, venue; may be `""`. |
| `address` | String | Required, full address; may be `""`. |

`event.date` is a calendar day with no time zone or time of day. Creation and modification timestamps represent absolute instants, exported with millisecond precision, such as `2026-01-01T00:00:00.000Z`. Import also accepts explicit UTC offsets, such as `2026-01-01T09:00:00.000+09:00`; do not omit `Z` or the offset. An event date may precede the creation time of a recap written later. Diary backups do not require member, group, or ledger backups.

## Import and conflicts

In “Diary management” → “Backup,” select one or more UTF-8 JSON files, or paste JSON and check the preview. The preview shows new and existing entry counts; data is written only after confirmation.

- Entries with the same ID and identical contents are deduplicated; creation and modification times are compared at millisecond precision.
- Different contents under the same ID, including differences in body, summary, tags, status, event details, or timestamps, stop the entire import and preserve the original data. Edit existing entries in the app; use a new ID to keep another version.
- Import only adds new entries. Existing entries absent from the file remain. The same conflict rules apply between multiple selected files.
- Each input and the merged data must fit within 20 MiB (`20 * 1024 * 1024` bytes). Manual edits share this limit.
- Duplicate IDs, missing fields, wrong types, invalid statuses, dates, timestamps, or UTF-8 produce errors; field errors include a location. Data is not written if validation fails.

The import interface rejects an empty entry collection. Internal storage and export allow `entries: []`, meaning there are currently no entries.

## Local data and language

The diary is stored independently and atomically in `OshicueKit/diary.json` under Application Support and loaded on restart. If the original file is damaged, a recovery copy named `diary.recovery-<UUID>.json` is kept in the same directory before the first successful replacement. This mechanism does not replace your own backups.

Language selection in Settings applies to the timetable, ledger, diary, and settings. Date display follows the language; your body text, event names, addresses, and other user content stay unchanged.

## Diary management and system appearance

- In 0.4.0 (build 10), open “Diary management” from the diary toolbar or Settings → Data management. It contains “Record management” and “Backup”; JSON import, preview and export of all entries remain under “Backup.”
- Combine status, event date range and search filters, then select individual entries, select all current results or clear the selection. Filter changes clear the selection. Deletion confirms the entry count and the removal of bodies and summaries; Cancel keeps them. Confirmation removes only the selected entries. Other diary entries, the ledger and timetable are kept, and the deletion is saved locally.
- The diary list, editor, publishing preparation, management, backup and settings follow system light or dark appearance immediately. Saved bodies and event snapshots retain their original contents.

Back to [README](../README.en.md) · Related: [Ledger](LEDGER.en.md) · [Diary verification](DIARY-VERIFICATION.en.md) · [Overall verification](VERIFICATION.en.md) · [Diary template](../Templates/diary-template.json)
