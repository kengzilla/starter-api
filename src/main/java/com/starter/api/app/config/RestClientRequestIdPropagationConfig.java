package com.starter.api.app.config;

import com.starter.api.app.logging.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Forwards the inbound {@link CorrelationIdFilter} correlation id to downstream HTTP calls made with
 * {@link org.springframework.web.client.RestClient} built from the auto-configured {@link org.springframework.web.client.RestClient.Builder}.
 */
@Configuration
public class RestClientRequestIdPropagationConfig {

	@Bean
	public RestClientCustomizer propagateRequestIdHeader() {
		return builder -> builder.requestInterceptor((request, body, execution) -> {
			String rid = MDC.get(CorrelationIdFilter.MDC_REQUEST_ID);
			if (StringUtils.hasText(rid)) {
				request.getHeaders().set(CorrelationIdFilter.HEADER_REQUEST_ID, rid);
			}
			return execution.execute(request, body);
		});
	}
}
