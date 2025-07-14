package net.autocrm.api.config;

import org.apache.commons.csv.CSVFormat;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CSVImportConfiguration {

	@Bean
	public CSVFormat getCSVFormat() {
		return CSVFormat.DEFAULT.builder()
	            .setIgnoreEmptyLines(false)
	            .setAllowMissingColumnNames(true)
	            .setDelimiter(';')
	            .build();
	}

}
