# Apêndice III, Seção III.6 – Item 2.3, revisão do Item 2.2 e organização no GitHub

Referência: ANTHROPIC (2026f). Se a revisão do Item 2.2 for citada separadamente, ela precisa de entrada própria.

- Ferramenta: Claude (Anthropic), https://claude.ai
- Acesso: 27 out. 2026
- Usuário: Davi Augusto Ferreira de Carvalho
- Arquivos e fontes anexados: versões do documento (PDF), revisão do Item 2.3 (PDF), definição da APS da docente (PDF) e o repositório https://github.com/isadora-yasmim/aps-dominios-de-software (branch feature/autenticacao)
- Uso: revisão do Item 2.2; rascunho do texto, das Tabelas 8 a 10 e das Figuras 8 a 10 do Item 2.3 e das correções da revisão; organização dos artefatos no GitHub; tudo revisado pelo autor

## Prompts, na ordem

1. "Faça a revisão critica e ortografica do Item 2.2 – Arquitetura da aplicação" e "Mande em docx por favor"
2. "Podemos passar para a parte 2.3 então, acabei trocando com minha colega essa parte, e já foi registrado em uma modificação nova do documento, então não precisa se preoucupar com essa mudança, antes de começar, acha bom ter acesso ao github para desenhar os componentes ou não será nescessario?"
3. "Aqui está a atualização, e aqui está o link do repositorio, que ja tem a feature de autentificação implementada: https://github.com/isadora-yasmim/aps-dominios-de-software/tree/feature/autenticacao"
4. "Sobre o WWW-Autenticate, qual voce sugere?" e "Era nescessario por conta da revisão tambem, verifique ela e veja oque é bom ser feito"
5. "Uma das exigencias academicas do projeto é o seguinte: Todos os artefatos devem estar sob gerência de configuração na ferramenta Github. Recomenda-se usar o workflow de desenvolvimento do Github (Kanban). Me instrua como podemos fazer essa etapa" e "Pode preparar o arquivo da pasta docs e um script para eu rodas"
6. "Oque voce sugere colocar na parte de Apêndice II – Atas e Comunicações? aqui o documento guia da professora" e "Prepare"
7. "Aqui está a ultima versao do documento, verifique o repostiorio novamente https://github.com/isadora-yasmim/aps-dominios-de-software/tree/feature/autenticacao e veja se a implementação está de acordo com o documento"
8. "Para fazer o codigo ser compativel, oque deveria ser mudado?", "Consegue me mandar os arquivos modificados para eu mesmo fazer o commit?", "Como faço para testar se deu tudo certo?" e as perguntas seguintes sobre a configuração do Java
9. "Agora, com essas mudanças, vamos voltar para a parte de Todos os artefatos devem estar sob gerência de configuração na ferramenta Github. Recomenda-se usar o workflow de desenvolvimento do Github (Kanban)."

Uso adicional: modelo do Apêndice II; conferência entre o documento e a implementação; alteração do código para enviar o cabeçalho WWW-Authenticate no 401, com testes; reorganização dos artefatos no GitHub. Tudo revisado e testado pelo autor (mvn test: 37 testes, 0 falhas).
