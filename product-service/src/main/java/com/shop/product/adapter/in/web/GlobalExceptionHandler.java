package com.shop.product.adapter.in.web;

import com.shop.product.adapter.in.web.dto.ErrorResponse;
import com.shop.product.domain.model.InsufficientStockException;
import com.shop.product.domain.model.IdempotencyConflictException;
import com.shop.product.domain.model.InvalidReservationTransitionException;
import com.shop.product.domain.model.ProductNotFoundException;
import com.shop.product.domain.model.ReservationNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/** Translates domain failures into HTTP status codes. The only place that knows HTTP. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleStock(InsufficientStockException ex) {
        return build(ErrorCode.INSUFFICIENT_STOCK, ex.getMessage());
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ProductNotFoundException ex) {
        return build(ErrorCode.PRODUCT_NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ReservationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReservationNotFound(
            ReservationNotFoundException ex) {
        return build(ErrorCode.RESERVATION_NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyConflict(
            IdempotencyConflictException ex) {
        return build(ErrorCode.IDEMPOTENCY_CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidReservationTransitionException.class)
    public ResponseEntity<ErrorResponse> handleTransition(
            InvalidReservationTransitionException ex) {
        return build(ErrorCode.INVALID_RESERVATION_TRANSITION, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(DataIntegrityViolationException ex) {
        return build(ErrorCode.IDEMPOTENCY_CONFLICT,
                "Idempotency-Key đã được dùng bởi request đồng thời khác");
    }

    /**
     * Two transactions read the same version and both tried to write. One won; this
     * is the loser. Nothing is corrupted — the caller may simply retry.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleConcurrentUpdate(OptimisticLockingFailureException ex) {
        return build(ErrorCode.CONCURRENT_UPDATE, "Stock changed concurrently, please retry");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return build(ErrorCode.INVALID_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fields.put(error.getField(), error.getDefaultMessage());
        }
        ErrorCode code = ErrorCode.VALIDATION_ERROR;
        return ResponseEntity.status(code.status())
                .body(ErrorResponse.of(code.name(), "Invalid request", fields));
    }

    private ResponseEntity<ErrorResponse> build(ErrorCode code, String message) {
        return ResponseEntity.status(code.status())
                .body(ErrorResponse.of(code.name(), message));
    }
}
