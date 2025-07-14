package net.autocrm.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sfa.common.SpringContextUtils;

@Configuration
public class CommonConfig {

	@Bean(name="springContextUtils")
	public SpringContextUtils getSpringContextUtils() {
		return SpringContextUtils.getInstance();
	}
}
