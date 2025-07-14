package net.autocrm.api.service;

public interface StockManager {

	/**
	 * 지정일 S3 연동
	 * 
	 * @param date
	 */
	void execS3(String date);

}
