package net.autocrm.api.config;

import java.io.IOException;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfa.common.SpringContextUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.autocrm.api.service.MainManager;
import net.autocrm.api.util.CustomHttpRequestBody;
import net.autocrm.common.model.Parameters;
import net.autocrm.common.utils.RequestUtil;

@Slf4j
public class APIKeyAuthFilter extends AbstractPreAuthenticatedProcessingFilter {

	private final ObjectMapper ob = new ObjectMapper();

	@Override
	protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
		Parameters<?, ?> p = null;
		if ( request.getContentLength() > 0 && !StringUtils.equals(request.getContextPath() + "/error", request.getRequestURI()) ) {
			try {
				p = ob.readValue( request.getReader().lines().collect(Collectors.joining()), Parameters.class );
			} catch (JsonParseException e) {
				log.debug( "그냥 패스" );
			} catch (IOException e) {
				log.debug( request.getContentType() );
				e.printStackTrace();
			}
		}
		String secret = p == null ? null : p.getString("secret");
		if ( StringUtils.isNotBlank(secret) ) {
			return secret;
		} else {
			String a = request.getHeader(HttpHeaders.AUTHORIZATION);
			if ( a == null ) {
				request.setAttribute("exception", ErrorCode.ACCESS_DENIED.getCode());
			} else if ( a.startsWith("Bearer ") ) {
				return a;
			} else {
				request.setAttribute("exception", ErrorCode.WRONG_TOKEN.getCode());
			}
			return null;
		}
	}

	
	@Override
	protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
    	return "N/A";
	}


	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		CustomHttpRequestBody req = new CustomHttpRequestBody((HttpServletRequest) request);

//		ContentCachingRequestWrapper req = new ContentCachingRequestWrapper((HttpServletRequest)request);
		ContentCachingResponseWrapper res = new ContentCachingResponseWrapper((HttpServletResponse) response);

    	super.doFilter(req, res, chain);

        // save REQUESET_LOG
    	String reqUri = req.getRequestURI();
    	if ( !ArrayUtils.contains(new String[] {"/api/auth/tokens", "/api/error", "/api/info/wdms", "/api/info/log"}, reqUri)
    			&& !StringUtils.startsWithAny(reqUri, "/api/swagger", "/api/v2/") ) {
    		MainManager mainMgr = (MainManager) SpringContextUtils.getBean("mainMgr");
            if ( mainMgr != null ) {
	    		try {
		        	Parameters<String, Object> params = new Parameters<>();
		        	params.put( "ip", RequestUtil.getRemoteAddr(req) );
		        	params.put( "requestUri", reqUri );
		        	params.put( "referer", req.getHeader(HttpHeaders.REFERER) );
		        	params.put( "query", req.getQueryString() );
		        	params.put( "body", new String(req.getRawData(), req.getCharacterEncoding()) );
		        	params.put( "userAgent", req.getHeader(HttpHeaders.USER_AGENT) );
		        	params.put( "method", req.getMethod() );
					params.put( "status", res.getStatus() );
					params.put( "res", new String(res.getContentAsByteArray(), req.getCharacterEncoding()) );
					mainMgr.insertRequestLog(params);
		        } catch ( Throwable e ) {
		        	log.warn( e.getMessage(),e );
		        }
            }
    	}

        res.copyBodyToResponse();
	}
}
