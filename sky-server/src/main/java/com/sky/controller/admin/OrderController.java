package com.sky.controller.admin;

import cn.hutool.db.sql.Order;
import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.jaxb.SpringDataJaxb;
import org.springframework.web.bind.annotation.*;

@RestController("adminOrderController")
@Slf4j
@RequestMapping("/admin/order")
public class OrderController {
    @Autowired
    private OrderService orderService;
    @GetMapping("/conditionSearch")
    public Result<PageResult>conditionSearch(OrdersPageQueryDTO  ordersPageQueryDTO){

        log.info("订单搜索 :{}",ordersPageQueryDTO);
       PageResult pageResult = orderService.conditionSearch(ordersPageQueryDTO);
       return Result.success(pageResult);


    }

    @GetMapping("/statistics")
    public Result<OrderStatisticsVO>  statistics(){
        log.info("查询各个状态的订单数量 ");
        OrderStatisticsVO orderStatisticsVO = orderService.statistics();
        return Result.success(orderStatisticsVO);

    }

    @GetMapping("/details/{id}")
    public Result<OrderVO> orderDetails(@PathVariable("id") Long id){
        log.info("查询订单详情 : {}",id);
        OrderVO orderVO = orderService.getOrderDetails(id);
        return Result.success(orderVO);
    }

    @PutMapping("/confirm")
    public Result confirm(@RequestBody OrdersConfirmDTO ordersConfirmDTO){
        log.info("商家接单 :{}",ordersConfirmDTO);
        orderService.comfirm(ordersConfirmDTO);
        return Result.success();

    }
    /**
     * 商家拒单
     */
    @PutMapping("/rejection")
    public Result rejection(@RequestBody OrdersRejectionDTO ordersRejectionDTO){
        log.info("商家拒单 :{},原因 :{}",ordersRejectionDTO.getId(),ordersRejectionDTO.getRejectionReason());
        orderService.rejection(ordersRejectionDTO);
        return Result.success();
    }

    /**
     * 取消订单
     */
    @PutMapping("/cancel")
    public Result cancel(@RequestBody OrdersCancelDTO ordersCancelDTO){
        log.info("商家取消订单 :{} , 原因 :{}",ordersCancelDTO.getId(),ordersCancelDTO.getCancelReason());
        orderService.cancel(ordersCancelDTO);
        return Result.success();
    }

    @PutMapping("/delivery/{id}")
    public Result delivery(@PathVariable("id") Long id){
        log.info("派送订单 :{}",id);
        orderService.delivery(id);
        return Result.success();
    }

    /**
     * 完成订单
     */
    @PutMapping("/complete/{id}")
    public Result complete(@PathVariable("id") Long id){
        log.info("完成订单 :{}",id);
        orderService.complete(id);
        return Result.success();


    }

}
