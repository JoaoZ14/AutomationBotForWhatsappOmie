package com.whatsappomie.bot.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Automation Bot - WhatsApp + Omie")
                        .description(
                                "Webhooks (Twilio / teste JSON) e API de disparo proativo. "
                                        + "Header X-Bot-Key obrigatorio apenas se app.bot.disparo-api-key estiver configurado.")
                        .version("0.0.1"));
    }
}
