package com.whatsappomie.bot.integration.whatsapp;

import com.whatsappomie.bot.config.WhatsAppProperties;
import com.whatsappomie.bot.util.TelefoneNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);

    private final RestTemplate restTemplate;
    private final WhatsAppProperties properties;

    public WhatsAppService(RestTemplate restTemplate, WhatsAppProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public void enviarMensagem(String telefone, String mensagem) {
        if (!properties.isTwilioConfigurado()) {
            log.warn(
                    "[WhatsApp MOCK] Mensagem NÃO enviada. provider={} twilioConfigurado=false. "
                            + "Configure WHATSAPP_PROVIDER=twilio, TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_WHATSAPP_FROM. "
                            + "Destino: {} | Texto: {}",
                    properties.getProvider(),
                    telefone,
                    mensagem);
            return;
        }

        WhatsAppProperties.Twilio t = properties.getTwilio();
        String url = String.format(
                "https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json", t.getAccountSid());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(t.getAccountSid(), t.getAuthToken());

        String to = TelefoneNormalizer.paraTwilioWhatsApp(telefone);
        String from = t.getFromNumber().trim();

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", to);
        form.add("From", from);
        form.add("Body", mensagem);

        try {
            ResponseEntity<String> resp =
                    restTemplate.postForEntity(url, new HttpEntity<>(form, headers), String.class);
            log.info(
                    "[WhatsApp Twilio] HTTP {} From={} To={} resposta={}",
                    resp.getStatusCode(),
                    from,
                    to,
                    resp.getBody());
        } catch (HttpStatusCodeException e) {
            log.error(
                    "[WhatsApp Twilio] Falha ao enviar status={} body={}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString());
            throw new IllegalStateException("Falha ao enviar mensagem WhatsApp (Twilio)", e);
        }
    }
}
