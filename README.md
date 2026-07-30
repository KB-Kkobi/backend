# KB 프로젝트 - 꼬비

## 프로젝트 소개

꼬비는 사용자의 금융 성향을 분석하고 맞춤형 금융 정보를 제공하는 KB 금융
프로젝트입니다. 이 저장소는 Spring Framework 기반의 백엔드로, Spring MVC,
Spring Security, JWT, MyBatis를 사용합니다.

애플리케이션은 WAR 파일로 빌드하며 외부 Tomcat에 배포해 실행합니다.

## 기술 환경

| 구분            | 버전 또는 구성                       |
| --------------- | ------------------------------------ |
| 운영체제        | 버전 고정 없음                       |
| Java            | Eclipse Temurin OpenJDK 17           |
| 프레임워크      | Spring Framework 5.3.37              |
| 보안            | Spring Security 5.8.13, JWT 0.11.5   |
| 빌드            | 프로젝트 Gradle Wrapper 8.8          |
| 데이터 접근     | MyBatis 3.5.13, MyBatis-Spring 2.1.1 |
| 데이터베이스    | MySQL Server 8.0.46                  |
| JDBC 드라이버   | MySQL Connector/J 8.1.0              |
| 서블릿 컨테이너 | 외부 Tomcat 9.0.120, Temurin 17      |
| 캐시            | Docker `redis:7`                     |
| 배포 환경       | Docker, Nginx, EC2                   |

Java 빌드에는 시스템에 별도로 설치한 Gradle 대신 저장소의 Gradle Wrapper를
사용합니다.

## 로컬 실행 방법

### 데이터베이스 설정

MySQL Server 8.0.46을 준비한 뒤 필요에 따라 다음 환경 변수를 설정합니다.
환경 변수를 지정하지 않으면 표의 기본값을 사용합니다.

| 환경 변수       | 기본값                                           |
| --------------- | ------------------------------------------------ |
| `JDBC_DRIVER`   | `net.sf.log4jdbc.sql.jdbcapi.DriverSpy`          |
| `JDBC_URL`      | `jdbc:log4jdbc:mysql://localhost:3306/scoula_db` |
| `JDBC_USERNAME` | `scoula`                                         |
| `JDBC_PASSWORD` | `1234`                                           |

공용 또는 운영 환경에서는 기본 비밀번호를 사용하지 말고 환경 변수나 배포
환경의 Secret으로 주입합니다. 데이터베이스 연결 테스트를 실행하려면 해당
데이터베이스와 계정이 먼저 준비되어 있어야 합니다.

### 빌드 및 테스트

Windows PowerShell:

```powershell
.\gradlew.bat clean war
.\gradlew.bat test
```

WSL 또는 Linux:

```sh
./gradlew clean war
./gradlew test
```

WAR 파일은 `build/libs/backend-1.0-SNAPSHOT.war`에 생성됩니다. 로컬 서버를
실행하려면 생성된 WAR 파일을 Tomcat의 `webapps` 디렉터리에 배포합니다.

## Docker 통합 실행

백엔드와 프론트엔드의 상위 디렉터리인 `C:\KB`에서 `compose.yaml`을
사용합니다. `.env.example`을 `.env`로 복사하고 예시 비밀번호를 변경한 뒤
실행합니다.

Windows PowerShell:

```powershell
Set-Location C:\KB
Copy-Item .env.example .env
docker compose up --build
```

WSL 또는 Linux:

```sh
cd /path/to/KB
cp .env.example .env
docker compose up --build
```

기본 서비스 포트는 다음과 같습니다. `.env`의 포트 값을 수정하면 호스트
포트를 변경할 수 있습니다.

| 서비스        | 기본 포트 |
| ------------- | --------- |
| 프론트엔드    | `80`      |
| 백엔드 Tomcat | `8080`    |
| MySQL         | `3306`    |
| Redis         | `6379`    |

컨테이너를 종료하려면 통합 프로젝트 루트에서 다음 명령을 실행합니다.

```sh
docker compose down
```

## 프로젝트 구조

```text
backend/
├── .githooks/
│   └── commit-msg                 # 커밋 메시지 검사 Hook
├── src/
│   ├── main/
│   │   ├── java/org/kkobi/
│   │   │   ├── config/            # Spring MVC 및 루트 설정
│   │   │   ├── controller/        # 요청 처리 컨트롤러
│   │   │   ├── exception/         # 공통 예외 처리
│   │   │   └── security/          # Spring Security 및 JWT 인증
│   │   ├── resources/             # 설정, MyBatis Mapper, 로그 설정
│   │   └── webapp/                # JSP 및 웹 애플리케이션 리소스
│   └── test/                       # 테스트 코드
├── build.gradle                    # 의존성, Java Toolchain, WAR 빌드 설정
├── Dockerfile                      # Gradle 빌드 및 Tomcat 배포 이미지
├── gradlew
└── gradlew.bat
```

기능 개발에 따라 `member`, `assessment`, `game`, `backtest`, `product`,
`leaderboard`, `tracking`, `external` 등의 도메인 패키지를 추가합니다.

## Git Hook 설정

저장소를 처음 받은 뒤 저장소 루트에서 다음 명령을 한 번 실행합니다.

```sh
git config core.hooksPath .githooks
```

커밋 메시지는 다음 형식을 사용합니다.

```text
#{이슈번호} {Type} : {작업 내용}
```

## 협업 규칙

### 브랜치 규칙

- `main`: 운영 브랜치입니다. 항상 배포 가능한 상태를 유지하며 직접 push하지
  않고 PR을 통해 병합합니다.
- `develop`: 개발 통합 브랜치입니다. 기능 브랜치는 이 브랜치에서 분기하고
  작업 완료 후 이 브랜치로 PR을 보냅니다.
- `feature/{도메인}-{작업내용}`: 기능 개발 브랜치입니다.
  - 예: `feature/member-login`, `feature/game-event-generation`
- `fix/{도메인}-{작업내용}`: 버그 수정 브랜치입니다.
  - 예: `fix/assessment-score-calculation`
- `refactor/{도메인}-{작업내용}`: 기능 변화가 없는 리팩터링 브랜치입니다.
- `chore/{작업내용}`: 빌드 및 설정 변경 브랜치입니다.
- `docs/{작업내용}`: 문서 변경 브랜치입니다.

도메인 접두어는 프로젝트 패키지 구조에 맞춰 `member`, `assessment`, `game`,
`backtest`, `product`, `leaderboard`, `tracking`, `security`, `common` 등을
사용합니다. 병합이 끝난 작업 브랜치는 삭제합니다.

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

| Type       | 설명                                  |
| ---------- | ------------------------------------- |
| `Feat`     | 새로운 기능 추가                      |
| `Fix`      | 버그 수정                             |
| `Refactor` | 기능 변화가 없는 코드 개선            |
| `Design`   | CSS 등 UI 또는 디자인 변경            |
| `Style`    | 포맷팅 등 기능에 영향이 없는 변경     |
| `Docs`     | 문서 추가 또는 수정                   |
| `Test`     | 테스트 코드 추가 또는 수정            |
| `Chore`    | 빌드, 설정, 패키지 관리 변경          |
| `Comment`  | 주석 추가 또는 수정                   |
| `Rename`   | 파일이나 디렉터리 이름 변경 또는 이동 |
| `Remove`   | 파일 삭제                             |

- 커밋은 하나의 논리적 작업 단위로 작게 나눕니다.
- 메시지는 한글로 작성하고 무엇을 변경했는지 명확하게 표현합니다.
- 첫 줄 맨 앞에 관련 이슈 번호를 `#{이슈번호}` 형식으로 작성합니다.

```text
#1 Feat : 회원가입 시 이메일 중복 검증 로직 추가
```
