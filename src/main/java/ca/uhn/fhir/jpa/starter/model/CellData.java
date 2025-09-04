package ca.uhn.fhir.jpa.starter.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class CellData {
	private final String text;
	private final BoundingBox boundingBox;

	@JsonCreator
	public CellData(
		@JsonProperty("text") String text,
		@JsonProperty("boundingBox") BoundingBox boundingBox){
		this.text = text;
		this.boundingBox = boundingBox;
	}
}