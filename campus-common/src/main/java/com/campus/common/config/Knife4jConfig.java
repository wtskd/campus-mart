package com.campus.common.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 接口文档配置（Knife4j / OpenAPI 3）
 *
 * <p>访问地址：http://localhost:{port}/doc.html</p>
 *
 * @author campus-mart
 */
@Configuration
@SecurityScheme(name = "Authorization", type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT", scheme = "bearer")
public class Knife4jConfig {

    @Value("${spring.application.name:campus-service}")
    private String appName;

    @Bean
    public OpenAPI campusOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("校园易站 CampusMart · " + appName)
                        .description("校园二手交易平台微服务接口文档")
                        .version("1.0.0")
                        .contact(new Contact().name("campus-mart"))
                        .license(new License().name("Apache 2.0")));
    }
}
