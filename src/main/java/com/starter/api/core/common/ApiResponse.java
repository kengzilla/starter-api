package com.starter.api.core.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
		boolean success,
		T data,
		String errorCode,
		String message,
		Instant timestamp) {

	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(true, data, null, "OK", Instant.now());
	}

	public static <T> ApiResponse<T> success(T data, String message) {
		return new ApiResponse<>(true, data, null, message, Instant.now());
	}

	public static <T> ApiResponse<T> error(String errorCode, String message) {
		return new ApiResponse<>(false, null, errorCode, message, Instant.now());
	}

	public static <T> ApiResponse<T> error(String errorCode, String message, T data) {
		return new ApiResponse<>(false, data, errorCode, message, Instant.now());
	}
}
