# 나루(NARU) 코드 기반 구현 현황 및 기능 분석서

> 분석 기준: Backend `backend` 저장소와 Frontend `frontend` 저장소의 현재 소스코드, 설정, SQL, Mapper XML, 테스트 코드 및 실제 API 호출 관계  
> 분석일: 2026-08-22  
> 판정 원칙: 화면·DTO·API의 존재만으로 완료 처리하지 않고, Frontend → API → Service → DB/Redis/외부 API 연결을 확인했다.

## 판정 용어

| 판정 | 의미 |
| --- | --- |
| 구현 완료 | 현재 코드에서 핵심 사용자 흐름, Backend 처리, 데이터 계층이 연결됨 |
| 부분 구현 | 핵심 흐름 일부가 없거나 특정 실행 경로만 연결됨 |
| Frontend만 구현 | 화면/상호작용은 있으나 대응 Backend 처리가 없음 |
| Backend만 구현 | API/비즈니스 로직은 있으나 현재 Frontend 호출이 확인되지 않음 |
| 코드는 존재하지만 실제 흐름에서 사용되지 않음 | 레거시·미사용 export·숨김 컴포넌트 등 |
| 미구현 | 사용자에게 보이는 의도와 대응하는 처리 코드가 없음 |
| 판단 불가 | 현재 코드와 실행 환경만으로 완료 여부를 확정할 수 없음 |

---

# 1. 프로젝트 전체 구조 요약

## 1.1 기술 스택

| 영역 | 코드에서 확인된 기술 | 근거 |
| --- | --- | --- |
| Backend | Java 17, Spring Framework/MVC 5.3.37, Servlet, WAR | `build.gradle`, `src/main/java/org/kkobi/config/ServletConfig.java`, `WebConfig.java` |
| 보안 | Spring Security 5.8.13, JWT(jjwt 0.11.5), BCrypt | `build.gradle`, `security/config/SecurityConfig.java`, `security/jwt/JwtProvider.java` |
| 데이터 접근 | MyBatis 3.5.13, Mapper interface/XML | `build.gradle`, `src/main/resources/mappers/*.xml`, `mybatis-config.xml` |
| Database | MySQL 8, HikariCP | `build.gradle`, `config/RootConfig.java`, `application.properties` |
| Redis | Spring Data Redis, Lettuce | `build.gradle`, `config/RedisConfig.java`, `security/token/RedisRefreshTokenStore.java` |
| 실시간 통신 | Spring WebSocket/STOMP, STOMP.js | `external/kis/websocket/WebSocketBrokerConfig.java`, `external/kis/websocket/*`, Frontend `package.json` |
| 외부 API | 금융감독원 금융상품 한눈에 Open API, 한국투자증권(KIS) REST/WebSocket API | `product/deposit/service/DepositProductService.java`, `product/saving/service/SavingProductService.java`, `external/kis/*` |
| Frontend | Vue 3 Composition API, Vite 8, Vue Router 5, Pinia 4, Tailwind CSS 3 | Frontend `package.json`, `src/main.js`, `src/router/index.js`, `src/stores/*` |
| 차트 | lightweight-charts 5.2 | Frontend `package.json`, `src/components/security/*Chart*.vue` |
| 문서/API | springdoc-openapi/Swagger UI | `build.gradle`, `config/OpenApiConfig.java` |
| 테스트 | JUnit 5, Mockito, Spring Test; Vitest | Backend `src/test/java`, Frontend `src/**/*.test.js` |
| 빌드/배포 | Gradle Wrapper, npm, Docker, Docker Compose, Tomcat WAR | `gradlew*`, Frontend `package.json`, `Dockerfile`, `compose.yaml`, `compose.ec2.yaml` |

Spring Boot 실행 애플리케이션은 확인되지 않았다. Spring 설정 클래스로 구성하고 WAR를 배포하는 전통적인 Spring MVC 구조다. `spring-boot` 계열 일부 라이브러리는 API 문서화 용도로 의존하지만, 이를 근거로 Spring Boot 프로젝트라고 분류하지 않았다.

## 1.2 프로젝트 디렉터리 구조

### Backend

```text
src/main/java/org/kkobi
├─ account            가상계좌와 통합 자산
├─ assessment         게임/가상투자 성향 계산·결과·스케줄러
├─ config             Spring MVC, DB, Redis, WebSocket 설정
├─ external/kis       KIS 인증·시세·차트·실시간 틱
├─ game               게임 시작·행동 로그·완료
├─ goal               금융 목표와 상품 추천 계획
├─ leaderboard        Redis 기반 성향/친구 리더보드
├─ notification       알림 설정·저장·이벤트 리스너
├─ persona            8개 성향 유형 조회
├─ product            예금·적금 수집/조회/가입/해지
├─ quiz               JSON 기반 일일 금융 퀴즈
├─ securities         종목 검색·추천·시세
├─ security           Security/JWT/Refresh Token
├─ trade              주문·체결·보유종목·포트폴리오
└─ users              회원·프로필·친구

src/main/resources
├─ db/migration       V1~V24 및 반복 성향 seed SQL
├─ mappers            MyBatis SQL
├─ game               SC001 시나리오 JSON
├─ DailyQuiz.json     날짜별 퀴즈 데이터
└─ application.properties
```

`traqcking` 패키지는 비어 있으며 실제 기능으로 보지 않았다.

### Frontend

```text
src
├─ api          공통 HTTP/JWT 재발급과 도메인 API 함수
├─ components   공통, 홈, 게임, 상품, 증권, 거래, 알림 UI
├─ composables  시세 피드·주문·가상자산 등 상태 로직
├─ constants    상품/거래/성향 표시 상수
├─ router       Route와 인증·성향 완료 guard
├─ stores       auth, assessment, notification 등 Pinia store
├─ utils        포맷, 거래, 게임 세션, 시장시간 유틸리티
└─ views        31개 주요 화면
```

## 1.3 전체 시스템 구조

```text
사용자
  → Vue View/Component
  → src/api/http.js (Bearer Access Token, 401 시 단일 Refresh 재시도)
  → Spring Security Filter Chain
  → Controller → Service → MyBatis Mapper → MySQL
                         ├→ Redis (Refresh Token, KIS Token/시세, Leaderboard)
                         ├→ 금융감독원 API (예·적금 원천 데이터)
                         └→ KIS REST/WebSocket (현재가·차트·실시간 틱)

KIS WebSocket → Backend Tick/Subscription Manager → STOMP topic → Vue 실시간 시세 화면
```

---

# 2. 전체 사용자 흐름

## 2.1 신규 사용자 핵심 흐름

| 단계 | Route / 화면 | 주요 행동 | 다음 이동 조건 |
| --- | --- | --- | --- |
| 회원가입 | `/signup` · `SignUpView.vue` | 이메일·비밀번호·닉네임·생년월일 입력 | 가입 성공 후 로그인 |
| 로그인 | `/login` · `LoginView.vue` | 자격 증명 제출, Access Token 저장 | 인증 성공 시 `/` |
| 홈 | `/` · `HomeView.vue` | 성향 결과 존재 여부 확인 | 미완료 사용자는 게임 소개 진입 가능 |
| 성향 게임 안내 | `/game/introduction` · `GameIntroductionView.vue` | 게임 취지 확인 | 튜토리얼/시작으로 이동 |
| 튜토리얼 | `/game/tutorial` · `GameTutorialView.vue` | 로컬 UI로 거래 방법 학습 | 실제 게임 시작으로 이동 |
| 게임 시작/배분 | `/game/start`, `/game/allocation` | 게임 세션 생성, 초기 현금·주식·예금 배분 | 세션이 있어야 `/game` 진입 |
| 게임 행동 | `/game` · `GameView.vue` | 시나리오 틱별 매수·매도·예금 행동 저장 | 마지막 틱 이후 완료 요청 |
| 성향 결과 | `/assessment/result` · `AssessmentResultView.vue` | RT/LH/RP, 8유형, 포트폴리오 설명 확인 | 목표 설정 또는 상품 탐색 |
| 목표 설정 | `/financial-goal` · `FinancialGoalView.vue` | 목표 종류·금액·현재금액·기간 저장 | 상품 화면으로 이동 |
| 상품 탐색 | `/products` · `ProductsView.vue` | 종목/예금/적금 검색·필터·정렬·추천 확인 | 상세/가입/종목상세 |
| 가상투자 시작 | `/virtual/start` · `VirtualInvestStartView.vue` | 500만원 시드 계좌 생성 | `/virtual/assets` |
| 투자/관리 | `/virtual/*` | 주문, 예·적금 가입/해지, 자산/내역 확인 | 지속 이용 |

## 2.2 기존 사용자 흐름과 Guard

- `src/router/index.js`의 전역 guard는 `requiresAuth`, `guestOnly`, `requiresAssessment`를 처리한다.
- `requiresAssessment` Route는 `assessmentStore.loadResult()`가 실제 결과 API를 호출하고, 결과가 없으면 홈으로 돌린다.
- `/game`은 `readGameStartSession()` 로컬 세션이 없으면 게임 소개로 돌린다.
- Access Token은 Frontend 저장소에, Refresh Token은 HttpOnly Cookie에 있으며 앱 초기화 시 Refresh API로 세션 복원을 시도한다.
- 상품 목록/상세는 로그인 후 성향 검사 전에도 접근 가능하다. 가상투자·리더보드는 성향 완료가 추가 조건이다.

---

# 3. 전체 기능 목록 및 기능 명세

## 3.1 기능 분류

- 회원·인증: 회원가입, 로그인, 인증 사용자 조회, 재발급, 로그아웃, 프로필 수정
- 성향 분석: 게임 상태/시작/행동/완료, 결과 조회, 8개 유형, 가상투자 후 재평가
- 목표: 단일 금융 목표 조회/저장/삭제, 목표 기준 상품 조회 계획
- 상품: 금융감독원 수집, 검색/필터/정렬, 상세, 가입 예상, 가입, 해지 예상/실행, 보유/내역
- 증권·거래: 종목 검색/추천/상세, REST/실시간 시세, 차트, 시장가/지정가 주문, 취소, 보유/포트폴리오
- 참여·소셜: 일일 퀴즈, 성향/친구 리더보드, 친구 요청/관리, 알림 설정/조회

아래 표의 한 “세부 기능”은 독립된 사용자 목적 단위이며, 단순 조회/수정 API는 같은 목적 안에 묶었다.

## 3.2 회원가입

| 항목 | 내용 |
| --- | --- |
| 대분류 | 회원관리 |
| 기능명 | 회원가입 |
| 기능 설명 | 신규 사용자 계정 생성 |
| 사용자 입력 | 이메일, 비밀번호, 닉네임, 생년월일 |
| 주요 처리 내용 | Bean Validation → 이메일/닉네임 중복 검사 → 비밀번호 BCrypt 해시 → 사용자 INSERT |
| 처리 결과 | 사용자 ID와 기본 정보 반환 |
| 관련 화면 | `/signup`, `SignUpView.vue` |
| 관련 API | `POST /api/auth/signup` |
| Backend 주요 코드 | `UserController.signup()`, `UserServiceImpl.signup()` |
| Frontend 주요 코드 | `src/views/SignUpView.vue`, `src/api/authApi.js` |
| 관련 DB / Redis | `users`; Redis 미사용 |
| 유효성 검증 | 이메일 형식, 과거 생년월일, 비밀번호 8자 이상·영문/숫자/특수문자·공백 금지, 이메일 포함 금지 |
| 예외처리 | 중복 이메일/닉네임 409, 검증 오류 400 |
| 구현 상태 | 구현 완료 |
| 비고 | Frontend와 Backend 양쪽 검증 |

## 3.3 로그인·토큰 수명주기

| 항목 | 내용 |
| --- | --- |
| 대분류 | 인증 |
| 기능명 | 로그인·재발급·로그아웃 |
| 기능 설명 | Stateless JWT 인증과 Refresh Token 회전 |
| 사용자 입력 | 로그인 이메일/비밀번호; 이후 Refresh Cookie |
| 주요 처리 내용 | 인증 → Access/Refresh 생성 → Refresh 해시 Redis 저장 → 401 시 원자적 소비·회전 → 로그아웃 시 소비 |
| 처리 결과 | Access Token JSON, Refresh Token HttpOnly Cookie |
| 관련 화면 | `LoginView.vue`, 앱 초기화·모든 인증 화면 |
| 관련 API | `POST /api/auth/login`, `/refresh`, `/logout`, `GET /me` |
| Backend 주요 코드 | `JwtUsernamePasswordAuthenticationFilter`, `JwtProvider`, `RefreshTokenService`, `JwtAuthenticationFilter` |
| Frontend 주요 코드 | `src/api/http.js`, `src/stores/auth.js` |
| 관련 DB / Redis | `users`; `auth:refresh:{jti}` Redis key |
| 유효성 검증 | 서명·issuer·token_type·만료·jti, Redis hash 일치 |
| 예외처리 | 자격 증명 401, 잘못된 요청 400, Refresh 오류 401 |
| 구현 상태 | 구현 완료 |
| 비고 | Access 30분, Refresh 14일이 기본 설정값 |

## 3.4 프로필·비밀번호 재설정

| 항목 | 내용 |
| --- | --- |
| 대분류 | 회원관리 |
| 기능명 | 프로필 조회/수정, 비밀번호 찾기 |
| 기능 설명 | 닉네임·생년월일 수정; 재설정 화면 제공 |
| 사용자 입력 | 닉네임, 생년월일 / 재설정 이메일 |
| 주요 처리 내용 | 프로필은 DB 조회/UPDATE; 재설정은 `setTimeout`과 `console.log`만 수행 |
| 처리 결과 | 수정 프로필 / 재설정 성공처럼 보이는 로컬 UI |
| 관련 화면 | `/my/profile`, `/password-reset` |
| 관련 API | `GET/PATCH /api/my/profile`; 비밀번호 재설정 API 없음 |
| Backend 주요 코드 | `ProfileController`, `UserServiceImpl` |
| Frontend 주요 코드 | `MyProfileView.vue`, `PasswordResetView.vue` |
| 관련 DB / Redis | `users` |
| 유효성 검증 | 닉네임 중복, 과거 생년월일 |
| 예외처리 | 프로필 오류 메시지; 재설정 서버 오류 처리 없음 |
| 구현 상태 | 프로필 구현 완료 / 비밀번호 재설정 Frontend만 구현 |
| 비고 | 발표에서 비밀번호 재설정을 완료 기능으로 표현하면 안 됨 |

## 3.5 게임형 성향 진단

| 항목 | 내용 |
| --- | --- |
| 대분류 | 성향 분석 |
| 기능명 | 시나리오 게임·행동 기록·완료 |
| 기능 설명 | 초기 자산 배분과 시장 상황별 행동으로 세 축 점수를 계산 |
| 사용자 입력 | 초기 현금/주식/예금 비중, 틱별 매수·매도·예금 행동 |
| 주요 처리 내용 | SC001 로드, 서버 게임 상태 관리, `action_logs` 적재, 완료 시 규칙 계산·8유형 분류 |
| 처리 결과 | RT/LH/RP 및 persona 결과 |
| 관련 화면 | `/game/introduction`, `/tutorial`, `/start`, `/allocation`, `/game`, `/assessment/result` |
| 관련 API | `/api/games/status`, `/start`, `/actions`, `/completion`, `/scenarios/{id}` |
| Backend 주요 코드 | `GameStartService`, `GameActionService`, `GameAssessmentService`, `GameBehaviorAssessmentCalculator` |
| Frontend 주요 코드 | `GameAllocationView.vue`, `GameView.vue`, `gameApi.js`, `gameStorage.js` |
| 관련 DB / Redis | `action_logs`, `results`, `personas`; 시나리오 JSON |
| 유효성 검증 | 틱 순서, 배분 합계/금액, 완료 중복, 시나리오 상태 |
| 예외처리 | 잘못된 게임 상태·행동 400 계열 |
| 구현 상태 | 구현 완료 |
| 비고 | 튜토리얼 자체는 로컬 시뮬레이션이며 실제 성향 점수에 반영되지 않음 |

## 3.6 가상투자 기반 성향 갱신

| 항목 | 내용 |
| --- | --- |
| 대분류 | 성향 분석 |
| 기능명 | 실제 가상투자 행동 재평가 |
| 기능 설명 | 주문·상품 가입/해지·기간 스냅샷을 이용해 기존 점수를 점진 갱신 |
| 사용자 입력 | 체결된 주문, 예·적금 거래, 보유기간/현금 비중 |
| 주요 처리 내용 | 행동 컨텍스트 생성 → 규칙 delta → 행동점수 → 90:10 EMA → persona 재분류 |
| 처리 결과 | 최신 `results` 점수/유형 변경 |
| 관련 화면 | 직접 입력 화면 없음; 거래·상품 기능의 후속 처리 |
| 관련 API | 별도 공개 API 없음 |
| Backend 주요 코드 | `VirtualInvestmentAssessmentService`, `BehaviorRuleEngine`, `SecurityOrderAssessmentListener`, assessment schedulers |
| Frontend 주요 코드 | 거래·상품 화면이 원인 행동을 생성 |
| 관련 DB / Redis | `results`, `security_orders`, `holding_*`, `account_daily_snapshots`, `assessment_settlements` |
| 유효성 검증 | 행동량/비율/보유기간/시장상태 조건 |
| 예외처리 | 이벤트·스케줄러별 로깅/트랜잭션 |
| 구현 상태 | 부분 구현 |
| 비고 | 틱 매칭으로 지연 체결된 지정가 주문은 성향 이벤트 연결 TODO |

## 3.7 금융 목표

| 항목 | 내용 |
| --- | --- |
| 대분류 | 목표관리 |
| 기능명 | 목표 조회·저장·삭제·월 기준액 계산 |
| 기능 설명 | 사용자당 하나의 목표와 필요 월 저축액을 관리 |
| 사용자 입력 | 목표 유형, 목표 금액, 현재 금액, 목표 개월 |
| 주요 처리 내용 | upsert, `ceil((target-current)/months)`, 목표 개월 이하 최대 표준 가입기간 산출 |
| 처리 결과 | 목표와 월 참고금액, 추천기간/설명 |
| 관련 화면 | `/financial-goal` |
| 관련 API | `GET/PUT/DELETE /api/financial-goals` |
| Backend 주요 코드 | `FinancialGoalService`, `FinancialGoalPlanner` |
| Frontend 주요 코드 | `FinancialGoalView.vue`, `financialGoalApi.js` |
| 관련 DB / Redis | `financial_goals` |
| 유효성 검증 | 양수 금액/개월, 현재금액 ≤ 목표금액 |
| 예외처리 | 잘못된 값 400, 목표 없음은 빈 응답 처리 |
| 구현 상태 | 구현 완료 |
| 비고 | 목표 날짜와 저장된 월 저축금액은 없음 |

## 3.8 예·적금 데이터 수집·구조화

| 항목 | 내용 |
| --- | --- |
| 대분류 | 금융상품 |
| 기능명 | 금융감독원 예·적금 수집 |
| 기능 설명 | Open API 페이지를 순회하고 상품/옵션/우대조건을 내부 모델로 저장 |
| 사용자 입력 | 운영자성 수집 API 호출 |
| 주요 처리 내용 | API 오류 검증, 상품/옵션 upsert, 우대조건 분류 및 금리조건 구조화 |
| 처리 결과 | 검색 가능한 상품 데이터 |
| 관련 화면 | 직접 호출 화면 확인 안 됨 |
| 관련 API | `GET /deposits/external`, `POST /deposits/collect`, `POST /savings/collect` |
| Backend 주요 코드 | `DepositProductService`, `SavingProductService`, `ProductPreferentialConditionService`, `PreferentialConditionParser`, `PreferentialRateConditionParser` |
| Frontend 주요 코드 | 없음 |
| 관련 DB / Redis | `products`, `product_options`, `product_preferential_conditions`, `product_preferential_rate_conditions` |
| 유효성 검증 | `err_cd=000`, null 목록, 옵션/상품 키 |
| 예외처리 | 외부 응답 오류를 예외로 변환 |
| 구현 상태 | Backend만 구현 |
| 비고 | 자동 정기 수집 스케줄은 확인되지 않음 |

## 3.9 상품 검색·상세

| 항목 | 내용 |
| --- | --- |
| 대분류 | 금융상품 |
| 기능명 | 예금·적금 검색/필터/정렬/상세 |
| 기능 설명 | 키워드·기간·우대조건·적립방식으로 상품 탐색 |
| 사용자 입력 | 탭, 검색어, 복수 기간, 우대조건, 적립방식, 정렬, 페이지 |
| 주요 처리 내용 | URL query와 필터 상태 동기화, MyBatis 동적 SQL, 옵션/우대조건 조립 |
| 처리 결과 | 페이지 목록과 상품 상세 |
| 관련 화면 | `/products`, `/products/:productType/:productId` |
| 관련 API | `GET /api/products/deposits|savings`, `/{id}` |
| Backend 주요 코드 | `DepositProductService`, `SavingProductService`, `ProductMapper.xml` |
| Frontend 주요 코드 | `ProductListPanel.vue`, `ProductDetailView.vue`, `productApi.js` |
| 관련 DB / Redis | `products`, `product_options`, preferential tables |
| 유효성 검증 | enum/기간/페이지/정렬 값 정규화 |
| 예외처리 | 없는 상품 404/IllegalArgument, Frontend retry/error/empty UI |
| 구현 상태 | 구현 완료 |
| 비고 | 금리 범위·금융사 전용 필터는 없음 |

## 3.10 추천

| 항목 | 내용 |
| --- | --- |
| 대분류 | 추천 |
| 기능명 | 종목 성향 추천·홈 상품 노출·목표 상품 조회 |
| 기능 설명 | 증권은 3축 거리 기반, 예·적금은 금리 순, 목표는 기간 계획을 함께 제시 |
| 사용자 입력 | 최신 성향, 목표, 상품 유형/페이지 |
| 주요 처리 내용 | 종목 Manhattan 거리 계산; 홈 예·적금 일반 목록 첫 항목; 목표 추천 요청 생성 |
| 처리 결과 | 종목 최대 2개 또는 상품 목록/목표 plan |
| 관련 화면 | 홈, `/products` |
| 관련 API | `/api/securities/recommendations`, `/api/financial-goals/recommendations`, `/api/products/recommendations` |
| Backend 주요 코드 | `SecurityService`, `SecurityMapper.xml`, `FinancialGoalService`, `ProductRecommendationService` |
| Frontend 주요 코드 | `HomeRecommendationCard.vue`, `ProductListPanel.vue` |
| 관련 DB / Redis | `results`, `securities`, `financial_goals`, product tables |
| 유효성 검증 | 성향 미완료 시 인기/거래량 fallback |
| 예외처리 | 빈 목록, 시세 일부 실패 개별 표시 |
| 구현 상태 | 증권 구현 완료 / 목표 부분 구현 / 예·적금 개인화 미구현 |
| 비고 | 상세 평가는 11~12절 참조 |

## 3.11 가상계좌·통합 자산

| 항목 | 내용 |
| --- | --- |
| 대분류 | 가상투자 |
| 기능명 | 계좌 생성·통합 자산 조회 |
| 기능 설명 | 성향 완료 후 500만원 시드 계좌를 한 번 만들고 현금/주식/예·적금 자산을 집계 |
| 사용자 입력 | 계좌 시작 버튼 |
| 주요 처리 내용 | 성향 완료/중복 확인, account INSERT, 각 자산 조회/합산 |
| 처리 결과 | 계좌와 자산 구성/수익률 |
| 관련 화면 | `/virtual/start`, `/virtual/assets` |
| 관련 API | `POST/GET /api/accounts`, `/api/accounts/me/holdings` |
| Backend 주요 코드 | `AccountService`, `PortfolioService`, `ProductHoldingService` |
| Frontend 주요 코드 | `VirtualInvestStartView.vue`, `VirtualAssetsView.vue`, `useVirtualAssets.js` |
| 관련 DB / Redis | `accounts`, `holding_securities`, `holding_products` |
| 유효성 검증 | 진단 완료, 계좌 중복 금지 |
| 예외처리 | 계좌 없음/중복/성향 미완료 오류 |
| 구현 상태 | 구현 완료 |
| 비고 | 초기 투자금은 코드상 5,000,000원 |

## 3.12 예·적금 가입·해지

| 항목 | 내용 |
| --- | --- |
| 대분류 | 가상투자 |
| 기능명 | 가입 예상/가입/보유/중도해지 |
| 기능 설명 | 옵션과 우대조건을 선택해 가상 예·적금을 가입하고 예상/현재 이자·세금·환급액을 계산 |
| 사용자 입력 | 옵션, 금액, 납입일, 선택 우대조건 |
| 주요 처리 내용 | 잔액·한도 검증, 적용금리 합산, 현금 차감, holding/transaction 저장, 해지 시 현금 환급·상태 변경 |
| 처리 결과 | 가입 결과, 보유 상세, 세후 만기/해지 예상, 거래내역 |
| 관련 화면 | 상품 가입, 가상상품, 보유목록/상세/해지 |
| 관련 API | `/api/products/holdings/**` |
| Backend 주요 코드 | `ProductHoldingService`의 `subscribeProduct()`, `estimateSubscription()`, `terminateProduct()` |
| Frontend 주요 코드 | `ProductSubscriptionView.vue`, `ProductHoldingDetailView.vue`, `ProductTerminationView.vue` |
| 관련 DB / Redis | `holding_products`, `product_transactions`, `account_transactions`, `accounts` |
| 유효성 검증 | 계좌/잔액/상품옵션/금액/납입일/우대조건 |
| 예외처리 | 존재하지 않음, 잔액 부족, 이미 해지됨 등 구조화 오류 |
| 구현 상태 | 부분 구현 |
| 비고 | 적금 가입 시 1회차만 기록하며 후속 월 납입 API/스케줄러는 확인되지 않음 |

## 3.13 증권 시세·차트

| 항목 | 내용 |
| --- | --- |
| 대분류 | 증권 |
| 기능명 | 종목 목록/상세/현재가/차트/실시간 구독 |
| 기능 설명 | DB 종목 메타데이터와 KIS 시세를 조합해 표시 |
| 사용자 입력 | 종목, 검색/정렬, 차트 기간 |
| 주요 처리 내용 | OAuth 토큰 캐시, batch quote, 일/주/월봉 조회, STOMP 실시간 틱 전달 |
| 처리 결과 | 종목 정보, 가격/등락률, 캔들 차트 |
| 관련 화면 | 상품 종목 탭, 종목 상세 |
| 관련 API | `/api/securities`, `/{ticker}`, `/quotes`, `/api/stocks/{code}/chart`, WebSocket `/ws-stocks` |
| Backend 주요 코드 | `SecurityService`, `SecurityQuoteService`, `KisTokenManager`, `KisApiClient`, `StockSubscriptionManager` |
| Frontend 주요 코드 | `SecurityDetailView.vue`, `usePriceFeed.js`, `securityApi.js` |
| 관련 DB / Redis | `securities`, `security_daily_prices`; KIS token/quote cache |
| 유효성 검증 | batch 최대 100개, KIS 지원 코드 여부, period D/W/M |
| 예외처리 | 종목별 unsupported/error, KIS 오류 502, 토큰 만료 1회 재시도 |
| 구현 상태 | 구현 완료 |
| 비고 | 외부 API 자격 증명과 장 운영 여부에 따라 실시간 시연 가능성이 달라짐 |

## 3.14 가상 주식 주문

| 항목 | 내용 |
| --- | --- |
| 대분류 | 거래 |
| 기능명 | 시장가/지정가 매수·매도·취소·보유내역 |
| 기능 설명 | 한국 장 시간 규칙과 잠금 자산을 적용한 가상 체결 |
| 사용자 입력 | 매수/매도, 시장가/지정가, 수량, 가격 |
| 주요 처리 내용 | 주문 가능액/수량 검사, 즉시 체결 또는 PENDING 잠금, 틱 매칭, 취소·장 마감 만료, 보유 평균단가 갱신 |
| 처리 결과 | 주문/체결/취소, 현금·보유종목 변경 |
| 관련 화면 | 종목 상세 하단 주문창, `/virtual/trade/:securityId`, 거래내역 |
| 관련 API | `POST/GET/DELETE /api/orders`, `/api/securities/{id}/quote|orderable`, account holdings |
| Backend 주요 코드 | `OrderService`, `OrderExecutionService`, `TickMatchingEngine`, `OrderMatchTransactionService`, `MarketCloseScheduler` |
| Frontend 주요 코드 | `TradeBottomSheet.vue`, `TradeView.vue`, `useTradeOrder.js` |
| 관련 DB / Redis | `security_orders`, `holding_securities`, `accounts`, `account_transactions`; 실시간 틱 메모리 map |
| 유효성 검증 | 평일 09:00~15:30 KST, 수량/가격, 잔액/보유량, 상태 전이 |
| 예외처리 | `TradeException` 코드/HTTP status, 중복 체결 방지 조건 UPDATE |
| 구현 상태 | 구현 완료(성향 후속 갱신은 일부 경로 누락) |
| 비고 | 거래 UI가 `TradeView`와 BottomSheet 두 경로로 중복됨 |

## 3.15 통합 거래내역

| 항목 | 내용 |
| --- | --- |
| 대분류 | 자산관리 |
| 기능명 | 주식·예적금 내역 통합 표시 |
| 기능 설명 | 서로 다른 API 결과를 Frontend에서 공통 행으로 변환 |
| 사용자 입력 | 기간, 유형, 정렬, 미체결 주문 취소 |
| 주요 처리 내용 | 주식 주문과 상품 history 동시 조회, client-side normalize/filter/sort |
| 처리 결과 | 통합 내역과 미체결 관리 |
| 관련 화면 | `/virtual/history` |
| 관련 API | `GET /api/orders`, `GET /api/products/holdings/history`, 주문 취소 |
| Backend 주요 코드 | `TradeOrderController`, `ProductHoldingController` 및 각 Service/Mapper |
| Frontend 주요 코드 | `VirtualHistoryView.vue` |
| 관련 DB / Redis | `security_orders`, `product_transactions` |
| 유효성 검증 | 기간/상태/유형 query, 취소 가능 상태 |
| 예외처리 | 부분 API 실패/취소 오류 UI |
| 구현 상태 | 구현 완료 |
| 비고 | 통합 자체는 Backend 단일 API가 아니라 Frontend 조합 |

## 3.16 일일 금융 퀴즈

| 항목 | 내용 |
| --- | --- |
| 대분류 | 참여 |
| 기능명 | 날짜별 O/X 퀴즈와 정답 보상 |
| 기능 설명 | 계좌 보유 사용자가 하루 한 번 참여하고 정답 시 투자금을 받음 |
| 사용자 입력 | O 또는 X |
| 주요 처리 내용 | KST 날짜로 JSON 문제 선택, DB 원자적 참여 표시, 정답 시 잔액과 거래내역 증가 |
| 처리 결과 | 정답/해설/보상액 |
| 관련 화면 | 홈 `HomeDailyQuizCard.vue` |
| 관련 API | `GET /api/quizzes/today`, `POST /today/answer` |
| Backend 주요 코드 | `QuizService`, `QuizDataService`, `QuizMapper.xml` |
| Frontend 주요 코드 | `HomeDailyQuizCard.vue`, `quizApi.js` |
| 관련 DB / Redis | `accounts.last_quiz_date`, `account_transactions`; `DailyQuiz.json` |
| 유효성 검증 | 계좌 존재, 당일 문제 존재, 하루 1회 |
| 예외처리 | 계좌/문제/중복 참여 오류 400 |
| 구현 상태 | 구현 완료 |
| 비고 | 문제 날짜가 JSON에 없으면 퀴즈 없음으로 표시 |

## 3.17 리더보드

| 항목 | 내용 |
| --- | --- |
| 대분류 | 소셜/성과 |
| 기능명 | 동일 성향·친구 수익률 순위 |
| 기능 설명 | 현금+현재가 주식+세후 예적금 평가액으로 총자산/수익률 산출 |
| 사용자 입력 | 성향/친구 탭 |
| 주요 처리 내용 | 1분마다 MySQL/KIS 조회·Redis cache/ZSET 갱신, 수익률 내림차순 동률 순위 |
| 처리 결과 | 내 순위와 목록 |
| 관련 화면 | `/leaderboard` |
| 관련 API | `/api/leaderboard/persona`, `/friends` |
| Backend 주요 코드 | `LeaderboardScheduler`, `LeaderboardRefreshService`, `LeaderboardRedisService`, `LeaderboardService` |
| Frontend 주요 코드 | `LeaderboardView.vue`, `leaderboardApi.js` |
| 관련 DB / Redis | accounts/holdings/results/personas; `leaderboard:user:*`, persona ZSET(3분 TTL) |
| 유효성 검증 | 성향/계좌/캐시 없으면 빈 결과 |
| 예외처리 | 외부 시세가 없는 보유분은 계산에서 제외될 수 있음 |
| 구현 상태 | 구현 완료 |
| 비고 | 캐시 TTL 3분, 갱신 fixedDelay 1분 |

## 3.18 친구·알림

| 항목 | 내용 |
| --- | --- |
| 대분류 | 소셜 |
| 기능명 | 친구 요청/수락/거절/삭제와 이벤트 알림 |
| 기능 설명 | 닉네임으로 친구 관계를 관리하고 친구/거래 이벤트 알림을 저장 |
| 사용자 입력 | 닉네임, 요청 처리, 알림 설정/읽음/삭제 |
| 주요 처리 내용 | 관계 중복 검사, transaction commit 후 별도 트랜잭션으로 알림 생성, 설정별 차단 |
| 처리 결과 | 친구 목록/요청, 읽지 않은 개수, 알림 목록 |
| 관련 화면 | `/my/friends`, `/my/notifications`, 헤더 알림 벨 |
| 관련 API | `/api/friends/**`, `/api/notifications/**` |
| Backend 주요 코드 | `FriendServiceImpl`, `FriendNotificationListener`, `TradeNotificationListener`, `NotificationServiceImpl` |
| Frontend 주요 코드 | `FriendManagementView.vue`, `NotificationSettingsView.vue`, `NotificationBellButton.vue`, `notification` store |
| 관련 DB / Redis | `friendships`, `notification_settings`, `notifications` |
| 유효성 검증 | 존재 닉네임, 자기요청 금지, 중복관계 금지, 소유자 조건 UPDATE/DELETE |
| 예외처리 | 잘못된 관계 작업 400; 알림 후처리 실패는 로그 후 원 거래 유지 |
| 구현 상태 | 구현 완료 |
| 비고 | 친구 화면은 받은 요청/친구를 4초 간격으로 갱신, unread는 12초 polling |

---

# 4. 화면 명세

| 화면명 | Route | 파일 | 화면 목적 | 주요 표시 정보 | 사용자 액션 | 연결 API | 이동 화면 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 홈 | `/` | `HomeView.vue` | 진단/추천/퀴즈 허브 | 사용자, 성향 상태, 추천 카드, 퀴즈 | 진단·상품·상세 이동, 퀴즈 답변 | auth/me, assessment, securities recommendation/quotes, product lists, quiz | 게임, 결과, 상품, 상세 |
| 상품 | `/products` | `ProductsView.vue` | 금융상품 통합 탐색 | 종목/예금/적금 탭 | 검색·필터·정렬·페이지 | securities, deposits, savings, goal recommendations | 상품/종목 상세 |
| 상품 상세 | `/products/:type/:id` | `ProductDetailView.vue` | 금리·조건 확인 | 옵션, 금리, 우대조건, 가입 URL | 옵션 선택, 가상가입 | product detail | 가입 |
| 상품 가입 | `/products/:type/:id/subscribe` | `ProductSubscriptionView.vue` | 가상 예·적금 가입 | 잔액, 만기/이자 예상 | 금액·우대조건·납입일 입력 | detail, estimate, holding POST | 보유/자산 |
| 가상투자 Shell | `/virtual` | `VirtualInvestView.vue` | 하위 화면 레이아웃 | 탭/RouterView | 탭 이동 | 없음 | assets/products/history |
| 자산 | `/virtual/assets` | `VirtualAssetsView.vue` | 통합 자산 현황 | 현금·주식·예적금·수익률 | 보유 상세 이동 | accounts, holdings | 주식/상품 보유 |
| 가상 상품 | `/virtual/products` | `VirtualProductsView.vue` | 보유상품 진입/탐색 | 보유 요약 | 상품/보유 이동 | product holdings | products/holdings |
| 거래내역 | `/virtual/history` | `VirtualHistoryView.vue` | 주문·상품 내역 통합 | 상태/금액/일자 | 기간·유형·정렬, 주문취소 | orders, product history | 상세 |
| 상품 보유목록 | `/virtual/holdings` | `ProductHoldingsView.vue` | 가입 상품 목록 | 현재가치·만기·상태 | 상세 선택 | holdings | 보유 상세 |
| 상품 보유상세 | `/virtual/holdings/:id` | `ProductHoldingDetailView.vue` | 가입 상품 평가 | 원금·예상이자·세금·만기 | 해지 이동 | holding detail | 해지 |
| 상품 해지 | `/virtual/holdings/:id/termination` | `ProductTerminationView.vue` | 중도해지 확인/실행 | 환급·포기이자·성향 변화 | 해지 확정 | latest assessment, estimate, terminate | 보유/자산 |
| 주식 보유 | `/virtual/stocks` | `StockHoldingsView.vue` | 보유종목 목록 | 평균단가·현재가·수익률 | 종목 선택 | account holdings, quotes | 종목 상세 |
| 가상투자 시작 | `/virtual/start` | `VirtualInvestStartView.vue` | 계좌 개설 | 시드머니 안내 | 계좌 생성 | POST accounts | 자산 |
| 종목 상세 | `/virtual/securities/:pk` | `SecurityDetailView.vue` | 시세·차트·주문 | 현재가, 등락, 캔들, 보유 | 기간 변경, 매수/매도 | security/detail/chart/quotes/holdings/orderable, STOMP | 주문/보유 |
| 전용 거래 | `/virtual/trade/:securityId` | `TradeView.vue` | 별도 주문 화면 | 호가성 현재가·주문 가능량 | 주문 제출 | quote, orderable, orders | 이전/내역 |
| 리더보드 | `/leaderboard` | `LeaderboardView.vue` | 성향/친구 순위 | 순위·총자산·수익률 | 탭/정렬 | leaderboard 2종 | 사용자 흐름 유지 |
| 마이 | `/my` | `MyPageView.vue` | 계정/설정 메뉴 | 프로필, 메뉴 | 프로필·친구·알림 이동 | auth/me | 하위 화면 |
| 프로필 | `/my/profile` | `MyProfileView.vue` | 개인정보 수정 | 이메일·닉네임·생년월일 | 수정 | profile GET/PATCH | 마이 |
| 친구 | `/my/friends` | `FriendManagementView.vue` | 친구/요청 관리 | 받은/보낸 요청·친구 | 요청·수락·거절·취소·삭제 | friends 전체 | 리더보드 등 |
| 알림 설정 | `/my/notifications` | `NotificationSettingsView.vue` | 알림 유형 on/off | 거래·친구 설정 | toggle 저장 | notifications/settings | 마이 |
| 로그인 | `/login` | `LoginView.vue` | 인증 | 입력/오류 | 로그인, 가입/재설정 이동 | auth/login | 홈/가입/재설정 |
| 회원가입 | `/signup` | `SignUpView.vue` | 계정 생성 | 입력 검증 | 가입 | auth/signup | 로그인 |
| 비밀번호 재설정 | `/password-reset` | `PasswordResetView.vue` | 재설정 요청 UI | 이메일/완료 상태 | 요청 | 없음 | 로그인 |
| 게임 소개 | `/game/introduction` | `GameIntroductionView.vue` | 진단 설명 | 단계/안내 | 튜토리얼/시작 | game status | 튜토리얼/시작 |
| 게임 튜토리얼 | `/game/tutorial` | `GameTutorialView.vue` | 로컬 연습 | 예시 자산/행동 | 연습 | 없음 | 시작 |
| 게임 시작 | `/game/start` | `GameStartView.vue` | 서버 게임 시작 | 진행 안내 | 시작 | game start/status | 배분 |
| 게임 배분 | `/game/allocation` | `GameAllocationView.vue` | 초기 자산 배분 | 현금·주식·예금 | 비중 조정/확정 | game action | 게임 |
| 게임 | `/game` | `GameView.vue` | 틱별 의사결정 | 시장상황·자산 | 매수/매도/예금/다음 | scenario, actions, completion | 결과 |
| 성향 결과 | `/assessment/result` | `AssessmentResultView.vue` | 진단 결과 설명 | 3축·유형·포트폴리오·이유 | 목표/상품 이동 | assessment result | 목표/상품/유형 |
| 성향 유형 | `/assessment/personas` | `PersonaTypesView.vue` | 8유형 탐색 | 이름·축·비중 | 유형 선택 | personas | 결과 |
| 금융 목표 | `/financial-goal` | `FinancialGoalView.vue` | 목표 저장 | 유형·금액·기간·월 참고액 | 저장/삭제 | financial-goals | 상품 |

## 4.1 주요 UI 동작

- 상품 목록은 탭/검색어/필터/정렬/페이지를 URL query와 동기화하고 검색어를 debounce한다.
- 종목 현재가는 polling과 WebSocket을 조합하며, batch 중 일부 종목 실패를 전체 실패로 만들지 않는다.
- 홈 추천은 가로 스냅 스크롤과 로딩/빈 결과/재시도 상태를 가진다. 개발환경 변수로만 preview mock을 사용할 수 있다.
- 주문 버튼은 장 시간, 수량·가격, 주문가능 금액/수량, 제출 중 상태에 따라 비활성화된다.
- 알림 벨은 읽음·전체 읽음·삭제 및 reference 기반 deep-link를 지원한다.
- 마이페이지의 공지/고객지원/약관 항목은 실제 Route/API가 아니라 `console.log`만 수행한다.

---

# 5. API 명세

표의 사용 상태는 현재 Frontend `src/api` 함수와 View/Component 호출까지 대조한 결과다. `인증`의 “공개”는 Security 설정 기준이다.

| Method | URL | 기능 / Request → Response | 인증 | Controller / Service | DB·외부 접근 | 사용 상태·예외 |
| --- | --- | --- | --- | --- | --- | --- |
| GET | `/` | JSP home | 공개 | `HomeController` | 없음 | 현재 Vue 흐름 미사용 |
| GET | `/api/health` | health text | 공개 | `HealthController` | 없음 | Backend만 존재 |
| GET | `/api/security/all` | 보안 테스트 | 공개 | `SecurityController` | 없음 | 레거시/미사용 |
| GET | `/api/security/member` | 인증 테스트 | 필요 | `SecurityController` | 없음 | 레거시/미사용 |
| GET | `/api/security/login`, `/logout` | 문자열 placeholder | 공개 규칙 혼재 | `SecurityController` | 없음 | 실제 인증 흐름 미사용 |
| POST | `/api/auth/signup` | 가입 DTO → 사용자 | 공개 | `UserController` / `UserServiceImpl` | users | Frontend 사용; 400/409 |
| POST | `/api/auth/login` | email/password → Access + cookie | 공개 | 로그인 Filter | users, Redis | Frontend 사용; 400/401 |
| GET | `/api/auth/me` | 현재 사용자 | 필요 | `UserController` | users | Frontend 사용 |
| POST | `/api/auth/refresh` | Refresh cookie → 새 token pair | 공개 | `UserController` / `RefreshTokenService` | Redis | Frontend 자동 사용; 401 |
| POST | `/api/auth/logout` | cookie 소비/삭제 | 공개 | `UserController` | Redis | Frontend 사용 |
| GET/PATCH | `/api/my/profile` | 프로필 조회/수정 | 필요 | `ProfileController` / `UserServiceImpl` | users | Frontend 사용; 중복/검증 |
| GET | `/api/games/status` | 게임 진행 상태 | 필요 | `GameController` / status service | action_logs/results | Frontend 사용 |
| POST | `/api/games/start` | 게임 시작 | 필요 | `GameController` / `GameStartService` | action_logs | Frontend 사용 |
| POST | `/api/games/actions` | 행동 DTO → 저장 결과 | 필요 | `GameController` / `GameActionService` | action_logs | Frontend 사용; 순서 검증 |
| POST | `/api/games/completion` | 완료 → 점수/유형 | 필요 | `GameController` / `GameAssessmentService` | action_logs/results/personas | Frontend 사용; 중복 완료 차단 |
| GET | `/api/games/scenarios/{scenarioId}` | 시나리오 JSON | 공개 | `ScenarioController` | resource JSON | Frontend 사용 |
| GET | `/api/assessments/me/latest` | 최신 축 점수 | 필요 | `AssessmentController` / result service | results | Frontend 사용; 없으면 404 |
| GET | `/api/assessments/me/result` | 전체 유형 보고서 | 필요 | `AssessmentController` / result service | results/personas | Frontend 사용; 없으면 204 |
| GET | `/api/personas` | 8유형 목록 | 공개 | `PersonaController` | personas | Frontend 사용 |
| POST/GET | `/api/accounts` | 계좌 생성/통합자산 | 필요 | `AccountController` / `AccountService` | accounts/holdings | Frontend 사용 |
| GET/PUT/DELETE | `/api/financial-goals` | 목표 조회/upsert/삭제 | 필요 | `FinancialGoalController` / `FinancialGoalService` | financial_goals | Frontend 사용 |
| GET | `/api/financial-goals/recommendations` | `productType,page,size` → goal plan+상품 | 필요 | `FinancialGoalController` / `FinancialGoalService` | goal/products | Frontend 사용; 기간 적용 결함 주의 |
| GET | `/api/products/deposits/external` | FSS 원본 조회 | 필요 | `DepositProductController` / external service | FSS API | Backend만 존재 |
| POST | `/api/products/deposits/collect` | 예금 수집 | 필요 | `DepositProductController` / collection service | FSS→products | Backend만 존재 |
| POST | `/api/products/savings/collect` | 적금 수집 | 필요 | `SavingProductController` / collection service | FSS→products | Backend만 존재 |
| GET | `/api/products/deposits` | 필터/페이지 목록 | 공개 | `DepositProductController` / product service | product tables | Frontend 사용 |
| GET | `/api/products/deposits/{id}` | 예금 상세 | 공개 | 위와 같음 | product tables | Frontend 사용; 없음 오류 |
| GET | `/api/products/savings` | 필터/페이지 목록 | 공개 | `SavingProductController` / product service | product tables | Frontend 사용 |
| GET | `/api/products/savings/{id}` | 적금 상세 | 공개 | 위와 같음 | product tables | Frontend 사용 |
| GET | `/api/products/recommendations` | 예·적금 중 최고금리 1건 | 필요 | `ProductRecommendationController` / `ProductRecommendationService` | product tables | Backend만 존재; export도 실제 호출 안 됨 |
| POST | `/api/products/holdings/subscription-estimate` | 가입 예상 | 필요 | `ProductHoldingController` / `ProductHoldingService` | product/account | Frontend 사용 |
| POST | `/api/products/holdings` | 가상 가입 | 필요 | 위와 같음 | account/holding/transactions/results | Frontend 사용 |
| GET | `/api/products/holdings` | 보유상품 목록 | 필요 | 위와 같음 | holding_products | Frontend 사용 |
| GET | `/api/products/holdings/{id}` | 보유 상세 | 필요 | 위와 같음 | holdings/products | Frontend 사용 |
| GET | `/api/products/holdings/{id}/termination-estimate` | 해지 예상 | 필요 | 위와 같음 | holdings | Frontend 사용 |
| POST | `/api/products/holdings/{id}/terminate` | 해지 실행 | 필요 | 위와 같음 | account/holding/transactions/results | Frontend 사용 |
| GET | `/api/products/holdings/assets` | 예적금 자산 집계 | 필요 | 위와 같음 | holding_products | 직접 Frontend 사용 미확인 |
| GET | `/api/products/holdings/history` | 상품 거래내역 | 필요 | 위와 같음 | product_transactions | Frontend 사용 |
| GET | `/api/securities` | 종목 목록/적합도 | Security 설정상 공개 | `SecuritiesController` / `SecurityService` | results/securities | Frontend 사용; 익명 null 위험 |
| GET | `/api/securities/recommendations` | 최대 2개 추천 | Security 설정상 공개 | 위와 같음 | results/securities | Frontend 사용; 익명 null 위험 |
| GET | `/api/securities/{ticker}` | 종목 상세 | Security 설정상 공개 | 위와 같음 | securities | Frontend 사용 |
| POST | `/api/securities/quotes` | ticker 최대 100개 → batch quote | Security 설정상 공개 | `SecuritiesController` / `SecurityQuoteService` | DB, Redis, KIS | Frontend 사용; 항목별 실패 |
| GET | `/api/stocks/{stockCode}/price` | 현재가 | Security 규칙상 인증 | `StockController` / KIS service | KIS | Backend만 존재로 판단 |
| GET | `/api/stocks/{stockCode}/chart` | 기간별 캔들 | 필요 | `StockController` / KIS service | KIS | Frontend 사용 |
| GET | `/api/securities/{id}/quote` | 거래용 시세 | 공개 matcher에 포함 가능 | `TradeSecurityController` | KIS | Frontend 사용 |
| GET | `/api/securities/{id}/orderable` | 주문 가능액/수량 | 필요(명시적 우선 matcher) | `TradeSecurityController` / trade service | account/holding/KIS | Frontend 사용 |
| POST | `/api/orders` | 주문 생성/체결 | 필요 | `TradeOrderController` / `OrderService` | order/account/holding/KIS | Frontend 사용; 구조화 거래 오류 |
| DELETE | `/api/orders/{id}` | PENDING 주문 취소 | 필요 | 위와 같음 | order/account/holding | Frontend 사용 |
| GET | `/api/orders` | 주문 목록/필터 | 필요 | 위와 같음 | security_orders | Frontend 사용 |
| GET | `/api/accounts/me/portfolio` | 포트폴리오 | 필요 | `TradeAccountController` | account/holdings/KIS | API 함수는 있으나 현재 화면 직접 사용 미확인 |
| GET | `/api/accounts/me/holdings` | 주식 보유 | 필요 | `TradeAccountController` | holdings/securities/KIS | Frontend 사용 |
| GET/POST | `/api/quizzes/today`, `/today/answer` | 오늘 퀴즈/응답 | 필요 | `QuizController` / `QuizService` | account/transaction/JSON | Frontend 사용 |
| GET | `/api/leaderboard/persona`, `/friends` | 성향/친구 순위 | 필요 | `LeaderboardController` / `LeaderboardService` | Redis | Frontend 사용 |
| POST | `/api/friends/requests` | 닉네임 요청 | 필요 | `FriendController` / `FriendServiceImpl` | friendships/users | Frontend 사용 |
| GET | `/api/friends/requests/received`, `/sent` | 요청 목록 | 필요 | 위와 같음 | friendships/users | Frontend 사용 |
| PATCH | `/api/friends/requests/{id}/accept`, `/reject` | 요청 처리 | 필요 | 위와 같음 | friendships | Frontend 사용 |
| GET/DELETE | `/api/friends`, `/api/friends/{userId}` | 목록/삭제 | 필요 | 위와 같음 | friendships | Frontend 사용 |
| DELETE | `/api/friends/requests/{id}` | 보낸 요청 취소 | 필요 | 위와 같음 | friendships/notifications | Frontend 사용 |
| GET/PATCH | `/api/notifications/settings` | 설정 조회/수정 | 필요 | `NotificationController` / `NotificationServiceImpl` | notification_settings | Frontend 사용 |
| GET | `/api/notifications`, `/unread-count` | 목록/미확인 수 | 필요 | 위와 같음 | notifications | Frontend 사용 |
| PATCH | `/api/notifications/{id}/read`, `/read-all` | 읽음 처리 | 필요 | 위와 같음 | notifications | Frontend 사용 |
| DELETE | `/api/notifications` | 전체 삭제 | 필요 | 위와 같음 | notifications | Frontend 사용 |

`POST /api/auth/login`은 Controller 메서드가 아니라 `JwtUsernamePasswordAuthenticationFilter`가 처리한다.

---

# 6. 데이터베이스 구조

## 6.1 테이블 목록

| 테이블 | 역할 | 주요 기능 |
| --- | --- | --- |
| `users` | 회원 | 로그인·프로필·친구 식별 |
| `personas` | 8개 성향 기준/설명 | axis code, 포트폴리오와 이유 |
| `action_logs` | 게임 행동 | 틱·행동·금액·시장 컨텍스트 |
| `results` | 성향 결과 이력 | RT/LH/RP, persona, 생성시각 |
| `accounts` | 가상계좌 | 시드, 현금, 잠금현금, 마지막 퀴즈일 |
| `securities` | 종목 메타데이터 | ticker/KIS code/type/market, RT/LH/RP |
| `security_daily_prices` | 종목 일봉 | OHLCV와 거래일 |
| `holding_securities` | 주식 보유 | 수량, 잠금수량, 평균단가 |
| `security_orders` | 주식 주문 | 유형, 가격, 수량, 상태, 체결정보 |
| `products` | 예·적금 상품 | 금융사·상품명·유형·공시 정보 |
| `product_options` | 기간/금리 옵션 | 저축기간, 기본/최고금리, 적립방식 |
| `product_preferential_conditions` | 검색용 우대조건 | 상품-분류 enum |
| `product_preferential_rate_conditions` | 옵션별 구조화 금리조건 | 조건명·추가금리·선택여부·그룹/순서 |
| `holding_products` | 가입 예·적금 | 원금·적용금리·시작/만기·납입정보·상태 |
| `product_transactions` | 상품 거래 | 가입/해지, 원금·이자·세금 |
| `account_transactions` | 계좌 원장 | 입출금·상품/주문/퀴즈 참조 |
| `account_daily_snapshots` | 일별 자산 스냅샷 | 기간 성향 계산 입력 |
| `assessment_settlements` | 평가 정산 상태 | 일/주 평가 중복 방지 |
| `friendships` | 친구/요청 | requester/receiver/status |
| `notification_settings` | 알림 설정 | 거래·친구 허용 |
| `notifications` | 사용자 알림 | 유형·제목·메시지·reference·읽음 |
| `financial_goals` | 사용자 단일 목표 | 유형·목표/현재금액·개월 |
| `logs` | 레거시 토큰 로그 | 현재 Redis Refresh 흐름에서는 사용 확인 안 됨 |

## 6.2 주요 관계

```text
users 1:1 accounts                 (accounts.user_id UNIQUE)
users 1:N action_logs
users 1:N results N:1 personas
users 1:1 financial_goals          (user_id UNIQUE)
users 1:1 notification_settings
users 1:N notifications
users N:M users through friendships

accounts 1:N holding_securities N:1 securities
accounts 1:N security_orders N:1 securities
accounts 1:N holding_products N:1 product_options N:1 products
accounts 1:N account_transactions
accounts 1:N product_transactions
accounts 1:N account_daily_snapshots
accounts 1:N assessment_settlements

securities 1:N security_daily_prices
products 1:N product_options
products 1:N product_preferential_conditions
product_options 1:N product_preferential_rate_conditions
```

## 6.3 기능별 데이터 흐름

- 게임: `users → action_logs → GameBehaviorAssessmentCalculator → results → personas`.
- 증권 추천: 최신 `results` 3축과 `securities` 3축을 SQL에서 비교한다.
- 주문: `accounts.locked_cash` 또는 `holding_securities.locked_quantity`를 확보하고 `security_orders` 상태를 전이한다. 체결 후 잔액/보유량/원장을 갱신한다.
- 상품 가입/해지: `product_options`와 구조화 우대조건을 검증하고 `holding_products`, `product_transactions`, `account_transactions`, `accounts`를 같은 트랜잭션에서 바꾼다.
- 리더보드: DB 보유 데이터를 읽고 KIS 현재가를 결합한 뒤 Redis value/ZSET에 3분 TTL로 저장한다.

## 6.4 스키마 적용 주의

`src/main/resources/db/migration`에 V1~V24와 `R__seed_personas.sql`이 있으나 `build.gradle`에서 Flyway 의존성을 확인하지 못했다. 따라서 마이그레이션 SQL의 존재와 현재 런타임 DB에 자동 적용된다는 사실은 구분해야 한다. 별도 배포 파이프라인/수동 실행이 있는지는 현재 저장소만으로 판단 불가다.

---

# 7. 회원·인증·보안 구조

## 7.1 실제 인증 흐름

```text
POST /api/auth/login
→ JwtUsernamePasswordAuthenticationFilter가 JSON 파싱
→ AuthenticationManager + DaoAuthenticationProvider
→ UserDetailsService + BCrypt 검증
→ JwtProvider가 access/refresh 생성
→ refresh SHA-256 hash를 Redis auth:refresh:{jti}에 TTL 저장
→ access는 JSON, refresh는 HttpOnly Cookie

인증 API 요청
→ Frontend http.js가 Bearer access 첨부
→ JwtAuthenticationFilter가 서명/issuer/type/만료 검증
→ CustomUserDetails 로드
→ SecurityContext에 Authentication 설정

Access 만료(401)
→ Frontend의 단일 진행(single-flight) refresh 요청
→ RefreshTokenService가 Lua GET/비교/DEL로 기존 token 원자 소비
→ 새 token pair 회전 후 원 요청 1회 재시도

로그아웃
→ Refresh token을 가능한 경우 소비
→ Cookie Max-Age=0
→ Frontend access 제거
```

## 7.2 보안 요소 판정

| 요소 | 실제 사용 | 근거 |
| --- | --- | --- |
| Spring Security | 예 | `SecurityConfig.securityFilterChain()` |
| JWT Access/Refresh | 예 | `JwtProvider`, `RefreshTokenService` |
| Redis Refresh 저장 | 예 | 해시 저장, jti key, TTL, Lua 원자 소비 |
| BCrypt | 예 | `BCryptPasswordEncoder`, signup/auth provider |
| HttpOnly Cookie | 예 | refresh cookie; Secure 기본 true, SameSite Strict 기본 |
| CSRF | 비활성 | Stateless API 구성상 `csrf.disable()` |
| CORS | 예 | 환경값 allowed origin patterns, credentials, 모든 method/header |
| 권한 제어 | 인증/공개 구분만 | 별도 role 기반 권한 모델은 확인되지 않음 |
| Session | Stateless | `SessionCreationPolicy.STATELESS` |

## 7.3 보안상 주의

- `/api/securities/**`는 대체로 `permitAll`인데 `SecuritiesController`의 목록/추천은 `authenticatedUser.getUserId()`를 null 검사 없이 호출한다. Frontend Route는 인증을 요구하지만, 익명 직접 호출은 500 가능성이 있다.
- Access Token은 Frontend local storage 계열 저장소에 보관된다. Refresh는 HttpOnly라 JavaScript에서 읽지 못한다.
- `logs` 테이블의 레거시 토큰 필드는 현재 Refresh Redis 구현과 연결되지 않는다.

---

# 8. 사용자 성향 분석 로직

## 8.1 저장 행동과 3축

- 게임 행동: 시나리오/틱, 행동 유형, 금액·수량, 초기 배분 등 `action_logs`.
- 가상투자 행동: 체결 주문, 상품 가입/해지, 관련 선행 행동(24시간), 보유기간, 실현/평가 수익률, 일 거래횟수, 현금/주식/상품 비중, 일별 스냅샷.
- 축: `RT` 위험감수, `LH` 유동성 선호, `RP` 기대수익 추구. UI도 각각 “위험감수·유동성·기대수익”으로 표시한다.

## 8.2 게임 최초 점수

`GameScoreCalculator`의 기본 구조:

```text
RT = clamp(50 + Σ RT delta, 0, 100)
LH = clamp(50 + Σ LH delta, 0, 100)
RP = clamp(50 + Σ RP delta, 0, 100)
소수 둘째 자리 반올림
```

실제 게임 완료는 `GameBehaviorAssessmentCalculator`가 초기 배분, 개별 행동, 반복, 순서/복합 행동을 계산한다.

대표 규칙:

| 조건 | RT | LH | RP |
| --- | ---: | ---: | ---: |
| 초기 주식 비중 ≥70% | +10 | -5 | +5 |
| 초기 예금 비중 ≥50% | -10 | -5 | -5 |
| 초기 현금 비중 ≥30% | -5 | +10 | -5 |
| 폭락장 매수(게임) | +10 | -5 | +5 |
| 예금 해지 후 주식 매수(게임) | +5 | -10 | +5 |
| 만기까지 예금 유지 | -5 | -10 | -5 |

게임 계산기는 같은 틱 중복 적용을 방지하며, 반복 규칙 그룹에 후보수/매수/매도/시장상태별 상한을 둔다. 코드 상수에는 후보 P95 4, 매수·매도 그룹 cap 2.5, 상태 그룹 cap 1.5가 있다. 이는 머신러닝 모델이 아니라 테스트 시뮬레이션으로 조정된 결정론적 규칙이다.

## 8.3 가상투자 행동 규칙

`BehaviorRuleEngine`에서 확인되는 주요 임계값:

| 행동 조건 | 점수 변화 |
| --- | --- |
| 폭락장 매수 비율 10~30% / ≥30% | `(+10,-5,0)` / `(+15,-10,0)` |
| 폭락장 매도 비율 20~50% / ≥50% / 전량 | `(0,+5,-5)` / `(-10,+5,-5)` / `(-15,+10,-5)` |
| 상승장 매수 비율 10~30% / ≥30% | `(0,-5,+10)` / `(+10,-10,+5)` |
| 상승장 이익 매도 20~50% / ≥50% | `(0,+5,+5)` / `(0,+10,0)` |
| 일중 변동폭 ≥5%이고 당일 매매 | `(+5,+5,+10)` |
| 평가손실 ≤-15% 물타기 10~30% / ≥30% | `(+10,-5,0)` / `(+15,-10,0)` |
| 실현손실 ≤-10% 손절 20~50% / ≥50% / 전량 | `(-5,+5,-5)` / `(-10,+10,-5)` / `(-15,+10,-10)` |
| 예금 해지 후 24시간 내 증권 매수 | `(+5,-10,+10)` |
| 3일 미만 보유 | `(+5,+5,+10)` |
| 30일 이상 보유 | `(0,-5,-5)` |
| 24시간 내 종목 교체 | `(+5,-5,+10)` |
| 하루 거래 ≥5회 / 평균 ≤0.2회 | RP `+5` / `-5` |

연속 같은 행동은 2회일 때 `×1.2`, 3회 이상 `×1.5`이며 초기 배분에는 적용하지 않는다. 현금 비중을 5일 유지하거나, 해지 후 현금 유지, 정상장 부분매도 후 현금 유지, 수익 실현 등 후속 행동 규칙도 존재한다.

## 8.4 시장상태와 컨텍스트

`MarketStateCalculator` 우선순위:

```text
일중 고저 변동폭 ≥ 5% → VOLATILE
가격 등락률 ≤ -5%     → CRASH
가격 등락률 ≥ 3%      → BULL
그 외                 → NORMAL
```

`BehaviorContextFactory`는 24시간 내 관련 행동, 매도비율, 매수금액비율, 보유일, 손익률, 동일 행동 연속성 등을 만든다. 게임의 연속 행동은 인접 tick 차이 ≤1, 가상투자는 24시간 범위로 판단한다.

## 8.5 시간 갱신·분류

가상투자 후 점수:

```text
delta = 0이면 기존 점수 유지
behaviorScore = clamp(50 + delta × 3.33, 0, 100)
updatedScore  = round(clamp(current × 0.9 + behaviorScore × 0.1, 0, 100), 2)
```

명시적인 지수 시간감쇠 함수는 없고, 새 행동을 10% 반영하는 EMA 형태다. 별도로 일/주 스냅샷 평가가 있다.

각 축은 `score >= 50`이면 H, 미만이면 L이다. RT/LH/RP 순서의 3비트 코드로 8유형을 결정한다.

| 코드 | 이름 | 추천 포트폴리오(주식/채권/예금 %) |
| --- | --- | --- |
| HHH | 불꽃 추격자 | 80 / 5 / 15 |
| HHL | 스마트 단타러 | 60 / 10 / 30 |
| HLH | 야망찬 개척자 | 80 / 15 / 5 |
| HLL | 신념의 가치투자자 | 60 / 30 / 10 |
| LHH | 실속파 정보통 | 40 / 20 / 40 |
| LHL | 현금 확보주의자 | 20 / 25 / 55 |
| LLH | 묵묵한 적립왕 | 40 / 40 / 20 |
| LLL | 철저한 금고지기 | 0 / 70 / 30 |

근거는 `R__seed_personas.sql`, `PersonaClassifier`, `AssessmentResultService`, Frontend `AssessmentResultView.vue`다. 결과 DB에는 점수/유형을 저장하지만, 어떤 규칙이 실제 적용됐는지에 대한 개별 rule trace는 저장하지 않는다. 화면의 추천 이유는 persona seed 설명이다.

## 8.6 갱신 경로의 한계

- 즉시 체결 주문은 `SecurityOrderFilledEvent → SecurityOrderAssessmentListener`로 갱신된다.
- 상품 가입/해지는 `ProductHoldingService`가 평가 서비스를 직접 호출한다.
- 일/주 평가는 snapshot/settlement scheduler로 수행된다.
- `TickMatchingEngine`에서 나중에 체결된 PENDING 지정가 주문은 `SecurityOrderFilledEvent` 발행이 TODO다. 따라서 거래 체결과 알림은 처리되지만 이 경로의 성향 점수는 갱신되지 않는다.

---

# 9. 금융상품 데이터 구조 및 수집

## 9.1 예·적금

- 제공 기관: 금융감독원 금융상품 한눈에 API.
- endpoint: `/depositProductsSearch.json`, `/savingProductsSearch.json`.
- 요청: API key, 금융권역 `020000`, page number.
- `DepositProductService`와 `SavingProductService`는 `RestTemplate`으로 호출하고 `max_page_no`까지 페이지를 순회하며 `err_cd == "000"`과 결과 목록을 검증한다.
- 기본 상품은 `products`, 기간/금리는 `product_options`에 upsert한다.
- 주요 원천/내부 필드: 금융회사 코드/명, 상품 코드/명, 가입방법, 만기 후 이율, 우대조건 원문, 가입제한/대상, 최고한도, 공시일, 기간, 기본/최고금리, 적립방식.

## 9.2 우대조건 가공

`PreferentialConditionParser`는 원문 `spcl_cnd`를 검색 필터용 분류로 변환한다. 확인된 분류는 급여이체, 카드사용, 자동이체, 첫거래, 마케팅동의, 주택청약, 오픈뱅킹, 비대면, 기타다.

`PreferentialRateConditionParser`는 옵션별로 조건명, 추가금리, 선택 가능 여부, 표시순서, 그룹 ID/역할을 구조화해 가입 화면의 선택과 적용금리 계산에 사용한다. 원문도 상품에 보존한다.

## 9.3 증권

- `securities`는 종목명/ticker/KIS code, STOCK·EQUITY_ETF·BOND_ETF 유형, market, RT/LH/RP 기준점수를 저장한다.
- KIS REST가 현재가/차트를 제공하고, 일봉 scheduler는 평일 16:00에 종목을 순회해 `security_daily_prices`에 upsert한다.
- 현재 저장소에서 증권 마스터와 RT/LH/RP 점수를 외부에서 수집·산출하는 로직은 확인되지 않았다. 추천 품질은 DB에 이미 들어 있는 값에 의존한다.

## 9.4 갱신 수준

- 수동 수집 API는 Backend에 구현됨.
- 예·적금 자동 정기수집 scheduler는 확인되지 않음.
- KIS 일봉 갱신 scheduler는 구현됨.
- 수집 API를 호출하는 Frontend 운영 화면은 없음.

---

# 10. 금융상품 검색·필터·정렬

| 조건 | 실제 구현 | 위치 |
| --- | --- | --- |
| 키워드 | 상품명 또는 금융회사명 `LIKE` | `ProductMapper.xml`, `ProductListPanel.vue` |
| 가입기간 | 복수 `saving_term IN (...)` | 위 파일 |
| 우대조건 | 선택한 조건마다 `EXISTS`; 복수 선택은 AND | `ProductMapper.xml` |
| 적립방식 | 적금 `reserve_type IN (...)` | `SavingProductService`, Mapper |
| 금리 범위 | 없음 | 전용 query/입력 미확인 |
| 금융사 전용 필터 | 없음 | 키워드 검색만 가능 |
| 상품유형 | 예금/적금 endpoint와 Frontend 탭으로 분리 | controllers, `ProductListPanel.vue` |
| 정렬 | 최고금리 내림/오름, 기본금리 내림/오름, 상품명/금융사명 오름 | Services/Mapper/Frontend constants |
| 기본 정렬 | 최고금리 내림차순 | product services |
| 목표 선호기간 | 필터가 아니라 ORDER 우선순위용 필드 | `preferredSavingTerm` |
| 페이지 | `page`, `size`, total count | services/mappers |

Frontend는 검색 debounce, 복수 checkbox, 탭별 조건, query string 복원과 pagination을 구현한다. 일반 `/products` 화면은 기간 필터가 초기값일 때 전체 지원기간을 명시적으로 보낸다.

Backend service에 기간 파라미터가 없으면 12개월을 기본값으로 넣는 동작이 있다. 따라서 API를 직접 호출하거나 홈에서 기간을 생략하면 사실상 12개월 상품만 조회된다.

---

# 11. 금융상품 추천 로직

## 11.1 사용자 성향 기반 추천

실제 적용 상품군은 증권이다. `SecurityService`가 최신 `results`를 읽고 SQL에서 사용자 축과 종목 축의 Manhattan distance를 계산한다.

```text
distance = |userRT-secRT| + |userLH-secLH| + |userRP-secRP|
matchScore = round(100 - distance / 300 × 100, 1)
정렬 = distance ASC (종목 기준점수 NULL은 뒤)
```

홈 추천은 STOCK+EQUITY_ETF에서 1개, BOND_ETF에서 1개로 최대 2개다. 성향 결과가 없으면 거래량 기준 fallback과 `sortFallback=true`를 반환한다.

예금·적금 상품 조회/순위에는 persona의 추천 비중이나 RT/LH/RP가 반영되지 않는다. 성향 결과 화면의 예금/주식/채권 비율은 설명 데이터이지 예·적금 상품별 추천 점수가 아니다.

## 11.2 목표 기반 추천

실제 저장값:

- 목표 유형: LUMP_SUM, TRAVEL, ELECTRONICS, EDUCATION, HOUSING, EMERGENCY, OTHER.
- 목표 금액, 현재 보유금액, 목표 개월.
- 목표 날짜는 없고, 월 저축금액은 저장하지 않으며 응답 때 계산한다.

계획 계산:

```text
monthlyReferenceAmount = ceil((targetAmount - currentAmount) / targetMonths)
recommendedTerm = 지원 가입기간 중 targetMonths 이하의 최댓값
```

그러나 `FinancialGoalService.createProductRequest()`는 추천기간을 `preferredSavingTerm`에만 넣고 `savingTerms`를 넣지 않는다. 이후 예금/적금 service가 누락 기간을 `[12]`로 정규화하므로 현재 SQL 결과는 12개월 상품으로 제한된다. 추천기간이 24/36개월이어도 해당 기간 상품을 가져온다고 볼 수 없다. 이 때문에 화면의 “목표 시점 전에 만기가 오는 상품” 표현과 실제 동작은 일치하지 않는다.

## 11.3 예·적금 홈/일반 추천

- `ProductRecommendationService`의 `/api/products/recommendations`는 예금 최고금리 1건과 적금 최고금리 1건을 비교해 더 높은 1건을 반환한다. 사용자·성향·목표는 조회하지 않는다.
- 현재 홈 `HomeRecommendationCard.vue`는 이 API도 사용하지 않는다. 예금 목록과 적금 목록을 각각 `{page:1,size:1}`로 호출해 첫 항목을 각각 노출한다. 기간 누락 기본값 때문에 12개월·최고금리 순 상품이다.
- 추천 사유는 증권 match score 또는 fallback 안내가 핵심이며, 예·적금별 개인화 사유는 없다.

---

# 12. 추천 로직의 실제 수준 판정

| 기능 | 실제 Backend 로직 | Frontend 반영 | DB 연결 | 최종 상태 |
| --- | --- | --- | --- | --- |
| 성향 기반 추천 | 최신 3축과 종목 3축 거리 | 종목 카드 match score/fallback | results + securities | 구현 완료(증권 한정) |
| 목표 기반 추천 | 월 참고액·추천기간 계획, 상품 request 생성 | 목표 context에서 전용 API 사용 | goals + products | 부분 구현: 기간 query 결함 |
| 예금 추천 | 최고금리 정렬/1건 선택 | 홈 일반 목록 첫 건, 목표 목록 | products/options | 개인화 추천 미구현 |
| 적금 추천 | 최고금리 정렬/1건 선택 | 홈 일반 목록 첫 건, 목표 목록 | products/options | 개인화 추천 미구현 |
| 주식 추천 | STOCK+EQUITY_ETF top 1 | 홈/목록 반영 | results + securities | 구현 완료 |
| 채권형 ETF 추천 | BOND_ETF top 1 | 홈 반영 | results + securities | 구현 완료 |
| 적합도 계산 | 3축 Manhattan 거리 → 0~100 | 숫자 표시 | results + securities | 구현 완료 |
| 추천 이유 표시 | persona seed의 일반 이유, fallback 문구 | 결과/카드 표시 | personas | 부분 구현: 상품별 동적 이유 없음 |

종합 판정: “성향 기반 금융상품 추천”은 **증권 종목에는 실제 알고리즘이 구현 완료**, 예·적금에는 **금리/필터 기반 탐색만 구현**이다. “목표 기반 추천”은 목표 계산과 UI 연결은 있으나 상품기간 적용 오류 때문에 완료로 분류할 수 없다.

---

# 13. 유효성 검증 및 예외처리

| 상황 | 처리 위치 | 처리 방식 | 사용자에게 보이는 결과 |
| --- | --- | --- | --- |
| 가입 필수값/이메일/생년월일 | signup DTO, `SignUpView` | Bean Validation + client validation | 필드 오류/400 |
| 비밀번호 강도·이메일 포함 | DTO regex, `UserServiceImpl` | 8자·조합·공백 금지, 이메일 문자열 포함 금지 | 가입 실패 메시지 |
| 이메일/닉네임 중복 | user/profile service | 사전조회+DB unique | 409 또는 오류 문구 |
| 로그인 JSON 오류/실패 | 로그인 Filter | parse 실패 400, 자격증명 실패 401 | 로그인 오류 |
| Access 만료 | `http.js` | refresh 후 원 요청 1회 재시도 | 성공 시 화면 유지, 실패 시 인증 해제 |
| Refresh 재사용/만료 | `RefreshTokenService` | Redis hash+Lua 원자 소비 | 401, 재로그인 |
| 인증 필요 | Security Filter | Authentication entry point | 401 |
| 없는 사용자/상품/종목 | Services, advice | NotFound/IllegalArgument 변환 | 404/400 및 API message |
| 잘못된 enum/query/JSON | `CommonExceptionAdvice` | bind/type/malformed 처리 | 400 |
| 평가 결과 없음 | `AssessmentController` | latest 404, result 204 | guard/빈 상태 분기 |
| 계좌 없음/중복 | `AccountService` | 성향완료·중복 검증 | 시작 유도/오류 |
| 상품 잔액/옵션/우대조건 오류 | `ProductHoldingService` | 가입 전 검증, transaction rollback | 가입/해지 오류 |
| 주문 장외/잔액/수량/상태 | trade services | `TradeException` code와 HTTP status | 주문창 오류 |
| 주문 중복 체결 | conditional UPDATE/독립 transaction | 상태가 PENDING인 경우만 전이 | 중복 자산 반영 방지 |
| KIS API 오류/토큰 만료 | `KisApiClient`, advice | 토큰 무효 1회 갱신, 이후 502 | 시세 오류/부분 표시 |
| batch 일부 종목 실패 | `SecurityQuoteService` | 항목별 error/unsupported | 성공 종목은 계속 표시 |
| 친구 자기요청/중복/잘못된 소유자 | `FriendServiceImpl`, mapper 조건 | 요청 차단/영향행 1 검사 | 400 message |
| 알림 설정 빈 PATCH | `NotificationServiceImpl` | 변경 필드 둘 다 null이면 거부 | 400 |
| 네트워크/API 실패 | 각 View/Component | loading/error/empty, 일부 retry | 오류 문구·다시 시도 |
| Backend 예기치 않은 오류 | `CommonExceptionAdvice` | generic 500 response | 일반 오류 메시지 |

Frontend가 모든 Backend 오류 코드를 개별 문구로 번역하는 것은 아니다. 다수 화면은 `ApiError.message` 또는 일반 fallback 문구를 사용한다.

---

# 14. Frontend와 Backend 실제 연동 현황

| 기능 | Frontend | API | Backend | DB | 실제 연결 상태 |
| --- | --- | --- | --- | --- | --- |
| 회원가입/로그인/재발급/로그아웃 | auth views/store/http | auth endpoints | filter/services | users+Redis | 완전 연결 |
| 프로필 | profile view | profile GET/PATCH | profile service | users | 완전 연결 |
| 비밀번호 재설정 | 화면/timer | 없음 | 없음 | 없음 | Frontend만 존재 |
| 게임 진단 | 6개 game view | game endpoints | game/assessment services | logs/results | 완전 연결 |
| 결과/8유형 | result/persona views | assessment/personas | result/persona services | results/personas | 완전 연결 |
| 가상행동 성향 갱신 | 거래/상품 행동 | 내부 이벤트/호출 | assessment services | results/snapshots | 부분 연결: 지연 지정가 누락 |
| 목표 CRUD | goal view | financial-goals | goal service | goals | 완전 연결 |
| 목표 상품 추천 | product panel | goal recommendations | goal+product services | goals/products | 부분 연결: 12개월 기본값 결함 |
| 상품 수집 | 없음 | collect/external | collection services | products/options | Backend만 존재 |
| 상품 검색/상세 | products/detail | list/detail | product services | product tables | 완전 연결 |
| 예·적금 개인화 | 일반 카드/목록 | 일반 list 또는 비개인화 API | 금리 정렬 | product tables | 개인화 로직 미구현 |
| 증권 성향 추천 | 홈/상품 탭 | securities recommendations/list | distance SQL | results/securities | 완전 연결 |
| 시세/차트/실시간 | detail/cards | REST+STOMP | KIS layer | DB+Redis+KIS | 완전 연결(외부환경 의존) |
| 가상계좌/자산 | start/assets | accounts/holdings | account/trade/product | holdings/account | 완전 연결 |
| 주식 주문/취소 | bottom sheet/TradeView | orders/orderable | trade engine | order/account/holding | 완전 연결 |
| 예·적금 가입/해지 | 4개 view | holdings endpoints | holding service | holdings/transactions | 부분 연결: 후속 적금 납입 없음 |
| 통합 내역 | history view | orders+product history | 두 domain service | orders/transactions | 완전 연결(Frontend 조합) |
| 퀴즈 | home card | quiz endpoints | quiz service | account/transaction+JSON | 완전 연결 |
| 리더보드 | leaderboard view | 2 endpoints | scheduler/cache/service | DB+Redis+KIS | 완전 연결 |
| 친구/알림 | views/bell/store | friend/notification endpoints | services/listeners | 3 tables | 완전 연결 |
| 공지/지원/약관 | 마이 메뉴 | 없음 | 없음 | 없음 | Frontend console 동작만 |

---

# 15. 구현 완료도

| 대분류 | 기능 | Frontend | Backend | DB | 연동 | 최종 판정 | 근거 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 인증 | JWT 전체 흐름 | O | O | O/Redis | O | 구현 완료 | Security/JWT/http/auth store |
| 회원 | 프로필 | O | O | O | O | 구현 완료 | profile view/controller/service |
| 회원 | 비밀번호 재설정 | O | X | X | X | Frontend만 구현 | timer/console only |
| 진단 | 게임 기록/최초 분류 | O | O | O | O | 구현 완료 | game+assessment flow |
| 진단 | 투자 후 동적 갱신 | 간접 | O | O | 일부 | 부분 구현 | tick matching TODO |
| 목표 | CRUD/월 참고액 | O | O | O | O | 구현 완료 | FinancialGoalService |
| 목표 | 기간 기반 상품추천 | O | O | O | 결함 | 부분 구현 | missing savingTerms→12개월 |
| 상품 | 외부 수집 | X | O | O | X | Backend만 구현 | collection controllers/services |
| 상품 | 검색/필터/상세 | O | O | O | O | 구현 완료 | ProductListPanel+Mapper |
| 추천 | 증권 성향 적합도 | O | O | O | O | 구현 완료 | SecurityMapper distance |
| 추천 | 예·적금 개인화 | 표시만 | X | O | X | 미구현 | 금리 목록만 사용 |
| 투자 | 계좌/통합자산 | O | O | O | O | 구현 완료 | AccountService/useVirtualAssets |
| 투자 | 주식 주문/취소 | O | O | O | O | 구현 완료 | order pipeline |
| 투자 | 예금 가입/해지 | O | O | O | O | 구현 완료 | ProductHoldingService |
| 투자 | 적금 가입/해지 | O | 일부 | O | 일부 | 부분 구현 | 후속 회차 납입 없음 |
| 참여 | 일일 퀴즈 | O | O | O | O | 구현 완료 | QuizService |
| 소셜 | 리더보드 | O | O | O/Redis | O | 구현 완료 | refresh/cache/service |
| 소셜 | 친구 | O | O | O | O | 구현 완료 | FriendServiceImpl |
| 소셜 | 알림 | O | O | O | O | 구현 완료 | listeners/service/store |
| 기타 | 공지/지원/약관 | 메뉴만 | X | X | X | 미구현 | MyPage console.log |
| 레거시 | JSP/security test endpoints | X | 코드 존재 | X | X | 실제 흐름 미사용 | Home/SecurityController |

## 15.1 구현 완료 기능

JWT 인증, 프로필, 게임 최초 진단, 결과/성향 목록, 목표 CRUD, 상품 탐색/상세, 증권 성향추천, KIS 시세/차트/실시간 구독, 가상계좌, 주식 주문/취소, 예금 가입/해지, 통합 거래내역, 퀴즈, 리더보드, 친구, 알림.

## 15.2 부분 구현 기능

가상투자 성향 갱신(지연 지정가 누락), 목표 기간 추천(12개월 기본값 결함), 적금 투자(후속 납입 없음), 추천 이유(동적 rule trace 없음).

## 15.3 미연동/Backend만 기능

금융감독원 수동 수집 API, `/api/products/recommendations`, `/api/products/holdings/assets`, `/api/stocks/{code}/price`, 현재 화면에서 직접 호출되지 않는 portfolio endpoint.

## 15.4 미구현 기능

비밀번호 재설정 서버 처리, 예·적금 성향 개인화, 적금 정기 후속납입, 공지/고객지원/약관 화면/API.

## 15.5 사용되지 않는 것으로 보이는 코드

레거시 JSP `HomeController`, 문자열 보안 테스트 `SecurityController`, 빈 `traqcking`, 비어 있는 `mainApi`/`invest` store, 실제 호출되지 않는 일부 API export, 실제 데이터가 없어 숨긴 `TradePortfolioImpact`.

## 15.6 실행 검증

- Frontend `npm test -- --run`: **6 files, 52 tests 통과**.
- Frontend `npm run build`: **성공**, 238 modules 변환. 500kB 초과 chunk 경고가 있으며 `index` JS와 대형 SVG 최적화 여지가 있다.
- Backend 테스트: 최초에는 Gradle hook이 sandbox git ownership 때문에 실패해 `-x installGitHooks`로 재시도했다. 이후 기존 `.gradle` cache의 `netty-transport-4.1.82.Final.jar` 접근 거부로 `compileJava` 단계가 중단됐다. 이 결과로 테스트 성공/실패를 판정할 수 없으며, Backend 테스트 상태는 **현재 환경에서 판단 불가**다. 저장소에는 성향·거래·KIS·인증·상품·목표 등을 포함한 광범위한 JUnit 테스트 코드가 존재한다.

---

# 16. 기술적으로 의미 있는 구현 포인트

## 16.1 게임 행동 기반 3축 성향 계산

**무엇을 구현했는가**: 단순 설문 대신 자산배분과 시장 행동을 RT/LH/RP로 변환한다.  
**어떻게 구현했는가**: 틱 중복 방지, 반복 상한, 행동 순서/복합 규칙, 0~100 보정과 8유형 분류를 적용한다.  
**관련 코드**: `GameBehaviorAssessmentCalculator`, `GameScoreCalculator`, `PersonaClassifier`.  
**의미**: 서비스 핵심인 금융성향을 재현 가능한 결정론적 로직으로 만든 부분이다.

## 16.2 가상투자 행동의 점진적 재평가

**무엇을 구현했는가**: 최초 진단 이후 거래와 예·적금 행동으로 성향을 갱신한다.  
**어떻게 구현했는가**: 24시간 관련행동, 시장상태, 비중/수익률/기간 조건과 90:10 EMA를 사용한다.  
**관련 코드**: `BehaviorRuleEngine`, `BehaviorContextFactory`, `VirtualInvestmentScoreCalculator`, schedulers.  
**의미**: 성향을 고정 프로필이 아니라 행동에 따라 변하는 상태로 다룬다. 단, 지정가 지연체결 누락은 보완 필요하다.

## 16.3 Refresh Token 회전과 재사용 방지

**무엇을 구현했는가**: Refresh 원문 대신 해시를 Redis에 저장하고 재발급 때 기존 token을 소비한다.  
**어떻게 구현했는가**: jti key+TTL, SHA-256, Lua GET/compare/DEL, HttpOnly Cookie, Frontend single-flight refresh.  
**관련 코드**: `RefreshTokenService`, `RedisRefreshTokenStore`, `RefreshTokenCookieManager`, Frontend `http.js`.  
**의미**: 다중 401과 Refresh 재사용 경쟁조건을 고려한 인증 구현이다.

## 16.4 KIS Token 분산 캐시/잠금

**무엇을 구현했는가**: KIS OAuth token을 여러 요청이 중복 발급하지 않도록 관리한다.  
**어떻게 구현했는가**: Redis token key, lock, double-check, 만료 여유 600초, 소유값 비교 Lua unlock, 만료 시 1회 재시도.  
**관련 코드**: `KisTokenManager`, `KisApiClient`.  
**의미**: 외부 API rate/인증 제약과 동시성을 직접 다룬다.

## 16.5 Batch 시세의 부분 실패 격리

**무엇을 구현했는가**: 최대 100개 종목 현재가를 병렬 조회한다.  
**어떻게 구현했는가**: 10초 Redis cache, ticker→KIS code 매핑, `CompletableFuture`, 항목별 성공/unsupported/error 응답.  
**관련 코드**: `SecurityQuoteService`.  
**의미**: 한 종목 오류가 전체 상품/보유 화면을 깨지 않게 한다.

## 16.6 실시간 지정가 주문 매칭과 자산 잠금

**무엇을 구현했는가**: 미체결 지정가 주문을 WebSocket tick으로 체결한다.  
**어떻게 구현했는가**: 최신 tick map drain(300ms), 주문별 `REQUIRES_NEW`, conditional status update, 현금/수량 잠금과 취소/장마감 해제.  
**관련 코드**: `TickMatchingEngine`, `OrderMatchTransactionService`, `MarketCloseScheduler`.  
**의미**: 단순 매매 CRUD가 아닌 주문 상태·동시성·자산 정합성을 구현했다.

## 16.7 우대조건 자연어의 구조화

**무엇을 구현했는가**: 금융감독원 우대조건 원문을 검색·선택 가능한 구조로 변환한다.  
**어떻게 구현했는가**: 조건 category와 옵션별 추가금리/그룹/역할을 별도 테이블에 저장한다.  
**관련 코드**: `PreferentialConditionParser`, `PreferentialRateConditionParser`, 관련 migrations.  
**의미**: 외부 원천문자열을 실제 필터와 금리 계산에 사용할 데이터로 정제한다.

## 16.8 예·적금 세후 가치/해지 계산

**무엇을 구현했는가**: 가입 전과 보유 중의 원금·이자·세금·해지 환급/포기이자를 계산한다.  
**어떻게 구현했는가**: 경과일/납입회차별 단리와 이자세 15.4%, transaction/holding/account 동시 반영.  
**관련 코드**: `ProductHoldingService`.  
**의미**: 상품 비교를 가상 자산 변화로 연결한다. 후속 적금 납입은 별도 보완 대상이다.

## 16.9 Redis 기반 실시간성 리더보드

**무엇을 구현했는가**: 현재 평가자산으로 성향/친구 순위를 제공한다.  
**어떻게 구현했는가**: 1분 refresh, KIS 현재가+세후 상품가치, 사용자 JSON cache와 persona ZSET, 동률 순위.  
**관련 코드**: `LeaderboardRefreshService`, `LeaderboardRedisService`, `LeaderboardService`.  
**의미**: 여러 자산군과 외부시세를 조회 API 앞단에서 캐시로 결합한다.

## 16.10 트랜잭션 커밋 후 이벤트 알림

**무엇을 구현했는가**: 거래 체결과 친구 상태 변화에서 사용자 알림을 만든다.  
**어떻게 구현했는가**: `AFTER_COMMIT` listener + `REQUIRES_NEW`, 사용자별 유형 설정, reference 기반 삭제/deep-link.  
**관련 코드**: `TradeNotificationListener`, `FriendNotificationListener`, `NotificationServiceImpl`.  
**의미**: 본 거래 성공과 부가 알림 실패를 분리한다.

---

# 17. 시스템 아키텍처 설명 자료

## 17.1 구성 요소

| 계층 | 구성 |
| --- | --- |
| Client | 모바일 폭 중심 브라우저 UI |
| Frontend | Vue View/Component, Router guards, Pinia, API modules, STOMP client |
| Backend | Spring MVC Controller, Service, calculator/rule engine, event listener, scheduler |
| Security | Spring Security filters, JWT, BCrypt, Refresh Cookie/Redis |
| Database | MySQL + MyBatis mapper/XML |
| Redis/Cache | Refresh token, KIS OAuth token/lock, 10초 시세, 3분 리더보드 |
| External API | 금융감독원 Finlife REST, KIS REST/WebSocket |
| 기타 인프라 | Tomcat WAR, Docker/Compose, Swagger/OpenAPI |

## 17.2 PPT용 데이터 흐름

```text
Vue → REST API → Spring Security → Controller → Service → MyBatis → MySQL
Vue ← STOMP topic ← Spring WebSocket ← KIS WebSocket
Spring Product Collection Service → 금융감독원 REST → 정제 Parser → MySQL
Spring Quote/Trade Service → Redis KIS Token/Quote Cache → KIS REST
주문/상품 거래 → Assessment Rule Engine → results → 증권 추천 SQL
MySQL 보유자산 + KIS 현재가 → Leaderboard Scheduler → Redis → Leaderboard API
로그인 → BCrypt 인증 → JWT 발급 → Refresh hash Redis / HttpOnly Cookie
```

배포 구조는 Compose 파일에 정의된 애플리케이션·MySQL·Redis 구성을 기준으로 설명할 수 있으나, 실제 운영 인스턴스 상태는 저장소 코드만으로 확인할 수 없다.

---

# 18. 최종 기획서에 활용 가능한 근거 정리

## 18.1 기획성에서 사용할 수 있는 구현 근거

- 설문이 아닌 시장 시나리오 행동으로 금융성향을 파악하고, 이후 가상투자로 성향을 갱신하는 흐름.
- 예금·적금·주식·채권형 ETF를 한 탐색/자산 경험에 묶고, 목표 금액/기간과 월 참고액을 연결한 구조.
- 상품 비교 후 가상가입/거래, 자산 변화, 성향 갱신, 리더보드로 이어지는 반복 이용 구조.

이는 코드에서 확인된 기능 연결이다. 실제 사용자 문제 해결 효과나 정확도는 사용자 조사/운영 데이터로 별도 증명해야 한다.

## 18.2 기술성에서 사용할 수 있는 구현 근거

- 규칙 기반 3축 점수·8유형 분류, 시뮬레이션/상관/도달성 테스트 코드.
- Redis 원자 소비 Refresh 회전과 KIS token 분산잠금.
- 금융감독원 원문 우대조건 구조화 및 동적 MyBatis 필터.
- KIS REST/WebSocket, batch 부분 실패, 지정가 tick matching/자산 잠금.
- 여러 자산군 평가액을 Redis ZSET 순위로 갱신하는 scheduler.

## 18.3 UI/UX에서 사용할 수 있는 구현 근거

- 인증/성향완료/게임세션 조건별 Router guard.
- 상품 탭·검색 debounce·복수 필터·정렬·pagination·URL 상태 복원.
- 로딩/빈 결과/재시도, 버튼 비활성화, 주문/가입 예상값 선확인.
- 모바일형 하단탭, 가로 추천 carousel, 실시간 차트/가격, 통합 거래내역.

## 18.4 완성도에서 사용할 수 있는 구현 근거

- 인증, 게임 진단, 상품 조회, 증권 추천, 가상 주식거래, 예금 가입/해지, 퀴즈, 친구/알림은 Frontend-Backend-DB까지 연결되어 있다.
- Frontend 자동화 테스트 52개 통과 및 production build 성공을 확인했다.
- Backend 테스트 코드는 폭넓지만 현재 실행환경의 Gradle cache ACL 때문에 실제 통과를 확인하지 못했으므로 “전체 테스트 통과”라고 발표하면 안 된다.

---

# 19. 최종 기획서 작성자가 반드시 알아야 할 주의사항

1. **예·적금 성향 추천은 없다.** 성향 적합도는 증권에만 적용된다. 홈 예·적금은 일반 최고금리 목록 첫 건이다.
2. **목표 추천은 완료로 표현하기 어렵다.** 계산된 선호기간과 달리 실제 상품 query가 기간 누락 기본값 12개월로 제한된다.
3. **적금은 후속 월 납입이 없다.** 가입 시 첫 회차만 반영되고 자동납입/추가납입 API·scheduler가 확인되지 않았다.
4. **비밀번호 재설정은 화면만 있다.** 서버 전송/인증/비밀번호 변경 없이 timer와 console만 실행된다.
5. **지연 지정가 체결은 성향 갱신에서 빠진다.** 체결/알림은 되지만 assessment event TODO가 남아 있다.
6. **추천 이유는 rule trace가 아니다.** 결과 화면 설명은 persona seed의 정적 문구이며 실제 적용 행동 규칙은 DB에 저장되지 않는다.
7. **증권 공개 matcher와 Controller 전제가 충돌한다.** 익명 목록/추천 호출 시 principal null dereference 가능성이 있다.
8. **금융상품 수집은 운영 UI/자동 schedule이 없다.** Backend 수동 endpoint만 구현됐다.
9. **스키마 자동 마이그레이션은 확인되지 않았다.** migration 파일은 있으나 Flyway runtime 의존성이 보이지 않는다.
10. **증권 추천 기준점수의 생성 출처가 없다.** 코드에는 저장/비교만 있고 종목 RT/LH/RP 산출·수집 과정은 확인되지 않는다.
11. **MyPage 일부 메뉴는 기능이 아니다.** 공지, 고객지원, 약관은 `console.log`만 수행한다.
12. **중복 거래 UI가 있다.** `TradeView.vue`와 `TradeBottomSheet.vue`가 유사 주문 흐름을 각각 구현해 유지보수 차이가 생길 수 있다.
13. **포트폴리오 영향 UI는 숨김 상태다.** `TradePortfolioImpact` 컴포넌트는 있으나 실제 데이터 미구현 주석과 함께 비노출된다.
14. **외부 연동 시연은 환경 의존이다.** KIS/금융감독원 key, Redis/MySQL, 장 운영시간과 데이터 seed가 필요하다.
15. **리더보드 현재가 누락 영향이 있다.** 조회할 수 없는 주식은 계산에서 제외될 수 있어 평가액이 낮아질 수 있다.
16. **Backend 테스트 통과는 확인되지 않았다.** 테스트 소스의 존재와 실행 통과는 구분해야 한다.
17. **Frontend bundle 경고가 있다.** production build는 성공했지만 500kB 초과 chunk 경고가 있다.
18. **레거시 endpoint를 서비스 기능으로 세면 안 된다.** JSP home과 `/api/security/*` 문자열 endpoint는 Vue 실제 흐름과 무관하다.
19. **친구 수락 알림의 유형값이 잘못 지정됐다.** `FriendNotificationListener.onFriendRequestAccepted()`도 `FRIEND_REQUEST_ACCEPTED`가 아니라 `FRIEND_REQUEST_RECEIVED`로 저장해, Frontend의 수락 유형 분기와 일치하지 않는다.

시연 전 최소 점검 대상은 목표 추천기간, 지연 지정가 성향 갱신, 적금 후속납입, 익명 증권 API, Backend clean test, 운영 DB migration/seed다.

---

# 20. 최종 요약

## A. 프로젝트 핵심 구현 기능 10개

1. JWT 회원가입·로그인·자동 재발급·로그아웃.
2. 시나리오 게임 행동 저장과 RT/LH/RP 성향 진단.
3. 8개 금융성향과 자산배분/이유 결과 화면.
4. 가상투자 행동 기반 성향 점진 갱신.
5. 금융감독원 예·적금 데이터 수집·우대조건 구조화.
6. 예금·적금·증권 통합 검색/필터/정렬/상세.
7. 증권 3축 적합도 기반 추천.
8. KIS 시세·차트·WebSocket과 시장가/지정가 가상거래.
9. 예·적금 가상가입·세후 평가·중도해지와 통합 자산/내역.
10. 금융 퀴즈, 친구/알림, 성향/친구 리더보드.

## B. 기술적으로 가장 강조할 만한 구현 5개

1. 반복/복합행동 보정과 시뮬레이션 테스트를 포함한 3축 성향 규칙 엔진.
2. Redis hash·Lua 원자 소비를 이용한 Refresh Token 회전.
3. KIS token 분산잠금, batch 부분 실패, REST/WebSocket 통합.
4. 자산 잠금·조건부 상태전이·300ms tick matching을 갖춘 지정가 주문 엔진.
5. 금융감독원 우대조건 구조화와 가입/해지 세후 금융 계산.

## C. 최종 기획서 작성 시 가장 주의해야 할 사항 5개

1. 예·적금은 성향 개인화 추천이 아니라 금리/필터 기반이다.
2. 목표 추천기간은 현재 상품 query에 정확히 적용되지 않는다.
3. 적금 자동/추가 납입과 비밀번호 재설정 Backend는 미구현이다.
4. 지연 지정가 체결은 성향 갱신 경로가 누락됐다.
5. Backend 전체 테스트 통과와 DB migration 자동 적용은 현재 근거로 주장할 수 없다.

---

## 주요 근거 파일 색인

- Backend 구성/보안: `build.gradle`, `src/main/java/org/kkobi/config/*`, `src/main/java/org/kkobi/security/*`
- Controller/API: `src/main/java/org/kkobi/**/controller/*Controller.java`
- 성향: `src/main/java/org/kkobi/assessment/calculator/*`, `assessment/service/*`, `src/main/resources/db/migration/R__seed_personas.sql`
- 상품: `src/main/java/org/kkobi/product/*`, `src/main/resources/mappers/*Product*.xml`
- KIS/거래: `src/main/java/org/kkobi/external/kis/*`, `src/main/java/org/kkobi/trade/*`
- DB: `src/main/resources/db/migration/*`, `src/main/resources/mappers/*`
- Frontend Route/화면: Frontend `src/router/index.js`, `src/views/*.vue`
- Frontend 연동: Frontend `src/api/*.js`, `src/stores/*.js`, `src/composables/*.js`
- 테스트: Backend `src/test/java/org/kkobi/**`, Frontend `src/**/*.test.js`
