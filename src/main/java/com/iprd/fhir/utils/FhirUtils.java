package com.iprd.fhir.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Provenance;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import autovalue.shaded.kotlin.Pair;
import autovalue.shaded.kotlin.Triple;
import org.apache.jena.ext.xerces.util.URI.MalformedURIException;

public class FhirUtils {


	public static String IDENTIFIER_SYSTEM_OCL = "http://iprdgroup.com/identifiers/ocl";
	public static String IDENTIFIER_SYSTEM_OCL_NEW = "http://iprdsolutions.com/identifiers/patient-ocl";

	public static String IDENTIFIER_SYSTEM_PATIENT_CARD = "http://iprdgroup.com/identifiers/patient-card";
	public static String IDENTIFIER_SYSTEM_PATIENT_CARD_NEW = "http://iprdsolutions.com/identifiers/patient-health-card";

	public static String IDENTIFIER_SYSTEM_PATIENT_WITH_OCL = "http://iprdgroup.com/identifiers/patientWithOcl";
	public static String IDENTIFIER_SYSTEM_PATIENT_WITH_OCL_NEW = "http://iprdsolutions.com/identifiers/patient-with-ocl";

	// HelperService
	public static String EXTENSION_PLUSCODE_URL = "http://iprdgroup.org/fhir/Extention/location-plus-code";
	public static String EXTENSION_PLUSCODE_URL_NEW = "http://iprdsolutions.com/fhir/uv/anc/StructureDefinition/location-pluscode";

	public static String IDENTIFIER_SYSTEM = "http://www.iprdgroup.com/Identifier/System";

	public static final String ORGANIZATION_TYPE_SYSTEM = "http://hl7.org/fhir/ValueSet/organization-type";
	public static final String ORGANIZATION_TYPE_SYSTEM_NEW = "http://terminology.hl7.org/CodeSystem/organization-type";

	public static final String FACILITY_CODE_SYSTEM = "http://www.iprdgroup.com/Identifier/System/facilityCode";
	public static final String FACILITY_CODE_SYSTEM_NEW = "http://www.iprdsolutions.com/identifier/System/facility-code";

	public static final String ORGANIZATION_TAG = "https://www.iprdgroup.com/ValueSet/OrganizationType/tags";
	public static final String ORGANIZATION_TAG_NEW = "http://iprdsolutions.com/fhir/uv/anc/CodeSystem/iprd-organization-type-tags";

	public static final String IDENTIFIER_SYSTEM_KEYCLOAK_ID = "http://www.iprdgroup.com/Identifier/System/KeycloakId";
	public static final String IDENTIFIER_SYSTEM_KEYCLOAK_ID_NEW = "http://iprdsolutions.com/identifier/keycloak-id";

	// FhirResourceTemplateHelper
	public static String LOCATION_PHYSICAL_TYPE_SYSTEM = "http://hl7.org/fhir/ValueSet/location-physical-type";
	public static String LOCATION_PHYSICAL_TYPE_SYSTEM_NEW = "http://hl7.org/fhir/R4/codesystem-location-physical-type";

	public static String SYSTEM_HCW = "https://www.iprdgroup.com/nigeria/oyo/ValueSet/Roles";
	public static String PRACTITIONER_ROLE_CODE_SYSTEM = "http://terminology.hl7.org/CodeSystem/practitioner-role";

	public static final String CODE_CLINIC = "prov";
	public static String CODE_GOVT = "govt";
	public static String DISPLAY_GOVERNMENT = "Government";
	public static String CODE_JDN = "jdn";
	public static String CODE_BU = "bu";
	public static String DISPLAY_BUILDING = "building";
	public static String DISPLAY_JURISDICTION = "Jurisdiction";

	public static final List<String> VALID_ORG_TYPES = Arrays.asList("country", "state", "lga", "ward", "facility");
	public static final List<String> FACILITY_SYNONYMS = Arrays.asList("prov", "provider", "clinic", "healthcare");

	// ServerInterceptor
	public static final String ENCOUNTER_MIGRATED_SYSTEM = "https://iprdgroup.com/identifier";
	public static final String ENCOUNTER_MIGRATED_SYSTEM_NEW = "https://iprdsolutions.com/identifier/encounter-migrated";



	private static final Logger logger = LoggerFactory.getLogger(FhirUtils.class);
	public static Boolean isOclPatient(List<Identifier> identifiers) {
		for (Identifier identifier : identifiers) {
			if (
				identifier.hasSystem() && identifier.getSystem().equals(IDENTIFIER_SYSTEM_PATIENT_WITH_OCL) ||
					identifier.hasSystem() && identifier.getSystem().equals(IDENTIFIER_SYSTEM_PATIENT_WITH_OCL_NEW)
			) {
				return true;
			}
		}
		return false;
	}
	
	public static Triple<String, String, String> getOclIdFromIdentifier(List<Identifier> identifiers) {
		Triple<String, String, String> oclId = null;
		String oclIdentifierValue = null;
		for (Identifier identifier : identifiers) {
			if (
				identifier.hasSystem() && identifier.getSystem().equals(IDENTIFIER_SYSTEM_OCL) ||
					identifier.hasSystem() && identifier.getSystem().equals(IDENTIFIER_SYSTEM_OCL_NEW)
			) {
				oclIdentifierValue = identifier.getValue();
				break;
			}
		}
		try {
			oclId = getOclIdFromString(oclIdentifierValue);
		} catch (MalformedURIException e) {
			logger.debug(e.getMessage());
		}
		return oclId;
	}

	public static Pair<String, String> getEncounterIdAndOrganizationIdForAppointment(String appointmentId, IGenericClient fhirClient){
		QuestionnaireResponse questionnaireResponse;
		Encounter encounter;
		Bundle provenanceBundle = fhirClient.search().forResource(Provenance.class).where(Provenance.TARGET.hasId(appointmentId)).returnBundle(Bundle.class).execute();
		if (!provenanceBundle.hasEntry()){
			return null;
		}
		Provenance provenance = (Provenance) provenanceBundle.getEntry().get(0).getResource();
		String questionnaireResponseId = provenance.getEntityFirstRep().getWhat().getReferenceElement().getIdPart();
		try{
			questionnaireResponse = fhirClient.read().resource(QuestionnaireResponse.class).withId(questionnaireResponseId).execute();
		} catch (ResourceNotFoundException resourceNotFoundException){
			resourceNotFoundException.printStackTrace();
			return null;
		}
		String encounterId = questionnaireResponse.getEncounter().getReferenceElement().getIdPart();
		try{
			encounter = fhirClient.read().resource(Encounter.class).withId(encounterId).execute();
		} catch (ResourceNotFoundException resourceNotFoundException){
			resourceNotFoundException.printStackTrace();
			return null;
		}
		String organizationId = encounter.getServiceProvider().getReferenceElement().getIdPart();
		return new Pair<>(encounterId, organizationId);
	}

	public static String getPatientCardNumberByPatientId(String patientId, IGenericClient fhirClient) {
		Bundle patientBundle = 	fhirClient.search().forResource(Patient.class).where(Patient.RES_ID.exactly().identifier(patientId)).returnBundle(Bundle.class).execute();
		if (!patientBundle.hasEntry())
			return null;
		Patient patient = (Patient) patientBundle.getEntry().get(0).getResource();
		return getPatientCardNumber(patient.getIdentifier());
	}

	public static String getPatientCardNumber(List<Identifier> identifiers) {
		for (Identifier identifier : identifiers) {
			if (
				identifier.hasSystem() && identifier.getSystem().equals(IDENTIFIER_SYSTEM_PATIENT_CARD) ||
					identifier.hasSystem() && identifier.getSystem().equals(IDENTIFIER_SYSTEM_PATIENT_CARD_NEW)
			) {
				return identifier.getValue();
			}
		}
		return null;
	}

	public static String getOclLink(List<Identifier> identifiers) {
		for (Identifier identifier : identifiers) {
			if (
				identifier.hasSystem() && identifier.getSystem().equals(IDENTIFIER_SYSTEM_OCL) ||
					identifier.hasSystem() && identifier.getSystem().equals(IDENTIFIER_SYSTEM_OCL_NEW)
			) {
				return identifier.getValue();
			}
		}
		return null;
	}
	
	public static Triple<String, String, String> getOclIdFromString(String query) throws MalformedURIException {
		try {
			URL url = new URL(query);
			String queryUrl = url.getQuery();
			if (queryUrl == null || queryUrl.isEmpty())
				return null;
			Map<String, String> queryMap = getQueryMap(queryUrl);
			if (queryMap.isEmpty() || !queryMap.containsKey("s")) return null;
			return new Triple<>(queryMap.get("s"), queryMap.get("v"), queryMap.get("g"));

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			return null;
		}
	}

	public static Map<String, String> getQueryMap(String query) {
		String[] params = query.split("&");
		Map<String, String> map = new HashMap<String, String>();

		for (String param : params) {
			String[] paramList = param.split("=");
			if (paramList.length > 1) {
				map.put(paramList[0], paramList[1]);
			}
		}
		return map;
	}

	public void getBundleBySearchUrl(Bundle bundle, String url,IGenericClient fhirClient) {
		Bundle searchBundle = fhirClient.search()
			.byUrl(url)
			.returnBundle(Bundle.class)
			.execute();
		bundle.getEntry().addAll(searchBundle.getEntry());
		// Recursively adding all the resources to the bundle if it contains next URL.
		if (searchBundle.hasLink() && bundleContainsNext(searchBundle)) {
			getBundleBySearchUrl(bundle, getNextUrl(searchBundle.getLink()),fhirClient);
		}
	}

	public static boolean bundleContainsNext(Bundle bundle) {
		for (Bundle.BundleLinkComponent link : bundle.getLink()) {
			if (link.getRelation().equals("next"))
				return true;
		}
		return false;
	}

	public static String getNextUrl(List<Bundle.BundleLinkComponent> bundleLinks) {
		for (Bundle.BundleLinkComponent link : bundleLinks) {
			if (link.getRelation().equals("next")) {
				return link.getUrl();
			}
		}
		return null;
	}

	public static String getOrganizationIdFromEncounter(Encounter encounter){
		return encounter.getServiceProvider().getReferenceElement().getIdPart();
	}

	public String getPractitionerRoleFromId(String practitionerRoleId,IGenericClient fhirClient){
		Bundle bundle = fhirClient.search().forResource(PractitionerRole.class).where(PractitionerRole.RES_ID.exactly().identifier(practitionerRoleId)).returnBundle(Bundle.class).execute();
		if (!bundle.hasEntry()) {
			return null;
		}
		PractitionerRole practitionerRole = (PractitionerRole) bundle.getEntry().get(0).getResource();
		String role = practitionerRole.getCodeFirstRep().getCodingFirstRep().getCode();
		return role;
	}

	public static Pair<List<String>,List<Identifier>> getMissingIdentifierAndNewIdentifier(List<Identifier> identifierOldList, List<Identifier> identifierNewList) {
	    List<String> missingFromNew = new ArrayList<String>();
		 List<Identifier> missingFromOldIdentifiers = new ArrayList<Identifier>();
	    Set<String> newIdentifiers = new HashSet<String>();
		 HashMap<String,Integer> valueToIndex = new HashMap<>();
	    int index =0;
	    // Add all identifiers in the new list to the set
	    for (Identifier identifier : identifierNewList) {
	    	if(identifier.getSystem().equals(IDENTIFIER_SYSTEM_OCL) ||
				identifier.getSystem().equals(IDENTIFIER_SYSTEM_OCL_NEW) ||
				identifier.getSystem().equals(IDENTIFIER_SYSTEM_PATIENT_CARD) ||
				identifier.getSystem().equals(IDENTIFIER_SYSTEM_PATIENT_CARD_NEW)
			) {
		        newIdentifiers.add(identifier.getValue());
				  valueToIndex.put(identifier.getValue(),index);
	    	}
			 index+=1;
	    }
	    // Check if each identifier in the old list is present in the new list
	    for (Identifier identifier : identifierOldList) {
	        if (!newIdentifiers.contains(identifier.getValue())) {
		    	if(identifier.getSystem().equals(IDENTIFIER_SYSTEM_OCL)||
					identifier.getSystem().equals(IDENTIFIER_SYSTEM_OCL_NEW) ||
					identifier.getSystem().equals(IDENTIFIER_SYSTEM_PATIENT_CARD) ||
					identifier.getSystem().equals(IDENTIFIER_SYSTEM_PATIENT_CARD_NEW)) {
		    		missingFromNew.add(identifier.getValue());
		    	}
	        }else {
				  missingFromOldIdentifiers.add(identifierNewList.get(valueToIndex.get(identifier.getValue())));
	        }
	    }
	    
	    return new Pair(missingFromNew,missingFromOldIdentifiers);
	}

	public enum KeyId{
		APPCLIENT,
		DASHBOARD
	}
}
