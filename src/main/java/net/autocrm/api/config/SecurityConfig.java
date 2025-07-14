package net.autocrm.api.config;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import net.autocrm.api.model.ClientDetails;
import net.autocrm.api.service.ClientDetailManager;
import net.autocrm.api.util.JwtGenerator;
import net.sf.json.JSONObject;

@Configuration
@EnableJpaAuditing
@EnableScheduling
@EnableWebSecurity
public class SecurityConfig {
	
	private List<ClientDetails> clients;
	
	@Autowired ClientDetailManager clientMgr;
	
	@Autowired JwtGenerator jwtGen;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    	this.clients = clientMgr.list();

    	APIKeyAuthFilter filter = new APIKeyAuthFilter();
		filter.setAuthenticationManager(new AuthenticationManager() {
		
			@Override
			public Authentication authenticate(Authentication authentication) throws AuthenticationException {
				Authentication auth = null;
				ClientDetails client = null;
				String role = null;
				final String principal = (String) authentication.getPrincipal();
				if ( principal.startsWith("Bearer ") ) {
					String tkn = StringUtils.substringAfter(principal, "Bearer ").trim();
					try {
						ClientDetails detail = clients.stream().filter(c -> c.getSeq().equals( Integer.parseInt( (String)jwtGen.getPayload(tkn).get("sub") ) )).findFirst().get();
						if ( detail != null && jwtGen.verify(detail.getApikey(), tkn) ) {
							role = "ROLE_ACCESS";
							client = detail;
						}
					} catch (RuntimeException e) {
						if ( e instanceof ArrayIndexOutOfBoundsException )
							throw new BadCredentialsException( ErrorCode.WRONG_TOKEN.getCode(), e );
						else if ( e instanceof io.jsonwebtoken.ExpiredJwtException )
							throw new BadCredentialsException( ErrorCode.EXPIRED_TOKEN.getCode(), e );
						else if ( e instanceof io.jsonwebtoken.MalformedJwtException )
							throw new BadCredentialsException( ErrorCode.WRONG_TYPE_TOKEN.getCode(), e );
						else if ( e instanceof io.jsonwebtoken.UnsupportedJwtException )
							throw new BadCredentialsException( ErrorCode.UNSUPPORTED_TOKEN.getCode(), e );
						else
							throw new BadCredentialsException( ErrorCode.UNKNOWN_ERROR.getCode(), e );
					}
				} else {
					role = "ROLE_REFRESH";
					client = clients.stream().filter(c -> c.getApikey().equals(principal)).findFirst().orElse(null);
				}
				if ( client != null ) {
					final List<GrantedAuthority> authorities = AuthorityUtils.commaSeparatedStringToAuthorityList(role);
					auth = new RestAuthenticationToken(principal, client, authorities);
					auth.setAuthenticated(true);
				}
				return auth;
			}
		});
		filter.setAuthenticationFailureHandler(new AuthenticationFailureHandler() {
			
			@Override
			public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
					AuthenticationException exception) throws IOException, ServletException {
				request.setAttribute("exception", exception.getMessage());
			}
		});

		return http.authorizeHttpRequests(
					requests -> requests
								.requestMatchers(
										PathPatternRequestMatcher.withDefaults().matcher("/img/**"),
										PathPatternRequestMatcher.withDefaults().matcher("/css/**"),
										PathPatternRequestMatcher.withDefaults().matcher("/js/**"),
										PathPatternRequestMatcher.withDefaults().matcher("/$/**"),
										PathPatternRequestMatcher.withDefaults().matcher("/swagger-ui/**"),
										PathPatternRequestMatcher.withDefaults().matcher("/v3/api-docs/**"),
										PathPatternRequestMatcher.withDefaults().matcher("/favicon.ico"),
										PathPatternRequestMatcher.withDefaults().matcher("/robots.txt"),
										PathPatternRequestMatcher.withDefaults().matcher("/robot.txt"),
			                            // AI 테스트 페이지 접근 허용 추가
			                            PathPatternRequestMatcher.withDefaults().matcher("/test"),
			                            PathPatternRequestMatcher.withDefaults().matcher("/ai-test"),
			                            PathPatternRequestMatcher.withDefaults().matcher("/ai-simple"),
			                            // JSP forward 경로 추가
			                            PathPatternRequestMatcher.withDefaults().matcher("/WEB-INF/jsp/**"),
			                            // AI health check는 인증 없이 접근 가능
			                            PathPatternRequestMatcher.withDefaults().matcher("/ai/health"),
			                            PathPatternRequestMatcher.withDefaults().matcher("/ai/models")
								).permitAll()
								.requestMatchers(
										PathPatternRequestMatcher.withDefaults().matcher("/auth/tokens")
								).hasAuthority("ROLE_REFRESH")
								// AI 엔드포인트는 ACCESS 권한 필요
								.requestMatchers(
										PathPatternRequestMatcher.withDefaults().matcher("/ai/**")
								).hasAuthority("ROLE_ACCESS")
								.anyRequest().hasAuthority("ROLE_ACCESS")
		        )
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
		        .csrf(csrf -> { try { csrf.disable(); } catch (Exception e) { e.printStackTrace(); } })
		        .sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilter(filter)
                .exceptionHandling(
                	handling -> handling
                				.authenticationEntryPoint(new CustomAuthenticationEntryPoint())
                )
                .build();

    }

    @Bean
    JwtGenerator jwtGen() {
    	return new JwtGenerator();
    }
    
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOriginPattern("*"); // 모든 도메인 허용 (개발환경용, 운영에서는 특정 도메인만)
        configuration.addAllowedMethod("*"); // 모든 HTTP 메서드 허용
        configuration.addAllowedHeader("*"); // 모든 헤더 허용
        configuration.setAllowCredentials(true); // 인증 정보 허용
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

@Getter
enum ErrorCode {

	WRONG_TYPE_TOKEN("1000", "Invalid Token"),
	EXPIRED_TOKEN("1010", "Expired Token"),
	UNSUPPORTED_TOKEN("1020", "Unsupported Token"),
	WRONG_TOKEN("1030", "Wrong Token"),
	ACCESS_DENIED("4000", "Access is Denied"),
	UNKNOWN_ERROR("9999", "Unknown Error")
    ;

    private final String code;
    private final String message;

    ErrorCode(final String code, final String message) {
        this.code = code;
        this.message = message;
    }
}

class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        String exception = (String)request.getAttribute("exception");

        if(exception == null) {
            setResponse(response, ErrorCode.ACCESS_DENIED);
        }
        else {
        	ErrorCode err = null;
            for (ErrorCode errorCode : ErrorCode.values()) {
            	if(exception.equals(errorCode.getCode())) {
            		err = errorCode;
            		break;
            	}
    		}
    		setResponse(response, err == null ? ErrorCode.UNKNOWN_ERROR : err);
        }
    }

    private void setResponse(HttpServletResponse response, ErrorCode ErrorCode) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        JSONObject responseJson = new JSONObject();
        responseJson.put("timestamp", DateFormatUtils.format(Calendar.getInstance(), "yyyy-MM-dd HH:mm:ss"));
        responseJson.put("errorCode", HttpServletResponse.SC_UNAUTHORIZED);
        responseJson.put("errorMessage", ErrorCode.getMessage());

        response.getWriter().print(responseJson);
    }
}

class RestAuthenticationToken extends AbstractAuthenticationToken {
	private static final long serialVersionUID = -7201404244040080775L;
	private final Object principal;

	public RestAuthenticationToken(Object principal, Object detail, Collection<? extends GrantedAuthority> authorities) {
		super(authorities);
		
		this.principal = principal;
		setDetails(detail);
		setAuthenticated(true);
	}

	@Override
	public Object getCredentials() {
		return "";
	}

	@Override
	public Object getPrincipal() {
		return this.principal;
	}
	
}