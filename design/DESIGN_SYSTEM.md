# Strike! 디자인 시스템

**프로젝트**: 볼링 동호회 회비 관리 앱
**버전**: 1.0
**작성일**: 2026-01-12

---

## 디자인 컨셉

### Retro-Sport Scorecard

1980-90년대 볼링장의 네온 감성과 디지털 점수판의 그리드 미학을 현대적으로 재해석한 디자인입니다.

**핵심 모티브**
- 볼링 레인의 곡선과 나무 질감
- 형광 점수판의 빛나는 숫자들
- 네온 조명의 글로우 효과
- 볼링 핀의 기하학적 형태

**디자인 원칙**
1. **대비**: 어두운 배경에 밝은 네온 컬러로 명확한 시각적 계층 구조
2. **가독성**: 점수판 스타일의 고정폭 숫자 폰트로 데이터 가독성 강화
3. **피드백**: 스트라이크, 스페어 등 볼링 이벤트를 연상시키는 시각적 피드백
4. **효율성**: 볼링장 현장에서 빠른 데이터 입력이 가능한 UI

---

## 색상 시스템

### Primary Colors - Neon Bowling Alley

```css
--neon-pink: #FF1F8F;      /* 주요 액센트, CTA 버튼 */
--neon-cyan: #00F5FF;      /* 데이터 강조, 링크 */
--neon-yellow: #FFE600;    /* 1등, 최고 점수, 경고 */
--neon-orange: #FF6B35;    /* FAB, 빠른 액션 */
--neon-green: #39FF14;     /* 성공, 활성 상태 */
```

### Dark Base - Bowling Alley Atmosphere

```css
--dark-void: #0A0A0F;      /* 가장 어두운 배경 */
--dark-lane: #12121A;      /* 기본 배경 */
--dark-gutter: #1A1A24;    /* 카드 배경 */
--dark-surface: #22222E;   /* 입력 필드 배경 */
--dark-elevated: #2A2A38;  /* 호버 상태 */
```

### Functional Colors

```css
--income-green: #00E676;   /* 수입, 납부 완료 */
--expense-red: #FF5252;    /* 지출, 미납 */
--warning-amber: #FFD740;  /* 경고, 휴면 상태 */
--info-blue: #40C4FF;      /* 정보성 메시지 */
```

### Text Colors

```css
--text-primary: #FFFFFF;              /* 100% */
--text-secondary: rgba(255,255,255,0.7);  /* 70% */
--text-tertiary: rgba(255,255,255,0.4);   /* 40% */
--text-on-neon: #0A0A0F;              /* 밝은 배경 위 텍스트 */
```

### Gradients

```css
--gradient-neon: linear-gradient(135deg, #FF1F8F 0%, #00F5FF 100%);
--gradient-strike: linear-gradient(135deg, #FF6B35 0%, #FFE600 100%);
--gradient-dark: linear-gradient(180deg, #12121A 0%, #0A0A0F 100%);
```

### Glow Effects

```css
--glow-pink: 0 0 20px rgba(255, 31, 143, 0.5), 0 0 40px rgba(255, 31, 143, 0.3);
--glow-cyan: 0 0 20px rgba(0, 245, 255, 0.5), 0 0 40px rgba(0, 245, 255, 0.3);
--glow-yellow: 0 0 20px rgba(255, 230, 0, 0.5), 0 0 40px rgba(255, 230, 0, 0.3);
```

---

## 타이포그래피

### Font Families

```css
--font-display: 'Orbitron', sans-serif;  /* 점수, 금액, 헤더 */
--font-body: 'Noto Sans KR', sans-serif; /* 본문, 설명 */
```

### Type Scale

| 용도 | Font | Size | Weight | Letter Spacing |
|------|------|------|--------|----------------|
| 대형 금액 | Orbitron | 2.75rem | 800 | 1px |
| 점수/순위 | Orbitron | 1.5rem | 700 | 0 |
| 섹션 타이틀 | Orbitron | 0.875rem | 600 | 2px |
| 본문 | Noto Sans KR | 1rem | 400 | 0 |
| 캡션 | Noto Sans KR | 0.75rem | 500 | 0.5px |
| 배지 | Noto Sans KR | 0.625rem | 600 | 0.5px |

### 사용 가이드

**Orbitron (디스플레이)**
- 금액 표시 (잔액, 정산금, 회비 등)
- 점수 및 에버리지
- 랭킹 순위
- 섹션 제목
- 날짜/시간

**Noto Sans KR (본문)**
- 회원 이름
- 설명 텍스트
- 버튼 레이블
- 메모 및 입력 필드
- 내비게이션 레이블

---

## 간격 시스템

```css
--spacing-xs: 4px;
--spacing-sm: 8px;
--spacing-md: 16px;
--spacing-lg: 24px;
--spacing-xl: 32px;
--spacing-2xl: 48px;
```

### 사용 패턴

| 요소 | 내부 패딩 | 외부 마진 |
|------|----------|----------|
| 카드 | lg (24px) | md (16px) |
| 리스트 아이템 | md (16px) | sm (8px) |
| 버튼 | md × xl | - |
| 입력 필드 | md (16px) | - |
| 섹션 | - | lg (24px) |

---

## 둥근 모서리

```css
--radius-sm: 8px;     /* 입력 필드, 배지 */
--radius-md: 12px;    /* 카드, 버튼 */
--radius-lg: 20px;    /* 큰 카드, 모달 */
--radius-full: 9999px; /* 아바타, 필터 탭 */
```

---

## 컴포넌트

### 1. 카드 (Card)

```css
.card {
    background: var(--dark-lane);
    border-radius: var(--radius-lg);
    padding: var(--spacing-lg);
    border: 1px solid rgba(255, 255, 255, 0.05);
}

.card:hover {
    border-color: rgba(255, 255, 255, 0.1);
    background: var(--dark-gutter);
}
```

### 2. 버튼 (Button)

**Primary Button**
```css
.btn-primary {
    background: var(--gradient-neon);
    color: var(--text-on-neon);
    box-shadow: 0 4px 20px rgba(255, 31, 143, 0.3);
    padding: var(--spacing-md) var(--spacing-xl);
    border-radius: var(--radius-md);
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 1px;
}
```

**Secondary Button**
```css
.btn-secondary {
    background: var(--dark-surface);
    color: var(--text-primary);
    border: 1px solid rgba(255, 255, 255, 0.1);
}
```

**FAB (Floating Action Button)**
```css
.fab {
    width: 56px;
    height: 56px;
    border-radius: 50%;
    background: var(--gradient-strike);
    box-shadow: 0 4px 20px rgba(255, 107, 53, 0.4);
}
```

### 3. 입력 필드 (Input)

```css
.input {
    background: var(--dark-surface);
    border: 2px solid rgba(255, 255, 255, 0.1);
    border-radius: var(--radius-md);
    padding: var(--spacing-md);
    color: var(--text-primary);
}

.input:focus {
    border-color: var(--neon-cyan);
    box-shadow: 0 0 0 4px rgba(0, 245, 255, 0.1);
}
```

**점수 입력 필드**
```css
.score-input {
    font-family: var(--font-display);
    font-size: 1.125rem;
    font-weight: 700;
    color: var(--neon-cyan);
    text-align: center;
}
```

### 4. 아바타 (Avatar)

```css
.avatar {
    width: 48px;
    height: 48px;
    border-radius: 50%;
    background: var(--gradient-neon);
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 700;
    color: var(--text-on-neon);
}

/* 성별에 따른 색상 */
.avatar.male { background: linear-gradient(135deg, #4A90D9 0%, #357ABD 100%); }
.avatar.female { background: linear-gradient(135deg, #FF6B9D 0%, #FF1F8F 100%); }
```

### 5. 배지 (Badge)

```css
.badge {
    font-size: 0.625rem;
    padding: 2px 6px;
    border-radius: var(--radius-full);
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.5px;
}

.badge.active { background: rgba(0, 230, 118, 0.1); color: var(--income-green); }
.badge.dormant { background: rgba(255, 215, 64, 0.1); color: var(--warning-amber); }
.badge.paid { background: rgba(0, 230, 118, 0.1); color: var(--income-green); }
.badge.unpaid { background: rgba(255, 82, 82, 0.1); color: var(--expense-red); }
```

### 6. 네비게이션 (Navigation)

**하단 네비게이션**
```css
.bottom-nav {
    background: rgba(10, 10, 15, 0.95);
    backdrop-filter: blur(20px);
    border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.nav-item.active::before {
    /* 상단 인디케이터 */
    width: 24px;
    height: 3px;
    background: var(--gradient-neon);
}
```

### 7. 프로그레스 바 (Progress Bar)

```css
.progress-bar {
    height: 8px;
    background: var(--dark-surface);
    border-radius: var(--radius-full);
}

.progress-fill {
    background: var(--gradient-neon);
    border-radius: var(--radius-full);
}
```

---

## 애니메이션

### Transitions

```css
--transition-fast: 0.15s ease;   /* 호버, 포커스 */
--transition-base: 0.25s ease;   /* 일반적인 전환 */
--transition-slow: 0.4s ease;    /* 페이지 전환, 모달 */
```

### 주요 애니메이션

**펄스 (상태 표시)**
```css
@keyframes pulse {
    0%, 100% { opacity: 1; transform: scale(1); }
    50% { opacity: 0.6; transform: scale(0.9); }
}
```

**스트라이크 글로우 (점수 300점)**
```css
@keyframes strikeGlow {
    0%, 100% { box-shadow: 0 0 0 rgba(255, 230, 0, 0); }
    50% { box-shadow: 0 0 30px rgba(255, 230, 0, 0.8); }
}
```

**페이드 인**
```css
@keyframes fadeIn {
    from { opacity: 0; transform: translateY(10px); }
    to { opacity: 1; transform: translateY(0); }
}
```

---

## 아이콘

앱에서 사용하는 이모지 아이콘:

| 기능 | 아이콘 |
|------|-------|
| 홈 | 🏠 |
| 회원 | 👥 |
| 회비 | 💰 |
| 장부 | 📒 |
| 점수 | 🎳 |
| 설정 | ⚙️ |
| 알림 | 🔔 |
| 검색 | 🔍 |
| 카메라/영수증 | 📷 |
| 모임 | 📋 |
| 수입 | 💵 |
| 지출 | 💸 |
| 식비 | 🍽️ |
| 찬조 | 🎁 |
| 트로피/랭킹 | 🏆 |
| 위치 | 📍 |

---

## 화면 구성

### 화면 목록

1. **홈 대시보드** (`index.html`)
   - 잔액 히어로 카드
   - 빠른 액션 그리드
   - 에버리지 TOP 3
   - 최근 거래 내역

2. **회원 관리** (`screens.html#member-list`)
   - 검색 바
   - 필터 탭 (활동/휴면/탈퇴)
   - 회원 카드 리스트
   - FAB (회원 추가)

3. **점수 입력** (`screens.html#score-input`)
   - 모임 정보 카드
   - 점수 입력 그리드
   - 회원/게임 추가 버튼

4. **회비 관리** (`screens.html#payment-status`)
   - 월 선택기
   - 납부 요약 카드
   - 납부 현황 그리드

5. **모임비 정산** (`screens.html#settlement`)
   - 총 비용 히어로
   - 1인당 부담금 카드
   - 수금 현황 프로그레스
   - 회원별 정산 상태

---

## Jetpack Compose 변환 가이드

### Color.kt
```kotlin
object AppColors {
    val NeonPink = Color(0xFFFF1F8F)
    val NeonCyan = Color(0xFF00F5FF)
    val NeonYellow = Color(0xFFFFE600)
    val NeonOrange = Color(0xFFFF6B35)

    val DarkVoid = Color(0xFF0A0A0F)
    val DarkLane = Color(0xFF12121A)
    val DarkGutter = Color(0xFF1A1A24)
    val DarkSurface = Color(0xFF22222E)

    val IncomeGreen = Color(0xFF00E676)
    val ExpenseRed = Color(0xFFFF5252)
}
```

### Type.kt
```kotlin
val OrbitronFontFamily = FontFamily(
    Font(R.font.orbitron_regular, FontWeight.Normal),
    Font(R.font.orbitron_bold, FontWeight.Bold),
    Font(R.font.orbitron_black, FontWeight.Black)
)

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = OrbitronFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 44.sp
    ),
    titleMedium = TextStyle(
        fontFamily = OrbitronFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 2.sp
    ),
    // ...
)
```

---

## 파일 구조

```
design/
├── index.html          # 홈 화면 프로토타입
├── screens.html        # 추가 화면 모음
├── DESIGN_SYSTEM.md    # 이 문서
└── assets/            # (향후) 이미지, 아이콘 리소스
```

---

## 참고 자료

- [Material Design 3](https://m3.material.io/)
- [Google Fonts - Orbitron](https://fonts.google.com/specimen/Orbitron)
- [Google Fonts - Noto Sans KR](https://fonts.google.com/noto/specimen/Noto+Sans+KR)

---

## 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-12 | Claude | 초안 작성 |
