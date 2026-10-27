#!/usr/bin/env sh
# Gera o hash Argon2id de uma senha (para montar banco/seed.sql).
# Requer o jar construído antes: (cd backend && mvn -q package -DskipTests)
# Uso: ./gerar-hash.sh            -> pede a senha sem ecoar
#      ./gerar-hash.sh 'S3nh@Forte'
set -eu
JAR="$(dirname "$0")/target/autenticacao.jar"
exec java -cp "$JAR" \
  -Dloader.main=br.ufg.aps.autenticacao.ferramenta.GeradorHash \
  org.springframework.boot.loader.launch.PropertiesLauncher "$@"
