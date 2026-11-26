package com.sky.controller.user;

import cn.hutool.db.sql.Order;
import com.github.pagehelper.Page;
import com.sky.dto.OrdersDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.mapper.OrderMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("userOrderController")
@RequestMapping("/user/order")
@Slf4j
public class OrderController {
    @Autowired
    private OrderService orderService;

    @RequestMapping("/submit")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO  ordersSubmitDTO) {
        log.info("用户下单 : {}", ordersSubmitDTO);
       OrderSubmitVO orderSubmitVO =   orderService.submit(ordersSubmitDTO);
       return Result.success(orderSubmitVO);
    }
    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
        log.info("生成预支付交易单：{}", orderPaymentVO);
        return Result.success(orderPaymentVO);
    }

    @GetMapping("/historyOrders")
    public Result<PageResult> page(Integer page,Integer pageSize,Integer status){
    log.info("查询历史订单 :{},{},{}",page,pageSize,status);
    PageResult pageResult  =  orderService.page(page,pageSize,status);
    return Result.success(pageResult);
    }

    @GetMapping("/orderDetail/{id}")
    public Result<OrderVO> getOrder(@PathVariable Long id){
        log.info("查询订单详情 :{}",id);
        OrderVO orderVO = orderService.getOrderDetails(id);
        return Result.success(orderVO);

    }

    @PutMapping("/cancel/{id}")
    public Result cancel(@PathVariable Long id){
        log.info("取消订单 :{}",id);
        orderService.cancelOrder(id);
        return Result.success();
    }

    @PostMapping("/repetition/{id}")
    public Result  repetition(@PathVariable  Long id){
        log.info("再来一单 :{}",id);
        orderService.repetition(id);
        return Result.success();}

    @GetMapping("/reminder/{id}")
    public Result  reminder(@PathVariable  Long id){
        log.info("用户催单 :{}",id);
        orderService.reminder(id);
        return Result.success();
    }
}
