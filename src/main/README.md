## 2. 메인 서버 — Spring Framework

main/
├── pom.xml                        # spring-webmvc, spring-jdbc/tx, mybatis, mybatis-spring,
│                                  # jackson-databind, jjwt, hikaricp, log4j2 ...
├── src/
│   ├── main/
│   │   ├── java/com/kb/fintech/
│   │   │   ├── config/         
│   │   │   │   ├── RootConfig.java       
│   │   │   │   ├── ServletConfig.java    
│   │   │   │   └── WebInitializer.java
│   │   │   ├── member/            # 회원 관리 (가입/로그인/JWT 발급)
│   │   │   │   ├── controller/
│   │   │   │   ├── service/
│   │   │   │   ├── mapper/       
│   │   │   │   └── dto/
│   │   │   ├── assessment/           # 성향&심리 분석 (지표 연산, 성향 판정/코드, 리포트)
│   │   │   │   ├── controller/
│   │   │   │   ├── service/
│   │   │   │   ├── mapper/
│   │   │   │   └── dto/
│   │   │   ├── game/              # 성향 파악 게임 (턴/이벤트, 성향점수 산출)
│   │   │   │   ├── controller/
│   │   │   │   ├── service/
│   │   │   │   ├── mapper/
│   │   │   │   ├── dto/
│   │   │   │   └── ai/            # Gemini 이벤트 생성/검증/익명화
│   │   │   ├── backtest/          # 백테스팅·위험도(MDD) (ECOS/지수 시계열)
│   │   │   │   ├── controller/
│   │   │   │   ├── service/
│   │   │   │   ├── mapper/
│   │   │   │   └── dto/
│   │   │   ├── product/           # 상품추천 (필터룰, 위험등급 산출, 소셜프루프)
│   │   │   │   ├── controller/
│   │   │   │   ├── service/
│   │   │   │   ├── mapper/
│   │   │   │   └── dto/
│   │   │   ├── leaderboard/       # 리더보드 (그룹핑, 랭킹, 일치도)
│   │   │   │   ├── controller/
│   │   │   │   ├── service/
│   │   │   │   ├── mapper/
│   │   │   │   └── dto/
│   │   │   ├── tracking/          # 데일리 트래킹 (조회/시각화, 변동경고 알림)
│   │   │   │   ├── controller/
│   │   │   │   ├── service/
│   │   │   │   ├── mapper/
│   │   │   │   └── dto/
│   │   │   ├── external/          # 외부 API 클라이언트
│   │   │   │   ├── FinlifeClient      # 예적금
│   │   │   │   ├── EcosClient         # 국고채/예금금리
│   │   │   │   ├── DataGoClient       # 지수/ETF/주식 시세
│   │   │   │   ├── GeminiClient
│   │   │   │   └── FcmClient          # 푸시알림
│   │   │   ├── security/          # JwtProvider, JwtFilter, 인증
│   │   │   └── common/            # 공통 응답/예외/유틸, CORS
│   │   ├── resources/
│   │   │   ├── mappers/           # *.xml (MyBatis SQL 쿼리)
│   │   │   ├── db.properties      # DB 접속 정보 등
│   │   │   └── log4j2.xml
│   │   └── webapp/               # 정적 리소스용(옵션)
│   └── test/
└── (build → main.war)