package net.autocrm.api.service.impl;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.sql.DataSource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import com.sfa.util.Configuration;
import com.sfa.util.Utility;

import lombok.extern.slf4j.Slf4j;
import net.autocrm.api.service.CommonManager;
import net.autocrm.api.service.WdmsLinkManager;
import net.autocrm.api.util.WdmsEncryption;
import net.autocrm.common.model.Parameters;

@Slf4j
@Service("wlMgr")
public class WdmsLinkManagerImpl extends CommonManager implements WdmsLinkManager {
	
	@Autowired
	protected MessageSource msg;

	@Autowired
	protected SqlSession	sqlSession;

	@Autowired
	protected DataSource	datasource;
	
	@Autowired WdmsEncryption wdmsEnc;
	
	@Override
	@Transactional
	public Map<?, ?> wdmsCustomerSave(Map<String,?> map) {
		log.info("Start wdmsCustomerSave");
		
		Parameters<String, String> instParams = new Parameters<String, String>();
		Parameters<String, String> instAddrParams = new Parameters<String, String>();
		Parameters<String, String> instAgreeParams = new Parameters<String, String>();
		Parameters<String, String> crmParams = new Parameters<String, String>();
		List<Map<String, Object>> rtn = new ArrayList<>();
		List<?> datas = (List<?>) map.get("DATA");
		String ListLength = datas.toString(); 
		//2023-12-14 수정
		//BAVARIAN-1282 고객정보 수신 시, JSON 길이가 10,000이하인 건만 업데이트 처리
		//https://jira.autocrm.net/jira/browse/BAVARIAN-1282
		log.info("ListLength.size() = "+ListLength.length());
		if(ListLength.length() >= 10000) {
			log.warn("Json Limit Length Overflow");
			Map<String, Object> rtn1 = new LinkedHashMap<>();
			rtn1.put("RST_CD", "99");
			rtn1.put("RST_MSG", "FAIL");
			rtn1.put("CRM_MSG", "Json Limit Length Overflow");
			return rtn1;
		}
		
		datas.forEach(o -> {
			Map<?,?> m = (Map<?,?>)o;
			List<?> jCustAddrList = (List<?>) m.get("CUST_ADDR");
			List<?> jCustAgreeList = (List<?>) m.get("CUST_AGRE");
			Map<?,?> jCust = (Map<?,?>)m.get("CUST");
			Map<?,?> jSc = (Map<?,?>)m.get("SC");
			Map<?,?> jCorp = (Map<?,?>)m.get("CORP");

			String custNo = (String) jCust.get("CUST_NO");
			String dlrCd = (String) jCust.get("DLR_CD");

			Map<String, Object> rslt = new LinkedHashMap<>();
			rtn.add(rslt);
		    rslt.put("DLR_CD", dlrCd);
		    rslt.put("CUST_NO", custNo);

		    try {
				String scBrand = (String) jSc.get("EMPL_BRAND_CD");
				if ( StringUtils.isBlank(scBrand) ) {
					rslt.put("RST_CD", "99");
					rslt.put("RST_MSG", "FAIL");
					rslt.put("CRM_MSG", "Please Check EMPL_BRAND_CD ");
					return;
				}
				
				if ( StringUtils.isBlank(dlrCd) || !dlrCd.equals( getClient().getWdmsid() ) ) {
					rslt.put("RST_CD", "99");
					rslt.put("RST_MSG", "FAIL");
					rslt.put("CRM_MSG", "Please Check DLR_CD ");
					return;
				}
	
				instParams.put("dlrCd", dlrCd);
		    	instParams.put("custNo", custNo);
		    	instParams.put("bpCd", (String) jCust.get("BP_CD"));
		    	instParams.put("custTp", (String) jCust.get("CUST_TP"));
		    	String wdmsNm = (String) jCust.get("CUST_NM");
		    	String decNm = HtmlUtils.htmlUnescape(wdmsNm);
		    	
		    	instParams.put("custNm", decNm);
		    	instParams.put("brchCd", (String) jCust.get("BRCH_CD"));
		    	instParams.put("custDstinTp", (String) jCust.get("CUST_DSTIN_TP"));
		    	instParams.put("custIfwTp", (String) jCust.get("CUST_IFW_TP"));
		    	instParams.put("custNm", (String) jCust.get("CUST_NM"));
		    	instParams.put("frgnrYn", (String) jCust.get("FRGNR_YN"));
		    	
		    	String hpPhone = wdmsEnc.AES_Decode((String) jCust.get("HP_NO"));
		    	instParams.put("hpNo", hpPhone);
		    	instParams.put("blackCustCd", (String) jCust.get("BLACK_CUST_CD"));
		    	instParams.put("email", (String) jCust.get("EMAIL"));
		    	instParams.put("emailRcvYn", (String) jCust.get("EMAIL_RCV_YN"));
		    	instParams.put("hpRcvYn", (String) jCust.get("HP_RCV_YN"));
		    	
		    	String postRcvYn = (String) jCust.get("POST_RCV_YN");
		    	instParams.put("postRcvYn", postRcvYn);
		    	String postRcvTp = (String) jCust.get("POST_RCV_TP");
		    	instParams.put("postRcvTp", postRcvTp);
		    	/*
		    	crm 
		    	postSend 1 직장 /2 자택 /3 수신거부
		    	 
		    	wdms
		    	POST_RCV_YN	우편수신여부
				POST_RCV_TP	우편물수신지유형
		    	우편물수신지유형	[H] 자택 [O] 회사 [E] 직접입력
		    	*/
		    	if("Y".equals(postRcvYn)) {
		    		if("H".equals(postRcvTp)) {
		    			instParams.put("crmPostRcvYn", "1");
		    		}else if("O".equals(postRcvTp)) {
		    			instParams.put("crmPostRcvYn", "2");
		    		}else {
		    			instParams.put("crmPostRcvYn", null);
		    		}
		    	} else {
		    		instParams.put("crmPostRcvYn", "3");
		    	}
		    	
		    	instParams.put("telRcvAgreYn", (String) jCust.get("TEL_RCV_AGRE_YN"));
		    	instParams.put("smsRcvAgreYn", (String) jCust.get("SMS_RCV_AGRE_YN"));
		    	instParams.put("delYn", (String) jCust.get("DEL_YN"));
		    	instParams.put("delDt", (String) jCust.get("DEL_DT"));
		    	instParams.put("custInfoRemark", (String) jCust.get("CUST_INFO_REMARK"));
		    	instParams.put("scId", (String) jSc.get("SC_ID"));
		    	instParams.put("emplNo", (String) jSc.get("EMPL_NO"));
		    	instParams.put("emplBrandCd", (String) jSc.get("EMPL_BRAND_CD"));
		    	
		    	instParams.put("bpCorpCd", (String) jCorp.get("BP_CORP_CD"));
		    	instParams.put("bpCorpNm", (String) jCorp.get("BP_CORP_NM"));
		    	instParams.put("bpCorpEnNm", (String) jCorp.get("BP_CORP_EN_NM"));
		    	instParams.put("bizTp", (String) jCorp.get("BIZ_TP"));
		    	instParams.put("bizCond", (String) jCorp.get("BIZ_COND"));
		    	instParams.put("corpRegNo", (String) jCorp.get("CORP_REG_NO"));
		    	instParams.put("bsnmRegNo", (String) jCorp.get("BSNM_REG_NO"));
		    	instParams.put("reprsntNm", (String) jCorp.get("REPRSNT_NM"));
		    	instParams.put("corpEmail", (String) jCorp.get("EMAIL"));
		    	instParams.put("corpTelNo", (String) jCorp.get("TEL_NO"));
		    	instParams.put("corpFaxNo", (String) jCorp.get("FAX_NO"));
		    	instParams.put("fdationDt", (String) jCorp.get("FDATION_DT"));
		    	instParams.put("chrgrNm", (String) jCorp.get("CHRGR_NM"));
		    	instParams.put("chrgrTelNo", (String) jCorp.get("TEL_NO"));
		    	instParams.put("corpZipNo", (String) jCorp.get("ZIP_NO"));
		    	instParams.put("corpAddr", (String) jCorp.get("ADDR"));
		    	instParams.put("corpDetlAddr", (String) jCorp.get("DETL_ADDR"));
		    	instParams.put("corpUseYn", (String) jCorp.get("USE_YN"));
		    	instParams.put("corpRemark", (String) jCorp.get("REMARK"));
		    	
		    	//단일 WDMS 정보 저장
		    	log.info("Start insertWdmsCustomer");
		    	log.info("instParams = "+instParams);
		    	insertWdmsCustomer(instParams);
	
		    	// WDMS 다중 정보 주소
		    	if( CollectionUtils.isNotEmpty(jCustAddrList) ) {
		    		jCustAddrList.forEach(a -> {
		    			Map<?,?> jCustAddr = (Map<?,?>)a;
			    		instAddrParams.put("custNo", (String) jCustAddr.get("CUST_NO"));
			    		instAddrParams.put("dlrCd", (String) jCustAddr.get("DLR_CD"));
			    		instAddrParams.put("addrDstinCd", (String) jCustAddr.get("ADDR_DSTIN_CD"));
			    		instAddrParams.put("zipNo", (String) jCustAddr.get("ZIP_NO"));
			    		instAddrParams.put("addr", (String) jCustAddr.get("ADDR"));
			    		instAddrParams.put("detlAddr", (String) jCustAddr.get("DETL_ADDR"));
			    		
			    		insertWdmsCustomerAddr(instAddrParams);
		    		});
		    	}
		    	
		    	
			    //WDMS 다중 정보 고객동의
		    	if( CollectionUtils.isNotEmpty(jCustAgreeList) ) {
		    		jCustAgreeList.forEach(a -> {
		    			Map<?,?> jCustAgree = (Map<?,?>)a;
			    		instAgreeParams.put("custNo", (String) jCustAgree.get("CUST_NO"));
			    		instAgreeParams.put("dlrCd", (String) jCustAgree.get("DLR_CD"));
			    		instAgreeParams.put("seqNo", (String) jCustAgree.get("SEQ_NO"));
			    		instAgreeParams.put("agreDt", (String) jCustAgree.get("AGRE_DT"));
			    		instAgreeParams.put("esstAgreEYn", (String) jCustAgree.get("ESST_AGREE_YN"));
			    		instAgreeParams.put("makAgreYn", (String) jCustAgree.get("MAK_AGRE_YN"));
			    		instAgreeParams.put("thirdAgreYn", (String) jCustAgree.get("THIRD_AGRE_YN"));
			    		instAgreeParams.put("thirdAgre1Yn", (String) jCustAgree.get("THIRD_AGRE1_YN"));
			    		instAgreeParams.put("thirdAgre2Yn", (String) jCustAgree.get("THIRD_AGRE2_YN"));
			    		instAgreeParams.put("thirdAgre3Yn", (String) jCustAgree.get("THIRD_AGRE3_YN"));
			    		instAgreeParams.put("thirdAgreCnclDt", (String) jCustAgree.get("THIRD_AGRE_CNCL_DT"));
			    		instAgreeParams.put("useYn", (String) jCustAgree.get("USE_YN"));
			    		
			    		insertWdmsCustomerAgree(instAgreeParams);
				    });
		    	}
			    
			    String scBrandDb = Configuration.getInstance().getString("wdms.brand."+scBrand); //딜러코드
				if ( StringUtils.isBlank(scBrandDb) ) {
					rslt.put("RST_CD", "99");
					rslt.put("RST_MSG", "FAIL");
					rslt.put("CRM_MSG", "This EMPL_BRAND_CD '" + scBrand + "' is not used in this system.");
					return;
				}
			    log.info("scBrandDb="+scBrandDb);
			    // CRM DB 업데이트
			    // CRM CUSTOMER_HEADERS 컬럼 추가
			    crmParams.put("dbName", scBrandDb);
			    crmParams.put("wdmsCustNo", (String) jCust.get("CUST_NO"));
			    crmParams.put("scId", (String) jSc.get("SC_ID"));
			    //crmParams.put("wdmsDlrCd", (String) jCust.get("DLR_CD"));
			    
			    //CRM CUSTOMER에 wdms고객번호 검색
			    List<?> srchCrmCustList = searchCrmCustomerList(crmParams);
			    
			    if(srchCrmCustList!=null) {
			    	if(srchCrmCustList.size()>0) {
			    		log.info("updateCrmCustomer Start");
			    		updateCrmCustomer(datas);
			    	} else {
			    		log.info("insertCrmCustomer Start");
			    		insertCrmCustomer(datas);
			    	}
			    } else {
			    	log.info("insertCrmCustomer Start");
			    	insertCrmCustomer(datas);
			    }

			    rslt.put("RST_CD", "00");
			    rslt.put("RST_MSG", "SUCCESS");
		    } catch ( Throwable e) {
				rslt.put("RST_CD", "99");
				rslt.put("RST_MSG", "FAIL");
				rslt.put("CRM_MSG", ExceptionUtils.getRootCauseMessage(e));
			}

		});
	    
	    return format(map, rtn);
	}
	
	@Override
	@Transactional
	public Map<?, ?> wdmsContractInfoSave(Map<String,?> map) {
		log.info("Start wdmsContractInfoSave");
		Parameters<String, Object> instContractInfo = new Parameters<>();
		Parameters<String, Object> instContractCust = new Parameters<>();
		Parameters<String, Object> instCrmContract = new Parameters<>();
		Parameters<String, Object> instCrmVacs = new Parameters<>();
		
		Parameters<String, Object> searchParams = new Parameters<>();

		List<Map<String, Object>> rtn = new ArrayList<>();

		List<?> datas = (List<?>)map.get("DATA");
		datas.forEach(o -> {
			Map<?,?> m = (Map<?,?>)o;

			Map<String, Object> rslt = new LinkedHashMap<>();
			rtn.add(rslt);

			List<?> jConInfoList = (List<?>) m.get("CONTRACT");
			Map<?,?> jCon = (Map<?,?>) jConInfoList.stream().findFirst().get();	// 단건으로 처리

			String dlrCd = (String) jCon.get("DLR_CD");
			String brchCd = (String) jCon.get("BRCH_CD");
			String contNo = (String) jCon.get("CONT_NO");
			
			if ( StringUtils.isBlank(dlrCd) || !dlrCd.equals( getClient().getWdmsid() ) ) {
				rslt.put("IF_RST_CD", "99");
				rslt.put("IF_RST_MSG", "FAIL");
				rslt.put("CRM_MSG", "Please Check DLR_CD ");
				return;
			}

			rslt.put("DLR_CD", dlrCd);
    		rslt.put("BRCH_CD", brchCd);
    		rslt.put("CONT_NO", contNo);

			String wdmsScID = (String) jCon.get("SC_ID");
	    	searchParams.put("wdmsUserId", wdmsScID);
	    	String contBrandCd = (String) jCon.get("BRAND_CD");
	    	
	    	String scBrand = null;
	    	String salesUserSeq = null;

		    //Contract List에서 SC_ID를 가져와 BRAND 결정
	    	List<?> wdmsScList = searchWdmsUserList(searchParams);
	    	log.info("BRAND_CHECKER");
	    	if(!CollectionUtils.isNotEmpty(wdmsScList)) {
	    		log.info("BRAND_LIST"+wdmsScList.size());
	    		if(wdmsScList.size()>0) {
	    			Map<?,?> sc = (Map<?,?>) wdmsScList.get(0);
	    			scBrand = (String) sc.get("EMPL_BRAND_CD");
	    			salesUserSeq = (String) sc.get("SALES_USER_SEQ");
	    		}else {
	    			if("M".equals(contBrandCd)) {
		    			scBrand = "M";
		    		} else {
		    			scBrand = "B";
		    		}
	    			String scBrandDb = Configuration.getInstance().getString("wdms.brand."+scBrand); //딜러코드
		    		searchParams.put("dbName", scBrandDb);
		    	    List<?> userList = searchCrmUserList(searchParams);
		    	    if(CollectionUtils.isNotEmpty(userList)) {
		    	    	Map<?,?> sc = (Map<?,?>) userList.get(0);
						salesUserSeq = (String) sc.get("SALES_USER_SEQ");
		    	    }
	    		}
	    	} else if(!"".equals(contBrandCd) ){
	    		log.info("BRAND_CD");
	    		if("M".equals(contBrandCd)) {
	    			scBrand = "M";
	    		} else {
	    			scBrand = "B";
	    		}
	    		
	    		String scBrandDb = Configuration.getInstance().getString("wdms.brand."+scBrand); //딜러코드
	    		searchParams.put("dbName", scBrandDb);
	    	    List<?> userList = searchCrmUserList(searchParams);
	    	    if(CollectionUtils.isNotEmpty(userList)) {
	    	    	Map<?,?> sc = (Map<?,?>) userList.get(0);
					salesUserSeq = (String) sc.get("SALES_USER_SEQ");
	    	    }
	    	} else {
	    		log.info("ELSE FAIL");
				rslt.put("IF_RST_CD", "99");
				rslt.put("IF_RST_MSG", "FAIL");
				rslt.put("CRM_MSG", "ELSE FAIL");
				return;
	    	}
	    	
	    	if(scBrand == null || scBrand == "") {
	    		log.info(""+"wdmsScID / scBrand="+wdmsScID+"/"+scBrand);
				rslt.put("IF_RST_CD", "99");
				rslt.put("IF_RST_MSG", "FAIL");
				rslt.put("CRM_MSG", "Please Check EMPL_BRAND_CD");
				return;
	    	}
	    	
	    	String scBrandDb = Configuration.getInstance().getString("wdms.brand."+scBrand); //딜러코드
	    	
	    	instContractInfo.put("scId", wdmsScID);
	    	instContractInfo.put("crmBrand", scBrand);
	    	
	    	instCrmContract.put("dbName", scBrandDb);
	    	instCrmContract.put("salesUserSeq", salesUserSeq);
	    	
	    	instCrmVacs.put("dbName", scBrandDb);
	    	instCrmVacs.put("salesUserSeq", salesUserSeq);
	    	
	    	instContractInfo.put("dlrCd", dlrCd);
	    	instContractInfo.put("brchCd", brchCd);
			instContractInfo.put("contNo", contNo);
			
			String contTp = (String) jCon.get("CONT_TP");
			instContractInfo.put("emplNo", (String) jCon.get("EMPL_NO"));
			instContractInfo.put("contDt", (String) jCon.get("CONT_DT"));
			instContractInfo.put("contStatCd", (String) jCon.get("CONT_STAT_CD"));
			instContractInfo.put("contTp", contTp);
			instContractInfo.put("carUsageTp", (String) jCon.get("CAR_USAGE_TP"));
			instContractInfo.put("custNo", (String) jCon.get("CUST_NO"));
			instContractInfo.put("brandCd", (String) jCon.get("BRAND_CD"));
			instContractInfo.put("seriesCd", (String) jCon.get("SERIES_CD"));
			instContractInfo.put("modelCd", (String) jCon.get("MODEL_CD"));
			instContractInfo.put("modelYear", (String) jCon.get("MODEL_YEAR"));
			instContractInfo.put("pkgGrpCd", (String) jCon.get("PKG_GRP_CD"));
			instContractInfo.put("vinNo", (String) jCon.get("VIN_NO"));
			instContractInfo.put("exColorCd", (String) jCon.get("EX_COLOR_CD"));
			instContractInfo.put("intColorCd", (String) jCon.get("INT_COLOR_CD"));
			instContractInfo.put("bankCd", (String) jCon.get("BANK_CD"));
			String acctNo = (String) jCon.get("VIRTL_ACCT_NO");
			instContractInfo.put("virtlAcctNo", acctNo);
			
			String contractSeq = (String) jCon.get("VIRTL_ACCT_MNG_NO");
			instContractInfo.put("virtlAcctMngNo", contractSeq);
			
			instCrmContract.put("contractSeq", contractSeq);
	    	instCrmVacs.put("contractSeq", contractSeq);
	    	instCrmVacs.put("acctNo", acctNo);
			
			instContractInfo.put("retlPrc", jCon.get("RETL_PRC"));
			instContractInfo.put("retlPrcVat", jCon.get("RETL_PRC_VAT"));
			
			instContractInfo.put("prcEditYn", (String) jCon.get("PRC_EDIT_YN"));
			instContractInfo.put("remark", (String) jCon.get("REMARK"));
			
			instContractInfo.put("paymRefNo", (String) jCon.get("PAYM_REF_NO"));
			
			//기타계약 연동 관련 추가 컬럼
			instContractInfo.put("useBrchCd", (String) jCon.get("USE_BRCH_CD"));
			instContractInfo.put("costAmt", jCon.get("COST_AMT"));
			instContractInfo.put("costVat", jCon.get("COST_VAT"));
			instContractInfo.put("vatAmt", jCon.get("VAT_AMT"));
			instContractInfo.put("vatVat", jCon.get("VAT_VAT"));
			
			Map<?,?> vacsCode = getCrmVacsInfo(instCrmContract);
			String vacs_org_cd = (String) vacsCode.get("RESTRICT_1");
			String vacs_bank_cd = (String) vacsCode.get("RESTRICT_2");
			String vacs_bank_nm = (String) vacsCode.get("RESTRICT_NAME_1");
//				String vacs_cust_nm = (String) vacsCode.get("RESTRICT_NAME_2");
			
	    	instCrmVacs.put("orgCd", vacs_org_cd);
	    	instCrmVacs.put("bankCd", vacs_bank_cd);
	    	instCrmVacs.put("bankNm", vacs_bank_nm);
			
			log.info("Start insertWdmsContractInfo");
			insertWdmsContractInfo(instContractInfo);
			log.info("END insertWdmsContractInfo");

			List<?> jConCustList = (List<?>) m.get("CONTRACT_CUST");
			jConCustList.forEach(cust -> {
				Map<?,?> jCust = (Map<?,?>) cust;
		    	instContractCust.put("dlrCd", (String) jCust.get("DLR_CD"));
		    	instContractCust.put("brchCd", (String) jCust.get("BRCH_CD"));
		    	instContractCust.put("contNo", (String) jCust.get("CONT_NO"));
		    	instContractCust.put("custNo", (String) jCust.get("CUST_NO"));
		    	instContractCust.put("custDstinTp", (String) jCust.get("CUST_DSTIN_TP"));
		    	instContractCust.put("custTp", (String) jCust.get("CUST_TP"));
		    	instContractCust.put("custNm", (String) jCust.get("CUST_NM"));
		    	log.info("NEXT 1");
				try {
			    	instContractCust.put("ssnRegNo", wdmsEnc.AES_Decode( (String) jCust.get("SSN_REG_NO") ));
				} catch (InvalidKeyException | UnsupportedEncodingException | NoSuchAlgorithmException
						| NoSuchPaddingException | InvalidAlgorithmParameterException | IllegalBlockSizeException
						| BadPaddingException e) {
					e.printStackTrace();
				}
		    	log.info("NEXT 2");
				try {
			    	instContractCust.put("hpNo", wdmsEnc.AES_Decode( (String) jCust.get("HP_NO") ));
				} catch (InvalidKeyException | UnsupportedEncodingException | NoSuchAlgorithmException
						| NoSuchPaddingException | InvalidAlgorithmParameterException | IllegalBlockSizeException
						| BadPaddingException e) {
					e.printStackTrace();
				}
		    	log.info("NEXT 3");
		    	instContractCust.put("email", (String) jCust.get("EMAIL"));
		    	instContractCust.put("zipNo", (String) jCust.get("ZIP_NO"));
		    	instContractCust.put("addr", (String) jCust.get("ADDR"));
		    	instContractCust.put("detlAddr", (String) jCust.get("DETL_ADDR"));
		    	instContractCust.put("bpNm", (String) jCust.get("BP_NM"));
		    	instContractCust.put("bpCd", (String) jCust.get("BP_CD"));
		    	instContractCust.put("corpRegNo", (String) jCust.get("CORP_REG_NO"));
		    	instContractCust.put("bsnmRegNo", (String) jCust.get("BSNM_REG_NO"));
		    	instContractCust.put("telNo", (String) jCust.get("TEL_NO"));
		    	instContractCust.put("bpZipNo", (String) jCust.get("BP_ZIP_NO"));
		    	instContractCust.put("bpAddr", (String) jCust.get("BP_ADDR"));
		    	instContractCust.put("bpDetlAddr", (String) jCust.get("BP_DETL_ADDR"));
		    	
		    	log.info("Start insertWdmsContractCust");
		    	log.debug(""+instContractCust);
		    	insertWdmsContractCust(instContractCust);
				
			});

			rslt.put("IF_RST_CD", "00");
			rslt.put("IF_RST_MSG", "SUCCESS");
		});
		
		return format(map, rtn);
	
	}
	
	@SuppressWarnings("unchecked")
	@Override
	@Transactional
	public Map<?, ?> wdmsUsedContractInfoSave(Map<String,?> map) {
		log.debug("wdmsUsedContractInfoSave"+map.toString());
		Parameters<String, String> searchParams = new Parameters<String, String>();
		
		List<Map<String, Object>> rtn = new ArrayList<>();

		List<?> datas = (List<?>)map.get("DATA");
		
		datas.forEach(o -> {
			Map<?,?> m = (Map<?,?>)o;

			Map<String, Object> rslt = new LinkedHashMap<>();
			rtn.add(rslt);

			List<?> jConInfoList = (List<?> ) m.get("USED_CONTRACT"); 
			List<?> jConCustList = (List<?> ) m.get("CONTRACT_CUST");
			List<?> jConFileList = (List<?> ) m.get("USED_CONTRACT_FILE");

			Map<String,Object> jCon = (Map<String,Object>) jConInfoList.stream().findFirst().get();	// 단건으로 처리

			String dlrCd = (String) jCon.get("DLR_CD");
			String brchCd = (String) jCon.get("BRCH_CD");
			String usedContNo = (String) jCon.get("USED_CONT_NO");
			String contStep = (String) jCon.get("CONT_STEP_CD");
			if ( StringUtils.isBlank(dlrCd) || !dlrCd.equals( getClient().getWdmsid() ) ) {
				rslt.put("IF_RST_CD", "99");
				rslt.put("IF_RST_MSG", "FAIL");
				rslt.put("CRM_MSG", "Please Check DLR_CD ");
				return;
			}

			rslt.put("DLR_CD", dlrCd);
    		rslt.put("BRCH_CD", brchCd);
    		rslt.put("USED_CONT_NO", usedContNo); 

			String scBrand = null;
			String wdmsScID = (String) jCon.get("SC_ID");
			searchParams.put("wdmsUserId", wdmsScID);

			//Contract List에서 SC_ID를 가져와 BRAND 결정
			Map<?,?> sc = null;
			try {
				List<?> wdmsScList = searchWdmsUserList(searchParams);
				sc = (Map<?,?>) wdmsScList.stream().findFirst().get();
				scBrand = (String) sc.get("EMPL_BRAND_CD");
				if(scBrand == null || scBrand == "") {
					rslt.put("IF_RST_CD", "99");
					rslt.put("IF_RST_MSG", "FAIL");
					rslt.put("CRM_MSG", "Please Check EMPL_BRAND_CD ");
					return;
				}
			} catch ( NoSuchElementException nsee ) {
				rslt.put("IF_RST_CD", "99");
				rslt.put("IF_RST_MSG", "FAIL");
				rslt.put("CRM_MSG", "Please Check EMPL_BRAND_CD ");
				return;
			}
			/*
			String custNo = (String) jCon.get("CUST_NO");
			searchParams.clear();
			searchParams.put("custNo", custNo);
			List<?> customerList = searchWdmsCustomerList(searchParams);
			if(customerList.isEmpty()){
				rslt.put("IF_RST_CD", "99");
				rslt.put("IF_RST_MSG", "FAIL");
				rslt.put("CRM_MSG", "Please Check CUST_NO ");
				return;
			}
			*/
			if ( "99".equals( rslt.get("IF_RST_CD") ) ) return;
			
			jCon.put("SC_ID", wdmsScID);
			jCon.put("CRM_BRAND", scBrand);
			
			searchParams.put("usedContNo", usedContNo);
			List<?> usedConList =  searchUsedConList(searchParams);

			int contractWdmsSeq;
			if(usedConList.isEmpty()) {
				contractWdmsSeq = insertWdmsUsedContractInfo(jCon);
			} else {
				Map<String,Object> uCon = (Map<String, Object>) usedConList.stream().findFirst().get();
				contractWdmsSeq = NumberUtils.toInt(String.valueOf(uCon.get("CONTRACT_WDMS_SEQ")));
				updateWdmsUsedContractInfo(jCon);
				
				//DEBPS-779 WDMS - 매입품의서 업데이트 로직 추가
				// WDMS에서 '승인요청'으로 전달 받은 경우 승인 내역 초기화
				String saleConferSeq = String.valueOf(uCon.get("SALE_CONFER_SEQ")); //매출
				String purchaseConferSeq = String.valueOf(uCon.get("PURCHASE_CONFER_SEQ")); //매입
				
				// 매입,매출 품의서 작성 후 수정시
				if((saleConferSeq.length() > 0 && !saleConferSeq.equals("null")) || (purchaseConferSeq.length() > 0 && !purchaseConferSeq.equals("null"))) {
					if("01".equals(contStep) ) {
						sqlSession.update("WdmsLinkManagerImpl.RESET_WDMS_USED_CONTRACT_INFO", jCon);
					} else if("03".equals(contStep) ) {
						sqlSession.update("WdmsLinkManagerImpl.RESET_WDMS_USED_CONTRACT_INFO2", jCon);
					}
				} else {
					//DEBPS-1135 매입, 판매전자계약관리 - 승인 처리 후, 승인요청이 넘어오는 경우, '대기'버튼이 활성화 되도록 수정
					if("01".equals(contStep) ) {
						sqlSession.update("WdmsLinkManagerImpl.RESET_WDMS_USED_CONTRACT_INFO", jCon);
					} else if("03".equals(contStep) ) {
						sqlSession.update("WdmsLinkManagerImpl.RESET_WDMS_USED_CONTRACT_INFO2", jCon);
					}
				}
			}
			
			rslt.put("DLR_CD", dlrCd);
			rslt.put("BRCH_CD", brchCd);
			rslt.put("USED_CONT_NO", usedContNo);

			log.info("NEXT jConCustList");
			
			String salesUserSeq = (String) sc.get("SALES_USER_SEQ");
			jConCustList.forEach(c -> {
				Map<String,Object> jCust = (Map<String,Object>) c; 
				
				jCust.put("SALES_USER_SEQ", salesUserSeq);
				jCust.put("CONTRACT_WDMS_SEQ", contractWdmsSeq);
				
				try {
					jCust.put("SSN_REG_NO", wdmsEnc.AES_Decode( (String) jCust.get("SSN_REG_NO") ));
				} catch (InvalidKeyException | UnsupportedEncodingException | NoSuchAlgorithmException
						| NoSuchPaddingException | InvalidAlgorithmParameterException | IllegalBlockSizeException
						| BadPaddingException e) {
					e.printStackTrace();
				}
				
				try {
					jCust.put("HP_NO", wdmsEnc.AES_Decode( (String) jCust.get("HP_NO") ));
				} catch (InvalidKeyException | UnsupportedEncodingException | NoSuchAlgorithmException
						| NoSuchPaddingException | InvalidAlgorithmParameterException | IllegalBlockSizeException
						| BadPaddingException e) {
					e.printStackTrace();
				}
				
				searchParams.put("custNo", String.valueOf(jCust.get("CUST_NO")));
				List<?> usedCustList = searchUsedCustList(searchParams);
				if(usedCustList.isEmpty()) {
					insertWdmsUsedContractCust(jCust);
				} else {
					updateWdmsUsedContractCust(jCust);
				}
				
			});

			log.info("NEXT jConFileList");

			jConFileList.forEach(c -> {
				Map<String,Object> jFile = (Map<String,Object>) c; 
				log.info("NEXT 1");
				jFile.put("CONTRACT_WDMS_SEQ", contractWdmsSeq);
				
				searchParams.put("fileSeqNo", String.valueOf(jFile.get("FILE_SEQ_NO")));
				searchParams.put("fileDetlSeqNo", String.valueOf(jFile.get("FILE_DETL_SEQ_NO")));
				List<?> usedCustList = searchUsedCustList(searchParams);
				if(usedCustList.isEmpty()) {
					insertWdmsUsedContractFile(jFile);
				} else {
					updateWdmsUsedContractFile(jFile);
				}
				
			});

			rslt.put("IF_RST_CD", "00");
			rslt.put("IF_RST_MSG", "SUCCESS");
		});
		return format(map, rtn);
		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Map<?,?> wdmsWholesaleSave(Map<String,?> map){
		List<Map<String, ?>> results = new ArrayList<>();
		List<Map<String, ?>> datas = (List<Map<String, ?>>)map.get("DATA");
		for (Map<String, ?> data : datas) {
			List<?> wholesales = (List<?>)data.get("WHSL_INVC");
			for (Object wholesale : wholesales) {
				sqlSession.insert("stock.insertWholesale", wholesale);
	
				Map<String,Object> result = (Map<String,Object>)wholesale;
	
				// 브랜드별 DB 결정
				String dbName = config.getProperty("wdms.brand." + result.get("BRAND_CD"));
				if ( StringUtils.isBlank(dbName) ) {
					result.put("IF_RST_CD", "99");
					result.put("IF_RST_MSG", "FAIL");
					result.put("CRM_MSG", "BRAND_CD does not exist.");
				} else {
					result.put("dbName", dbName);
		
					_wdmsWholesaleSave(result);	// 위 stock.insertWholesale 은 오류 나더라도 기록될 수 있도록 Transactional 분리.
		
					result.remove("dbName");
					result.remove("seq");
					result.put("IF_RST_CD", "00");
					result.put("IF_RST_MSG", "SUCCESS");
				}
				
				Object[] keys = result.keySet().toArray();
				for (Object key : keys) {
					if ( !ArrayUtils.contains(new String[]{"VIN_NO","SEQ_NO","BILL_NO","BILL_DT", "IF_RST_CD", "IF_RST_MSG", "CRM_MSG"}, key)) {
						result.remove(key);
					}
				}

				results.add( result );
			}
		}
		return format(map, results);
	}

	@Transactional
	public Map<?, ?> wdmsStatsRequest(Map<String,?> map) {
		log.info("STARTwdmsStatsRequest ");
		Parameters<String, String> updateCrmConfer = new Parameters<String, String>();
		Parameters<String, String> searchParams = new Parameters<String, String>();
		Parameters<String, String> crmDbParams = new Parameters<String, String>();
		List<Map<String, Object>> rtn = new ArrayList<>();
	
		List<?> datas = (List<?>)map.get("DATA");
		datas.forEach(o -> {
			Map<?,?> m = (Map<?,?>)o;
	
			List<?> jConInfoList = (List<?>) m.get("CONTRACT_INFO");
			jConInfoList.forEach(info -> {
				Map<?,?> jCon = (Map<?,?>)info;
				String scId = null;
				String scBrand = null;
				String saleConferSeq = null;
				String contNo = (String) jCon.get("VIRTL_ACCT_MNG_NO");
		    	searchParams.put("contNo", contNo);
				
				Map<String, Object> rslt = new LinkedHashMap<>();
				rtn.add(rslt);
	
		    	rslt.put("DLR_CD", (String) jCon.get("DLR_CD"));
		    	rslt.put("BRCH_CD", (String) jCon.get("BRCH_CD"));
		    	rslt.put("NCAR_DSTIN_CD", (String) jCon.get("NCAR_DSTIN_CD"));
		    	rslt.put("CONT_STAT_CD", (String) jCon.get("CONT_STAT_CD"));
		    	rslt.put("CONT_NO", (String) jCon.get("CONT_NO"));
		    	rslt.put("CONT_APPR_NO", (String) jCon.get("CONT_APPR_NO"));
	
			    //Contract List에서 SC_ID를 가져와 BRAND 결정
		    	List<?> wdmsScList = searchWdmsUserToContNoList(searchParams);
				try {
					Map<?,?> sc = (Map<?,?>) wdmsScList.stream().findFirst().get();
		    		scId = (String) sc.get("SC_ID");
		    		scBrand = (String) sc.get("EMPL_BRAND_CD");
		    		if(scBrand == null || "".equals(scBrand)) {
		    			scBrand = (String) sc.get("EMPL_BRAND_CD2");
		    		}
				} catch ( NoSuchElementException nsee ) {
					rslt.put("IF_RST_CD", "99");
					rslt.put("IF_RST_MSG", "FAIL");
					rslt.put("CRM_MSG", "Please Check VIRTL_ACCT_MNG_NO");
					return;
				}
		    	
		    	String scBrandDb = Configuration.getInstance().getString("wdms.brand."+scBrand); //딜러코드
				if ( StringUtils.isBlank(scBrandDb) ) {
					rslt.put("RST_CD", "99");
					rslt.put("RST_MSG", "FAIL");
					rslt.put("CRM_MSG", "This EMPL_BRAND_CD '" + scBrand + "' is not used in this system.");
					return;
				}
		    	searchParams.put("dbName", scBrandDb);
		    	List<?> crScList = searchCrmConferToContNoList(searchParams);
				try {
					Map<?,?> confer = (Map<?,?>) crScList.stream().findFirst().get();
		    		saleConferSeq = (String) confer.get("SALE_CONFER_SEQ");
				} catch ( NoSuchElementException nsee ) {
					rslt.put("IF_RST_CD", "99");
					rslt.put("IF_RST_MSG", "FAIL");
					return;
				}
	
				updateCrmConfer.put("scId", scId);
		    	updateCrmConfer.put("crmBrand", scBrand);
		    	updateCrmConfer.put("dbName", scBrandDb);
		    	
		    	updateCrmConfer.put("saleConferSeq", saleConferSeq);
		    	
		    	updateCrmConfer.put("dlrCd",(String) jCon.get("DLR_CD"));
		    	updateCrmConfer.put("brchCd",(String) jCon.get("BRCH_CD"));
		    	updateCrmConfer.put("contStatCd",(String) jCon.get("CONT_STAT_CD"));
		    	updateCrmConfer.put("contNo",(String) jCon.get("CONT_NO"));
		    	updateCrmConfer.put("ncarDstinCd",(String) jCon.get("NCAR_DSTIN_CD"));
		    	updateCrmConfer.put("contApprNo",(String) jCon.get("CONT_APPR_NO"));
		    	updateCrmConfer.put("virtlAcctMngNo",(String) jCon.get("VIRTL_ACCT_MNG_NO"));
		    	updateCrmConfer.put("carRegDt",(String) jCon.get("CAR_REG_DT"));
		    	updateCrmConfer.put("carRegNo",(String) jCon.get("CAR_REG_NO"));
		    	updateCrmConfer.put("vinNo",(String) jCon.get("VIN_NO"));
		    	updateCrmConfer.put("salesCnfmYn",(String) jCon.get("SALES_CNFM_YN"));
				log.info("insertWdmsConferStats");
				insertWdmsConferStats(updateCrmConfer);
				updateCrmConferStats(updateCrmConfer);
				crmDbParams.put("dbName", scBrandDb);
				
				Map<?, ?> conMap = getCrmFixedConfer(updateCrmConfer);
				String autoSeq = "1";
				String customerSeq = null;
				if(conMap != null) {
					autoSeq = String.valueOf(conMap.get("AUTO_SEQ"));
					customerSeq = String.valueOf(conMap.get("CUSTOMER_SEQ"));
					sqlSession.update("WdmsLinkManagerImpl.GET_UPDATE_CRM_FIXED_CONFER", updateCrmConfer);
					crmDbParams.put("tableParams","&p_customerSeq="+customerSeq+"&p_autoSeq="+autoSeq);
			    	crmDbParams.put("tableName","CUSTOMER_AUTO");
			    	crmToplinkNotify(crmDbParams);
				}
				
				crmDbParams.put("tableName","SALE_CONFER_EXT");
		    	crmDbParams.put("tableParams","&p_saleConferSeq="+saleConferSeq);
		    	crmToplinkNotify(crmDbParams);
		    	crmDbParams.put("tableName","SALE_CONFER");
		    	crmToplinkNotify(crmDbParams);
	
				rslt.put("IF_RST_CD", "00");
				rslt.put("IF_RST_MSG", "SUCCESS");
	
			});
		});
		
		return format(map, rtn);
	
	}

	@Transactional
	private int insertWdmsUsedContractInfo(Map<String, Object> jCon) {
		sqlSession.insert("WdmsLinkManagerImpl.INSERT_WDMS_USED_CONTRACT_INFO", jCon);
		return (int) jCon.get("CONTRACT_WDMS_SEQ");
	}
	
	@Transactional
	private void insertWdmsUsedContractCust(Map<String, Object> jCust) {
		sqlSession.insert("WdmsLinkManagerImpl.INSERT_WDMS_USED_CONTRACT_CUST", jCust);
	}
	
	@Transactional
	private void insertWdmsUsedContractFile(Map<String, Object> jFile) {
		sqlSession.insert("WdmsLinkManagerImpl.INSERT_WDMS_USED_CONTRACT_FILE", jFile);
	}
	
	@Transactional
	private void updateWdmsUsedContractInfo(Map<String, Object> jCon) {
		sqlSession.update("WdmsLinkManagerImpl.UPDATE_WDMS_USED_CONTRACT_INFO", jCon);
		//return (int) jCon.get("CONTRACT_WDMS_SEQ");
	}
	
	@Transactional
	private void updateWdmsUsedContractCust(Map<String, Object> jCust) {
		sqlSession.update("WdmsLinkManagerImpl.UPDATE_WDMS_USED_CONTRACT_CUST", jCust);
	}
	
	@Transactional
	private void updateWdmsUsedContractFile(Map<String, Object> jFile) {
		sqlSession.update("WdmsLinkManagerImpl.UPDATE_WDMS_USED_CONTRACT_FILE", jFile);
	}

	private List<?> searchWdmsCustomerList(Parameters<String, String> params) {
		List<?> rtnList = sqlSession.selectList("WdmsLinkManagerImpl.SEARCH_WDMS_CUSTOMER_LIST", params);
		return rtnList;
		
	}
	
	private List<?> searchUsedConList(Parameters<String, String> params) {
		List<?> rtnList = sqlSession.selectList("WdmsLinkManagerImpl.SEARCH_USED_CON_LIST", params);
		return rtnList;
		
	}
	
	private List<?> searchUsedCustList(Parameters<String, String> params) {
		List<?> rtnList = sqlSession.selectList("WdmsLinkManagerImpl.SEARCH_USED_CUST_LIST", params);
		return rtnList;
		
	}
	
	private List<?> searchUsedFileList(Parameters<String, String> params) {
		List<?> rtnList = sqlSession.selectList("WdmsLinkManagerImpl.SEARCH_USED_FILE_LIST", params);
		return rtnList;
		
	}
	
	private List<?> searchCrmCustomerList(Parameters<String, ?> params) {
		List<?> rtnList = sqlSession.selectList("WdmsLinkManagerImpl.SELECT_CRM_CUSTOMER_HEADERS", params);
		return rtnList;
		
	}
	
	private List<?> searchCrmUserList(Parameters<String, ?> params) {
		List<?> rtnList = sqlSession.selectList("WdmsLinkManagerImpl.SELECT_CRM_USER", params);
		return rtnList;
	}
	
	private List<?> searchWdmsUserList(Parameters<String, ?> params) {
		List<?> rtnList = sqlSession.selectList("WdmsLinkManagerImpl.SELECT_WDMS_USER", params);
		return rtnList;
	}
	
	private List<?> searchWdmsUserToContNoList(Parameters<String, ?> params) {
		List<?> rtnList = sqlSession.selectList("WdmsLinkManagerImpl.SELECT_WDMS_USER_TO_CONTNO", params);
		return rtnList;
	}
	
	private List<?> searchCrmConferToContNoList(Parameters<String, ?> params) {
		List<?> rtnList = sqlSession.selectList("WdmsLinkManagerImpl.SELECT_CRM_CONFER_TO_CONTNO", params);
		return rtnList;
	}
	
	private Map<?,?> getCrmVacsInfo(Parameters<String, ?> params) {
		return sqlSession.selectOne("WdmsLinkManagerImpl.GET_CRM_VACS_INFO", params);
	}
	
	private Map<?,?> getCrmFixedConfer(Parameters<String, ?> params) {
		return sqlSession.selectOne("WdmsLinkManagerImpl.SELECT_CRM_FIXED_CONFER", params);
	}
	
	@Transactional
	private void insertWdmsCustomer(Parameters<String, ?> params) {
		sqlSession.insert("WdmsLinkManagerImpl.INSERT_WDMS_CUSTOMER", params);
	}
	
	@Transactional
	private void insertWdmsCustomerAddr(Parameters<String, ?> params) {
		sqlSession.insert("WdmsLinkManagerImpl.INSERT_WDMS_CUSTOMER_ADDR", params);
	}
	
	@Transactional
	private void insertWdmsCustomerAgree(Parameters<String, ?> params) {
		sqlSession.insert("WdmsLinkManagerImpl.INSERT_WDMS_CUSTOMER_AGREE", params);
	}
	
	@Transactional
	private void insertWdmsContractInfo(Parameters<String, ?> params) {
		sqlSession.insert("WdmsLinkManagerImpl.INSERT_WDMS_CONTRACT_INFO", params);
	}
	
	@Transactional
	private void insertWdmsContractCust(Parameters<String, ?> params) {
		sqlSession.insert("WdmsLinkManagerImpl.INSERT_WDMS_CONTRACT_CUST", params);
	}
	
	@Transactional
	private void insertWdmsConferStats(Parameters<String, ?> params) {
		sqlSession.insert("WdmsLinkManagerImpl.INSERT_WDMS_CONFER_STATS", params);
	}
	
	@Transactional
	private void insertCrmCustomerHeaders(Parameters<String, ?> params) {
		sqlSession.insert("WdmsLinkManagerImpl.INSERT_CRM_CUSTOMER", params);
	}
	
	@Transactional
	private void insertCrmCustomerAddr(Parameters<String, ?> params) {
		sqlSession.insert("WdmsLinkManagerImpl.INSERT_CRM_CUSTOMER_ADDR", params);
	}
	
	@Transactional
	private void insertCrmCustomerPhone(Parameters<String, ?> params) {
		sqlSession.insert("WdmsLinkManagerImpl.INSERT_CRM_CUSTOMER_PHONE", params);
	}
	
	@Transactional
	private void insertCrmCustomerCompany(Parameters<String, ?> params) {
		sqlSession.insert("WdmsLinkManagerImpl.INSERT_CRM_CUSTOMER_COMPANY", params);
	}
	
	@Transactional
	private void insertCrmCustomerAddrEtc(Parameters<String, ?> params) {
		sqlSession.insert("WdmsLinkManagerImpl.INSERT_CRM_CUSTOMER_ADDR_ETC", params);
	}
	
	@Transactional
	private void insertCrmCustomerPhoneEtc(Parameters<String, ?> params) {
		sqlSession.insert("WdmsLinkManagerImpl.INSERT_CRM_CUSTOMER_PHONE_ETC", params);
	}
	
	@Transactional
	private void insertCrmCustomerCompanyEtc(Parameters<String, ?> params) {
		sqlSession.insert("WdmsLinkManagerImpl.INSERT_CRM_CUSTOMER_COMPANY_ETC", params);
	}
	
	@Transactional
	private void insertCrmContract(Parameters<String, ?> params) {
		sqlSession.insert("WdmsLinkManagerImpl.INSERT_CRM_CONTRACT", params);
	}
	
	@Transactional
	private void insertCrmVacs(Parameters<String, ?> params) {
		sqlSession.insert("WdmsLinkManagerImpl.INSERT_CRM_VACS", params);
	}
	
	@Transactional
	private void updateCrmCustomerHeaders(Parameters<String, ?> params) {
		sqlSession.insert("WdmsLinkManagerImpl.UPDATE_CRM_CUSTOMER", params);
	}
	
	@Transactional
	private void updateCrmCustomerAddr(Parameters<String, ?> params) {
		sqlSession.insert("WdmsLinkManagerImpl.UPDATE_CRM_CUSTOMER_ADDR", params);
	}
	
	@Transactional
	private void updateCrmCustomerPhone(Parameters<String, ?> params) {
		sqlSession.insert("WdmsLinkManagerImpl.UPDATE_CRM_CUSTOMER_PHONE", params);
	}
	
	@Transactional
	private void updateCrmCustomerCompany(Parameters<String, ?> params) {
		sqlSession.insert("WdmsLinkManagerImpl.UPDATE_CRM_CUSTOMER_COMPANY", params);
	}
	
	@Transactional
	private void updateCrmConferStats(Parameters<String, ?> params) {
		sqlSession.insert("WdmsLinkManagerImpl.UPDATE_CRM_CONFER_STATS", params);
	}
	
	@Transactional
	private List<?> getChkCustomerCompany(Parameters<String, ?> params) {
		List<?> rtnList = sqlSession.selectList("WdmsLinkManagerImpl.GET_CHK_CUSTOMER_COMPANY", params);
		return rtnList;
	}
	
	@Transactional
	private void insertCrmCustomer(List<?> datas) {
		datas.forEach(o -> {
			Parameters<String, String> srchParams = new Parameters<String, String>();
			Parameters<String, String> custParams = new Parameters<String, String>();
			Parameters<String, String> custPhoneParams = new Parameters<String, String>();
			Parameters<String, String> custAddrParams = new Parameters<String, String>();
			Parameters<String, String> custCompanyParams = new Parameters<String, String>();

			Map<?,?> m = (Map<?,?>)o;
			List<?> jCustAddrList = (List<?>) m.get("CUST_ADDR");
			List<?> jCustAgreeList = (List<?>) m.get("CUST_AGRE");
			Map<?,?> jCust = (Map<?,?>)m.get("CUST");
			Map<?,?> jSc = (Map<?,?>)m.get("SC");
			Map<?,?> jCorp = (Map<?,?>)m.get("CORP");
	    
		    String scBrand = (String) jSc.get("EMPL_BRAND_CD");
		    String scBrandDb = Configuration.getInstance().getString("wdms.brand."+scBrand); //딜러코드
			if ( StringUtils.isBlank(scBrandDb) ) {
		    	throw new RuntimeException("This EMPL_BRAND_CD '" + scBrand + "' is not used in this system.");
			}
		    srchParams.put("dbName", scBrandDb);
	    
		    String salesUserSeq = null;
		    // CRM DB 업데이트
		    //WDMS_SC의 SC_ID로 CRM의 SALES_USER_SEQ 가져오기
		    srchParams.put("wdmsUserId", (String)jSc.get("SC_ID"));
		    List<?> userList = searchCrmUserList(srchParams);
		    if(userList != null) {
		    	if(userList.size() > 0) {
		    		Map<?,?> userMap = (Map<?,?>) userList.get(0);
		    		salesUserSeq = (String) userMap.get("SALES_USER_SEQ");
		    	}
		    } 
		    log.debug(""+"insert salesUserSeq = "+salesUserSeq);
		    if ( salesUserSeq == null ) {
		    	throw new RuntimeException("사용자가 없습니다. (SC_ID : " + jSc.get("SC_ID") + ")");
		    }
	    
		    //CustomerHeaders
		    //SC의 브랜드에 따른 DB 셋팅
		    custParams.put("dbName", scBrandDb);
		    custPhoneParams.put("dbName", scBrandDb);
			custAddrParams.put("dbName", scBrandDb);
			custCompanyParams.put("dbName", scBrandDb);
		
		    if(jCust != null) {
		    	
		    	log.debug(""+"Start Json Cust");
		    	
		    	custPhoneParams.put("wdmsCustNo", (String) jCust.get("CUST_NO"));
		    	custAddrParams.put("wdmsCustNo", (String) jCust.get("CUST_NO"));
		    	custCompanyParams.put("wdmsCustNo", (String) jCust.get("CUST_NO"));
		    	
		    	custParams.put("wdmsCustNo", (String) jCust.get("CUST_NO"));
		    	custParams.put("wdmsDlrCd", (String) jCust.get("DLR_CD"));
		    	custParams.put("wdmsCustTp", (String) jCust.get("CUST_TP"));
		    	custParams.put("wdmsCustDstiTp", (String) jCust.get("CUST_DSTIN_TP"));
		    	custParams.put("wdmsCustIfwTp", (String) jCust.get("CUST_IFW_TP"));
		    	custParams.put("wdmsBrchCd", (String) jCust.get("BRCH_CD"));
		    	
		    	custParams.put("salesUserSeq", salesUserSeq);
		    	custParams.put("customerName", (String) jCust.get("CUST_NM"));
		    	
		    	custParams.put("customerGubunGroup", "CG0001");
		    	custParams.put("customerFirstGroup", "CG0002");
		    	custParams.put("customerProspectGroup", "CG0003");
	    	
		    	/*
		    	고객구분
				  - 신규 고객의 경우, ‘가망고객’으로 저장
				  - 출고 고객의 경우, ‘출고고객'으로 유지
				최초인지경로
				  - 신규 고객의 경우, ‘기타’로 저장
				  - 영업사원 변경 가능
		    	*/
		    	
		    	if("U".equals(scBrand)) {//중고차 기준
		    		custParams.put("customerGubunSeq", "CS0001");
		    	}else { //신차 기준
		    		custParams.put("customerGubunSeq", "CS0004");
		    	}
	    		
		    	custParams.put("customerFirstSeq", "CS1000");
		    	custParams.put("customerProspectSeq", "CS0001");
		    	log.debug(""+"Start Json Cust1");
		    	custParams.put("email", (String) jCust.get("EMAIL"));
		    	String emailRcvYn = (String) jCust.get("EMAIL_RCV_YN"); 
		    	if(emailRcvYn.equals("Y")) {
		    		custParams.put("emailRcvYn", null);
		    	}else {
		    		custParams.put("emailRcvYn", "1");
		    	}
		    	log.debug(""+"Start Json Cust2");
		    	/*
		    	 * CRM
		    	<option value="1">직장</option>
				<option value="2">자택</option>
				<option value="3">수신거부</option>
				
				WDMS
				[H] 자택
				[O] 회사
				[E] 직접입력
				*/
				String pPostRcvTp = (String) jCust.get("POST_RCV_TP");
				String postRcvTp = null;
				
				if(pPostRcvTp!=null) {
					if(pPostRcvTp.equals("O")) {
						postRcvTp = "1";
					}else if(pPostRcvTp.equals("H")) {
						postRcvTp = "2";
					}else {
						postRcvTp = "3";
					}
				}
				log.debug(""+"Start Json Cust3");
		    	if( CollectionUtils.isNotEmpty(jCustAgreeList) ) {
		    		jCustAgreeList.forEach(a -> {
		    			Map<?,?> jCustAgree = (Map<?,?>)a;
			    		custParams.put("esstAgreEYn", (String) jCustAgree.get("ESST_AGREE_YN"));
			    		custParams.put("makAgreYn", (String) jCustAgree.get("MAK_AGRE_YN"));
			    		custParams.put("thirdAgreCnclDt", (String) jCustAgree.get("THIRD_AGRE_CNCL_DT"));
					});
				}
			
				log.debug(""+"Start Json Cust4");
		    	custParams.put("postRcvTp", postRcvTp);
		    	if(jCorp != null) {
		    		if(jCorp.size() > 0) {
				    	custParams.put("bpCorpCd", (String) jCorp.get("BP_CORP_CD"));
				    	custParams.put("bpCorpNm", (String) jCorp.get("BP_CORP_NM"));//고객법인명
				    	custParams.put("corpRemark", (String) jCorp.get("REMARK"));
		    		}
		    	}
		    	custParams.put("custInfoRemark", (String) jCust.get("CUST_INFO_REMARK"));

		    	// generate customerSeq
		    	sqlSession.update("WdmsLinkManagerImpl.generateCustomerSeq", custParams);
		    	String customerSeq = custParams.getString("customerSeq");
		    	if ( customerSeq == null ) throw new RuntimeException(" === customerSeq 생성 오류! === ");
			    custPhoneParams.put("customerSeq", customerSeq);
				custAddrParams.put("customerSeq", customerSeq);
				custCompanyParams.put("customerSeq", customerSeq);
	    	
		    	log.debug(""+"insertCrmCustomerHeaders Start");
		    	log.debug(""+"custParams="+custParams);
		    	insertCrmCustomerHeaders(custParams);
		    	log.debug(""+"insertCrmCustomerHeaders End");

		    	//=== 휴대전화 Start ===
		    	String hpPhone = null;
				try {
					hpPhone = wdmsEnc.AES_Decode((String) jCust.get("HP_NO"));
				} catch (InvalidKeyException | UnsupportedEncodingException | NoSuchAlgorithmException
						| NoSuchPaddingException | InvalidAlgorithmParameterException | IllegalBlockSizeException
						| BadPaddingException e) {
					e.printStackTrace();
				}
		    	
		    	String hpRcvYn = (String) jCust.get("HP_RCV_YN");
		    	String hpPhone1= null, hpPhone2= null, hpPhone3 = null;
		    	if(hpPhone!=null && hpPhone !="") {
		    		if(hpPhone.length() == 11) {
		    			hpPhone1 = hpPhone.substring(0, 3);
		    			hpPhone2 = hpPhone.substring(3, 7);
		    			hpPhone3 = hpPhone.substring(7);
			    	}else {
			    		hpPhone1 = hpPhone.substring(0, 3);
		    			hpPhone2 = hpPhone.substring(3, 6);
		    			hpPhone3 = hpPhone.substring(6);
			    	}
		    		
		    		if(hpRcvYn.equals("N")) {
		    			custPhoneParams.put("hpRcvYn", hpRcvYn);
		    		}else {
		    			custPhoneParams.put("hpRcvYn", null);
		    		}
		    		
		    		custPhoneParams.put("phoneSeq", "1");
		    		custPhoneParams.put("hpPhone1", hpPhone1);
		    		custPhoneParams.put("hpPhone2", hpPhone2);
		    		custPhoneParams.put("hpPhone3", hpPhone3);
		    		
		    		log.debug(""+"insertCrmCustomerPhone Start");
		    		insertCrmCustomerPhone(custPhoneParams);
			    	log.debug(""+"insertCrmCustomerPhone End");
		    	}
		    	//=== 휴대전화 END ===
		    	//=== 주소 Start ===
		    	// WDMS CUSTO_ADDR에서 [H] 코드만 사용 자택주소에 표시
		    	// 회사주소의 경우 CORP의 Addr컬럼에서 표시
		    	if( CollectionUtils.isNotEmpty(jCustAddrList) ) {
		    		jCustAddrList.forEach(a -> {
		    			//CUST_ADDR = 자택주소
		    			Map<?,?> jCustAddr = (Map<?,?>)a;
			    		custAddrParams.put("custNo", (String) jCustAddr.get("CUST_NO"));
			    		custAddrParams.put("addressGubun", "2");
			    		custAddrParams.put("zipNo", (String) jCustAddr.get("ZIP_NO"));
			    		custAddrParams.put("addr", (String) jCustAddr.get("ADDR"));
			    		custAddrParams.put("addrDtl", (String) jCustAddr.get("DETL_ADDR"));
			    		
			    		log.debug(""+"insertCrmCustomerAddr2 Start");
			    		insertCrmCustomerAddr(custAddrParams);
				    	log.debug(""+"insertCrmCustomerAddr2 End");
		    		});
		    	}
	    	
		    	if(jCorp != null) {
		    		if(jCorp.size() > 0) {
		    			//Corp = 직장주소
		    			custAddrParams.put("addressGubun", "1");
		    			custAddrParams.put("custNo", (String) jCorp.get("CUST_NO"));
			    		custAddrParams.put("zipNo", (String) jCorp.get("ZIP_NO"));
			    		custAddrParams.put("addr", (String) jCorp.get("ADDR"));
			    		custAddrParams.put("addrDtl", (String) jCorp.get("DETL_ADDR"));
			    		
			    		log.debug(""+"insertCrmCustomerAddr1 Start");
			    		insertCrmCustomerAddr(custAddrParams);
				    	log.debug(""+"insertCrmCustomerAddr1 End");
				    	
				    	//=== 법인 정보 Start ===
				    	custCompanyParams.put("bpCorpCd", (String) jCorp.get("BP_CORP_CD"));
				    	custCompanyParams.put("bpCorpNm", (String) jCorp.get("BP_CORP_NM"));//고객법인명
				    	custCompanyParams.put("reprsntNm", (String) jCorp.get("REPRSNT_NM"));
				    	
				    	//사업자 번호 123-12-12345
				    	String pBsnmRegNo = (String) jCorp.get("BSNM_REG_NO");
				    	String bsnmRegNo = null;
				    	if(pBsnmRegNo != null ) {
				    		if(pBsnmRegNo.length()>0) {
				    			bsnmRegNo = pBsnmRegNo.substring(0, 3)+"-"+pBsnmRegNo.substring(3, 5)+"-"+pBsnmRegNo.substring(5);
				    		}
				    	}
				    	custCompanyParams.put("bsnmRegNo", bsnmRegNo);
				    	
				    	//법인등록번호 123456-1234567
				    	String pCorpRegNo = (String) jCorp.get("CORP_REG_NO");
				    	String corpRegNo1 = null;
				    	String corpRegNo2 = null;
				    	if(pCorpRegNo!=null) {
				    		if(pCorpRegNo.length()>0) {
				    			corpRegNo1 = pCorpRegNo.substring(0, 6);
				    			corpRegNo2 = pCorpRegNo.substring(6);
				    		}
				    	}
				    	custCompanyParams.put("corpRegNo1", corpRegNo1);
				    	custCompanyParams.put("corpRegNo2", corpRegNo2);
				    	
				    	custCompanyParams.put("chrgrNm", (String) jCorp.get("CHRGR_NM"));
				    	custCompanyParams.put("relationSeq", "1");
				    	custCompanyParams.put("chrgrTelNo", hpFormat((String) jCorp.get("CHRGR_TEL_NO")));
				    	
				    	log.debug(""+"insertCrmCustomerCompany Start");
				    	insertCrmCustomerCompany(custCompanyParams);
				    	log.debug(""+"insertCrmCustomerCompany End");
				    	//=== 법인 정보 END ===
				    	
		    		}
		    	}
		    	//=== 주소 END ==
		    }
		});
	}
	
	@Transactional
	private void updateCrmCustomer(List<?> datas) {
		datas.forEach(o -> {
			Parameters<String, String> srchParams = new Parameters<String, String>();
			Parameters<String, String> custParams = new Parameters<String, String>();
			Parameters<String, String> custPhoneParams = new Parameters<String, String>();
			Parameters<String, String> custAddrParams = new Parameters<String, String>();
			Parameters<String, String> custCompanyParams = new Parameters<String, String>();
			
			Parameters<String, String> crmDbParams = new Parameters<String, String>();

			Map<?,?> m = (Map<?,?>)o;
			List<?> jCustAddrList = (List<?>) m.get("CUST_ADDR");
			List<?> jCustAgreeList = (List<?>) m.get("CUST_AGRE");
			Map<?,?> jCust = (Map<?,?>)m.get("CUST");
			Map<?,?> jSc = (Map<?,?>)m.get("SC");
			Map<?,?> jCorp = (Map<?,?>)m.get("CORP");
		    
		    String scBrand = (String) jSc.get("EMPL_BRAND_CD");
		    String scBrandDb = Configuration.getInstance().getString("wdms.brand."+scBrand); //딜러코드
			if ( StringUtils.isBlank(scBrandDb) ) {
		    	throw new RuntimeException("This EMPL_BRAND_CD '" + scBrand + "' is not used in this system.");
			}
		    srchParams.put("dbName", scBrandDb);
		    
		    String salesUserSeq = null;
		    
		    srchParams.put("wdmsUserId", (String)jSc.get("SC_ID"));
		    List<?> userList = searchCrmUserList(srchParams);
		    if( CollectionUtils.isNotEmpty(userList) ) {
		    		Map<?,?> userMap = (Map<?,?>) userList.get(0);
		    		salesUserSeq = (String) userMap.get("SALES_USER_SEQ");
		    } else {
		    	// SalesUsers 없으므로 오류
		    	throw new RuntimeException("사용자가 없습니다. (SC_ID : " + jSc.get("SC_ID") + ")");
		    }
		    
		    String custNo = (String) jCust.get("CUST_NO");

		    srchParams.clear();
		    srchParams.put("dbName", scBrandDb);
		    srchParams.put("wdmsCustNo", custNo);
		    srchParams.put("salesUserSeq", salesUserSeq);
	    
		    log.debug(""+"searchCrmCustomerList Start");
		    List<?> custList = searchCrmCustomerList(srchParams);
		    log.debug(""+"searchCrmCustomerList End");
		    if( CollectionUtils.isEmpty(custList) ) {
		    	// 수정할 고객 없으므로 오류
		    	throw new RuntimeException("No Customer.");
		    } else {
	    		custPhoneParams.put("wdmsCustNo", custNo);
	    		custPhoneParams.put("dbName", scBrandDb);
		    	custAddrParams.put("wdmsCustNo", custNo);
		    	custAddrParams.put("dbName", scBrandDb);
		    	custCompanyParams.put("wdmsCustNo", custNo);
		    	custCompanyParams.put("dbName", scBrandDb);
		    	custParams.put("wdmsCustNo", custNo);
		    	custParams.put("wdmsCustTp", (String) jCust.get("CUST_TP"));
		    	custParams.put("wdmsCustDstiTp", (String) jCust.get("CUST_DSTIN_TP"));
		    	custParams.put("wdmsCustIfwTp", (String) jCust.get("CUST_IFW_TP"));
		    	custParams.put("dbName", scBrandDb);
		    	
		    	crmDbParams.put("dbName", scBrandDb);
		    		
	    		Map<?,?> custMap = (Map<?,?>) custList.get(0);
	    		final String customerSeq = (String) custMap.get("CUSTOMER_SEQ");
	    		
	    		custParams.put("customerSeq", customerSeq);
	    		custPhoneParams.put("customerSeq", customerSeq);
	    		custAddrParams.put("customerSeq", customerSeq);
	    		custCompanyParams.put("customerSeq", customerSeq);
	    		
	    		log.debug(""+"emailRcvYn Start");
	    		custParams.put("email", (String) jCust.get("EMAIL"));
	    		String emailRcvYn = (String) jCust.get("EMAIL_RCV_YN"); 
		    	if(emailRcvYn.equals("Y")) {
		    		custParams.put("emailRcvYn", null);
		    	}else {
		    		custParams.put("emailRcvYn", "1");
		    	}

		    	custParams.put("custNm", (String) jCust.get("CUST_NM"));
		    	
		    	String pPostRcvTp = (String) jCust.get("POST_RCV_TP");
				String postRcvTp = null;
					
				if(pPostRcvTp!=null) {
					if(pPostRcvTp.equals("O")) {
						postRcvTp = "1";
					}else if(pPostRcvTp.equals("H")) {
						postRcvTp = "2";
					}else {
						postRcvTp = "3";
					}
				}
				
				log.debug(""+"jCustAgreeList Start");
		    	if( CollectionUtils.isNotEmpty(jCustAgreeList) ) {
		    		jCustAgreeList.forEach(a -> {
		    			Map<?,?> jCustAgree = (Map<?,?>)a;
			    		custParams.put("esstAgreEYn", (String) jCustAgree.get("ESST_AGREE_YN"));
			    		custParams.put("makAgreYn", (String) jCustAgree.get("MAK_AGRE_YN"));
			    		custParams.put("thirdAgreCnclDt", (String) jCustAgree.get("THIRD_AGRE_CNCL_DT"));
		    		});
				}
					
		    	custParams.put("postRcvTp", postRcvTp);
		    	if(jCorp != null) {
		    		custParams.put("corpRemark", (String) jCorp.get("REMARK"));
		    		custParams.put("bpCorpCd", (String) jCorp.get("BP_CORP_CD"));
			    	custParams.put("bpCorpNm", (String) jCorp.get("BP_CORP_NM"));//고객법인명
		    	}
		    	custParams.put("custInfoRemark", (String) jCust.get("CUST_INFO_REMARK"));
		    	log.debug(""+"updateCrmCustomerHeaders Start");
		    	updateCrmCustomerHeaders(custParams);
		    	crmDbParams.put("tableName","CUSTOMER_HEADERS");
		    	crmDbParams.put("tableParams","&p_customerSeq="+customerSeq);
		    	crmToplinkNotify(crmDbParams);
		    	crmDbParams.put("tableName","CUSTOMER_WDMS");
		    	crmDbParams.put("tableParams","&p_customerSeq="+customerSeq);
		    	crmToplinkNotify(crmDbParams);
			    	
		    	//=== 휴대전화 Start ===
		    	String hpPhone = null;
				try {
					hpPhone = wdmsEnc.AES_Decode((String) jCust.get("HP_NO"));
				} catch (InvalidKeyException | UnsupportedEncodingException | NoSuchAlgorithmException
						| NoSuchPaddingException | InvalidAlgorithmParameterException | IllegalBlockSizeException
						| BadPaddingException e) {
					e.printStackTrace();
				}
		    	String hpRcvYn = (String) jCust.get("HP_RCV_YN");
		    	String hpPhone1= null, hpPhone2= null, hpPhone3 = null;
		    	if(hpPhone!=null && hpPhone !="") {
		    		if(hpPhone.length() == 11) {
		    			hpPhone1 = hpPhone.substring(0, 3);
		    			hpPhone2 = hpPhone.substring(3, 7);
		    			hpPhone3 = hpPhone.substring(7);
			    	}else {
			    		hpPhone1 = hpPhone.substring(0, 3);
		    			hpPhone2 = hpPhone.substring(3, 6);
		    			hpPhone3 = hpPhone.substring(6);
			    	}
		    		
		    		if(hpRcvYn.equals("N")) {
		    			custPhoneParams.put("hpRcvYn", hpRcvYn);
		    		}else {
		    			custPhoneParams.put("hpRcvYn", null);
		    		}
		    		
		    		custPhoneParams.put("phoneSeq", "1");
		    		custPhoneParams.put("hpPhone1", hpPhone1);
		    		custPhoneParams.put("hpPhone2", hpPhone2);
		    		custPhoneParams.put("hpPhone3", hpPhone3);
		    		log.debug(""+"updateCrmCustomerPhone Start");
		    		updateCrmCustomerPhone(custPhoneParams);
		    		
		    		Map<?,?> crmPhoneInfo = (Map<?,?>) sqlSession.selectOne("WdmsLinkManagerImpl.CHK_CUSTOMER_PHONE", custPhoneParams);
		    		int crmphone = 0;
		    		if(crmPhoneInfo!=null) {
		    			crmphone = (Integer) crmPhoneInfo.get("CNT");
		    			if(crmphone == 0) {
		    				insertCrmCustomerPhoneEtc(custPhoneParams);
		    			}
		    		}
		    		crmDbParams.put("tableName","CUSTOMER_PHONE");
			    	crmDbParams.put("tableParams","&p_customerSeq="+customerSeq+"&p_phoneSeq=1");
			    	crmToplinkNotify(crmDbParams);
		    	}
		    	//=== 휴대전화 END ===
			    	
		    	//=== 주소 Start ===
		    	// WDMS CUSTO_ADDR에서 [H] 코드만 사용 자택주소에 표시
		    	// 회사주소의 경우 CORP의 Addr컬럼에서 표시
		    	if( CollectionUtils.isNotEmpty(jCustAddrList) ) {
		    		jCustAddrList.forEach(a -> {
		    			//CUST_ADDR = 자택주소
		    			Map<?,?> jCustAddr = (Map<?,?>)a;
		    			String addrGubun = (String) jCustAddr.get("ADDR_DSTIN_CD");
		    			if("H".equals(addrGubun)) {
		    				custAddrParams.put("custNo", (String) jCustAddr.get("CUST_NO"));
				    		custAddrParams.put("addressGubun", "2");
				    		custAddrParams.put("zipNo", (String) jCustAddr.get("ZIP_NO"));
				    		custAddrParams.put("addr", (String) jCustAddr.get("ADDR"));
				    		custAddrParams.put("addrDtl", (String) jCustAddr.get("DETL_ADDR"));
				    		log.debug(""+"updateCrmCustomerAddr2 Start");
				    		updateCrmCustomerAddr(custAddrParams);
				    		
				    		Map<?,?> crmAddrInfoH = (Map<?,?>) sqlSession.selectOne("WdmsLinkManagerImpl.CHK_CUSTOMER_ADDR", custAddrParams);
				    		int crmAddrH = 0;
				    		if(crmAddrInfoH!=null) {
				    			crmAddrH = (Integer) crmAddrInfoH.get("CNT");
				    			if(crmAddrH == 0) {
				    				insertCrmCustomerAddrEtc(custAddrParams);
				    			}
				    		}
				    		crmDbParams.put("tableName","CUSTOMER_ADDRESS");
					    	crmDbParams.put("tableParams","&p_customerSeq="+customerSeq+"&p_addressGubun=2");
					    	crmToplinkNotify(crmDbParams);
		    			}
		    		});
		    	}
			    	
		    	if(jCorp != null) {
		    		if(jCorp.size() > 0) {
		    			//Corp = 직장주소
		    			custAddrParams.clear();
		    			custAddrParams.put("dbName", scBrandDb);
		    			custAddrParams.put("customerSeq", customerSeq);
		    			custAddrParams.put("addressGubun", "1");
		    			custAddrParams.put("custNo", (String) jCorp.get("CUST_NO"));
			    		custAddrParams.put("zipNo", (String) jCorp.get("ZIP_NO"));
			    		custAddrParams.put("addr", (String) jCorp.get("ADDR"));
			    		custAddrParams.put("addrDtl", (String) jCorp.get("DETL_ADDR"));
			    		log.debug(""+"updateCrmCustomerAddr1 Start");
			    		updateCrmCustomerAddr(custAddrParams);
			    		
			    		Map<?,?> crmAddrInfoC = (Map<?,?>) sqlSession.selectOne("WdmsLinkManagerImpl.CHK_CUSTOMER_ADDR", custAddrParams);
			    		int crmAddrC = 0;
			    		if(crmAddrInfoC!=null) {
			    			crmAddrC = (Integer) crmAddrInfoC.get("CNT");
			    			if(crmAddrC == 0) {
			    				insertCrmCustomerAddrEtc(custAddrParams);
			    			}
			    		}
			    		
			    		crmDbParams.put("tableName","CUSTOMER_ADDRESS");
				    	crmDbParams.put("tableParams","&p_customerSeq="+customerSeq+"&p_addressGubun=1");
				    	crmToplinkNotify(crmDbParams);
				    	
			    		//=== 법인 정보 Start ===
				    	custCompanyParams.put("bpCorpCd", (String) jCorp.get("BP_CORP_CD"));
				    	custCompanyParams.put("bpCorpNm", (String) jCorp.get("BP_CORP_NM"));//고객법인명
				    	custCompanyParams.put("reprsntNm", (String) jCorp.get("REPRSNT_NM"));
				    	
				    	//사업자 번호 123-12-12345
				    	String pBsnmRegNo = (String) jCorp.get("BSNM_REG_NO");
				    	String bsnmRegNo = null;
				    	if(pBsnmRegNo != null ) {
				    		if(pBsnmRegNo.length()>0) {
				    			bsnmRegNo = pBsnmRegNo.substring(0, 3)+"-"+pBsnmRegNo.substring(3, 5)+"-"+pBsnmRegNo.substring(5);
				    		}
				    	}
				    	custCompanyParams.put("bsnmRegNo", bsnmRegNo);
				    	
				    	//법인등록번호 123456-1234567
				    	String pCorpRegNo = (String) jCorp.get("CORP_REG_NO");
				    	String corpRegNo1 = null;
				    	String corpRegNo2 = null;
				    	if(pCorpRegNo!=null) {
				    		if(pCorpRegNo.length()>0) {
				    			corpRegNo1 = pCorpRegNo.substring(0, 6);
				    			corpRegNo2 = pCorpRegNo.substring(6);
				    		}
				    	}
				    	custCompanyParams.put("corpRegNo1", corpRegNo1);
				    	custCompanyParams.put("corpRegNo2", corpRegNo2);
				    	
				    	custCompanyParams.put("chrgrNm", (String) jCorp.get("CHRGR_NM"));
				    	custCompanyParams.put("relationSeq", "1");
				    	
				    	custCompanyParams.put("chrgrTelNo", hpFormat((String) jCorp.get("CHRGR_TEL_NO")));
				    	log.debug(""+"updateCrmCustomerCompany Start");
				    	updateCrmCustomerCompany(custCompanyParams);
				    	Map<?,?> crmCompanyInfo = (Map<?,?>) sqlSession.selectOne("WdmsLinkManagerImpl.CHK_CUSTOMER_COMPANY_CNT", custCompanyParams);
			    		int crmCompany = 0;
			    		if(crmCompanyInfo!=null) {
			    			crmCompany = (Integer) crmCompanyInfo.get("CNT");
			    			if(crmCompany == 0) {
			    				insertCrmCustomerCompany(custCompanyParams);
			    			}
			    		}
				    	
				    	crmDbParams.put("tableName","CUSTOMER_COMPANY");
				    	crmDbParams.put("tableParams","&p_customerSeq="+customerSeq);
				    	crmToplinkNotify(crmDbParams);
				    	crmDbParams.put("tableName","CUSTOMER_PEOPLE");
				    	crmDbParams.put("tableParams","&p_customerSeq="+customerSeq+"&p_relationSeq=1");
				    	crmToplinkNotify(crmDbParams);
				    	//=== 법인 정보 END ===
				    	
				    	//=== 직장전화 Start ===
				    	String comptel = (String) jCorp.get("TEL_NO");
				    	String comptel1= null, comptel2= null, comptel3 = null;
				    	if(comptel!=null && comptel !="") {
				    		if(hpPhone.length() == 11) {
				    			comptel1 = comptel.substring(0, 3);
				    			comptel2 = comptel.substring(3, 7);
				    			comptel3 = comptel.substring(7);
					    	}else {
					    		comptel1 = comptel.substring(0, 3);
					    		comptel2 = comptel.substring(3, 6);
					    		comptel3 = comptel.substring(6);
					    	}
				    		
				    		custPhoneParams.clear();
				    		custPhoneParams.put("dbName", scBrandDb);
				    		custPhoneParams.put("customerSeq", customerSeq);
				    		custPhoneParams.put("phoneSeq", "2");
				    		custPhoneParams.put("hpPhone1", comptel1);
				    		custPhoneParams.put("hpPhone2", comptel2);
				    		custPhoneParams.put("hpPhone3", comptel3);
				    		custPhoneParams.put("hpRcvYn", null);
				    		log.debug(""+"updateCrmCustomerPhone Corp Start");
				    		updateCrmCustomerPhone(custPhoneParams);
				    		
				    		Map<?,?> crmPhoneInfoC = (Map<?,?>) sqlSession.selectOne("WdmsLinkManagerImpl.CHK_CUSTOMER_PHONE", custPhoneParams);
				    		int crmphoneC = 0;
				    		if(crmPhoneInfoC!=null) {
				    			crmphoneC = (Integer) crmPhoneInfoC.get("CNT");
				    			if(crmphoneC == 0) {
				    				insertCrmCustomerPhoneEtc(custPhoneParams);
				    			}
				    		}
				    		
				    		crmDbParams.put("tableName","CUSTOMER_PHONE");
					    	crmDbParams.put("tableParams","&p_customerSeq="+customerSeq+"&p_phoneSeq=2");
					    	crmToplinkNotify(crmDbParams);
				    	}
				    	//=== 직장전화 END ===
		    		}
		    	}
		    	//=== 주소 END ==
		    }
		});
	}
	
	/**
		 * AutoCRM DB에 Wholesale 정보 업데이트
		 * 
		 * @param result
		 */
		@Transactional
		private void _wdmsWholesaleSave(Map<?,?> result){
	
			Map<?,?> stock = (Map<?,?>) sqlSession.selectOne("stock.getOldWholesale", result);
			if ( stock == null ) return;
	
			String stockSeq = (String) stock.get("STOCK_SEQ");
			if ( StringUtils.isNotBlank(stockSeq) ) {
	
				sqlSession.update("stock.updateBuyingDate", result);
				sqlSession.update("stock.updateWholesalePriceStock", result);
				sqlSession.update("stock.updateWholesalePriceConfer", result);
				sqlSession.update("stock.applyWholesale", result);
		
				// update eclipselink cache 
				Parameters<String, String> p = new Parameters<>();
				p.put("dbName", (String) result.get("dbName"));
		    	p.put("tableParams","&p_stockSeq=" + stockSeq);
		    	p.put("tableName","STOCK_EXT");
				crmToplinkNotify(p);	// refresh 시 STOCK 도 같이 refresh 됨
				
				String saleConferSeq = (String) stock.get("SALE_CONFER_SEQ");
				if ( StringUtils.isNotBlank(saleConferSeq) ) {
			    	p.put("tableParams","&p_saleConferSeq=" + saleConferSeq);
			    	p.put("tableName","SALE_CONFER");
					crmToplinkNotify(p);
				}
			}
	//		else throw new GenericRuntimeException(result.get("VIN_NO") + " does not exist in Stock DB.");
		}

	private void crmToplinkNotify(Parameters<String, String> params) {
		/*
		WDMS 고객사용 테이블
		CUSTOMER_HEADERS
		CUSTOMER_WDMS
		CUSTOMER_PHONE
		CUSTOMER_ADDRESS
		CUSTOMER_COMPANY
		CUSTOMER_PEOPLE 
		*/
		sqlSession.insert("WdmsLinkManagerImpl.INSERT_CRM_TOPLINK_NOTIFY", params);
	}
	
	
	
	private String hpFormat(String fullNumber) {
    	String rtnNum = null;
    	String hpChk;
    	
    	if(fullNumber != null) {
    		if(fullNumber.length() > 3) {
    			fullNumber = fullNumber.replace("-", "");
        		hpChk = fullNumber.substring(0, 3);
        		if(hpChk.contains("010") || hpChk.contains("011")) {
        			if(fullNumber.length() == 11) {
        				rtnNum = fullNumber.substring(0, 3)+"-"+fullNumber.substring(3, 7)+"-"+fullNumber.substring(7);
    		    	} else {
    		    		rtnNum = fullNumber.substring(0, 3)+"-"+fullNumber.substring(3, 6)+"-"+fullNumber.substring(6);
    		    	}
        		} else {
        			if(hpChk.contains("02")) {
        				if(fullNumber.length() >= 10) {
        					rtnNum = fullNumber.substring(0, 2)+"-"+fullNumber.substring(2, 6)+"-"+fullNumber.substring(6);
        				} else {
        					rtnNum = fullNumber.substring(0, 2)+"-"+fullNumber.substring(2, 5)+"-"+fullNumber.substring(5);
        				}
        			} else{
        				if(fullNumber.length() == 10) {
        					rtnNum = fullNumber.substring(0, 3)+"-"+fullNumber.substring(3, 5)+"-"+fullNumber.substring(5);
        				} else {
        					rtnNum = fullNumber.substring(0, 3)+"-"+fullNumber.substring(3, 6)+"-"+fullNumber.substring(6);
        				}
        			}
        		}
    		}
    	}
    	
    	return rtnNum;
	}

	@SuppressWarnings("unchecked")
	private Map<?, ?> format(Map<String, ?> map, List<?> results){
		Map<String, Object> header = (Map<String, Object>) map.get("HEADER");
		header.put("IF_DATE", DateFormatUtils.format(Calendar.getInstance(), "yyyy-MM-dd HH:mm:ss"));
		header.put("IF_SYSTEM", "AutoCRM");

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("RESULT", results);

		ArrayList<Map<String, Object>> dataArr = new ArrayList<>();
		dataArr.add(data);

		Map<String, Object> ret = new LinkedHashMap<>();
		ret.put("HEADER", header);
		ret.put("DATA", dataArr);

		return ret;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Map<?,?> wdmsPromotionInfoSave(Map<String,?> map){

		String today = Utility.calendarToString(Utility.getCalendar(),"yyyy-MM-dd HH:mm:ss.SSS");
		
		List<Map<String, ?>> results = new ArrayList<>();
		List<Map<String, ?>> datas = (List<Map<String, ?>>)map.get("DATA");
		
		for (Map<String, ?> data : datas) {
			List<?> promotionInfos = (List<?>)data.get("PRMTION_INFO");
			for (Object promotionInfo : promotionInfos) {
				Map<String,Object> result = (Map<String,Object>)promotionInfo;

				// 브랜드별 DB 결정
				String dbName = config.getProperty("wdms.brand." + result.get("BRAND_CD"));
				
				String saleDetlTp = (String) result.get("SALE_DETL_TP");
				String additionalLossTypeSeq = "";
				
				String dcTp = (String) result.get("DC_TP");
				String chkDcVal = (String) result.get("DC_VAL");
				
				//01,03 패밀리 02,04 로얄티 05,06,07,09,10 프로모션(법인)
				if("01".equals(saleDetlTp) || "03".equals(saleDetlTp)) {
					additionalLossTypeSeq = config.getProperty("promotion.family.codeseq");
				}else if("02".equals(saleDetlTp) || "04".equals(saleDetlTp)){
					additionalLossTypeSeq = config.getProperty("promotion.loyalty.codeseq");
				}else if("05".equals(saleDetlTp) || "06".equals(saleDetlTp) || "07".equals(saleDetlTp) || "09".equals(saleDetlTp) || "10".equals(saleDetlTp) || "14".equals(saleDetlTp)) {
					additionalLossTypeSeq = config.getProperty("promotion.inc.codeseq");
				}
				
				if ( StringUtils.isBlank(dbName) ) {
					result.put("IF_RST_CD", "99");
					result.put("IF_RST_MSG", "FAIL");
					result.put("CRM_MSG", "BRAND_CD does not exist.");
				} else if(StringUtils.isBlank(additionalLossTypeSeq)){
					result.put("IF_RST_CD", "99");
					result.put("IF_RST_MSG", "FAIL");
					result.put("CRM_MSG", "SALE_DETL_TP does not exist.");
				} else if(StringUtils.isBlank(dcTp) || StringUtils.isBlank(chkDcVal)){
					result.put("IF_RST_CD", "99");
					result.put("IF_RST_MSG", "FAIL");
					result.put("CRM_MSG", "Please Check DC_TP,DC_VAL");
				}else {
					
					sqlSession.insert("WdmsLinkManagerImpl.INSERT_PROMOTION_INFO", promotionInfo); //API에 받은 값 저장
					
					Parameters<String, Object> searchParams = new Parameters<String, Object>();
					Parameters<String, Object> updateParams = new Parameters<String, Object>();
					
					String statCd = (String) result.get("STAT_CD");
					//String s_contractSeq = (String) result.get("VIRTL_ACCT_MNG_NO");
					int chkSeq = NumberUtils.toInt(result.get("seq").toString());
					
					//03,04,05의 경우에만 가격 update가 진행됨
					if("03".equals(statCd) || "04".equals(statCd) || "05".equals(statCd)) {
						
						searchParams.put("chkSeq" , chkSeq);
						searchParams.put("dbName", dbName);
						searchParams.put("additionalLossTypeSeq",additionalLossTypeSeq);
						
						//가격등 정보 가져옴..
						Map<?,?> conferInfo = (Map<?,?>) sqlSession.selectOne("WdmsLinkManagerImpl.GET_CRM_SALE_CONFER_INFO", searchParams);
						if(conferInfo == null) {
							Map<String, Object> rslt = new LinkedHashMap<>();
							rslt.put("IF_RST_CD", "99");
							rslt.put("IF_RST_MSG", "FAIL");
							rslt.put("CRM_MSG", "SaleConfer does not exist.");
							return rslt;
						}
						
						String dlrCd = (String) result.get("DLR_CD");
						
						int additionalLossSeq = NumberUtils.toInt(conferInfo.get("ADDITIONAL_LOSS_SEQ").toString());
						String saleConferSeq = (String) conferInfo.get("SALE_CONFER_SEQ");
						long modelPrice = NumberUtils.toLong(conferInfo.get("MODEL_PRICE").toString());
						
						//deutsch 딜러코드 001652
						if("001652".equals(dlrCd)) {
							
							int beforePromotionAmount = 0;
							int cashItemOptionAmount = 0;	//비용계 총합
							long discountPrice = 0;
							long vehiclePrice= 0;	//판매가격
							long supplyPrice= 0;	//공급가격
							long vat = 0;	//VAT
							long contractAmount= 0;	//계약금
							long deliveryAmount= 0;	//인도금
							long consigmentPrice = 0;
							long beforeLossAmount = 0;
							long prevailingPrice = 0;
							long lossAmount = 0;
							
							beforePromotionAmount = NumberUtils.toInt(conferInfo.get("BEFORE_PROMOTION_AMOUNT").toString());
							cashItemOptionAmount = NumberUtils.toInt(conferInfo.get("CASH_ITEM_OPTION_AMOUNT").toString());
							vehiclePrice = NumberUtils.toLong(conferInfo.get("VEHICLE_PRICE").toString());
							supplyPrice = NumberUtils.toLong(conferInfo.get("SUPPLY_PRICE").toString());
							vat = NumberUtils.toLong(conferInfo.get("VAT").toString());
							contractAmount = NumberUtils.toLong(conferInfo.get("CONTRACT_AMOUNT").toString());
							consigmentPrice = NumberUtils.toLong(conferInfo.get("CONSIGMENT_PRICE").toString());
							beforeLossAmount = NumberUtils.toLong(conferInfo.get("BEFORE_LOSS_AMOUNT").toString());
							prevailingPrice = NumberUtils.toLong(conferInfo.get("PREVAILING_PRICE").toString());
							
							//P:할인율,A:금액 - 할인율로만 보낼 예정이라고함
							if("P".equals(dcTp)) {
								double dcVal = NumberUtils.toDouble(result.get("DC_VAL").toString());
								lossAmount = (long) Math.floor(((prevailingPrice * dcVal) / 100));
							}else {
								int dcVal = NumberUtils.toInt(result.get("DC_VAL").toString());
								lossAmount = dcVal;
							}
							
							//04(반려) ,05(삭제)의 경우 0원처리
							if("04".equals(statCd) || "05".equals(statCd)) {
								lossAmount = 0;
							}
						
							//SALE_CONFER UPDATE
							discountPrice = (beforePromotionAmount - beforeLossAmount + lossAmount) - cashItemOptionAmount;
							vehiclePrice =  modelPrice - discountPrice + consigmentPrice; //판매가격=차량가격-계산서DC+탁송료
							supplyPrice = Math.round(vehiclePrice / 1.1);
							vat = vehiclePrice - supplyPrice;
							//인도금 UPDATE
							deliveryAmount = vehiclePrice -  contractAmount;	//인도금 = 판매가격 - 계약금
							
							updateParams.put("discountPrice",discountPrice);
							updateParams.put("supplyPrice",supplyPrice);
							updateParams.put("vat",vat);
							updateParams.put("vehiclePrice",vehiclePrice);
							updateParams.put("lossAmount",lossAmount);
							updateParams.put("deliveryAmount",deliveryAmount);
							updateParams.put("updateChk","Y"); // deutsch만 품의 및 인도금 update
							
							
						}else if("001633".equals(dlrCd) || "001639".equals(dlrCd) ) {//bavarian 딜러코드 001633 , SM 딜러코드 001639
							long baseLossAmount = 0;
							double lossRate = 0;
							
							//P:할인율,A:금액 - 할인율로만 보낼 예정이라고함
							if("P".equals(dcTp)) {
								lossRate = NumberUtils.toDouble(result.get("DC_VAL").toString());
								baseLossAmount = (long) Math.floor(((modelPrice * lossRate) / 100));
							}else {
								
								baseLossAmount = Long.parseLong(result.get("DC_VAL").toString().replace(",", ""));
								String lossRateStr = String.format("%.2f",(((double) baseLossAmount / modelPrice) * 100));
								lossRate = Double.parseDouble(lossRateStr);
								
							}
							
							//04(반려) ,05(삭제)의 경우 0원처리
							if("04".equals(statCd) || "05".equals(statCd)) {
								baseLossAmount = 0;
								lossRate = 0;
							}
							
							updateParams.put("lossRate", lossRate);
							updateParams.put("baseLossAmount",baseLossAmount);
						}
						
						updateParams.put("saleConferSeq",saleConferSeq);
						updateParams.put("additionalLossSeq",additionalLossSeq);
						updateParams.put("additionalLossTypeSeq",additionalLossTypeSeq);
					}
					
					String chkApprovalAuthSeq = config.getProperty("deadline.chkApprovalAuthSeq");
					updateParams.put("chkApprovalAuthSeq", chkApprovalAuthSeq);
					updateParams.put("statCd" , statCd);
					updateParams.put("chkSeq",chkSeq);
					updateParams.put("today",today);
					updateParams.put("dbName", dbName);
					
					result.put("dbName", dbName);
					
					_wdmsPromotionInfoSave(updateParams);
					
					result.remove("dbName");
					result.remove("seq");
					result.put("IF_RST_CD", "00");
					result.put("IF_RST_MSG", "SUCCESS");
				}
				
				Object[] keys = result.keySet().toArray();
				for (Object key : keys) {
					if ( !ArrayUtils.contains(new String[]{"BRAND_CD","DLR_CD","PRMT_NO","CONT_APPR_NO", "CONT_NO", "VIRTL_ACCT_MNG_NO", "IF_RST_CD", "IF_RST_MSG", "CRM_MSG"}, key)) {
						result.remove(key);
					}
				}

				results.add( result );
			}
		}
		return format(map, results);
	}
	
	
	@Transactional
	private void _wdmsPromotionInfoSave(Parameters<String, ?> params){

		Map<?,?> conferInfo = (Map<?,?>) sqlSession.selectOne("WdmsLinkManagerImpl.GET_CHK_CONFER_STATUS", params);
		if ( conferInfo == null ) return;

		String statCd = params.getString("statCd");
		String additionalLossTypeSeq = params.getString("additionalLossTypeSeq");
		String additionalLossSeq = params.getString("additionalLossSeq");
		String updateChk = params.getString("updateChk");
		
		String saleConferSeq = (String) conferInfo.get("SALE_CONFER_SEQ");
		if ( StringUtils.isNotBlank(saleConferSeq) ) {

			//UPDATE의 경우 승인,반려,취소만...
			if("03".equals(statCd) || "04".equals(statCd) || "05".equals(statCd)) {
				if("Y".equals(updateChk)) {
					sqlSession.update("WdmsLinkManagerImpl.UPDATE_CRM_SALE_CONFER", params);
					sqlSession.update("WdmsLinkManagerImpl.UPDATE_CRM_RECEIPT_INFO", params);
					sqlSession.update("WdmsLinkManagerImpl.UPDATE_CRM_ADDITIONAL_LOSS_INFO", params);
				}else {
					sqlSession.update("WdmsLinkManagerImpl.UPDATE_CRM_BASE_LOSS_AMOUNT", params);
				}
			}
			sqlSession.update("WdmsLinkManagerImpl.APPLY_PROMOTION_INFO", params);
	
			// update eclipselink cache 
			Parameters<String, String> p = new Parameters<>();
			p.put("dbName", (String) params.get("dbName"));
			
			if ( StringUtils.isNotBlank(saleConferSeq) ) {
		    	p.put("tableParams","&p_saleConferSeq=" + saleConferSeq);
		    	p.put("tableName","SALE_CONFER");
				crmToplinkNotify(p);
			}
			
			if(StringUtils.isNotBlank(additionalLossTypeSeq) && StringUtils.isNotBlank(additionalLossSeq)) {
				p.put("tableParams","&p_saleConferSeq=" + saleConferSeq + "&p_additionalLossTypeSeq=" + additionalLossTypeSeq + "&p_additionalLossSeq=" + additionalLossSeq );
		    	p.put("tableName","ADDITIONAL_LOSS_INFO");
				crmToplinkNotify(p);
			}
			
		}
	}
}
