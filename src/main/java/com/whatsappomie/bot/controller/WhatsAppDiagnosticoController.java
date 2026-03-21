package com.whatsappomie.bot.controller;

import com.whatsappomie.bot.config.WhatsAppProperties;
import com.whatsappomie.bot.util.TelefoneNormalizer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Bot - diagnóstico WhatsApp")
@RestController
@RequestMapping("/api/bot")
public class WhatsAppDiagnosticoController {

    private final WhatsAppProperties whatsAppProperties;

    public WhatsAppDiagnosticoController(WhatsAppProperties whatsAppProperties) {
        this.whatsAppProperties = whatsAppProperties;
    }

    @Operation(summary = "Ver se Twilio está ativo e como o telefone será formatado")
    @GetMapping("/whatsapp/diagnostico")
    public Map<String, Object> diagnostico(@RequestParam(required = false) String telefone) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("provider", whatsAppProperties.getProvider());
        m.put("twilioEnvioAtivo", whatsAppProperties.isTwilioConfigurado());
        m.put(
                "dica",
                whatsAppProperties.isTwilioConfigurado()
                        ? "Se a mensagem não chega: (1) No sandbox Twilio, envie join <palavra> do SEU WhatsApp para o número da Twilio. (2) Console Twilio: Monitor > Logs > Errors."
                        : "MODO MOCK: nada vai ao WhatsApp real. Defina WHATSAPP_PROVIDER=twilio e TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_WHATSAPP_FROM.");
        if (telefone != null && !telefone.isBlank()) {
            m.put("e164", TelefoneNormalizer.paraE164(telefone.trim()));
            m.put("twilioTo", TelefoneNormalizer.paraTwilioWhatsApp(telefone.trim()));
        }
        return m;
    }
}
