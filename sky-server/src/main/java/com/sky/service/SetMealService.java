package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetMealService {
    void saveSetMeal(SetmealDTO setmealDTO);

    void updateSetmeal(SetmealDTO setmealDTO);

    PageResult getByPage(SetmealPageQueryDTO setmealPageQueryDTO);

    SetmealVO getById(Long id);

    void setMeal(Long id ,Integer status);

    void deleteByIds(List<Long> ids);
}
