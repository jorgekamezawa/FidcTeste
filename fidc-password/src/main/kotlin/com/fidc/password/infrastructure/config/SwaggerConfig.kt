package com.fidc.password.infrastructure.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("FidcPassword API")
                    .description("API para gestão de primeiro acesso e redefinição de senhas com integração Active Directory")
                    .version("1.0.0")
            )
            .addServersItem(
                Server()
                    .url("http://localhost:8080")
                    .description("Ambiente Local")
            )
    }
}