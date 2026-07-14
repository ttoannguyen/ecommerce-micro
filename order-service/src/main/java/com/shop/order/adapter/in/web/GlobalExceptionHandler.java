package com.shop.order.adapter.in.web;

import com.shop.order.adapter.in.web.dto.ErrorResponse;
import com.shop.order.domain.model.InsufficientStockException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/** Dịch lỗi miền -> mã HTTP. Nơi duy nhất biết HTTP status. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleStock(InsufficientStockException ex) {
        return build(ErrorCode.INSUFFICIENT_STOCK, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return build(ErrorCode.INVALID_REQUEST, ex.getMessage());
    }

    /** @Valid trượt: gom lỗi từng field thay vì ném một chuỗi khó đọc. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fields.put(error.getField(), error.getDefaultMessage());
        }
        ErrorCode code = ErrorCode.VALIDATION_ERROR;
        return ResponseEntity.status(code.status())
                .body(ErrorResponse.of(code.name(), "Dữ liệu không hợp lệ", fields));
    }

    private ResponseEntity<ErrorResponse> build(ErrorCode code, String message) {
        return ResponseEntity.status(code.status())
                .body(ErrorResponse.of(code.name(), message));
    }
}
