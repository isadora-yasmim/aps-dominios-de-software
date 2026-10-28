package br.ufg.aps.autenticacao.modelo;

/** Corpo de todas as respostas de erro da API: {@code {"mensagem": "..."}}. */
public record RespostaErro(String mensagem) {
}
