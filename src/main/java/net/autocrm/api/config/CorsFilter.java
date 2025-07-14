package net.autocrm.api.config;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import net.autocrm.common.ConfigKeys;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsFilter implements Filter {

	@Value( ConfigKeys.CORS_ALLOWED_ORIGIN )
	private String[] allowedOrigins;

	@Value( ConfigKeys.CORS_ALLOWED_HEADERS )
	private String allowedHeaders;

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		
		if ( ArrayUtils.contains( allowedOrigins, "*" ) ) {
			response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
		} else {
			String origin = request.getHeader(HttpHeaders.ORIGIN);
			if ( ArrayUtils.contains( allowedOrigins, origin ) ) {
				response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
			}
		}

		if ( StringUtils.isNotBlank( response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN) ) ) {
			response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "POST, GET, PUT, OPTIONS, DELETE");
			response.setHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
			response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, allowedHeaders);
			response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);
		}

        if( HttpMethod.OPTIONS.name().equalsIgnoreCase(request.getMethod()) ) {
            response.setStatus(HttpServletResponse.SC_OK);
        }else {
            chain.doFilter(req, res);
        }
	}

	public void init(FilterConfig filterConfig) {}

	public void destroy() {}

}