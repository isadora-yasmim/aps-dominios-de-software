'use strict';

const MENSAGEM_400 = 'Verifique o e-mail e a senha informados.';
const MENSAGEM_401_PADRAO = 'E-mail ou senha inválidos.';
const MENSAGEM_INDISPONIVEL = 'Serviço indisponível no momento. Tente novamente mais tarde.';

const formulario = document.getElementById('formulario-login');
const campoEmail = document.getElementById('email');
const campoSenha = document.getElementById('senha');
const botao = document.getElementById('botao-entrar');
const resultado = document.getElementById('resultado');

// Usa textContent (nunca innerHTML) para que dados vindos do servidor não virem HTML.
function exibir(tipo, linhas) {
    resultado.replaceChildren(...linhas.map((texto) => {
        const p = document.createElement('p');
        p.textContent = texto;
        return p;
    }));
    resultado.className = 'resultado ' + tipo;
    resultado.hidden = false;
}

async function lerMensagem(resposta, padrao) {
    try {
        const corpo = await resposta.json();
        return corpo && typeof corpo.mensagem === 'string' ? corpo.mensagem : padrao;
    } catch {
        return padrao;
    }
}

formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();

    const email = campoEmail.value;
    const senha = campoSenha.value;

    botao.disabled = true;
    resultado.hidden = true;

    try {
        const resposta = await fetch('/api/autenticacao', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, senha }),
        });

        // fetch não trata 4xx/5xx como falha: o status é verificado explicitamente.
        if (resposta.status === 200) {
            const usuario = await resposta.json();
            exibir('sucesso', ['Acesso concedido', `${usuario.nome} (${usuario.email})`]);
        } else if (resposta.status === 401) {
            exibir('erro', [await lerMensagem(resposta, MENSAGEM_401_PADRAO)]);
        } else if (resposta.status === 400) {
            exibir('erro', [MENSAGEM_400]);
        } else {
            exibir('erro', [MENSAGEM_INDISPONIVEL]);
        }
    } catch {
        // Erro de rede ou resposta 200 com corpo ilegível.
        exibir('erro', [MENSAGEM_INDISPONIVEL]);
    } finally {
        botao.disabled = false;
    }
});
