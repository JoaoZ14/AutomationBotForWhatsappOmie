package com.whatsappomie.bot.util;

/**
 * Unifica telefone em E.164 (ex.: {@code +5511999999999}) para armazenar e enviar à Twilio.
 */
public final class TelefoneNormalizer {

    private TelefoneNormalizer() {}

    public static String paraE164(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String s = input.replace("whatsapp:", "").trim().replaceAll("\\s", "");
        String digits = s.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return input.trim();
        }
        if (!digits.startsWith("55") && digits.length() <= 11) {
            digits = "55" + digits;
        }
        return "+" + digits;
    }

    /** Formato exigido pela API Twilio WhatsApp ({@code whatsapp:+5511...}). */
    public static String paraTwilioWhatsApp(String e164OuQualquer) {
        String e164 = paraE164(e164OuQualquer);
        if (e164.isEmpty()) {
            return e164OuQualquer;
        }
        return "whatsapp:" + e164;
    }
}
