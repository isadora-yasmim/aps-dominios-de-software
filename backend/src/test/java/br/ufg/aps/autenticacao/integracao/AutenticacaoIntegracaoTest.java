package br.ufg.aps.autenticacao.integracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import br.ufg.aps.autenticacao.persistencia.RepositorioUsuario;

/**
 * Aplicação completa contra um PostgreSQL real, criado com os mesmos scripts de banco/
 * e acessado com o usuário de banco da aplicação (somente CONNECT e SELECT).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class AutenticacaoIntegracaoTest {

    private static final String SENHA_APP = "senha-do-teste";
    private static final Path BANCO = Path.of(System.getProperty("user.dir"), "..", "banco").normalize();

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("autenticacao")
            .withEnv("DB_SENHA_APP", SENHA_APP)
            .withCopyFileToContainer(script("schema.sql"), "/docker-entrypoint-initdb.d/01-schema.sql")
            .withCopyFileToContainer(script("permissoes.sql"), "/docker-entrypoint-initdb.d/02-permissoes.sql")
            .withCopyFileToContainer(script("seed.sql"), "/docker-entrypoint-initdb.d/03-seed.sql");

    @DynamicPropertySource
    static void bancoDaAplicacao(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", () -> "app_autenticacao");
        registro.add("spring.datasource.password", () -> SENHA_APP);
    }

    private static MountableFile script(String nome) {
        return MountableFile.forHostPath(BANCO.resolve(nome));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RepositorioUsuario repositorio;

    private MvcResult enviar(String email, String senha) throws Exception {
        String corpo = "{\"email\": \"" + email + "\", \"senha\": \"" + senha + "\"}";
        return mockMvc.perform(post("/api/autenticacao").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andReturn();
    }

    @Test
    void usuarioDoSeedAutenticaComASenhaCorreta() throws Exception {
        mockMvc.perform(post("/api/autenticacao").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"maria@aplicacao.example\", \"senha\": \"S3nh@Forte\"}"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(
                        "{\"id\": 1, \"email\": \"maria@aplicacao.example\", \"nome\": \"Maria Souza\"}",
                        JsonCompareMode.STRICT));
    }

    @Test
    void senhaIncorretaEUsuarioInexistenteTemRespostasIdenticas() throws Exception {
        MvcResult senhaIncorreta = enviar("maria@aplicacao.example", "errada");
        MvcResult inexistente = enviar("ninguem@aplicacao.example", "S3nh@Forte");

        assertThat(senhaIncorreta.getResponse().getStatus()).isEqualTo(401);
        assertThat(inexistente.getResponse().getStatus()).isEqualTo(401);
        assertThat(inexistente.getResponse().getContentAsString())
                .isEqualTo(senhaIncorreta.getResponse().getContentAsString())
                .isEqualTo("{\"mensagem\":\"E-mail ou senha inválidos.\"}");
        assertThat(inexistente.getResponse().getHeader("WWW-Authenticate"))
                .isEqualTo(senhaIncorreta.getResponse().getHeader("WWW-Authenticate"))
                .isEqualTo("Credenciais realm=\"aplicacao.example\"");
    }

    // RNF05: a consulta é parametrizada, então o texto chega ao SGBD como valor literal.
    @Test
    void injecaoDeSqlNoEmailNaoEncontraNenhumUsuario() throws Exception {
        assertThat(repositorio.buscarPorEmail("x' OR '1'='1")).isEmpty();
        assertThat(repositorio.buscarPorEmail("maria@aplicacao.example' --")).isEmpty();
        assertThat(enviar("x'OR'1'='1@a.com", "qualquer").getResponse().getStatus()).isEqualTo(401);
        assertThat(enviar("x' OR '1'='1@a.com", "qualquer").getResponse().getStatus()).isIn(400, 401);
    }

    // RNF02: o que está armazenado é Argon2id.
    @Test
    void hashesArmazenadosSaoArgon2id() {
        assertThat(jdbcTemplate.queryForList("SELECT hash_senha FROM usuario", String.class))
                .hasSize(3)
                .allSatisfy(hash -> assertThat(hash).startsWith("$argon2id$"));
    }

    // O usuário de banco da aplicação só tem SELECT: não consegue alterar dados.
    @Test
    void usuarioDeBancoDaAplicacaoNaoAlteraDados() {
        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE usuario SET nome = 'x'"))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO usuario (email, nome, hash_senha) VALUES ('a@a.com', 'a', 'a')"))
                .isInstanceOf(DataAccessException.class);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM usuario", Integer.class)).isEqualTo(3);
    }
}
