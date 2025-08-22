package ca.uhn.fhir.jpa.starter.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class RawDocumentData {
	private final List<String> headers;
	private final List<List<String>> rows; // <-- This MUST be List<List<String>>

	@JsonCreator
	public RawDocumentData(@JsonProperty("headers") List<String> headers, @JsonProperty("rows") List<List<String>> rows) {
		this.headers = headers;
		this.rows = rows;
	}
}
