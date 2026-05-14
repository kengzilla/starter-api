package com.starter.api.app.logging;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.logging")
public class LoggingProperties {

	private boolean enabled = true;
	private boolean logBodyOnError = true;
	private boolean logBodyDebug = false;
	private int maxBodyBytes = 8192;
	private List<String> sensitiveFieldNames = defaultSensitiveNames();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isLogBodyOnError() {
		return logBodyOnError;
	}

	public void setLogBodyOnError(boolean logBodyOnError) {
		this.logBodyOnError = logBodyOnError;
	}

	public boolean isLogBodyDebug() {
		return logBodyDebug;
	}

	public void setLogBodyDebug(boolean logBodyDebug) {
		this.logBodyDebug = logBodyDebug;
	}

	public int getMaxBodyBytes() {
		return maxBodyBytes;
	}

	public void setMaxBodyBytes(int maxBodyBytes) {
		this.maxBodyBytes = maxBodyBytes;
	}

	public List<String> getSensitiveFieldNames() {
		return sensitiveFieldNames;
	}

	public void setSensitiveFieldNames(List<String> sensitiveFieldNames) {
		this.sensitiveFieldNames = sensitiveFieldNames != null ? new ArrayList<>(sensitiveFieldNames) : defaultSensitiveNames();
	}

	private static List<String> defaultSensitiveNames() {
		return new ArrayList<>(List.of(
				"password",
				"token",
				"accessToken",
				"refreshToken",
				"authorization",
				"secret",
				"clientSecret"));
	}
}
