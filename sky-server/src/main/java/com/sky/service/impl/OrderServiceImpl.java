package com.sky.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.service.AddressBookService;
import com.sky.service.OrderService;
import com.sky.service.ShoppingCartService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
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

    // ===== [AI 修改 2026-11-23] 开始：打包费由「每单固定」改为「按份累计」 =====
    /**
     * 每份打包费单价（元/份）。
     * 打包费 = 购物车内所有菜品、套餐的份数之和 × 该单价，默认 1 元/份；
     * 可通过配置 sky.order.pack-unit-price 调整，前端传的 packAmount 不参与计算。
     */
    @Value("${sky.order.pack-unit-price:1}")
    private BigDecimal packUnitPrice;
    /**
     * 固定配送费（元/单）。
     * 原型口径：任何区域任何地址的订单配送费默认 6 元；
     * 可通过配置 sky.order.delivery-fee 调整。
     * 注：orders 表没有配送费字段，这部分金额只体现在 amount 里。
     */
    @Value("${sky.order.delivery-fee:6}")
    private BigDecimal deliveryFee;
    // ===== [AI 修改 2026-11-23] 结束 =====
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;

    private Orders orders;
    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

//        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
//
//        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
//            throw new OrderBusinessException("该订单已支付");
//        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code","ORDERPAID");
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));
        Integer OrderPaidStatus = Orders.PAID;//支付状态，已支付
        Integer OrderStatus = Orders.TO_BE_CONFIRMED;  //订单状态，待接单
        LocalDateTime check_out_time = LocalDateTime.now();//更新支付时间
        orderMapper.updateStatus(OrderStatus, OrderPaidStatus, check_out_time, this.orders.getId());
        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();

        // 根据订单号查询当前用户的订单
        Orders ordersDB = orderMapper.getByNumberAndUserId(outTradeNo, userId);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }
    // ===== [AI 修改 2026-11-22] 结束 =====
    @Transactional(rollbackFor = Exception.class)
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
        // ===== [AI 修改 2026-11-23] 开始：打包费按份累计 + 固定配送费 =====
        // 原型口径：打包费 = 菜品数量 × 1 元，配送费固定 6 元
        // 订单总额 = 菜品费总和 + 打包费 + 配送费
        int packUnits = 0; // 购物车内所有菜品、套餐的份数之和
        for (ShoppingCart cart : list) {
            packUnits += cart.getNumber();
        }
        BigDecimal packAmount = packUnitPrice
                .multiply(BigDecimal.valueOf(packUnits))
                .setScale(2, RoundingMode.HALF_UP);
        // 配送费不单独落库（orders 无该字段），只体现在 amount 里
        BigDecimal total = goodsAmount.add(packAmount).add(deliveryFee)
                .setScale(2, RoundingMode.HALF_UP);
        // ===== [AI 修改 2026-11-23] 结束 =====

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
        // orders.pack_amount 是整型列：按「元」取整后入库（单价建议配整数元）
        order.setPackAmount(packAmount.setScale(0, RoundingMode.HALF_UP).intValue());
        // ===== [AI 修改 2026-11-22] 结束 =====

        order.setPhone(addressBook.getPhone());
        order.setAddress(addressBook.getDetail());
        order.setConsignee(addressBook.getConsignee());
        order.setNumber(IdUtil.getSnowflakeNextIdStr());
        order.setUserId(userId);

        order.setStatus(Orders.PENDING_PAYMENT);
        order.setPayStatus(Orders.UN_PAID);
        order.setOrderTime(LocalDateTime.now());
        this.orders = order;
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
