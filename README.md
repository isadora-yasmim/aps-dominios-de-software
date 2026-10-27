# aps-dominios-de-software

Projeto em trio de um software no domínio de aplicações Web para a disciplina de Domínios de Software – semestre 2026/2.

Aplicação Web de **autenticação de usuários** (RF01). Ela recebe e-mail e senha e devolve a identificação do usuário (id, e-mail, nome) ou um erro de autenticação. É **sem estado**: não cria sessão, não emite token, não envia `Set-Cookie` e não altera dados.

- **Estilo arquitetural:** cliente-servidor; o servidor é um monólito em camadas (um único programa Spring Boot).
- **Ambiente de execução:** navegador → DNS → Nginx (TLS 1.3, porta 443) → Spring Boot/Tomcat (`127.0.0.1:8080`) → PostgreSQL (5432, JDBC/HikariCP).

## Estrutura

```
/
├── backend/                 projeto Spring Boot (Maven)
│   ├── src/main/java/br/ufg/aps/autenticacao/
│   │   ├── controle/        ControladorAutenticacao, TratadorExcecoes        (camada 2)
│   │   ├── servico/         ServicoAutenticacao, VerificadorSenha,
│   │   │                    FalhaAutenticacaoException, ConfiguracaoSenha    (camada 3)
│   │   ├── persistencia/    RepositorioUsuario                               (camada 4)
│   │   ├── modelo/          RequisicaoAutenticacao, UsuarioAutenticado, Usuario, RespostaErro
│   │   └── ferramenta/      GeradorHash (CLI separado da API)
│   ├── src/test/java/...    testes (MockMvc, Mockito, Testcontainers)
│   ├── Dockerfile
│   └── gerar-hash.sh
├── frontend/                index.html, estilo.css, app.js                   (camada 1)
├── nginx/                   nginx.conf, gerar-certificado.sh
├── banco/                   schema.sql, permissoes.sql, seed.sql, pg_hba.conf
├── docker-compose.yml
└── .env.exemplo
```

Cada camada chama apenas a camada imediatamente abaixo: página → `ControladorAutenticacao` → `ServicoAutenticacao` (com o auxiliar `VerificadorSenha`) → `RepositorioUsuario` → SGBD.

## Pré-requisitos

- Docker com o plugin Compose (para o ambiente e para o teste de integração).
- Java 25 e Maven 3.9 (para rodar os testes e o gerador de hash fora do contêiner).
- `openssl` (para o certificado de desenvolvimento).

## Como subir o ambiente

```sh
cp .env.exemplo .env              # troque as senhas no .env
./nginx/gerar-certificado.sh      # cria nginx/tls/aplicacao.crt e .key
docker compose up -d --build
```

O Compose reproduz a topologia do projeto:

| Serviço | Papel | Rede |
|---|---|---|
| `app` | Spring Boot (Tomcat embutido) em `127.0.0.1:8080` | `172.28.0.10`; publica 80 e 443 |
| `nginx` | servidor web; usa `network_mode: "service:app"` (mesma máquina lógica) | a mesma do `app` |
| `postgres` | SGBD; escuta só em `172.28.0.20`; porta 5432 **não** publicada | `172.28.0.20` |

Se as portas 80 ou 443 do host já estiverem em uso, defina `PORTA_HTTP` e/ou `PORTA_HTTPS` no `.env` (por exemplo, `PORTA_HTTP=8081`).

Para parar: `docker compose down`. Os scripts de `banco/` só rodam com o banco vazio; para recriá-lo do zero, use `docker compose down -v`.

### Apontar `aplicacao.example` para a máquina local

Adicione esta linha ao arquivo hosts:

```
127.0.0.1   aplicacao.example
```

- Linux/macOS: `/etc/hosts` (editar com `sudo`).
- Windows: `C:\Windows\System32\drivers\etc\hosts` (editar como administrador).

### Aceitar o certificado autoassinado

Abra `https://aplicacao.example`. O navegador vai avisar que o certificado não é confiável, porque é autoassinado:

- Chrome/Edge: **Avançado → Continuar para aplicacao.example (não seguro)**.
- Firefox: **Avançado → Aceitar o risco e continuar**.

Outra opção é importar `nginx/tls/aplicacao.crt` como autoridade confiável no navegador ou no sistema. Ao gerar um novo certificado, é preciso aceitá-lo de novo.

## Usuários de teste (`banco/seed.sql`)

| E-mail | Senha | Nome |
|---|---|---|
| `maria@aplicacao.example` | `S3nh@Forte` | Maria Souza |
| `joao@aplicacao.example` | `J0ao#Seguro26` | João Pereira |
| `ana@aplicacao.example` | `An@L1ma-2026` | Ana Lima |

## Testar no navegador

Acesse `https://aplicacao.example`, informe um dos usuários acima e clique em **Entrar**:

- credenciais corretas → "Acesso concedido" com nome e e-mail;
- senha errada ou e-mail não cadastrado → "E-mail ou senha inválidos.";
- servidor parado ou SGBD indisponível → mensagem de indisponibilidade.

## Testar com `curl`

`-k` aceita o certificado autoassinado, e `--resolve` dispensa a edição do arquivo hosts.

```sh
API=https://aplicacao.example/api/autenticacao
R="--resolve aplicacao.example:443:127.0.0.1"

# 200 OK
curl -k $R -i -X POST $API -H 'Content-Type: application/json' \
  -d '{"email": "maria@aplicacao.example", "senha": "S3nh@Forte"}'
# {"id":1,"email":"maria@aplicacao.example","nome":"Maria Souza"}

# 401 Unauthorized (senha incorreta; cabeçalho e corpo iguais para e-mail inexistente)
# WWW-Authenticate: Credenciais realm="aplicacao.example"
curl -k $R -i -X POST $API -H 'Content-Type: application/json' \
  -d '{"email": "maria@aplicacao.example", "senha": "errada"}'
# {"mensagem":"E-mail ou senha inválidos."}

# 400 Bad Request (JSON malformado, campo ausente/vazio ou e-mail inválido)
curl -k $R -i -X POST $API -H 'Content-Type: application/json' -d '{"email": "maria"'
curl -k $R -i -X POST $API -H 'Content-Type: application/json' \
  -d '{"email": "nao-e-email", "senha": "x"}'
# {"mensagem":"Requisição inválida."}

# 301 na porta 80
curl -i -H 'Host: aplicacao.example' http://127.0.0.1/api/autenticacao
```

## Testes automatizados

```sh
cd backend
mvn test
```

O teste de integração (`AutenticacaoIntegracaoTest`) sobe um PostgreSQL 18 com Testcontainers, usando os mesmos `schema.sql`, `permissoes.sql` e `seed.sql`, e conecta como `app_autenticacao`. Sem Docker disponível, esse teste é pulado e os demais rodam normalmente.

| # | Caso exigido | Teste |
|---|---|---|
| 1 | Credenciais válidas → 200 só com `id`, `email`, `nome` | `ControladorAutenticacaoTest.credenciaisValidasDevolvem200SoComIdEmailENome`; `AutenticacaoIntegracaoTest.usuarioDoSeedAutenticaComASenhaCorreta` |
| 2 | Senha incorreta → 401 com `WWW-Authenticate` | `ControladorAutenticacaoTest.senhaIncorretaDevolve401ComMensagemGenerica` |
| 3 | Inexistente → 401 com corpo idêntico (RNF03) | `ControladorAutenticacaoTest.usuarioInexistenteDevolveCorpoIdenticoAoDeSenhaIncorreta`; `AutenticacaoIntegracaoTest.senhaIncorretaEUsuarioInexistenteTemRespostasIdenticas` |
| 4 | Inexistente → verificação contra o hash fictício (RNF06) | `ServicoAutenticacaoTest.usuarioInexistenteVerificaASenhaContraOHashFicticio`; `VerificadorSenhaTest.verificacaoDeUsuarioInexistenteUsaOHashFicticio` |
| 5 | Requisição inválida → 400 sem chamar o repositório (FA1) | `ControladorAutenticacaoTest.requisicaoInvalidaDevolve400SemConsultarORepositorio` (13 variações) |
| 6 | Falha no banco → 500 sem detalhes (FE1) | `ControladorAutenticacaoTest.falhaNoBancoDevolve500SemDetalhesInternos` |
| 7 | Nenhuma resposta contém senha ou hash (RNF04) | `ControladorAutenticacaoTest.nenhumaRespostaContemSenhaOuHash` |
| 8 | Injeção de SQL no e-mail → 400 ou 401, nunca 200 (RNF05) | `ControladorAutenticacaoTest.tentativaDeInjecaoNuncaAutentica`, `...emailComInjecaoChegaAoRepositorioComoValorLiteral`; `AutenticacaoIntegracaoTest.injecaoDeSqlNoEmailNaoEncontraNenhumUsuario` |
| 9 | Hash Argon2id; verificador aceita a correta e rejeita a incorreta (RNF02) | `VerificadorSenhaTest.hashGeradoEArgon2idEVerificadorReconheceSoASenhaCorreta`; `AutenticacaoIntegracaoTest.hashesArmazenadosSaoArgon2id` |

Testes adicionais: `usuarioDeBancoDaAplicacaoNaoAlteraDados` (o usuário de banco não consegue `UPDATE`/`INSERT`) `metodoOuContentTypeNaoSuportadosUsamOCorpoDoContrato` (inclui o cabeçalho `Allow` do 405) e `rotaInexistenteDevolve404ComOCorpoDoContrato`.

## Gerar o hash de uma senha (fora da API)

O cadastro está fora do escopo: os usuários são inseridos por meio externo (`seed.sql`). Para montar o seed, use o utilitário `GeradorHash`, que é uma classe `main` separada, não inicia o Spring e não expõe endpoint:

```sh
cd backend
mvn -q package -DskipTests
./gerar-hash.sh                 # pede a senha sem ecoar no terminal
./gerar-hash.sh 'S3nh@Forte'    # ou recebe a senha como argumento
# $argon2id$v=19$m=19456,t=2,p=1$...
```

Com o ambiente no ar, também funciona dentro do contêiner:

```sh
docker compose exec app java -cp /app/app.jar \
  -Dloader.main=br.ufg.aps.autenticacao.ferramenta.GeradorHash \
  org.springframework.boot.loader.launch.PropertiesLauncher 'S3nh@Forte'
```

## Administração do SGBD

Só o superusuário `postgres`, pelo socket local do contêiner, tem acesso administrativo:

```sh
docker compose exec -u postgres postgres psql -d autenticacao
```

- `app_autenticacao` tem apenas `CONNECT` no banco `autenticacao` e `SELECT` na tabela `usuario` (`banco/permissoes.sql`).
- `listen_addresses=172.28.0.20` (endereço da rede interna, não `*`).
- `banco/pg_hba.conf` aceita pela rede apenas `172.28.0.10/32` (o servidor da aplicação), com `app_autenticacao`, no banco `autenticacao`, via `scram-sha-256`. Qualquer outra conexão é recusada.

## Rastreabilidade

| Requisito | Como é atendido | Arquivos / classes |
|---|---|---|
| **RF01.1** Entrada | A página envia `POST /api/autenticacao` com JSON. O controlador converte o JSON e valida `@NotBlank`/`@Email`. Se a requisição for inválida, responde 400 sem chamar o serviço. | `frontend/app.js`; `ControladorAutenticacao`; `RequisicaoAutenticacao`; `TratadorExcecoes` (`HttpMessageNotReadableException`, `MethodArgumentNotValidException`) |
| **RF01.2** Existência | O serviço pede ao repositório o usuário com o e-mail recebido. | `ServicoAutenticacao`; `RepositorioUsuario.buscarPorEmail`; `banco/schema.sql` (UNIQUE → índice) |
| **RF01.3** Comparação | A senha recebida é comparada com `hash_senha` pelo Argon2id, nunca em texto. | `VerificadorSenha.confere`; `ConfiguracaoSenha` |
| **RF01.4** Decisão | Se a senha confere, devolve 200 com id, e-mail e nome; senão, lança `FalhaAutenticacaoException`, convertida em 401. | `ServicoAutenticacao`; `UsuarioAutenticado`; `FalhaAutenticacaoException`; `TratadorExcecoes` |
| **RNF01** | Requisito da camada de apresentação: página de login simples e responsiva, mensagens para cada status, servida por HTTPS (TLS 1.3, HSTS) pelo Nginx. | `frontend/index.html`, `estilo.css`, `app.js`; `nginx/nginx.conf` |
| **RNF02** Hash de senha | Argon2id (m=19 MiB, t=2, p=1) via `spring-security-crypto` + Bouncy Castle. O seed só contém hashes. | `ConfiguracaoSenha`; `VerificadorSenha`; `GeradorHash`; `banco/seed.sql` |
| **RNF03** Mensagem genérica | Uma única exceção para "não existe" e "senha incorreta", e uma única resposta 401, com o mesmo cabeçalho `WWW-Authenticate: Credenciais realm="aplicacao.example"`. | `FalhaAutenticacaoException`; `TratadorExcecoes` |
| **RNF04** Sem senha/hash nas respostas | O DTO de sucesso tem só 3 campos. `Usuario` nunca é serializado e o `toString` oculta o hash. O 500 é genérico e o `server.error.include-*` está desligado. | `UsuarioAutenticado`; `Usuario`; `TratadorExcecoes`; `application.properties` |
| **RNF05** SQL seguro | Consulta parametrizada única via `JdbcTemplate`. O usuário de banco tem só leitura e o SGBD só aceita a aplicação. | `RepositorioUsuario`; `banco/permissoes.sql`; `banco/pg_hba.conf` |
| **RNF06** Tempo semelhante | Se o usuário não existe, a senha é verificada contra um hash fictício gerado na inicialização (mesmo algoritmo e parâmetros) e o resultado é descartado. | `VerificadorSenha.verificarContraHashFicticio`; `ServicoAutenticacao` |

## Decisões tomadas onde a especificação era omissa

1. **Versões:** Java 25 (LTS instalada), Spring Boot 4.1.2 (última estável; a 4.2 ainda era RC), Bouncy Castle `bcprov-jdk18on` 1.86, PostgreSQL 18 (`postgres:18-alpine`), Nginx `nginx:stable-alpine`. As demais versões são as gerenciadas pelo Spring Boot.
2. **Parâmetros do Argon2id:** os mínimos recomendados pela OWASP (memória 19 MiB, 2 iterações, paralelismo 1, sal de 16 bytes, hash de 32 bytes). O hash fictício usa o mesmo codificador.
3. **Comparação do e-mail:** exata, como recebido (sem `trim` nem minúsculas), pois a consulta é `WHERE email = ?`. O seed usa e-mails em minúsculas.
4. **Formato de e-mail:** vale o `@Email` padrão do Hibernate Validator, que aceita, por exemplo, `a@b` sem TLD. Não há limite de tamanho além do que o banco já impõe.
5. **Campos extras no JSON** são ignorados (padrão do Jackson no Spring Boot).
6. **Erros do próprio Spring MVC** (método diferente de POST → 405, Content-Type diferente de JSON → 415, rota `/api/...` inexistente → 404): mantêm o status, com o corpo `{"mensagem": "Requisição inválida."}` e sem consultar o SGBD. Assim, nenhum erro da API cai na rota `/error`.
7. **Cabeçalho do 401:** a RFC 9110 exige `WWW-Authenticate` em toda resposta 401. Como as credenciais vão no corpo JSON, o desafio usa um esquema próprio, `Credenciais realm="aplicacao.example"`; o esquema `Basic` faria o navegador abrir a janela nativa de usuário e senha.
8. **Log do 500:** o stack trace vai só para o log do servidor; a resposta é genérica.
9. **Somente leitura reforçada:** `spring.datasource.hikari.read-only=true` e `spring.sql.init.mode=never` (a aplicação nunca executa scripts no banco).
10. **Gerador de hash:** classe `GeradorHash` no mesmo projeto Maven (reaproveita `ConfiguracaoSenha`, garantindo os mesmos parâmetros), executada pelo script `gerar-hash.sh` a partir do jar, sem iniciar o Spring.
11. **Nomes do banco:** banco `autenticacao`, usuário `app_autenticacao`. As senhas ficam no `.env` (não versionado). `permissoes.sql` lê a senha da aplicação com `\getenv DB_SENHA_APP`.
12. **Acesso administrativo:** além da regra da aplicação, o `pg_hba.conf` tem `local all postgres peer` (socket Unix dentro do contêiner, sem rede), necessária para os scripts de inicialização e para a administração. Todas as outras conexões são `reject`. O `USAGE` no esquema `public` vem do padrão do PostgreSQL 15+ para `PUBLIC`; a aplicação recebe explicitamente só `CONNECT` e `SELECT`.
13. **Endereços fixos** na rede interna `172.28.0.0/24` (app `.10`, postgres `.20`), para que `listen_addresses` e `pg_hba.conf` possam nomear endereços. Se a sub-rede conflitar com outra rede local, ajuste `docker-compose.yml`, `pg_hba.conf` e `DB_URL`.
14. **Portas do host configuráveis** (`PORTA_HTTP`, `PORTA_HTTPS`; padrão 80 e 443). Dentro do contêiner o Nginx continua em 80 e 443.
15. **Certificado de desenvolvimento:** autoassinado, chave EC P-256, validade de 365 dias, com SAN `DNS:aplicacao.example`.
16. **`nginx.conf`** contém apenas os blocos `server` especificados e é montado como `/etc/nginx/conf.d/default.conf`.
17. **Imagem do backend:** construída sem rodar os testes (`-DskipTests`), pois o teste de integração precisa de Docker. Os testes rodam com `mvn test`. O processo roda como usuário sem privilégios.
18. **Página de login:** mantém a validação nativa do navegador (`required`, `type="email"`). O formulário usa `method="post"` para que a senha nunca vá para a URL, mesmo sem JavaScript. Para 401, mostra a mensagem recebida; para 400, "Verifique o e-mail e a senha informados."; para 500, 502, 504, qualquer outro status ou erro de rede, "Serviço indisponível no momento. Tente novamente mais tarde.". Nada é guardado em `localStorage`, `sessionStorage` ou cookies.
19. **Seed** com 3 usuários fictícios no domínio `aplicacao.example`. Os ids são gerados pelo `BIGSERIAL` (Maria recebe `id` 1).
20. **Dados do SGBD** ficam no volume anônimo do contêiner. `docker compose down -v` recria o banco a partir dos scripts.
21. **502/504 em desenvolvimento:** o Nginx compartilha a pilha de rede do contêiner `app`, cujo processo principal é o Java. Parar o Spring Boot (`docker compose stop app`) também desliga as portas publicadas, então do host a conexão simplesmente falha. Para ver o 502, chame o Nginx por dentro do contêiner dele:
    `docker compose exec nginx wget -S -O - --no-check-certificate --header='Host: aplicacao.example' --header='Content-Type: application/json' --post-data='{}' https://127.0.0.1/api/autenticacao` → `502 Bad Gateway`.
    Para voltar ao normal: `docker compose up -d --force-recreate`.
