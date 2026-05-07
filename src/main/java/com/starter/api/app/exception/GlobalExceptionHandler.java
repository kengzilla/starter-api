package com.starter.api.app.exception;

import com.starter.api.shared.api.ApiResponse;
import com.starter.api.shared.exception.ApiBusinessException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ApiBusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusiness(ApiBusinessException ex) {
		return ResponseEntity.status(ex.getStatus())
				.body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
		String msg = ex.getBindingResult().getFieldErrors().stream()
				.map(err -> err.getField() + ": " + err.getDefaultMessage())
				.collect(Collectors.joining("; "));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error("VALIDATION_ERROR", msg));
	}
}
