package com.starter.api.app.exception;

import com.starter.api.shared.api.ApiResponse;
import com.starter.api.shared.exception.ApiBusinessException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	private static final String INTERNAL_ERROR_CODE = "INTERNAL_ERROR";
	private static final String INTERNAL_ERROR_MESSAGE = "An unexpected error occurred";

	@ExceptionHandler(ApiBusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusiness(ApiBusinessException ex) {
		log.warn("business_error code={} httpStatus={} message={}", ex.getErrorCode(), ex.getStatus().value(), ex.getMessage());
		return ResponseEntity.status(ex.getStatus())
				.body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
		String msg = ex.getBindingResult().getFieldErrors().stream()
				.map(err -> err.getField() + ": " + err.getDefaultMessage())
				.collect(Collectors.joining("; "));
		log.warn("validation_error {}", summarize(msg, 2000));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error("VALIDATION_ERROR", msg));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException ex) {
		log.warn("http_message_not_readable {}", summarize(ex.getMostSpecificCause().getMessage(), 500));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error("BAD_REQUEST", "Malformed request body"));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
		log.error("unhandled_exception", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error(INTERNAL_ERROR_CODE, INTERNAL_ERROR_MESSAGE));
	}

	private static String summarize(String raw, int maxLen) {
		if (raw == null) {
			return "";
		}
		if (raw.length() <= maxLen) {
			return raw;
		}
		return raw.substring(0, maxLen) + "...(truncated)";
	}
}
