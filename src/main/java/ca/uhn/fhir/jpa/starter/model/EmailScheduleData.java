package ca.uhn.fhir.jpa.starter.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailScheduleData {
	private Integer id;
	private String orgId;
	private String recipientEmail;
	private String emailSubject;
	private String scheduleType;
	private Timestamp updatedAt;
	private String cronExpression; // FIX: Added the missing field

	@Override
	public String toString() {
		return String.format("EmailScheduleData{id=%d, orgId='%s', scheduleType='%s', cronExpression='%s'}",
			id, orgId, scheduleType, cronExpression);
	}
}