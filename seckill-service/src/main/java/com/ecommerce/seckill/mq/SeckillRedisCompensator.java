package com.ecommerce.seckill.mq;

import com.alibaba.fastjson2.JSON;
import com.ecommerce.common.constant.RedisKeyConstant;
import com.ecommerce.seckill.vo.SeckillOrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀 Redis 回补：按订单号幂等，原子完成「标记完成 + 加库存 + 清用户标记」。
 */
@Slf4j
@Component
public class SeckillRedisCompensator {

    private static final String COMPENSATE_DONE_PREFIX = "seckill:compensate:done:";
    private static final String COMPENSATE_FAIL_PREFIX = "seckill:compensate:fail:";

    /**
     * KEYS[1]=done, [2]=stock, [3]=userMark, [4]=failMark; ARGV[1]=qty, [2]=ttlSeconds
     * return 1=applied, 0=already done
     */
    private static final String COMPENSATE_LUA =
            "if redis.call('exists', KEYS[1]) == 1 then return 0 end " +
            "redis.call('incrby', KEYS[2], ARGV[1]) " +
            "redis.call('del', KEYS[3]) " +
            "redis.call('set', KEYS[1], '1', 'EX', ARGV[2]) " +
            "redis.call('del', KEYS[4]) " +
            "return 1";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public enum CompensateResult {
        APPLIED,
        ALREADY_DONE,
        FAILED
    }

    public CompensateResult compensate(SeckillOrderVO orderVO) {
        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(COMPENSATE_LUA, Long.class);
            Long applied = stringRedisTemplate.execute(script,
                    Arrays.asList(
                            doneKey(orderVO.getOrderNo()),
                            RedisKeyConstant.SECKILL_STOCK_KEY + orderVO.getActivityId(),
                            RedisKeyConstant.USER_SECKILL_KEY + orderVO.getActivityId() + ":" + orderVO.getUserId(),
                            failKey(orderVO.getOrderNo())
                    ),
                    String.valueOf(orderVO.getQuantity()),
                    String.valueOf(TimeUnit.DAYS.toSeconds(7))
            );
            if (applied != null && applied == 0L) {
                log.info("Redis 回补已幂等跳过，orderNo={}", orderVO.getOrderNo());
                return CompensateResult.ALREADY_DONE;
            }
            log.info("Redis 回补成功，orderNo={}, qty={}", orderVO.getOrderNo(), orderVO.getQuantity());
            return CompensateResult.APPLIED;
        } catch (Exception ex) {
            log.error("Redis 回补失败，写入失败标记。orderNo={}", orderVO.getOrderNo(), ex);
            markCompensateFail(orderVO);
            return CompensateResult.FAILED;
        }
    }

    public void markCompensateFail(SeckillOrderVO orderVO) {
        try {
            stringRedisTemplate.opsForValue().set(
                    failKey(orderVO.getOrderNo()),
                    JSON.toJSONString(orderVO),
                    7, TimeUnit.DAYS
            );
        } catch (Exception ignore) {
            log.error("写入补偿失败标记也失败，orderNo={}", orderVO.getOrderNo());
        }
    }

    public boolean hasCompensateFailMark(String orderNo) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(failKey(orderNo)));
    }

    public boolean isCompensateDone(String orderNo) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(doneKey(orderNo)));
    }

    public static String doneKey(String orderNo) {
        return COMPENSATE_DONE_PREFIX + orderNo;
    }

    public static String failKey(String orderNo) {
        return COMPENSATE_FAIL_PREFIX + orderNo;
    }
}
