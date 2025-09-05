package ca.uhn.fhir.jpa.starter.service;

// The ImageQueuePayload import is no longer needed
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@Slf4j
public class ImageDiscoveryService {

	private final RabbitTemplate rabbitTemplate;

	@Value("${hapi.fhir.image_path}")
	private String unprocessedDir;

	@Value("${hapi.fhir.processed_image_path}")
	private String processedDir;

	@Value("${rabbitmq.exchange.p2pExchange.name}")
	private String exchange;

	@Value("${rabbitmq.binding.p2pImage.name}")
	private String imageRoutingKey;

	@Autowired
	public ImageDiscoveryService(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	@Scheduled(fixedRate = 10000)
	public void discoverAndQueueImages() {
		Path sourcePath = Paths.get(unprocessedDir);
		Path targetPath = Paths.get(processedDir);

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

		log.info("Found {} new image(s) to send.", files.length);

		for (File file : files) {
			try {
				String filename = file.getName();

				String documentId = filename.substring(0, filename.lastIndexOf('.'));

				rabbitTemplate.convertAndSend(exchange, imageRoutingKey, documentId);
				log.info("Sent documentId '{}' to queue '{}'.", documentId, "ImagesQueue");

				Path destinationFile = targetPath.resolve(filename);
				Files.move(file.toPath(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
				log.info("Moved image '{}' to processed directory.", filename);

			} catch (Exception e) {
				log.error("Failed to process or move image file: {}", file.getName(), e);
			}
		}
	}
}