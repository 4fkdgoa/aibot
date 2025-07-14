package net.autocrm.api.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import net.autocrm.api.model.ClientDetails;
import net.autocrm.api.service.AutoManager;
import net.autocrm.api.service.ClientDetailManager;
import net.autocrm.api.service.StockManager;
	
@RequestMapping("/s3")
@RestController
public class S3Controller extends CommonController {

	@Autowired AutoManager autoMgr;
	@Autowired StockManager stockMgr;
	@Autowired ClientDetailManager clientMgr;

	@Operation(summary = "차량정보연동")
	@RequestMapping(value = "/auto/retrieve/{dt}", method = RequestMethod.HEAD)
	public void retrieveAuto(@PathVariable String dt) {
		autoMgr.execAllS3(dt);
	}

	@Hidden
	@RequestMapping(value = "/auto/retrieve/{table}/{dt}", method = RequestMethod.HEAD)
	public void retrieveAuto(@PathVariable String table, @PathVariable String dt) {
		autoMgr.execS3(table, dt);
	}

	@Operation(summary = "배정재고연동")
	@RequestMapping(value = "/stock/retrieve/{dt}", method = RequestMethod.HEAD)
	public void retrieveStock(@PathVariable String dt) {
		stockMgr.execS3(dt);
	}

	@Operation(summary = "S3 Info 수정")
	@Parameters({
		@Parameter(name = "ak", description = "S3 Access Key"),
		@Parameter(name = "sk", description = "S3 Secret Access Key")
	})
    @PostMapping("/upd")
    public @ResponseBody Map<String, ?> updateS3Info(@RequestParam("ak") String accessKey, @RequestParam("sk") String secretKey) {
    	ClientDetails bean = getClient();
    	bean.setS3accesskey( accessKey );
    	bean.setS3secretkey( secretKey );
    	clientMgr.save( bean );

    	HashMap<String, String> rslt = new HashMap<>();
		rslt.put( "status", "success" );
		return rslt;
    }

}
