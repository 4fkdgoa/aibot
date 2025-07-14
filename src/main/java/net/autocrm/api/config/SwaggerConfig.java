package net.autocrm.api.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import io.swagger.v3.oas.annotations.servers.Server;

@Configuration
@OpenAPIDefinition(
        info = @Info(
				title = "AutoCRM API Document",
//        		description = "API Document",
        		version = "1.0",
//				termsOfService = "https://ec.ums.local",
        		license = @License(
        				name = "Autobrain Inc.",
        				url = "http://www.autobrain.co.kr"
                )
        ),
        servers = {
        		@Server(url = "${server.servlet.context-path}")	
        },
        security = {
        		@SecurityRequirement(name = "OAUTH2 token")
        }
)
@SecuritySchemes({
	@SecurityScheme(
			name = "OAUTH2 token",
			type = SecuritySchemeType.HTTP,
			scheme = "bearer",
			bearerFormat = "JWT",
			description = "Bearer 제외하고 입력",
			in = SecuritySchemeIn.HEADER,
			paramName = HttpHeaders.AUTHORIZATION
	)
})
@ConditionalOnExpression(value = "${use.swagger:false}")
public class SwaggerConfig {

	@Bean
	GroupedOpenApi groupApi () {
		return GroupedOpenApi.builder()
				.group("api")
				.packagesToScan("net.autocrm.api.controller")
				.build();
	}

//	@Bean
//    OpenAPI baseOpenAPI() {
//		io.swagger.v3.oas.models.security.SecurityScheme securityScheme
//			= new io.swagger.v3.oas.models.security.SecurityScheme()
//                .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
//                .scheme("bearer").bearerFormat("JWT")
//                .in(io.swagger.v3.oas.models.security.SecurityScheme.In.HEADER)
//                .name(HttpHeaders.AUTHORIZATION);
//		io.swagger.v3.oas.models.security.SecurityRequirement securityRequirement
//			= new io.swagger.v3.oas.models.security.SecurityRequirement().addList("bearerAuth");
//        return new OpenAPI()
//        		.components(
//        				new Components()
//        				.addSecuritySchemes("bearerAuth", securityScheme)
//        		).security(Arrays.asList(securityRequirement));
//    }

}
