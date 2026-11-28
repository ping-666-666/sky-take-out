package com.sky.controller.admin;

import com.sky.mapper.OrderMapper;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;



@RestController
@Slf4j
@RequestMapping("/admin/report")
public class ReportController {
    @Autowired
    private ReportService reportService ;
    @Autowired
    private OrderService orderService;

    @GetMapping("/turnoverStatistics")
    public Result<TurnoverReportVO> turnoverReport(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {

        log.info("营业额统计 : {},{}",begin,end );
        return Result.success(reportService.turnoverReport(begin,end));


    }

    @GetMapping("/userStatistics")
    public  Result<UserReportVO> userReport(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("统计用户数量 :{},{}",begin,end );
        return Result.success(reportService.countUser(begin,end)) ;


    }

    @GetMapping("/ordersStatistics")
    public  Result<OrderReportVO> ordersReport(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
                                               @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){


        log.info("订单统计 :{},{}",begin,end );
        return Result.success(reportService.getOrdersReport(begin,end));
    }
}
