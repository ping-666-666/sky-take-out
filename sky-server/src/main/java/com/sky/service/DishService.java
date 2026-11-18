package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService {

   void saveDish(DishDTO dishDTO);

    PageResult page(DishPageQueryDTO dishPageQueryDTO);

    void deleteByIds(List<Long> ids);

    DishVO getByIdWitnFlavor(Long id);

    void updateByFlavor(DishDTO dishDTO);

    void setStatus(Integer status, Long id);

    List<DishVO> getDishById(Integer id);


 /**
  * 条件查询菜品和口味
  * @param dish
  * @return
  */
 List<DishVO> listWithFlavor(Dish dish);
}
