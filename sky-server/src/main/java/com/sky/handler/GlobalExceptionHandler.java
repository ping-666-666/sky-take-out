package com.sky.handler;

import com.sky.constant.MessageConstant;
import com.sky.exception.BaseException;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    @ExceptionHandler
    public Result exceptionHandler(SQLIntegrityConstraintViolationException sex){
        String message = sex.getMessage();
        if (message.contains("Duplicate entry")) {
            String[] split = message.split(" ");
            String s = split[2];

            String msg = s + MessageConstant.ALREADY_EXISTS;
            return Result.error(msg);
        }else{

            return Result.error(MessageConstant.UNKNOWN_ERROR);
        }

    }

    // ===== [AI 修改 2026-11-17] 开始：兜底异常处理 =====
    // 作用：空 body / 非法 JSON / 415 / 405 这类框架异常原本会返回 Spring 默认的
    // {"timestamp":..,"status":400,"error":"Bad Request","path":".."}，和项目统一的
    // {"code":..,"msg":..,"data":..} 格式不一致，前端要写两套解析。这里统一成 Result 格式。
    @ExceptionHandler(Exception.class)
    public Result exceptionHandler(Exception ex) {
        log.error("未知异常：", ex);
        return Result.error(MessageConstant.UNKNOWN_ERROR);
    }
    // ===== [AI 修改 2026-11-17] 结束 =====

}
