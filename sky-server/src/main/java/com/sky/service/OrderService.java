package com.sky.service;

import com.sky.dto.*;
import com.sky.entity.Orders;
import com.sky.result.PageResult;
import com.sky.vo.*;
import org.apache.ibatis.annotations.Select;

public interface OrderService {
    OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    PageResult page(Integer page, Integer pageSize, Integer status);

    OrderVO getOrderDetails(Long id);

    void cancelOrder(Long id);

    void repetition(Long id);

    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    OrderStatisticsVO statistics();

    void comfirm(OrdersConfirmDTO ordersConfirmDTO);


    void rejection(OrdersRejectionDTO ordersRejectionDTO);


    void cancel(OrdersCancelDTO ordersCancelDTO);

    void delivery(Long id);

    void complete(Long id);

    void reminder(Long id);


}
