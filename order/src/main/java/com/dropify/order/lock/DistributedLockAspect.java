package com.dropify.order.lock;

import com.dropify.common.exception.BusinessException;
import com.dropify.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.annotation.Order;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@Order(-1) // StockChangeAspect(@Order(0))보다 먼저 실행 — 락이 가장 바깥 레이어
@RequiredArgsConstructor
public class DistributedLockAspect {

    private final RedissonClient redissonClient;
    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(distributedLock)")
    public Object lock(ProceedingJoinPoint pjp, DistributedLock distributedLock) throws Throwable {
        String lockKey = resolveKey(pjp, distributedLock.key());
        RLock lock = redissonClient.getLock(lockKey);

        boolean acquired = lock.tryLock(
                distributedLock.waitTime(),
                distributedLock.leaseTime(),
                distributedLock.timeUnit()
        );

        if (!acquired) {
            log.warn("분산 락 획득 실패: key={}", lockKey);
            throw new BusinessException(ErrorCode.CONCURRENT_ORDER);
        }

        log.debug("분산 락 획득: key={}", lockKey);
        try {
            return pjp.proceed();
        } finally {
            // isHeldByCurrentThread: 현재 스레드가 소유한 락만 해제 (leaseTime 초과로 자동 해제된 경우 방어)
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("분산 락 해제: key={}", lockKey);
            }
        }
    }

    /**
     * @DistributedLock의 key 속성(SpEL)을 실제 락 키 문자열로 평가한다.
     * 메서드 파라미터 이름을 SpEL 변수로 등록하므로 #productId 같은 표현식을 사용할 수 있다.
     */
    private String resolveKey(ProceedingJoinPoint pjp, String keyExpression) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        String[] paramNames = signature.getParameterNames();
        Object[] args = pjp.getArgs();

        StandardEvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < paramNames.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }

        return parser.parseExpression(keyExpression).getValue(context, String.class);
    }
}
