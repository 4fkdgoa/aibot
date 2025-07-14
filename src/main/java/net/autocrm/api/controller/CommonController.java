package net.autocrm.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import net.autocrm.api.model.ClientDetails;
import net.autocrm.api.util.JwtGenerator;

public abstract class CommonController {

    @Autowired
	protected Environment config;
	
	@Autowired
	protected MessageSource msg;

	@Autowired
	protected JwtGenerator jwtGen;

	protected ClientDetails getClient() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		return (ClientDetails)auth.getDetails();
	}

	protected String getApikey() {
		return getClient().getApikey();
	}
}
