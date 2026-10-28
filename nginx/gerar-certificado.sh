#!/usr/bin/env sh
# Gera um certificado autoassinado para aplicacao.example (somente desenvolvimento).
# Saída: nginx/tls/aplicacao.crt e nginx/tls/aplicacao.key (montados em /etc/nginx/tls).
set -eu
DIR="$(cd "$(dirname "$0")" && pwd)/tls"
mkdir -p "$DIR"

openssl req -x509 -nodes -days 365 \
  -newkey ec -pkeyopt ec_paramgen_curve:prime256v1 \
  -subj "/CN=aplicacao.example" \
  -addext "subjectAltName=DNS:aplicacao.example" \
  -keyout "$DIR/aplicacao.key" \
  -out "$DIR/aplicacao.crt"

chmod 644 "$DIR/aplicacao.crt"
chmod 600 "$DIR/aplicacao.key"
echo "Certificado gerado em $DIR"
