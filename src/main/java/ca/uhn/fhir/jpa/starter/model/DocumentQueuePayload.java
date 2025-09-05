package ca.uhn.fhir.jpa.starter.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentQueuePayload implements Serializable {
	private String filename;
	private String jsonContent;
}