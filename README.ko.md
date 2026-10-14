# Oshicue Kit for Android

[简体中文](README.md) | [繁體中文](README.zh-Hant.md) | [繁體粵語](README.zh-HK.md) | [English](README.en.md) | [日本語](README.ja.md) | [한국어](README.ko.md) | [ไทย](README.th.md) | [Bahasa Indonesia](README.id.md) | [Bahasa Melayu](README.ms.md)

행사 시간표를 보고, 물판 지출을 기록하고, 행사 후기를 정리하는 네이티브 Android 앱입니다(Kotlin + Jetpack Compose). 탭은 시간표, 수첩, 일기, 설정 순서입니다. 현재 버전은 **0.4.0(versionCode 10)**이며 iOS판 Oshicue Kit 0.4.0(build 10)과 기능이 일치합니다. Android 8.0(API 26) 이상을 지원합니다.

## 스크린샷

| 시간표 | 내 시간표 | 수첩 달력 |
| --- | --- | --- |
| ![시간표](screenshots/timetable.png) | ![내 시간표](screenshots/my-timetable.png) | ![수첩 달력](screenshots/ledger-calendar.png) |

| 기록 추가 | 수첩 통계 | 일기 |
| --- | --- | --- |
| ![기록 추가](screenshots/ledger-record-form.png) | ![수첩 통계](screenshots/ledger-statistics.png) | ![일기](screenshots/diary.png) |

| 새 일기 | 설정 |
| --- | --- |
| ![새 일기](screenshots/diary-editor.png) | ![설정](screenshots/settings.png) |

## 기능

### 시간표

- 모든 일정은 공연장과 LIVE／물판으로 나뉩니다. 날짜, 이벤트 다중 선택, 유형 필터를 조합할 수 있습니다. 공연장은 가로로, 시간은 세로로 스크롤합니다.
- 세션을 탭하면 상세 정보를 보고, 더블 탭으로 그룹을 즐겨찾기하거나 해제할 수 있습니다. 즐겨찾기는 `artistID` 기준으로 이벤트 간에 공유되며, "내 시간표"는 즐겨찾기한 그룹의 LIVE와 물판 세션을 함께 표시합니다. 겹치는 일정은 나란히 배치하고 충돌을 알려 줍니다.
- 시간축은 50%, 70%, 100%, 150% 확대를 지원합니다(기본 100%, 4dp／분). 겹침이 많으면 가로로 넓어지고, 짧은 세션은 이름을 우선 표시하며 글자 크기를 자동으로 조정합니다. 상세 화면에서는 전체 정보를 확인할 수 있습니다.
- 현재 시간선은 실제 시스템 시간을 사용하며 15초마다 갱신됩니다. "지금"은 선택한 이벤트에 오늘 일정이 있을 때만 오늘로 전환합니다.
- 여러 이벤트는 문서 순서에서 첫 번째로 선택한 이벤트의 시간대를 따릅니다. 상세 화면에서 원래 이벤트의 현지 시간도 확인할 수 있고, 자정을 넘는 세션은 표시 시간대의 날짜별로 나뉩니다.
- 가져오기는 JSON 붙여넣기 또는 하나／여러 개의 UTF-8 JSON 파일을 지원합니다. 검증과 미리보기 후 확인하면 적용됩니다. 같은 이벤트 ID는 해당 이벤트 전체를 교체하고, 같은 배치 안의 중복 ID는 거부됩니다. 시간표 삭제는 확인이 필요하며, 남은 이벤트에 없는 아티스트의 즐겨찾기도 정리됩니다.

### 수첩

- 달력에서 일별·월별 수량과 통화별 금액을 확인합니다. 멤버 카드를 탭하면 물판 기록을 편집하고, 여러 건이면 먼저 기록을 선택합니다.
- 그룹, 멤버, 아바타, 그룹별 대표 색상과 기본 가격, 구매 유형, 이벤트 템플릿을 관리합니다. 겸임 멤버는 기존 멤버 연결로 추가합니다.
- 날짜, 멤버와 그룹, 유형, 수량, 통화, 단가, 선택 사항인 이벤트 정보를 기록하며 시간표의 이벤트 정보를 가져올 수도 있습니다.
- 순위는 일／월／연／평생 기준이며 수량 또는 지출, 멤버 합산／멤버·그룹별／그룹, 유형과 그룹 필터를 지원합니다. 동점은 `1、1、3`으로 표시됩니다. 지출은 단일 통화만 사용하며 환율 환산은 하지 않습니다.
- 기록에는 이름, 색상, 이벤트, 가격 스냅샷이 보존됩니다. 멤버／그룹 삭제는 보관(아카이브) 방식이라 기록과 통계가 유지되고 과거 기록도 계속 편집할 수 있습니다.
- 멤버를 "즐겨찾기 멤버"로 지정할 수 있고, 기록할 때 멤버 선택 화면 상단에 표시됩니다. 금액은 정수 최소 통화 단위를 사용하며 `CNY`, `JPY`, `HKD`, `TWD`, `USD`, `KRW`, `THB`, `IDR`, `MYR`을 지원합니다.
- 수첩 관리에는 기록 관리(기간·멤버·그룹·유형·통화·이벤트·텍스트 조합 필터 후 일괄 삭제, 전체 삭제), 카탈로그 정리(멤버／그룹 보관), JSON 백업(내보내기, 미리보기 가져오기, 충돌 거부)이 있습니다.

### 일기

- 행사 후기를 작성하고 제목, 본문, 요약, 태그를 편집하며 초안／정리됨으로 표시합니다. 행사 날짜로 보고, 상태로 필터링하고, 본문과 행사 정보를 검색합니다.
- 수첩 이벤트에서 날짜, 이름, 도시, 공연장, 주소를 가져와 독립적으로 수정할 수 있습니다. 일기는 자체 스냅샷을 저장합니다.
- 편집을 멈추고 약 0.5초 후 자동 저장됩니다. 편집기를 닫거나 백그라운드로 전환하면 즉시 저장하고, 실패 시 다시 시도할 수 있습니다.
- "게시 준비"에서 Markdown／일반 텍스트 복사, `.md`／`.txt` 내보내기, 시스템 공유를 사용할 수 있습니다.
- 일기 관리에서는 필터, 일괄 삭제, JSON 백업 가져오기／내보내기를 제공합니다.

### 설정 및 데이터

- 언어 전환(9개 언어 + 시스템 설정 따르기)이 즉시 적용됩니다. 날짜, 시간, 숫자는 선택한 지역 형식을 따릅니다.
- 시간표 가져오기, 수첩 관리, 일기 관리, 예시 불러오기, 버전 정보를 한곳에서 제공합니다.
- 세 모듈은 기기 내 `files/OshicueKit/`의 `schedule.json`, `ledger.json`, `diary.json`에 각각 저장되며 원자적으로 기록됩니다. 핵심 기능은 오프라인에서도 동작합니다.

## 빌드 및 실행

요구 사항:

- JDK 17 이상(Android Studio 내장 JBR 또는 JDK 21+ 권장)
- Android SDK Platform 37과 Build-Tools 36(Android Studio에서 자동 설치 가능)
- 저장소 루트에 `local.properties`를 만들고 `sdk.dir=/path/to/Android/sdk`를 쓰거나 `ANDROID_HOME` 환경 변수 설정
- 첫 빌드에는 Maven 저장소 접근 네트워크가 필요합니다. Gradle Wrapper가 알맞은 버전을 자동으로 내려받습니다

```sh
./gradlew :app:assembleDebug        # 디버그 APK 빌드
./gradlew :app:installDebug         # 연결된 기기/에뮬레이터에 설치
./gradlew :app:testDebugUnitTest    # 단위 테스트 93개(iOS 테스트와 1:1 대응)
./gradlew :app:lintDebug            # 정적 검사
./gradlew :app:assembleRelease      # 릴리스 APK 빌드
```

### 릴리스 서명

저장소에는 서명 자료가 포함되지 않습니다. 배포 가능한 릴리스 APK를 만들려면 저장소 루트에 `keystore.properties`를 만드세요(이 파일과 `*.jks`는 `.gitignore`에서 제외됩니다).

```sh
keytool -genkeypair -v -keystore oshicue-kit-release.jks -alias oshicue-kit \
  -keyalg RSA -keysize 2048 -validity 10000
```

```properties
storeFile=oshicue-kit-release.jks
storePassword=비밀번호
keyAlias=oshicue-kit
keyPassword=비밀번호
```

그다음 `./gradlew :app:assembleRelease`를 실행하면 `app/build/outputs/apk/release/app-release.apk`가 만들어집니다(`keystore.properties`가 없으면 서명되지 않은 APK). `oshicue-kit-release.jks`와 비밀번호는 반드시 백업하세요. 이후 버전도 같은 서명을 사용해야 합니다.

## 데이터 형식과 호환성

- 세 모듈의 JSON 형식은 iOS판과 완전히 같습니다(`schemaVersion`은 `1`). 앱에서 내보낸／가져오는 백업은 iOS판과 서로 사용할 수 있고, 기기 내 데이터 파일 구조도 동일합니다.
- 타임스탬프는 밀리초가 포함된 ISO 8601(`yyyy-MM-ddTHH:mm:ss.SSSZ`), 날짜는 그레고리력 `yyyy-MM-dd`, 금액은 정수 최소 통화 단위(`unitPriceMinor`)입니다.
- 가져오기는 항상 검증과 미리보기를 거친 뒤 적용합니다. 같은 ID에 내용이 다른 기록은 가져오기 전체를 거부해 기록을 덮어쓰지 않습니다.
- `Templates/`와 `docs/`는 iOS판의 형식 템플릿과 문서를 그대로 이어받았습니다.

## 현지화

간체 중국어, 번체 중국어(대만), 번체 광둥어(홍콩), English, 日本語, 한국어, ไทย, Bahasa Indonesia, Bahasa Melayu 9개 언어가 포함되어 있으며 리소스는 `app/src/main/res/values-*/strings.xml`에 있습니다.

`tools/convert_localization.py`는 iOS판 `Localizable.strings`에서 이 저장소의 언어 리소스를 다시 생성하는 스크립트입니다(`%@` → `%1$s` 위치 인수 변환 포함). Android판만 관리한다면 해당 `strings.xml`을 직접 편집하면 됩니다.

## 디렉터리 구조

| 경로 | 내용 |
| --- | --- |
| `app/src/main/kotlin/com/oshicue/kit/core/` | 모델, JSON 가져오기/내보내기, 통계와 순위, 시간축 레이아웃 |
| `app/src/main/kotlin/com/oshicue/kit/data/` | 세 개의 독립 Store, 원자적 쓰기와 복구 백업 |
| `app/src/main/kotlin/com/oshicue/kit/i18n/` | 언어 해석과 문자열 조회 |
| `app/src/main/kotlin/com/oshicue/kit/ui/` | Compose 화면: 시간표, 수첩, 일기, 설정 및 모든 편집·백업 화면 |
| `app/src/main/res/` | 9개 언어 리소스, 런처 아이콘, Unbounded 글꼴 |
| `app/src/main/assets/` | 내장 가져오기 템플릿 |
| `app/src/test/` | 단위 테스트(가져오기 검증, 통화 정밀도, 동점 순위, 시간축 레이아웃, UTF-8 처리 등) |
| `tools/` | 현지화 리소스 변환 스크립트 |
| `Templates/`、`docs/` | JSON 템플릿과 형식 문서(iOS판에서 이어받음) |

## iOS판과의 차이

- 플랫폼 구성 요소: 파일 가져오기/내보내기는 시스템 파일 선택기, 아바타는 시스템 사진 선택기(512px로 축소, JPEG 0.8, Base64 저장 — iOS와 동일), 공유는 시스템 공유 시트를 사용합니다.
- 접근성: iOS의 VoiceOver 사용자 동작은 Compose의 탭／더블 탭 제스처와 시맨틱으로 대응됩니다.
- 그 외의 상호 작용 흐름, 경계 조건, JSON 데이터 형식은 iOS판과 일치합니다.

## 라이선스

이 프로젝트는 원 프로젝트의 [MIT License](LICENSE)를 따릅니다. 서드파티 구성 요소는 각자의 라이선스를 유지합니다. 내장된 Unbounded 글꼴은 [SIL OFL 1.1](THIRD_PARTY_NOTICES)이며, 자세한 내용은 [서드파티 고지](THIRD_PARTY_NOTICES)를 참고하세요.

Oshicue Kit 원본은 SwiftUI 기반 네이티브 iOS 앱(0.4.0, build 10)이며, 이 저장소는 그 Android 이식판입니다.
