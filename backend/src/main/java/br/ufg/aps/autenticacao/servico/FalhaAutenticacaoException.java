package br.ufg.aps.autenticacao.servico;

/**
 * Credenciais não conferem. É a mesma exceção para usuário inexistente e para
 * senha incorreta, de modo que a camada de controle não consegue distingui-los (RNF03).
 */
public class FalhaAutenticacaoException extends RuntimeException {

    public FalhaAutenticacaoException() {
        super("Falha de autenticação");
    }
}
