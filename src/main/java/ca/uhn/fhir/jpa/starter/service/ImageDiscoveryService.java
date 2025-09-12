package ca.uhn.fhir.jpa.starter.service;

import ca.uhn.fhir.jpa.starter.AppProperties;
import ca.uhn.fhir.jpa.starter.RabbitMQProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.*;

@Service
@Slf4j
public class ImageDiscoveryService {

	private final RabbitTemplate rabbitTemplate;
	private final AppProperties appProperties;
	private final RabbitMQProperties rabbitMQProperties;

	public static class ImageMessage implements Serializable {
		@JsonProperty("file_id")
		private final String file_id;

		@JsonProperty("register_type")
		private final String register_type;

		@JsonProperty("page_side")
		private final String page_side;

		public ImageMessage(String fileId, String registerType, String pageSide) {
			this.file_id = fileId;
			this.register_type = registerType;
			this.page_side = pageSide;
		}

		public String getFileId() {
			return file_id;
		}

		public String getRegisterType() {
			return register_type;
		}

		public String getPageSide() {
			return page_side;
		}
	}

	public ImageDiscoveryService(RabbitTemplate rabbitTemplate, AppProperties appProperties, RabbitMQProperties rabbitMQProperties) {
		this.rabbitTemplate = rabbitTemplate;
		this.appProperties = appProperties;
		this.rabbitMQProperties = rabbitMQProperties;
	}

	@Scheduled(fixedRate = 10000)
	public void discoverAndQueueImages() {
		Path sourcePath = Paths.get(appProperties.getImage_path());
		Path targetPath = Paths.get(appProperties.getProcessed_image_path());

		try {
			if (!Files.exists(sourcePath)) Files.createDirectories(sourcePath);
			if (!Files.exists(targetPath)) Files.createDirectories(targetPath);
		} catch (IOException e) {
			log.error("Could not create necessary image directories.", e);
			return;
		}

		File[] files = sourcePath.toFile().listFiles((dir, name) ->
			name.toLowerCase().endsWith(".jpg")
		);

		if (files == null || files.length == 0) {
			return;
		}

		log.info("Found {} new image(s) to process.", files.length);

		for (File file : files) {
			try {
				String filename = file.getName();
				String documentId = filename.substring(0, filename.lastIndexOf('.'));
				String[] parts = documentId.split("_");

				if (parts.length < 3) {
					log.error("Skipping malformed file: {}. Does not contain expected parts.", filename);
					continue;
				}

				String docType = parts[parts.length - 3];
				String docPart = parts[parts.length - 2];

				String pageSide;
				switch (docPart.toUpperCase()) {
					case "A":
						pageSide = "LEFT";
						break;
					case "B":
						pageSide = "RIGHT";
						break;
					default:
						pageSide = docPart;
						break;
				}

				ImageMessage messagePayload = new ImageMessage(documentId, docType, pageSide);

				String exchange = rabbitMQProperties.getExchange().getP2pExchange().getName();
				String routingKey = rabbitMQProperties.getBinding().getP2pImage().getName();

				rabbitTemplate.convertAndSend(exchange, routingKey, messagePayload);

				log.warn("Sent message to queue with file_id: '{}', register_type: '{}', page_side: '{}'",
					messagePayload.getFileId(),
					messagePayload.getRegisterType(),
					messagePayload.getPageSide()
				);


				Path destinationFile = targetPath.resolve(filename);
				Files.move(file.toPath(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
				log.info("Moved image '{}' to processed directory.", filename);

			} catch (Exception e) {
				log.error("Failed to process or move image file: {}", file.getName(), e);
			}
		}
	}
}