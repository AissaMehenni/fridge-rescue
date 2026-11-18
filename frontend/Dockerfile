# syntax=docker/dockerfile:1
# Contexte : dossier frontend/ (docker buildx build frontend)

# --- Build statique (indépendant de l'architecture cible)
FROM --platform=$BUILDPLATFORM node:24.22.3-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm test && npm run build

# --- Service : nginx non root
FROM nginxinc/nginx-unprivileged:1.30.6-alpine
# Correctifs de sécurité Alpine publiés après l'image de base (scan trivy).
USER root
RUN apk upgrade --no-cache
COPY security-headers.conf /etc/nginx/snippets/security-headers.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
USER 101
EXPOSE 8080
