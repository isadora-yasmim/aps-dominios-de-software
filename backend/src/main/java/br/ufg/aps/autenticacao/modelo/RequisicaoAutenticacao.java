package br.ufg.aps.autenticacao.modelo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Corpo de {@code POST /api/autenticacao} (RF01.1). */
public record RequisicaoAutenticacao(
        @NotBlank @Email String email,
        @NotBlank String senha) {

    @Override
    public String toString() {
        return "RequisicaoAutenticacao[email=" + email + ", senha=***]";
    }
}
