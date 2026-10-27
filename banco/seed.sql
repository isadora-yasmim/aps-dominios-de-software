-- Carga de usuários fictícios para testes. O cadastro está fora do escopo da aplicação.
-- Hashes Argon2id gerados com backend/gerar-hash.sh (senhas de teste listadas no README).
INSERT INTO usuario (email, nome, hash_senha) VALUES
    ('maria@aplicacao.example', 'Maria Souza',
     '$argon2id$v=19$m=19456,t=2,p=1$A3w1guQbdcnPFwCwUb0Tbw$1Iu08O6Fgq8/QWTTKNNTPHkWtiqrGxYPJTXkrKQ5HOM'),
    ('joao@aplicacao.example', 'João Pereira',
     '$argon2id$v=19$m=19456,t=2,p=1$uVdUp0r30MotgeS2cMnuDg$I/38renCoRcZ2CNSOQtZQ55giPIMXIK5FRcbNKw1dV8'),
    ('ana@aplicacao.example', 'Ana Lima',
     '$argon2id$v=19$m=19456,t=2,p=1$vdpEDrvQ1RGSdr+GCV+xEg$25LdHJwgkvkTXiNMc8dze97g9qEj4rlPIvW+7rSUFfg');
