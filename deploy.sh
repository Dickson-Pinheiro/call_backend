#!/bin/bash

# Script de deploy automatizado para Fly.io
# Autor: Sistema de Deploy Automatizado
# Data: 3 de dezembro de 2025

set -e  # Parar em caso de erro

echo "========================================="
echo "🚀 Iniciando processo de deploy..."
echo "========================================="
echo ""

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 1. Limpar e compilar o projeto
echo -e "${BLUE}[1/5] 📦 Compilando projeto Maven...${NC}"
./mvnw -q clean package -DskipTests
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Compilação concluída com sucesso${NC}"
else
    echo -e "${RED}✗ Erro na compilação${NC}"
    exit 1
fi
echo ""

# 2. Build da imagem Docker
echo -e "${BLUE}[2/5] 🐳 Construindo imagem Docker...${NC}"
docker build -t registry.fly.io/call-backend:latest .
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Imagem Docker criada com sucesso${NC}"
else
    echo -e "${RED}✗ Erro ao criar imagem Docker${NC}"
    exit 1
fi
echo ""

# 3. Autenticar no Docker registry do Fly.io
echo -e "${BLUE}[3/5] 🔐 Autenticando no Docker registry...${NC}"
~/.fly/bin/flyctl auth docker
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Autenticação bem-sucedida${NC}"
else
    echo -e "${RED}✗ Erro na autenticação${NC}"
    exit 1
fi
echo ""

# 4. Push da imagem para o registry
echo -e "${BLUE}[4/5] ⬆️  Fazendo push da imagem para o registry...${NC}"
docker push registry.fly.io/call-backend:latest
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Push concluído com sucesso${NC}"
else
    echo -e "${RED}✗ Erro ao fazer push da imagem${NC}"
    exit 1
fi
echo ""

# 5. Deploy no Fly.io
echo -e "${BLUE}[5/5] 🚀 Fazendo deploy no Fly.io...${NC}"
~/.fly/bin/flyctl deploy --local-only -a call-backend
if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}=========================================${NC}"
    echo -e "${GREEN}✓ Deploy concluído com sucesso!${NC}"
    echo -e "${GREEN}=========================================${NC}"
    echo ""
    echo -e "${YELLOW}📱 Aplicação disponível em:${NC}"
    echo -e "${BLUE}https://call-backend.fly.dev/${NC}"
    echo ""
else
    echo -e "${RED}✗ Erro no deploy${NC}"
    exit 1
fi
