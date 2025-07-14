package net.autocrm.api.util;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import org.apache.commons.codec.binary.Base64;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import net.autocrm.api.model.ClientDetails;
import net.autocrm.common.utils.PasswordGenerator;

public class JwtGenerator {

    public String createJWT(long ttlMills) {
    	return createJWT(null, null, null,null,null, ttlMills);
    }

    public String createJWT(Map<String, Object> header, Map<String, Object> claims, long ttlMills) {
    	return createJWT(header, claims, null,null,null, ttlMills);
    }

    public String createJWT(Map<String, Object> header, Map<String, Object> claims, String id, String issuer, String subject, long ttlMills) {
    	return createJWT(getApiKey(), header, claims, id, issuer, subject, ttlMills);
    }

    /**
     * 토큰생성
     * @param apikey
     * @param header
     * @param claims
     * @param issuer
     * @param subject
     * @param ttlMills
     * @return
     */
    private String createJWT(String apikey, Map<String, Object> header, Map<String, Object> claims, String id, String issuer, String subject, long ttlMills) {

        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);

        // 표준 클레임 셋팅
        JwtBuilder builder = Jwts.builder()
        		.header()
        			.empty()
        			.keyId(id)
        			.add(header)
        			.and()
        		.issuedAt(now)
                .subject(subject)
                .issuer(issuer)
                .signWith(getSignKey(apikey))
        		.claims(claims);

        // 토큰 만료 시간 셋팅
        if(ttlMills >= 0){
            long expMillis = nowMillis + ttlMills;
            Date exp = new Date(expMillis);
            builder.expiration(exp);
        }

        // 토큰 생성
        return builder.compact();
    }

    /**
     * 토큰 파싱
     * @param jwt
     * @return
     */
    public Jws<Claims> parseJwt(String jwt) {
        return parseJwt(getApiKey(), jwt);
    }

    /**
     * 토큰 파싱
     * @param jwt
     * @return
     */
    public Jws<Claims> parseJwt(String apikey, String jwt) {
        Jws<Claims> claims = Jwts.parser()
                .verifyWith(getSignKey(apikey))
                .build()
                .parseSignedClaims(jwt);
        
        return claims;
    }
    
    public boolean verify(String jwt) {
    	try {
    		parseJwt(jwt);
    	} catch (Throwable e) {
    		return false;
    	}
    	return true;
    }
    
    public boolean verify(String apikey, String jwt) {
    	try {
    		parseJwt(apikey, jwt);
    	} catch ( io.jsonwebtoken.security.SignatureException e ) {
    		return false;
    	}
    	return true;
    }

	@SuppressWarnings("unchecked")
	public Map<String, ?> getHeader(String jwt) {
		String[] pieces = jwt.split("\\.");
		String b64payload = pieces[0];
		try {
			String jsonString = new String(Base64.decodeBase64(b64payload), "UTF-8");
			return new ObjectMapper().readValue(jsonString, Map.class);
		} catch (UnsupportedEncodingException | JsonProcessingException e) {
			e.printStackTrace();
		}
		return null;
    }

	@SuppressWarnings("unchecked")
	public Map<String, ?> getPayload(String jwt) {
		String[] pieces = jwt.split("\\.");
		String b64payload = pieces[1];
		try {
			String jsonString = new String(Base64.decodeBase64(b64payload), "UTF-8");
			return new ObjectMapper().readValue(jsonString, Map.class);
		} catch (UnsupportedEncodingException | JsonProcessingException e) {
			e.printStackTrace();
		}
		return null;
    }

    private String getApiKey(){
    	Authentication a = SecurityContextHolder.getContext().getAuthentication();
    	return ((ClientDetails)a.getDetails()).getApikey();
    }

    private SecretKey getSignKey(String apikey){
        return Keys.hmacShaKeyFor(apikey.getBytes(StandardCharsets.UTF_8));
    }

    public static void main(String[] args) {
    	String apikey = PasswordGenerator.generatePassword(32,PasswordGenerator.ALPHA_CAPS + PasswordGenerator.NUMERIC + PasswordGenerator.ALPHA);
		System.out.println(apikey);
    	String tkn = new JwtGenerator().createJWT(apikey, null, null, null,null,null, 1000);
		System.out.println(tkn);
		System.out.println(new JwtGenerator().parseJwt(apikey, tkn));

		Key key = Jwts.SIG.HS512.key().build();
		String token = Jwts.builder().signWith(key).compact();
		System.out.println(token);
	}
}
