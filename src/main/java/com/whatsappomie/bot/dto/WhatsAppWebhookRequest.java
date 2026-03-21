package com.whatsappomie.bot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class WhatsAppWebhookRequest {

    @NotBlank
    @JsonProperty("from")
    private String from;

    @NotBlank
    private String body;

    public WhatsAppWebhookRequest() {}

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
