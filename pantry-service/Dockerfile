# syntax=docker/dockerfile:1
# Contexte de build : racine du dépôt (docker buildx build -f pantry-service/Dockerfile .)

# --- Étape 1 : compilation, sur l'architecture de la machine de build (le jar est portable)
FROM --platform=$BUILDPLATFORM eclipse-temurin:17.0.21_9-jdk-noble AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle gradle.properties ./
COPY gradle gradle
# Tous les modules Gradle doivent exister (settings.gradle) ; seul pantry-service est assemblé.
COPY recipe-contract recipe-contract
COPY recipe-service/build.gradle recipe-service/build.gradle
COPY pantry-service pantry-service
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :pantry-service:bootJar --no-daemon -q \
 && java -Djarmode=tools -jar pantry-service/build/libs/app.jar extract --layers --launcher --destination /extracted

# --- Étape 2 : exécution, JRE seul, utilisateur non root
FROM eclipse-temurin:17.0.21_9-jre-noble
RUN groupadd --system --gid 10001 app && useradd --system --uid 10001 --gid app --no-create-home app
WORKDIR /app
# Couches Spring Boot : les dépendances changent rarement, le code souvent.
COPY --from=build /extracted/dependencies/ ./
COPY --from=build /extracted/spring-boot-loader/ ./
COPY --from=build /extracted/snapshot-dependencies/ ./
COPY --from=build /extracted/application/ ./
USER 10001:10001
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
