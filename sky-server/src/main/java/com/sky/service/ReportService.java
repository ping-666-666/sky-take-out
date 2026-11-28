package com.sky.service;

import com.sky.vo.OrderReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import java.time.LocalDate;
import java.time.LocalDateTime;


public interface ReportService {

    TurnoverReportVO turnoverReport(LocalDate begin, LocalDate end);

    UserReportVO countUser(LocalDate begin, LocalDate end);

    OrderReportVO getOrdersReport(LocalDate  begin, LocalDate end);
}
