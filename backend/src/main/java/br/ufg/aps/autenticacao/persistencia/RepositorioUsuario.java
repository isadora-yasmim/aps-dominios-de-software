package br.ufg.aps.autenticacao.persistencia;

import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import br.ufg.aps.autenticacao.modelo.Usuario;

/** Único ponto de acesso ao SGBD (RF01.2; RNF05). */
@Repository
public class RepositorioUsuario {

    /** Consulta parametrizada: o e-mail nunca é concatenado no texto SQL. */
    static final String CONSULTA_POR_EMAIL =
            "SELECT id, email, nome, hash_senha FROM usuario WHERE email = ?";

    private static final RowMapper<Usuario> MAPEADOR = (rs, linha) -> new Usuario(
            rs.getLong("id"),
            rs.getString("email"),
            rs.getString("nome"),
            rs.getString("hash_senha"));

    private final JdbcTemplate jdbcTemplate;

    public RepositorioUsuario(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Busca o usuário pelo e-mail. A restrição UNIQUE garante 0 ou 1 linha.
     * Erros do banco sobem como {@link org.springframework.dao.DataAccessException}.
     */
    public Optional<Usuario> buscarPorEmail(String email) {
        return jdbcTemplate.query(CONSULTA_POR_EMAIL, MAPEADOR, email).stream().findFirst();
    }
}
