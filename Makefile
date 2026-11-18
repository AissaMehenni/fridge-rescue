# Fridge Rescue — commandes du projet. `make help` pour la liste.
SHELL        := /bin/bash
VERSION      ?= 0.4.0
FRONT_VERSION ?= 0.5.0
REGISTRY     ?= aissiuuuu
PLATFORMS    ?= linux/amd64,linux/arm64
NAMESPACE    := fridge-rescue
OVERLAY      ?= k8s/overlays/dev
K8S_VERSION  ?= 1.34.0
SERVICES     := pantry-service recipe-service
MINIKUBE_MEMORY ?= 8192   # baisser (ex. 7168) si Docker Desktop dispose de moins de 9 Go
CRD_SCHEMAS  := https://raw.githubusercontent.com/datreeio/CRDs-catalog/main/{{.Group}}/{{.ResourceKind}}_{{.ResourceAPIVersion}}.json
export JAVA_HOME ?= $(shell /usr/libexec/java_home -v 17 2>/dev/null)

.DEFAULT_GOAL := help
.PHONY: up cluster open kiali tls scan secrets help build test images push load deploy validate demo clean istio observability

help: ## Affiche cette aide
	@grep -E '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) | awk 'BEGIN{FS=":.*?## "}{printf "  \033[36m%-10s\033[0m %s\n",$$1,$$2}'

up: cluster istio observability tls load deploy ## Tout depuis zéro : minikube, Istio, images, déploiement

cluster: ## Démarre minikube (driver docker, 6 CPU)
	minikube status >/dev/null 2>&1 || minikube start --driver=docker --cpus=6 --memory=$(MINIKUBE_MEMORY)

open: ## Expose la gateway sur https://localhost:8443 (Ctrl+C pour arrêter)
	@echo "→ https://localhost:8443  (CA locale : certs/ca.crt)"
	kubectl -n istio-system port-forward svc/istio-ingressgateway 8443:443 8080:80

kiali: ## Ouvre le graphe du service mesh dans Kiali
	istioctl dashboard kiali

build: ## Compile tous les services (sans tests)
	./gradlew assemble

test: ## Lance les tests (Java + front)
	./gradlew test
	cd frontend && npm ci --no-audit --no-fund && npm test

images: ## Construit les images locales (architecture de la machine)
	@for s in $(SERVICES); do \
	  docker buildx build -f $$s/Dockerfile -t $(REGISTRY)/fridge-$$s:$(VERSION) --load . || exit 1; done
	docker buildx build -t $(REGISTRY)/fridge-frontend:$(FRONT_VERSION) --load frontend

push: ## Construit et pousse les images multi-arch sur Docker Hub
	@for s in $(SERVICES); do \
	  docker buildx build --platform $(PLATFORMS) -f $$s/Dockerfile -t $(REGISTRY)/fridge-$$s:$(VERSION) --push . || exit 1; done
	docker buildx build --platform $(PLATFORMS) -t $(REGISTRY)/fridge-frontend:$(FRONT_VERSION) --push frontend

load: images ## Charge les images locales dans minikube (sans registry)
	@for s in $(SERVICES); do minikube image load --overwrite=true $(REGISTRY)/fridge-$$s:$(VERSION); done
	minikube image load --overwrite=true $(REGISTRY)/fridge-frontend:$(FRONT_VERSION)

secrets: ## Génère le fichier local des identifiants PostgreSQL (non versionné)
	@f=$(OVERLAY)/db-credentials.env; if [ -f $$f ]; then echo "$$f existe déjà"; else \
	  printf 'DB_USER=pantry\nDB_PASSWORD=%s\n' "$$(openssl rand -base64 24 | tr -d '/+=')" > $$f && chmod 600 $$f && echo "$$f créé"; fi

tls: ## Génère le certificat auto-signé et le Secret TLS de la gateway
	scripts/gen-tls.sh fridge-rescue.local

scan: ## Scanne les images (trivy) : échoue sur HIGH/CRITICAL corrigeables
	@for i in $(foreach s,$(SERVICES),$(REGISTRY)/fridge-$(s):$(VERSION)) $(REGISTRY)/fridge-frontend:$(FRONT_VERSION); do \
	  trivy image --quiet --scanners vuln --severity HIGH,CRITICAL --ignore-unfixed \
	    --ignorefile .trivyignore --exit-code 1 $$i || exit 1; done

validate: secrets ## Valide les manifests Kubernetes (kustomize + kubeconform)
	kubectl kustomize $(OVERLAY) | kubeconform -strict -summary \
	  -schema-location default -schema-location '$(CRD_SCHEMAS)' -kubernetes-version $(K8S_VERSION) -

ISTIO_ADDONS := https://raw.githubusercontent.com/istio/istio/release-1.32/samples/addons

istio: ## Installe Istio (profil demo) sur le cluster courant
	istioctl install -f infra/istio/istio-config.yaml -y

observability: ## Installe Prometheus, Grafana et Kiali (add-ons Istio 1.32)
	@for a in prometheus grafana kiali; do kubectl apply -f $(ISTIO_ADDONS)/$$a.yaml; done
	kubectl -n istio-system rollout status deploy/kiali --timeout=300s

deploy: secrets ## Déploie l'overlay sur le cluster courant
	kubectl apply -k $(OVERLAY)
	kubectl -n $(NAMESPACE) rollout status deploy --timeout=300s

demo: ## Script de démo (3 min) : parcours fonctionnel + preuves de sécurité
	scripts/demo.sh

clean: ## Supprime le namespace et les artefacts de build (le cluster est conservé)
	-kubectl delete namespace $(NAMESPACE) --ignore-not-found
	./gradlew clean
	rm -rf frontend/dist
