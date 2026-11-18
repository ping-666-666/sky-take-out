package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.exception.BaseException;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/setmeal")
@Slf4j
public class SetmealController {
    @Autowired
    private SetmealService setMealService;

    @PostMapping
    public Result saveSetmeal(@RequestBody SetmealDTO setmealDTO){
        log.info("新增套餐 :{}",setmealDTO);
        setMealService.saveSetMeal(setmealDTO);
        return Result.success();
    }

    @PutMapping
    public Result updateSetmeal(@RequestBody SetmealDTO setmealDTO){
        log.info("修改套餐 :{}",setmealDTO);
        setMealService.updateSetmeal(setmealDTO);
        return Result.success();
    }

    @GetMapping("/page")
    public Result<PageResult> getByPage(SetmealPageQueryDTO setmealPageQueryDTO){
        log.info("分页查询 :{}",setmealPageQueryDTO);
        PageResult pageResult = setMealService.getByPage(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    public Result<SetmealVO> getSetmealById(@PathVariable Long id){
        log.info("根据iD查询套餐 :{}",id);
       SetmealVO setmealVO= setMealService.getById(id);
       return Result.success(setmealVO);

    }

    @PostMapping("/status/{status}")
    public Result setmealStatus(@PathVariable Integer status,Long id ){
        log.info("套餐{}起售与停售 :{}" ,id, status == 1 ? "起售中" : "停售中");
        setMealService.setMeal(id,status);
        return Result.success();
    }

    @DeleteMapping
    public Result deleteSetmealByIds(@RequestParam List<Long> ids){
        if(ids==null||ids.isEmpty()){
            throw new BaseException("请选择要删除的套餐");
        }

        log.info("批量删除套餐:{}",ids);
        setMealService.deleteByIds(ids);
        return Result.success();
    }


}
