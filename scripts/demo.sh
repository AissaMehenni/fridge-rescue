#!/usr/bin/env bash
# Démo en 3 minutes : état du cluster, parcours fonctionnel, preuves de sécurité.
# Prérequis : `make up` déjà exécuté. Ouvre un port-forward HTTPS temporaire sur la gateway.
set -euo pipefail
NS=fridge-rescue
PORT=${PORT:-8443}
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CURL=(curl -s --cacert "$ROOT/certs/ca.crt" --resolve "fridge-rescue.local:${PORT}:127.0.0.1")
BASE="https://fridge-rescue.local:${PORT}"
step() { printf '\n\033[1;32m▶ %s\033[0m\n' "$*"; }

kubectl -n istio-system port-forward svc/istio-ingressgateway "${PORT}:443" >/dev/null 2>&1 &
PF=$!; trap 'kill $PF 2>/dev/null' EXIT; sleep 3

step "1. Workloads : chaque pod a son sidecar Envoy (2/2)"
kubectl -n $NS get pods -o wide | awk '{print $1, $2, $3}' | column -t

step "2. HTTPS via Istio Gateway → pantry-service (REST)"
"${CURL[@]}" "$BASE/api/items/stats"; echo

step "3. Suggestions : pantry → recipe-service en gRPC (mTLS)"
"${CURL[@]}" "$BASE/api/suggestions?limit=3" | python3 -c "$(cat <<'PY'
import sys, json
d = json.load(sys.stdin)
for s in d["suggestions"]:
    r = s["recipe"]
    rescued = ", ".join(s["rescuedIngredients"]) or "-"
    print(f"  {s['score']:3d}/100  {r['emoji']} {r['title']:28s} sauve : {rescued}")
PY
)"

step "4. mTLS STRICT vérifié par Istio"
istioctl x describe pod "$(kubectl -n $NS get pod -l app=recipe-service -o jsonpath='{.items[0].metadata.name}')" -n $NS | grep -E 'mTLS|DestinationRule' | sort -u

step "5. AuthorizationPolicy : un pod imposteur (label pantry, identité fridge-viewer) est rejeté"
kubectl -n $NS run impostor --rm -i --quiet --restart=Never --image=curlimages/curl:8.23.0 \
  --labels=app=pantry-service --overrides='{"spec":{"serviceAccountName":"fridge-viewer"}}' --command -- sh -c \
  'sleep 4; curl -s -m 5 --http2-prior-knowledge -X POST -H "content-type: application/grpc" --data-binary "" -D - -o /dev/null http://recipe-service:9090/fridgerescue.recipe.v1.RecipeService/ListRecipes | grep -E "grpc-(status|message)"; curl -s -X POST localhost:15020/quitquitquit >/dev/null' 2>/dev/null || true

step "6. NetworkPolicy : un pod hors mesh ne joint pas pantry-service (timeout)"
kubectl -n default run outsider --rm -i --quiet --restart=Never --image=curlimages/curl:8.23.0 --command -- \
  sh -c 'curl -s -m 4 http://pantry-service.fridge-rescue:8080/api/items/stats || echo "bloqué (exit $?)"' 2>/dev/null || true

step "7. RBAC à moindre privilège"
for q in "fridge-viewer list pods" "fridge-viewer get secrets" "fridge-deployer patch deployments"; do
  set -- $q; printf '  %-16s %-6s %-12s → %s\n' "$1" "$2" "$3" "$(kubectl auth can-i "$2" "$3" -n $NS --as="system:serviceaccount:$NS:$1")"
done

step "8. Circuit breaker / mode dégradé : recipe-service arrêté"
kubectl -n $NS scale deploy/recipe-service --replicas=0 >/dev/null
kubectl -n $NS wait --for=delete pod -l app=recipe-service --timeout=60s >/dev/null
"${CURL[@]}" -w '   (%{time_total}s)\n' "$BASE/api/suggestions"
kubectl -n $NS scale deploy/recipe-service --replicas=2 >/dev/null
kubectl -n $NS rollout status deploy/recipe-service --timeout=120s >/dev/null && echo "  recipe-service relancé"

printf '\nInterface : make open puis https://localhost:%s  |  Kiali : make kiali\n' "$PORT"
