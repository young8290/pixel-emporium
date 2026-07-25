package com.example.common.advice;

import com.example.common.exception.CustomException;
import com.example.common.exception.CustomForbiddenException;
import com.example.common.exception.CustomConflictException;
import com.example.common.exception.CustomUnauthorizedException;
import com.example.common.exception.CustomValidationException;
import com.example.common.exception.ErrorCode;
import com.example.common.response.R;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(value = NoSuchElementException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<Object> noSuchElementExceptionHandler(NoSuchElementException e) {
        return R.fail(ErrorCode.NOT_FOUND);
    }

    @ExceptionHandler(value = {NoResourceFoundException.class, NoHandlerFoundException.class})
    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<Object> resourceNotFoundExceptionHandler(Exception e) {
        return R.fail(ErrorCode.NOT_FOUND);
    }

    /**
     * 捕捉到 Spring 框架 BindException 校验异常的统一处理
     *
     * @param e 异常
     * @return R 统一返回结果
     */
    @ExceptionHandler(value = BindException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public R<Object> bindExceptionHandler(BindException e) {
        return validationFailed(e);
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public R<Object> methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
        return validationFailed(e);
    }

    @ExceptionHandler(value = MissingServletRequestParameterException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Object> missingServletRequestParameterExceptionHandler(
            MissingServletRequestParameterException e) {
        return R.fail(ErrorCode.BAD_REQUEST,
                e.getParameterName() + ": 请求参数不能为空");
    }

    @ExceptionHandler(value = MethodArgumentTypeMismatchException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Object> methodArgumentTypeMismatchExceptionHandler(
            MethodArgumentTypeMismatchException e) {
        return R.fail(ErrorCode.BAD_REQUEST,
                e.getName() + ": 请求参数格式不正确");
    }

    @ExceptionHandler(value = ConstraintViolationException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public R<Object> constraintViolationExceptionHandler(ConstraintViolationException e) {
        String errorMessage = e.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));

        return R.fail(ErrorCode.VALIDATION_FAILED, errorMessage);
    }

    @ExceptionHandler(value = CustomValidationException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public R<Object> customValidationExceptionHandler(CustomValidationException e) {
        return R.fail(ErrorCode.VALIDATION_FAILED, e.getMessage());
    }

    @ExceptionHandler(value = CustomUnauthorizedException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public R<Object> customTokenValidationExceptionHandler(CustomUnauthorizedException e) {
        return R.fail(ErrorCode.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(value = CustomForbiddenException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<Object> customForbiddenExceptionHandler(CustomForbiddenException e) {
        return R.fail(ErrorCode.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(value = CustomConflictException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.CONFLICT)
    public R<Object> customConflictExceptionHandler(CustomConflictException e) {
        return R.fail(ErrorCode.CONFLICT, e.getMessage());
    }

    /**
     * 捕捉到 CustomException 自定义异常的统一处理
     * 这是自定义类型异常的兜底操作，建议针对明确的异常类型做特定处理，方便返回更合适的状态码和消息
     *
     * @param e 异常
     * @return R 统一返回结果
     */
    @ExceptionHandler(value = CustomException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Object> customExceptionHandler(CustomException e) {
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Object> exceptionHandler(Exception e) {
        log.error("未处理异常", e);
        return R.fail(ErrorCode.INTERNAL_ERROR);
    }

    private R<Object> validationFailed(BindException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return R.fail(ErrorCode.VALIDATION_FAILED, errorMessage);
    }
}
