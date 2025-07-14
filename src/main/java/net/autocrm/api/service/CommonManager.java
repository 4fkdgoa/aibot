package net.autocrm.api.service;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.reactive.function.client.WebClient;

import com.google.common.collect.Lists;

import lombok.extern.slf4j.Slf4j;
import net.autocrm.api.model.ClientDetails;
import net.autocrm.common.ConfigKeys;
import net.autocrm.common.model.PagedList;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Slf4j
public abstract class CommonManager {

    @Autowired
	protected Environment config;
	
	@Autowired
	protected MessageSource msg;

	@Autowired
	protected SqlSession	sqlSession;

	@Autowired
	protected DataSource	datasource;

    @Value( ConfigKeys.S3_FOLDER_AUTO )
    protected String folderAuto;

    @Value( ConfigKeys.S3_FOLDER_STOCK )
    protected String folderStock;

    @Autowired
	protected CSVFormat csvFormat;

    private WebClient webclient;

    protected WebClient getWebClient() {
 		return this.webclient.mutate().baseUrl( getClient().getUrl() ).build();
    }

    @Autowired
    protected final void setWebClient(WebClient webclient) {
		this.webclient = webclient;
	}

    protected S3Client getS3Client() {
    	return getS3Client( getClient() );
    }

    protected S3Client getS3Client(ClientDetails client) {
        AwsCredentials credentials = AwsBasicCredentials.create( client.getS3accesskey(), client.getS3secretkey() );
        AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create( credentials );
    	return S3Client.builder()
    		.credentialsProvider(credentialsProvider)
    		.region( Region.of( client.getS3region() ) )
    		.build();
    }

    protected String getWdmsToken() {
    	ClientDetails c = getClient();
    	return getWdmsToken( c.getWdmsid(), c.getWdmssecret() );
    }

    private String getWdmsToken(String wdmsId, String wdmsSecret) {
		String authJson = "{\"corpCd\":\"" + wdmsId + "\",\"secretKey\":\"" + wdmsSecret + "\"}";
		Map<?,?> ret = getWebClient().post()
				.uri( "/auth/tokens" )
				.bodyValue( authJson )
				.accept( MediaType.APPLICATION_JSON )
				.retrieve()
				.bodyToMono( Map.class )
				.block();
		return ret.get( "accessToken" ).toString();
	}

	protected ClientDetails getClient() {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		return auth == null || !(auth.getDetails() instanceof ClientDetails) ? null : (ClientDetails)auth.getDetails();
	}

	protected void _insert(File csv, String statement) {
		try ( Reader in = new FileReader(csv) ) {
			_insert(in , csv.getName() ,  statement);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	final int LIMIT_PARAM_CNT = 2100;

	protected void _insert(Reader csv, String csvName, String statement) throws IOException {
		String date = StringUtils.right( StringUtils.substringBeforeLast(csvName, " "), 8 );
		try ( CSVParser parser = csvFormat.parse(csv) ) {
			int sizeDevide = 0, cntParam = 0;
			ArrayList<ArrayList<Object>> arr = new ArrayList<>();
			for (CSVRecord record : parser) {
				int cntRecord = record.size();
				ArrayList<Object> r = new ArrayList<>();
				for (int i=0; i < cntRecord; i++) {
					r.add( StringUtils.defaultIfBlank( StringUtils.trim(record.get(i)), null) );
				}
				if ( !r.isEmpty() ) {
					r.add( csvName );
					r.add( date );
					arr.add(r);
					cntParam += (cntRecord + 2);
					if ( cntParam < LIMIT_PARAM_CNT ) {
						sizeDevide++;
					}
				}
			}
			if ( !arr.isEmpty() ) {
				Lists.partition(arr, sizeDevide).forEach( l -> {
					try {
						sqlSession.insert( statement, l );
					} catch (RuntimeException e) {
						log.error( e.getMessage() + arr.toString() , e );
					}
				});
			}
		}
	}


	/* ******************************************
	 * iBatis 용 멤버 시작
	 * ******************************************/
	
	public PagedList<?> search(String selectStatement,
			Map<String, ?> params, int currentPage, int pageSize) {
		return search(this.getClass().getSimpleName(), selectStatement, params, new RowBounds(
				(currentPage - 1) * pageSize, pageSize));
	}

	public PagedList<?> search( String mapper, String selectStatement,
			Map<String, ?> params, int currentPage, int pageSize) {
		return search(mapper, selectStatement, params, new RowBounds(
				(currentPage - 1) * pageSize, pageSize));
	}
	

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public PagedList<?> searchRowNum(String mapper, String selectStatement,
			Map<String, ?> params, int currentPage, int pageSize) {

		int _currentPage = (currentPage - 1) * pageSize;
		int _pageSize = pageSize * currentPage;
		
		Map<String, Object> p = new HashMap<String, Object>(params);
		p.put("firstRowNum", Integer.toString(_currentPage));
		p.put("lastRowNum", Integer.toString(_pageSize));

		Number totalCount = null;
		try {
			totalCount = (Number) sqlSession.selectOne(mapper
				+ ".count-" + selectStatement, p);
		} catch (MyBatisSystemException e) {
			Map<String, Object> countParam = new HashMap<String, Object>(params);
			countParam.put("count", "true");
			Object _cnt = sqlSession.selectOne(mapper + '.' + selectStatement, countParam);
			if ( _cnt instanceof Map ) {
				Map<?,?> cntMap = (Map<?,?>)_cnt;
				totalCount = (Number)cntMap.get( cntMap.containsKey("cnt") ? "cnt" : cntMap.containsKey("count") ? "count" : "" );
			} else {
				totalCount = (Number)_cnt;
			}
		}

		List list = sqlSession.selectList(mapper + '.' + selectStatement, p);
		
		PagedList<?> results = new PagedList(totalCount.intValue(), list);
		results.setPageSize(pageSize);
		results.setCurrentPage(currentPage);
		return results;
	}
	
	
	/**
	 * iBatis SqlSession 을 이용한 페이징 목록
	 * 
	 * @param mapper
	 *            해당 mapper의 namespace
	 * @param selectStatement
	 *            select statement id
	 * @param params
	 * @param rowBounds
	 * @return PagedList
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public PagedList<?> search(String mapper, String selectStatement,
			Map<String, ?> params, RowBounds rowBounds) {

		// Total Count set
		Number totalCount = null;
		try {
			totalCount = (Number) sqlSession.selectOne(mapper
				+ ".count-" + selectStatement, params);
		} catch (MyBatisSystemException e) {
			Map<String, Object> countParam = new HashMap<String, Object>(params);
			countParam.put("count", "true");
			Object _cnt = sqlSession.selectOne(mapper + '.' + selectStatement, countParam);
			if ( _cnt instanceof Map ) {
				Map<?,?> cntMap = (Map<?,?>)_cnt;
				totalCount = (Number)cntMap.get( cntMap.containsKey("cnt") ? "cnt" : cntMap.containsKey("count") ? "count" : "" );
			} else {
				totalCount = (Number)_cnt;
			}
		}

		List list = sqlSession.selectList(mapper + '.' + selectStatement,
				params, rowBounds);

		PagedList<?> results = new PagedList(totalCount.intValue(), list);
		results.setPageSize(rowBounds.getLimit());
		results.setCurrentPage(rowBounds.getOffset() / rowBounds.getLimit() + 1);

		return results;
	}
	
	

	/**
	 * iBatis SqlSession 을 이용한 입력
	 * 
	 * @param mapper
	 *            해당 mapper의 namespace
	 * @param selectStatement
	 *            select statement id
	 * @param params
	 * @return
	 */
	public List<?> search(String selectStatement,
			Map<String, ?> params) {
		return search(this.getClass().getSimpleName(), selectStatement, params);
	}

	public List<?> search(String mapper, String selectStatement,
			Map<String, ?> params) {
		return sqlSession.selectList(mapper + '.' + selectStatement, params);
	}

	public List<?> search(String mapper, String selectStatement, String value) {
		return sqlSession.selectList(mapper + '.' + selectStatement, value);
	}

	public List<?> search(String mapper, String selectStatement) {
		return sqlSession.selectList(mapper + '.' + selectStatement);
	}

	public Object searchOne(String selectStatement, Map<String, ?> params) {
		return searchOne(this.getClass().getSimpleName(), selectStatement, params);
	}
	
	public Object searchOne(String mapper, String selectStatement,
			Map<String, ?> params) {
		return sqlSession.selectOne(mapper + '.' + selectStatement, params);
	}
	
	public Object searchOne(String mapper, String selectStatement,
			String value) {
		return sqlSession.selectOne(mapper + '.' + selectStatement, value);
	}

	public void insertOne(String mapper, String insertStatement,
			Map<String, ?> params) {
		sqlSession.insert(mapper + '.' + insertStatement, params);
	}

	public void insertOne(String mapper, String insertStatement, String value) {
		sqlSession.insert(mapper + '.' + insertStatement, value);
	}
	
	public void update(String mapper, String updateStatement,
			Map<String, ?> params) {
		sqlSession.update(mapper + '.' + updateStatement, params);
	}
	
	public void update(String mapper, String updateStatement, String value) {
		sqlSession.update(mapper + '.' + updateStatement, value);
	}

	public void deleteOne(String mapper, String deleteStatement,
			Map<String, ?> params) {
		sqlSession.delete(mapper + '.' + deleteStatement, params);
	}

	public void deleteOne(String mapper, String deleteStatement, String value) {
		sqlSession.delete(mapper + '.' + deleteStatement, value);
	}

	/* ******************************************
	 * iBatis 용 멤버 끝
	 * ******************************************/
}
