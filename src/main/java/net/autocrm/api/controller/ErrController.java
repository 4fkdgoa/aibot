package net.autocrm.api.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.WebUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import net.autocrm.api.util.CustomHttpRequestBody;

@Slf4j
@Hidden
@Controller
public class ErrController implements ErrorController {
	ObjectMapper ob = new ObjectMapper();

	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/error")
	public @ResponseBody Map<?,?> handleError(HttpServletRequest request, Exception ex) {
		log.warn(request.getRequestURI(), ex);

		Map<String, Object> ret = new LinkedHashMap<>();
		// 에러 코드를 획득한다.
		Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
		// 에러 코드에 대한 상태 정보
		HttpStatus httpStatus = HttpStatus.valueOf(Integer.valueOf(status.toString()));
		if (status != null) {

			Map<String, String> header = null;

			CustomHttpRequestBody req = WebUtils.getNativeRequest(request, CustomHttpRequestBody.class);
			if ( req != null && req.getRawData() != null && req.getRawData().length > 0 ) {
				try {
					Map<?, ?> map = ob.readValue(req.getRawData(), Map.class);
					header = (Map<String, String>)map.get("HEADER");
				} catch (IOException e) {
					e.printStackTrace();
					header = new LinkedHashMap<>();
				}
			} else {
				header = new LinkedHashMap<>();
			}
			header.put("IF_DATE", DateFormatUtils.format(Calendar.getInstance(), "yyyy-MM-dd HH:mm:ss"));
			header.put("IF_SYSTEM", "AutoCRM");
			ret.put("HEADER", header);

			Map<String, Object> rslt = new LinkedHashMap<>();
			rslt.put("IF_RST_CD", "99");
			rslt.put("IF_RST_MSG", "FAIL");

			ArrayList<Map<String, Object>> datas = new ArrayList<>();
			ret.put("DATA", datas);

			ArrayList<Map<?, ?>> results = new ArrayList<>();
			results.add(rslt);

			Map<String, Object> data = new LinkedHashMap<>();
			data.put("RESULT", results);
			datas.add(data);

//			ret.put("TAIL", rslt);
		}
		return ret;
	}

}
