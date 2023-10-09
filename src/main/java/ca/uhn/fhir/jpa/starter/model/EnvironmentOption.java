package ca.uhn.fhir.jpa.starter.model;

import java.util.List;

public class EnvironmentOption {

	private String id;

	private String name;

	private List<EnvironmentOptionItem> options;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<EnvironmentOptionItem> getOptions() {
		return options;
	}

	public void setOptions(List<EnvironmentOptionItem> options) {
		this.options = options;
	}
}
