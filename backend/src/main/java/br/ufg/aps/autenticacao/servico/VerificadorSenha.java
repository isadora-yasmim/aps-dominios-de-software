package br.ufg.aps.autenticacao.servico;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Componente auxiliar do serviço que compara senhas com hashes (RF01.3; RNF02; RNF06). */
@Component
public class VerificadorSenha {

    private final PasswordEncoder codificador;

    /** Gerado uma vez na inicialização, com o mesmo algoritmo e parâmetros dos hashes reais. */
    private final String hashFicticio;

    public VerificadorSenha(PasswordEncoder codificador) {
        this.codificador = codificador;
        this.hashFicticio = codificador.encode(UUID.randomUUID().toString());
    }

    /** A senha nunca é comparada em texto: o codificador recalcula o hash e compara. */
    public boolean confere(String senha, String hashSenha) {
        return codificador.matches(senha, hashSenha);
    }

    /**
     * Usado quando o usuário não existe: faz o mesmo trabalho de uma verificação real
     * e descarta o resultado, para que o tempo de resposta seja semelhante (RNF06).
     */
    public void verificarContraHashFicticio(String senha) {
        codificador.matches(senha, hashFicticio);
    }
}
