package ca.uhn.fhir.jpa.starter.service;

import ca.uhn.fhir.jpa.starter.AppProperties;
import ca.uhn.fhir.jpa.starter.model.DocumentQueuePayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

@Service
@Slf4j
public class DocumentListenerService {

	@Autowired
	private AppProperties appProperties;

	private final ObjectMapper objectMapper;
	private Path jsonDirectoryPath;

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
		if (payload == null || payload.getFilename() == null || payload.getJsonContent() == null) {
			log.error("Received malformed or incomplete message: {}", payload);
			return;
		}

		try {
			String originalFilename = payload.getFilename();
			String baseName = originalFilename.contains(".")
				? originalFilename.substring(0, originalFilename.lastIndexOf('.'))
				: originalFilename;

			String finalFilename = baseName + "_v0.json";
			Path filePath = jsonDirectoryPath.resolve(finalFilename);

			String prettyJsonContent = objectMapper.writerWithDefaultPrettyPrinter()
				.writeValueAsString(payload.getJsonContent());

			Files.write(filePath,
				prettyJsonContent.getBytes(StandardCharsets.UTF_8),
				StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING);

			log.info("Successfully saved JSON document to: {}", filePath);

		} catch (Exception e) {
			log.error("Error writing JSON file", e);
		}
	}
}
