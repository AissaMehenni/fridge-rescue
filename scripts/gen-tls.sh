#!/usr/bin/env bash
# Génère une CA locale + un certificat serveur pour la gateway Istio, puis crée le Secret TLS.
# Les clés restent dans ./certs (ignoré par git). Usage : scripts/gen-tls.sh [host]
set -euo pipefail
HOST="${1:-fridge-rescue.local}"
DIR="$(cd "$(dirname "$0")/.." && pwd)/certs"
mkdir -p "$DIR" && cd "$DIR"

if [[ ! -f ca.key ]]; then
  openssl req -x509 -new -nodes -newkey rsa:4096 -sha256 -days 825 \
    -subj "/O=Fridge Rescue/CN=Fridge Rescue Local CA" -keyout ca.key -out ca.crt
fi

openssl req -new -nodes -newkey rsa:2048 -subj "/O=Fridge Rescue/CN=${HOST}" \
  -keyout tls.key -out tls.csr
cat > tls.ext <<EXT
basicConstraints=CA:FALSE
keyUsage=digitalSignature,keyEncipherment
extendedKeyUsage=serverAuth
subjectAltName=DNS:${HOST},DNS:localhost,IP:127.0.0.1
EXT
openssl x509 -req -in tls.csr -CA ca.crt -CAkey ca.key -CAcreateserial \
  -days 397 -sha256 -extfile tls.ext -out tls.crt
chmod 600 ca.key tls.key

# Le Secret doit être dans le namespace de la gateway (istio-system).
kubectl -n istio-system create secret tls fridge-tls --cert=tls.crt --key=tls.key \
  --dry-run=client -o yaml | kubectl apply -f -
echo "Certificat émis pour ${HOST} (CA : certs/ca.crt)"
