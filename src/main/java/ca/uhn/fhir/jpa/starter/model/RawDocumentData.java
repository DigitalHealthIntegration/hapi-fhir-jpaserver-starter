package ca.uhn.fhir.jpa.starter.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class RawDocumentData {
	private final List<String> headers;
	private final List<List<String>> rows;
	private final Map<String, Object> context;
	private final Map<String, Object> totals;

	@JsonCreator
	public RawDocumentData(
		@JsonProperty("headers") List<String> headers,
		@JsonProperty("rows") List<List<String>> rows,
		@JsonProperty("context") Map<String, Object> context,
		@JsonProperty("totals") Map<String, Object> totals
	) {
		this.headers = headers;
		this.rows = rows;
		this.context = context;
		this.totals = totals;
	}
}
