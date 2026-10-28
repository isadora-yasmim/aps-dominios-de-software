package br.ufg.aps.autenticacao.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class VerificadorSenhaTest {

    // Caso 9 (RNF02)
    @Test
    void hashGeradoEArgon2idEVerificadorReconheceSoASenhaCorreta() {
        PasswordEncoder codificador = ConfiguracaoSenha.criarCodificador();
        VerificadorSenha verificador = new VerificadorSenha(codificador);

        String hash = codificador.encode("S3nh@Forte");

        assertThat(hash).startsWith("$argon2id$v=19$m=19456,t=2,p=1$");
        assertThat(verificador.confere("S3nh@Forte", hash)).isTrue();
        assertThat(verificador.confere("s3nh@forte", hash)).isFalse();
        assertThat(verificador.confere("S3nh@Forte ", hash)).isFalse();
    }

    @Test
    void hashesDaMesmaSenhaSaoDiferentesPorCausaDoSal() {
        PasswordEncoder codificador = ConfiguracaoSenha.criarCodificador();

        assertThat(codificador.encode("S3nh@Forte")).isNotEqualTo(codificador.encode("S3nh@Forte"));
    }

    // Caso 4 (RNF06): o hash fictício é gerado uma vez, com o mesmo codificador, e usado na verificação
    @Test
    void verificacaoDeUsuarioInexistenteUsaOHashFicticio() {
        PasswordEncoder codificador = mock(PasswordEncoder.class);
        when(codificador.encode(anyString())).thenReturn("$argon2id$hash-ficticio");
        VerificadorSenha verificador = new VerificadorSenha(codificador);

        verificador.verificarContraHashFicticio("S3nh@Forte");

        verify(codificador).encode(anyString());
        verify(codificador).matches("S3nh@Forte", "$argon2id$hash-ficticio");
    }
}
