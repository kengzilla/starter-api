package com.starter.api.app.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.starter.api.testsupport.TestLoggingController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestLoggingController.class)
@ExtendWith(OutputCaptureExtension.class)
class LoggingIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void echoesRequestIdHeader() throws Exception {
		mockMvc.perform(get("/api/v1/health").header(CorrelationIdFilter.HEADER_REQUEST_ID, "support-123"))
				.andExpect(status().isOk())
				.andExpect(header().string(CorrelationIdFilter.HEADER_REQUEST_ID, "support-123"));
	}

	@Test
	void unexpectedErrorReturnsGenericEnvelope(CapturedOutput output) throws Exception {
		mockMvc.perform(get("/api/v1/_test/boom"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
				.andExpect(jsonPath("$.message").value("An unexpected error occurred"));
		assertThat(output.getOut()).contains("unhandled_exception");
	}

	@Test
	void businessErrorStays4xx(CapturedOutput output) throws Exception {
		mockMvc.perform(get("/api/v1/_test/business"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("TEST_NOT_FOUND"));
		assertThat(output.getOut()).contains("business_error");
	}

	@Test
	void logsMaskedRequestBodyOnServerError(CapturedOutput output) throws Exception {
		mockMvc.perform(post("/api/v1/_test/boom")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"password\":\"secret123\",\"name\":\"n1\"}"))
				.andExpect(status().isInternalServerError());
		assertThat(output.getOut()).contains("http_request_body");
		assertThat(output.getOut()).doesNotContain("secret123");
	}

	@Test
	void malformedJsonBodyReturns400(CapturedOutput output) throws Exception {
		mockMvc.perform(post("/api/v1/_test/boom")
						.contentType(MediaType.APPLICATION_JSON)
						.content("not-json"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
		assertThat(output.getOut()).contains("http_message_not_readable");
	}
}
