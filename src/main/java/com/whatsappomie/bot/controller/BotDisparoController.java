package com.whatsappomie.bot.controller;

import com.whatsappomie.bot.config.BotApiProperties;
import com.whatsappomie.bot.dto.DisparoBoasVindasRequest;
import com.whatsappomie.bot.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Disparo proativo: envia a primeira mensagem (menu) para o cliente poder responder em seguida.
 * Útil com Twilio/Meta (mensagem iniciada pelo negócio dentro das regras da plataforma).
 */
@Tag(name = "Bot - disparo proativo")
@RestController
@RequestMapping("/api/bot")
public class BotDisparoController {

    private final ConversationService conversationService;
    private final BotApiProperties botApiProperties;

    public BotDisparoController(ConversationService conversationService, BotApiProperties botApiProperties) {
        this.conversationService = conversationService;
        this.botApiProperties = botApiProperties;
    }

    @Operation(
            summary = "Disparar mensagem inicial (menu 1/2)",
            description =
                    "Envia boas-vindas no WhatsApp e deixa o atendimento aguardando 1 ou 2. "
                            + "Header X-Bot-Key obrigatorio apenas se app.bot.disparo-api-key estiver configurado.")
    @PostMapping("/disparar-boas-vindas")
    public ResponseEntity<Map<String, String>> dispararBoasVindas(
            @RequestHeader(value = "X-Bot-Key", required = false) String botKey,
            @Valid @RequestBody DisparoBoasVindasRequest body) {
        if (!botApiProperties.autorizarDisparo(botKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("erro", "X-Bot-Key inválido ou ausente"));
        }
        conversationService.dispararMensagemInicial(body.getTelefone());
        return ResponseEntity.ok(Map.of("mensagem", "Boas-vindas enviadas; aguardando resposta do cliente."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> onIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> onIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erro", e.getMessage()));
    }
}
