package br.ufg.aps.autenticacao.servico;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Algoritmo e parâmetros do hash de senha (RNF02), compartilhados pela API e pelo GeradorHash. */
@Configuration
public class ConfiguracaoSenha {

    // Argon2id com os parâmetros mínimos recomendados pela OWASP: m=19 MiB, t=2, p=1.
    private static final int TAMANHO_SAL = 16;
    private static final int TAMANHO_HASH = 32;
    private static final int PARALELISMO = 1;
    private static final int MEMORIA_KIB = 19 * 1024;
    private static final int ITERACOES = 2;

    public static PasswordEncoder criarCodificador() {
        return new Argon2PasswordEncoder(TAMANHO_SAL, TAMANHO_HASH, PARALELISMO, MEMORIA_KIB, ITERACOES);
    }

    @Bean
    public PasswordEncoder codificadorSenha() {
        return criarCodificador();
    }
}
