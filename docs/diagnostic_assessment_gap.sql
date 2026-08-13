-- 체결됐지만 성향 평가 기록이 없을 가능성이 있는 주문 조회 (사용자 단위 근사)
-- 사용 목적: SecurityOrderAssessmentListener 장애 가시성 점검
--
-- [한계 — 반드시 숙지]
--   results 테이블에 주문 단위 식별자(reference_type / reference_id)가 없어
--   "어떤 주문에 대한 채점인지" 를 주문 단위로 대조할 수 없다.
--   대신, 최근 24시간 내 FILLED 주문이 있는 사용자 중
--   그 주문 시각 이후로 results 가 신규 생성된 기록이 없는 경우를 조회한다.
--
-- [FP 주의]
--   규칙 미발화(정상 경로)도 이 결과에 포함된다.
--   예: VOLATILE 시장 첫 매수처럼 어떤 규칙도 매칭되지 않으면
--       리스너가 정상 실행됐더라도 results 가 저장되지 않는다.
--   WARN 로그(securityOrderId, userId, error)와 대조해 구분해야 한다.
--
-- [조회 범위 조정] executed_at 조건의 INTERVAL 값을 변경한다.

SELECT
    so.security_order_id,
    so.account_id,
    a.user_id,
    so.order_type,
    so.order_method,
    so.quantity,
    so.executed_price,
    so.executed_at
FROM security_orders so
INNER JOIN accounts a ON a.account_id = so.account_id
WHERE so.status       = 'FILLED'
  AND so.order_method IN ('MARKET', 'LIMIT')
  AND so.executed_at  >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
  AND NOT EXISTS (
      SELECT 1
      FROM results r
      WHERE r.user_id    = a.user_id
        AND r.created_at >= so.executed_at
  )
ORDER BY so.executed_at DESC;
