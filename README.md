# KB 프로젝트 - 꼬비

## 프로젝트 소개

꼬비는 사용자의 금융 성향을 분석하고 맞춤형 금융 정보를 제공하는 KB 금융 프로젝트입니다.

이 저장소는 Spring Framework 기반의 백엔드 프로젝트로,
Spring MVC, Spring Security, JWT, MyBatis를 사용합니다.

애플리케이션은 WAR 파일로 빌드하며 외부 Tomcat에 배포해 실행합니다.

## 기술 환경

| 구분 | 버전 또는 구성 |
| --- | --- |
| Java | Eclipse Temurin OpenJDK 17 |
| 프레임워크 | Spring Framework 5.3.37 |
| 보안 | Spring Security 5.8.13 |
| JWT | JJWT 0.11.5 |
| 빌드 | Gradle Wrapper 8.8 |
| 데이터 접근 | MyBatis 3.5.13, MyBatis-Spring 2.1.1 |
| 데이터베이스 | MySQL Server 8.0.46 |
| JDBC 드라이버 | MySQL Connector/J 8.1.0 |
| Connection Pool | HikariCP 4.0.3 |
| JSON 처리 | Jackson Databind 2.13.5 |
| 서블릿 컨테이너 | Apache Tomcat 9.0.120 |
| Redis | Docker `redis:7` |
| DB Migration | Flyway 12.10.0 |
| 컨테이너 환경 | Docker, Docker Compose |

Java 빌드에는 시스템에 별도로 설치한 Gradle 대신
저장소의 Gradle Wrapper를 사용합니다.

## 로컬 실행 방법

### 데이터베이스 설정

로컬 개발에서는 Docker MySQL을 사용합니다.

MySQL은 호스트의 `3307` 포트를 컨테이너의 `3306` 포트에 연결하며,
IntelliJ Tomcat 등 호스트 환경에서 실행되는 백엔드는
`localhost:3307`을 통해 MySQL에 연결합니다.

| 환경 변수 | 기본값 또는 설명 |
| --- | --- |
| `JDBC_DRIVER` | `net.sf.log4jdbc.sql.jdbcapi.DriverSpy` |
| `JDBC_URL` | `jdbc:log4jdbc:mysql://localhost:3307/kkobi?allowPublicKeyRetrieval=true&sslMode=DISABLED` |
| `JDBC_USERNAME` | `kkobi` |
| `JDBC_PASSWORD` | 필수 설정 |
| `FINLIFE_API_KEY` | 금융감독원 금융상품 API Key |

비밀번호와 API Key 등의 민감한 값은 저장소에 커밋하지 않습니다.

IntelliJ Tomcat으로 실행하는 경우 필요한 값은
Run/Debug Configuration의 환경 변수에 설정합니다.

### 빌드 및 테스트

Windows PowerShell:

```powershell
.\gradlew.bat clean war
.\gradlew.bat test
```

Git Bash / WSL / Linux:

```sh
./gradlew clean war
./gradlew test
```

WAR 파일은 다음 경로에 생성됩니다.

```text
build/libs/backend-1.0-SNAPSHOT.war
```

로컬 서버 실행 시 IntelliJ의 Tomcat 설정을 사용하거나
생성된 WAR 파일을 외부 Tomcat에 배포합니다.

## 백엔드 Docker 실행

프로젝트 루트의 `.env.example`을 참고해 `.env` 파일을 생성하고
필요한 환경변수를 설정합니다.

Docker Compose에서는 다음 서비스를 실행합니다.

| 서비스 | 역할 | 호스트 포트 |
| --- | --- | --- |
| Backend | Tomcat + Spring WAR 실행 | `8080` |
| MySQL | 애플리케이션 데이터베이스 | `3307` |
| Redis | Redis 서버 | `6379` |
| Flyway | DB 마이그레이션 | 포트 없음 |

### 전체 실행

```sh
docker compose up -d --build
```

### 백엔드 다시 빌드 및 실행

```sh
docker compose up -d --build backend
```

### 실행 상태 확인

```sh
docker compose ps -a
```

### 백엔드 로그 확인

```sh
docker compose logs backend --tail=100
```

### 전체 종료

```sh
docker compose down
```

### 백엔드만 종료

```sh
docker compose stop backend
```

로컬 개발을 IntelliJ Tomcat으로 진행할 경우
Docker Backend만 중지하고 MySQL과 Redis는 계속 사용할 수 있습니다.

## 프로젝트 구조

```text
backend/
├── .githooks/
│   └── commit-msg                 # 커밋 메시지 검사 Hook
├── src/
│   ├── main/
│   │   ├── java/org/kkobi/
│   │   │   ├── config/            # Spring 설정
│   │   │   ├── controller/        # 요청 처리 Controller
│   │   │   ├── exception/         # 공통 예외 처리
│   │   │   └── security/          # Spring Security 및 JWT 인증
│   │   ├── resources/
│   │   │   ├── db/
│   │   │   │   └── migration/     # Flyway 마이그레이션 SQL
│   │   │   └── mapper/            # MyBatis Mapper XML
│   │   └── webapp/                # 웹 애플리케이션 리소스
│   └── test/                       # 테스트 코드
├── .dockerignore
├── .env.example
├── build.gradle
├── compose.yaml
├── Dockerfile
├── gradlew
├── gradlew.bat
└── README.md
```

기능 개발에 따라 `member`, `assessment`, `game`, `backtest`, `product`,
`leaderboard`, `tracking`, `external` 등의 도메인 패키지를 사용합니다.

## Git Hook 설정

커밋 메시지 검증을 위해 `.githooks/commit-msg` Hook을 사용합니다.

저장소 루트에서 다음 명령을 실행합니다.

```sh
git config core.hooksPath .githooks
```

현재 설정은 다음 명령으로 확인할 수 있습니다.

```sh
git config core.hooksPath
```

커밋 메시지는 다음 형식을 사용합니다.

```text
#{이슈번호} {Type} : {작업 내용}
```

예:

```text
#3 Chore : 백엔드 Docker 실행 환경 구성
```

## 협업 규칙

### 브랜치 규칙

- `main`
  - 운영 브랜치
  - 항상 배포 가능한 상태 유지
  - 직접 push하지 않고 PR을 통해 병합

- `develop`
  - 개발 통합 브랜치
  - 작업 브랜치는 `develop`에서 분기
  - 작업 완료 후 `develop`으로 PR 생성

- `feature/{도메인}-{작업내용}`
  - 새로운 기능 개발
  - 예: `feature/member-login`
  - 예: `feature/game-event-generation`

- `fix/{도메인}-{작업내용}`
  - 버그 수정
  - 예: `fix/assessment-score-calculation`

- `refactor/{도메인}-{작업내용}`
  - 기능 변화가 없는 코드 개선

- `chore/{작업내용}`
  - 빌드 및 설정 변경

- `docs/{작업내용}`
  - 문서 추가 또는 수정

도메인 접두어는 프로젝트 패키지 구조에 맞춰 다음과 같이 사용합니다.

```text
member
assessment
game
backtest
product
leaderboard
tracking
security
common
```

병합이 끝난 작업 브랜치는 삭제합니다.

### 이슈 규칙

- 모든 작업은 이슈를 등록한 뒤 진행합니다.
- 이슈 제목은 `{Type} : {작업 내용 요약}` 형식으로 작성합니다.
- 이슈 본문에는 작업 배경, 목표, 작업 체크리스트를 포함합니다.
- 담당 도메인에 맞는 라벨을 지정합니다.
- PR 본문에 `Closes #이슈번호`를 작성해 병합 시 관련 이슈가 닫히도록 합니다.

이슈 작성 예시:

```markdown
제목: Feat : 회원가입 시 이메일 중복 검증 로직 구현
라벨: member

## 배경

현재 회원가입 API는 이메일 중복 여부를 검증하지 않아 동일한 이메일로
여러 계정이 생성될 수 있습니다.

## 목표

- 회원가입 전에 이메일 중복 여부 검증
- 중복 가입 시 명확한 오류 메시지 반환

## 작업 내용

- [ ] 이메일 존재 여부 조회 메서드 추가
- [ ] 회원가입 서비스에 중복 검증 추가
- [ ] 중복 이메일 예외 및 오류 응답 추가
- [ ] 정상·중복 사례 테스트 작성
```

### 커밋 규칙

| Type | 설명 |
| --- | --- |
| `Feat` | 새로운 기능 추가 |
| `Fix` | 버그 수정 |
| `Refactor` | 기능 변화가 없는 코드 개선 |
| `Design` | CSS 등 UI 또는 디자인 변경 |
| `Style` | 포맷팅 등 기능에 영향이 없는 변경 |
| `Docs` | 문서 추가 또는 수정 |
| `Test` | 테스트 코드 추가 또는 수정 |
| `Chore` | 빌드, 설정, 패키지 관리 변경 |
| `Comment` | 주석 추가 또는 수정 |
| `Rename` | 파일이나 디렉터리 이름 변경 또는 이동 |
| `Remove` | 파일 삭제 |

- 커밋은 하나의 논리적 작업 단위로 작게 나눕니다.
- 메시지는 한글로 작성하고 무엇을 변경했는지 명확하게 표현합니다.
- 첫 줄 맨 앞에 관련 이슈 번호를 `#{이슈번호}` 형식으로 작성합니다.

```text
#1 Feat : 회원가입 시 이메일 중복 검증 로직 추가
```