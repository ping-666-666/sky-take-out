package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private DishMapper dishMapper;


    @Override
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO,shoppingCart);

        //获取用户iD
        Long currentId = BaseContext.getCurrentId();
        shoppingCart.setUserId(currentId);

        //判断当前添加的菜品或套餐是否存在购物车
        List<ShoppingCart> list =  shoppingCartMapper.list(shoppingCart);

        //不存在,则添加
        if(list==null||list.size()==0){
            //判断添加的是菜品还是套餐
            if (shoppingCart.getDishId()==null){
                //是套餐
                shoppingCart.setName(setmealMapper.getById(shoppingCartDTO.getSetmealId()).getName());
                shoppingCart.setAmount(setmealMapper.getById(shoppingCartDTO.getSetmealId()).getPrice());
                shoppingCart.setImage(setmealMapper.getById(shoppingCartDTO.getSetmealId()).getImage());

            }else {
                //是菜品
                shoppingCart.setName(dishMapper.getById(shoppingCartDTO.getDishId()).getName());
                shoppingCart.setAmount(dishMapper.getById(shoppingCartDTO.getDishId()).getPrice());
                shoppingCart.setImage(dishMapper.getById(shoppingCartDTO.getDishId()).getImage());


            }shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());

            //插入
            shoppingCartMapper.insert(shoppingCart);

        }

        //存在,加一就行
        else {
            ShoppingCart cart = list.get(0);
            cart.setNumber(cart.getNumber()+1);
            shoppingCartMapper.updateNumberById(cart);

        }


    }
}
