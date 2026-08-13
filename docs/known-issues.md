# Known Issues

## 1. results 이중 채점 방지 수단 부재

- **현상**: `results` 테이블에 주문 단위 식별자(`reference_type` / `reference_id`) 컬럼이
  없고, `(user_id, ...)` UNIQUE 제약도 없다.
  동일 사용자·동일 주문에 대해 리스너가 두 번 발화하면 `results` 행이 중복 삽입된다.
- **영향**: 재시도 시나리오(서버 재기동, 이벤트 재발행)에서 이중 채점 발생 가능.
- **대응 방향**: `results`에 `reference_type`, `reference_id` 컬럼을 추가하고
  `(reference_type, reference_id)` UNIQUE 인덱스 또는 INSERT 전 중복 체크 로직 추가.

## 2. VOLATILE 시장 첫 매수에 매칭되는 규칙 없음 (설계 확인 필요)

- **현상**: `MarketStateCalculator`는 VOLATILE(`dailyPriceRangeRate ≥ 5%`) 조건을
  CRASH(`currentPriceChangeRate ≤ -5%`)보다 먼저 검사한다.
  변동폭 5% 이상인 날에는 등락률이 -6% 이더라도 `MarketState.VOLATILE`로 분류된다.
  VOLATILE + BUY에 해당하는 별도 규칙이 없고, 첫 매수라 이전 거래 이력도 없으면
  어떤 규칙도 발화하지 않아 `results` 행이 저장되지 않는다.
- **영향**: 변동성이 큰 날의 첫 매수는 성향 점수 변화가 없다.
  `diagnostic_assessment_gap.sql` 조회 결과에 FP로 나타난다.
- **대응 방향**: VOLATILE_BUY 규칙 추가 또는 MarketState 우선순위 재검토. 설계 확인 필요.

## 3. 경로 B(틱 체결) SecurityOrderAssessmentListener 미연결

- **현상**: `OrderService.placeOrder()`가 즉시 체결하는 경로
  (MARKET 주문, 즉시 체결되는 LIMIT 주문)에서만 `SecurityOrderFilledEvent`를 발행한다.
  틱 매칭 엔진(경로 B)이 나중에 체결하는 PENDING LIMIT 주문은 이벤트를 발행하지 않아
  리스너가 발화하지 않는다.
- **영향**: PENDING → FILLED 전환 시 성향 채점이 누락된다.
- **대응 방향**: 틱 매칭 엔진의 체결 완료 시점에 동일 이벤트(`SecurityOrderFilledEvent`) 발행 추가.
