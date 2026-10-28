package br.ufg.aps.autenticacao.modelo;

/** Identificação devolvida em caso de sucesso: só id, e-mail e nome (RNF04). */
public record UsuarioAutenticado(long id, String email, String nome) {
}
