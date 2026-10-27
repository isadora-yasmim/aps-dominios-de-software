-- Esquema do banco "autenticacao". Executado pelo superusuário na criação do SGBD.
CREATE TABLE usuario (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(254) NOT NULL UNIQUE,  -- a restrição UNIQUE cria o índice usado pelo planejador
    nome        VARCHAR(150) NOT NULL,
    hash_senha  VARCHAR(255) NOT NULL
);
