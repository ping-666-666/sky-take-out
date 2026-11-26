package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理支付超时订单
     */
    @Transactional(rollbackFor = Exception.class)
    @Scheduled(cron = "0 * * * * ? ")
    public void processTimeoutOrder(){
        log.info("处理超时订单 :{}",new Date());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime time = now.plusMinutes(-15);
        List<Orders> list =  orderMapper.getByStatusAndOrderTime(Orders.PENDING_PAYMENT,time);

        if (list!=null && list.size()>=0){
            for (Orders orders : list) {
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单超时");
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
            }

        }

    }

    /**
     * 处理“派送中”的订单
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void processOrder(){

        log.info("处理派送中状态的订单 :{}",new Date());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime time = now.plusHours(-1);
        List<Orders> list = orderMapper.getByStatusAndOrderTime(Orders.DELIVERY_IN_PROGRESS, time);

        if (list!=null||list.size()>=0){
            list.forEach(orders->{
                orders.setStatus(Orders.COMPLETED);
                orderMapper.update(orders);
            });
        }


    }
}
