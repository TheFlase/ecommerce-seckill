package com.ecommerce.seckill.mq;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import com.ecommerce.common.constant.MqConstant;
import com.ecommerce.seckill.entity.SeckillOrder;
import com.ecommerce.seckill.mapper.SeckillOrderMapper;
import com.ecommerce.seckill.vo.SeckillOrderVO;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀订单消费者
 * 
 * 异步处理秒杀订单，实现削峰填谷
 */
@Slf4j
@Component
public class SeckillOrderConsumer {

    @Resource
    private SeckillOrderMapper seckillOrderMapper;

    @RabbitListener(queues = MqConstant.SECKILL_ORDER_QUEUE)
    public void handleSeckillOrder(Message message, Channel channel) throws Exception {
        try {
            String messageBody = new String(message.getBody());
            log.info("接收到秒杀订单消息：{}", messageBody);

            SeckillOrderVO orderVO = JSON.parseObject(messageBody, SeckillOrderVO.class);

            // 创建秒杀订单
            SeckillOrder order = new SeckillOrder();
            order.setOrderNo(orderVO.getOrderNo());
            order.setUserId(orderVO.getUserId());
            order.setActivityId(orderVO.getActivityId());
            order.setProductId(orderVO.getProductId());
            order.setProductName(orderVO.getProductName());
            order.setQuantity(orderVO.getQuantity());
            order.setSeckillPrice(orderVO.getSeckillPrice());
            order.setTotalPrice(orderVO.getSeckillPrice().multiply(
                    new BigDecimal(orderVO.getQuantity())
            ));
            order.setStatus(0); // 待支付
            order.setCreateTime(LocalDateTime.now());
            order.setUpdateTime(LocalDateTime.now());

            seckillOrderMapper.insert(order);
            log.info("秒杀订单创建成功：{}", order.getOrderNo());

            // 手动确认消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("处理秒杀订单消息失败：{}", e.getMessage(), e);
            // 消息重新入队
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }
}



