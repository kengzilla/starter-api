package com.starter.api.app.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
@EnableConfigurationProperties(LoggingProperties.class)
public class LoggingFilterConfig {

	@Bean
	public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter() {
		var registration = new FilterRegistrationBean<CorrelationIdFilter>();
		registration.setFilter(new CorrelationIdFilter());
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 5);
		return registration;
	}

	@Bean
	public FilterRegistrationBean<HttpRequestLogFilter> httpRequestLogFilter(
			LoggingProperties loggingProperties, ObjectMapper objectMapper) {
		var registration = new FilterRegistrationBean<HttpRequestLogFilter>();
		registration.setFilter(new HttpRequestLogFilter(loggingProperties, objectMapper));
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
		return registration;
	}
}
