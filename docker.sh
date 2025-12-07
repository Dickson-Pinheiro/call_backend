#!/bin/bash

# 🐳 Call Backend - Docker Helper Script
# Facilita o gerenciamento do ambiente Docker

set -e

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Funções auxiliares
print_header() {
    echo -e "\n${BLUE}=========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}=========================================${NC}\n"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

# Verificar se Docker está instalado
check_docker() {
    if ! command -v docker &> /dev/null; then
        print_error "Docker não está instalado!"
        exit 1
    fi

    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        print_error "Docker Compose não está instalado!"
        exit 1
    fi
}

# Função para usar docker-compose ou docker compose
docker_compose() {
    if command -v docker-compose &> /dev/null; then
        docker-compose "$@"
    else
        docker compose "$@"
    fi
}

# Comandos
start() {
    print_header "🚀 Iniciando serviços Docker"
    docker_compose up -d
    print_success "Serviços iniciados com sucesso!"
    print_info "Executando health checks..."
    sleep 5
    docker_compose ps
}

stop() {
    print_header "🛑 Parando serviços Docker"
    docker_compose down
    print_success "Serviços parados com sucesso!"
}

restart() {
    print_header "🔄 Reiniciando serviços Docker"
    docker_compose restart
    print_success "Serviços reiniciados com sucesso!"
}

rebuild() {
    print_header "🔨 Reconstruindo aplicação"
    docker_compose build --no-cache app
    print_success "Aplicação reconstruída com sucesso!"
    print_info "Reiniciando serviço..."
    docker_compose up -d app
}

logs() {
    print_header "📋 Visualizando logs"
    if [ -n "$1" ]; then
        docker_compose logs -f "$1"
    else
        docker_compose logs -f
    fi
}

clean() {
    print_header "🧹 Limpando ambiente Docker"
    print_info "Isso irá remover containers, volumes e dados!"
    read -p "Tem certeza? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        docker_compose down -v
        print_success "Ambiente limpo com sucesso!"
    else
        print_info "Operação cancelada"
    fi
}

status() {
    print_header "📊 Status dos serviços"
    docker_compose ps
    echo ""
    print_info "Portas expostas:"
    echo "  - PostgreSQL: localhost:5432"
    echo "  - Redis: localhost:6379"
    echo "  - Aplicação: http://localhost:8080"
}

shell() {
    service=${1:-app}
    print_header "🐚 Abrindo shell no container: $service"
    docker_compose exec "$service" sh
}

db_shell() {
    print_header "🗄️  Abrindo PostgreSQL shell"
    docker_compose exec postgres psql -U postgres -d call_backend
}

redis_shell() {
    print_header "🔴 Abrindo Redis CLI"
    docker_compose exec redis redis-cli
}

dev() {
    print_header "💻 Modo Desenvolvimento (somente infraestrutura)"
    print_info "Iniciando PostgreSQL e Redis..."
    docker_compose up -d postgres redis
    print_success "Infraestrutura iniciada!"
    echo ""
    print_info "Execute a aplicação na sua IDE com o profile 'local'"
    print_info "Ou use: ./mvnw spring-boot:run -Dspring-boot.run.profiles=local"
}

test_infra() {
    print_header "🧪 Testando infraestrutura"
    
    echo "Testando PostgreSQL..."
    if docker_compose exec -T postgres psql -U postgres -d call_backend -c "SELECT 1;" &> /dev/null; then
        print_success "PostgreSQL: OK"
    else
        print_error "PostgreSQL: FALHOU"
    fi
    
    echo "Testando Redis..."
    if docker_compose exec -T redis redis-cli ping | grep -q "PONG"; then
        print_success "Redis: OK"
    else
        print_error "Redis: FALHOU"
    fi
}

help() {
    print_header "📚 Call Backend Docker Helper"
    echo "Uso: ./docker.sh [comando]"
    echo ""
    echo "Comandos disponíveis:"
    echo "  start          - Inicia todos os serviços"
    echo "  stop           - Para todos os serviços"
    echo "  restart        - Reinicia todos os serviços"
    echo "  rebuild        - Reconstrói a aplicação"
    echo "  logs [service] - Visualiza logs (opcional: especificar serviço)"
    echo "  clean          - Remove containers e volumes"
    echo "  status         - Mostra status dos serviços"
    echo "  shell [service]- Abre shell no container (padrão: app)"
    echo "  db             - Abre PostgreSQL shell"
    echo "  redis          - Abre Redis CLI"
    echo "  dev            - Inicia apenas PostgreSQL e Redis (para dev local)"
    echo "  test           - Testa conectividade da infraestrutura"
    echo "  help           - Mostra esta ajuda"
    echo ""
    echo "Exemplos:"
    echo "  ./docker.sh start"
    echo "  ./docker.sh logs app"
    echo "  ./docker.sh shell postgres"
}

# Main
check_docker

case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        restart
        ;;
    rebuild)
        rebuild
        ;;
    logs)
        logs "$2"
        ;;
    clean)
        clean
        ;;
    status)
        status
        ;;
    shell)
        shell "$2"
        ;;
    db)
        db_shell
        ;;
    redis)
        redis_shell
        ;;
    dev)
        dev
        ;;
    test)
        test_infra
        ;;
    help|--help|-h|"")
        help
        ;;
    *)
        print_error "Comando desconhecido: $1"
        help
        exit 1
        ;;
esac
