package net.autocrm.api.service.impl;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;

import jakarta.transaction.Transactional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import net.autocrm.api.model.ClientDetails;
import net.autocrm.api.repository.ClientDetailsRepo;
import net.autocrm.api.service.CommonManager;
import net.autocrm.api.service.StockManager;
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
public class StockManagerImpl extends CommonManager implements StockManager {

	@Autowired ClientDetailsRepo repo;

	@Override
	public void execS3(String date) {
    	execS3( getClient(), date );
    }

    @Transactional
    private void execS3(ClientDetails c, String date) {
		log.info("==== Start retieve Stock - " + date);
		String prefix = folderStock + StringUtils.left(date, 6);
		ListObjectsRequest req = ListObjectsRequest.builder()
				.bucket( c.getS3bucket() )
				.prefix( prefix )
				.build();

		S3Client s3 = getS3Client( c );
		ListObjectsResponse res = s3.listObjects(req);
		if ( res.hasContents() ) {
			String wdmsId = c.getWdmsid();
			boolean hasContent = false;
		    for ( S3Object s3Object : res.contents() ) {
		    	String key = s3Object.key();
		    	if ( StringUtils.startsWith( key, prefix + "/" + wdmsId + ".VEHCLE." + date ) ) {
		    		hasContent = true;

		    		insCSV(c, s3, key);
		    	}
		    }
		    if ( !hasContent )
				log.warn(prefix + "내에 " + date + " csv 파일이 없습니다.");
		} else {
			log.warn(prefix + "내에 csv 파일이 없습니다.");
		}
		log.info("==== End retieve Stock - " + date);
	}
	
    private void insCSV(ClientDetails c, S3Client s3, String key) {
		if ( countTable(key) > 0 ) {
			log.warn("이미 등록된 csv 입니다. (" + key + ")");
			return;
		}
		GetObjectRequest reqGet = GetObjectRequest.builder()
			.bucket( c.getS3bucket() )
			.key( key )
			.build();
		try ( ResponseInputStream<GetObjectResponse> resGet = s3.getObject( reqGet ) ) {
    		try ( InputStreamReader in = new InputStreamReader(resGet) ) {
    			_insert(in, key, "stock.insertVehicle");
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		log.info(key + " 완료!");
	}

	private int countTable(String key) {
		Parameters<String, String> p = new Parameters<String, String>();
		p.put("csv", key);
		return (Integer) sqlSession.selectOne("stock.cntTable", p);
	}

	/**
	 * WDMS에서 가용차량정보 가져오기 
	 */
	@Scheduled(cron = "${cron.retrieve.stock}")
	public void retrieve() {
		String today = DateFormatUtils.format(Calendar.getInstance(), "yyyyMMdd");
		repo.findById( config.getProperty("wdms.id", "wdms") ).ifPresent(t -> execS3(t, today));
	}
}
