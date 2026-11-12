package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/admin/dish")
public class DishController {
    @Autowired
    private DishService dishService;

    @PostMapping
    public Result saveDish(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品: {}",dishDTO);
        dishService.saveDish(dishDTO);
        return Result.success();

    }
    @GetMapping("/page")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO) {
    log.info("菜品分页查询：{}",dishPageQueryDTO);
    PageResult pageResult =dishService.page(dishPageQueryDTO);
    return Result.success(pageResult);

    }

    @DeleteMapping
    public Result delete(@RequestParam List<Long> ids) {

        log.info("删除菜品: {}",ids);
        dishService.deleteByIds(ids);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<DishVO> getById(@PathVariable Long id){
        log.info("根据iD查询菜品: {}",id);
        DishVO dishVO = dishService.getByIdWitnFlavor(id);
        return Result.success(dishVO);
    }

    @PutMapping
    public Result update(@RequestBody DishDTO dishDTO) {
        log.info("修改菜品: {}",dishDTO);
        dishService.updateByFlavor(dishDTO);
        return Result.success();


    }


}
