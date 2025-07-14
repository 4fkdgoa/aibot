package net.autocrm.api.config;

import javax.sql.DataSource;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.autocrm.common.ConfigKeys;

@Configuration
public class DatasourceConfig {

	@Value( ConfigKeys.JDBC_DRIVER )
	String driver;

	@Value( ConfigKeys.JDBC_URL )
	String url;

	@Value( ConfigKeys.JDBC_USER )
	String username;

	@Value( ConfigKeys.JDBC_PASS )
	String password;

	@Bean
	@ConfigurationProperties(prefix="spring.datasource.hikari")
	public DataSource datasource() {
		return DataSourceBuilder.create()
				.driverClassName(driver)
				.url( decrypt(url) )
				.username( decrypt(username) )
				.password( decrypt(password) )
				.build();
	}

	private String decrypt(String str) {
		try {
			return jasyptStringEncryptor.decrypt(str);
		} catch (EncryptionOperationNotPossibleException e ) {
			return str;
		}
	}
	
	@Autowired
	StringEncryptor jasyptStringEncryptor;
}
