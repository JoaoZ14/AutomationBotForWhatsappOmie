package com.whatsappomie.bot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "atendimento")
public class Atendimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String telefone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private StatusAtendimento status;

    @Column(nullable = false)
    private Integer tentativas = 0;

    @Column(nullable = false)
    private Instant ultimoContato;

    private Instant expiraEm;

    @Column(columnDefinition = "TEXT")
    private String contextoJson;

    public Atendimento() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public StatusAtendimento getStatus() {
        return status;
    }

    public void setStatus(StatusAtendimento status) {
        this.status = status;
    }

    public Integer getTentativas() {
        return tentativas;
    }

    public void setTentativas(Integer tentativas) {
        this.tentativas = tentativas;
    }

    public Instant getUltimoContato() {
        return ultimoContato;
    }

    public void setUltimoContato(Instant ultimoContato) {
        this.ultimoContato = ultimoContato;
    }

    public Instant getExpiraEm() {
        return expiraEm;
    }

    public void setExpiraEm(Instant expiraEm) {
        this.expiraEm = expiraEm;
    }

    public String getContextoJson() {
        return contextoJson;
    }

    public void setContextoJson(String contextoJson) {
        this.contextoJson = contextoJson;
    }
}
