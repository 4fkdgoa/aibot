package net.autocrm.api.service;

import java.util.Map;

public interface WdmsLinkManager {
	Map<?, ?> wdmsCustomerSave(Map<String,?> map);
	Map<?, ?> wdmsContractInfoSave(Map<String,?> map);
	Map<?, ?> wdmsStatsRequest(Map<String,?> map);
	Map<?, ?> wdmsUsedContractInfoSave(Map<String, ?> map);
	Map<?, ?> wdmsWholesaleSave(Map<String, ?> map);
	Map<?, ?> wdmsPromotionInfoSave(Map<String, ?> map);
}
