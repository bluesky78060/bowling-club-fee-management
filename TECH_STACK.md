# 기술 스택 제안서

**프로젝트**: 볼링 동호회 회비 관리 앱
**플랫폼**: Android
**작성일**: 2026-01-12

---

## 1. 권장 기술 스택 (Option A: Kotlin + Jetpack Compose)

### 1.1 개요

최신 Android 개발 표준을 따르는 네이티브 앱 개발 스택입니다.

```
┌─────────────────────────────────────────────────┐
│                    UI Layer                      │
│              Jetpack Compose (선언형 UI)          │
├─────────────────────────────────────────────────┤
│                 ViewModel Layer                  │
│         Android ViewModel + StateFlow           │
├─────────────────────────────────────────────────┤
│                 Domain Layer                     │
│              Use Cases / Repository              │
├─────────────────────────────────────────────────┤
│                  Data Layer                      │
│                Room Database                     │
└─────────────────────────────────────────────────┘
```

### 1.2 상세 기술 스택

| 영역 | 기술 | 버전 | 선택 이유 |
|------|------|------|----------|
| 언어 | Kotlin | 1.9+ | Android 공식 언어, 간결한 문법 |
| UI | Jetpack Compose | 1.5+ | 선언형 UI, 빠른 개발 속도 |
| 아키텍처 | MVVM | - | Google 권장 패턴 |
| 로컬 DB | Room | 2.6+ | SQLite 추상화, 타입 안전성 |
| DI | Hilt | 2.48+ | 의존성 주입 표준 |
| 비동기 | Coroutines + Flow | 1.7+ | 비동기 처리 표준 |
| 네비게이션 | Navigation Compose | 2.7+ | 화면 전환 관리 |
| 차트 | Vico / MPAndroidChart | - | 통계 시각화 |
| **OCR** | **ML Kit Text Recognition** | **16.0+** | **영수증 스캔 및 텍스트 인식** |
| 카메라 | CameraX | 1.3+ | 카메라 촬영 및 이미지 처리 |

### 1.3 프로젝트 구조

```
app/
├── src/main/
│   ├── java/com/bowlingclub/
│   │   ├── di/                    # Hilt 모듈
│   │   │   └── DatabaseModule.kt
│   │   │
│   │   ├── data/                  # 데이터 계층
│   │   │   ├── local/
│   │   │   │   ├── database/
│   │   │   │   │   ├── AppDatabase.kt
│   │   │   │   │   ├── dao/
│   │   │   │   │   │   ├── MemberDao.kt
│   │   │   │   │   │   ├── PaymentDao.kt
│   │   │   │   │   │   ├── AccountDao.kt
│   │   │   │   │   │   ├── ScoreDao.kt
│   │   │   │   │   │   └── TeamDao.kt
│   │   │   │   │   └── entity/
│   │   │   │   │       ├── MemberEntity.kt
│   │   │   │   │       ├── PaymentEntity.kt
│   │   │   │   │       └── ...
│   │   │   │   └── datastore/
│   │   │   │       └── SettingsDataStore.kt
│   │   │   │
│   │   │   └── repository/
│   │   │       ├── MemberRepository.kt
│   │   │       ├── PaymentRepository.kt
│   │   │       └── ...
│   │   │
│   │   ├── domain/                # 도메인 계층
│   │   │   ├── model/
│   │   │   │   ├── Member.kt
│   │   │   │   ├── Payment.kt
│   │   │   │   └── ...
│   │   │   └── usecase/
│   │   │       ├── member/
│   │   │       ├── payment/
│   │   │       ├── score/
│   │   │       │   ├── CalculateAverageUseCase.kt
│   │   │       │   └── CalculateHandicapUseCase.kt
│   │   │       └── ...
│   │   │
│   │   ├── ui/                    # UI 계층
│   │   │   ├── theme/
│   │   │   │   ├── Color.kt
│   │   │   │   ├── Theme.kt
│   │   │   │   └── Type.kt
│   │   │   │
│   │   │   ├── components/        # 공통 컴포넌트
│   │   │   │   ├── CommonButton.kt
│   │   │   │   ├── MemberCard.kt
│   │   │   │   └── ScoreInputField.kt
│   │   │   │
│   │   │   ├── navigation/
│   │   │   │   └── AppNavigation.kt
│   │   │   │
│   │   │   └── screens/
│   │   │       ├── home/
│   │   │       │   ├── HomeScreen.kt
│   │   │       │   └── HomeViewModel.kt
│   │   │       ├── member/
│   │   │       │   ├── MemberListScreen.kt
│   │   │       │   ├── MemberDetailScreen.kt
│   │   │       │   └── MemberViewModel.kt
│   │   │       ├── payment/
│   │   │       ├── account/
│   │   │       └── score/
│   │   │
│   │   └── BowlingClubApp.kt      # Application 클래스
│   │
│   └── res/
│       ├── values/
│       │   ├── strings.xml
│       │   └── colors.xml
│       └── ...
│
├── build.gradle.kts
└── proguard-rules.pro
```

### 1.4 build.gradle.kts (앱 모듈)

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.bowlingclub.fee"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bowlingclub.fee"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
}

dependencies {
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48.1")
    ksp("com.google.dagger:hilt-compiler:2.48.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Chart (Vico)
    implementation("com.patrykandpatrick.vico:compose:1.13.1")
    implementation("com.patrykandpatrick.vico:compose-m3:1.13.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ML Kit OCR (영수증 스캔)
    implementation("com.google.mlkit:text-recognition-korean:16.0.0")

    // CameraX (카메라 촬영)
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
}
```

### 1.5 영수증 OCR 스캔 기능

#### 1.5.1 기능 개요
카메라로 영수증을 촬영하면 금액, 날짜, 상호명 등을 자동으로 인식하여 장부 입력을 간편하게 합니다.

```
┌─────────────────────────────────┐
│  📷 영수증 스캔                  │
├─────────────────────────────────┤
│                                 │
│     [카메라 프리뷰 영역]          │
│                                 │
│     ┌───────────────┐          │
│     │   영수증을     │          │
│     │   프레임 안에   │          │
│     │   맞춰주세요    │          │
│     └───────────────┘          │
│                                 │
├─────────────────────────────────┤
│        [📸 촬영]                 │
└─────────────────────────────────┘

        ↓ 인식 결과

┌─────────────────────────────────┐
│  ✅ 인식 완료                    │
├─────────────────────────────────┤
│  상호명: 행복볼링장               │
│  날짜: 2026-01-12               │
│  금액: 85,000원                 │
├─────────────────────────────────┤
│  [수정]  [장부에 등록]            │
└─────────────────────────────────┘
```

#### 1.5.2 인식 항목
| 항목 | 인식 패턴 | 정확도 |
|------|----------|--------|
| 금액 | 숫자 + "원" 패턴 | 높음 |
| 날짜 | YYYY-MM-DD, YY/MM/DD 등 | 높음 |
| 상호명 | 영수증 상단 텍스트 | 중간 |
| 사업자번호 | XXX-XX-XXXXX 패턴 | 높음 |

#### 1.5.3 사용 시나리오
1. 장부 등록 화면에서 "영수증 스캔" 버튼 탭
2. 카메라로 영수증 촬영
3. OCR로 텍스트 인식 및 파싱
4. 인식된 정보 확인/수정
5. 장부에 자동 등록

#### 1.5.4 기술 구현
```kotlin
// ML Kit Text Recognition 사용 예시
val recognizer = TextRecognition.getClient(
    KoreanTextRecognizerOptions.Builder().build()
)

fun processImage(image: InputImage) {
    recognizer.process(image)
        .addOnSuccessListener { visionText ->
            parseReceipt(visionText.text)
        }
}

fun parseReceipt(text: String): ReceiptData {
    // 금액 추출: "85,000원" 또는 "85000" 패턴
    val amountRegex = """(\d{1,3}(,\d{3})*)\s*원?""".toRegex()

    // 날짜 추출: "2026-01-12" 또는 "26/01/12" 패턴
    val dateRegex = """\d{2,4}[-/]\d{2}[-/]\d{2}""".toRegex()

    return ReceiptData(
        amount = amountRegex.find(text)?.value,
        date = dateRegex.find(text)?.value,
        rawText = text
    )
}
```

#### 1.5.5 제한 사항
- 선명한 이미지 필요 (흐릿하면 인식률 저하)
- 한글/숫자 혼합 인식 최적화
- 오프라인 동작 (ML Kit 온디바이스 모델)
- 앱 크기 약 20MB 증가 (OCR 모델 포함)

---

### 1.6 장단점

**장점**
- Google 공식 권장 스택
- 최신 Android 개발 트렌드
- 선언형 UI로 빠른 개발
- 강력한 타입 안전성
- 풍부한 라이브러리 생태계
- **ML Kit으로 오프라인 OCR 지원**

**단점**
- Kotlin 학습 필요 (Java 개발자의 경우)
- Compose 러닝 커브
- 앱 크기가 다소 큼 (OCR 모델 포함 시 약 40MB)

---

## 2. 대안 기술 스택 (Option B: Flutter)

### 2.1 개요

크로스 플랫폼 프레임워크로, 향후 iOS 확장 시 유리합니다.

```
┌─────────────────────────────────────────────────┐
│                    UI Layer                      │
│              Flutter Widgets                     │
├─────────────────────────────────────────────────┤
│               State Management                   │
│            Riverpod / Provider                   │
├─────────────────────────────────────────────────┤
│                  Data Layer                      │
│               Drift (SQLite)                     │
└─────────────────────────────────────────────────┘
```

### 2.2 상세 기술 스택

| 영역 | 기술 | 버전 | 선택 이유 |
|------|------|------|----------|
| 언어 | Dart | 3.0+ | Flutter 전용 언어 |
| UI | Flutter | 3.16+ | 크로스 플랫폼 UI |
| 상태관리 | Riverpod | 2.4+ | 타입 안전 상태관리 |
| 로컬 DB | Drift | 2.14+ | 타입 안전 SQLite |
| 네비게이션 | go_router | 12.0+ | 선언형 라우팅 |
| 차트 | fl_chart | 0.65+ | Flutter 네이티브 차트 |

### 2.3 프로젝트 구조

```
lib/
├── main.dart
├── app/
│   ├── app.dart
│   └── router.dart
├── core/
│   ├── constants/
│   ├── theme/
│   └── utils/
├── data/
│   ├── database/
│   │   ├── app_database.dart
│   │   └── tables/
│   ├── models/
│   └── repositories/
├── features/
│   ├── home/
│   ├── member/
│   ├── payment/
│   ├── account/
│   └── score/
└── shared/
    └── widgets/
```

### 2.4 장단점

**장점**
- iOS 확장 용이 (코드 재사용)
- 빠른 UI 개발
- Hot Reload로 빠른 개발 사이클
- 일관된 UI (플랫폼 독립적)

**단점**
- Dart 학습 필요
- 앱 크기가 큼 (약 10-20MB 추가)
- 플랫폼 네이티브 기능 접근 시 브릿지 필요

---

## 3. 최종 권장 사항

### 3.1 권장 스택: Option A (Kotlin + Jetpack Compose)

**선택 이유**

| 기준 | 평가 |
|------|------|
| 현재 요구사항 | Android 전용이므로 네이티브 최적 |
| 성능 | 네이티브가 더 우수 |
| 앱 크기 | 네이티브가 더 작음 |
| 유지보수 | Google 공식 지원 |
| 향후 확장 | iOS 필요시 KMP 고려 가능 |

### 3.2 개발 환경 설정

```
필수 도구:
- Android Studio Hedgehog (2023.1.1) 이상
- JDK 17
- Kotlin 1.9+
- Gradle 8.0+

권장 플러그인:
- Kotlin
- Android Compose
- Database Navigator (SQLite 확인용)
```

### 3.3 예상 개발 일정

| 단계 | 작업 | 예상 기간 |
|------|------|----------|
| 1 | 프로젝트 설정 + DB 스키마 | 1주 |
| 2 | 회원 관리 기능 | 1주 |
| 3 | 회비 관리 기능 | 1주 |
| 4 | 장부 기능 | 1주 |
| 5 | 점수 관리 + 핸디캡 | 2주 |
| 6 | 팀전 기능 | 1주 |
| 7 | 홈 대시보드 + 통계 | 1주 |
| 8 | 테스트 + 버그 수정 | 1주 |
| **총계** | | **9주** |

### 3.4 MVP 범위 (4주 목표)

빠른 출시를 위한 최소 기능:

1. **회원 관리**: 등록/조회/수정
2. **회비 납부**: 등록/조회
3. **장부**: 수입/지출 등록 및 잔액 표시
4. **점수 입력**: 기본 점수 입력 및 에버리지 계산

**MVP 이후 추가**:
- 핸디캡 시스템
- 팀전 기능
- 상세 통계/차트
- 데이터 백업/복원

---

## 4. 참고 자료

### 4.1 공식 문서
- [Jetpack Compose 가이드](https://developer.android.com/jetpack/compose)
- [Room 가이드](https://developer.android.com/training/data-storage/room)
- [Hilt 가이드](https://developer.android.com/training/dependency-injection/hilt-android)

### 4.2 아키텍처 참고
- [Android 앱 아키텍처 가이드](https://developer.android.com/topic/architecture)
- [Now in Android (공식 샘플)](https://github.com/android/nowinandroid)

---

## 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-12 | Claude | 초안 작성 |
