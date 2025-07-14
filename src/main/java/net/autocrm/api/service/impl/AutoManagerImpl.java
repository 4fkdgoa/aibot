package net.autocrm.api.service.impl;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.List;
import java.util.ListIterator;

import jakarta.transaction.Transactional;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.text.CaseUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import net.autocrm.api.model.ClientDetails;
import net.autocrm.api.repository.ClientDetailsRepo;
import net.autocrm.api.service.AutoManager;
import net.autocrm.api.service.CommonManager;
import net.autocrm.common.model.Parameters;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

@Slf4j
@Service
public class AutoManagerImpl extends CommonManager implements AutoManager {

	@Autowired ClientDetailsRepo repo;

	@Override
	public void doModel() {
	    execS3("MODEL");
//		_insert( "D:\\coolclou\\Desktop\\딜러 인터페이스 스팩정보\\MODEL.20220627 161650.csv" );
	}

	@Override
	public void doModelPkg() {
	    execS3("MODEL_PKG");
//		_insert( "D:\\coolclou\\Desktop\\딜러 인터페이스 스팩정보\\MODEL_PKG.20220627 161650.csv" );
	}

	@Override
	public void doModelPkgGrp() {
	    execS3("MODEL_PKG_GRP");
//		_insert( "D:\\coolclou\\Desktop\\딜러 인터페이스 스팩정보\\MODEL_PKG_GRP.20220627 161650.csv" );
	}

	@Override
	public void doModelPrc() {
	    execS3("MODEL_PRC");
//		_insert( "D:\\coolclou\\Desktop\\딜러 인터페이스 스팩정보\\MODEL_PRC.20220627 161650.csv" );
	}

	@Override
	public void doExColor() {
	    execS3("EX_COLOR");
//		_insert( "D:\\coolclou\\Desktop\\딜러 인터페이스 스팩정보\\EX_COLOR.20220627 161650.csv" );
	}

	@Override
	public void doInColor() {
	    execS3("IN_COLOR");
//		_insert( "D:\\coolclou\\Desktop\\딜러 인터페이스 스팩정보\\IN_COLOR.20220627 161650.csv" );
	}

	public void execAllS3(String date) {
		execAllS3( getClient(), date );
	}

	@Transactional
	private void execAllS3(ClientDetails c, String date) {
		log.info("==== Start retieve Auto - " + date);
		String prefix = folderAuto + StringUtils.left(date, 6);
		ListObjectsRequest req = ListObjectsRequest.builder()
				.bucket( c.getS3bucket() )
				.prefix( prefix )
				.build();

		S3Client s3 = getS3Client( c );
		ListObjectsResponse res = s3.listObjects(req);
		if ( res.hasContents() ) {
			boolean hasContent = false;
			List<S3Object> list = res.contents();
			ListIterator<S3Object> iter = res.contents().listIterator( list.size() );
			while ( iter.hasPrevious() ) {
				S3Object s3Object = iter.previous();
		    	String key = s3Object.key();
				String tableName = StringUtils.substringBefore( StringUtils.substringAfterLast(key, '/'), '.' );
				String dtCsv = StringUtils.left( StringUtils.substringAfter(key, '.'), 8);
				if ( ArrayUtils.contains(arr, tableName) && StringUtils.equals(date, dtCsv) ) {
					hasContent = true;
					if ( countTable(tableName, date) > 0 ) {
						log.warn("이미 등록된 csv 입니다. (" + tableName + "," + date + ")");
						continue;
					}

		    		insCSV(c, s3, key, tableName);
				}
		    }
		    if ( !hasContent )
				log.warn(prefix + "내에 " + date + " csv 파일이 없습니다.");
		    else {
		    	sqlSession.insert("auto.insertModels");
		    	sqlSession.update("auto.updateModels");
		    	sqlSession.insert("auto.insertModelsYear");
		    	sqlSession.insert("auto.updateModelsYear");
		    }
		} else {
			log.warn(prefix + "내에 csv 파일이 없습니다.");
		}
		log.info("==== End retieve Auto - " + date);
	}

	private void execS3(String tableName) {
		String today = DateFormatUtils.format(Calendar.getInstance(), "yyyyMMdd");
		execS3(tableName, today);
	}

	@Override
	public void execS3(String tableName, String date) {
    	execS3( getClient(), tableName, date );
	}

	@Transactional
	private void execS3(ClientDetails c, String tableName, String date) {
		log.info("==== Start retieve " + tableName + " - " + date);
		if ( countTable(tableName, date) > 0 ) {
			log.warn("이미 등록된 csv 입니다. (" + tableName + "," + date + ")");
			return;
		}
		String prefix = folderAuto + StringUtils.left(date, 6);
		ListObjectsRequest req = ListObjectsRequest.builder()
				.bucket( c.getS3bucket() )
				.prefix( prefix )
				.build();

		S3Client s3 = getS3Client( c );
		ListObjectsResponse res = s3.listObjects(req);
		if ( res.hasContents() ) {
			boolean hasContent = false;
			List<S3Object> list = res.contents();
			ListIterator<S3Object> iter = res.contents().listIterator( list.size() );
			while ( iter.hasPrevious() ) {
				S3Object s3Object = iter.previous();
		    	String key = s3Object.key();
		    	if ( StringUtils.startsWith( key, prefix + "/" + tableName + "." + date ) ) {
		    		hasContent = true;

		    		insCSV(c, s3, key, tableName);
		    	}
		    }
		    if ( !hasContent )
				log.warn(prefix + "내에 " + date + " csv 파일이 없습니다.");
		} else {
			log.warn(prefix + "내에 csv 파일이 없습니다.");
		}
		log.info("==== End retieve " + tableName + " - " + date);
	}
	
	private void insCSV(ClientDetails c, S3Client s3, String key, String tableName) {
		GetObjectRequest reqGet = GetObjectRequest.builder()
				.bucket( c.getS3bucket() )
				.key( key )
				.build();
		try ( ResponseInputStream<GetObjectResponse> resGet = s3.getObject( reqGet ) ) {
    		try ( InputStreamReader in = new InputStreamReader(resGet) ) {
    			_insert(in, key, CaseUtils.toCamelCase("auto.insert_" + tableName, false, '_'));
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		log.info(key + " 완료!");
	}

	private int countTable(String tableName, String date) {
		Parameters<String, String> p = new Parameters<String, String>();
		p.put("tableName", tableName);
		p.put("dt", date);
		return (Integer) sqlSession.selectOne("auto.cntTable", p);
	}

	@Transactional
	@Override
	public void execDir(String dir) {
		File d = new File(dir);
		if ( d.isDirectory() ) {
			FileFilter filter = new FileFilter() {
			    public boolean accept(File f) {
			        return f.getName().endsWith("cvs");
			    }
			};
			File[] files = d.listFiles(filter);
			for (File cvs : files) {
				_insert(cvs);
			}
		}
	}

	@SuppressWarnings("unused")
	private void _insert(String csvfile) {
		File csv = new File(csvfile);
		_insert(csv);
	}

	private void _insert(File csv) {
		String prefix = StringUtils.substringBefore( StringUtils.substringAfterLast(csv.getName(), '\\'), '.' );
		if ( ArrayUtils.contains(arr, prefix) ) {
			String statement = CaseUtils.toCamelCase("auto.insert_" + prefix, false, '_');
			_insert(csv ,  statement);
		}
	}

	private String[] arr = { "MODEL", "MODEL_PKG", "MODEL_PKG_GRP", "MODEL_PRC", "EX_COLOR", "IN_COLOR" };


	/**
	 * WDMS에서 차량스펙정보 가져오기 
	 */
	@Scheduled(cron = "${cron.retrieve.auto}")
	public void retrieve() {
		String today = DateFormatUtils.format(Calendar.getInstance(), "yyyyMMdd");
		repo.findById( config.getProperty("wdms.id", "wdms") ).ifPresent(t -> execAllS3(t, today));
	}

	/**
	 * 테이블 분할 - default : 매월 첫째 일요일 0시 10분
	 */
	@Scheduled(cron = "${cron.devide.tables:0 10 0 ? * SUN#1}")
	public void devideTables() {
		sqlSession.update("auto.execDevide");
	}
}
