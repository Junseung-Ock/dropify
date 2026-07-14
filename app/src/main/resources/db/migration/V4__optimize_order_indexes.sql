-- ──────────────────────────────────────────────────────────────────────────────
-- orders 인덱스 최적화
--
-- 기존 단일 컬럼 인덱스 문제:
--   findByUserIdOrderByCreatedAtDesc → WHERE user_id = ? ORDER BY created_at DESC
--   idx_orders_user_id 단독 사용 시 created_at 정렬을 위한 filesort 발생
--
-- 개선:
--   (user_id, created_at DESC) 복합 인덱스로 filesort 제거
--   기존 idx_orders_user_id, idx_orders_created_at 단일 인덱스는 제거
-- ──────────────────────────────────────────────────────────────────────────────

-- 복합 인덱스를 먼저 생성해야 FK 제약 조건 위반 없이 기존 인덱스를 삭제할 수 있다.
-- (user_id, created_at DESC)의 leftmost prefix가 기존 idx_orders_user_id의 FK 역할을 대체한다.
CREATE INDEX idx_orders_user_id_created_at ON orders (user_id, created_at DESC);

DROP INDEX idx_orders_user_id    ON orders;
DROP INDEX idx_orders_created_at ON orders;
