-- ────────────────────────────────────────────────────────────────────────────
-- orders 테이블 쿼리 튜닝 — EXPLAIN ANALYZE
--
-- 실행 방법:
--   docker exec -it dropify-mysql mysql -u<user> -p<password> dropify
--   그 다음 아래 쿼리를 순서대로 실행
--
-- 확인 포인트:
--   V4 마이그레이션(복합 인덱스) 적용 전: "Sort: orders.created_at DESC" 라인이 보임 (filesort)
--   V4 마이그레이션 적용 후:              Sort 라인 없음, rows 수 감소
-- ────────────────────────────────────────────────────────────────────────────

USE dropify;

-- ① 현재 인덱스 목록 확인
SHOW INDEX FROM orders;

-- ② 주문 목록 조회 쿼리 실행계획
--    → findByUserIdOrderByCreatedAtDesc(userId, pageable)가 실행하는 쿼리
--    확인 포인트: "using filesort" 여부, rows 추정치
EXPLAIN ANALYZE
SELECT *
FROM orders
WHERE user_id = 1
ORDER BY created_at DESC
LIMIT 10 OFFSET 0;

-- ③ 주문 단건 조회 쿼리 실행계획
--    → findByIdAndUserId(orderId, userId)가 실행하는 쿼리
--    PK(id) 단독 조회 후 user_id 필터 — 이미 최적
EXPLAIN ANALYZE
SELECT *
FROM orders
WHERE id = 1
  AND user_id = 1;

-- ④ (선택) 상태별 주문 조회 — 관리자 쿼리 시뮬레이션
--    idx_orders_status 인덱스 사용 여부 확인
EXPLAIN ANALYZE
SELECT *
FROM orders
WHERE status = 'PENDING'
ORDER BY created_at DESC
LIMIT 20;

-- ────────────────────────────────────────────────────────────────────────────
-- slow query log 현황 확인
-- ────────────────────────────────────────────────────────────────────────────
SHOW VARIABLES LIKE 'slow_query_log%';
SHOW VARIABLES LIKE 'long_query_time';
SHOW VARIABLES LIKE 'log_queries_not_using_indexes';
