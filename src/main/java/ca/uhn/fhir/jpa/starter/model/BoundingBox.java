package ca.uhn.fhir.jpa.starter.model;

import lombok.Getter;

@Getter
public class BoundingBox {
	private final float left;
	private final float top;
	private final float right;
	private final float bottom;

	public BoundingBox(float left, float top, float right, float bottom) {
		this.left = left;
		this.top = top;
		this.right = right;
		this.bottom = bottom;
	}
}
