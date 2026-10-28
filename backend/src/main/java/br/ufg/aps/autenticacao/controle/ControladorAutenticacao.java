package br.ufg.aps.autenticacao.controle;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import br.ufg.aps.autenticacao.modelo.RequisicaoAutenticacao;
import br.ufg.aps.autenticacao.modelo.UsuarioAutenticado;
import br.ufg.aps.autenticacao.servico.ServicoAutenticacao;
import jakarta.validation.Valid;

/**
 * Recebe {@code POST /api/autenticacao} (RF01.1). JSON inválido ou credenciais fora do
 * formato não chegam ao serviço: são convertidos em 400 pelo {@link TratadorExcecoes}.
 */
@RestController
public class ControladorAutenticacao {

    private final ServicoAutenticacao servico;

    public ControladorAutenticacao(ServicoAutenticacao servico) {
        this.servico = servico;
    }

    @PostMapping(path = "/api/autenticacao",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public UsuarioAutenticado autenticar(@Valid @RequestBody RequisicaoAutenticacao requisicao) {
        return servico.autenticar(requisicao.email(), requisicao.senha());
    }
}
