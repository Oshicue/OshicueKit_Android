# Delete timetables

[简体中文](SCHEDULE-DELETION.md) | [繁體中文](SCHEDULE-DELETION.zh-Hant.md) | [繁體粵語](SCHEDULE-DELETION.zh-HK.md) | [English](SCHEDULE-DELETION.en.md) | [日本語](SCHEDULE-DELETION.ja.md) | [한국어](SCHEDULE-DELETION.ko.md) | [ไทย](SCHEDULE-DELETION.th.md) | [Bahasa Indonesia](SCHEDULE-DELETION.id.md) | [Bahasa Melayu](SCHEDULE-DELETION.ms.md)

## Operation

Open event selection from the timetable screen. Each timetable has a red delete button with a trash icon. Tapping it shows confirmation with the target name. Cancel keeps the data; confirm removes the event and all its LIVE and merchandise sessions. Selecting and deleting an event are separate actions.

Deletion writes the local file before updating the interface. A save failure shows an error and keeps the original timetable so you can retry. Deleting an unselected event keeps other selections. Deleting the only selected event automatically selects the remaining events and adjusts the date.

## Favorites and independent data

Only favorites for groups still declared in other events' `artists` arrays are retained. This does not depend on displayed names or remaining sessions. Groups with the same name but different IDs do not share favorites.

The ledger and diary store independent data and event snapshots. Deleting a timetable does not delete purchases, statistics or diary drafts. Deleting all events saves an empty timetable that remains empty after relaunch. Import again or deliberately load the demo from settings. Demo loading merges by demo event IDs and retains events with other IDs.

## Repeat the checks

1. Import two events sharing one group ID, each with exclusive groups. Favorite the shared group and an exclusive group in the event to be deleted.
2. Check the name in the deletion prompt, cancel first and confirm that events, sessions and selections remain.
3. Confirm deletion. Check that target sessions disappear, shared favorites remain and favorites declared only in the deleted event are removed. Check remaining selections and date.
4. Delete an unselected event and check other selections remain. Check ledger and diary data are unaffected.
5. Delete all events and relaunch to confirm the demo is not restored. Import again, favorite a group and confirm normal use.

Core checks are in [ScheduleDeletionTests.swift](../Tests/ScheduleDeletionTests.swift); native operation checks are in [ScheduleDeletionUITests.swift](../UITests/ScheduleDeletionUITests.swift). UI tests were not rerun during this update.

---

[Back to README](../README.en.md) · [Import format](IMPORT.en.md) · [Verification guide](VERIFICATION.en.md) · [Display and zoom](TIMETABLE-READABILITY.en.md) · [Delete timetables](SCHEDULE-DELETION.en.md)

Templates: [Timetable](../Templates/timetable-template.json) · [Fixed demo](../Templates/demo-timetable.json) · [Readability checks](../Templates/timetable-readability.json)
