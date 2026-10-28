package br.ufg.aps.autenticacao.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import br.ufg.aps.autenticacao.modelo.Usuario;
import br.ufg.aps.autenticacao.modelo.UsuarioAutenticado;
import br.ufg.aps.autenticacao.persistencia.RepositorioUsuario;

@ExtendWith(MockitoExtension.class)
class ServicoAutenticacaoTest {

    private static final String EMAIL = "maria@aplicacao.example";
    private static final Usuario MARIA = new Usuario(42, EMAIL, "Maria Souza", "$argon2id$hash-da-maria");

    @Mock
    private RepositorioUsuario repositorio;

    @Mock
    private VerificadorSenha verificador;

    @InjectMocks
    private ServicoAutenticacao servico;

    @Test
    void credenciaisValidasDevolvemIdEmailENome() {
        when(repositorio.buscarPorEmail(EMAIL)).thenReturn(Optional.of(MARIA));
        when(verificador.confere("S3nh@Forte", MARIA.hashSenha())).thenReturn(true);

        UsuarioAutenticado autenticado = servico.autenticar(EMAIL, "S3nh@Forte");

        assertThat(autenticado).isEqualTo(new UsuarioAutenticado(42, EMAIL, "Maria Souza"));
    }

    @Test
    void senhaIncorretaLancaFalhaDeAutenticacao() {
        when(repositorio.buscarPorEmail(EMAIL)).thenReturn(Optional.of(MARIA));
        when(verificador.confere("errada", MARIA.hashSenha())).thenReturn(false);

        assertThatThrownBy(() -> servico.autenticar(EMAIL, "errada"))
                .isInstanceOf(FalhaAutenticacaoException.class);
        verify(verificador, never()).verificarContraHashFicticio(anyString());
    }

    // Caso 4 (RNF06)
    @Test
    void usuarioInexistenteVerificaASenhaContraOHashFicticio() {
        when(repositorio.buscarPorEmail("ninguem@aplicacao.example")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.autenticar("ninguem@aplicacao.example", "S3nh@Forte"))
                .isInstanceOf(FalhaAutenticacaoException.class);
        verify(verificador).verificarContraHashFicticio("S3nh@Forte");
        verify(verificador, never()).confere(anyString(), anyString());
    }

    @Test
    void falhaNoBancoSobeSemSerConvertidaEmFalhaDeAutenticacao() {
        when(repositorio.buscarPorEmail(EMAIL)).thenThrow(new DataAccessResourceFailureException("SGBD fora"));

        assertThatThrownBy(() -> servico.autenticar(EMAIL, "S3nh@Forte"))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }
}
