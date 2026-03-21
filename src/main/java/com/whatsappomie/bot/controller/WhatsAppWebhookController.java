package com.whatsappomie.bot.controller;

import com.whatsappomie.bot.dto.WebhookAckResponse;
import com.whatsappomie.bot.dto.WhatsAppWebhookRequest;
import com.whatsappomie.bot.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Webhook - teste JSON")
@RestController
@RequestMapping("/webhook")
public class WhatsAppWebhookController {

    private final ConversationService conversationService;

    public WhatsAppWebhookController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @Operation(summary = "Webhook JSON (Postman / testes)")
    @PostMapping("/whatsapp")
    public ResponseEntity<WebhookAckResponse> receberMensagem(@Valid @RequestBody WhatsAppWebhookRequest body) {
        conversationService.processarMensagem(body.getFrom(), body.getBody());
        return ResponseEntity.ok(new WebhookAckResponse(true, "processado"));
    }
}
