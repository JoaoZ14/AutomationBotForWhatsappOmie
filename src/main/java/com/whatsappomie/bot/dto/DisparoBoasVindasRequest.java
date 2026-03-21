package com.whatsappomie.bot.dto;

import jakarta.validation.constraints.NotBlank;

public class DisparoBoasVindasRequest {

    @NotBlank
    private String telefone;

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }
}
