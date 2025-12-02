# Dockerfile para Call Backend - Spring Boot Application
# Multi-stage build para otimizar o tamanho da imagem

# ========================================
# Stage 1: Build da aplicação
# ========================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

# Definir diretório de trabalho
WORKDIR /app

# Copiar apenas arquivos de dependências primeiro (cache layer)
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Baixar dependências (cached se pom.xml não mudar)
RUN mvn dependency:go-offline -B

# Copiar código fonte
COPY src ./src

# Build da aplicação (pular testes para build mais rápido)
RUN mvn clean package -DskipTests

# ========================================
# Stage 2: Runtime da aplicação
# ========================================
FROM eclipse-temurin:21-jre-alpine

# Informações da imagem
LABEL maintainer="Call Backend Team"
LABEL description="API REST e WebSocket para sistema de videochamadas estilo Omegle"
LABEL version="0.0.1"

# Criar usuário não-root para segurança
RUN addgroup -S spring && adduser -S spring -G spring

# Definir diretório de trabalho
WORKDIR /app

# Copiar JAR da stage de build
COPY --from=builder /app/target/call_backend-*.jar app.jar

# Criar diretório para dados do H2
RUN mkdir -p /app/data && chown -R spring:spring /app

# Mudar para usuário não-root
USER spring:spring

# Expor porta da aplicação
EXPOSE 8080

# Variáveis de ambiente (podem ser sobrescritas no docker run)
ENV JAVA_OPTS="-Xms256m -Xmx512m" \
    SERVER_PORT=8080 \
    SERVER_ADDRESS=0.0.0.0

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Comando para executar a aplicação
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]
