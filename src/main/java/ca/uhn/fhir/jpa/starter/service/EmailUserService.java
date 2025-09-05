package ca.uhn.fhir.jpa.starter.service;

import ca.uhn.fhir.jpa.starter.DashboardConfigContainer;
import ca.uhn.fhir.jpa.starter.RabbitMQProperties;
import ca.uhn.fhir.jpa.starter.ReportProperties;
import ca.uhn.fhir.jpa.starter.model.EmailScheduleData;
import ca.uhn.fhir.jpa.starter.model.EmailScheduleEntity;
import ca.uhn.fhir.jpa.starter.model.IndicatorColumn;
import ca.uhn.fhir.jpa.starter.model.ReportType;
import com.iprd.report.model.data.ScoreCardItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import android.util.Pair;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

/**
 * Service class responsible for generating and sending facility summaries via email.
 * This service retrieves facility data, processes it into reports, converts reports to CSV format,
 * and sends them as email attachments using a RabbitMQ message queue.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailUserService {

	private static final Logger logger = LoggerFactory.getLogger(EmailUserService.class);

	private final RabbitTemplate rabbitTemplate;
	private final HelperService helperService;
	private final CSVConverter csvConverter;
	private final TaskScheduler taskScheduler;

	@Autowired
	private ReportProperties reportProperties;

	@Autowired
	private RabbitMQProperties rabbitMQProperties;

	@Autowired
	private Map<String, DashboardConfigContainer> dashboardEnvToConfigMap;

	private final ExecutorService facilityExecutor = Executors.newFixedThreadPool(10);
	private final Map<Integer, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
	private final Map<Integer, EmailScheduleData> previousSchedules = new ConcurrentHashMap<>();

	@Scheduled(cron = "0 0 * * * *")
	private void cacheEmailSchedulesForProcessing() {
		try {
			List<EmailScheduleEntity> schedules = NotificationDataSource.getInstance().getAllEmailSchedules();
			if (schedules == null) {
				logger.warn("NotificationDataSource.getInstance().getAllEmailSchedules() returned null.");
				schedules = new ArrayList<>();
			}

			List<EmailScheduleData> dataList = schedules.stream()
				.map(s -> {
					String scheduleType = s.getScheduleType() != null ? s.getScheduleType().toLowerCase() : "";
					String cronExpression;
					switch (scheduleType) {
						case "daily": cronExpression = "0 0 2 * * *"; break;
						case "weekly": cronExpression = "0 30 2 * * MON"; break;
						case "monthly": cronExpression = "0 45 2 1 * *"; break;
						default:
							logger.warn("Invalid schedule type {} for ID {}, skipping schedule", scheduleType, s.getId());
							return null;
					}
					return new EmailScheduleData(s.getId(), s.getOrgId(), s.getRecipientEmail(), s.getEmailSubject(), s.getScheduleType(), s.getUpdatedAt(), cronExpression);
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

			synchronized (this) {
				Map<Integer, EmailScheduleData> newSchedules = dataList.stream()
					.collect(Collectors.toMap(EmailScheduleData::getId, schedule -> schedule));

				Set<Integer> deletedIds = new HashSet<>(previousSchedules.keySet());
				deletedIds.removeAll(newSchedules.keySet());
				deletedIds.forEach(id -> {
					ScheduledFuture<?> future = scheduledTasks.remove(id);
					if (future != null) {
						future.cancel(false);
						logger.info("Canceled task for deleted schedule ID: {}", id);
					}
					previousSchedules.remove(id);
				});

				for (EmailScheduleData schedule : dataList) {
					Integer id = schedule.getId();
					EmailScheduleData prevSchedule = previousSchedules.get(id);

					boolean isNew = prevSchedule == null;
					boolean isUpdated = !isNew && (
						(schedule.getUpdatedAt() != null && prevSchedule.getUpdatedAt() == null) ||
							(schedule.getUpdatedAt() != null && prevSchedule.getUpdatedAt() != null && schedule.getUpdatedAt().after(prevSchedule.getUpdatedAt())) ||
							!Objects.equals(schedule.getCronExpression(), prevSchedule.getCronExpression())
					);

					if ((isNew || isUpdated) && StringUtils.hasText(schedule.getCronExpression())) {
						ScheduledFuture<?> existingFuture = scheduledTasks.remove(id);
						if (existingFuture != null) {
							existingFuture.cancel(false);
						}
						try {
							CronTrigger trigger = new CronTrigger(schedule.getCronExpression());
							ScheduledFuture<?> future = taskScheduler.schedule(() -> sendFacilitySummaryForSchedule(schedule), trigger);
							scheduledTasks.put(id, future);
							previousSchedules.put(id, schedule);
							logger.info("Scheduled report for ID {} with cron: {}", id, schedule.getCronExpression());
						} catch (IllegalArgumentException e) {
							logger.warn("Invalid cron expression for schedule ID {}: {}", id, schedule.getCronExpression());
						}
					}
				}
			}
		} catch (Exception e) {
			logger.warn("Email schedule caching task failed: {}", ExceptionUtils.getStackTrace(e));
		}
	}

	private void sendFacilitySummaryForSchedule(EmailScheduleData schedule) {
		LocalDate today = LocalDate.now();
		LocalDate startDate;
		String scheduleType = schedule.getScheduleType().toLowerCase();

		switch (scheduleType) {
			case "daily": startDate = today.minusDays(1); break;
			case "monthly": startDate = today.minusMonths(1).withDayOfMonth(1); break;
			case "weekly": default: startDate = today.minusWeeks(1).with(DayOfWeek.MONDAY); break;
		}

		LocalDate endDate = "monthly".equals(scheduleType) ? startDate.withDayOfMonth(startDate.lengthOfMonth()) :
			"daily".equals(scheduleType) ? startDate :
				startDate.with(DayOfWeek.SUNDAY);

		String formattedStartDate = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
		String formattedEndDate = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE);

		DashboardConfigContainer configContainer = dashboardEnvToConfigMap.getOrDefault(reportProperties.getEnv(), new DashboardConfigContainer());
		if (configContainer.getIndicatorColumns() == null || configContainer.getIndicatorColumns().isEmpty()) {
			logger.warn("No indicator columns found for environment: {}", reportProperties.getEnv());
			return;
		}
		Map<Integer, String> indicatorMap = configContainer.getIndicatorColumns().stream()
			.collect(Collectors.toMap(IndicatorColumn::getId, IndicatorColumn::getName, (v1, v2) -> v1, LinkedHashMap::new));

		Pair<List<String>, LinkedHashMap<String, List<String>>> facilityData;
		try {
			facilityData = helperService.getFacilityIdsAndOrgIdToChildrenMapPair(schedule.getOrgId());
		} catch (Exception e) {
			logger.warn("Failed to fetch facility IDs for org ID {}: {}", schedule.getOrgId(), ExceptionUtils.getStackTrace(e));
			return;
		}

		if (facilityData.first == null || facilityData.first.isEmpty()) {
			logger.warn("No facilities found for org ID: {}", schedule.getOrgId());
			return;
		}
		Map<String, String> orgIdToNameCache = new ConcurrentHashMap<>();
		String stateName = getValidOrganizationName(schedule.getOrgId(), "state", orgIdToNameCache);
		if (!isValidName(stateName)) {
			logger.warn("Invalid state name for org ID: {}. Aborting report.", schedule.getOrgId());
			return;
		}

		List<CompletableFuture<ReportEntry>> futures = facilityData.first.stream()
			.map(facilityId -> CompletableFuture.supplyAsync(() -> processSingleFacility(
				facilityId, facilityData.second, orgIdToNameCache, indicatorMap, scheduleType, formattedStartDate, formattedEndDate
			), facilityExecutor))
			.collect(Collectors.toList());

		List<ReportEntry> reportEntries = futures.stream()
			.map(CompletableFuture::join).filter(Objects::nonNull).collect(Collectors.toList());

		if (reportEntries.isEmpty()) {
			logger.warn("No report entries generated for state: {}", stateName);
			return;
		}

		byte[] csvAttachment = csvConverter.convertReportToCSV(reportEntries);
		String emailSubject = schedule.getEmailSubject().replace("{startDate}", formattedStartDate).replace("{endDate}", formattedEndDate).replace("{state}", stateName);
		String attachmentName = reportProperties.getEmailAttachmentName().replace("{state}", stateName).replace("{startDate}", formattedStartDate).replace("{endDate}", formattedEndDate);
		String messageBody = String.format("%s Facility Summary Report for %s from %s to %s",
			StringUtils.capitalize(scheduleType), stateName, formattedStartDate, formattedEndDate);

		try {
			rabbitTemplate.convertAndSend(
				rabbitMQProperties.getExchange().getEmail().getName(),
				rabbitMQProperties.getBinding().getEmail().getName(),
				new EmailListener.EmailDetails(schedule.getRecipientEmail(), messageBody, emailSubject, csvAttachment, attachmentName)
			);
			logger.warn("Queued report email to {} for org ID: {}", schedule.getRecipientEmail(), schedule.getOrgId());
		} catch (Exception e) {
			logger.warn("Failed to queue summary email for state {}: {}", stateName, ExceptionUtils.getStackTrace(e));
		}
	}

	private ReportEntry processSingleFacility(String facilityId, LinkedHashMap<String, List<String>> orgIdToChildrenMap, Map<String, String> orgIdToNameCache,
															Map<Integer, String> indicatorMap, String scheduleType, String formattedStartDate, String formattedEndDate) {

		String facilityName = getValidOrganizationName(facilityId, "facility", orgIdToNameCache);
		if (!isValidName(facilityName)) {
			logger.warn("Skipping facility {} due to invalid name", facilityId);
			return null;
		}

		Map<String, String> hierarchy = findParentHierarchy(facilityId, orgIdToChildrenMap);
		String stateNameHierarchy = getValidOrganizationName(hierarchy.get("state"), "state", orgIdToNameCache);
		String lgaName = getValidOrganizationName(hierarchy.get("lga"), "lga", orgIdToNameCache);
		String wardName = getValidOrganizationName(hierarchy.get("ward"), "ward", orgIdToNameCache);
		Map<String, String> indicatorValues = new LinkedHashMap<>();
		indicatorMap.values().forEach(name -> indicatorValues.put(name, "0"));

		try {
			ResponseEntity<?> response = helperService.processDataForReport(
				formattedStartDate, formattedEndDate, ReportType.valueOf(scheduleType), new LinkedHashMap<>(), reportProperties.getEnv(), facilityId, false);
			if (response.getStatusCode().is2xxSuccessful() && response.getBody() instanceof List) {
				List<ScoreCardItem> scoreCardItems = (List<ScoreCardItem>) response.getBody();
				if (scoreCardItems != null && !scoreCardItems.isEmpty()) {
					updateIndicatorValues(scoreCardItems, indicatorMap, indicatorValues, facilityName);
				}
			}
		} catch (Exception e) {
			logger.warn("Failed to process facility {}: {}", facilityName, ExceptionUtils.getStackTrace(e));
			indicatorMap.values().forEach(name -> indicatorValues.put(name, "Error"));
		}

		String dateRange = formattedStartDate + " to " + formattedEndDate;
		return new ReportEntry(dateRange, stateNameHierarchy, lgaName, wardName, facilityName, indicatorValues);
	}

	// --- FIX: Full implementation of helper methods ---

	private Map<String, String> findParentHierarchy(String facilityId, LinkedHashMap<String, List<String>> orgIdToChildrenMap) {
		Map<String, String> hierarchy = new HashMap<>();
		String currentId = facilityId;
		for (int i = 0; i < 10 && currentId != null; i++) { // Max 10 levels deep
			String parentId = null;
			for (Map.Entry<String, List<String>> entry : orgIdToChildrenMap.entrySet()) {
				if (entry.getValue().contains(currentId)) {
					parentId = entry.getKey();
					break;
				}
			}
			if (parentId == null) break;

			String parentType = helperService.getOrganizationType(parentId);
			String level = mapOrgTypeToLevel(parentType);
			if (level != null) {
				hierarchy.put(level, parentId);
			}
			currentId = parentId;
		}
		return hierarchy;
	}

	private String mapOrgTypeToLevel(String orgType) {
		if (orgType == null) return null;
		switch (orgType.toLowerCase()) {
			case "state": case "govt": return "state";
			case "lga": return "lga";
			case "ward": return "ward";
			default: return null;
		}
	}

	private String getValidOrganizationName(String orgId, String level, Map<String, String> nameCache) {
		if (orgId == null) return "N/A";
		if (nameCache.containsKey(orgId)) return nameCache.get(orgId);

		String name = helperService.getOrganizationName(orgId);
		if (!isValidName(name)) {
			name = "N/A";
		}
		nameCache.put(orgId, name);
		return name;
	}

	private boolean isValidName(String name) {
		return StringUtils.hasText(name) && !"Unknown".equalsIgnoreCase(name);
	}

	private void updateIndicatorValues(List<ScoreCardItem> scoreCardItems, Map<Integer, String> indicatorMap,
												  Map<String, String> indicatorValues, String facilityName) {
		for (Map.Entry<Integer, String> entry : indicatorMap.entrySet()) {
			scoreCardItems.stream()
				.filter(scoreItem -> scoreItem.getIndicatorId() == entry.getKey())
				.findFirst()
				.ifPresent(item -> indicatorValues.put(entry.getValue(), item.getValue()));
		}
	}
}