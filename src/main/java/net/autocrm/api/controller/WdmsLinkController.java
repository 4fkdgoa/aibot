package net.autocrm.api.controller;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.autocrm.api.service.MainManager;
import net.autocrm.api.service.StockManager;
import net.autocrm.api.service.WdmsLinkManager;
import net.autocrm.api.util.WdmsEncryption;
import net.sf.json.JSONException;

@Configuration
@RestController
public class WdmsLinkController {
	@Autowired WdmsLinkManager wlMgr;
	@Autowired protected WdmsEncryption wdmsCiper;
	@Autowired StockManager stockMgr;
	@Autowired MainManager mainMgr;

    @Bean
    WdmsEncryption wdmsCiper() {
    	return new WdmsEncryption();
    }
	
	/**
	 * 고객정보
	 * 
	 * @param res
	 * @param req
	 * @param map
	 * @return
	 */
	@Operation(summary = "고객정보")
	@PostMapping("/wdms/customerInfo")
    public @ResponseBody Map<?, ?> customerInfo(HttpServletResponse res, HttpServletRequest req, @RequestBody Map<String, Object> map){
		
		/*
		DLR_CD	딜러코드	VARCHAR2(10 BYTE)		"001655"
		CUST_NO	고객번호	VARCHAR2(10 BYTE)		"3937121"
		RST_CD	Result Code	VARCHAR2(2 BYTE)	"00" or "99"	
		RST_MSG	Result Message	VARCHAR2(100 BYTE)	"SUCCESS" or "FAIL"
		*/

		return wlMgr.wdmsCustomerSave(map);
    }
	
	/**
	 * 신차 매입정보
	 * 
	 * @param map
	 * @return
	 */
	@Operation(summary = "신차 매입정보")
	@PostMapping("/wdms/wholeSaleInfo")
	public Map<?, ?> wholeSaleInfo(@RequestBody Map<String, ?> map) {
		return wlMgr.wdmsWholesaleSave(map);
	}

	/**
	 * 신차 계약정보
	 * 
	 * @param res
	 * @param req
	 * @param map
	 * @return
	 */
	@Operation(summary = "신차 계약정보")
	@PostMapping("/wdms/contractInfo")
    public @ResponseBody Map<?, ?> contractInfo(HttpServletResponse res, HttpServletRequest req, @RequestBody Map<String, Object> map){
		
		/*
		DLR_CD	딜러코드	VARCHAR2(10 BYTE)	
		BRCH_CD	대리점코드	VARCHAR2(5 BYTE)	
		CONT_NO	계약번호	VARCHAR2(20 BYTE)	DMS Next Contract System Key
		IF_RST_CD	Result Code	VARCHAR2(2 BYTE)	정의 필요
		IF_RST_MSG	Result Message	VARCHAR2(100 BYTE)	"SUCCESS" or "FAIL"
		*/

		return wlMgr.wdmsContractInfoSave(map);
    }
	
	/**
	 * 신차_중고차 프로모션
	 * 
	 * @param map
	 * @return
	 */
	@Operation(summary = "신차/중고차 프로모션")
	@PostMapping("/wdms/promotionInfo")
	public Map<?, ?> promotionInfo(@RequestBody Map<String, ?> map) {
		return wlMgr.wdmsPromotionInfoSave(map);
	}
	
	/**
	 * 중고차 계약정보
	 * 
	 * @param res
	 * @param req
	 * @param map
	 * @return
	 * @throws InvalidKeyException
	 * @throws JSONException
	 * @throws UnsupportedEncodingException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidAlgorithmParameterException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws Exception
	 */
	@Operation(summary = "중고차 계약정보")
	@PostMapping("/wdms/bpsContractInfo")
    public @ResponseBody Map<?, ?> usedContractInfo(HttpServletResponse res, HttpServletRequest req, @RequestBody Map<String, Object> map) {
		/*
		DLR_CD	딜러코드	VARCHAR2(10 BYTE)	
		BRCH_CD	대리점코드	VARCHAR2(5 BYTE)	
		CONT_NO	계약번호	VARCHAR2(20 BYTE)	DMS Next Contract System Key
		IF_RST_CD	Result Code	VARCHAR2(2 BYTE)	정의 필요
		IF_RST_MSG	Result Message	VARCHAR2(100 BYTE)	"SUCCESS" or "FAIL"
		*/

		return wlMgr.wdmsUsedContractInfoSave(map);
    }
	
	
	/**
	 * 상태정보 To Dealer
	 * 
	 * @param res
	 * @param req
	 * @param map
	 * @return
	 */
	@Operation(summary = "상태정보 To Dealer")
	@PostMapping("/wdms/salesStatus")
    public @ResponseBody Map<?, ?> salesStatus(HttpServletResponse res, HttpServletRequest req, @RequestBody Map<String, Object> map){
		return wlMgr.wdmsStatsRequest(map);
	}
}
