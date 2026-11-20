package com.sky.controller.user;


import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Delete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@Api(tags = "购物车相关接口")
@RequestMapping("/user/shoppingCart")
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartCartService;

    @PostMapping("/add")
    public Result add(@RequestBody ShoppingCartDTO  shoppingCartDTO){
        log.info("加入购物车的菜品或套餐iD :{}",shoppingCartDTO.getDishId() == null ? shoppingCartDTO.getSetmealId() : shoppingCartDTO.getDishId());
        shoppingCartCartService.addShoppingCart(shoppingCartDTO);
        return Result.success();





    }

    @GetMapping("/list")
    public Result<List<ShoppingCart>> showShoppingCart(){
        Long id = BaseContext.getCurrentId();
        log.info("用户: {} 查看购物车 ",id);
       List<ShoppingCart> list =  shoppingCartCartService.showShoppingCart(id);
       return Result.success(list);

    }

    @DeleteMapping("/clean")
    public Result cleanShoppingCart(){
        Long id = BaseContext.getCurrentId();
        log.info("用户: {} 清空购物车 ",id);
        shoppingCartCartService.clean(id);
        return Result.success();
    }

    @PostMapping("/sub")
    public  Result delete(@RequestBody ShoppingCartDTO  shoppingCartDTO){
        log.info("删除购物车中的一个商品 : {}",shoppingCartDTO.getSetmealId() == null ? "菜品" + shoppingCartDTO.getDishId() : "套餐" + shoppingCartDTO.getSetmealId());
        shoppingCartCartService.subShoppingCart(shoppingCartDTO);
        return Result.success();

    }




}
