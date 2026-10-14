# 일기와 전역 설정 검증 안내

[简体中文](DIARY-VERIFICATION.md) | [繁體中文](DIARY-VERIFICATION.zh-Hant.md) | [繁體粵語](DIARY-VERIFICATION.zh-HK.md) | [English](DIARY-VERIFICATION.en.md) | [日本語](DIARY-VERIFICATION.ja.md) | [한국어](DIARY-VERIFICATION.ko.md) | [ไทย](DIARY-VERIFICATION.th.md) | [Bahasa Indonesia](DIARY-VERIFICATION.id.md) | [Bahasa Melayu](DIARY-VERIFICATION.ms.md)

이 문서는 현재 저장소의 테스트 범위와 재현 방법을 설명하며 현재 환경에서 테스트가 통과했다는 선언이 아닙니다. 프로젝트 설정은 0.4.0 / build 10이며 최소 iOS 17을 지원합니다. 핵심 Swift Package는 macOS 14도 지원합니다. 실제 결과에는 커밋, Xcode 버전, 실행 기기, 테스트 출력을 기록하세요.

## 핵심 테스트

[DiaryTests.swift](../Tests/DiaryTests.swift)에 일기 테스트 18개가 정의되어 있습니다. 프로젝트 루트에서 실행합니다.

```sh
swift test --filter DiaryTests
```

| 범위 | 확인 내용 |
| --- | --- |
| 초안과 행사 스냅샷 | 빈 일기와 미완성 초안이 유효함, 행사 정보가 독립적임, 유효한 행사 날짜가 없으면 선택한 날짜를 사용함. |
| 텍스트와 타임스탬프 | 긴 Unicode 본문, 공백, 줄바꿈의 왕복 보존, 밀리초 정밀도의 중복 제거, 시간대 오프셋이 같은 절대 시각을 나타냄. |
| 병합과 충돌 | 동일 일기 중복 제거와 새 일기 추가, 같은 ID의 본문·요약·태그·상태·주소·수정 시각이 다르면 전체 묶음 거부. |
| 데이터 검증 | 중복 또는 빈 ID, 유효하지 않은 날짜와 시간 순서, 버전, 필수 필드, 필드 유형, 타임스탬프, 잘못된 JSON 및 UTF-8, 20 MiB 제한. |
| 정리와 내보내기 | 일본어 청음·탁음 및 악센트 차이를 유지하는 태그 분리와 중복 제거, 여러 검색어의 필드 간 검색, 행사 정보·요약·원본 본문이 포함된 일반 텍스트와 Markdown, 제목 이스케이프. |

중복, 충돌, 저장 크기 제한, 내보내기 테스트는 현재 코드를 기준으로 합니다. 테스트 수나 범위가 변경되면 이 문서도 갱신하세요.

## 네이티브 UI 테스트

[DiaryUITests.swift](../UITests/DiaryUITests.swift)에 테스트 5개가 정의되어 있습니다.

1. 수첩 행사를 선택하고 여러 줄 본문, 요약, 태그를 입력한 뒤 백그라운드로 이동합니다. 재실행 후 행사 정보와 텍스트를 확인하고 일기가 하나뿐인지 확인합니다.
2. JSON을 미리 보고 가져옵니다. 다시 가져오면 중복이 제거되고, 같은 ID의 본문 변경은 거부되며 재실행 후 원본 본문이 유지됩니다.
3. 삭제 확인에서 취소 및 확정 흐름을 확인합니다. 재실행 후에도 삭제 상태가 유지되고 수첩 행사와 구매 기록은 남습니다.
4. 네 번째 설정 탭에서 일기 관리를 열고 백업을 선택한 후 영어로 변경하면 여러 기능에 적용되며 원본 일기 본문은 유지됩니다.
5. 9개 언어별로 일기 제목, 새 일기 버튼, 네 번째 설정 탭, 빈 일기 상태를 확인합니다. 모든 UI 문구를 수동으로 검토했다는 뜻은 아닙니다.

먼저 사용 가능한 로컬 시뮬레이터를 확인하고 `<SIMULATOR_ID>`를 실제 기기 ID로 바꿉니다.

```sh
xcrun simctl list devices available
xcodebuild test -project OshicueKit.xcodeproj -scheme OshicueKit -destination 'platform=iOS Simulator,id=<SIMULATOR_ID>' -only-testing:OshicueKitUITests/DiaryUITests
```

Debug 테스트는 `OSHICUE_UI_TESTS=1`을 통해 별도 파일 `ui-test-diary.json`을 사용하며 재실행 시 유지 흐름은 `OSHICUE_PRESERVE_UI_DATA=1`을 사용합니다. 이 환경 변수는 테스트 격리용이며 사용자 백업 진입점이 아닙니다.

## 언어 검사, 수동 검사와 증거

macOS에서 다음 스크립트를 실행하여 9개 언어의 키 집합, 비어 있지 않은 값, 자리 표시자, `.strings` 파싱을 검사합니다. 번역 의미나 시각적 배치를 판단하지는 않습니다.

```sh
python3 scripts/verify-localizations.py
```

수동 검사는 자동 저장과 실패 후 재시도, 행사 선택과 스냅샷 독립성, 검색과 필터, 삭제 취소 및 확정, 여러 파일 병합과 충돌, 빈 백업 거부, 실제 `.md`／`.txt`／JSON 저장 및 다시 읽기, 시스템 공유 취소, 9개 언어의 긴 텍스트와 날짜 표시를 포함해야 합니다. 허가 없이 공유 수신자를 선택하지 마세요.

기존 [일기 목록](../screenshots/diary-list.png), [편집기](../screenshots/diary-editor.png), [행사 정보](../screenshots/diary-editor-event.png), [백업 미리 보기](../screenshots/diary-backup-preview.png), [충돌 메시지](../screenshots/diary-backup-conflict.png), [전역 설정](../screenshots/global-settings.png)은 저장소의 참고 스크린샷이며 현재 빌드의 통과를 증명하지 않습니다. 공유 수신, 실제 기기 설치, 게시 과정, 자동 저장 실패 복구는 핵심 테스트만으로 판단할 수 없습니다. 실제 수행 시 별도로 결과를 기록하세요.

## 0.4.0 관리·최애·화면 모드 재검증

다음은 0.4.0 / build 10에서 추가한 테스트 정의와 재검증 범위이며 이번에 모든 UI 테스트를 실행해 통과했다는 뜻이 아닙니다. 실제 실행에 따라 통과, 실패, 미실행을 기록하세요. 색상표 점검도 각 화면의 수동 확인을 대체하지 않습니다.

| 테스트 파일 | 수 | 추가 범위 |
| --- | --- | --- |
| [DiaryManagementTests.swift](../Tests/DiaryManagementTests.swift) | 4 | 상태·날짜·검색 조합, 잘못된 범위, 선택한 일기 삭제 후 다른 본문과 행사 자료 유지, 빈 일기 백업. |
| [DataManagementUITests.swift](../UITests/DataManagementUITests.swift) | 6 | 구매 필터 및 다중 선택의 취소·확인, 조건 변경 시 선택 해제, 모든 통화 삭제, 일기 일괄 삭제, 설정 관리의 백업 양식. 자료 다중 선택, 보관 처리 취소·확인과 구매 기록 유지. |
| [DataManagementFailureUITests.swift](../UITests/DataManagementFailureUITests.swift) | 2 | 격리된 Debug 테스트 자료에서 `OSHICUE_FAIL_BATCH_DELETE=1`로 실제 저장 실패를 주입합니다. 원래 기록, 일기 본문과 현재 선택 유지, 재실행 후 데이터 유지, 실패 해제 후 재선택하여 삭제 재시도를 확인합니다. |
| [AppearanceUITests.swift](../UITests/AppearanceUITests.swift) | 5 | 실제 UIKit 동적 색상표, 두 모드의 텍스트 및 선택 컨트롤 대비, 행사장 배경, 대비 증가, 멤버 색상 가독성. 앱 화면을 직접 조작하지 않습니다. |

```sh
swift test --filter Diary
xcodebuild test -project OshicueKit.xcodeproj -scheme OshicueKit -destination 'platform=iOS Simulator,id=<SIMULATOR_ID>' -only-testing:OshicueKitUITests/DataManagementUITests -only-testing:OshicueKitUITests/AppearanceUITests -only-testing:OshicueKitUITests/DataManagementFailureUITests
```

## 추가 수동 재검증 단계

- 설정에서 두 관리 화면을 열고 백업의 기존 가져오기·내보내기를 확인하세요. 조건을 조합해 일부 결과를 선택하고 확인 수를 검토한 후 먼저 취소하고 다음에 삭제를 확인한 뒤 재실행하세요. 구매 금액은 통화별 표시, 조건 변경은 선택 해제가 필요합니다.
- 겸임 멤버와 한 그룹을 일괄 보관 처리하여 범위, 다른 소속, 과거 편집을 확인하세요. 전체 구매 삭제 후 자료와 일기가 남는지 확인하세요. 상태·날짜·검색으로 일기를 골라 삭제하고 미선택 본문과 다른 모듈을 확인하세요.
- 최애를 설정하고 다른 소속에서 다시 열어 공유 설정, 선택 화면 상단, 소속별 가격, 새 기록 기본값을 확인하세요. 해제, 보관 처리, 내보내기, 재가져오기도 확인하세요. `isFavorite`가 없는 옛 JSON은 최애가 아니며 `schemaVersion`은 `1`이어야 합니다.
- 시스템 밝은 모드와 어두운 모드를 전환하고 네 모듈, 상세, 편집, 선택, 관리, 백업을 확인하세요. 큰 글자, 9개 언어의 긴 문구, 프로필 사진, 멤버 색상, 금액, 선택 버튼, 오류도 확인하세요. 기기와 시스템 버전을 기록하고 옛 스크린샷을 현재 결과로 대체하지 마세요.

[README로 돌아가기](../README.ko.md) · 관련: [일기](DIARY.ko.md) · [전체 검증](VERIFICATION.ko.md) · [일기 템플릿](../Templates/diary-template.json)
