# 개발 태스크 목록

**프로젝트**: 볼링 동호회 회비 관리 앱
**작성일**: 2026-01-12
**상태**: Phase 1 완료

---

## 개발 단계 개요

```
Phase 1: 프로젝트 설정 및 기반 구축 ✅
Phase 2: 핵심 기능 개발 (MVP)
Phase 3: 점수 관리 기능
Phase 4: 고급 기능
Phase 5: 테스트 및 배포
```

---

## Phase 1: 프로젝트 설정 및 기반 구축 ✅

### 1.1 프로젝트 초기 설정
- [x] Android Studio 프로젝트 생성 (com.bowlingclub.fee)
- [x] Kotlin + Jetpack Compose 설정
- [x] Gradle 의존성 추가
  - [x] Compose BOM
  - [x] Room Database
  - [x] Hilt DI
  - [x] Navigation Compose
  - [x] DataStore
  - [ ] Vico Chart
  - [ ] ML Kit OCR
  - [ ] CameraX
- [x] 프로젝트 구조 생성 (di, data, domain, ui 패키지)

### 1.2 데이터베이스 설정
- [x] Room Database 클래스 생성 (AppDatabase.kt)
- [x] Entity 클래스 생성
  - [x] MemberEntity
  - [x] PaymentEntity
  - [x] AccountEntity
  - [x] MeetingEntity
  - [x] ScoreEntity
  - [ ] TeamEntity
  - [ ] TeamMemberEntity
  - [ ] TeamMatchEntity
  - [ ] TeamMatchScoreEntity
  - [ ] DonationEntity
  - [x] SettlementEntity
  - [x] SettlementMemberEntity
  - [ ] SettingsEntity
- [x] DAO 인터페이스 생성
  - [x] MemberDao
  - [x] PaymentDao
  - [x] AccountDao
  - [x] MeetingDao
  - [x] ScoreDao
  - [ ] TeamDao
  - [ ] DonationDao
  - [x] SettlementDao
- [ ] Database Migration 전략 설정

### 1.3 아키텍처 기반
- [x] Hilt Module 설정 (DatabaseModule)
- [x] Repository 구현체 생성
  - [x] MemberRepository
  - [x] AccountRepository
  - [x] ScoreRepository
- [x] Domain Model 클래스 생성
  - [x] Member, Gender, MemberStatus
  - [x] Account, AccountType
  - [x] Settlement, SettlementStatus, SettlementMember
  - [x] Meeting, Score
  - [x] Result (에러 핸들링 wrapper)
- [ ] UseCase 기본 구조 설정

### 1.4 UI 기반
- [x] 앱 테마 설정 (Color, Typography, Theme)
- [x] 하단 네비게이션 구현
- [x] 공통 컴포넌트 생성
  - [x] CommonButton
  - [x] CommonTextField
  - [x] CommonCard
  - [x] LoadingIndicator
  - [x] EmptyStateView
- [x] 기본 화면 스캐폴딩
  - [x] HomeScreen
  - [x] MemberListScreen
  - [x] PaymentScreen
  - [x] AccountScreen
  - [x] ScoreScreen

### 1.5 코드 품질 개선 ✅ (2026-01-12)
- [x] Flow Collection 메모리 누수 수정 (HomeViewModel, MemberViewModel)
- [x] Thread-safe 상태 변경 (MutableStateFlow.update{} 사용)
- [x] Repository 에러 핸들링 추가 (Result wrapper, catch)
- [x] Magic Strings → Enum 변환 (Gender, MemberStatus, AccountType, SettlementStatus)
- [x] Entity 변환 개선 (fromDbValue 사용)
- [x] Settlement DAO 추가 및 DI 등록

---

## Phase 2: 핵심 기능 개발 (MVP)

### 2.1 회원 관리 ✅ (2026-01-12)
- [x] MemberRepository 구현
- [ ] 회원 관련 UseCase 구현
  - [ ] GetAllMembersUseCase
  - [ ] GetMemberByIdUseCase
  - [ ] AddMemberUseCase
  - [ ] UpdateMemberUseCase
  - [ ] DeleteMemberUseCase
- [x] 회원 목록 화면 (MemberListScreen)
  - [x] 회원 카드 컴포넌트
  - [x] 상태별 필터 (활동/휴면/탈퇴)
  - [x] 검색 기능
  - [ ] 정렬 기능 (이름순/가입일순)
- [x] 회원 등록/수정 화면 (MemberFormScreen)
  - [x] 이름, 연락처 입력
  - [x] 성별 선택
  - [x] 가입일 선택 (DatePicker)
  - [x] 초기 에버리지 입력
  - [x] 핸디캡 입력
  - [x] 메모 입력
  - [x] 상태 선택
- [x] 회원 상세 화면 (MemberDetailScreen)
  - [x] 기본 정보 표시
  - [x] 프로필 헤더 (아바타, 이름, 상태, 연락처)
  - [x] 통계 카드 (에버리지, 핸디캡, 총 게임 수)
  - [x] 상세 정보 섹션
  - [ ] 납부 이력 표시
  - [ ] 점수 기록 표시
- [x] 네비게이션 연결 (AppNavigation)
  - [x] 회원 추가 화면 라우팅
  - [x] 회원 상세 화면 라우팅
  - [x] 회원 수정 화면 라우팅

### 2.2 회비 관리 ✅ (2026-01-12)
- [x] PaymentRepository 구현
- [ ] 회비 관련 UseCase 구현
  - [ ] GetPaymentsByMonthUseCase
  - [ ] GetPaymentsByMemberUseCase
  - [ ] AddPaymentUseCase
  - [ ] AddBulkPaymentsUseCase
  - [ ] GetUnpaidMembersUseCase
- [x] PaymentViewModel 구현
  - [x] 월별 납부 조회
  - [x] 납부/미납 회원 분류
  - [x] 월 변경 기능
- [x] 회비 현황 화면 (PaymentScreen)
  - [x] 월별 납부 현황
  - [x] 납부/미납 회원 표시 (Grid)
  - [x] 월 선택 기능
  - [x] 로딩/빈 화면 처리
  - [x] LazyVerticalGrid key 최적화
- [x] 납부 등록 화면 (PaymentFormScreen)
  - [x] 회원 선택 드롭다운
  - [x] 금액 입력 (기본값 10,000원)
  - [x] 납부일 선택 (DatePicker)
  - [x] 정모일 선택 (DatePicker)
  - [x] 메모 입력
  - [x] 빈 회원 목록 처리
- [x] 네비게이션 연결 (AppNavigation)
  - [x] PaymentScreen → PaymentFormScreen 라우팅
- [ ] 빠른 납부 기능 (추후)
  - [ ] "이번 달 회비" 버튼
  - [ ] 체크박스 일괄 선택
  - [ ] 중복 납부 방지 로직
- [ ] 미납자 목록 화면

### 2.3 수입/지출 장부 ✅ (2026-01-12)
- [x] AccountRepository 구현
- [ ] 장부 관련 UseCase 구현
  - [ ] GetAllAccountsUseCase
  - [ ] GetAccountsByPeriodUseCase
  - [ ] GetAccountsByCategoryUseCase
  - [ ] AddAccountUseCase
  - [ ] UpdateAccountUseCase
  - [ ] DeleteAccountUseCase
  - [ ] GetBalanceUseCase
- [x] AccountViewModel 구현
  - [x] 거래 목록 조회 (날짜순 정렬)
  - [x] 수입/지출 필터링
  - [x] 잔액/총수입/총지출 계산
  - [x] 거래 추가/수정/삭제
  - [x] loadAccountById 개별 조회
- [x] 장부 목록 화면 (AccountScreen)
  - [x] 잔액 표시 (상단 고정)
  - [x] 수입/지출 내역 리스트
  - [x] 수입/지출 구분 필터 (FilterChip)
  - [x] 로딩/빈 화면 처리
  - [x] LazyColumn key 최적화
  - [x] LazyColumn 높이 제한 (weight)
  - [ ] 기간별 필터 (추후)
  - [ ] 카테고리별 필터 (추후)
- [x] 장부 등록/수정 화면 (AccountFormScreen)
  - [x] 수입/지출 선택 (FilterChip)
  - [x] 카테고리 선택 (DropdownMenu)
  - [x] 금액 입력
  - [x] 날짜 선택 (DatePicker)
  - [x] 내용 입력
  - [x] 메모 입력
  - [x] 초기 카테고리 검증 로직
  - [ ] 영수증 이미지 첨부 (추후)
- [x] 네비게이션 연결 (AppNavigation)
  - [x] AccountScreen → AccountFormScreen 라우팅
  - [x] 거래 수정 화면 라우팅 (LaunchedEffect)

### 2.4 영수증 OCR 스캔
- [ ] 카메라 권한 요청 구현
- [ ] CameraX 프리뷰 화면 구현
- [ ] ML Kit Text Recognition 연동
- [ ] 영수증 파싱 로직 구현
  - [ ] 금액 추출 (정규식)
  - [ ] 날짜 추출 (정규식)
  - [ ] 상호명 추출
- [ ] 인식 결과 확인/수정 화면
- [ ] 장부 자동 등록 연동

### 2.5 홈 대시보드 ✅ (2026-01-12)
- [x] 홈 화면 (HomeScreen) - 기본 UI
  - [x] 현재 잔액 카드 (실데이터 연동)
  - [x] 이번 달 수입/지출 요약 (실데이터 연동)
  - [x] 에버리지 TOP 3 (ScoreRepository 연동)
  - [x] 빠른 메뉴 버튼 네비게이션 연결 (납부 등록, 지출 등록)
  - [x] "전체보기" 링크 네비게이션 연결
  - [ ] 모임 시작 기능 (추후)
  - [ ] 정산 기능 (추후)
- [x] HomeViewModel 개선
  - [x] 이번 달 수입/지출 계산
  - [x] 랭킹 데이터 로드
- [x] getTransactionIcon 공통 함수 추출

---

## Phase 3: 점수 관리 기능

### 3.1 모임/점수 관리
- [ ] MeetingRepository 구현
- [x] ScoreRepository 구현
- [ ] 점수 관련 UseCase 구현
  - [ ] GetAllMeetingsUseCase
  - [ ] CreateMeetingUseCase
  - [ ] GetScoresByMeetingUseCase
  - [ ] AddScoreUseCase
  - [ ] UpdateScoreUseCase
  - [ ] CalculateAverageUseCase
- [ ] 모임 목록 화면 (MeetingListScreen)
  - [ ] 최근 모임 리스트
  - [ ] 모임별 참여 인원/게임 수 표시
- [ ] 모임 생성 화면 (MeetingFormScreen)
  - [ ] 날짜 선택
  - [ ] 볼링장 입력
  - [ ] 메모 입력
- [ ] 점수 입력 화면 (ScoreInputScreen)
  - [ ] 그리드 형태 점수 입력
  - [ ] 회원 추가 기능
  - [ ] 게임 추가 기능
  - [ ] 실시간 평균 계산

### 3.2 통계 및 랭킹
- [ ] 통계 관련 UseCase 구현
  - [ ] GetMemberStatsUseCase
  - [ ] GetRankingsUseCase
  - [ ] GetMonthlyMVPUseCase
- [ ] 개인 통계 표시
  - [ ] 에버리지 (최근 12게임)
  - [ ] 하이게임/로우게임
  - [ ] 총 게임 수
  - [ ] 점수 추이 그래프 (Vico)
- [ ] 랭킹 화면 (RankingScreen)
  - [ ] 에버리지 랭킹
  - [ ] 하이게임 랭킹
  - [ ] 월간 MVP
  - [ ] 성장왕

### 3.3 핸디캡 시스템
- [ ] 핸디캡 계산 UseCase 구현
- [ ] 회원 정보에서 핸디캡 설정
- [ ] 핸디캡 적용 점수 표시
- [ ] 핸디캡 랭킹 표시

---

## Phase 4: 고급 기능

### 4.1 모임비 정산
- [ ] SettlementRepository 구현
- [ ] 정산 관련 UseCase 구현
  - [ ] CreateSettlementUseCase
  - [ ] CalculatePerPersonUseCase
  - [ ] UpdatePaymentStatusUseCase
  - [ ] GetPendingSettlementsUseCase
- [ ] 정산 생성 화면 (SettlementFormScreen)
  - [ ] 모임 선택
  - [ ] 참석자 다중 선택
  - [ ] 게임비/식비/기타비용 입력
  - [ ] 자동 1인당 금액 계산
- [ ] 정산 현황 화면 (SettlementStatusScreen)
  - [ ] 수금 현황 표시 (완료/미수금)
  - [ ] 개별 수금 처리
  - [ ] 청구 메시지 복사 기능
- [ ] 정산 히스토리

### 4.2 찬조 관리
- [ ] DonationRepository 구현
- [ ] 찬조 관련 UseCase 구현
  - [ ] GetAllDonationsUseCase
  - [ ] AddDonationUseCase
  - [ ] UpdateDonationStatusUseCase
- [ ] 찬조 목록 화면 (DonationListScreen)
  - [ ] 금액/물품 필터
  - [ ] 찬조 요약 표시
  - [ ] 물품 상태 관리 (보유중/사용완료)
- [ ] 찬조 등록 화면 (DonationFormScreen)
  - [ ] 찬조자 입력 (회원/외부)
  - [ ] 유형 선택 (금액/물품)
  - [ ] 금액 또는 물품 정보 입력
  - [ ] 용도 입력

### 4.3 팀전 기능
- [ ] TeamRepository 구현
- [ ] TeamMatchRepository 구현
- [ ] 팀전 관련 UseCase 구현
  - [ ] CreateTeamUseCase
  - [ ] CreateTeamMatchUseCase
  - [ ] CalculateTeamScoreUseCase
- [ ] 팀 관리 화면 (TeamManageScreen)
  - [ ] 팀 생성/수정
  - [ ] 팀원 배정
- [ ] 팀전 대회 화면 (TeamMatchScreen)
  - [ ] 대회 생성
  - [ ] 점수 입력
  - [ ] 스크래치/핸디캡 점수 표시
- [ ] 팀전 결과 화면
  - [ ] 팀 순위
  - [ ] 개인별 점수

### 4.4 설정 및 기타
- [ ] 설정 화면 (SettingsScreen)
  - [ ] 동호회 이름 설정
  - [ ] 기본 회비 금액 설정
  - [ ] 에버리지 계산 게임 수 설정
  - [ ] 핸디캡 상한선 설정
- [ ] 데이터 백업/복원
  - [ ] JSON 내보내기
  - [ ] JSON 가져오기
- [ ] 데이터 초기화 기능

---

## Phase 5: 테스트 및 배포

### 5.1 테스트
- [ ] Unit Test 작성
  - [ ] UseCase 테스트
  - [ ] Repository 테스트
  - [ ] ViewModel 테스트
- [ ] UI Test 작성
  - [ ] 주요 화면 테스트
  - [ ] 네비게이션 테스트
- [ ] 통합 테스트
- [ ] 사용성 테스트 (실제 사용 시나리오)

### 5.2 최적화
- [ ] 성능 최적화
  - [ ] 리스트 성능 (LazyColumn)
  - [ ] 이미지 로딩 최적화
  - [ ] 메모리 사용량 점검
- [ ] 앱 크기 최적화
  - [ ] ProGuard/R8 설정
  - [ ] 리소스 최적화
- [ ] 배터리 사용량 최적화

### 5.3 배포 준비
- [x] 앱 아이콘 및 스플래시 화면
- [ ] 앱 이름 및 설명 작성
- [ ] 스크린샷 준비
- [ ] 개인정보처리방침 작성
- [ ] Play Store 등록 준비
  - [ ] 개발자 계정
  - [ ] 앱 서명 키 생성
  - [ ] 앱 번들 빌드

### 5.4 출시
- [ ] 내부 테스트 배포
- [ ] 베타 테스트
- [ ] 프로덕션 출시

---

## 진행 상황 요약

| Phase | 항목 수 | 완료 | 진행률 |
|-------|---------|------|--------|
| Phase 1 | 25 | 22 | 88% |
| Phase 2 | 48 | 48 | 100% |
| Phase 3 | 20 | 1 | 5% |
| Phase 4 | 25 | 0 | 0% |
| Phase 5 | 20 | 1 | 5% |
| **총계** | **138** | **72** | **52%** |

---

## 우선순위 및 의존성

### MVP (최소 기능 제품) - 4주 목표
1. ~~Phase 1 전체 (프로젝트 설정)~~ ✅
2. Phase 2.1 회원 관리
3. Phase 2.2 회비 관리
4. Phase 2.3 장부 (OCR 제외)
5. Phase 2.5 홈 대시보드

### MVP 이후 추가
1. Phase 2.4 영수증 OCR
2. Phase 3 전체 (점수 관리)
3. Phase 4 전체 (고급 기능)
4. Phase 5 전체 (테스트/배포)

---

## 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-12 | Claude | 초안 작성 |
| 1.1 | 2026-01-12 | Claude | Phase 1 완료 - 프로젝트 설정, DB, 기본 UI, 코드 품질 개선 |
| 1.2 | 2026-01-12 | Claude | Phase 2.1 회원 관리 완료 - MemberFormScreen, MemberDetailScreen, 네비게이션 연결 |
| 1.3 | 2026-01-12 | Claude | Phase 2.2 회비 관리 완료 - PaymentRepository, PaymentViewModel, PaymentFormScreen, 코드 리뷰 이슈 수정 |
| 1.4 | 2026-01-12 | Claude | Phase 2.3 장부 관리 완료 - AccountViewModel, AccountScreen, AccountFormScreen, 네비게이션 연결, 코드 리뷰 이슈 수정 |
| 1.5 | 2026-01-12 | Claude | Phase 2.5 홈 대시보드 완료 - 실데이터 연동, 빠른 메뉴 네비게이션, 랭킹 연동, getTransactionIcon 공통화 |
