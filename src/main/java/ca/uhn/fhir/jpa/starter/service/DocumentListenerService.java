package ca.uhn.fhir.jpa.starter.service;

import ca.uhn.fhir.jpa.starter.AppProperties;
import ca.uhn.fhir.jpa.starter.model.DocumentQueuePayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

@Service
@Slf4j
public class DocumentListenerService {

	@Autowired
	private AppProperties appProperties;

	private final ObjectMapper objectMapper;
	private Path jsonDirectoryPath;

	private static final Map<String, String> REPLACEMENT_MAP;

	static {
		Map<String, String> map = new LinkedHashMap<>();
		map.put("<s_X>", "Unknown");
		map.put("<s_tick>", "Tick");
		map.put("<s_br>", "-");
		map.put("<s_illegible>", "Unknown");
		REPLACEMENT_MAP = Collections.unmodifiableMap(map);
	}

	public DocumentListenerService(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@PostConstruct
	public void initialize() {
		this.jsonDirectoryPath = Paths.get(appProperties.getP2p_json_path());
		try {
			if (!Files.exists(jsonDirectoryPath)) {
				Files.createDirectories(jsonDirectoryPath);
				log.info("Created JSON storage directory: {}", jsonDirectoryPath);
			}
		} catch (IOException e) {
			log.error("FATAL: Could not create JSON storage directory on startup.", e);
		}
	}

	@RabbitListener(queues = "${rabbitmq.queue.document.name}")
	public void receiveDocument(DocumentQueuePayload payload) {
		if (payload == null || payload.getRequest() == null || payload.getJsonContent() == null || payload.getRequest().get("file_id") == null) {
			log.error("Received malformed or incomplete message: {}", payload);
			return;
		}

		log.info("Processing document for fileId: {}, type: {}, part: {}",
			payload.getRequest().get("file_id"),
			payload.getRequest().get("register_type"),
			payload.getRequest().get("page_side"));

		try {
			Map<String, Object> finalJsonMap = new LinkedHashMap<>();
			finalJsonMap.putAll(payload.getRequest());
			finalJsonMap.putAll(payload.getJsonContent());

			JsonNode finalJsonNode = objectMapper.valueToTree(finalJsonMap);

			cleanJsonValues(finalJsonNode);

			String originalFilename = (String) payload.getRequest().get("file_id");

			String baseName = originalFilename.contains(".")
				? originalFilename.substring(0, originalFilename.lastIndexOf('.'))
				: originalFilename;

			String finalFilename = baseName + "_v0.json";
			Path filePath = jsonDirectoryPath.resolve(finalFilename);

			String prettyJsonContent = objectMapper.writerWithDefaultPrettyPrinter()
				.writeValueAsString(finalJsonNode);

			Files.write(filePath,
				prettyJsonContent.getBytes(StandardCharsets.UTF_8),
				StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING);

			log.info("Successfully saved merged JSON document to: {}", filePath);

		} catch (Exception e) {
			log.error("Error writing JSON file for payload: " + payload.toString(), e);
		}
	}

	private void cleanJsonValues(JsonNode node) {
		if (node == null) {
			return;
		}

		if (node.isObject()) {
			ObjectNode objectNode = (ObjectNode) node;
			List<String> fieldNames = new ArrayList<>();
			objectNode.fieldNames().forEachRemaining(fieldNames::add);

			for (String fieldName : fieldNames) {
				JsonNode childNode = objectNode.get(fieldName);
				if (childNode.isTextual()) {
					String cleanedText = applyReplacements(childNode.asText());
					if (!cleanedText.equals(childNode.asText())) {
						objectNode.put(fieldName, cleanedText);
					}
				} else {
					cleanJsonValues(childNode);
				}
			}
		} else if (node.isArray()) {
			ArrayNode arrayNode = (ArrayNode) node;
			for (int i = 0; i < arrayNode.size(); i++) {
				JsonNode element = arrayNode.get(i);
				if (element.isTextual()) {
					String cleanedText = applyReplacements(element.asText());
					if (!cleanedText.equals(element.asText())) {
						arrayNode.set(i, new TextNode(cleanedText));
					}
				} else {
					cleanJsonValues(element);
				}
			}
		}
	}

	private String applyReplacements(String originalText) {
		String result = originalText;
		for (Map.Entry<String, String> entry : REPLACEMENT_MAP.entrySet()) {
			result = result.replace(entry.getKey(), entry.getValue());
		}
		return result;
	}
}