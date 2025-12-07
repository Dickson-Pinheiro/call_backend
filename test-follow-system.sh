#!/bin/bash

# Script de teste do sistema de Follow
# Necessário: jq instalado (para parsear JSON)

BASE_URL="http://localhost:8080/api"

echo "🧪 Teste do Sistema de Follow"
echo "================================"
echo ""

# Cores para output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 1. Criar usuários de teste (assumindo que API de criação existe)
echo -e "${BLUE}1. Criando usuários de teste...${NC}"
# USER1_ID=1
# USER2_ID=2
# USER3_ID=3

# 2. User 1 segue User 2
echo -e "${BLUE}2. User 1 segue User 2${NC}"
curl -X POST "${BASE_URL}/follows/2?userId=1" \
  -H "Content-Type: application/json" | jq '.'
echo ""

# 3. User 1 segue User 3
echo -e "${BLUE}3. User 1 segue User 3${NC}"
curl -X POST "${BASE_URL}/follows/3?userId=1" \
  -H "Content-Type: application/json" | jq '.'
echo ""

# 4. User 2 segue User 1 (mútuo)
echo -e "${BLUE}4. User 2 segue User 1 (follow mútuo)${NC}"
curl -X POST "${BASE_URL}/follows/1?userId=2" \
  -H "Content-Type: application/json" | jq '.'
echo ""

# 5. Listar quem User 1 segue (deve mostrar User 2 e 3)
echo -e "${BLUE}5. Quem User 1 está seguindo?${NC}"
curl -X GET "${BASE_URL}/follows/1/following" | jq '.'
echo ""

# 6. Listar seguidores de User 1 (deve mostrar User 2)
echo -e "${BLUE}6. Quem segue User 1?${NC}"
curl -X GET "${BASE_URL}/follows/1/followers" | jq '.'
echo ""

# 7. Verificar se User 1 segue User 2
echo -e "${BLUE}7. User 1 segue User 2?${NC}"
curl -X GET "${BASE_URL}/follows/check?followerId=1&followingId=2" | jq '.'
echo ""

# 8. Ver estatísticas de User 2 (perspectiva de User 1)
echo -e "${BLUE}8. Estatísticas de User 2 (visão de User 1)${NC}"
curl -X GET "${BASE_URL}/follows/2/stats?currentUserId=1" | jq '.'
echo ""

# 9. User 1 deixa de seguir User 3
echo -e "${BLUE}9. User 1 deixa de seguir User 3${NC}"
curl -X DELETE "${BASE_URL}/follows/3?userId=1" \
  -H "Content-Type: application/json" | jq '.'
echo ""

# 10. Verificar novamente quem User 1 segue (deve mostrar apenas User 2)
echo -e "${BLUE}10. Quem User 1 está seguindo agora?${NC}"
curl -X GET "${BASE_URL}/follows/1/following" | jq '.'
echo ""

echo -e "${GREEN}✅ Testes concluídos!${NC}"
echo ""
echo "📊 Resumo esperado:"
echo "  - User 1 segue: User 2"
echo "  - User 1 tem seguidores: User 2"
echo "  - User 2 segue: User 1"
echo "  - User 3 não tem relação com User 1"
