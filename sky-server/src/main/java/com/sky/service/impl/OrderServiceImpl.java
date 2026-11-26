package com.sky.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.db.sql.Order;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.properties.WeChatProperties;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

// ===== [AI 修改 2026-11-22] 开始：问题一（金额完全信任前端）修复 - 新增 import =====
import com.sky.exception.OrderBusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Collectors;
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
    @Autowired
    private WeChatProperties weChatProperties;



    private Orders orders;
    @Autowired
    private OrderService orderService;

    @Value("${sky.shop.address}")
    private String shopAddress;

    @Value("${sky.baidu.ak}")
    private String ak;


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


    public PageResult page(Integer page, Integer pageSize, Integer status) {

        PageHelper.startPage(page, pageSize);
        Long userId = BaseContext.getCurrentId();
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(userId);
        ordersPageQueryDTO.setStatus(status);

        Page<Orders> p = orderMapper.pageQuery(ordersPageQueryDTO);
        List<OrderVO> list = new ArrayList<>();

        if (p != null && p.getTotal() > 0) {
            for (Orders order : p) {
                Long orderId = order.getId();
                List<OrderDetail> orderDetails =  orderDetailMapper.getByOrderId(orderId);
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(order, orderVO);
                orderVO.setOrderDetailList(orderDetails);
                list.add(orderVO);
            }
        }
        return new PageResult(p.getTotal(), list);

    }


    public OrderVO getOrderDetails(Long id) {
        Orders orders = new Orders();
        orders.setId(id);

        OrderVO orderVO = new OrderVO();
       Orders orders1 =  orderMapper.getOrder(orders);
       BeanUtils.copyProperties(orders1,orderVO);

        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(id);
        orderVO.setOrderDetailList(orderDetails);
        return orderVO;
    }


    public void cancelOrder(Long id) {
        Orders orders = orderMapper.getById(id);
        //检验订单是否存在
        if(orders==null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        Integer status = orders.getStatus();

        //订单已接单，已送出，配送中，已完成，已取消
        if(status > 2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 订单处于待接单状态下取消，需要进行退款
        if(status.equals(Orders.TO_BE_CONFIRMED))
        {
           //调用微信支付退款接口
//            try {
//                weChatPayUtil.refund(orders.getNumber(),
//                        orders.getNumber(),
//                        orders.getAmount(),
//                        orders.getAmount());
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
            orders.setPayStatus(Orders.REFUND);

        }
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());

        orderMapper.update(orders);




    }

    /**
     * 再来一单
     * @param id
     */

    public void repetition(Long id) {
        Long userId = BaseContext.getCurrentId();
        //查询相关订单详情
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(id);
        //添加到购物车
        List<ShoppingCart> shoppingCartList = orderDetails.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());
            return shoppingCart;

        }).collect(Collectors.toList());
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 订单搜索
     * @param ordersPageQueryDTO
     * @return
     */

    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> orders = orderMapper.pageQuery(ordersPageQueryDTO);

        if (orders != null && orders.getTotal() > 0) {
            List<OrderVO> orderVOlist = new ArrayList<>();
            for (Orders order : orders) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(order, orderVO);
                orderVO.setOrderDetailList(orderDetailMapper.getByOrderId(order.getId()));
                orderVOlist.add(orderVO);
            }
            return new PageResult(orders.getTotal(), orderVOlist);
        }
        return null;


    }


    public OrderStatisticsVO statistics() {

        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress =  orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);

        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);


        return orderStatisticsVO;


    }

    /**
     * 商家接单
     * @param ordersConfirmDTO
     */

    public void comfirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = new Orders();
        orders.setId(ordersConfirmDTO.getId());
        orders.setStatus(Orders.CONFIRMED);
        orderMapper.update(orders);

    }


    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        Long orderId = ordersRejectionDTO.getId();
        Orders orders =  orderMapper.getById(orderId);
        //只有未接单时可以拒单
        if(orders == null && orders.getStatus() .equals(Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //付款的要退款
        if(orders.getPayStatus() == Orders.PAID){
            orders.setPayStatus(Orders.REFUND);
            log.info("退款");
        }

        //指定拒单原因  改变订单状态
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelTime(LocalDateTime.now());

        orderMapper.update(orders);


    }

    /**
     * 商家取消订单
     * @param ordersCancelDTO
     */
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        //判断订单是否存在
        Long orderId = ordersCancelDTO.getId();
        Orders orders =  orderMapper.getById(orderId);
        if(orders == null && !orders.getStatus() .equals(Orders.CONFIRMED) ){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //支付的要退款
        if(orders.getPayStatus() == Orders.PAID){
            orders.setPayStatus(Orders.REFUND);
        }
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 派送订单
     * @param id
     */
    public void delivery(Long id) {
        //订单是否存在且为待派送状态
        Orders orders = orderMapper.getById(id);
        if(orders == null && !orders.getStatus() .equals(Orders.CONFIRMED) ){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //修改状态
        Orders build = Orders.builder()
                .id(id)
                .status(Orders.DELIVERY_IN_PROGRESS)
                .build();
        orderMapper.update(build);
    }

    public void complete(Long id) {
        Orders order = orderMapper.getById(id);
        if(order == null && !orders.getStatus().equals(Orders.DELIVERY_IN_PROGRESS) ){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        order = Orders.builder()
                .id(id)
                .status(Orders.COMPLETED)
                .deliveryTime(LocalDateTime.now())
                .build();
        orderMapper.update(order);


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
        //检验用户的收货地址是否超出配送范围
        checkOutOfRange(addressBook.getCityName()+addressBook.getDistrictName()+addressBook.getDetail());

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

    /**
     * 检查客户的收货地址是否超出配送范围
     * @param address
     */
    private void checkOutOfRange(String address) {
        Map map = new HashMap();
        map.put("address",shopAddress);
        map.put("output","json");
        map.put("ak",ak);

        //获取店铺的经纬度坐标
        String shopCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        JSONObject jsonObject = JSON.parseObject(shopCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("店铺地址解析失败");
        }

        //数据解析
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lat = location.getString("lat");
        String lng = location.getString("lng");
        //店铺经纬度坐标
        String shopLngLat = lat + "," + lng;

        map.put("address",address);
        //获取用户收货地址的经纬度坐标
        String userCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        jsonObject = JSON.parseObject(userCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("收货地址解析失败");
        }

        //数据解析
        location = jsonObject.getJSONObject("result").getJSONObject("location");
        lat = location.getString("lat");
        lng = location.getString("lng");
        //用户收货地址经纬度坐标
        String userLngLat = lat + "," + lng;

        map.put("origin",shopLngLat);
        map.put("destination",userLngLat);
        map.put("steps_info","0");

        //路线规划
        String json = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map);

        jsonObject = JSON.parseObject(json);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("配送路线规划失败");
        }

        //数据解析
        JSONObject result = jsonObject.getJSONObject("result");
        JSONArray jsonArray = (JSONArray) result.get("routes");
        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");

        if(distance > 5000){
            //配送距离超过5000米
            throw new OrderBusinessException("超出配送范围");
        }
    }





}
