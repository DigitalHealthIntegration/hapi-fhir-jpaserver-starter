package com.iprd.fhir.utils;

import java.util.*;
import java.lang.String;

import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Location.LocationMode;
import org.hl7.fhir.r4.model.Location.LocationStatus;
import org.hl7.fhir.r4.model.Practitioner.PractitionerQualificationComponent;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class FhirResourceTemplateHelper {
	private static String CODE_JDN = "jdn";
	private static String CODE_BU = "bu";
	private static String DISPLAY_BUILDING = "building";
	private static String DISPLAY_JURISDICTION = "Jurisdiction";
	private static String SYSTEM_LOCATION_PHYSICAL_TYPE = "http://hl7.org/fhir/ValueSet/location-physical-type";
	private static String CODE_CLINIC = "prov";
	private static String DISPLAY_CLINIC = "Healthcare Provider";
	private static String SYSTEM_ORGANIZATION_PHYSICAL_TYPE = "	http://hl7.org/fhir/ValueSet/organization-type";
	private static String SYSTEM_HCW = "https://www.iprdgroup.com/nigeria/oyo/ValueSet/Roles";
	private static String IDENTIFIER_SYSTEM = "http://www.iprdgroup.com/Identifier/System";
	
	public static Organization state(String name)
	{
		Organization state = new Organization();
		List<CodeableConcept> codeableConcepts = new ArrayList<>();
		List<Address> addresses = new ArrayList<>();
		Address stateAddress = new Address();
		stateAddress.setState(name);
		addresses.add(stateAddress);
		state.setAddress(addresses);
		CodeableConcept statePhysicalType = new CodeableConcept();
		Coding physicalTypeCoding = new Coding();
		physicalTypeCoding
		.setCode(CODE_JDN)
		.setDisplay(DISPLAY_JURISDICTION)
		.setSystem(SYSTEM_ORGANIZATION_PHYSICAL_TYPE);
		statePhysicalType.addCoding(physicalTypeCoding);
		statePhysicalType.setText(DISPLAY_JURISDICTION);
		codeableConcepts.add(statePhysicalType);
		state.setType(codeableConcepts);
		state.setName(name);
		state.setId(new IdType("Organization", generateUUID()));
		return state;
	}
	
	public static Organization lga(String nameOfLga, String state, String stateId) {
		Organization lga = new Organization();
		List<CodeableConcept> codeableConcepts = new ArrayList<>();
		List<Address> addresses = new ArrayList<>();
		Address lgaAddress = new Address();
		lgaAddress.setState(state);
		lgaAddress.setDistrict(nameOfLga);
		addresses.add(lgaAddress);
		lga.setAddress(addresses);
		CodeableConcept lgaPhysicalType = new CodeableConcept();
		Coding physicalTypeCoding = new Coding();
		physicalTypeCoding
		.setCode(CODE_JDN)
		.setDisplay(DISPLAY_JURISDICTION)
		.setSystem(SYSTEM_ORGANIZATION_PHYSICAL_TYPE);
		lgaPhysicalType.addCoding(physicalTypeCoding);
		lgaPhysicalType.setText(DISPLAY_JURISDICTION);
		codeableConcepts.add(lgaPhysicalType);
		lga.setType(codeableConcepts);
		lga.setName(nameOfLga);
		lga.setId(new IdType("Organization", generateUUID()));
		lga.setPartOf(new Reference("Organization/" + stateId));
		return lga;
	}
	
	public static Organization ward(String state, String district, String city, String lgaId) {
		Organization ward = new Organization();
		List<CodeableConcept> codeableConcepts = new ArrayList<>();
		List<Address> addresses = new ArrayList<>();
		Address wardAddress = new Address();
		wardAddress.setState(state);
		wardAddress.setDistrict(district);
		wardAddress.setCity(city);
		addresses.add(wardAddress);
		ward.setAddress(addresses);
		CodeableConcept wardPhysicalType = new CodeableConcept();
		Coding physicalTypeCoding = new Coding();
		physicalTypeCoding
			.setCode(CODE_JDN)
			.setDisplay(DISPLAY_JURISDICTION)
			.setSystem(SYSTEM_ORGANIZATION_PHYSICAL_TYPE);
		wardPhysicalType.addCoding(physicalTypeCoding);
		wardPhysicalType.setText(DISPLAY_JURISDICTION);
		codeableConcepts.add(wardPhysicalType);
		ward.setType(codeableConcepts);
		ward.setName(city);
		ward.setId(new IdType("Organization", generateUUID()));
		ward.setPartOf(new Reference("Organization/" + lgaId));
		return ward;
	}
	
	public static Location clinic(String state, String district, String city, String clinic) {
		Location facility = new Location();
		Address facilityAddress = new Address();
		CodeableConcept facilityPhysicalType = new CodeableConcept();
		Coding physicalTypeCoding = new Coding();
		physicalTypeCoding
		.setCode(CODE_BU)
		.setDisplay(DISPLAY_BUILDING)
		.setSystem(SYSTEM_LOCATION_PHYSICAL_TYPE);
		facilityPhysicalType.addCoding(physicalTypeCoding);
		facilityAddress.setState(state);
		facilityAddress.setCity(city);
		facilityAddress.setDistrict(district);
		facility.setName(clinic);
		facility.setId(new IdType("Location", generateUUID()));
		facility.setAddress(facilityAddress);
		facility.setStatus(LocationStatus.ACTIVE);
		facility.setMode(LocationMode.INSTANCE);
		facility.setPhysicalType(facilityPhysicalType);
		return facility;
	}
	
	public static Organization clinic(String nameOfClinic,String facilityUID,String facilityCode ,String countryCode, String contact, String state, String district, String city, String wardId) {
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
		facilityUIDIdentifier.setSystem(IDENTIFIER_SYSTEM+"/facilityCode");
		facilityUIDIdentifier.setId(facilityCode);
		facilityCodeIdentifier.setSystem(IDENTIFIER_SYSTEM+"/facilityUID");
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
		coding.setSystem(SYSTEM_ORGANIZATION_PHYSICAL_TYPE);
		coding.setDisplay(DISPLAY_CLINIC);
		codeableConcept.addCoding(coding);
		codeableConcept.setText(DISPLAY_CLINIC);
		codeableConcepts.add(codeableConcept);
		clinic.setType(codeableConcepts);
		clinic.setName(nameOfClinic);
		clinic.setId(new IdType("Organization", generateUUID()));
		clinic.setPartOf(new Reference("Organization/"+ wardId));
		return clinic;
	}
	
	public static Practitioner hcw(String firstName,String lastName, String telecom, String countryCode, String gender, String dob, String state, String lga, String ward, String facilityUID, String role, String qualification, String stateIdentifierString) throws Exception {
		Practitioner practitioner = new Practitioner();
		List<Identifier> identifiers = new ArrayList<>();
		Identifier clinicIdentifier = new Identifier();
		clinicIdentifier.setSystem(IDENTIFIER_SYSTEM+"/facilityUID");
		clinicIdentifier.setId(facilityUID);
		Identifier stateIdentifier = new Identifier();
		stateIdentifier.setSystem(IDENTIFIER_SYSTEM+"/stateIdentifier");
		stateIdentifier.setId(stateIdentifierString);
		identifiers.add(clinicIdentifier);
		identifiers.add(stateIdentifier);
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
		contactPoint.setSystem(org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem.PHONE);
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
		try {
			Date dateOfBirth = new SimpleDateFormat("dd/MM/yyyy").parse(dob);
			practitioner.setBirthDate(dateOfBirth);
		}catch(ParseException exception) {
			exception.printStackTrace();
		}
		practitioner.setId(new IdType("Practitioner", generateUUID()));
		return practitioner;
	}
	
	public static PractitionerRole practitionerRole(String role, String qualification, String practitionerId)
	{
		PractitionerRole practitionerRole = new PractitionerRole();
		Reference PractitionerReference = new  Reference("Practitioner/"+practitionerId);
		List<CodeableConcept> codeableConcepts = new ArrayList<>();
		CodeableConcept roleCoding = new CodeableConcept();
		Coding coding2 = new Coding();
		coding2.setCode(role);
		coding2.setDisplay(qualification);
		coding2.setSystem(SYSTEM_HCW);
		roleCoding.addCoding(coding2);
		codeableConcepts.add(roleCoding);
		practitionerRole.setCode(codeableConcepts);
		practitionerRole.setPractitioner(PractitionerReference);
		practitionerRole.setId(new IdType("PractitionerRole", generateUUID()));
		return practitionerRole;
	}
	
	private static String generateUUID() {
		return UUID.randomUUID().toString();
	}
}
