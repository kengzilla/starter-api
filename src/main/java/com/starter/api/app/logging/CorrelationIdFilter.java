package com.starter.api.app.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Assigns a stable {@value #HEADER_REQUEST_ID} for support correlation and puts it in MDC as {@code requestId}.
 * Distributed trace identifiers come from Micrometer/Brave ({@code traceId} in MDC) when present.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

	public static final String MDC_REQUEST_ID = "requestId";
	public static final String HEADER_REQUEST_ID = "X-Request-Id";
	private static final String HEADER_CORRELATION = "X-Correlation-Id";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String requestId = firstNonBlank(request.getHeader(HEADER_REQUEST_ID), request.getHeader(HEADER_CORRELATION));
		if (requestId == null) {
			requestId = UUID.randomUUID().toString();
		}
		MDC.put(MDC_REQUEST_ID, requestId);
		response.setHeader(HEADER_REQUEST_ID, requestId);
		try {
			filterChain.doFilter(request, response);
		} finally {
			MDC.remove(MDC_REQUEST_ID);
		}
	}

	private static String firstNonBlank(String a, String b) {
		if (a != null && !a.isBlank()) {
			return a.trim();
		}
		if (b != null && !b.isBlank()) {
			return b.trim();
		}
		return null;
	}
}
