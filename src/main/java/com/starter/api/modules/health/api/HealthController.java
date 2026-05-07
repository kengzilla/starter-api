package com.starter.api.modules.health.api;

import com.starter.api.shared.api.ApiResponse;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

	@GetMapping
	public ApiResponse<HealthStatusResponse> health() {
		HealthStatusResponse data = new HealthStatusResponse("UP", "starter-api", Instant.now());
		return ApiResponse.success(data);
	}

	public record HealthStatusResponse(String status, String service, Instant checkedAt) {
	}
}
