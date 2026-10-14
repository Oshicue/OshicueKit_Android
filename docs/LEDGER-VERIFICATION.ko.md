# 수첩 검증 안내

[简体中文](LEDGER-VERIFICATION.md) | [繁體中文](LEDGER-VERIFICATION.zh-Hant.md) | [繁體粵語](LEDGER-VERIFICATION.zh-HK.md) | [English](LEDGER-VERIFICATION.en.md) | [日本語](LEDGER-VERIFICATION.ja.md) | [한국어](LEDGER-VERIFICATION.ko.md) | [ไทย](LEDGER-VERIFICATION.th.md) | [Bahasa Indonesia](LEDGER-VERIFICATION.id.md) | [Bahasa Melayu](LEDGER-VERIFICATION.ms.md)

현재 소스 코드를 기준으로 기존 테스트 범위와 반복 가능한 인수 확인 절차를 설명합니다. 테스트 파일이 있다는 사실은 이번 실행의 통과를 뜻하지 않습니다. 옛 버전 기록, 임시 결과 묶음, 스크린샷은 현재 버전의 통과 증거가 아닙니다. 실제 결과는 가장 최근 실행 로그를 기준으로 확인하세요.

## 핵심 테스트

프로젝트 루트에서 실행합니다.

```sh
swift test --filter Ledger
```

[LedgerTests.swift](../Tests/LedgerTests.swift)에 16개, [LedgerDeletionTests.swift](../Tests/LedgerDeletionTests.swift)에 7개 테스트가 있으며 다음을 다룹니다.

- 예시·템플릿 인코딩 왕복, 날짜 형식, 윤일, 시간대, 일·월·연·전체 기간 경계.
- 날짜별 유형 요약, 멤버의 그룹 간 합산과 분리 집계, 그룹 순위, 통화별 합계, 공동 순위, 일관된 정렬, 지출 0의 비율.
- 최소 단위 파싱, 과도한 소수 자릿수 거부, 0원 가격, 잘못된 수량·가격, 구매별 및 합계 오버플로.
- 이름·색상·단가 스냅샷과 자료 갱신 후 과거 기록 유지.
- ID별 자료 병합, 구매 중복 제거, 같은 ID의 다른 내용 충돌, 잘못된 병합 실패 시 부분 변경 방지.
- 필수 필드, 중복 ID, 소속·가격의 고유성, 참조, 날짜, 통화, 색상, JPEG, 파일 크기 검증.
- 옛 백업의 보관 필드 생략, 모든 그룹의 멤버 보관, 그룹 보관, 활성 소속 필터, 보관 백업 왕복, 과거 기록 수정, 자료 복원 시 이중 집계 방지.

## 네이티브 UI 테스트

Xcode에서 `OshicueKit.xcodeproj`를 열고 `OshicueKit` scheme과 사용 가능한 iPhone 시뮬레이터를 선택하여 해당 테스트 클래스를 실행합니다. 프로젝트에 필요한 Xcode, iOS SDK, 서명 설정이 있어야 합니다. UI 테스트는 독립된 `ui-test-ledger.json`을 사용하고 프로세스 종료 및 재실행으로 저장을 확인합니다.

| 테스트 파일 | 개수 | 주요 범위 |
| --- | --- | --- |
| [LedgerUITests.swift](../UITests/LedgerUITests.swift) | 6 | 추가·재실행 저장, 멤버 합산/그룹별, 지출 통화, 잘못된/유효한 가져오기, 빠른 멤버 추가·기본 가격, 9개 언어 제목·달력 진입 |
| [LedgerEditingDeletionUITests.swift](../UITests/LedgerEditingDeletionUITests.swift) | 5 | 단일 구매 수정, 여러 구매 중 선택·수정, 구매 삭제 확인, 모든 그룹의 멤버 삭제, 그룹 삭제, 과거 수정·재실행 저장 |

첫 예시 불러오기와 저장 정책은 [LedgerStore.swift](../OshicueKit/Support/LedgerStore.swift)에 있습니다. 이 UI 저장 클래스는 핵심 패키지에 포함되지 않습니다. 정책을 수정하면 핵심 테스트와 함께 네이티브 UI 및 재실행 동작도 확인해야 합니다.

## 수동 인수 체크리스트

1. 별도 테스트 데이터로 새 수첩을 시작합니다. 예시 배지 없이 예시가 표시되고 생성 당일 수량은 10, 전체 수량은 16이어야 합니다. 수정 후 저장하고 재실행해도 유지되며 기존 수첩에 예시를 자동으로 복구하지 않아야 합니다.
2. 같은 멤버에 두 번째 그룹을 추가해 색상과 가격을 각각 설정합니다. 합산/그룹별 순위는 묶음만 바뀌고 현재 범위 총수량은 같아야 합니다. 같은 이름에 다른 ID는 따로 집계합니다.
3. 구매 추가·수정에서 날짜, 통화, 기본 가격 입력, 수량, 선택 행사 정보, 구매별 총액을 확인합니다. 소속·유형·통화를 바꾸면 기본 가격을 다시 입력하므로 저장 전 실제 단가를 확인합니다.
4. 단일 구매 카드는 바로 수정하고 여러 구매는 먼저 선택하며 저장은 선택한 기록만 바꿔야 합니다. 삭제 취소는 수량을 유지하고 확정은 수량·금액을 차감하며 재실행 후에도 유지해야 합니다.
5. 겸임 멤버 삭제는 모든 소속을 숨깁니다. 그룹 하나를 삭제해도 다른 그룹의 겸임 멤버는 남습니다. 과거 수량·금액은 같고 기록은 수정 가능하며 새 구매에 삭제된 소속을 선택할 수 없어야 합니다.
6. `CNY` 단가 `80.50`, 수량 `2`의 합계 `161.00`을 확인합니다. `JPY`/`KRW` 소수와 다른 통화의 두 자릿수 초과는 오류여야 합니다. 수량은 여러 통화를 합산할 수 있고 지출 순위는 선택 통화만 집계합니다.
7. [ledger-template.json](../Templates/ledger-template.json)으로 붙여 넣기 가져오기, 미리보기, 내보내기를 확인합니다. 미수정 예시는 대체되고 수정된 예시는 병합됩니다. 반복 가져오기는 이중 집계하지 않고 같은 ID의 다른 구매는 전체를 차단합니다. 잘못된 자료는 원래 수첩을 바꾸지 않습니다.
8. 유효 백업으로 보관 자료를 복원해 과거 기록이 한 번씩만 있는지 확인합니다. 수정된 구매에 이전 내용의 백업을 복원하면 충돌해야 합니다.
9. 앱의 9개 언어를 전환해 수첩, 달력, 통계, 멤버, 수정, 삭제, 가져오기 오류를 확인합니다. 긴 이름, 큰 금액, 여러 통화의 배치도 확인합니다.
10. 테스트 기기에서 실제 사진 선택, 프로필 사진 제거, 시스템 다중 파일 선택·저장과 백업 이전을 확인합니다. 자동 테스트는 모든 시스템 대화상자와 실제 사진 흐름을 다루지 않습니다.

## 증거 기록

검증마다 코드 버전, 실행 날짜·시간대, Xcode·시스템·기기 또는 시뮬레이터 버전, 명령이나 테스트 클래스, 통과·실패·미실행 항목, 보관 가능한 로그·결과 묶음을 기록합니다. `screenshots/ledger-*.png`는 UI 이해에 도움을 주지만 정적 스크린샷은 현재 테스트 통과를 증명하지 않습니다. 실기기 설치, 시스템 파일 흐름, 출시 인수 확인은 별도로 기록해야 합니다.

## 0.4.0 관리·최애·화면 모드 재검증

다음은 0.4.0 / build 10에서 추가한 테스트 정의와 재검증 범위이며 이번에 모든 UI 테스트를 실행해 통과했다는 뜻이 아닙니다. 실제 실행에 따라 통과, 실패, 미실행을 기록하세요. 색상표 점검도 각 화면의 수동 확인을 대체하지 않습니다.

| 테스트 파일 | 수 | 추가 범위 |
| --- | --- | --- |
| [LedgerManagementTests.swift](../Tests/LedgerManagementTests.swift) | 6 | 날짜 경계와 조합 필터, 과거 자료 일치, 빈 결과와 잘못된 범위, 선택한 수·수량·통화별 금액, 일괄 및 전체 구매 삭제 후 자료 유지. |
| [LedgerCatalogManagementTests.swift](../Tests/LedgerCatalogManagementTests.swift) | 3 | 멤버의 전체 소속 또는 선택 그룹만 일괄 보관 처리, 기록 유지, 빈 선택·알 수 없는 ID·반복 작업. |
| [LedgerFavoritesTests.swift](../Tests/LedgerFavoritesTests.swift) | 8 | 옛 백업의 최애 필드 생략, JSON 왕복과 타입, 병합, 보관 자료 제외, 소속별 가격, 안정 정렬, 새 기록 기본 선택과 과거 소속. |
| [DataManagementUITests.swift](../UITests/DataManagementUITests.swift) | 6 | 구매 필터 및 다중 선택의 취소·확인, 조건 변경 시 선택 해제, 모든 통화 삭제, 일기 일괄 삭제, 설정 관리의 백업 양식. 자료 다중 선택, 보관 처리 취소·확인과 구매 기록 유지. |
| [DataManagementFailureUITests.swift](../UITests/DataManagementFailureUITests.swift) | 2 | 격리된 Debug 테스트 자료에서 `OSHICUE_FAIL_BATCH_DELETE=1`로 실제 저장 실패를 주입합니다. 원래 기록, 일기 본문과 현재 선택 유지, 재실행 후 데이터 유지, 실패 해제 후 재선택하여 삭제 재시도를 확인합니다. |
| [LedgerFavoriteUITests.swift](../UITests/LedgerFavoriteUITests.swift) | 2 | 그룹 간 최애 공유와 재실행 유지, 상단의 여러 소속 바로 선택, 최애 기본 선택과 해제. |
| [AppearanceUITests.swift](../UITests/AppearanceUITests.swift) | 5 | 실제 UIKit 동적 색상표, 두 모드의 텍스트 및 선택 컨트롤 대비, 행사장 배경, 대비 증가, 멤버 색상 가독성. 앱 화면을 직접 조작하지 않습니다. |
| [AppearanceScreenUITests.swift](../UITests/AppearanceScreenUITests.swift) | 1 | 시스템 밝은 모드 또는 어두운 모드에서 네 모듈, 멤버 최애와 선택, 편집, 관리, 백업을 실제로 조작합니다. 앱이 테마를 강제하지 않으며 화면 검증 후 실제 스크린샷 13개를 첨부합니다. |

```sh
swift test --filter Ledger
xcodebuild test -project OshicueKit.xcodeproj -scheme OshicueKit -destination 'platform=iOS Simulator,id=<SIMULATOR_ID>' -only-testing:OshicueKitUITests/DataManagementUITests -only-testing:OshicueKitUITests/LedgerFavoriteUITests -only-testing:OshicueKitUITests/AppearanceUITests -only-testing:OshicueKitUITests/DataManagementFailureUITests -only-testing:OshicueKitUITests/AppearanceScreenUITests
```

## 추가 수동 재검증 단계

- 설정에서 두 관리 화면을 열고 백업의 기존 가져오기·내보내기를 확인하세요. 조건을 조합해 일부 결과를 선택하고 확인 수를 검토한 후 먼저 취소하고 다음에 삭제를 확인한 뒤 재실행하세요. 구매 금액은 통화별 표시, 조건 변경은 선택 해제가 필요합니다.
- 겸임 멤버와 한 그룹을 일괄 보관 처리하여 범위, 다른 소속, 과거 편집을 확인하세요. 전체 구매 삭제 후 자료와 일기가 남는지 확인하세요. 상태·날짜·검색으로 일기를 골라 삭제하고 미선택 본문과 다른 모듈을 확인하세요.
- 최애를 설정하고 다른 소속에서 다시 열어 공유 설정, 선택 화면 상단, 소속별 가격, 새 기록 기본값을 확인하세요. 해제, 보관 처리, 내보내기, 재가져오기도 확인하세요. `isFavorite`가 없는 옛 JSON은 최애가 아니며 `schemaVersion`은 `1`이어야 합니다.
- 시스템 밝은 모드와 어두운 모드를 전환하고 네 모듈, 상세, 편집, 선택, 관리, 백업을 확인하세요. 큰 글자, 9개 언어의 긴 문구, 프로필 사진, 멤버 색상, 금액, 선택 버튼, 오류도 확인하세요. 기기와 시스템 버전을 기록하고 옛 스크린샷을 현재 결과로 대체하지 마세요.

[README로 돌아가기](../README.ko.md) · [수첩 사용법과 데이터 형식](LEDGER.ko.md) · [JSON 템플릿](../Templates/ledger-template.json)
