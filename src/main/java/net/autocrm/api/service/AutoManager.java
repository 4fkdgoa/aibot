package net.autocrm.api.service;

public interface AutoManager {

	/**
	 * 모델 정보 처리
	 */
	void doModel();

	/**
	 * 패키지 가격 정보 처리
	 */
	void doModelPkg();

	/**
	 * Description 정보 처리
	 */
	void doModelPkgGrp();

	/**
	 * 모델 가격 처리
	 */
	void doModelPrc();

	/**
	 * 외장색상 처리
	 */
	void doExColor();

	/**
	 * 내장색상 처리
	 */
	void doInColor();

	/**
	 * AWS S3 특정 폴더 내의 지정일 지정 cvs 처리
	 * 
	 */
	void execS3(String tableName, String date);

	/**
	 * AWS S3 특정 폴더 내의 지정일 cvs 처리
	 * 
	 */
	void execAllS3(String date);

	/**
	 * local 특정 디렉토리 내의 모든 cvs 처리
	 * 
	 * @param dir
	 */
	void execDir(String dir);

}
