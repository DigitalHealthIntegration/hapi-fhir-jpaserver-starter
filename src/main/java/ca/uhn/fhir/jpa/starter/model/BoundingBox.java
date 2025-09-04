package ca.uhn.fhir.jpa.starter.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class BoundingBox {
	private final float left;
	private final float top;
	private final float right;
	private final float bottom;

	@JsonCreator
	public BoundingBox(
		@JsonProperty("left") float left,
		@JsonProperty("top") float top,
		@JsonProperty("right") float right,
		@JsonProperty("bottom") float bottom) {
		this.left = left;
		this.top = top;
		this.right = right;
		this.bottom = bottom;
	}
}