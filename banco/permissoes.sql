-- Usuário de banco próprio da aplicação: apenas CONNECT no banco e SELECT na tabela usuario.
-- A senha vem da variável de ambiente DB_SENHA_APP (nunca fica neste arquivo).
\getenv senha_app DB_SENHA_APP

CREATE ROLE app_autenticacao LOGIN PASSWORD :'senha_app';

-- Ninguém além do dono e da aplicação conecta ao banco.
REVOKE ALL ON DATABASE autenticacao FROM PUBLIC;
GRANT CONNECT ON DATABASE autenticacao TO app_autenticacao;

-- Nenhum privilégio de escrita: somente leitura da tabela usuario.
REVOKE ALL ON TABLE usuario FROM PUBLIC;
GRANT SELECT ON TABLE usuario TO app_autenticacao;
