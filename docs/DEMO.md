# Démo en 3 minutes

## Avant la soutenance (5 min, une seule fois)

```bash
make up                 # cluster prêt (idempotent)
make demo > /dev/null   # préchauffe les images curl utilisées par la démo
```

Ouvrir trois terminaux et un navigateur :

| Terminal | Commande |
|---|---|
| T1 | `make open` (gateway sur https://localhost:8443) |
| T2 | `make kiali` (graphe du mesh) |
| T3 | les commandes ci-dessous |

## Déroulé

| Temps | Ce qu'on montre | Commande / action | Ce qu'on dit |
|---|---|---|---|
| 0:00 | Le produit | Navigateur → https://localhost:8443 | « 3 aliments à sauver avant demain. La frise place chaque aliment selon les jours qui lui restent ; les recettes sauvent d'abord ceux de gauche. » |
| 0:30 | Ajout d'un aliment | Formulaire : *Courgette*, date = demain, **Ajouter au frigo** | La courgette apparaît dans *Demain* et le classement des recettes change : REST → PostgreSQL → gRPC. |
| 0:50 | Architecture | `kubectl -n fridge-rescue get pods` | 4 workloads, chacun `2/2` : l'application et son sidecar Envoy. PostgreSQL en StatefulSet avec PVC. |
| 1:05 | Service mesh | Kiali → *Traffic Graph*, namespace `fridge-rescue` | gateway → frontend / pantry → recipe (gRPC) et postgres, cadenas = mTLS. |
| 1:25 | Sécurité | `make demo` (étapes 4 à 7 défilent) | mTLS STRICT ; imposteur refusé (`grpc-status 7`) ; pod hors mesh bloqué par NetworkPolicy ; RBAC : viewer ne lit pas les secrets. |
| 2:15 | Résilience | (étape 8 du script) | recipe-service coupé : réponse `available:false` en 15 ms, l'interface reste utilisable ; relance automatique. |
| 2:35 | Industrialisation | Onglet GitHub Actions + `docs/ARCHITECTURE.md` | CI : tests, kubeconform, scan trivy, images multi-arch sur tag. Overlay cloud prêt (HPA, PDB). |
| 3:00 | Fin | | |

## Commandes de secours

```bash
# Le front ne répond pas : relancer le port-forward
make open

# Montrer le contrat gRPC
cat recipe-contract/src/main/proto/fridgerescue/recipe/v1/recipe.proto

# Prouver HTTPS + redirection
curl -sI http://localhost:8080/ | head -3
curl -s --cacert certs/ca.crt --resolve fridge-rescue.local:8443:127.0.0.1 \
  https://fridge-rescue.local:8443/api/items/stats

# Persistance : tuer tous les pods, les données restent
kubectl -n fridge-rescue delete pod -l 'app in (pantry-service,postgres)'
```

Remarque : le script de démo arrête puis relance `recipe-service` (étape 8). Il est idempotent et peut
être rejoué.
