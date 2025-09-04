package ca.uhn.fhir.jpa.starter.tus;

import ca.uhn.fhir.jpa.starter.AppProperties;
import ca.uhn.fhir.jpa.starter.model.CellData;
import ca.uhn.fhir.jpa.starter.model.DocumentReviewPayload;
import ca.uhn.fhir.jpa.starter.model.RawDocumentData;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.desair.tus.server.TusFileUploadService;
import me.desair.tus.server.exception.TusException;
import me.desair.tus.server.upload.UploadInfo;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.common.lucene.store.InputStreamIndexInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class JsonFileStrategy implements FileStrategy {

	private static final Logger logger = LoggerFactory.getLogger(JsonFileStrategy.class);

	@Autowired
	private TusFileUploadService tusFileUploadService;
	@Autowired
	private AppProperties appProperties;
	@Autowired
	private FileStrategyContext fileStrategyContext;
	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public void transferToFinalStorage(String uploadUrl) throws TusException, IOException, UnsupportedAudioFileException {
		UploadInfo uploadInfo = tusFileUploadService.getUploadInfo(uploadUrl);
		if (uploadInfo == null || uploadInfo.isUploadInProgress()) {
			logger.warn("Upload is still in progress or info is null for URL: {}", uploadUrl);
			return;
		}

		Map<String, String> metadata = fileStrategyContext.getEncodedMetaData(uploadUrl);
		String originalFilename = new String(Base64.decodeBase64(metadata.get("filename")), Charsets.UTF_8);
		String documentId = originalFilename.replace("_edited.json", "");

		DocumentReviewPayload uploadedData;
		try (InputStream inputStream = tusFileUploadService.getUploadedBytes(uploadUrl)) {
			uploadedData = objectMapper.readValue(inputStream, DocumentReviewPayload.class);
		}

		List<List<String>> textOnlyRows = uploadedData.getRows().stream()
			.map(row -> row.stream().map(CellData::getText).collect(Collectors.toList()))
			.collect(Collectors.toList());

		RawDocumentData dataToSave = new RawDocumentData(
			uploadedData.getJobId(),
			uploadedData.getHeaders(),
			textOnlyRows,
			uploadedData.getContext(),
			uploadedData.getTotals()
		);

		Path destinationDirectory = Paths.get(appProperties.getP2p_json_path());
		int nextVersion = findNextVersion(destinationDirectory, documentId);
		String newFileName = String.format("%s_v%d.json",documentId,nextVersion);
		Path outputPath = destinationDirectory.resolve(newFileName);

		objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputPath.toFile(),dataToSave);
		logger.info("Successfully transformed and saved new version: {}", outputPath);

		tusFileUploadService.deleteUpload(uploadUrl);
		logger.info("Cleaned up temporary TUS files for {}", uploadUrl);
	}

	public int findNextVersion(Path directory, String documentId){
		File dir = directory.toFile();
		if (!dir.exists() || !dir.isDirectory()) {
			return 1;
		}

		File[] matchingFiles = dir.listFiles((d,name) -> name.startsWith(documentId) && name.contains("_v"));
		if(matchingFiles == null || matchingFiles.length == 0){
			return 1;
		}

		int maxVersion = Arrays.stream(matchingFiles)
			.map(file -> file.getName().substring(file.getName().lastIndexOf("_v") + 2).replace(".json", ""))
			.mapToInt(Integer::parseInt)
			.max()
			.orElse(0);

		return maxVersion + 1;
	}

}