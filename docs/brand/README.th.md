# สื่อแบรนด์ Oshicue Kit

[简体中文](README.md) | [繁體中文](README.zh-Hant.md) | [繁體粵語](README.zh-HK.md) | [English](README.en.md) | [日本語](README.ja.md) | [한국어](README.ko.md) | [ไทย](README.th.md) | [Bahasa Indonesia](README.id.md) | [Bahasa Melayu](README.ms.md)

ตัวอักษรแบรนด์ใช้ฟอนต์ Unbounded ตาม `oshicue.html` น้ำหนัก `900` ระยะห่างตัวอักษร `0.02em` ส่วน `OSHI` และ `KIT` ใช้ `#f4f3ee` ส่วน `CUE` ใช้สีเน้น `#ff3d6e` และพื้นหลังไอคอนใช้ `#0a0a0d` ชื่อแบรนด์คงเป็น `OSHICUE KIT`

## สื่อและทรัพยากรแอป

| ไฟล์ | การใช้งาน |
| --- | --- |
| [app-icon.png](app-icon.png) | ภาพต้นฉบับไอคอน iOS สี่เหลี่ยมจัตุรัสทึบแสง `1024 × 1024` ระบบจะเพิ่มมุมมนให้ |
| [wordmark.png](wordmark.png) | ตัวอักษรแบรนด์ครบชุดบนพื้นหลังโปร่งใส ขนาดปัจจุบัน `948 × 133` ตัวอักษรสีอ่อนเหมาะกับพื้นหลังเข้ม |
| [wordmark.svg](wordmark.svg) | ตัวอักษรแบรนด์แบบเวกเตอร์ที่สร้างจากเส้นขอบฟอนต์ ไม่ต้องติดตั้งฟอนต์ |
| [brand.json](brand.json) | บันทึกพาธฟอนต์ ชื่อ PostScript น้ำหนัก ระยะห่าง สี และขนาดตัวอักษรแบรนด์ |

โฟลเดอร์ทรัพยากรแอป [Assets.xcassets](../../OshicueKit/Resources/Assets.xcassets) มี `AppIcon`, `BrandWordmark` และ `AccentColor` แสดงตัวอักษรแบรนด์โดยตรงใน SwiftUI ได้:

```swift
Image("BrandWordmark").resizable().scaledToFit()
```

ชื่อที่ลงทะเบียนของฟอนต์ในแอปคือ `Unbounded-Regular` ใช้ `Font.custom("Unbounded-Regular", size: …).weight(.black)` ได้ สคริปต์สร้างกำหนดแกน OpenType `wght` เป็น `900` อย่างชัดเจน CoreText คืนชื่ออินสแตนซ์ `Unbounded-Regular_Black` แต่ชื่อฟอนต์ที่ลงทะเบียนยังต้องใช้ `Unbounded-Regular`

## สร้างใหม่

บน macOS ที่ติดตั้ง Swift ให้รันจากโฟลเดอร์หลักของโครงการ:

```sh
CLANG_MODULE_CACHE_PATH=/tmp/oshicue-clang-cache swift scripts/generate_brand.swift
```

[generate_brand.swift](../../scripts/generate_brand.swift) ใช้ AppKit, CoreText และ [Unbounded.ttf](../../OshicueKit/Resources/Fonts/Unbounded.ttf) ที่มีอยู่ในโครงการ ไม่ต้องดาวน์โหลดหรือติดตั้งฟอนต์ สคริปต์เขียนทับ PNG สองไฟล์ SVG และ `brand.json` ในโฟลเดอร์นี้ พร้อมอัปเดต `AppIcon.appiconset/AppIcon.png` และ `BrandWordmark.imageset/wordmark.png` แต่ไม่สร้าง `AccentColor` หรือ `Contents.json` ของทรัพยากร

เมื่อแก้ข้อกำหนด ให้ปรับสคริปต์และทรัพยากรที่เกี่ยวข้องพร้อมกัน หลังสร้างใหม่ตรวจขนาดและความทึบของไอคอน พื้นหลังโปร่งใสของตัวอักษรแบรนด์ เส้นขอบ SVG และเมตาดาตา ควรตรวจตัวอักษรโปร่งใสบนพื้นหลังเข้ม หน้านี้ไม่ได้ยืนยันว่ามีการสร้างใหม่หรือตรวจสื่อในรอบนี้แล้ว

## ใบอนุญาตฟอนต์

ฟอนต์ Unbounded ที่รวมมามี [SIL Open Font License 1.1](../../OshicueKit/Resources/Fonts/OFL.txt) ลิขสิทธิ์ของ `The Unbounded Project Authors` (2022) เมื่อแจกจ่ายฟอนต์ต้องเก็บข้อความลิขสิทธิ์และใบอนุญาต และปฏิบัติตามเงื่อนไขการแก้ไข การตั้งชื่อ และการแจกจ่ายในใบอนุญาตฉบับเต็ม ใบอนุญาตฟอนต์ไม่บังคับให้เอกสารที่สร้างด้วยฟอนต์ใช้ใบอนุญาตเดียวกัน หน้านี้ไม่ได้ให้ใบอนุญาตเพิ่มเติมสำหรับชื่อแบรนด์หรือสื่อแบรนด์

กลับไป [README](../../README.th.md) · ที่เกี่ยวข้อง: [การตรวจสอบโดยรวม](../VERIFICATION.th.md) · [แม่แบบตารางเวลา](../../Templates/timetable-template.json) · [แม่แบบสมุดบันทึก](../../Templates/ledger-template.json) · [แม่แบบบันทึกประจำวัน](../../Templates/diary-template.json)
