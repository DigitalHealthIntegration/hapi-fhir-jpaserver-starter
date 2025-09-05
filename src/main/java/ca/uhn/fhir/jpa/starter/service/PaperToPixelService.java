package ca.uhn.fhir.jpa.starter.service;

import ca.uhn.fhir.jpa.starter.model.BoundingBox;
import ca.uhn.fhir.jpa.starter.model.CellData;
import ca.uhn.fhir.jpa.starter.model.DocumentReviewPayload;
import ca.uhn.fhir.jpa.starter.model.RawDocumentData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;

@Service
public class PaperToPixelService {

	private static final Logger logger = LoggerFactory.getLogger(PaperToPixelService.class);

	private final ObjectMapper objectMapper;

	@Value("${hapi.fhir.processed_image_path}")
	private String imageDir;

	@Value("${hapi.fhir.p2p_json_path}")
	private String jsonDir;

	@Autowired
	public PaperToPixelService(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public DocumentReviewPayload getDocumentReviewPayload(String documentId) {
		try {
			Path jsonPath = findLatestVersionPath(Paths.get(jsonDir), documentId);
			Path imagePath = Paths.get(imageDir,documentId + ".jpg");

			File jsonFile = jsonPath.toFile();
			if (!jsonFile.exists()) {
				throw new RuntimeException("JSON file not found for ID: " + documentId + "at path: " + jsonPath);
			}

			InputStream inputStream = Files.newInputStream(jsonFile.toPath());
			RawDocumentData rawData = objectMapper.readValue(inputStream, RawDocumentData.class);

			File imageFile = imagePath.toFile();
			if (!imageFile.exists()) {
				throw new RuntimeException("Image file not found for ID" + documentId);
			}

			BufferedImage image = ImageIO.read(imageFile);
			float imageWidth = image.getWidth();
			float imageHeight = image.getHeight();

			List<List<CellData>> rowsWithCoordinates = calculateCoordinates(rawData.getHeaders(),rawData.getRows(),imageWidth,imageHeight);

			String imageUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
				.path("/api/p2p/documents/")
				.path(documentId)
				.path("/image")
				.toUriString();

			return new DocumentReviewPayload(
				rawData.getJobId(),
				imageUrl,
				rawData.getContext(),
				rawData.getHeaders(),
				rowsWithCoordinates,
				rawData.getTotals()
			);


		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to read or parse document data file", e);
		}
	}

	/**
	 * Replicates the coordinate calculation logic from the Android application.
	 *
	 * @param headers The list of table headers.
	 * @param rows The raw table data (strings only).
	 * @param imageWidth The width of the source image.
	 * @param imageHeight The height of the source image.
	 * @return A list of rows where each cell contains its text and a calculated BoundingBox.
	 */
	private List<List<CellData>> calculateCoordinates(
		List<String> headers,
		List<List<String>> rows,
		float imageWidth,
		float imageHeight
	) {
		if (headers == null || rows == null) {
			return new ArrayList<>();
		}

		int numColumns = headers.size();
		int numDataRows = rows.size();

		if (numDataRows == 0 || numColumns == 0) {
			return new ArrayList<>();
		}

		int totalRowsInImage = numDataRows * 10;
		float rowHeight = imageHeight / totalRowsInImage;

		float horizontalMargin = -imageWidth * 0.5f;
		float usableWidth = imageWidth - (2 * horizontalMargin);
		float columnWidth = usableWidth / numColumns;

		List<List<CellData>> cellDataRows = new ArrayList<>();

		for (int rowIndex = 0; rowIndex < numDataRows; rowIndex++) {
			List<String> rowValues = rows.get(rowIndex);
			List<CellData> rowCells = new ArrayList<>();

			float top = rowIndex * rowHeight;
			float bottom = top + rowHeight;

			for (int colIndex = 0; colIndex < numColumns; colIndex++) {
				String text = (rowValues != null && colIndex < rowValues.size())
					? rowValues.get(colIndex)
					: "";
				float left = horizontalMargin + (colIndex * columnWidth);
				float right = left + columnWidth;

				BoundingBox boundingBox = new BoundingBox(left, top, right, bottom);
				rowCells.add(new CellData(text, boundingBox));
			}
			cellDataRows.add(rowCells);
		}

		return cellDataRows;
	}

	public Resource getDocumentImage(String documentId) {
		try {
			Path filePath = Paths.get(imageDir).resolve(documentId + ".jpg").normalize();
			Resource resource = new UrlResource(filePath.toUri());

			if (resource.exists() && resource.isReadable()) {
				return resource;
			} else {
				throw new RuntimeException("Could not read the image file for ID: " + documentId + " at path: " + filePath);
			}
		} catch (MalformedURLException e) {
			throw new RuntimeException("Error retrieving image for ID: " + documentId, e);
		}
	}

	private Path findLatestVersionPath(Path directory, String documentId){
		File dir = directory.toFile();
		final Path originalPath = directory.resolve(documentId + "_v0.json");

		if (!dir.exists() || !dir.isDirectory()) {
			return originalPath;
		}

		File[] matchingFiles = dir.listFiles((d, name) -> name.startsWith(documentId) && name.contains("_v"));
		if (matchingFiles == null || matchingFiles.length == 0) {
			return originalPath;
		}

		int maxVersion = -1;
		String latestFilename = "";
		for (File file : matchingFiles) {
			try {
				int version = Integer.parseInt(file.getName().substring(file.getName().lastIndexOf("_v") + 2).replace(".json", ""));
				if (version > maxVersion) {
					maxVersion = version;
					latestFilename = file.getName();
				}
			} catch (NumberFormatException e) {
				logger.warn("Could not parse version number from malformed filename: {}", file.getName());
			}
		}

		if (maxVersion != -1) {
			logger.info("Found latest version v{} for document {}", maxVersion, documentId);
			return directory.resolve(latestFilename);
		} else {
			return originalPath;
		}

	}
}
