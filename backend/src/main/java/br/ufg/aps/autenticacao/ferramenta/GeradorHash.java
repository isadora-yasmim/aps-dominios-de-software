package br.ufg.aps.autenticacao.ferramenta;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import br.ufg.aps.autenticacao.servico.ConfiguracaoSenha;

/**
 * Utilitário de linha de comando, separado da API, que gera o hash Argon2id de uma
 * senha para montar o {@code banco/seed.sql}. Não inicia o Spring nem expõe endpoint.
 *
 * <p>Uso: {@code ./gerar-hash.sh} (pede a senha sem ecoar) ou {@code ./gerar-hash.sh <senha>}.
 */
public final class GeradorHash {

    private GeradorHash() {
    }

    public static void main(String[] args) throws IOException {
        String senha = args.length > 0 ? args[0] : lerSenha();
        if (senha == null || senha.isBlank()) {
            System.err.println("Informe uma senha não vazia.");
            System.exit(1);
        }
        System.out.println(ConfiguracaoSenha.criarCodificador().encode(senha));
    }

    private static String lerSenha() throws IOException {
        Console console = System.console();
        if (console != null) {
            char[] senha = console.readPassword("Senha: ");
            return senha == null ? null : new String(senha);
        }
        return new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)).readLine();
    }
}
