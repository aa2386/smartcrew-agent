package com.smartcrew.agent.common.exception;

import com.smartcrew.agent.common.domain.R;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器，用于将常见异常转换为统一响应结构。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常并返回统一失败响应。
     */
    @ExceptionHandler(ServiceException.class)
    public R<Void> handleServiceException(ServiceException exception) {
        return R.fail(exception.getCode(), exception.getMessage());
    }

    /**
     * 处理参数校验和请求反序列化异常。
     */
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class
    })
    public R<Void> handleValidationException(Exception exception) {
        return R.fail(400, exception.getMessage());
    }

    /**
     * 处理未捕获异常并记录错误日志。
     */
    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception exception) {
        log.error("Unhandled exception", exception);
        return R.fail(500, exception.getMessage());
    }
}
