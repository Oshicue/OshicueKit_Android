# 시간표 삭제

[简体中文](SCHEDULE-DELETION.md) | [繁體中文](SCHEDULE-DELETION.zh-Hant.md) | [繁體粵語](SCHEDULE-DELETION.zh-HK.md) | [English](SCHEDULE-DELETION.en.md) | [日本語](SCHEDULE-DELETION.ja.md) | [한국어](SCHEDULE-DELETION.ko.md) | [ไทย](SCHEDULE-DELETION.th.md) | [Bahasa Indonesia](SCHEDULE-DELETION.id.md) | [Bahasa Melayu](SCHEDULE-DELETION.ms.md)

## 사용 방법

시간표의 이벤트 선택 화면을 엽니다. 각 시간표에 빨간 삭제 버튼과 휴지통 아이콘이 있습니다. 누르면 대상 이름이 포함된 확인 메시지가 표시됩니다. 취소하면 자료를 유지하고, 확인하면 해당 이벤트와 모든 LIVE 및 굿즈 판매 일정을 제거합니다. 이벤트 선택과 삭제는 별도 작업입니다.

삭제는 로컬 파일 저장에 성공한 후 화면을 업데이트합니다. 저장 실패 시 오류를 표시하고 원래 시간표를 유지하므로 다시 시도할 수 있습니다. 선택하지 않은 이벤트를 삭제하면 다른 선택은 유지됩니다. 유일하게 선택한 이벤트를 삭제하면 남은 이벤트를 자동으로 선택하고 날짜도 조정합니다.

## 즐겨찾기와 독립 자료

다른 이벤트의 `artists` 배열에 아직 선언된 그룹의 즐겨찾기만 유지합니다. 표시 이름이나 남은 일정 유무로 판단하지 않습니다. 이름이 같아도 ID가 다른 그룹은 즐겨찾기를 공유하지 않습니다.

가계부와 일지는 독립 자료와 이벤트 스냅샷을 저장합니다. 시간표를 삭제해도 구매 기록, 통계 또는 일지 초안은 삭제되지 않습니다. 모든 이벤트를 삭제하면 빈 시간표를 저장하고 다시 실행해도 빈 상태를 유지합니다. 다시 가져오거나 설정에서 직접 예제를 불러올 수 있습니다. 예제를 불러오면 예제 이벤트 ID에 따라 합치며 다른 ID의 이벤트는 유지합니다.

## 재점검

1. 그룹 ID 하나를 공유하고 각자 고유 그룹도 있는 이벤트 두 개를 가져옵니다. 공유 그룹과 삭제할 이벤트의 고유 그룹을 즐겨찾기합니다.
2. 삭제 안내에서 이름을 확인한 뒤 먼저 취소하고 이벤트, 일정, 선택이 유지되는지 확인합니다.
3. 삭제를 확인합니다. 대상 일정이 사라지고 공유 즐겨찾기는 유지되며 삭제한 이벤트에서만 선언된 즐겨찾기가 제거되는지 확인합니다. 남은 선택과 날짜도 점검합니다.
4. 선택하지 않은 이벤트를 삭제하고 다른 선택이 유지되는지 확인합니다. 가계부와 일지 자료가 영향을 받지 않는지 확인합니다.
5. 모두 삭제한 뒤 다시 실행해 예제가 복구되지 않는지 확인합니다. 다시 가져오고 즐겨찾기해 정상 사용을 확인합니다.

핵심 점검은 [ScheduleDeletionTests.swift](../Tests/ScheduleDeletionTests.swift), 네이티브 동작 점검은 [ScheduleDeletionUITests.swift](../UITests/ScheduleDeletionUITests.swift)에 있습니다. 이번 작업에서는 UI 테스트를 다시 실행하지 않았습니다.

---

[README로 돌아가기](../README.ko.md) · [가져오기 형식](IMPORT.ko.md) · [검증 안내](VERIFICATION.ko.md) · [표시 및 확대](TIMETABLE-READABILITY.ko.md) · [시간표 삭제](SCHEDULE-DELETION.ko.md)

템플릿: [시간표](../Templates/timetable-template.json) · [고정 예제](../Templates/demo-timetable.json) · [가독성 점검](../Templates/timetable-readability.json)
