# 시간표 검증 안내

[简体中文](VERIFICATION.md) | [繁體中文](VERIFICATION.zh-Hant.md) | [繁體粵語](VERIFICATION.zh-HK.md) | [English](VERIFICATION.en.md) | [日本語](VERIFICATION.ja.md) | [한국어](VERIFICATION.ko.md) | [ไทย](VERIFICATION.th.md) | [Bahasa Indonesia](VERIFICATION.id.md) | [Bahasa Melayu](VERIFICATION.ms.md)

현재 소스의 검증 범위와 점검을 반복하는 방법을 설명합니다. 테스트 파일이 있다고 이번 작업에서 실행했다는 뜻은 아닙니다. 과거 스크린샷은 화면 참고 자료이며 현재 빌드 통과의 증거가 아닙니다.

## 재현 가능한 점검

프로젝트 루트에서 실행합니다:

```sh
swift test
python3 scripts/verify-localizations.py
```

`swift test`는 Foundation 핵심 테스트를 실행하며 iOS 화면 테스트는 실행하지 않습니다. 현지화 점검은 아홉 언어의 키 집합, 비어 있지 않은 텍스트, 형식 자리표시자 및 `.strings` 문법을 확인합니다.

| 파일 | 검증 범위 |
| --- | --- |
| [ScheduleImporterTests.swift](../Tests/ScheduleImporterTests.swift) | 형식과 필드 오류, 참조와 중복 ID, 시간 오프셋, 소수 초, 자정, 서머타임, 이벤트 전체 업데이트, 템플릿과 초기 예제 생성. |
| [TimelineLayoutTests.swift](../Tests/TimelineLayoutTests.swift) | 맞닿는 경계, 중복 열 배치, 최소 동시 열 수, 안정적인 정렬, 밀집 열 너비 및 가로 충돌 방지. |
| [ScheduleDeletionTests.swift](../Tests/ScheduleDeletionTests.swift) | 이벤트 삭제, 공유 즐겨찾기, 같은 이름의 다른 ID, 알 수 없는 ID, 빈 문서 인코딩과 다시 불러오기. |

## 네이티브 화면 재점검

Xcode에서 프로젝트를 열고 iOS 시뮬레이터에서 UI 테스트를 실행합니다. 주요 사례는 [OshicueKitUITests.swift](../UITests/OshicueKitUITests.swift), [TimelineReadabilityUITests.swift](../UITests/TimelineReadabilityUITests.swift), [ScheduleDeletionUITests.swift](../UITests/ScheduleDeletionUITests.swift)에 있습니다.

한 번 탭해 상세 보기, 두 번 탭해 즐겨찾기, 내 시간표의 일정 종류 통합, 잘못된 가져오기 확인 불가, 유효한 가져오기의 예제 대체, 아홉 언어 선택과 전환, 짧은 일정과 동시 굿즈 판매의 가독성, 삭제 확인, 선택 전환, 빈 시간표 재실행을 확인합니다. DEBUG UI 테스트는 `OSHICUE_UI_TESTS=1`로 별도 일정 파일을 사용하며, `OSHICUE_PRESERVE_UI_DATA=1`로 다시 실행해도 테스트 자료를 유지합니다.

## 추가 수동 점검

여러 파일을 가져와 잘못된 파일이나 파일 간 이벤트 ID 중복이 일부만 적용되지 않는지 확인합니다. 다른 시간대의 이벤트를 각각 또는 함께 선택하고 필터 표시줄의 시간대를 확인합니다. 자정을 넘는 일정과 정확히 자정에 끝나는 일정을 확인하고, 시스템 글자 크기를 키워 상세 점검을 반복합니다. 삭제 후 가계부와 일지의 스냅샷이 유지되는지, 다시 실행한 뒤 시간표와 즐겨찾기가 유지되는지 확인합니다.

시뮬레이터 점검은 실제 기기, 여러 시스템 버전, 큰 글자 및 파일 선택기의 실제 검증을 대체하지 않습니다. UI 테스트, 실제 기기 설치, 게시의 이번 실제 실행 결과를 각각 기록하세요. 기존 [스크린샷 폴더](../screenshots/)에는 과거 화면 참고 자료가 있습니다.

## 0.4.0 관리·최애·화면 모드 재검증

다음은 0.4.0 / build 10에서 추가한 테스트 정의와 재검증 범위이며 이번에 모든 UI 테스트를 실행해 통과했다는 뜻이 아닙니다. 실제 실행에 따라 통과, 실패, 미실행을 기록하세요. 색상표 점검도 각 화면의 수동 확인을 대체하지 않습니다.

| 테스트 파일 | 수 | 추가 범위 |
| --- | --- | --- |
| [DataManagementUITests.swift](../UITests/DataManagementUITests.swift) | 6 | 구매 필터 및 다중 선택의 취소·확인, 조건 변경 시 선택 해제, 모든 통화 삭제, 일기 일괄 삭제, 설정 관리의 백업 양식. 자료 다중 선택, 보관 처리 취소·확인과 구매 기록 유지. |
| [DataManagementFailureUITests.swift](../UITests/DataManagementFailureUITests.swift) | 2 | 격리된 Debug 테스트 자료에서 `OSHICUE_FAIL_BATCH_DELETE=1`로 실제 저장 실패를 주입합니다. 원래 기록, 일기 본문과 현재 선택 유지, 재실행 후 데이터 유지, 실패 해제 후 재선택하여 삭제 재시도를 확인합니다. |
| [AppearanceUITests.swift](../UITests/AppearanceUITests.swift) | 5 | 실제 UIKit 동적 색상표, 두 모드의 텍스트 및 선택 컨트롤 대비, 행사장 배경, 대비 증가, 멤버 색상 가독성. 앱 화면을 직접 조작하지 않습니다. |
| [AppearanceScreenUITests.swift](../UITests/AppearanceScreenUITests.swift) | 1 | 시스템 밝은 모드 또는 어두운 모드에서 네 모듈, 멤버 최애와 선택, 편집, 관리, 백업을 실제로 조작합니다. 앱이 테마를 강제하지 않으며 화면 검증 후 실제 스크린샷 13개를 첨부합니다. |

```sh
xcodebuild test -project OshicueKit.xcodeproj -scheme OshicueKit -destination 'platform=iOS Simulator,id=<SIMULATOR_ID>' -only-testing:OshicueKitUITests/AppearanceUITests -only-testing:OshicueKitUITests/DataManagementFailureUITests -only-testing:OshicueKitUITests/AppearanceScreenUITests
```

## 추가 수동 재검증 단계

- 설정에서 두 관리 화면을 열고 백업의 기존 가져오기·내보내기를 확인하세요. 조건을 조합해 일부 결과를 선택하고 확인 수를 검토한 후 먼저 취소하고 다음에 삭제를 확인한 뒤 재실행하세요. 구매 금액은 통화별 표시, 조건 변경은 선택 해제가 필요합니다.
- 겸임 멤버와 한 그룹을 일괄 보관 처리하여 범위, 다른 소속, 과거 편집을 확인하세요. 전체 구매 삭제 후 자료와 일기가 남는지 확인하세요. 상태·날짜·검색으로 일기를 골라 삭제하고 미선택 본문과 다른 모듈을 확인하세요.
- 최애를 설정하고 다른 소속에서 다시 열어 공유 설정, 선택 화면 상단, 소속별 가격, 새 기록 기본값을 확인하세요. 해제, 보관 처리, 내보내기, 재가져오기도 확인하세요. `isFavorite`가 없는 옛 JSON은 최애가 아니며 `schemaVersion`은 `1`이어야 합니다.
- 시스템 밝은 모드와 어두운 모드를 전환하고 네 모듈, 상세, 편집, 선택, 관리, 백업을 확인하세요. 큰 글자, 9개 언어의 긴 문구, 프로필 사진, 멤버 색상, 금액, 선택 버튼, 오류도 확인하세요. 기기와 시스템 버전을 기록하고 옛 스크린샷을 현재 결과로 대체하지 마세요.

---

[README로 돌아가기](../README.ko.md) · [가져오기 형식](IMPORT.ko.md) · [검증 안내](VERIFICATION.ko.md) · [표시 및 확대](TIMETABLE-READABILITY.ko.md) · [시간표 삭제](SCHEDULE-DELETION.ko.md)

템플릿: [시간표](../Templates/timetable-template.json) · [고정 예제](../Templates/demo-timetable.json) · [가독성 점검](../Templates/timetable-readability.json)
