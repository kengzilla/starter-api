package com.starter.api.app.logging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class SensitiveDataMasker {

	private static final String MASK = "***";

	private SensitiveDataMasker() {
	}

	static String maskAndTruncate(byte[] raw, int maxBytes, List<String> sensitiveFieldNames, ObjectMapper objectMapper) {
		if (raw == null || raw.length == 0) {
			return "";
		}
		int len = Math.min(raw.length, Math.max(0, maxBytes));
		String text = new String(raw, 0, len, StandardCharsets.UTF_8);
		boolean truncated = raw.length > len;
		String masked = maskJsonIfPossible(text, sensitiveFieldNames, objectMapper);
		if (truncated) {
			return masked + "...(truncated," + raw.length + " bytes total)";
		}
		return masked;
	}

	private static String maskJsonIfPossible(String text, List<String> sensitiveFieldNames, ObjectMapper objectMapper) {
		String trimmed = text.trim();
		if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
			return maskPlainKeywords(text);
		}
		try {
			JsonNode root = objectMapper.readTree(trimmed);
			Set<String> lowerNames = toLowerSet(sensitiveFieldNames);
			redactNode(root, lowerNames);
			return objectMapper.writeValueAsString(root);
		} catch (Exception ignored) {
			return maskPlainKeywords(text);
		}
	}

	private static void redactNode(JsonNode node, Set<String> sensitiveLower) {
		if (node.isObject()) {
			ObjectNode obj = (ObjectNode) node;
			var it = obj.fields();
			while (it.hasNext()) {
				var e = it.next();
				String key = e.getKey();
				if (sensitiveLower.contains(key.toLowerCase(Locale.ROOT))) {
					obj.put(key, MASK);
				} else {
					redactNode(e.getValue(), sensitiveLower);
				}
			}
		} else if (node.isArray()) {
			ArrayNode arr = (ArrayNode) node;
			for (JsonNode child : arr) {
				redactNode(child, sensitiveLower);
			}
		}
	}

	private static String maskPlainKeywords(String text) {
		return text;
	}

	private static Set<String> toLowerSet(List<String> names) {
		Set<String> s = new HashSet<>();
		for (String n : names) {
			if (n != null && !n.isBlank()) {
				s.add(n.toLowerCase(Locale.ROOT));
			}
		}
		return s;
	}
}
