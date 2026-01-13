# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

볼링 동호회 회비 관리 Android 앱 (Strike!)
- 회원 관리, 회비 납부, 수입/지출 장부, 점수 관리 기능 제공
- Kotlin + Jetpack Compose 기반 네이티브 앱

## Build Commands

```bash
# 빌드
./gradlew assembleDebug

# 에뮬레이터 설치
./gradlew installDebug

# 클린 빌드
./gradlew clean assembleDebug

# 테스트
./gradlew testDebugUnitTest

# 린트
./gradlew lintDebug
```

## Architecture

### 계층 구조 (Clean Architecture + MVVM)

```
app/src/main/java/com/bowlingclub/fee/
├── di/                    # Hilt DI 모듈
│   └── DatabaseModule.kt  # Room DB, DAO 프로바이더
├── data/
│   ├── local/database/
│   │   ├── AppDatabase.kt         # Room Database (version 2)
│   │   ├── dao/                   # DAO 인터페이스
│   │   └── entity/                # Room Entity 클래스
│   └── repository/                # Repository 구현체
├── domain/model/                  # 도메인 모델 및 Enum
└── ui/
    ├── theme/                     # Color, Typography, Theme
    ├── components/                # 공통 Composable
    ├── navigation/AppNavigation.kt
    └── screens/{feature}/         # Screen + ViewModel
```

### 데이터 흐름
```
Compose UI → ViewModel → Repository → DAO → Room Database
         ← StateFlow ← Flow ← Flow ←
```

### 핵심 패턴

**ViewModel StateFlow 패턴**
```kotlin
private val _uiState = MutableStateFlow(UiState())
val uiState: StateFlow<UiState> = _uiState.asStateFlow()

// 상태 업데이트는 항상 update{} 사용 (thread-safe)
_uiState.update { it.copy(isLoading = true) }
```

**Repository Result 패턴**
```kotlin
// domain/model/Result.kt의 sealed class 사용
suspend fun insertMeeting(meeting: Meeting): Result<Long> =
    Result.runCatching { meetingDao.insert(MeetingEntity.fromDomain(meeting)) }
```

**Flow collect 메모리 누수 방지**
```kotlin
private var dataJob: Job? = null

fun loadData() {
    dataJob?.cancel()  // 이전 Job 취소 필수
    dataJob = viewModelScope.launch {
        repository.getAll().collect { ... }
    }
}
```

## Database

- Room Database version: 2
- 마이그레이션: `AppDatabase.MIGRATION_1_2`
- 주요 테이블: members, payments, accounts, meetings, scores, settlements

**Entity 변환 규칙**
```kotlin
// Entity → Domain
entity.toDomain()

// Domain → Entity (신규 생성)
MeetingEntity.fromDomain(meeting)

// Domain → Entity (수정, createdAt 유지)
MeetingEntity.fromDomain(meeting, preserveCreatedAt = true)
```

## Navigation

- Navigation Compose 사용
- 하단 네비게이션: Home, Member, Payment, Account, Score
- Screen 경로는 `AppNavigation.kt`의 `Screen` object에 정의

```kotlin
// 네비게이션 예시
navController.navigate(Screen.memberDetail(memberId))
navController.navigate(Screen.scoreInput(meetingId))
```

## Key Files

| 파일 | 설명 |
|------|------|
| `TASKS.md` | 개발 진행 상황 및 태스크 목록 |
| `TECH_STACK.md` | 기술 스택 상세 설명 |
| `design/DESIGN_SYSTEM.md` | UI/UX 디자인 시스템 |
| `PRD.md` | 제품 요구사항 문서 |

## Code Conventions

- Enum 사용: `MemberStatus`, `Gender`, `AccountType`, `SettlementStatus`
- Entity 상태 값은 DB에 소문자 문자열로 저장 (`"active"`, `"male"`)
- 날짜: `LocalDate` (epochDay로 저장), 시간: `LocalDateTime` (epochMilli로 저장)
- 에러 메시지는 한글로 사용자에게 표시

## 주의사항

- `Result.isError` 사용 (isFailure 아님 - 커스텀 Result 클래스)
- Flow `collect` 전에 기존 Job 취소 필수
- combine 람다 내에서 suspend 함수 직접 호출 금지 → collect 블록에서 호출
- ViewModel에서 hiltViewModel() 사용 시 parent entry 공유 확인
