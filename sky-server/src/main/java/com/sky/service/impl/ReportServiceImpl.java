package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    public TurnoverReportVO turnoverReport(LocalDate begin, LocalDate end) {
        //datelist
        List<LocalDate> datelist = new ArrayList<>();
        datelist.add(begin);
        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            datelist.add(begin);
        }

        //turnoverList
       List<Double> turnoverList = new ArrayList<>();
        for (LocalDate localDate : datelist) {
            LocalDateTime beginTime = LocalDateTime.of(localDate, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(localDate, LocalTime.MAX);

            Map map = new HashMap<>();
            map.put("status", Orders.COMPLETED);
            map.put("begin", beginTime);
            map.put("end", endTime);

            Double turnover =  orderMapper.sumByMap(map);
           turnover=  turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }

        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(datelist,","))
                .turnoverList(StringUtils.join(turnoverList,","))
                .build();


    }


    public UserReportVO countUser(LocalDate begin, LocalDate end) {
        //获取日期列表
        List<LocalDate> datelist = new ArrayList<>();
        datelist.add(begin);
        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            datelist.add(begin);
        }
        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();


        for (LocalDate date : datelist) {
            //获取时间
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            //新用户
            Integer newUser =  userMapper.getUserCount(beginTime,endTime);

            //总用户数
            Integer totalUser = userMapper.getUserCount(null,endTime);

            newUserList.add(newUser);
            totalUserList.add(totalUser);

        }

        return UserReportVO
                .builder()
                .dateList(StringUtils.join(datelist,","))
                .newUserList(StringUtils.join(newUserList,","))
                .totalUserList(StringUtils.join(totalUserList,","))
                .build();





    }


    public OrderReportVO getOrdersReport(LocalDate  begin, LocalDate end) {
        //dateList
        List<LocalDate> datelist = new ArrayList<>();
        datelist.add(begin);
        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            datelist.add(begin);
        }
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();

        for (LocalDate date : datelist) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap<>();
            map.put("begin", beginTime);
            map.put("end", endTime);

            //orderCountList
            Integer orderCount = getOrderCount(map);
            orderCountList.add(orderCount);
            //validOrderCountList
            map.put("status", Orders.COMPLETED);
            Integer validOrderCount = getOrderCount(map);
            validOrderCountList.add(validOrderCount);

        }

        //validOrderCount
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();

        //totalOrderCount
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();
        //orderCompletionRate
       double orderCompletionRate = 0.0;
       if (totalOrderCount != 0){
             orderCompletionRate =  validOrderCount.doubleValue() /totalOrderCount ;
       }

       return OrderReportVO
               .builder()
               .dateList(StringUtils.join(datelist,","))
               .orderCountList(StringUtils.join(orderCountList,","))
               .validOrderCountList(StringUtils.join(validOrderCountList,","))
               .totalOrderCount(totalOrderCount)
               .validOrderCount(validOrderCount)
               .orderCompletionRate(orderCompletionRate)
               .build();

    }


    private Integer getOrderCount(Map map) {
       return orderMapper.countByMap(map);

    }

}
