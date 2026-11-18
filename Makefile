# Fridge Rescue — commandes du projet. `make help` pour la liste.
SHELL        := /bin/bash
VERSION      ?= 0.1.0
REGISTRY     ?= aissiuuuu
PLATFORMS    ?= linux/amd64,linux/arm64
NAMESPACE    := fridge-rescue
OVERLAY      ?= k8s/overlays/dev
K8S_VERSION  ?= 1.34.0
SERVICES     := pantry-service
export JAVA_HOME ?= $(shell /usr/libexec/java_home -v 17 2>/dev/null)

.DEFAULT_GOAL := help
.PHONY: help build test images push load deploy validate demo clean

help: ## Affiche cette aide
	@grep -E '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) | awk 'BEGIN{FS=":.*?## "}{printf "  \033[36m%-10s\033[0m %s\n",$$1,$$2}'

build: ## Compile tous les services (sans tests)
	./gradlew assemble

test: ## Lance les tests unitaires et d'intégration
	./gradlew test

images: ## Construit les images locales (architecture de la machine)
	@for s in $(SERVICES); do \
	  docker buildx build -f $$s/Dockerfile -t $(REGISTRY)/fridge-$$s:$(VERSION) --load . || exit 1; done

push: ## Construit et pousse les images multi-arch sur Docker Hub
	@for s in $(SERVICES); do \
	  docker buildx build --platform $(PLATFORMS) -f $$s/Dockerfile -t $(REGISTRY)/fridge-$$s:$(VERSION) --push . || exit 1; done

load: images ## Charge les images locales dans minikube (sans registry)
	@for s in $(SERVICES); do minikube image load $(REGISTRY)/fridge-$$s:$(VERSION); done

validate: ## Valide les manifests Kubernetes (kustomize + kubeconform)
	kubectl kustomize $(OVERLAY) | kubeconform -strict -summary -ignore-missing-schemas -kubernetes-version $(K8S_VERSION) -

deploy: ## Déploie l'overlay sur le cluster courant
	kubectl apply -k $(OVERLAY)
	kubectl -n $(NAMESPACE) rollout status deploy --timeout=300s

demo: ## Affiche l'état du cluster et un appel de démonstration
	kubectl -n $(NAMESPACE) get pods,svc

clean: ## Supprime le namespace et les artefacts de build
	-kubectl delete namespace $(NAMESPACE) --ignore-not-found
	./gradlew clean
