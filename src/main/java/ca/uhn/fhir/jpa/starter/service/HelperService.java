package ca.uhn.fhir.jpa.starter.service;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Date;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import ca.uhn.fhir.jpa.starter.model.ComGenerator;

import org.apache.commons.io.FileUtils;
import org.apache.jena.ext.xerces.util.URI.MalformedURIException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleLinkComponent;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Media;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.token.TokenManager;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.iprd.fhir.utils.DateUtilityHelper;
import com.iprd.fhir.utils.FhirResourceTemplateHelper;
import com.iprd.fhir.utils.FhirUtils;
import com.iprd.fhir.utils.KeycloakTemplateHelper;
import com.iprd.fhir.utils.Validation;
import com.iprd.report.FhirClientProvider;
import com.iprd.report.ReportGeneratorFactory;

import android.util.Base64;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.starter.AppProperties;
import ca.uhn.fhir.model.primitive.BooleanDt;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.impl.GenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;


@Import(AppProperties.class)
@Service
@Interceptor
public class HelperService {

	@Autowired
	AppProperties appProperties;
	@Autowired
	HttpServletRequest request;
	
	private static final Logger logger = LoggerFactory.getLogger(HelperService.class);
	private static String IDENTIFIER_SYSTEM = "http://www.iprdgroup.com/Identifier/System";
	private static final long INITIAL_DELAY = 5 * 30000L;
	private static final long FIXED_DELAY = 5 * 60000L;

	private static final long AUTH_INITIAL_DELAY = 25 * 60000L;
	private static final long AUTH_FIXED_DELAY = 50 * 60000L;
	private static final long DELAY = 2 * 60000;

	public ResponseEntity<LinkedHashMap<String, Object>> createGroups(MultipartFile file) throws IOException {

		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		List<String> states = new ArrayList<>();
		List<String> lgas = new ArrayList<>();
		List<String> wards = new ArrayList<>();
		List<String> clinics = new ArrayList<>();

		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(file.getInputStream(), "UTF-8"));
		String singleLine;
		int iteration = 0;
		String stateId = "", lgaId = "", wardId = "", facilityOrganizationId = "", facilityLocationId = "";
		String stateGroupId = "", lgaGroupId = "", wardGroupId = "", facilityGroupId = "";

		while ((singleLine = bufferedReader.readLine()) != null) {
			if (iteration == 0) { //skip header of CSV file
				iteration++;
				continue;
			}
			String[] csvData = singleLine.split(",");
			//State, LGA, Ward, FacilityUID, FacilityCode, CountryCode, PhoneNumber, FacilityName, FacilityLevel, Ownership
			if (Validation.validateClinicAndStateCsvLine(csvData)) {
				if (!states.contains(csvData[0])) {
					Location state = FhirResourceTemplateHelper.state(csvData[0]);
					stateId = createResource(state, Location.class, Location.NAME.matches().value(state.getName()));
					states.add(state.getName());
					GroupRepresentation stateGroupRep = KeycloakTemplateHelper.stateGroup(state.getName(), stateId);
					stateGroupId = createGroup(stateGroupRep);
					updateResource(stateGroupId, stateId, Location.class);
				}

				if (!lgas.contains(csvData[1])) {
					Location lga = FhirResourceTemplateHelper.lga(csvData[1], csvData[0], stateId);
					lgaId = createResource(lga, Location.class, Location.NAME.matches().value(lga.getName()));
					lgas.add(lga.getName());
					GroupRepresentation lgaGroupRep = KeycloakTemplateHelper.lgaGroup(lga.getName(), stateGroupId, lgaId);
					lgaGroupId = createGroup(lgaGroupRep);
					updateResource(lgaGroupId, lgaId, Location.class);
				}

				if (!wards.contains(csvData[2])) {
					Location ward = FhirResourceTemplateHelper.ward(csvData[0], csvData[1], csvData[2], lgaId);
					wardId = createResource(ward, Location.class, Location.NAME.matches().value(ward.getName()));
					wards.add(ward.getName());
					GroupRepresentation wardGroupRep = KeycloakTemplateHelper.wardGroup(ward.getName(), lgaGroupId, wardId);
					wardGroupId = createGroup(wardGroupRep);
					updateResource(wardGroupId, wardId, Location.class);
				}

				if (!clinics.contains(csvData[7])) {
					Location clinicLocation = FhirResourceTemplateHelper.clinic(csvData[0], csvData[1], csvData[2], csvData[7], wardId);
					Organization clinicOrganization = FhirResourceTemplateHelper.clinic(csvData[7], csvData[3], csvData[4], csvData[5], csvData[6], csvData[0], csvData[1], csvData[2]);
					facilityOrganizationId = createResource(clinicOrganization, Organization.class, Organization.NAME.matches().value(clinicOrganization.getName()));
					facilityLocationId = createResource(clinicLocation, Location.class, Location.NAME.matches().value(clinicLocation.getName()));
					clinics.add(clinicOrganization.getName());

					GroupRepresentation facilityGroupRep = KeycloakTemplateHelper.facilityGroup(
						clinicOrganization.getName(),
						wardGroupId,
						facilityOrganizationId,
						facilityLocationId,
						csvData[8],
						csvData[9],
						csvData[3],
						csvData[4]
					);
					facilityGroupId = createGroup(facilityGroupRep);
					updateResource(facilityGroupId, facilityOrganizationId, Organization.class);
					updateResource(facilityGroupId, facilityLocationId, Location.class);
				}
			}
		}
		map.put("uploadCSV", "Successful");
		return new ResponseEntity<LinkedHashMap<String, Object>>(map, HttpStatus.OK);
	}

	public ResponseEntity<LinkedHashMap<String, Object>> createUsers(@RequestParam("file") MultipartFile file) throws Exception {
		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		List<String> practitioners = new ArrayList<>();

		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(file.getInputStream(), "UTF-8"));
		String singleLine;
		int iteration = 0;
		String practitionerRoleId = "";
		String practitionerId = "";

		while ((singleLine = bufferedReader.readLine()) != null) {
			if (iteration == 0) { //Skip header of CSV
				iteration++;
				continue;
			}
			String hcwData[] = singleLine.split(",");
			//firstName,lastName,email,countryCode,phoneNumber,gender,birthDate,keycloakUserName,initialPassword,state,lga,ward,facilityUID,role,qualification,stateIdentifier
			if (Validation.validationHcwCsvLine(hcwData)) {
				if (!(practitioners.contains(hcwData[0]) && practitioners.contains(hcwData[1]) && practitioners.contains(hcwData[4] + hcwData[3]))) {
					Practitioner hcw = FhirResourceTemplateHelper.hcw(hcwData[0], hcwData[1], hcwData[4], hcwData[3], hcwData[5], hcwData[6], hcwData[9], hcwData[10], hcwData[11], hcwData[12], hcwData[13], hcwData[14], hcwData[15]);
					practitionerId = createResource(hcw,
						Practitioner.class,
						Practitioner.GIVEN.matches().value(hcw.getName().get(0).getGivenAsSingleString()),
						Practitioner.FAMILY.matches().value(hcw.getName().get(0).getFamily()),
						Practitioner.TELECOM.exactly().systemAndValues(ContactPoint.ContactPointSystem.PHONE.toCode(), Arrays.asList(hcwData[4] + hcwData[3]))
					); // Catch index out of bound
					practitioners.add(hcw.getName().get(0).getFamily());
					practitioners.add(hcw.getName().get(0).getGivenAsSingleString());
					practitioners.add(hcw.getTelecom().get(0).getValue());
					PractitionerRole practitionerRole = FhirResourceTemplateHelper.practitionerRole(hcwData[13], hcwData[14], practitionerId);
					practitionerRoleId = createResource(practitionerRole, PractitionerRole.class, PractitionerRole.PRACTITIONER.hasId(practitionerId));
					UserRepresentation user = KeycloakTemplateHelper.user(hcwData[0], hcwData[1], hcwData[2], hcwData[7], hcwData[8], hcwData[4], hcwData[3], practitionerId, practitionerRoleId, hcwData[13], hcwData[9], hcwData[10], hcwData[11], hcwData[12]);
					String keycloakUserId = createUser(user);
					if (keycloakUserId != null) {
						updateResource(keycloakUserId, practitionerId, Practitioner.class);
						updateResource(keycloakUserId, practitionerRoleId, PractitionerRole.class);
					}
				}
			}
		}
		map.put("uploadCsv", "Successful");
		return new ResponseEntity<LinkedHashMap<String, Object>>(map, HttpStatus.OK);
	}

	public List<GroupRepresentation> getGroupsByUser(String userId) {
		RealmResource realmResource = FhirClientAuthenticatorService.getKeycloak().realm("fhir-hapi");
		List<GroupRepresentation> groups = realmResource.users().get(userId).groups(0, appProperties.getKeycloak_max_group_count(), false);
		return groups;
	}

	public ResponseEntity<InputStreamResource> generateDailyReport(String date, String organizationId, List<List<String>> fhirExpressions) {
		FhirClientProvider fhirClientProvider = new FhirClientProviderImpl((GenericClient) FhirClientAuthenticatorService.getFhirClient());
		byte[] pdfContent = ReportGeneratorFactory.INSTANCE.reportGenerator().generateDailyReport(fhirClientProvider, date, organizationId, fhirExpressions);
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(pdfContent);
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Disposition", "inline; filename=daily_report.pdf");
		return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF).body(new InputStreamResource(byteArrayInputStream));
	}

	private String createGroup(GroupRepresentation groupRep) {
		RealmResource realmResource = FhirClientAuthenticatorService.getKeycloak().realm("fhir-hapi");
		List<GroupRepresentation> groups = realmResource.groups().groups(groupRep.getName(), 0, Integer.MAX_VALUE);
		if (!groups.isEmpty()) {
			return groups.get(0).getId();
		}
		Response response = realmResource.groups().add(groupRep);
		return CreatedResponseUtil.getCreatedId(response);
	}

	private String createResource(Resource resource, Class<? extends IBaseResource> theClass, ICriterion<?>... theCriterion) {
		IQuery<IBaseBundle> query = FhirClientAuthenticatorService.getFhirClient().search().forResource(theClass).where(theCriterion[0]);
		for (int i = 1; i < theCriterion.length; i++)
			query = query.and(theCriterion[i]);
		Bundle bundle = query.returnBundle(Bundle.class).execute();
		if (!bundle.hasEntry()) {
			MethodOutcome outcome = FhirClientAuthenticatorService.getFhirClient().update().resource(resource).execute();
			return outcome.getId().getIdPart();
		}
		return bundle.getEntry().get(0).getFullUrl().split("/")[5];
	}

	private <R extends IBaseResource> void updateResource(String keycloakId, String resourceId, Class<R> resourceClass) {
		R resource = FhirClientAuthenticatorService.getFhirClient().read().resource(resourceClass).withId(resourceId).execute();
		try {
			Method getIdentifier = resource.getClass().getMethod("getIdentifier");
			List<Identifier> identifierList = (List<Identifier>) getIdentifier.invoke(resource);
			for (Identifier identifier : identifierList) {
				if (identifier.getSystem().equals(IDENTIFIER_SYSTEM + "/KeycloakId")) {
					return;
				}
			}
			Method addIdentifier = resource.getClass().getMethod("addIdentifier");
			Identifier obj = (Identifier) addIdentifier.invoke(resource);
			obj.setSystem(IDENTIFIER_SYSTEM + "/KeycloakId");
			obj.setValue(keycloakId);
			MethodOutcome outcome = FhirClientAuthenticatorService.getFhirClient().update().resource(resource).execute();
		} catch (SecurityException | NoSuchMethodException | InvocationTargetException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			e.printStackTrace();
		}
	}

	private String createUser(UserRepresentation userRep) {
		RealmResource realmResource = FhirClientAuthenticatorService.getKeycloak().realm(appProperties.getKeycloak_Client_Realm());
		List<UserRepresentation> users = realmResource.users().search(userRep.getUsername(), 0, Integer.MAX_VALUE);
		//if not empty, return id
		if (!users.isEmpty())
			users.get(0).getId();
		try {
			Response response = realmResource.users().create(userRep);
			return CreatedResponseUtil.getCreatedId(response);
		} catch (WebApplicationException e) {
			logger.error("Cannot create user " + userRep.getUsername() + " with groups " + userRep.getGroups() + "\n" + e.getStackTrace().toString());
			return null;
		}
	}

	@Scheduled(fixedDelay = DELAY, initialDelay = DELAY)
	public void mapResourcesToPatient() {
		//Searching for patient created with OCL-ID
		Bundle tempPatientBundle = new Bundle();
		getBundleBySearchUrl(tempPatientBundle, FhirClientAuthenticatorService.serverBase + "/Patient?identifier=patient_with_ocl");

		for (BundleEntryComponent entry : tempPatientBundle.getEntry()) {
			Patient tempPatient = (Patient) entry.getResource();
			String tempPatientId = tempPatient.getIdElement().getIdPart();
			String oclId = tempPatient.getIdentifier().get(0).getValue();

			//Searching for actual patient with OCL-ID
			String actualPatientId = getActualPatientId(oclId);
			if (actualPatientId == null) {
				continue;
			}
			Bundle resourceBundle = new Bundle();
			getBundleBySearchUrl(resourceBundle, FhirClientAuthenticatorService.serverBase + "/Patient/" + tempPatientId + "/$everything");

			for (BundleEntryComponent resourceEntry : resourceBundle.getEntry()) {
				Resource resource = resourceEntry.getResource();
				if (resource.fhirType().equals("Patient")) {
					continue;
				} else if (resource.fhirType().equals("Immunization")) {
					Immunization immunization = (Immunization) resource;
					immunization.getPatient().setReference("Patient/" + actualPatientId);
					FhirClientAuthenticatorService.getFhirClient().update()
						.resource(immunization)
						.execute();
					continue;
				} else if (resource.fhirType().equals("Appointment")) {
					Appointment appointment = (Appointment) resource;
					appointment.getParticipant().get(0).getActor().setReference("Patient/" + actualPatientId);
					FhirClientAuthenticatorService.getFhirClient().update()
						.resource(appointment)
						.execute();
					continue;
				}
				try {
					Method getSubject = resource.getClass().getMethod("getSubject");
					Reference subject = (Reference) getSubject.invoke(resource);
					subject.setReference("Patient/" + actualPatientId);
					FhirClientAuthenticatorService.getFhirClient().update()
						.resource(resource)
						.execute();
				} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException |
							InvocationTargetException e) {
					logger.error(e.getMessage());
				}
			}
		}
	}

	private String getActualPatientId(String oclId) {
		Bundle patientBundle = new Bundle();
		String queryPath = "/Patient?";
		queryPath += "identifierPartial:contains=" + oclId + "&";
		queryPath += "identifier:not=patient_with_ocl";
		getBundleBySearchUrl(patientBundle, FhirClientAuthenticatorService.serverBase + queryPath);
		if (patientBundle.hasEntry() && patientBundle.getEntry().size() > 0) {
			Patient patient = (Patient) patientBundle.getEntry().get(0).getResource();
			return patient.getIdElement().getIdPart();
		}

		for (BundleEntryComponent entry : patientBundle.getEntry()) {
			Patient patient = (Patient) entry.getResource();
			if (isActualPatient(patient, oclId))
				return patient.getIdElement().getIdPart();
		}
		return null;
	}

	private boolean isActualPatient(Patient patient, String oclId) {
		for (Identifier identifier : patient.getIdentifier()) {
			if (identifier.getValue().equals("patient_with_ocl")) {
				return false;
			}
		}
		return true;
	}

	private void getBundleBySearchUrl(Bundle bundle, String url) {
		Bundle searchBundle = FhirClientAuthenticatorService.getFhirClient()
				.search()
				.byUrl(url)
			.returnBundle(Bundle.class)
			.execute();
		bundle.getEntry().addAll(searchBundle.getEntry());
		if (searchBundle.hasLink() && bundleContainsNext(searchBundle)) {
			getBundleBySearchUrl(bundle, getNextUrl(searchBundle.getLink()));
		}
	}

	private boolean bundleContainsNext(Bundle bundle) {
		for (BundleLinkComponent link : bundle.getLink()) {
			if (link.getRelation().equals("next"))
				return true;
		}
		return false;
	}

	private String getNextUrl(List<BundleLinkComponent> bundleLinks) {
		for (BundleLinkComponent link : bundleLinks) {
			if (link.getRelation().equals("next")) {
				return link.getUrl();
			}
		}
		return null;
	}
}
