package ca.uhn.fhir.jpa.starter.service;

import ca.uhn.fhir.jpa.starter.model.DocumentQueuePayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Service
@Slf4j
public class DocumentListenerService {

	@Value("${hapi.fhir.p2p_json_path}")
	private String jsonDir;

	@RabbitListener(queues = "${rabbitmq.queue.document.name}")
	public void receiveDocument(DocumentQueuePayload payload) {
		if (payload == null || payload.getFilename() == null || payload.getJsonContent() == null) {
			log.error("Received a malformed or incomplete message. Discarding message: {}", payload);
			return;
		}

		String originalFilename = payload.getFilename();
		log.info("Received document with base name '{}' from queue '{}'.", originalFilename, "DocumentsQueue");

		try {
			String versionedFilename = originalFilename + "_v0.json";

			Path jsonDirPath = Paths.get(jsonDir);
			if (!Files.exists(jsonDirPath)) {
				Files.createDirectories(jsonDirPath);
			}

			Path filePath = jsonDirPath.resolve(versionedFilename);

			Files.write(
				filePath,
				payload.getJsonContent().getBytes(StandardCharsets.UTF_8),
				StandardOpenOption.CREATE,
				StandardOpenOption.WRITE,
				StandardOpenOption.TRUNCATE_EXISTING
			);

			log.info("Successfully saved versioned JSON document to: {}", filePath);

		} catch (IOException e) {
			log.error("Failed to save JSON file for base name '{}' to directory '{}'", originalFilename, jsonDir, e);
		}
	}
}