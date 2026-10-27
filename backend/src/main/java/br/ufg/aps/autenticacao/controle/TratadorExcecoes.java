package br.ufg.aps.autenticacao.controle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.ufg.aps.autenticacao.modelo.RespostaErro;
import br.ufg.aps.autenticacao.servico.FalhaAutenticacaoException;

/**
 * Traduz exceções em respostas HTTP. É o único lugar onde os códigos de status de erro
 * são definidos, e nenhuma resposta expõe detalhes internos (RF01.4; RNF03; RNF04).
 */
@RestControllerAdvice
public class TratadorExcecoes {

    static final String MENSAGEM_401 = "E-mail ou senha inválidos.";
    static final String MENSAGEM_400 = "Requisição inválida.";
    static final String MENSAGEM_500 = "Erro interno. Tente novamente mais tarde.";

    private static final Logger log = LoggerFactory.getLogger(TratadorExcecoes.class);

    /** JSON malformado ou corpo ausente. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespostaErro> jsonInvalido(HttpMessageNotReadableException e) {
        return resposta(HttpStatus.BAD_REQUEST, MENSAGEM_400);
    }

    /** Campo ausente, vazio ou e-mail fora do formato. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespostaErro> validacao(MethodArgumentNotValidException e) {
        return resposta(HttpStatus.BAD_REQUEST, MENSAGEM_400);
    }

    /** Usuário inexistente ou senha incorreta: mesma resposta nos dois casos. */
    @ExceptionHandler(FalhaAutenticacaoException.class)
    public ResponseEntity<RespostaErro> falhaAutenticacao(FalhaAutenticacaoException e) {
        return resposta(HttpStatus.UNAUTHORIZED, MENSAGEM_401);
    }

    /** Qualquer outra falha, inclusive DataAccessException (SGBD indisponível). */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespostaErro> erroInesperado(Exception e) {
        // Erros do próprio Spring MVC causados pelo cliente (rota inexistente, método ou
        // Content-Type não suportados) mantêm o status 4xx, com o corpo do contrato.
        if (e instanceof ErrorResponse erro && erro.getStatusCode().is4xxClientError()) {
            return ResponseEntity.status(erro.getStatusCode())
                    .headers(erro.getHeaders())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new RespostaErro(MENSAGEM_400));
        }
        log.error("Falha inesperada ao processar a requisição", e);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, MENSAGEM_500);
    }

    private static ResponseEntity<RespostaErro> resposta(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RespostaErro(mensagem));
    }
}
