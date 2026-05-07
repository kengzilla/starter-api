package com.starter.api.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiBusinessException extends RuntimeException {

	private final HttpStatus status;
	private final String errorCode;

	public ApiBusinessException(HttpStatus status, String errorCode, String message) {
		super(message);
		this.status = status;
		this.errorCode = errorCode;
	}
}
