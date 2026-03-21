package com.whatsappomie.bot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bot")
public class BotApiProperties {

    /**
     * Se preenchido, o disparo exige header {@code X-Bot-Key} com o mesmo valor.
     */
    private String disparoApiKey = "";

    public String getDisparoApiKey() {
        return disparoApiKey;
    }

    public void setDisparoApiKey(String disparoApiKey) {
        this.disparoApiKey = disparoApiKey;
    }

    public boolean autorizarDisparo(String headerXBotKey) {
        if (disparoApiKey == null || disparoApiKey.isBlank()) {
            return true;
        }
        return disparoApiKey.equals(headerXBotKey);
    }
}
