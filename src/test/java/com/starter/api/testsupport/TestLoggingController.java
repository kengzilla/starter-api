package com.starter.api.testsupport;

import com.starter.api.shared.exception.ApiBusinessException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only endpoints for logging / error-handler coverage. Imported explicitly by tests.
 */
@RestController
@RequestMapping("/api/v1/_test")
public class TestLoggingController {

	@GetMapping("/boom")
	public void boom() {
		throw new RuntimeException("intentional test failure");
	}

	@PostMapping(value = "/boom", consumes = MediaType.APPLICATION_JSON_VALUE)
	public void boomPost(@SuppressWarnings("unused") @RequestBody Map<String, Object> body) {
		throw new RuntimeException("intentional post failure");
	}

	@GetMapping("/business")
	public void business() {
		throw new ApiBusinessException(HttpStatus.NOT_FOUND, "TEST_NOT_FOUND", "missing entity");
	}
}
