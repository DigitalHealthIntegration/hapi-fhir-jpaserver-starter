package ca.uhn.fhir.jpa.starter.service;

import ca.uhn.fhir.jpa.starter.AppProperties;
import ca.uhn.fhir.jpa.starter.DashboardConfigContainer;
import ca.uhn.fhir.jpa.starter.DashboardEnvironmentConfig;
import ca.uhn.fhir.jpa.starter.model.CacheEntity;
import ca.uhn.fhir.rest.client.impl.GenericClient;
import com.iprd.fhir.utils.DateUtilityHelper;
import com.iprd.fhir.utils.Operation;
import com.iprd.fhir.utils.OperationHelper;
import com.iprd.fhir.utils.Utils;
import com.iprd.report.*;
import com.iprd.report.model.data.BarChartItemDataCollection;
import com.iprd.report.model.data.BarComponentData;
import com.iprd.report.model.data.LineChartItem;
import com.iprd.report.model.data.LineChartItemCollection;
import com.iprd.report.model.data.PieChartItem;
import com.iprd.report.model.data.ScoreCardItem;
import com.iprd.report.model.definition.BarChartDefinition;
import com.iprd.report.model.definition.BarChartItemDefinition;
import com.iprd.report.model.definition.BarComponent;
import com.iprd.report.model.definition.IndicatorItem;
import com.iprd.report.model.definition.LineChart;
import com.iprd.report.model.definition.LineChartItemDefinition;
import com.iprd.report.model.definition.PieChartDefinition;
import com.iprd.report.model.definition.TabularItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

@Import(AppProperties.class)
@Service
public class CachingService {
	@Autowired
	AppProperties appProperties;

	NotificationDataSource notificationDataSource;

	@Autowired
	DashboardEnvironmentConfig dashboardEnvironmentConfig;
	
	private static final Logger logger = LoggerFactory.getLogger(CachingService.class);
	private static final int MAX_RETRY = 6;

	private static final long DELAY = 3600000;
	
	public void cacheData(String orgId, Date startDate, List<IndicatorItem>indicators, int count, Map<String, List<ScoreCardItem>> map,String filterString) {
	   long start = System.nanoTime();

		notificationDataSource = NotificationDataSource.getInstance();
		LinkedHashMap<Integer, String> mapOfIdToMd5 = new LinkedHashMap<>();
		for (IndicatorItem item : indicators) {
			mapOfIdToMd5.put(item.getId(),Utils.md5Bytes((item.getFhirPath()+filterString).getBytes(StandardCharsets.UTF_8)));
		}
			List<ScoreCardItem> data = map.get(startDate.toLocalDate().toString());
			final Date date = (Date) startDate.clone();
			if(data!=null) {
				ArrayList<CacheEntity> cacheEntitiesForInsert = new ArrayList<CacheEntity>();
				ArrayList<CacheEntity> cacheEntitiesForUpdate = new ArrayList<CacheEntity>();
				 data.forEach(item -> {
					OperationHelper.doWithRetry(MAX_RETRY, new Operation() {
					    @Override public void doIt() {
							List<CacheEntity> cacheEntities = notificationDataSource.getCacheByDateIndicatorAndOrgId(date, mapOfIdToMd5.get(item.getIndicatorId()), item.getOrgId());
					    	if (cacheEntities.isEmpty()) {
								CacheEntity cacheEntity = new CacheEntity(item.getOrgId(), mapOfIdToMd5.get(item.getIndicatorId()), date, Double.valueOf(item.getValue()),Date.valueOf(LocalDate.now()));
								cacheEntitiesForInsert.add(cacheEntity);
							} else {
								CacheEntity cacheEntity = cacheEntities.get(0);
								cacheEntity.setValue(Double.valueOf(item.getValue()));
								cacheEntitiesForUpdate.add(cacheEntity);
							}   
					    }
					});
				});		
				   notificationDataSource.insertObjects(cacheEntitiesForInsert);
				   notificationDataSource.updateObjects(cacheEntitiesForUpdate);
			}
			else {
				ArrayList<CacheEntity> cacheEntitiesForInsert = new ArrayList<CacheEntity>();
				indicators.forEach(item -> {					
					OperationHelper.doWithRetry(MAX_RETRY, new Operation() {
					    @Override public void doIt() {
					    	List<CacheEntity> cacheEntities = notificationDataSource.getCacheByDateIndicatorAndOrgId(date, mapOfIdToMd5.get(item.getId()), orgId);
					    	if (cacheEntities.isEmpty()) {
								CacheEntity cacheEntity = new CacheEntity(orgId, mapOfIdToMd5.get(item.getId()), date, Double.valueOf(0),Date.valueOf(LocalDate.now()));
								cacheEntitiesForInsert.add(cacheEntity);
						}
					  }
					});
					
				});
				notificationDataSource.insertObjects(cacheEntitiesForInsert);
			}
		   long end = System.nanoTime();
		   double diff = (end-start);
//		   logger.warn("CACHE DATA "+String.valueOf(diff));
	}
	
	public void cacheDataForBarChart(String orgId, Date startDate, List<BarChartDefinition> barCharts,int count,Map<String, List<BarChartItemDataCollection>> map,String filterString) {
		notificationDataSource = NotificationDataSource.getInstance();
		LinkedHashMap<String, String> mapOfIdToMd5 = new LinkedHashMap<>();
		for (BarChartDefinition barChart : barCharts) {
			for(BarChartItemDefinition barChartItem: barChart.getBarChartItemDefinitions()) {
				for(BarComponent barComponent:barChartItem.getBarComponentList()) {
					mapOfIdToMd5.put(String.valueOf(barChart.getId())+" "+String.valueOf(barChartItem.getId())+" "+String.valueOf(barComponent.getId()),Utils.getMd5KeyForLineCacheMd5((barComponent.getFhirPath()+filterString), barComponent.getBarChartItemId(), barChart.getId()));
				}
			}
			
		}
		
			List<BarChartItemDataCollection> data = map.get(startDate.toLocalDate().toString());
			final Date date = (Date) startDate.clone();
			if (data != null) {
				ArrayList<CacheEntity> cacheEntitiesForInsert = new ArrayList<CacheEntity>();
				ArrayList<CacheEntity> cacheEntitiesForUpdate = new ArrayList<CacheEntity>();
			    data.forEach(barChartItemCollection -> {
			        barChartItemCollection.getBarComponentData().stream().forEach(barComponent -> {
			            OperationHelper.doWithRetry(MAX_RETRY, new Operation() {
			                @Override
			                public void doIt() {
			                    List<CacheEntity> cacheEntities = notificationDataSource.getCacheByDateIndicatorAndOrgId(date, mapOfIdToMd5.get(String.valueOf(barChartItemCollection.getChartId()) + " " + String.valueOf(barChartItemCollection.getId()) + " " + String.valueOf(barComponent.getId())), orgId);
			                    if (cacheEntities.isEmpty()) {
			                        CacheEntity cacheEntityForPatient = new CacheEntity(orgId, mapOfIdToMd5.get(String.valueOf(barChartItemCollection.getChartId()) + " " + String.valueOf(barChartItemCollection.getId()) + " " + String.valueOf(barComponent.getId())), date, Double.valueOf(barComponent.getValue()), Date.valueOf(LocalDate.now()));
			                        cacheEntitiesForInsert.add(cacheEntityForPatient);

			                    } else {
			                        CacheEntity cacheEntityForPatient = cacheEntities.get(0);
			                        cacheEntityForPatient.setValue(Double.valueOf(barComponent.getValue()));
			                        cacheEntitiesForUpdate.add(cacheEntityForPatient);
			                    }
			                }
			            });
			        });
			    });
		    	notificationDataSource.insertObjects(cacheEntitiesForInsert);
		    	notificationDataSource.updateObjects(cacheEntitiesForUpdate);
			} else {
				ArrayList<CacheEntity> cacheEntitiesForInsert = new ArrayList<CacheEntity>();
			    barCharts.forEach(chart -> {
			        chart.getBarChartItemDefinitions().forEach(itemDef -> {
			            itemDef.getBarComponentList().forEach(barComponent -> {
			                OperationHelper.doWithRetry(MAX_RETRY, new Operation() {
			                    @Override
			                    public void doIt() {
			                    	List<CacheEntity> cacheEntities = notificationDataSource.getCacheByDateIndicatorAndOrgId(date, mapOfIdToMd5.get(String.valueOf(chart.getId()) + " " + String.valueOf(itemDef.getId()) + " " + String.valueOf(barComponent.getId())), orgId);
				                    if (cacheEntities.isEmpty()) {
				                        CacheEntity cacheEntityForPatient = new CacheEntity(orgId, mapOfIdToMd5.get(String.valueOf(chart.getId()) + " " + String.valueOf(itemDef.getId()) + " " + String.valueOf(barComponent.getId())), date, Double.valueOf(0), Date.valueOf(LocalDate.now()));
				                        cacheEntitiesForInsert.add(cacheEntityForPatient);
				                    }
			                    }
			                });
			            });
			        });
			    });
                notificationDataSource.insertObjects(cacheEntitiesForInsert);
			}

	}
	
	public void cacheTabularData(String orgId, Date startDate, List<TabularItem> indicators,int count,Map<String, List<ScoreCardItem>> map,String filterString) {

		notificationDataSource = NotificationDataSource.getInstance();
		LinkedHashMap<Integer, String> mapOfIdToMd5 = new LinkedHashMap<>();
		for (TabularItem item : indicators) {
			mapOfIdToMd5.put(item.getId(),Utils.md5Bytes((item.getFhirPath()+filterString).getBytes(StandardCharsets.UTF_8)));
		}
		
			List<ScoreCardItem> data = map.get(startDate.toLocalDate().toString());
			final Date date = (Date) startDate.clone();
			if(data!=null) {
				ArrayList<CacheEntity> cacheEntitiesForInsert = new ArrayList<CacheEntity>();
				ArrayList<CacheEntity> cacheEntitiesForUpdate = new ArrayList<CacheEntity>();
				 data.forEach(item -> {
					OperationHelper.doWithRetry(MAX_RETRY, new Operation() {
					    @Override public void doIt() {
					List<CacheEntity> cacheEntities = notificationDataSource.getCacheByDateIndicatorAndOrgId(date, mapOfIdToMd5.get(item.getIndicatorId()), item.getOrgId());
					if (cacheEntities.isEmpty()) {
						CacheEntity cacheEntity = new CacheEntity(item.getOrgId(), mapOfIdToMd5.get(item.getIndicatorId()), date, Double.valueOf(item.getValue()),Date.valueOf(LocalDate.now()));
						cacheEntitiesForInsert.add(cacheEntity);
					} else {
						CacheEntity cacheEntity = cacheEntities.get(0);
						cacheEntity.setValue(Double.valueOf(item.getValue()));
						cacheEntitiesForUpdate.add(cacheEntity);
					}
					    }
					});
				});	
				 notificationDataSource.insertObjects(cacheEntitiesForInsert);
			    	notificationDataSource.updateObjects(cacheEntitiesForUpdate);
			}
			else {
				ArrayList<CacheEntity> cacheEntitiesForInsert = new ArrayList<CacheEntity>();
				indicators.forEach(item -> {
					OperationHelper.doWithRetry(MAX_RETRY, new Operation() {
					    @Override public void doIt() {
					    	List<CacheEntity> cacheEntities = notificationDataSource.getCacheByDateIndicatorAndOrgId(date, mapOfIdToMd5.get(item.getId()),orgId);
							if (cacheEntities.isEmpty()) {
								CacheEntity cacheEntity = new CacheEntity(orgId, mapOfIdToMd5.get(item.getId()), date, Double.valueOf(0),Date.valueOf(LocalDate.now()));
								cacheEntitiesForInsert.add(cacheEntity);
							}
					    }
					});
				});
                notificationDataSource.insertObjects(cacheEntitiesForInsert);
			}

	}

	public void cachePieChartData(String orgId, Date startDate, List<PieChartDefinition> pieChartDefinitions,int count,Map<String, List<PieChartItem>> map,String filterString){

		notificationDataSource = NotificationDataSource.getInstance();
		LinkedHashMap<String, String> mapOfIdToMd5 = new LinkedHashMap<>();
		for(PieChartDefinition pieChartDefinition : pieChartDefinitions){
			mapOfIdToMd5.put(String.valueOf(pieChartDefinition.getId()),Utils.md5Bytes((pieChartDefinition.getFhirPath()+filterString).getBytes(StandardCharsets.UTF_8)));
		}

		List<PieChartItem> data = map.get(startDate.toLocalDate().toString());
		final Date date = (Date) startDate.clone();
		if(data!=null) {
			ArrayList<CacheEntity> cacheEntitiesForInsert = new ArrayList<CacheEntity>();
			ArrayList<CacheEntity> cacheEntitiesForUpdate = new ArrayList<CacheEntity>();
			 data.forEach(item -> {
				OperationHelper.doWithRetry(MAX_RETRY, new Operation() {
				    @Override public void doIt() {
				List<CacheEntity> cacheEntities = notificationDataSource.getCacheByDateIndicatorAndOrgId(date, mapOfIdToMd5.get(String.valueOf(item.getId())), item.getOrgId());
				if(cacheEntities.isEmpty()){
					CacheEntity cacheEntity = new CacheEntity(item.getOrgId(), mapOfIdToMd5.get(String.valueOf(item.getId())), date, Double.valueOf(item.getValue()),Date.valueOf(LocalDate.now()));
					cacheEntitiesForInsert.add(cacheEntity);

				} else {
					CacheEntity cacheEntity = cacheEntities.get(0);
					cacheEntity.setValue(Double.valueOf(item.getValue()));
					cacheEntitiesForUpdate.add(cacheEntity);
				}		
				    }
				});
			});	
			 notificationDataSource.insertObjects(cacheEntitiesForInsert);
			 notificationDataSource.updateObjects(cacheEntitiesForUpdate);
		}
		else {
			ArrayList<CacheEntity> cacheEntitiesForInsert = new ArrayList<CacheEntity>();
			pieChartDefinitions.forEach(chart -> {
				OperationHelper.doWithRetry(MAX_RETRY, new Operation() {
				    @Override public void doIt() {
				    	List<CacheEntity> cacheEntities = notificationDataSource.getCacheByDateIndicatorAndOrgId(date, mapOfIdToMd5.get(String.valueOf(chart.getId())), orgId);
						if(cacheEntities.isEmpty()){
							CacheEntity cacheEntity = new CacheEntity(orgId, mapOfIdToMd5.get(String.valueOf(chart.getId())), date, Double.valueOf(0),Date.valueOf(LocalDate.now()));
							cacheEntitiesForInsert.add(cacheEntity);
						}
				    }
				});
			});
            notificationDataSource.insertObjects(cacheEntitiesForInsert);
		}
	}

	public void cacheDataLineChart(String orgId, Date startDate, List<LineChart> lineCharts,int count,Map<String, List<LineChartItemCollection>> map,String filterString) {

		notificationDataSource = NotificationDataSource.getInstance();
		LinkedHashMap<String, String> mapOfIdToMd5 = new LinkedHashMap<>();
		for (LineChart lineChart : lineCharts) {
			for(LineChartItemDefinition lineDefinition: lineChart.getLineChartItemDefinitions()) {
				mapOfIdToMd5.put(String.valueOf(lineChart.getId())+" "+String.valueOf(lineDefinition.getId()), Utils.getMd5KeyForLineCacheMd5(lineDefinition.getFhirPath()+filterString, lineDefinition.getId(), lineChart.getId()));	
			}
		}

		List<LineChartItemCollection> data = map.get(startDate.toLocalDate().toString());
		final Date date = (Date) startDate.clone();
		if (data != null) {
			ArrayList<CacheEntity> cacheEntitiesForInsert = new ArrayList<CacheEntity>();
			ArrayList<CacheEntity> cacheEntitiesForUpdate = new ArrayList<CacheEntity>();
		    data.forEach(lineChartItemCollection -> {
		        lineChartItemCollection.getValue().forEach(lineChartItem -> {
		            OperationHelper.doWithRetry(MAX_RETRY, new Operation() {
		                @Override
		                public void doIt() {
		                    List<CacheEntity> cacheEntities = notificationDataSource.getCacheByDateIndicatorAndOrgId(date, mapOfIdToMd5.get(String.valueOf(lineChartItemCollection.getChartId()) + " " + String.valueOf(lineChartItem.getLineId())), orgId);
		                    if (cacheEntities.isEmpty()) {
		                        CacheEntity cacheEntity = new CacheEntity(orgId, mapOfIdToMd5.get(String.valueOf(lineChartItemCollection.getChartId()) + " " + String.valueOf(lineChartItem.getLineId())), date, Double.valueOf(lineChartItem.getValue()), Date.valueOf(LocalDate.now()));
		    					cacheEntitiesForInsert.add(cacheEntity);

		                    } else {
		                        CacheEntity cacheEntity = cacheEntities.get(0);
		                        cacheEntity.setValue(Double.valueOf(lineChartItem.getValue()));
		                        cacheEntitiesForUpdate.add(cacheEntity);
		                    }
		                }
		            });
		        });
		    });
		    notificationDataSource.insertObjects(cacheEntitiesForInsert);
			notificationDataSource.updateObjects(cacheEntitiesForUpdate);
		} else {
			ArrayList<CacheEntity> cacheEntitiesForInsert = new ArrayList<CacheEntity>();
		    lineCharts.forEach(chart -> {
		        chart.getLineChartItemDefinitions().forEach(lineChartItemDefinition -> {
		            OperationHelper.doWithRetry(MAX_RETRY, new Operation() {
		                @Override
		                public void doIt() {
		                	List<CacheEntity> cacheEntities = notificationDataSource.getCacheByDateIndicatorAndOrgId(date, mapOfIdToMd5.get(String.valueOf(chart.getId()) + " " + String.valueOf(lineChartItemDefinition.getId())), orgId);
		                    if (cacheEntities.isEmpty()) {
			                    CacheEntity cacheEntity = new CacheEntity(orgId, mapOfIdToMd5.get(String.valueOf(chart.getId()) + " " + String.valueOf(lineChartItemDefinition.getId())), date, Double.valueOf(0), Date.valueOf(LocalDate.now()));
			                    cacheEntitiesForInsert.add(cacheEntity);
		                    }
		                }
		            });
		        });
		    });
            notificationDataSource.insertObjects(cacheEntitiesForInsert);
		}
	}
	
	@Async("asyncTaskExecutor")
	public void cacheData(String orgId, Date date, List<IndicatorItem> indicators,int count,String filterString) {
//		logger.warn("-- Caching started for Score card chart data batch number "+String.valueOf(count));
		long startTime = System.nanoTime();

		notificationDataSource = NotificationDataSource.getInstance();
		LinkedHashMap<Integer, String> mapOfIdToMd5 = new LinkedHashMap<>();
		for (IndicatorItem item : indicators) {
			mapOfIdToMd5.put(item.getId(),Utils.md5Bytes((item.getFhirPath()+filterString).getBytes(StandardCharsets.UTF_8)));
		}
		FhirClientProvider fhirClientProvider = new FhirClientProviderImpl((GenericClient) FhirClientAuthenticatorService.getFhirClient());
		List<ScoreCardItem> data = ReportGeneratorFactory.INSTANCE.reportGenerator().getFacilityData(fhirClientProvider, orgId, new DateRange(date.toString(), date.toString()), indicators, Collections.emptyList()).get(date.toLocalDate().toString());
		if(data!=null) {
			ArrayList<CacheEntity> cacheEntitiesForInsert = new ArrayList<CacheEntity>();
			ArrayList<CacheEntity> cacheEntitiesForUpdate = new ArrayList<CacheEntity>();
			for (ScoreCardItem item : data) {
				OperationHelper.doWithRetry(MAX_RETRY, new Operation() {
				    @Override public void doIt() {
				    	List<CacheEntity> cacheEntities = notificationDataSource.getCacheByDateIndicatorAndOrgId(date, mapOfIdToMd5.get(item.getIndicatorId()), item.getOrgId());
						if (cacheEntities.isEmpty()) {
							CacheEntity cacheEntity = new CacheEntity(item.getOrgId(), mapOfIdToMd5.get(item.getIndicatorId()), date, Double.valueOf(item.getValue()),Date.valueOf(LocalDate.now()));
							cacheEntitiesForInsert.add(cacheEntity);
						} else {
							CacheEntity cacheEntity = cacheEntities.get(0);
							cacheEntity.setValue(Double.valueOf(item.getValue()));
							cacheEntitiesForUpdate.add(cacheEntity);
						}
				    }
				});	
			}	
			 	notificationDataSource.insertObjects(cacheEntitiesForInsert);
				notificationDataSource.updateObjects(cacheEntitiesForUpdate);
		}else {
			ArrayList<CacheEntity> cacheEntitiesForInsert = new ArrayList<CacheEntity>();
			for (IndicatorItem item : indicators) {
				OperationHelper.doWithRetry(MAX_RETRY, new Operation() {
				    @Override public void doIt() {
				    	List<CacheEntity> cacheEntities = notificationDataSource.getCacheByDateIndicatorAndOrgId(date, mapOfIdToMd5.get(item.getId()), orgId);
				    	if (cacheEntities.isEmpty()) {
							CacheEntity cacheEntity = new CacheEntity(orgId, mapOfIdToMd5.get(item.getId()), date, Double.valueOf(0),Date.valueOf(LocalDate.now()));
							cacheEntitiesForInsert.add(cacheEntity);
						}    
				    }
				});	
			}
            notificationDataSource.insertObjects(cacheEntitiesForInsert);
		}
		long endTime = System.nanoTime();
		long duration = (endTime - startTime);  //divide by 1000000 to get milliseconds.
	}
	
	@Async("asyncTaskExecutor")
	public void cacheDataForBarChart(String orgId, Date date, List<BarChartDefinition> barCharts,int count,String filterString) {
//		logger.warn("-- Caching started for Bar chart data batch number "+String.valueOf(count));
		long startTime = System.nanoTime();

		notificationDataSource = NotificationDataSource.getInstance();
		LinkedHashMap<String, String> mapOfIdToMd5 = new LinkedHashMap<>();
		for (BarChartDefinition barChart : barCharts) {
			for(BarChartItemDefinition barChartItem: barChart.getBarChartItemDefinitions()) {
				for(BarComponent barComponent:barChartItem.getBarComponentList()) {
					mapOfIdToMd5.put(String.valueOf(barChart.getId())+" "+String.valueOf(barChartItem.getId())+" "+String.valueOf(barComponent.getId()),Utils.getMd5KeyForLineCacheMd5(barComponent.getFhirPath()+filterString, barComponent.getBarChartItemId(), barChart.getId()));
				}
			}
			
		}
		FhirClientProvider fhirClientProvider = new FhirClientProviderImpl((GenericClient) FhirClientAuthenticatorService.getFhirClient());
		List<BarChartItemDataCollection> barChartItemCollections = ReportGeneratorFactory.INSTANCE.reportGenerator().getBarChartData(fhirClientProvider, orgId, new DateRange(date.toString(), date.toString()), barCharts, Collections.emptyList()).get(date.toLocalDate().toString());
		if(barChartItemCollections!=null) {
			ArrayList<CacheEntity> cacheEntitiesForInsert = new ArrayList<CacheEntity>();
			ArrayList<CacheEntity> cacheEntitiesForUpdate = new ArrayList<CacheEntity>();
			for (BarChartItemDataCollection barChartItemCollection : barChartItemCollections) {
				for(BarComponentData barComponent: barChartItemCollection.getBarComponentData()) {
					OperationHelper.doWithRetry(MAX_RETRY, new Operation() {
					    @Override public void doIt() {
					    	List<CacheEntity> cacheEntities = notificationDataSource.getCacheByDateIndicatorAndOrgId(date, mapOfIdToMd5.get(String.valueOf(barChartItemCollection.getChartId())+" "+String.valueOf(barChartItemCollection.getId())+" "+String.valueOf(barComponent.getId())), orgId);
							if (cacheEntities.isEmpty()) {
								CacheEntity cacheEntityForPatient = new CacheEntity(orgId, mapOfIdToMd5.get(String.valueOf(barChartItemCollection.getChartId())+" "+String.valueOf(barChartItemCollection.getId())+" "+String.valueOf(barComponent.getId())), date, Double.valueOf(barComponent.getValue()),Date.valueOf(LocalDate.now()));
								cacheEntitiesForInsert.add(cacheEntityForPatient);
						
							} else {
								CacheEntity cacheEntityForPatient = cacheEntities.get(0);
								cacheEntityForPatient.setValue(Double.valueOf(barComponent.getValue()));
								cacheEntitiesForUpdate.add(cacheEntityForPatient);
							}	
					    }
					});	
				}
			}
			notificationDataSource.insertObjects(cacheEntitiesForInsert);
			notificationDataSource.updateObjects(cacheEntitiesForUpdate);
		}else {
			ArrayList<CacheEntity> cacheEntitiesForInsert = new ArrayList<CacheEntity>();
			for (BarChartDefinition item : barCharts) {
				for(BarChartItemDefinition itemDef: item.getBarChartItemDefinitions()) {
					for(BarComponent barComponent:itemDef.getBarComponentList()) {
					      List<CacheEntity> cacheEntities = notificationDataSource.getCacheByDateIndicatorAndOrgId(date, mapOfIdToMd5.get(String.valueOf(item.getId()) + " " + String.valueOf(itemDef.getId()) + " " + String.valueOf(barComponent.getId())), orgId);
			              if (cacheEntities.isEmpty()) {
			                  CacheEntity cacheEntityForPatient = new CacheEntity(orgId, mapOfIdToMd5.get(String.valueOf(item.getId()) + " " + String.valueOf(itemDef.getId()) + " " + String.valueOf(barComponent.getId())), date, Double.valueOf(0), Date.valueOf(LocalDate.now()));
			                  cacheEntitiesForInsert.add(cacheEntityForPatient);
			              }	    		
					}
				}
			}
			notificationDataSource.insertObjects(cacheEntitiesForInsert);
		}
		long endTime = System.nanoTime();
		long duration = (endTime - startTime);  //divide by 1000000 to get milliseconds.
	}
	
	@Async("asyncTaskExecutor")
	public void cacheTabularData(String orgId, Date date, List<TabularItem> indicators,int count,String filterString) {
//		logger.warn("-- Caching started for Tabular data batch number "+String.valueOf(count));
		long startTime = System.nanoTime();

		notificationDataSource = NotificationDataSource.getInstance();
		LinkedHashMap<Integer, String> mapOfIdToMd5 = new LinkedHashMap<>();
		for (TabularItem item : indicators) {
			mapOfIdToMd5.put(item.getId(),Utils.md5Bytes((item.getFhirPath()+filterString).getBytes(StandardCharsets.UTF_8)));
		}
		FhirClientProvider fhirClientProvider = new FhirClientProviderImpl((GenericClient) FhirClientAuthenticatorService.getFhirClient());
		List<ScoreCardItem> scoreCardItems = ReportGeneratorFactory.INSTANCE.reportGenerator().getTabularData(fhirClientProvider, orgId, new DateRange(date.toString(), date.toString()), indicators, Collections.emptyList()).get(date.toLocalDate().toString());
		if(scoreCardItems!=null) {
			ArrayList<CacheEntity> cacheEntitiesForInsert = new ArrayList<CacheEntity>();
			ArrayList<CacheEntity> cacheEntitiesForUpdate = new ArrayList<CacheEntity>();
			for (ScoreCardItem item : scoreCardItems) {
				OperationHelper.doWithRetry(MAX_RETRY, new Operation() {
				    @Override public void doIt() {
				    	List<CacheEntity> cacheEntities = notificationDataSource.getCacheByDateIndicatorAndOrgId(date, mapOfIdToMd5.get(item.getIndicatorId()), item.getOrgId());
						if (cacheEntities.isEmpty()) {
							CacheEntity cacheEntity = new CacheEntity(item.getOrgId(), mapOfIdToMd5.get(item.getIndicatorId()), date, Double.valueOf(item.getValue()),Date.valueOf(LocalDate.now()));
							cacheEntitiesForInsert.add(cacheEntity);
						} else {
							CacheEntity cacheEntity = cacheEntities.get(0);
							cacheEntity.setValue(Double.valueOf(item.getValue()));
							cacheEntitiesForUpdate.add(cacheEntity);
						}	
				    }
				});	
			}
			notificationDataSource.insertObjects(cacheEntitiesForInsert);
			notificationDataSource.updateObjects(cacheEntitiesForUpdate);
		}else {
			ArrayList<CacheEntity> cacheEntitiesForInsert = new ArrayList<CacheEntity>();
			for (TabularItem item : indicators) {
				List<CacheEntity> cacheEntities = notificationDataSource.getCacheByDateIndicatorAndOrgId(date, mapOfIdToMd5.get(item.getId()),orgId);
				if (cacheEntities.isEmpty()) {
					CacheEntity cacheEntity = new CacheEntity(orgId, mapOfIdToMd5.get(item.getId()), date, Double.valueOf(0),Date.valueOf(LocalDate.now()));
					cacheEntitiesForInsert.add(cacheEntity);
				}    
			}
			notificationDataSource.insertObjects(cacheEntitiesForInsert);
		}
		long endTime = System.nanoTime();
		long duration = (endTime - startTime);  //divide by 1000000 to get milliseconds.
	}

	@Async("asyncTaskExecutor")
	public void cachePieChartData(String orgId, Date date, List<PieChartDefinition> pieChartDefinitions,int count,String filterString){
//		logger.warn("-- Caching started for PieChart data batch number "+String.valueOf(count));
		long startTime = System.nanoTime();

		notificationDataSource = NotificationDataSource.getInstance();
		LinkedHashMap<String, String> mapOfIdToMd5 = new LinkedHashMap<>();
		for(PieChartDefinition pieChartDefinition : pieChartDefinitions){
			String key = pieChartDefinition.getFhirPath() + filterString;
			mapOfIdToMd5.put(String.valueOf(pieChartDefinition.getId()),Utils.md5Bytes(key.getBytes(StandardCharsets.UTF_8)));
		}
		FhirClientProvider fhirClientProvider = new FhirClientProviderImpl((GenericClient) FhirClientAuthenticatorService.getFhirClient());
		List<PieChartItem> pieChartItems = ReportGeneratorFactory.INSTANCE.reportGenerator().getPieChartData(fhirClientProvider, orgId, new DateRange(date.toString(), date.toString()), pieChartDefinitions, Collections.emptyList()).get(date.toLocalDate().toString());
		if(pieChartItems!=null) {
			ArrayList<CacheEntity> cacheEntitiesForInsert = new ArrayList<CacheEntity>();
			ArrayList<CacheEntity> cacheEntitiesForUpdate = new ArrayList<CacheEntity>();
			for(PieChartItem item: pieChartItems){
				OperationHelper.doWithRetry(MAX_RETRY, new Operation() {
				    @Override public void doIt() {
				    	List<CacheEntity> cacheEntities = notificationDataSource.getCacheByDateIndicatorAndOrgId(date, mapOfIdToMd5.get(String.valueOf(item.getId())), item.getOrgId());
						if(cacheEntities.isEmpty()){
							CacheEntity cacheEntity = new CacheEntity(item.getOrgId(), mapOfIdToMd5.get(String.valueOf(item.getId())), date, Double.valueOf(item.getValue()),Date.valueOf(LocalDate.now()));
							cacheEntitiesForInsert.add(cacheEntity);
						} else {
							CacheEntity cacheEntity = cacheEntities.get(0);
							cacheEntity.setValue(Double.valueOf(item.getValue()));
							cacheEntitiesForUpdate.add(cacheEntity);
						}	
				    }
				});	
			}
			notificationDataSource.insertObjects(cacheEntitiesForInsert);
			notificationDataSource.updateObjects(cacheEntitiesForUpdate);
		}else {
			ArrayList<CacheEntity> cacheEntitiesForInsert = new ArrayList<CacheEntity>();
			for (PieChartDefinition item : pieChartDefinitions) {
	            List<CacheEntity> cacheEntities = notificationDataSource.getCacheByDateIndicatorAndOrgId(date, mapOfIdToMd5.get(String.valueOf(item.getId())), orgId);
				if(cacheEntities.isEmpty()){
					CacheEntity cacheEntity = new CacheEntity(orgId, mapOfIdToMd5.get(String.valueOf(item.getId())), date, Double.valueOf(0),Date.valueOf(LocalDate.now()));
					cacheEntitiesForInsert.add(cacheEntity);	
				}   
			}
			notificationDataSource.insertObjects(cacheEntitiesForInsert);
		}
		long endTime = System.nanoTime();
		long duration = (endTime - startTime);  //divide by 1000000 to get milliseconds.
	}
	
	@Async("asyncTaskExecutor")
	public void cacheDataLineChart(String orgId, Date date, List<LineChart> lineCharts,int count,String filterString) {
//		logger.warn("-- Caching started for Line data batch number "+String.valueOf(count));
		long startTime = System.nanoTime();

		notificationDataSource = NotificationDataSource.getInstance();
		LinkedHashMap<String, String> mapOfIdToMd5 = new LinkedHashMap<>();
		for (LineChart lineChart : lineCharts) {
			for(LineChartItemDefinition lineDefinition: lineChart.getLineChartItemDefinitions()) {
				mapOfIdToMd5.put(String.valueOf(lineChart.getId())+" "+String.valueOf(lineDefinition.getId()), Utils.getMd5KeyForLineCacheMd5(lineDefinition.getFhirPath()+filterString, lineDefinition.getId(), lineChart.getId()));	
			}
		}
		
		FhirClientProvider fhirClientProvider = new FhirClientProviderImpl((GenericClient) FhirClientAuthenticatorService.getFhirClient());
		List<LineChartItemCollection> lineChartItemCollections = ReportGeneratorFactory.INSTANCE.reportGenerator().getLineChartData(fhirClientProvider, orgId, new DateRange(date.toString(), date.toString()), lineCharts, Collections.emptyList()).get(date.toLocalDate().toString());
		if(lineChartItemCollections!=null) {
			ArrayList<CacheEntity> cacheEntitiesForInsert = new ArrayList<CacheEntity>();
			ArrayList<CacheEntity> cacheEntitiesForUpdate = new ArrayList<CacheEntity>();
			for (LineChartItemCollection lineChartItemCollection : lineChartItemCollections) {
				for(LineChartItem lineChartItem: lineChartItemCollection.getValue()) {
					OperationHelper.doWithRetry(MAX_RETRY, new Operation() {
					    @Override public void doIt() {
					    	List<CacheEntity> cacheEntities = notificationDataSource.getCacheByDateIndicatorAndOrgId(date, mapOfIdToMd5.get(String.valueOf(lineChartItemCollection.getChartId())+" "+String.valueOf(lineChartItem.getLineId())), orgId);
							if (cacheEntities.isEmpty()) {
								CacheEntity cacheEntity = new CacheEntity(orgId,mapOfIdToMd5.get(String.valueOf(lineChartItemCollection.getChartId())+" "+String.valueOf(lineChartItem.getLineId())), date, Double.valueOf(lineChartItem.getValue()),Date.valueOf(LocalDate.now()));
								cacheEntitiesForInsert.add(cacheEntity);
							} else {
								CacheEntity cacheEntity = cacheEntities.get(0);
								cacheEntity.setValue(Double.valueOf(lineChartItem.getValue()));
								cacheEntitiesForUpdate.add(cacheEntity);
							}	
					    }
					});		
				}
			}
			notificationDataSource.insertObjects(cacheEntitiesForInsert);
			notificationDataSource.updateObjects(cacheEntitiesForUpdate);
		}
		else {
			ArrayList<CacheEntity> cacheEntitiesForInsert = new ArrayList<CacheEntity>();
			for (LineChart item : lineCharts) {
				for(LineChartItemDefinition lineChartItemDefinition:item.getLineChartItemDefinitions()) {
		            List<CacheEntity> cacheEntities = notificationDataSource.getCacheByDateIndicatorAndOrgId(date, mapOfIdToMd5.get(String.valueOf(item.getId()) + " " + String.valueOf(lineChartItemDefinition.getId())), orgId);
                    if (cacheEntities.isEmpty()) {
	                    CacheEntity cacheEntity = new CacheEntity(orgId, mapOfIdToMd5.get(String.valueOf(item.getId()) + " " + String.valueOf(lineChartItemDefinition.getId())), date, Double.valueOf(0), Date.valueOf(LocalDate.now()));
	                    cacheEntitiesForInsert.add(cacheEntity);
                    }  	
				}
			}
			notificationDataSource.insertObjects(cacheEntitiesForInsert);
		}
	}
}
