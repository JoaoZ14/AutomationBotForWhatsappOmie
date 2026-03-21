package com.whatsappomie.bot.controller;

import com.whatsappomie.bot.service.ConversationService;
import com.whatsappomie.bot.util.TelefoneNormalizer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook no formato Twilio (form urlencoded). Configure no console Twilio apontando para URL pública (ex.: ngrok).
 */
@Tag(name = "Webhook - Twilio")
@RestController
@RequestMapping("/webhook/twilio")
public class TwilioWhatsAppWebhookController {

    private final ConversationService conversationService;

    public TwilioWhatsAppWebhookController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @Operation(summary = "Webhook Twilio (form-urlencoded)")
    @PostMapping(value = "/whatsapp", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> receber(
            @RequestParam("From") String from,
            @RequestParam(value = "Body", required = false) String body) {
        String telefone = TelefoneNormalizer.paraE164(from);
        conversationService.processarMensagem(telefone, body != null ? body : "");
        return ResponseEntity.ok().build();
    }
}
