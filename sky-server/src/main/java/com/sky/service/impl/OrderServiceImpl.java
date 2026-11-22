package com.sky.service.impl;

import cn.hutool.core.util.IdUtil;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.AddressBookService;
import com.sky.service.OrderService;
import com.sky.service.ShoppingCartService;
import com.sky.vo.OrderSubmitVO;
import org.aspectj.weaver.ast.Var;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// ===== [AI 修改 2026-11-22] 开始：问题一（金额完全信任前端）修复 - 新增 import =====
import com.sky.exception.OrderBusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
// ===== [AI 修改 2026-11-22] 结束 =====

@Service
@Transactional(rollbackFor = Exception.class)
// ===== [AI 修改 2026-11-22] 开始：问题一修复 - 用于记录前端金额与服务端计算值不一致 =====
@Slf4j
// ===== [AI 修改 2026-11-22] 结束 =====
public class OrderServiceImpl implements OrderService {
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;

    // ===== [AI 修改 2026-11-22] 开始：问题一修复 - 打包费改由服务端定价 =====
    /**
     * 每单打包费（元）。
     * 由服务端决定，可通过配置 sky.order.pack-amount 调整；
     * 不再采信前端传入的 packAmount（前端传什么都不会影响订单金额）。
     */
    @Value("${sky.order.pack-amount:2}")
    private int packFee;
    // ===== [AI 修改 2026-11-22] 结束 =====

    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        //异常情况的处理（收货地址为空、超出配送范围、购物车为空）
        //收货地址判空
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if(addressBook==null  ||  !Objects.equals(addressBook.getUserId(),BaseContext.getCurrentId())){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //购物车判空
        //查询当前用户的购物车数据
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .build();
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if(list==null || list.size()==0){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // ===== [AI 修改 2026-11-22] 开始：问题一（金额完全信任前端）修复 =====
        // 订单金额只认服务端：购物车里的 amount 是加购时由服务端从 dish/setmeal 表写入的，
        // 前端传来的 ordersSubmitDTO.amount 一律不采信，仅用于比对告警。
        BigDecimal goodsAmount = BigDecimal.ZERO; // 商品总额 = Σ(单价 × 数量)
        for (ShoppingCart cart : list) {
            if (cart.getNumber() == null || cart.getNumber() <= 0) {
                throw new OrderBusinessException(MessageConstant.ORDER_ITEM_INVALID);
            }
            if (cart.getAmount() == null || cart.getAmount().signum() < 0) {
                throw new OrderBusinessException(MessageConstant.ORDER_ITEM_INVALID);
            }
            goodsAmount = goodsAmount.add(
                    cart.getAmount().multiply(BigDecimal.valueOf(cart.getNumber())));
        }
        // 打包费由服务端定价（配置 sky.order.pack-amount），前端 packAmount 不再生效
        BigDecimal total = goodsAmount.add(BigDecimal.valueOf(packFee))
                .setScale(2, RoundingMode.HALF_UP);

        // 前端金额只用来发现异常（前端算错或被篡改），不影响最终入库金额
        if (ordersSubmitDTO.getAmount() != null
                && ordersSubmitDTO.getAmount().compareTo(total) != 0) {
            log.warn("下单金额与购物车不一致：前端={}，服务端={}，以服务端为准",
                    ordersSubmitDTO.getAmount(), total);
        }
        // ===== [AI 修改 2026-11-22] 结束 =====

        //构造订单数据
        Orders order = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,order);

        // ===== [AI 修改 2026-11-22] 开始：问题一修复 - 用服务端算出的金额覆盖前端传值 =====
        order.setAmount(total);
        order.setPackAmount(packFee);
        // ===== [AI 修改 2026-11-22] 结束 =====

        order.setPhone(addressBook.getPhone());
        order.setAddress(addressBook.getDetail());
        order.setConsignee(addressBook.getConsignee());
        order.setNumber(IdUtil.getSnowflakeNextIdStr());
        order.setUserId(userId);
        order.setStatus(Orders.PENDING_PAYMENT);
        order.setPayStatus(Orders.UN_PAID);
        order.setOrderTime(LocalDateTime.now());

        //向订单表插入1条数据
        orderMapper.insert(order);

        //向明细表插入n条数据

        List<OrderDetail> orderDetails = new ArrayList<>();
        for (ShoppingCart cart : list) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart,orderDetail);
            orderDetail.setOrderId(order.getId());
            orderDetails.add(orderDetail);
        }

        orderDetailMapper.insertBatch(orderDetails);

        //清理购物车中的数据
        shoppingCartMapper.deleteAll(userId);
        //封装返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(order.getId())
                .orderNumber(order.getNumber())
                .orderAmount(order.getAmount())
                .orderTime(order.getOrderTime())
                .build();
        return orderSubmitVO;

    }
}
