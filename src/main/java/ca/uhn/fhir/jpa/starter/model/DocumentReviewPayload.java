package ca.uhn.fhir.jpa.starter.model;

import lombok.Getter;
import java.util.List;

@Getter

public class DocumentReviewPayload {

	private final String imageUrl;
	private final List<String> headers;
	private final List<List<CellData>> rows;

	public DocumentReviewPayload(String imageUrl, List<String> headers, List<List<CellData>> rows) {
		this.imageUrl = imageUrl;
		this.headers = headers;
		this.rows = rows;
	}
}