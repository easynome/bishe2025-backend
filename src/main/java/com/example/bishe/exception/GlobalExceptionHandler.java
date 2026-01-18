package com.example.bishe.exception;

import com.example.bishe.entity.R;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.ConnectException;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice//拦截所有RestController抛出的异常
public class GlobalExceptionHandler {
    // 处理运行时异常
    @ExceptionHandler(RuntimeException.class)
    public R<String> handleRuntimeException(RuntimeException e){
        log.error("系统运行异常：{}",e.getMessage());
        return R.failed("系统异常："+e.getMessage());
    }

    // 处理Redis异常
    @ExceptionHandler({ConnectException.class, RedisConnectionFailureException.class})
    public R<String> handleRedisException(Exception e){
        log.error("缓存连接失败，系统已降级查库：{}",e.getMessage());
        return R.failed("缓存服务暂时不可用，请稍后刷新");
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<String> handleValidationException(MethodArgumentNotValidException e){
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("，"));
        log.warn("参数验证失败：{}",message);
        return R.failed(message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public R<String> handleConstraintViolationException(ConstraintViolationException e){
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("，"));
        log.warn("参数验证失败：{}",message);
        return R.failed("参数非法："+message);
    }

    // 处理所有异常
    @ExceptionHandler(Exception.class)
    public R<String> handleException(Exception e){
        log.error("未知错误：{}",e.getMessage());
        return R.failed("未知错误，请联系管理员："+e.getMessage());
    }

}
