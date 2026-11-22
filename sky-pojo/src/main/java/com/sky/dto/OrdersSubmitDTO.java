package com.sky.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrdersSubmitDTO implements Serializable {
    //地址簿id
    private Long addressBookId;
    //付款方式
    private int payMethod;
    //备注
    private String remark;
    //预计送达时间
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime estimatedDeliveryTime;
    //配送状态  1立即送出  0选择具体时间
    private Integer deliveryStatus;
    //餐具数量
    private Integer tablewareNumber;
    //餐具数量状态  1按餐量提供  0选择具体数量
    private Integer tablewareStatus;
    // ===== [AI 修改 2026-11-22] 开始：问题一（金额完全信任前端）修复 =====
    // 打包费字段已删除：打包费改由服务端定价（配置 sky.order.pack-amount），
    // 前端即使仍然传 packAmount，也会被 Jackson 忽略（FAIL_ON_UNKNOWN_PROPERTIES=false）。
    // 总金额：仅用于和服务端计算结果做一致性比对（不一致只记 warn 日志），
    // 传什么值都不会进入订单表，真正入库的金额由 OrderServiceImpl 按购物车重新计算。
    private BigDecimal amount;
    // ===== [AI 修改 2026-11-22] 结束 =====
}
