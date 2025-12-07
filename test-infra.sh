#!/bin/bash

# 🧪 Script de teste rápido da infraestrutura Docker
# Verifica se todos os serviços estão funcionando corretamente

set -e

# Cores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}🧪 Testando Infraestrutura Call Backend${NC}"
echo -e "${BLUE}=========================================${NC}\n"

# Detectar docker-compose
if command -v docker-compose &> /dev/null; then
    DC="docker-compose"
else
    DC="docker compose"
fi

# Função para testar serviço
test_service() {
    local service=$1
    local test_command=$2
    local service_name=$3
    
    echo -n "Testando ${service_name}... "
    
    if eval "$test_command" &> /dev/null; then
        echo -e "${GREEN}✓ OK${NC}"
        return 0
    else
        echo -e "${RED}✗ FALHOU${NC}"
        return 1
    fi
}

# Verificar se containers estão rodando
echo "📦 Verificando containers..."
echo ""
$DC ps
echo ""

# Contador de falhas
failures=0

# Teste PostgreSQL
if ! test_service "postgres" \
    "$DC exec -T postgres psql -U postgres -d call_backend -c 'SELECT 1;'" \
    "PostgreSQL"; then
    ((failures++))
    echo -e "${YELLOW}   Dica: Verifique os logs com: $DC logs postgres${NC}"
fi

# Teste Redis
if ! test_service "redis" \
    "$DC exec -T redis redis-cli ping | grep -q 'PONG'" \
    "Redis"; then
    ((failures++))
    echo -e "${YELLOW}   Dica: Verifique os logs com: $DC logs redis${NC}"
fi

# Teste Redis Pub/Sub
echo -n "Testando Redis Pub/Sub... "
if $DC exec -T redis redis-cli PUBLISH test:channel "test-message" &> /dev/null; then
    echo -e "${GREEN}✓ OK${NC}"
else
    echo -e "${RED}✗ FALHOU${NC}"
    ((failures++))
fi

# Teste aplicação (se estiver rodando)
if $DC ps app | grep -q "Up"; then
    echo -n "Testando Aplicação... "
    
    # Aguardar um pouco para garantir que a app iniciou
    sleep 2
    
    if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health 2>/dev/null | grep -q "200\|404"; then
        echo -e "${GREEN}✓ OK${NC}"
    else
        # Tentar endpoint raiz
        if curl -s -o /dev/null http://localhost:8080 2>/dev/null; then
            echo -e "${GREEN}✓ OK (respondendo)${NC}"
        else
            echo -e "${YELLOW}⚠ Verificar${NC}"
            echo -e "${YELLOW}   A aplicação pode estar iniciando. Aguarde alguns segundos.${NC}"
        fi
    fi
else
    echo -e "${YELLOW}ℹ Aplicação não está rodando no Docker${NC}"
fi

echo ""
echo -e "${BLUE}=========================================${NC}"

# Resultado final
if [ $failures -eq 0 ]; then
    echo -e "${GREEN}✓ Todos os testes passaram!${NC}"
    echo ""
    echo -e "${BLUE}🎉 Infraestrutura pronta para uso!${NC}"
    echo ""
    echo "Portas disponíveis:"
    echo "  - PostgreSQL: localhost:5432"
    echo "  - Redis:      localhost:6379"
    echo "  - Aplicação:  http://localhost:8080"
else
    echo -e "${RED}✗ $failures teste(s) falharam${NC}"
    echo ""
    echo "Para investigar:"
    echo "  $DC logs        # Ver todos os logs"
    echo "  $DC ps          # Ver status dos containers"
    echo "  $DC restart     # Reiniciar serviços"
    exit 1
fi

echo -e "${BLUE}=========================================${NC}"
