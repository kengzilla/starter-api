package com.starter.api.app.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * One-line access log (INFO) and optional request/response bodies on server errors or when debug is enabled.
 */
public class HttpRequestLogFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(HttpRequestLogFilter.class);

	private final LoggingProperties loggingProperties;
	private final ObjectMapper objectMapper;

	public HttpRequestLogFilter(LoggingProperties loggingProperties, ObjectMapper objectMapper) {
		this.loggingProperties = loggingProperties;
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (!loggingProperties.isEnabled()) {
			filterChain.doFilter(request, response);
			return;
		}
		if (shouldNotFilter(request)) {
			filterChain.doFilter(request, response);
			return;
		}

		boolean captureBodies = shouldCaptureBodies(request);
		if (!captureBodies) {
			filterWithoutBodyCapture(request, response, filterChain);
			return;
		}

		int limit = Math.max(256, loggingProperties.getMaxBodyBytes());
		var wrappedRequest = new ContentCachingRequestWrapper(request, limit);
		var wrappedResponse = new ContentCachingResponseWrapper(response);
		long startNanos = System.nanoTime();
		try {
			filterChain.doFilter(wrappedRequest, wrappedResponse);
		} finally {
			long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
			int status = wrappedResponse.getStatus();
			logAccess(request, status, durationMs);
			if (shouldLogBodies(status)) {
				logBodies(wrappedRequest, wrappedResponse);
			}
			wrappedResponse.copyBodyToResponse();
		}
	}

	private void filterWithoutBodyCapture(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		long startNanos = System.nanoTime();
		chain.doFilter(request, response);
		long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
		logAccess(request, response.getStatus(), durationMs);
	}

	private boolean shouldCaptureBodies(HttpServletRequest request) {
		if (isSkipBodyPath(request.getRequestURI())) {
			return false;
		}
		return loggingProperties.isLogBodyDebug() || loggingProperties.isLogBodyOnError();
	}

	private static boolean isSkipBodyPath(String uri) {
		return uri.startsWith("/actuator")
				|| uri.startsWith("/swagger-ui")
				|| uri.startsWith("/v3/api-docs");
	}

	private boolean shouldLogBodies(int status) {
		if (loggingProperties.isLogBodyDebug()) {
			return true;
		}
		return loggingProperties.isLogBodyOnError() && status >= 500;
	}

	private void logAccess(HttpServletRequest request, int status, long durationMs) {
		log.info(
				"http_request method={} path={} status={} duration_ms={}",
				request.getMethod(),
				fullPath(request),
				status,
				durationMs);
	}

	private static String fullPath(HttpServletRequest request) {
		String uri = request.getRequestURI();
		String qs = request.getQueryString();
		return qs == null ? uri : uri + "?" + qs;
	}

	private void logBodies(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response) {
		int max = loggingProperties.getMaxBodyBytes();
		String contentType = request.getContentType();
		if (contentType != null && contentType.toLowerCase().startsWith("application/json")) {
			String body = SensitiveDataMasker.maskAndTruncate(
					request.getContentAsByteArray(), max, loggingProperties.getSensitiveFieldNames(), objectMapper);
			if (!body.isEmpty()) {
				log.warn("http_request_body {}", body);
			}
		}
		String respContentType = response.getContentType();
		if (respContentType != null && respContentType.toLowerCase().startsWith("application/json")) {
			String body = SensitiveDataMasker.maskAndTruncate(
					response.getContentAsByteArray(), max, loggingProperties.getSensitiveFieldNames(), objectMapper);
			if (!body.isEmpty()) {
				log.warn("http_response_body {}", body);
			}
		}
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return request.getRequestURI().startsWith("/actuator");
	}
}
