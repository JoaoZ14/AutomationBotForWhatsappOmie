package com.whatsappomie.bot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.whatsapp")
public class WhatsAppProperties {

    /** mock = só log; twilio = envio/recebimento via Twilio. */
    private String provider = "mock";

    private final Twilio twilio = new Twilio();

    public boolean isTwilio() {
        return "twilio".equalsIgnoreCase(provider.trim());
    }

    public boolean isTwilioConfigurado() {
        return isTwilio()
                && !twilio.getAccountSid().isBlank()
                && !twilio.getAuthToken().isBlank()
                && !twilio.getFromNumber().isBlank();
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Twilio getTwilio() {
        return twilio;
    }

    public static class Twilio {

        private String accountSid = "";
        private String authToken = "";
        /** Ex.: {@code whatsapp:+14155238886} (sandbox) ou seu número aprovado. */
        private String fromNumber = "";

        public String getAccountSid() {
            return accountSid;
        }

        public void setAccountSid(String accountSid) {
            this.accountSid = accountSid;
        }

        public String getAuthToken() {
            return authToken;
        }

        public void setAuthToken(String authToken) {
            this.authToken = authToken;
        }

        public String getFromNumber() {
            return fromNumber;
        }

        public void setFromNumber(String fromNumber) {
            this.fromNumber = fromNumber;
        }
    }
}
