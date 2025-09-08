package ca.uhn.fhir.jpa.starter.service;

import ca.uhn.fhir.jpa.starter.AppProperties;
import ca.uhn.fhir.jpa.starter.RabbitMQProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

@Service
@Slf4j
public class ImageDiscoveryService {

	private final RabbitTemplate rabbitTemplate;
	private final AppProperties appProperties;
	private final RabbitMQProperties rabbitMQProperties;

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

		log.info("Found {} new image(s) to send.", files.length);

		for (File file : files) {
			try {
				String filename = file.getName();

				String documentId = filename.substring(0, filename.lastIndexOf('.'));

				String exchange = rabbitMQProperties.getExchange().getP2pExchange().getName();
				String routingKey = rabbitMQProperties.getBinding().getP2pImage().getName();

				rabbitTemplate.convertAndSend(exchange, routingKey, documentId);
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