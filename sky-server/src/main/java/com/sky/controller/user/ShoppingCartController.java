package com.sky.controller.user;


import com.sky.dto.ShoppingCartDTO;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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




}
