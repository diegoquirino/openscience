# syntax=docker/dockerfile:1
# Production image of Veredito, also used by local staging (compose.yaml) and CI.
ARG VEREDITO_VERSION=0.3.0+dev

FROM node:26-bookworm-slim AS frontend
ARG VEREDITO_VERSION
WORKDIR /build/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund
COPY frontend/ ./
RUN VEREDITO_VERSION="${VEREDITO_VERSION}" npm run build

FROM eclipse-temurin:25-jdk AS backend
ARG VEREDITO_VERSION
WORKDIR /build/backend
COPY backend/.mvn .mvn
COPY backend/mvnw backend/pom.xml ./
COPY backend/src src
COPY --from=frontend /build/frontend/dist/frontend/browser/pt-BR src/main/resources/static/pt-BR
COPY --from=frontend /build/frontend/dist/frontend/browser/en-US src/main/resources/static/en-US
COPY --from=frontend /build/frontend/dist/frontend/browser/es-ES src/main/resources/static/es-ES
RUN --mount=type=cache,target=/root/.m2 \
    sh ./mvnw -B -ntp -DskipTests "-Dveredito.version=${VEREDITO_VERSION}" package \
    && cp target/veredito-*.jar /build/app.jar

FROM eclipse-temurin:25-jre
RUN groupadd --system veredito && useradd --system --gid veredito --no-create-home veredito
WORKDIR /app
COPY --from=backend /build/app.jar app.jar
USER veredito
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
