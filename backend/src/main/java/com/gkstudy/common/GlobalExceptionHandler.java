package com.gkstudy.common;
import org.slf4j.Logger; import org.slf4j.LoggerFactory; import org.springframework.http.HttpStatus; import org.springframework.web.bind.MethodArgumentNotValidException; import org.springframework.web.bind.annotation.*;
@RestControllerAdvice public class GlobalExceptionHandler {
 private static final Logger log=LoggerFactory.getLogger(GlobalExceptionHandler.class);
 @ExceptionHandler(BusinessException.class) public ApiResponse<Void> business(BusinessException e){return ApiResponse.failure(e.getCode(),e.getMessage());}
 @ExceptionHandler(MethodArgumentNotValidException.class) @ResponseStatus(HttpStatus.BAD_REQUEST) public ApiResponse<Void> validation(MethodArgumentNotValidException e){return ApiResponse.failure("INVALID_ARGUMENT",e.getBindingResult().getAllErrors().get(0).getDefaultMessage());}
 @ExceptionHandler(Exception.class) @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) public ApiResponse<Void> system(Exception e){log.error("unexpected system error",e);return ApiResponse.failure("SYSTEM_ERROR","系统繁忙，请稍后重试");}
}
