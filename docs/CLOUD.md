# Déploiement cloud (GKE, variante AKS)

> **Statut : préparé, non déployé.** Un cluster managé est facturé à l'heure. Estimez le coût avec le
> [calculateur Google Cloud](https://cloud.google.com/products/calculator) avant de lancer quoi que ce
> soit, et supprimez le cluster après la démo.

L'overlay [`k8s/overlays/cloud`](../k8s/overlays/cloud) réutilise exactement la base de minikube et ajoute :

| Ajout | Pourquoi |
|---|---|
| `HorizontalPodAutoscaler` pantry / recipe (2 → 6, CPU 70 %) | Absorber la charge sans surdimensionner |
| `PodDisruptionBudget` (minAvailable 1) | Pas de coupure pendant les mises à jour de nœuds |
| `topologySpreadConstraints` par zone | Survivre à la perte d'une zone |
| PVC `standard-rwo`, 10 Gi | Disque persistant managé (AKS : `managed-csi`) |
| Ressources pantry augmentées | Pool JDBC et JIT plus confortables |

## 1. Prérequis

- `gcloud` authentifié (`gcloud auth login`) et un projet avec facturation activée
- images publiées sur Docker Hub (`git tag v0.4.0 && git push --tags` déclenche la CI)

## 2. Cluster GKE Standard

GKE Standard plutôt qu'Autopilot : l'injection de sidecar Istio « open source » a besoin du conteneur
`istio-init` (`NET_ADMIN`). Dataplane V2 applique les NetworkPolicy.

```bash
export PROJECT_ID=<votre-projet> REGION=europe-west9
gcloud config set project "$PROJECT_ID"
gcloud services enable container.googleapis.com

gcloud container clusters create fridge-rescue \
  --region "$REGION" --num-nodes 1 --machine-type e2-standard-4 \
  --enable-dataplane-v2 --release-channel regular \
  --workload-pool "$PROJECT_ID.svc.id.goog"

gcloud container clusters get-credentials fridge-rescue --region "$REGION"
```

## 3. Istio, TLS, secrets

```bash
istioctl install -f infra/istio/istio-config.yaml -y
make observability

# Certificat : auto-signé (démo) ou cert-manager + Let's Encrypt pour un vrai domaine
scripts/gen-tls.sh fridge.example.com

# Identifiants PostgreSQL depuis Secret Manager, jamais commités
gcloud secrets versions access latest --secret=pantry-db-credentials \
  > k8s/overlays/cloud/db-credentials.env
```

## 4. Déploiement

```bash
make validate OVERLAY=k8s/overlays/cloud
kubectl apply -k k8s/overlays/cloud
kubectl -n fridge-rescue rollout status deploy --timeout=600s

# IP publique de la gateway (LoadBalancer)
kubectl -n istio-system get svc istio-ingressgateway \
  -o jsonpath='{.status.loadBalancer.ingress[0].ip}'
```

Pointez ensuite un enregistrement DNS A vers cette IP, ou testez avec
`curl --resolve fridge.example.com:443:<IP> --cacert certs/ca.crt https://fridge.example.com/`.

## 5. Nettoyage (obligatoire)

```bash
gcloud container clusters delete fridge-rescue --region "$REGION"
```

## Variante AKS

```bash
az group create -n fridge-rescue -l francecentral
az aks create -g fridge-rescue -n fridge-rescue --node-count 3 \
  --node-vm-size Standard_D4s_v5 --network-plugin azure --network-policy cilium \
  --network-dataplane cilium --generate-ssh-keys
az aks get-credentials -g fridge-rescue -n fridge-rescue
# Remplacer storageClassName par managed-csi dans patches/postgres-storage.yaml, puis étapes 3 et 4.
az group delete -n fridge-rescue   # nettoyage
```

## Pistes pour aller plus loin

- **Registry privé** (Artifact Registry / ACR) avec `imagePullSecrets` ou Workload Identity, et images
  référencées par digest dans l'overlay.
- **Signature des images** (cosign) et vérification à l'admission.
- **External Secrets Operator** pour synchroniser Secret Manager vers Kubernetes.
- **Base managée** (Cloud SQL / Azure Database for PostgreSQL) à la place du StatefulSet.
