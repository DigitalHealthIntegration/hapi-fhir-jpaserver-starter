package ca.uhn.fhir.jpa.starter.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import java.util.List;
import java.util.Map;

@Getter
public class DocumentReviewPayload {
	private final String jobId;
	private final String imageUrl;
	private final Map<String, Object> context;
	private final List<String> headers;
	private final List<List<CellData>> rows;
	private final Map<String, Object> totals;

	@JsonCreator
	public DocumentReviewPayload(
		@JsonProperty("jobId") String jobId,
		@JsonProperty("imageUrl") String imageUrl,
		@JsonProperty("context") Map<String, Object> context,
		@JsonProperty("headers") List<String> headers,
		@JsonProperty("rows") List<List<CellData>> rows,
		@JsonProperty("totals") Map<String, Object> totals) {
		this.jobId = jobId;
		this.imageUrl = imageUrl;
		this.context = context;
		this.headers = headers;
		this.rows = rows;
		this.totals = totals;
	}
}