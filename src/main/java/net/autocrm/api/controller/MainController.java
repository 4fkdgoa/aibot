package net.autocrm.api.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.Getter;
import lombok.Setter;
import net.autocrm.api.model.ClientDetails;
import net.autocrm.api.service.MainManager;
import net.autocrm.common.model.Parameters;

@Hidden
@Controller
public class MainController extends CommonController {
	
	@Autowired MainManager mainMgr;

	@GetMapping(value={"/robots.txt", "/robot.txt"})
    @ResponseBody
    public String getRobotsTxt() {
        return "User-agent: *\n" +
                "Disallow: /\n";
    }

    @PostMapping("/auth/tokens")
    public @ResponseBody Map<String, ?> createAccessToken(@RequestHeader(defaultValue = "1") int minute){
    	ClientDetails client = getClient();

    	Parameters<String, Object> claims = new Parameters<>();
    	claims.put( "sub", String.valueOf( client.getSeq() ) );
    	String tkn = jwtGen.createJWT(null, claims, 1000 * 60 * minute);

    	HashMap<String, String> rslt = new HashMap<>();
    	rslt.put("tokenType", "Bearer");
		rslt.put("accessToken", tkn);
		return rslt;
    }

    @PostMapping("/info/wdms")
    public @ResponseBody Map<String, ?> getWdmsInfo(){
    	ClientDetails client = getClient();

    	HashMap<String, String> rslt = new HashMap<>();
    	rslt.put("corpCd", client.getWdmsid());
		rslt.put("secretKey", client.getWdmssecret());
    	rslt.put("host", client.getUrl());
		return rslt;
    }

    @PostMapping("/info/log")
    public @ResponseBody Map<?, ?> listRequestLog(@RequestBody Parameters<String, String> params) {
    	return mainMgr.listRequestLog(params, params.getInt("page", 1), params.getInt("rows", 10)).getPagedInfos();
    }

    @Getter
    @Setter
    class AuthReq{
    	String secret;
    }
}
