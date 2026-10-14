# Oshicue Kit 브랜드 자료

[简体中文](README.md) | [繁體中文](README.zh-Hant.md) | [繁體粵語](README.zh-HK.md) | [English](README.en.md) | [日本語](README.ja.md) | [한국어](README.ko.md) | [ไทย](README.th.md) | [Bahasa Indonesia](README.id.md) | [Bahasa Melayu](README.ms.md)

워드마크는 `oshicue.html`의 Unbounded 글꼴을 사용하며 굵기 `900`, 자간 `0.02em`을 유지합니다. `OSHI`와 `KIT`은 `#f4f3ee`, `CUE`는 강조색 `#ff3d6e`, 아이콘 배경은 `#0a0a0d`를 사용합니다. 브랜드 이름은 `OSHICUE KIT`을 유지합니다.

## 자료와 앱 리소스

| 파일 | 용도 |
| --- | --- |
| [app-icon.png](app-icon.png) | `1024 × 1024`, 불투명한 정사각형 iOS 아이콘 원본. 둥근 모서리는 시스템이 적용합니다. |
| [wordmark.png](wordmark.png) | 투명 배경의 전체 워드마크. 현재 크기는 `948 × 133`이며 밝은 글자색은 어두운 배경에 적합합니다. |
| [wordmark.svg](wordmark.svg) | 글꼴 윤곽으로 생성한 벡터 워드마크. 글꼴 설치가 필요 없습니다. |
| [brand.json](brand.json) | 글꼴 경로, PostScript 이름, 굵기, 자간, 색상, 워드마크 크기 기록. |

앱 리소스 폴더 [Assets.xcassets](../../OshicueKit/Resources/Assets.xcassets)에 `AppIcon`, `BrandWordmark`, `AccentColor`가 있습니다. SwiftUI에서 워드마크를 바로 표시할 수 있습니다.

```swift
Image("BrandWordmark").resizable().scaledToFit()
```

내장 글꼴의 등록 이름은 `Unbounded-Regular`이며 `Font.custom("Unbounded-Regular", size: …).weight(.black)`을 사용할 수 있습니다. 생성 스크립트는 OpenType `wght` 축을 명시적으로 `900`으로 설정합니다. CoreText는 인스턴스 이름 `Unbounded-Regular_Black`을 반환하지만 등록 글꼴 이름으로는 계속 `Unbounded-Regular`를 사용해야 합니다.

## 다시 생성하기

Swift가 설치된 macOS에서 프로젝트 루트 기준으로 실행합니다.

```sh
CLANG_MODULE_CACHE_PATH=/tmp/oshicue-clang-cache swift scripts/generate_brand.swift
```

[generate_brand.swift](../../scripts/generate_brand.swift)는 AppKit, CoreText, 내장 [Unbounded.ttf](../../OshicueKit/Resources/Fonts/Unbounded.ttf)를 사용하며 글꼴 다운로드나 설치가 필요 없습니다. 이 폴더의 PNG 2개, SVG, `brand.json`을 덮어쓰고 `AppIcon.appiconset/AppIcon.png`와 `BrandWordmark.imageset/wordmark.png`를 갱신합니다. `AccentColor`나 리소스 폴더의 `Contents.json`은 생성하지 않습니다.

규격을 변경할 때 스크립트와 관련 리소스를 함께 수정하세요. 다시 생성한 뒤 아이콘 크기와 불투명도, 워드마크 투명 배경, SVG 윤곽, 메타데이터를 확인합니다. 투명 워드마크는 어두운 배경에서 확인해야 합니다. 이 문서는 이번 작업에서 자료를 다시 생성하거나 검증했다고 선언하지 않습니다.

## 글꼴 라이선스

내장 Unbounded 글꼴에는 [SIL Open Font License 1.1](../../OshicueKit/Resources/Fonts/OFL.txt)이 포함되어 있으며 저작권자는 `The Unbounded Project Authors`(2022)입니다. 글꼴을 배포할 때 저작권 고지와 라이선스를 보존하고 전체 라이선스의 수정, 이름 지정, 배포 조건을 따라야 합니다. 이 글꼴로 만든 문서에 같은 라이선스를 적용할 의무는 없습니다. 이 문서는 브랜드 이름이나 브랜드 자료에 별도의 라이선스를 부여하지 않습니다.

[README로 돌아가기](../../README.ko.md) · 관련: [전체 검증](../VERIFICATION.ko.md) · [시간표 템플릿](../../Templates/timetable-template.json) · [수첩 템플릿](../../Templates/ledger-template.json) · [일기 템플릿](../../Templates/diary-template.json)
