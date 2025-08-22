package ca.uhn.fhir.jpa.starter.model;

import lombok.Getter;

@Getter
public class CellData {
	private final String text;
	private final BoundingBox boundingBox;

	public CellData(String text, BoundingBox boundingBox){
		this.text = text;
		this.boundingBox = boundingBox;
	}
}
