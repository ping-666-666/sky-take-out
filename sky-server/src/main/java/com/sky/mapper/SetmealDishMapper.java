package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.enumeration.OperationType;
import com.sky.vo.SetmealVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {
    void insertBatches(List<SetmealDish> setmealDishes);

    Page<SetmealVO> Page(SetmealPageQueryDTO setmealPageQueryDTO);

    @Select("select * from setmeal_dish where setmeal_id =#{id}")
    List<SetmealDish> getBySetmealId(Long id);

    @Delete("delete  from setmeal_dish where setmeal_id =#{setmealId}")
    void delete(Long setmealId);

    void deleteByIds(List<Long> ids);
}
