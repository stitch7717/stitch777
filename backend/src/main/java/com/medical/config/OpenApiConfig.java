package com.medical.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI 配置：访问 http://localhost:8080/swagger-ui.html
 * 先调登录接口拿 token，再点 Authorize 填 Bearer token。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI hisOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("医疗后台管理系统 API")
                        .version("1.0.0")
                        .description("HIS 后台接口文档。先调用 /api/system/user/login 获取 token，再点 Authorize 填入。"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
