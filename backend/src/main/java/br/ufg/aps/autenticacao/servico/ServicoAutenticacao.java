package br.ufg.aps.autenticacao.servico;

import java.util.Optional;

import org.springframework.stereotype.Service;

import br.ufg.aps.autenticacao.modelo.Usuario;
import br.ufg.aps.autenticacao.modelo.UsuarioAutenticado;
import br.ufg.aps.autenticacao.persistencia.RepositorioUsuario;

/**
 * Decide o resultado da autenticação (RF01.2–RF01.4). Não conhece HTTP: devolve o
 * usuário autenticado ou lança {@link FalhaAutenticacaoException}.
 */
@Service
public class ServicoAutenticacao {

    private final RepositorioUsuario repositorio;
    private final VerificadorSenha verificador;

    public ServicoAutenticacao(RepositorioUsuario repositorio, VerificadorSenha verificador) {
        this.repositorio = repositorio;
        this.verificador = verificador;
    }

    public UsuarioAutenticado autenticar(String email, String senha) {
        // RF01.2 – existência
        Optional<Usuario> encontrado = repositorio.buscarPorEmail(email);
        if (encontrado.isEmpty()) {
            verificador.verificarContraHashFicticio(senha);
            throw new FalhaAutenticacaoException();
        }

        // RF01.3 – comparação
        Usuario usuario = encontrado.get();
        if (!verificador.confere(senha, usuario.hashSenha())) {
            throw new FalhaAutenticacaoException();
        }

        // RF01.4 – decisão
        return new UsuarioAutenticado(usuario.id(), usuario.email(), usuario.nome());
    }
}
