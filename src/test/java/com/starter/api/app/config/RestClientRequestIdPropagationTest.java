package com.starter.api.app.config;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.starter.api.app.logging.CorrelationIdFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@SpringBootTest
class RestClientRequestIdPropagationTest {

	@Autowired
	private ObjectProvider<RestClientCustomizer> restClientCustomizers;

	@AfterEach
	void clearMdc() {
		MDC.clear();
	}

	@Test
	void propagatesMdcRequestIdToOutboundHeader() {
		RestClient.Builder builder = RestClient.builder();
		restClientCustomizers.orderedStream().forEach(c -> c.customize(builder));
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		RestClient client = builder.baseUrl("http://localhost").build();

		MDC.put(CorrelationIdFilter.MDC_REQUEST_ID, "support-chain-7");
		server.expect(requestTo("http://localhost/x"))
				.andExpect(header(CorrelationIdFilter.HEADER_REQUEST_ID, "support-chain-7"))
				.andRespond(withSuccess());
		client.get().uri("/x").retrieve().toBodilessEntity();
		server.verify();
	}
}
