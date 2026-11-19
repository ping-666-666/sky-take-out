package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@Slf4j
@RequestMapping("/admin/dish")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;


    @PostMapping
    public Result saveDish(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品: {}",dishDTO);
        dishService.saveDish(dishDTO);
        String key = "dish_" + dishDTO.getCategoryId();
        cleanCache(key);
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

        cleanCache("dish_*");
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

        cleanCache("dish_*");
        return Result.success();


    }

    @PostMapping("/status/{status}")
    public Result setStatus(@PathVariable Integer status,Long id ){
        log.info("菜品{}起售与停售 :{}", id,status == 1 ? "起售" : "停售");
        dishService.setStatus(status,id);
        cleanCache("dish_*");

        return Result.success();

    }

    @GetMapping("/list")
    public Result<List<DishVO>> getDishById(Integer categoryId){
        log.info("根据分类iD查询菜品 :{}",categoryId);
        List<DishVO> list = dishService.getDishById(categoryId);
        return Result.success(list);

    }

    private void cleanCache(String pattern){
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);


    }

}
