package com.iprd.fhir.utils;

import java.util.List;
import java.util.ArrayList;
import java.lang.String;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Location.LocationMode;
import org.hl7.fhir.r4.model.Location.LocationStatus;
import org.hl7.fhir.r4.model.Practitioner.PractitionerQualificationComponent;
import java.text.SimpleDateFormat;  
import java.util.Date;  

public class FhirResourceTemplateHelper {
	private static String CODE_JDN = "jdn";
	private static String DISPLAY_JURISDICTION = "Jurisdiction";
	private static String SYSTEM_JURISDICTION = "http://hl7.org/fhir/ValueSet/location-physical-type";
	private static String CODE_CLINIC = "prov";
	private static String DISPLAY_CLINIC = "Healthcare Provider";
	private static String SYSTEM_CLINIC = "	http://hl7.org/fhir/ValueSet/organization-type";
	private static String SYSTEM_HCW = "https://www.iprdgroup.com/nigeria/oyo/ValueSet/Roles";
	
	public static Location state(String name)
	{
		Location state = new Location();
		Address stateAddress = new Address();
		CodeableConcept statePhysicalType = new CodeableConcept();
		Coding physicalTypeCoding = new Coding();
		physicalTypeCoding
		.setCode(CODE_JDN)
		.setDisplay(DISPLAY_JURISDICTION)
		.setSystem(SYSTEM_JURISDICTION);
		statePhysicalType.addCoding(physicalTypeCoding);
		stateAddress.setState(name);
		state.setName(name);
		state.setAddress(stateAddress);
		state.setStatus(LocationStatus.ACTIVE);
		state.setMode(LocationMode.INSTANCE);
		state.setPhysicalType(statePhysicalType);
		return state;
	}
	public static Location lga(String nameOfLga, String state) {
		Location lga = new Location();
		Address lgaAddress = new Address();
		CodeableConcept lgaPhysicalType = new CodeableConcept();
		Coding physicalTypeCoding = new Coding();
		physicalTypeCoding
		.setCode(CODE_JDN)
		.setDisplay(DISPLAY_JURISDICTION)
		.setSystem(SYSTEM_JURISDICTION);
		lgaPhysicalType.addCoding(physicalTypeCoding);
		lgaAddress.setState(state);
		lgaAddress.setDistrict(nameOfLga);
		lga.setName(nameOfLga);
		lga.setAddress(lgaAddress);
		lga.setStatus(LocationStatus.ACTIVE);
		lga.setMode(LocationMode.INSTANCE);
		lga.setPhysicalType(lgaPhysicalType);
		return lga;
	}
	public static Location ward(String state, String district, String city) {
		Location ward = new Location();
		Address wardAddress = new Address();
		CodeableConcept wardPhysicalType = new CodeableConcept();
		Coding physicalTypeCoding = new Coding();
		physicalTypeCoding
		.setCode(CODE_JDN)
		.setDisplay(DISPLAY_JURISDICTION)
		.setSystem(SYSTEM_JURISDICTION);
		wardPhysicalType.addCoding(physicalTypeCoding);
		wardAddress.setState(state);
		wardAddress.setCity(city);
		wardAddress.setDistrict(district);
		ward.setName(city);
		ward.setAddress(wardAddress);
		ward.setStatus(LocationStatus.ACTIVE);
		ward.setMode(LocationMode.INSTANCE);
		ward.setPhysicalType(wardPhysicalType);
		return ward;
	}
	
	public static Organization clinic(String nameOfClinic,String facilityUID,String facilityCode ,String countryCode, String contact, String state, String district, String city) {
		Organization clinic = new Organization();
		List<CodeableConcept> codeableConcepts = new ArrayList<>();
		List<Address> addresses = new ArrayList<>();
		Address address = new Address();
		address.setState(state);
		address.setDistrict(district);
		address.setCity(city);
		addresses.add(address);
		clinic.setAddress(addresses);
		List<Identifier> identifiers = new ArrayList<>();
		Identifier facilityUIDIdentifier = new Identifier();
		Identifier facilityCodeIdentifier = new Identifier();
		facilityUIDIdentifier.setId(facilityCode);
		facilityCodeIdentifier.setId(facilityUID);
		identifiers.add(facilityUIDIdentifier);
		identifiers.add(facilityCodeIdentifier);
		clinic.setIdentifier(identifiers);
		List<ContactPoint> contactPoints = new ArrayList<>();
		ContactPoint contactPoint = new ContactPoint();
		contactPoint.setValue(countryCode+contact);
		contactPoints.add(contactPoint);
		clinic.setTelecom(contactPoints);
		CodeableConcept codeableConcept = new CodeableConcept();
		Coding coding = new Coding();
		coding.setCode(CODE_CLINIC);
		coding.setSystem(SYSTEM_CLINIC);
		coding.setDisplay(DISPLAY_CLINIC);
		codeableConcept.addCoding(coding);
		codeableConcept.setText(DISPLAY_CLINIC);
		codeableConcepts.add(codeableConcept);
		clinic.setType(codeableConcepts);
		clinic.setName(nameOfClinic);
		return clinic;
	}
	
	public static Practitioner hcw(String firstName, String lastName, String telecom, String countryCode, String gender, String dob, String state, String lga, String ward, String facilityUID, String role, String qualification) throws Exception {
		Practitioner practitioner = new Practitioner();
		List<Identifier> identifiers = new ArrayList<>();
		Identifier clinicId = new Identifier();
		clinicId.setId(facilityUID);
		identifiers.add(clinicId);
		practitioner.setIdentifier(identifiers);
		List<HumanName> hcwName = new ArrayList<>();
		HumanName humanName = new HumanName();
		List<StringType> list = new ArrayList<>();
		list.add(new StringType(firstName));
		humanName.setGiven(list);
		humanName.setFamily(lastName);
		hcwName.add(humanName);
		practitioner.setName(hcwName);
		List<ContactPoint> contactPoints = new ArrayList<>();
		ContactPoint contactPoint = new ContactPoint();
		contactPoint.setValue(countryCode+telecom);
		contactPoints.add(contactPoint);
		practitioner.setTelecom(contactPoints);
		practitioner.setGender(AdministrativeGender.fromCode(gender));
		CodeableConcept codeableConcept = new CodeableConcept();
		Coding coding = new Coding();
		coding.setCode(qualification);
		coding.setDisplay(qualification);
		coding.setSystem(SYSTEM_HCW);
		codeableConcept.addCoding(coding);
		List<PractitionerQualificationComponent> practitionerQualificationComponents = new ArrayList<>();
		PractitionerQualificationComponent qualificationComponent = new PractitionerQualificationComponent();
		qualificationComponent.setCode(codeableConcept);
		practitionerQualificationComponents.add(qualificationComponent);
		practitioner.setQualification(practitionerQualificationComponents);
		Date dateOfBirth = new SimpleDateFormat("MM/dd/yyyy").parse(dob);
		practitioner.setBirthDate(dateOfBirth);
		return practitioner;
	}
	
	public static PractitionerRole practitionerRole(String role, String qualification)
	{
		PractitionerRole practitionerRole = new PractitionerRole();
		List<CodeableConcept> codeableConcepts = new ArrayList<>();
		CodeableConcept roleCoding = new CodeableConcept();
		Coding coding2 = new Coding();
		coding2.setCode(role);
		coding2.setDisplay(qualification);
		coding2.setSystem(SYSTEM_HCW);
		roleCoding.addCoding(coding2);
		codeableConcepts.add(roleCoding);
		practitionerRole.setCode(codeableConcepts);
		return practitionerRole;
	}
}
