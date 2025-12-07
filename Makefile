.PHONY: help start stop restart rebuild logs clean status db redis dev test

# Cores para output
GREEN := \033[0;32m
YELLOW := \033[1;33m
BLUE := \033[0;34m
NC := \033[0m

# Detectar docker-compose ou docker compose
DOCKER_COMPOSE := $(shell if command -v docker-compose > /dev/null 2>&1; then echo "docker-compose"; else echo "docker compose"; fi)

help: ## Mostra esta ajuda
	@echo "$(BLUE)Call Backend - Docker Commands$(NC)"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(GREEN)%-15s$(NC) %s\n", $$1, $$2}'
	@echo ""

start: ## Inicia todos os serviços
	@echo "$(BLUE)🚀 Iniciando serviços...$(NC)"
	@$(DOCKER_COMPOSE) up -d
	@echo "$(GREEN)✓ Serviços iniciados!$(NC)"
	@make status

stop: ## Para todos os serviços
	@echo "$(BLUE)🛑 Parando serviços...$(NC)"
	@$(DOCKER_COMPOSE) down
	@echo "$(GREEN)✓ Serviços parados!$(NC)"

restart: ## Reinicia todos os serviços
	@echo "$(BLUE)🔄 Reiniciando serviços...$(NC)"
	@$(DOCKER_COMPOSE) restart
	@echo "$(GREEN)✓ Serviços reiniciados!$(NC)"

rebuild: ## Reconstrói a aplicação
	@echo "$(BLUE)🔨 Reconstruindo aplicação...$(NC)"
	@$(DOCKER_COMPOSE) build --no-cache app
	@$(DOCKER_COMPOSE) up -d app
	@echo "$(GREEN)✓ Aplicação reconstruída!$(NC)"

logs: ## Visualiza logs (use: make logs SERVICE=app)
	@$(DOCKER_COMPOSE) logs -f $(SERVICE)

clean: ## Remove containers e volumes
	@echo "$(YELLOW)⚠️  Isso irá remover containers e volumes!$(NC)"
	@read -p "Confirmar? (y/N): " confirm && [ "$$confirm" = "y" ] && \
		$(DOCKER_COMPOSE) down -v && \
		echo "$(GREEN)✓ Ambiente limpo!$(NC)" || \
		echo "$(YELLOW)Operação cancelada$(NC)"

status: ## Mostra status dos serviços
	@echo "$(BLUE)📊 Status dos serviços:$(NC)"
	@$(DOCKER_COMPOSE) ps
	@echo ""
	@echo "$(BLUE)Portas disponíveis:$(NC)"
	@echo "  PostgreSQL: localhost:5432"
	@echo "  Redis:      localhost:6379"
	@echo "  Aplicação:  http://localhost:8080"

db: ## Abre PostgreSQL shell
	@$(DOCKER_COMPOSE) exec postgres psql -U postgres -d call_backend

redis: ## Abre Redis CLI
	@$(DOCKER_COMPOSE) exec redis redis-cli

dev: ## Modo dev (somente PostgreSQL e Redis)
	@echo "$(BLUE)💻 Iniciando infraestrutura para desenvolvimento...$(NC)"
	@$(DOCKER_COMPOSE) up -d postgres redis
	@echo "$(GREEN)✓ PostgreSQL e Redis iniciados!$(NC)"
	@echo ""
	@echo "$(YELLOW)Execute a aplicação com:$(NC)"
	@echo "  ./mvnw spring-boot:run -Dspring-boot.run.profiles=local"

test: ## Testa infraestrutura
	@echo "$(BLUE)🧪 Testando infraestrutura...$(NC)"
	@$(DOCKER_COMPOSE) exec -T postgres psql -U postgres -d call_backend -c "SELECT 1;" > /dev/null 2>&1 && \
		echo "$(GREEN)✓ PostgreSQL: OK$(NC)" || \
		echo "$(YELLOW)✗ PostgreSQL: FALHOU$(NC)"
	@$(DOCKER_COMPOSE) exec -T redis redis-cli ping | grep -q "PONG" && \
		echo "$(GREEN)✓ Redis: OK$(NC)" || \
		echo "$(YELLOW)✗ Redis: FALHOU$(NC)"

app-logs: ## Logs apenas da aplicação
	@$(DOCKER_COMPOSE) logs -f app

postgres-logs: ## Logs apenas do PostgreSQL
	@$(DOCKER_COMPOSE) logs -f postgres

redis-logs: ## Logs apenas do Redis
	@$(DOCKER_COMPOSE) logs -f redis

shell: ## Shell no container (use: make shell SERVICE=app)
	@$(DOCKER_COMPOSE) exec $(or $(SERVICE),app) sh

build: ## Compila o projeto com Maven
	@echo "$(BLUE)📦 Compilando projeto...$(NC)"
	@./mvnw clean package -DskipTests
	@echo "$(GREEN)✓ Compilação concluída!$(NC)"

full-restart: stop build rebuild start ## Para, compila, reconstrói e inicia

prune: ## Remove tudo do Docker (cuidado!)
	@echo "$(YELLOW)⚠️  Isso irá remover TUDO do Docker!$(NC)"
	@read -p "Confirmar? (y/N): " confirm && [ "$$confirm" = "y" ] && \
		docker system prune -a --volumes -f && \
		echo "$(GREEN)✓ Docker limpo!$(NC)" || \
		echo "$(YELLOW)Operação cancelada$(NC)"
