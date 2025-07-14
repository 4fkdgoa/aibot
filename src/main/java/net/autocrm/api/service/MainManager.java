package net.autocrm.api.service;

import net.autocrm.common.model.PagedList;
import net.autocrm.common.model.Parameters;

public interface MainManager {

	String getWdmsid();
	void insertRequestLog(Parameters<String, Object> params);
	PagedList<?> listRequestLog(Parameters<String, ?> params, int currentPage, int pageSize);

}
