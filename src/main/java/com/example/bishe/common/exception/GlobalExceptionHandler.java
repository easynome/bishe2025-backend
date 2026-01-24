package com.example.bishe.common.exception;

import com.example.bishe.common.result.R;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<R<String>> handleRuntimeException(RuntimeException e){
        log.error("系统运行时异常：{}",e.getMessage());

        //如果是限流异常 返回429
        if(e.getMessage()!=null&&e.getMessage().contains("请求太频繁")){
            return ResponseEntity
                    //响应状态码429
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(R.failed("系统异常："+e.getMessage()));
        }
        //普通运行时异常返回500
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(R.failed("系统异常："+e.getMessage()));
    }

    // 处理Redis异常:返回503（Service Unavailable）
    @ExceptionHandler({ConnectException.class, RedisConnectionFailureException.class})
    public ResponseEntity<R<String>> handleRedisException(Exception e){
        log.error("缓存连接失败，系统已降级查库：{}",e.getMessage());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(R.failed("缓存服务暂时不可用，请稍后刷新"));
    }

    // 处理参数校验异常(DTO/Bean Validation): 返回400（Bad Request）
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<String>> handleValidationException(MethodArgumentNotValidException e){
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("，"));
        log.warn("参数验证失败：{}",message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(R.failed(message));
    }

    // 处理路径变量/简单参数校验异常(JSR303): 400（Bad Request）
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<R<String>> handleConstraintViolationException(ConstraintViolationException e){
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("，"));
        log.warn("参数验证失败：{}",message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(R.failed("参数非法："+message));
    }

    // 处理其他异常: 500（Internal Server Error）
    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<String>> handleException(Exception e){
        log.error("未知错误：{}",e.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(R.failed("未知错误，请联系管理员："+e.getMessage()));
    }

}
