# Timetable JSON import

[简体中文](IMPORT.zh-CN.md) | [繁體中文](IMPORT.zh-Hant.md) | [繁體粵語](IMPORT.zh-HK.md) | [English](IMPORT.en.md) | [日本語](IMPORT.ja.md) | [한국어](IMPORT.ko.md) | [ไทย](IMPORT.th.md) | [Bahasa Indonesia](IMPORT.id.md) | [Bahasa Melayu](IMPORT.ms.md)

Use UTF-8 JSON. Open import from the timetable screen, then paste the full text or select one or more `.json` or plain text files. Successful validation shows event and session counts; data is applied only after confirmation. Editing the preview text requires validation again.

## Updates and favorites

First launch generates and saves two fictional timetables anchored to the current hour in Tokyo, including sessions for the following day; the first two groups are initially favorited. The heading has no demo label. Later launches load saved data. The first real import replaces timetables still in demo mode and clears demo favorites. Subsequent imports keep other events and replace the full contents of an event with the same `id`. Supply the complete event when updating, rather than a subset of its sessions.

A group's `id` is its shared favorite identity across events. Keeping the ID preserves favorites after a name change. Double tap a LIVE or merchandise block to toggle the group favorite. My Timetable combines both types for favorited groups, filtered by selected events and date. Overlapping times are valid and appear side by side with a conflict hint. The hint compares times only; it does not assess travel or whether you can attend both.

## Fields

| Location | Field | Rule |
| --- | --- | --- |
| Root | `schemaVersion` | Required integer, always `1`. |
| Root | `events` | Required event array. The import screen requires at least one event and at least one session in the whole batch. |
| Event | `id`, `name`, `city` | Required nonempty strings. Event IDs must be unique within the batch. |
| Event | `timeZone` | Required valid IANA time zone, such as `Asia/Tokyo`. |
| Event | `venues`, `artists`, `sessions` | Required arrays; an individual event may have no sessions. |
| Venue | `id`, `name`, `color` | Nonempty ID and name; color must be `#RRGGBB`, such as `#78C9EB`. |
| Group | `id`, `name` | Required nonempty strings. Keep group IDs stable across events. |
| Session | `id`, `artistID`, `venueID` | Nonempty session ID; references must point to a group and venue declared in this event. |
| Session | `kind` | Only `live` or `goods`. |
| Session | `startsAt`, `endsAt` | Complete ISO 8601 timestamps with seconds and `Z` or an explicit offset; end must be later than start. |
| Session | `area`, `notes` | Optional strings; may be omitted or `null`. |

Venue, group and session IDs must each be unique within their event. Required strings cannot contain only whitespace. JSON does not support comments or trailing commas. Unknown fields are accepted but not displayed by the app or retained when reencoded.

## Validation and batch import

Errors show a field path, such as `$.events[0].sessions[2].endsAt`. Invalid versions, types, dates, time zones, colors, duplicate IDs, references or durations stop validation of the entire input. Multiple files are combined and validated as one batch; duplicate event IDs across files also fail. A failure in any file prevents partial import. The import screen cannot apply an empty event array or a batch with no sessions. The core format permits `events: []` to store an internal empty state after deletion.

## Time zones and midnight

Accepted values include `2026-10-14T12:00:00+09:00`, `2026-10-14T03:00:00Z` and fractional seconds. `12:00`, values without seconds, and values without a time zone are invalid. The offset identifies the absolute instant; event `timeZone` determines its local date. Use the actual time zone for each session. The repeated daylight saving hour `2026-11-01T01:30:00-04:00` and `2026-11-01T01:30:00-05:00` represents two different instants.

For a session crossing midnight, include the next date in the end time: `2026-10-14T23:40:00+09:00` → `2026-10-15T00:20:00+09:00`. It appears on both dates it spans, clipped to each day. Ending exactly at `00:00:00` does not add an empty date. There is no artificial maximum duration.

When events in different time zones are selected together, the current interface uses the time zone of the first selected event in document order for dates, times and details. The filter bar shows that zone. Select an event alone to view its own local time. The current time line uses the real system time; fixed demo dates may not be today.

## Edit a template

Copy the timetable template linked at the end of this page. Keep `schemaVersion` and `events`, fill in stable IDs, then copy sessions and edit references, types and timestamps. All events, groups and schedules in the templates are fictional. Templates are not imported automatically.

---

[Back to README](../README.en.md) · [Import format](IMPORT.en.md) · [Verification guide](VERIFICATION.en.md) · [Display and zoom](TIMETABLE-READABILITY.en.md) · [Delete timetables](SCHEDULE-DELETION.en.md)

Templates: [Timetable](../Templates/timetable-template.json) · [Fixed demo](../Templates/demo-timetable.json) · [Readability checks](../Templates/timetable-readability.json)
