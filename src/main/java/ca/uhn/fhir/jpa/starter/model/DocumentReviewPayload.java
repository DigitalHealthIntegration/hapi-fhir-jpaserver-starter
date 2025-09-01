package ca.uhn.fhir.jpa.starter.model;

import lombok.Getter;
import java.util.List;
import java.util.Map;

@Getter
public class DocumentReviewPayload {

	private final String imageUrl;
	private final Map<String, Object> context;
	private final List<String> headers;
	private final List<List<CellData>> rows;
	private final Map<String, Object> totals;

	public DocumentReviewPayload(String imageUrl, Map<String, Object> context, List<String> headers, List<List<CellData>> rows, Map<String, Object> totals) {
		this.imageUrl = imageUrl;
		this.context = context;
		this.headers = headers;
		this.rows = rows;
		this.totals = totals;
	}
}