package br.ufg.aps.autenticacao.controle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import br.ufg.aps.autenticacao.modelo.Usuario;
import br.ufg.aps.autenticacao.persistencia.RepositorioUsuario;
import br.ufg.aps.autenticacao.servico.ConfiguracaoSenha;
import br.ufg.aps.autenticacao.servico.ServicoAutenticacao;
import br.ufg.aps.autenticacao.servico.VerificadorSenha;

/**
 * Camadas de controle e serviço reais (com o Argon2id real); só o repositório é simulado.
 * Cobre os casos 1, 2, 3, 5, 6, 7 e 8 da especificação.
 */
@WebMvcTest(ControladorAutenticacao.class)
@Import({ServicoAutenticacao.class, VerificadorSenha.class, ConfiguracaoSenha.class})
class ControladorAutenticacaoTest {

    private static final String URL = "/api/autenticacao";
    private static final String EMAIL = "maria@aplicacao.example";
    private static final String SENHA = "S3nh@Forte";
    private static final String CORPO_401 = "{\"mensagem\": \"E-mail ou senha inválidos.\"}";
    private static final String CORPO_400 = "{\"mensagem\": \"Requisição inválida.\"}";
    private static final String CORPO_500 = "{\"mensagem\": \"Erro interno. Tente novamente mais tarde.\"}";
    private static final String DESAFIO_401 = "Credenciais realm=\"aplicacao.example\"";

    private static String hashMaria;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RepositorioUsuario repositorio;

    @BeforeAll
    static void gerarHash() {
        hashMaria = ConfiguracaoSenha.criarCodificador().encode(SENHA);
    }

    private void mariaCadastrada() {
        when(repositorio.buscarPorEmail(EMAIL))
                .thenReturn(Optional.of(new Usuario(42, EMAIL, "Maria Souza", hashMaria)));
    }

    private MvcResult enviar(String corpo) throws Exception {
        return mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(corpo)).andReturn();
    }

    private static String credenciais(String email, String senha) {
        return "{\"email\": \"" + email + "\", \"senha\": \"" + senha + "\"}";
    }

    // Caso 1
    @Test
    void credenciaisValidasDevolvem200SoComIdEmailENome() throws Exception {
        mariaCadastrada();

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(credenciais(EMAIL, SENHA)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(
                        "{\"id\": 42, \"email\": \"maria@aplicacao.example\", \"nome\": \"Maria Souza\"}",
                        JsonCompareMode.STRICT))
                .andExpect(jsonPath("$.*", hasSize(3)))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    // Caso 2
    @Test
    void senhaIncorretaDevolve401ComMensagemGenerica() throws Exception {
        mariaCadastrada();

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(credenciais(EMAIL, "errada")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", DESAFIO_401))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(CORPO_401, JsonCompareMode.STRICT))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    // Caso 3 (RNF03)
    @Test
    void usuarioInexistenteDevolveCorpoIdenticoAoDeSenhaIncorreta() throws Exception {
        mariaCadastrada();

        MvcResult senhaIncorreta = enviar(credenciais(EMAIL, "errada"));
        MvcResult inexistente = enviar(credenciais("ninguem@aplicacao.example", SENHA));

        assertThat(inexistente.getResponse().getStatus()).isEqualTo(401);
        assertThat(senhaIncorreta.getResponse().getStatus()).isEqualTo(401);
        assertThat(inexistente.getResponse().getContentAsString())
                .isEqualTo(senhaIncorreta.getResponse().getContentAsString());
        assertThat(inexistente.getResponse().getHeaderNames())
                .containsExactlyInAnyOrderElementsOf(senhaIncorreta.getResponse().getHeaderNames());
        assertThat(inexistente.getResponse().getHeader("WWW-Authenticate"))
                .isEqualTo(senhaIncorreta.getResponse().getHeader("WWW-Authenticate"))
                .isEqualTo(DESAFIO_401);
    }

    // Caso 5 (RF01.1; FA1)
    @ParameterizedTest
    @ValueSource(strings = {
            "{\"email\": \"maria@aplicacao.example\", \"senha\": ",  // JSON malformado
            "não é JSON",
            "",                                                      // corpo ausente
            "null",
            "[]",
            "{\"senha\": \"S3nh@Forte\"}",                           // e-mail ausente
            "{\"email\": \"maria@aplicacao.example\"}",              // senha ausente
            "{\"email\": \"\", \"senha\": \"S3nh@Forte\"}",          // e-mail vazio
            "{\"email\": \"maria@aplicacao.example\", \"senha\": \"\"}",  // senha vazia
            "{\"email\": \"maria@aplicacao.example\", \"senha\": \"   \"}",
            "{\"email\": null, \"senha\": \"S3nh@Forte\"}",
            "{\"email\": \"maria.aplicacao.example\", \"senha\": \"S3nh@Forte\"}",  // e-mail inválido
            "{\"email\": \"maria@\", \"senha\": \"S3nh@Forte\"}"
    })
    void requisicaoInvalidaDevolve400SemConsultarORepositorio(String corpo) throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(CORPO_400, JsonCompareMode.STRICT));

        verifyNoInteractions(repositorio);
    }

    // Caso 6 (FE1)
    @Test
    void falhaNoBancoDevolve500SemDetalhesInternos() throws Exception {
        when(repositorio.buscarPorEmail(anyString())).thenThrow(new DataAccessResourceFailureException(
                "FATAL: SQLSTATE 08006 conexão recusada em 172.28.0.20:5432"));

        MvcResult resultado = mockMvc.perform(
                        post(URL).contentType(MediaType.APPLICATION_JSON).content(credenciais(EMAIL, SENHA)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(CORPO_500, JsonCompareMode.STRICT))
                .andReturn();

        assertThat(resultado.getResponse().getContentAsString())
                .doesNotContain("SQLSTATE", "08006", "172.28", "Exception", "at br.");
    }

    // Caso 7 (RNF04)
    @Test
    void nenhumaRespostaContemSenhaOuHash() throws Exception {
        mariaCadastrada();

        for (String corpo : new String[] {
                credenciais(EMAIL, SENHA),
                credenciais(EMAIL, "errada"),
                credenciais("ninguem@aplicacao.example", SENHA),
                credenciais("invalido", SENHA)}) {
            String resposta = enviar(corpo).getResponse().getContentAsString();
            // A palavra "senha" aparece na mensagem do 401; o que não pode existir é o campo.
            assertThat(resposta)
                    .doesNotContain("\"senha\"", "hash_senha", "hashSenha", "argon2", hashMaria, SENHA);
        }
    }

    // Caso 8 (RNF05)
    @ParameterizedTest
    @ValueSource(strings = {
            "x' OR '1'='1@a.com",
            "x'OR'1'='1@a.com",
            "maria@aplicacao.example' --",
            "'; DROP TABLE usuario; --@a.com"
    })
    void tentativaDeInjecaoNuncaAutentica(String email) throws Exception {
        mariaCadastrada();

        int status = enviar(credenciais(email, "qualquer")).getResponse().getStatus();

        assertThat(status).isIn(400, 401);
    }

    @Test
    void emailComInjecaoChegaAoRepositorioComoValorLiteral() throws Exception {
        String email = "x'OR'1'='1@a.com";

        assertThat(enviar(credenciais(email, "qualquer")).getResponse().getStatus()).isEqualTo(401);
        verify(repositorio).buscarPorEmail(email);
    }

    @Test
    void metodoOuContentTypeNaoSuportadosUsamOCorpoDoContrato() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("POST")))
                .andExpect(content().json(CORPO_400, JsonCompareMode.STRICT));
        mockMvc.perform(post(URL).contentType(MediaType.TEXT_PLAIN).content("email=a&senha=b"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().json(CORPO_400, JsonCompareMode.STRICT));

        verifyNoInteractions(repositorio);
    }

    @Test
    void rotaInexistenteDevolve404ComOCorpoDoContrato() throws Exception {
        mockMvc.perform(get("/api/inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(CORPO_400, JsonCompareMode.STRICT));

        verifyNoInteractions(repositorio);
    }
}
