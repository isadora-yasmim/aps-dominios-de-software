package br.ufg.aps.autenticacao.modelo;

/** Linha da tabela {@code usuario}. Uso interno: nunca é serializada em respostas. */
public record Usuario(long id, String email, String nome, String hashSenha) {

    @Override
    public String toString() {
        return "Usuario[id=" + id + ", email=" + email + ", nome=" + nome + ", hashSenha=***]";
    }
}
