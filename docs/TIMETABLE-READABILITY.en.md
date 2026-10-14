# Timetable display and zoom

[简体中文](TIMETABLE-READABILITY.md) | [繁體中文](TIMETABLE-READABILITY.zh-Hant.md) | [繁體粵語](TIMETABLE-READABILITY.zh-HK.md) | [English](TIMETABLE-READABILITY.en.md) | [日本語](TIMETABLE-READABILITY.ja.md) | [한국어](TIMETABLE-READABILITY.ko.md) | [ไทย](TIMETABLE-READABILITY.th.md) | [Bahasa Indonesia](TIMETABLE-READABILITY.id.md) | [Bahasa Melayu](TIMETABLE-READABILITY.ms.md)

## Timeline and names

All Timetable has separate columns by event, venue and LIVE/merchandise type. My Timetable combines both session types for favorited groups. Overlapping sessions appear side by side, with the minimum lanes needed for each overlap cluster. Later independent sessions regain the full column width. Scroll horizontally while the time scale on the left stays in place.

At standard text size, each parallel lane is at least 180 pt wide; long names or short sessions can expand it to 320 pt. Total venue column width grows with concurrency. Lane headings repeat the venue so it remains identifiable when scrolling. Lane width and name font sizes follow system text size.

Artist names have priority and may wrap. The default is 12 pt in All Timetable and 15 pt in My Timetable, shrinking to a standard 11 pt when needed. If space is tight, secondary information such as time, venue or area is hidden first. Block position and height reflect actual time with drawing gaps; sessions are not lengthened to fit text. Very short sessions, very long names or large text can still be truncated. Single tap details to read the full original name, time, venue, area and notes; VoiceOver can also read the name.

Double tap a block to favorite or unfavorite its group. Details also provide a favorite action. Single tap viewing does not change favorites.

## Zoom and current time

| Scale | Height per minute |
| --- | --- |
| 50% | 2 pt |
| 70% | 2.8 pt |
| 100% (default) | 4 pt |
| 150% | 6 pt |

Zoom changes only the timeline display scale. The current time line uses real system time. The location button switches to today if the current events have sessions today, and scrolls to now when the current instant is within the displayed time range.

## Readability checks

Import the readability template linked at the end of this page, then select `2026-06-14` and `Asia/Tokyo`. It contains long Japanese/English names, 5 minute and 10 minute LIVE sessions, and four simultaneous merchandise sessions at one venue. All schedules are fictional.

1. At the default 100%, check short session durations and names. Scroll horizontally to read all four merchandise sessions and confirm they do not cover one another.
2. Single tap to view full details. After double tapping to favorite, check My Timetable for both LIVE and merchandise sessions and review the overlap hint.
3. Select 50%, 70%, 100% and 150% in turn. Confirm positions follow the time scale and details remain available when zoomed out.
4. Increase system text size and repeat scrolling, details and favorite checks.

See the verification guide for core layout and UI cases. UI tests were not rerun during this update; the large text steps remain checks to perform.

---

[Back to README](../README.en.md) · [Import format](IMPORT.en.md) · [Verification guide](VERIFICATION.en.md) · [Display and zoom](TIMETABLE-READABILITY.en.md) · [Delete timetables](SCHEDULE-DELETION.en.md)

Templates: [Timetable](../Templates/timetable-template.json) · [Fixed demo](../Templates/demo-timetable.json) · [Readability checks](../Templates/timetable-readability.json)
