package com.example.bishe.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI springShopOpenAPI() {
        String schemaName = "Authorization";
        return new OpenAPI()
                .info(new Info()
                        .title("毕设个性化学习推荐系统-接口文档")
                        .version("v1.0.0")
                        .description("基于Spring Boot 3 + Redis +异步审计的高性能系统"))
//                        .license(new License().name("Apache 2.0").url("https://springdoc.org"))
                //配置全局鉴权（JWT）
                //在Swagger前端输入框中输入：Authorization，输入框右侧选择Bearer Token，输入JWT生成的Token
//                .addSecurityItem(new SecurityRequirement().addList(schemaName))
                .components(new Components()
                        .addSecuritySchemes(schemaName, new SecurityScheme()
//                                .name(schemaName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    @Bean
    public GroupedOpenApi publicApi(){
        String schemaName = "Authorization";
      return GroupedOpenApi.builder()
              .group("全部接口")
              .pathsToMatch("/api/**")
              //不需要在Controller加注解,自动给匹配路径下的所有接口加"锁"
              .addOperationCustomizer((operation, handlerMethod) -> {
                  operation.addParametersItem(new io.swagger.v3.oas.models.parameters.Parameter()
                          .in("header")
                          .name(schemaName)
                          .description("格式：Bearer {token}")
                          .required(true)
                          .schema(new io.swagger.v3.oas.models.media.StringSchema()));
                  return operation;
              })
              .build();
    }
}

