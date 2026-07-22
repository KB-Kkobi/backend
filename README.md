# member-service

## 브랜치 전략 (Branch Strategy)

- `main` : 운영 브랜치. 항상 배포 가능한 상태를 유지합니다. 직접 push 금지, PR을 통해서만 병합합니다. 해당 브랜치는 최종적으로 기능을
완성 후에 병합합니다.
- `develop` : 개발 통합 브랜치. 기능 브랜치들이 병합되는 기준 브랜치입니다. 대부분 여기서 분기 후 기능별 구현 후 다시 이곳에 병합
- `feature/{도메인}-{작업내용}` : 기능 개발 브랜치. `develop`에서 분기하여 작업 후 `develop`으로 PR을 보냅니다.
  - 예: `feature/member-login`, `feature/game-event-generation`, `feature/product-recommendation`
- `fix/{도메인}-{작업내용}` : 버그 수정 브랜치. 기능을 구현하다가 문제가 발생할 경우 `feature/{도메인}-{작업내용}`에서 분기 후 디버깅
  - 예: `fix/assessment-score-calculation`
- `refactor/{도메인}-{작업내용}`, `chore/{작업내용}`, `docs/{작업내용}` : 리팩터링, 빌드/설정, 문서 작업 브랜치.

도메인 접두어는 프로젝트 패키지 구조를 따릅니다: `member`, `assessment`, `game`, `backtest`, `product`, `leaderboard`, `tracking`, `security`, `common`.

작업이 끝난 브랜치는 병합 후 삭제합니다.

## 이슈 전략 (Issue Strategy)

- 모든 작업은 이슈 등록 후 진행하는 것을 원칙으로 합니다.
- 이슈 제목: `{Type} : {작업 내용 요약}` (예: `Feat : 게임 이벤트 생성 API 구현`)
- 이슈 본문에는 작업 배경, 목표, 체크리스트(TODO)를 포함합니다.
- 담당 도메인에 맞는 라벨(`member`, `assessment`, `game`, `backtest`, `product`, `leaderboard`, `tracking`, `bug`, `docs` 등)을 지정합니다.
- 브랜치명과 커밋 메시지에는 관련 이슈 번호를 연결합니다.
- PR 생성 시 `Closes #이슈번호`를 본문에 작성하여, 병합 시 이슈가 자동으로 닫히도록 합니다.

1. 이슈 등록
```
이슈 제목:
Feat : 회원가입 시 이메일 중복 검증 로직 구현
라벨: member
```
2. 이슈 본문:
```
## 배경 (Background)
현재 회원가입 API는 이메일 중복 여부를 검증하지 않아,
동일한 이메일로 여러 계정이 생성되는 문제가 발생하고 있습니다.

## 목표 (Goal)
- 회원가입 시 이메일 중복 여부를 사전에 검증
- 중복된 이메일로 가입 시도 시 명확한 에러 메시지 반환

## 작업 내용 (TODO)
- [ ] `MemberRepository`에 이메일 존재 여부 조회 메서드 추가
- [ ] 회원가입 서비스 로직에 중복 검증 단계 추가
- [ ] 중복 시 발생시킬 커스텀 예외(`DuplicateEmailException`) 정의
- [ ] 예외 핸들러에 에러 응답 매핑 추가
- [ ] 단위 테스트 작성 (중복/정상 케이스)

## 참고 사항
- 관련 이슈: 없음
```

## 커밋 전략 (Commit Strategy)

커밋 메시지는 다음 형식을 따릅니다.

```
#{이슈번호} {Type} : {작업 내용}
```

### Type 종류

| Type | 설명 |
|---|---|
| Feat | 새로운 기능 추가 |
| Fix | 버그 수정 |
| Refactor | 기능 변화 없는 코드 개선 |
| Design | CSS 등 UI/디자인 변경 |
| Style | 코드 포맷팅, 세미콜론 등 기능에 영향 없는 변경 |
| Docs | 문서 추가/수정 |
| Test | 테스트 코드 추가/수정 |
| Chore | 빌드/설정 파일, 패키지 매니저 등 |
| Comment | 주석 추가/수정 |
| Rename | 파일/폴더명 변경 또는 이동 |
| Remove | 파일 삭제 |

### 규칙

- 커밋은 하나의 논리적 작업 단위로 작게 나눠서 진행합니다.
- 메시지는 한글로 작성하며, 무엇을 했는지 명확히 서술합니다.
- 커밋 메시지 첫 줄 맨 앞에 관련 이슈 번호를 `#{이슈번호}` 형식으로 남깁니다.

예:
```
#1 Feat : 회원가입 시 이메일 중복 검증 로직 추가
- DB 쿼리문을 통해...
- 여러분 안녕...
```

### 강제 적용 (Enforcement)

형식에 맞지 않는 커밋은 아래 두 단계로 차단됩니다.

1. **로컬 훅 (`commit-msg`)** — 저장소 클론 후 최초 1회 아래 명령으로 훅 경로를 등록하면, 형식에 맞지 않는 커밋은 로컬에서 즉시 거부됩니다.
   ```
   git config core.hooksPath .githooks
   ```
   훅 스크립트: [`/.githooks/commit-msg`](.githooks/commit-msg)

2. **CI 검사 (`.github/workflows/commit-lint.yml`)** — `develop`, `main`으로의 PR에서 포함된 모든 커밋 메시지를 검사합니다. 로컬 훅을 건너뛰거나 삭제해도 이 단계에서 최종적으로 걸러집니다.
   GitHub 저장소 설정에서 `develop`, `main` 브랜치 보호 규칙(Branch protection rule)에 이 워크플로우(`Commit Message Lint`)를 **필수 상태 검사(Required status check)**로 지정해야 실제로 병합이 막힙니다. (Settings → Branches → Branch protection rules → Require status checks to pass before merging)


