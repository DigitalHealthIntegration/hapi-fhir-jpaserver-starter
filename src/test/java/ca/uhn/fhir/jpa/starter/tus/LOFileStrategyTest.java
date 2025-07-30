package ca.uhn.fhir.jpa.starter.tus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class LOFileStrategyTest {

	private LOFileStrategy loFileStrategy;

	@BeforeEach
	void setUp() {
		loFileStrategy = new LOFileStrategy();
	}

	@Test
	void extractKeyValuesFromMetaData_testAllKeysPresent() {
		String encodedInput = "filename testfile.loraw calib_file_name calib.txt lo_cam_length 15 lo_cam_name locamera isCalibFile True";

		Map<String, String> result = loFileStrategy.extractKeyValuesFromMetaData(encodedInput);

		assertEquals("testfile.loraw", result.get("filename").trim());
		assertEquals("calib.txt", result.get("calib_file_name").trim());
		assertEquals("15", result.get("lo_cam_length").trim());
		assertEquals("locamera", result.get("lo_cam_name").trim());
		assertEquals("True", result.get("isCalibFile").trim());
	}

	@Test
	void extractKeyValuesFromMetaData_testSomeKeysMissing() {
		String encodedInput = "filename fileA lo_cam_length 30 isCalibFile False";

		Map<String, String> result = loFileStrategy.extractKeyValuesFromMetaData(encodedInput);

		assertEquals("fileA", result.get("filename").trim());
		assertEquals("30", result.get("lo_cam_length").trim());
		assertEquals("False", result.get("isCalibFile").trim());
		assertEquals("NA", result.get("calib_file_name").trim()); // missing
		assertEquals("NA", result.get("lo_cam_name").trim());     // missing
	}

	@Test
	void extractKeyValuesFromMetaData_testNoKeysPresent() {
		String encodedInput = "some random metadata string";

		Map<String, String> result = loFileStrategy.extractKeyValuesFromMetaData(encodedInput);

		for (String key : new String[]{"filename", "calib_file_name", "lo_cam_length", "lo_cam_name", "isCalibFile"}) {
			assertEquals("NA", result.get(key));
		}
	}

	@Test
	void extractKeyValuesFromMetaData_testEmptyInput() {
		String encodedInput = "";

		Map<String, String> result = loFileStrategy.extractKeyValuesFromMetaData(encodedInput);

		for (String key : new String[]{"filename", "calib_file_name", "lo_cam_length", "lo_cam_name", "isCalibFile"}) {
			assertEquals("NA", result.get(key));
		}
	}
}
