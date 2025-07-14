package net.autocrm.api.service.impl;

import org.springframework.stereotype.Service;

import net.autocrm.api.service.CommonManager;
import net.autocrm.api.service.MainManager;
import net.autocrm.common.model.PagedList;
import net.autocrm.common.model.Parameters;

@Service("mainMgr")
public class MainManagerImpl extends CommonManager implements MainManager {


	@Override
	public String getWdmsid() {
		return getClient().getWdmsid();
	}

	@Override
	public void insertRequestLog(Parameters<String, Object> params) {
		if ( getClient() != null ) params.put( "id", getClient().getId() );
		insertOne("common", "insertRequestLog", params);
	}

	@Override
	public PagedList<?> listRequestLog(Parameters<String, ?> params, int currentPage, int pageSize) {
		return search("common", "listRequestLog", params, currentPage, pageSize);
	}

}
