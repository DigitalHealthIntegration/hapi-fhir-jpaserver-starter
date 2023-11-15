package com.iprd.fhir.utils;

import com.iprd.report.OrgType;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Location.LocationMode;
import org.hl7.fhir.r4.model.Location.LocationStatus;
import org.hl7.fhir.r4.model.Practitioner.PractitionerQualificationComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class FhirResourceTemplateHelper {
	private static String CODE_JDN = "jdn";
	private static String CODE_BU = "bu";
	private static String DISPLAY_BUILDING = "building";
	private static String DISPLAY_JURISDICTION = "Jurisdiction";
	private static String SYSTEM_LOCATION_PHYSICAL_TYPE = "http://hl7.org/fhir/ValueSet/location-physical-type";
	private static String CODE_CLINIC = "prov";
	private static String DISPLAY_CLINIC = "Healthcare Provider";
	private static String CODE_GOVT = "govt";
	private static String DISPLAY_GOVERNMENT = "Government";
	private static String SYSTEM_ORGANIZATION_PHYSICAL_TYPE = "	http://hl7.org/fhir/ValueSet/organization-type";
	// todo - change the URLs below once resources are updated as per Implementation
	// Guide
	private static String SYSTEM_HCW = "https://www.iprdgroup.com/nigeria/oyo/ValueSet/Roles";
	private static String IDENTIFIER_SYSTEM = "http://www.iprdgroup.com/Identifier/System";
	private static String SYSTEM_ORG_TYPE = "https://www.iprdgroup.com/ValueSet/OrganizationType/tags";
	private static String EXTENSION_PLUSCODE_URL = "http://iprdgroup.org/fhir/Extention/location-plus-code";

	private static final Logger logger = LoggerFactory.getLogger(FhirResourceTemplateHelper.class);
	 
	/**
     * Creates an Organization resource representing a country.
     *
     * @param name the name of the country
     * @return Organization object representing the country
     */
	public static Organization country(String name) {
		Organization country = new Organization();
		country.setMeta(getMetaByOrgType(OrgType.COUNTRY));
		List<CodeableConcept> codeableConcepts = new ArrayList<>();
		CodeableConcept countryPhysicalType = new CodeableConcept();
		Coding physicalTypeCoding = new Coding();
		physicalTypeCoding.setCode(CODE_GOVT).setDisplay(DISPLAY_GOVERNMENT)
				.setSystem(SYSTEM_ORGANIZATION_PHYSICAL_TYPE);
		countryPhysicalType.addCoding(physicalTypeCoding);
		countryPhysicalType.setText(DISPLAY_GOVERNMENT);
		codeableConcepts.add(countryPhysicalType);
		country.setType(codeableConcepts);
		country.setName(name);
		country.setId(new IdType("Organization", generateUUID()));
		return country;
	}
	
    /**
     * Creates an Organization resource representing a state.
     *
     * @param name      the name of the state
     * @param country   the name of the country the state belongs to
     * @param countryId the identifier of the country
     * @return Organization object representing the state
     */
	public static Organization state(String name, String country, String countryId) {
		Organization state = new Organization();
		state.setMeta(getMetaByOrgType(OrgType.STATE));
		List<CodeableConcept> codeableConcepts = new ArrayList<>();
		List<Address> addresses = new ArrayList<>();
		Address stateAddress = new Address();
		stateAddress.setState(name);
		addresses.add(stateAddress);
		state.setAddress(addresses);
		CodeableConcept statePhysicalType = new CodeableConcept();
		Coding physicalTypeCoding = new Coding();
		physicalTypeCoding.setCode(CODE_GOVT).setDisplay(DISPLAY_GOVERNMENT)
				.setSystem(SYSTEM_ORGANIZATION_PHYSICAL_TYPE);
		statePhysicalType.addCoding(physicalTypeCoding);
		statePhysicalType.setText(DISPLAY_GOVERNMENT);
		codeableConcepts.add(statePhysicalType);
		state.setType(codeableConcepts);
		state.setName(name);
		state.setId(new IdType("Organization", generateUUID()));
		state.setPartOf(new Reference("Organization/" + countryId));
		return state;
	}


    /**
     * Creates an Organization resource representing a local government area (LGA).
     *
     * @param nameOfLga the name of the LGA
     * @param state     the name of the state the LGA belongs to
     * @param stateId   the identifier of the state
     * @return Organization object representing the LGA
     */
	public static Organization lga(String nameOfLga, String state, String stateId) {
		Organization lga = new Organization();
		lga.setMeta(getMetaByOrgType(OrgType.LGA));
		List<CodeableConcept> codeableConcepts = new ArrayList<>();
		List<Address> addresses = new ArrayList<>();
		Address lgaAddress = new Address();
		lgaAddress.setState(state);
		lgaAddress.setDistrict(nameOfLga);
		addresses.add(lgaAddress);
		lga.setAddress(addresses);
		CodeableConcept lgaPhysicalType = new CodeableConcept();
		Coding physicalTypeCoding = new Coding();
		physicalTypeCoding.setCode(CODE_GOVT).setDisplay(DISPLAY_GOVERNMENT)
				.setSystem(SYSTEM_ORGANIZATION_PHYSICAL_TYPE);
		lgaPhysicalType.addCoding(physicalTypeCoding);
		lgaPhysicalType.setText(DISPLAY_GOVERNMENT);
		codeableConcepts.add(lgaPhysicalType);
		lga.setType(codeableConcepts);
		lga.setName(nameOfLga);
		lga.setId(new IdType("Organization", generateUUID()));
		lga.setPartOf(new Reference("Organization/" + stateId));
		return lga;
	}
	
    /**
     * Creates an Organization resource representing a ward.
     *
     * @param state   the name of the state the ward belongs to
     * @param district the district where the ward is located
     * @param city    the city where the ward is located
     * @param lgaId   the identifier of the local government area (LGA)
     * @return Organization object representing the ward
     */
	public static Organization ward(String state, String district, String city, String lgaId) {
		Organization ward = new Organization();
		ward.setMeta(getMetaByOrgType(OrgType.WARD));
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
		physicalTypeCoding.setCode(CODE_GOVT).setDisplay(DISPLAY_GOVERNMENT)
				.setSystem(SYSTEM_ORGANIZATION_PHYSICAL_TYPE);
		wardPhysicalType.addCoding(physicalTypeCoding);
		wardPhysicalType.setText(DISPLAY_GOVERNMENT);
		codeableConcepts.add(wardPhysicalType);
		ward.setType(codeableConcepts);
		ward.setName(city);
		ward.setId(new IdType("Organization", generateUUID()));
		ward.setPartOf(new Reference("Organization/" + lgaId));
		return ward;
	}
	
    /**
     * Creates a Location resource representing a clinic.
     *
     * @param state        the state where the clinic is located
     * @param district     the district where the clinic is located
     * @param city         the city where the clinic is located
     * @param clinic       the name of the clinic
     * @param longitude    the longitude coordinate of the clinic
     * @param latitude     the latitude coordinate of the clinic
     * @param pluscode     the plus code location identifier of the clinic
     * @param organizationReference reference to the managing organization
     * @return Location object representing the clinic
     */
	public static Location clinic(String state, String district, String city, String clinic, String longitude,
			String latitude, String pluscode, String organizationReference) {
		Location facility = new Location();
		Address facilityAddress = new Address();
		CodeableConcept facilityPhysicalType = new CodeableConcept();
		Coding physicalTypeCoding = new Coding();
		physicalTypeCoding.setCode(CODE_BU).setDisplay(DISPLAY_BUILDING).setSystem(SYSTEM_LOCATION_PHYSICAL_TYPE);
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
		Reference organizationRef = new Reference();
		organizationRef.setReference("Organization/" + organizationReference);
		facility.setManagingOrganization(organizationRef);
		try {
			Location.LocationPositionComponent position = new Location.LocationPositionComponent();
			position.setLongitude(Double.parseDouble(longitude));
			position.setLatitude(Double.parseDouble(latitude));
			facility.setPosition(position);
			Extension pluscodeExtension = new Extension();
			pluscodeExtension.setUrl(EXTENSION_PLUSCODE_URL);
			StringType pluscodeValue = new StringType(pluscode);
			pluscodeExtension.setValue(pluscodeValue);
			facility.addExtension(pluscodeExtension);
		} catch (NumberFormatException e) {
			logger.warn("The provided latitude or longitude value is non-numeric. Clinic Details - ", clinic, district,
					city, state);
		}
		return facility;
	}
	
    /**
     * Creates an Organization resource representing a clinic.
     *
     * @param nameOfClinic     the name of the clinic
     * @param facilityUID      the unique identifier of the facility
     * @param facilityCode     the code of the facility
     * @param countryCode      the country code
     * @param contact          the contact information
     * @param state            the state where the clinic is located
     * @param district         the district where the clinic is located
     * @param city             the city where the clinic is located
     * @param facilityLevel    the level of the facility
     * @param parent           the id of the parent
     * @param argusoftId       the Argusoft identifier
     * @return Organization object representing the clinic
     */
	public static Organization clinic(String nameOfClinic, String facilityUID, String facilityCode, String countryCode,
			String contact, String state, String district, String city,String facilityLevel, String parent, String argusoftId) {
		Organization clinic = new Organization();
		clinic.setMeta(getMetaByOrgType(OrgType.FACILITY));
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
		Identifier argusoftIdentifier = new Identifier();
		Identifier facilityLevelIdentifier = new Identifier();

		facilityUIDIdentifier.setSystem(IDENTIFIER_SYSTEM + "/facilityCode");
		facilityUIDIdentifier.setValue(facilityCode);
		facilityCodeIdentifier.setSystem(IDENTIFIER_SYSTEM + "/facilityUID");
		facilityCodeIdentifier.setValue(facilityUID);
		argusoftIdentifier.setSystem(IDENTIFIER_SYSTEM + "/argusoft_identifier");
		argusoftIdentifier.setValue(argusoftId);
		
		facilityLevelIdentifier.setSystem(IDENTIFIER_SYSTEM+"/facilityLevel");
		facilityLevelIdentifier.setValue(facilityLevel);
		
		identifiers.add(facilityUIDIdentifier);
		identifiers.add(facilityCodeIdentifier);
		identifiers.add(argusoftIdentifier);
		identifiers.add(facilityLevelIdentifier);
		
		clinic.setIdentifier(identifiers);
		List<ContactPoint> contactPoints = new ArrayList<>();
		ContactPoint contactPoint = new ContactPoint();
		contactPoint.setValue(countryCode + contact);
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
		clinic.setPartOf(new Reference("Organization/" + parent));
		return clinic;
	}

    /**
     * Creates a Practitioner resource for a healthcare worker.
     *
     * @param firstName             the first name of the healthcare worker
     * @param lastName              the last name of the healthcare worker
     * @param telecom               the telephone contact
     * @param countryCode           the country code
     * @param gender                the gender of the healthcare worker
     * @param dob                   the date of birth of the healthcare worker
     * @param state                 the state where the healthcare worker is located
     * @param lga                   the local government area of the healthcare worker
     * @param ward                  the ward of the healthcare worker
     * @param facilityUID           the facility UID
     * @param role                  the role of the healthcare worker
     * @param qualification         the qualification of the healthcare worker
     * @param stateIdentifierString the state identifier string
     * @param argusoftId            the Argusoft identifier
     * @return Practitioner object representing the healthcare worker
     * @throws Exception if there is an error in parsing the date of birth
     */
	public static Practitioner hcw(String firstName, String lastName, String telecom, String countryCode, String gender,
			String dob, String state, String lga, String ward, String facilityUID, String role, String qualification,
			String stateIdentifierString, String argusoftId) throws Exception {
		Practitioner practitioner = new Practitioner();
		List<Identifier> identifiers = new ArrayList<>();
		Identifier clinicIdentifier = new Identifier();
		Identifier argusoftIdentifier = new Identifier();
		clinicIdentifier.setSystem(IDENTIFIER_SYSTEM + "/facilityUID");
		clinicIdentifier.setValue(facilityUID);
		Identifier stateIdentifier = new Identifier();
		stateIdentifier.setSystem(IDENTIFIER_SYSTEM + "/stateIdentifier");
		stateIdentifier.setValue(stateIdentifierString);
		argusoftIdentifier.setSystem(IDENTIFIER_SYSTEM + "/argusoft_identifier");
		argusoftIdentifier.setValue(argusoftId);
		identifiers.add(clinicIdentifier);
		identifiers.add(stateIdentifier);
		identifiers.add(argusoftIdentifier);
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
		contactPoint.setValue(countryCode + telecom);
		contactPoint.setSystem(org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem.PHONE);
		contactPoints.add(contactPoint);
		practitioner.setTelecom(contactPoints);
		if ("M".equals(gender)) {
			gender = "male";
			practitioner.setGender(AdministrativeGender.fromCode(gender));
		} else if ("F".equals(gender) || "FM".equals(gender)) {
			gender = "female";
			practitioner.setGender(AdministrativeGender.fromCode(gender));
		}
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
		} catch (ParseException exception) {
			logger.warn(ExceptionUtils.getStackTrace(exception));
		}
		practitioner.setId(new IdType("Practitioner", generateUUID()));
		return practitioner;
	}

	public static Practitioner user(String firstName, String lastName, String telecom, String countryCode,
			String gender, String dob, String state, String facilityUID, String type) {
		Practitioner practitioner = new Practitioner();
		List<Identifier> identifiers = new ArrayList<>();
		Identifier clinicIdentifier = new Identifier();
		clinicIdentifier.setSystem(IDENTIFIER_SYSTEM + "/facilityUID");
		clinicIdentifier.setSystem(facilityUID);
		identifiers.add(clinicIdentifier);
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
		contactPoint.setValue(countryCode + telecom);
		contactPoint.setSystem(org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem.PHONE);
		contactPoints.add(contactPoint);
		practitioner.setTelecom(contactPoints);
		if (gender == "M") {
			gender = "male";
			practitioner.setGender(AdministrativeGender.fromCode(gender));
		} else if (gender == "F" || gender == "FM") {
			gender = "female";
			practitioner.setGender(AdministrativeGender.fromCode(gender));
		}
		try {
			Date dateOfBirth = new SimpleDateFormat("dd/MM/yyyy").parse(dob);
			practitioner.setBirthDate(dateOfBirth);
		} catch (ParseException exception) {
			logger.warn(ExceptionUtils.getStackTrace(exception));
		}
		practitioner.setId(new IdType("Practitioner", generateUUID()));
		return practitioner;
	}

	public static PractitionerRole practitionerRole(String role, String qualification, String practitionerId,
			String organizationId) {
		PractitionerRole practitionerRole = new PractitionerRole();
		Reference PractitionerReference = new Reference("Practitioner/" + practitionerId);
		Reference organizatioReference = new Reference("Organization/" + organizationId);
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
		practitionerRole.setOrganization(organizatioReference);
		practitionerRole.setId(new IdType("PractitionerRole", generateUUID()));
		return practitionerRole;
	}

	public static String generateUUID() {
		return UUID.randomUUID().toString();
	}

	public static Meta getMetaByOrgType(OrgType orgType) {
		Meta meta = new Meta();
		Coding coding = new Coding();
		coding.setSystem(SYSTEM_ORG_TYPE);
		coding.setCode(orgType.getValue());
		coding.setDisplay(orgType.name());
		meta.addTag(coding);
		return meta;
	}
}
