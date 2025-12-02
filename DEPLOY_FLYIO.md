# 🚀 Deploy no Fly.io - Guia Rápido

## ✅ Status Atual
- ✅ Aplicação configurada para usar PostgreSQL
- ✅ Imagem Docker construída localmente
- ✅ Configuração com variáveis de ambiente

## 📋 Pré-requisitos

### 1. Instalar Fly.io CLI

```bash
# Linux/Mac
curl -L https://fly.io/install.sh | sh

# Adicionar ao PATH
export PATH="$HOME/.fly/bin:$PATH"
source ~/.bashrc  # ou ~/.zshrc
```

Verificar instalação:
```bash
fly version
```

## 🔐 Configuração de Secrets (IMPORTANTE!)

### 1. Fazer login no Fly.io

```bash
fly auth login
```

### 2. Configurar secrets do banco de dados

**NÃO deixe credenciais no código!** Configure via secrets:

```bash
fly secrets set \
  SPRING_DATASOURCE_URL="jdbc:postgresql://dpg-d4nivq7pm1nc73e8d7h0-a.oregon-postgres.render.com:5432/call_db_eg84" \
  SPRING_DATASOURCE_USERNAME="call_db" \
  SPRING_DATASOURCE_PASSWORD="LbyiNPqztfJwq6qtiDJ9zSBL7osQpBKN" \
  -a call-backend
```

### 3. Configurar JWT Secret (RECOMENDADO)

```bash
fly secrets set \
  JWT_SECRET="my-super-secret-key-for-jwt-token-generation-with-at-least-256-bits-for-security" \
  JWT_EXPIRATION="86400000" \
  -a call-backend
```

### 4. Verificar secrets configurados

```bash
fly secrets list -a call-backend
```

## 🐳 Deploy com Build Local (Evita limite de CPU)

### Opção 1: Deploy direto com imagem já construída

```bash
# Autenticar Docker com Fly.io registry
fly auth docker

# Push da imagem (já construída anteriormente)
docker push registry.fly.io/call-backend:latest

# Deploy usando a imagem
fly deploy --local-only -a call-backend
```

### Opção 2: Rebuild e deploy

```bash
# Rebuild da imagem (se necessário)
docker build -t registry.fly.io/call-backend:latest .

# Push
docker push registry.fly.io/call-backend:latest

# Deploy
fly deploy --local-only -a call-backend
```

## 🔍 Monitoramento

### Ver logs em tempo real

```bash
fly logs -a call-backend
```

### Ver logs de debug

```bash
fly logs -a call-backend | grep ">>>"
```

### Ver status da aplicação

```bash
fly status -a call-backend
```

### Acessar console SSH

```bash
fly ssh console -a call-backend
```

## 🌐 Acessar a Aplicação

Após o deploy, sua aplicação estará disponível em:

```
https://call-backend.fly.dev
```

Endpoints:
- **API REST:** https://call-backend.fly.dev/api
- **WebSocket:** wss://call-backend.fly.dev/ws
- **Health Check:** https://call-backend.fly.dev/actuator/health

## 🧪 Testar a Conexão

### Health Check

```bash
curl https://call-backend.fly.dev/actuator/health
```

### Teste de Signup

```bash
curl -X POST https://call-backend.fly.dev/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@example.com","password":"123456"}'
```

### Teste de Login

```bash
curl -X POST https://call-backend.fly.dev/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"123456"}'
```

## ⚠️ Troubleshooting

### Erro: "Your organization is limited to 4 CPU cores"

**Solução:** Use build local (já implementado):
```bash
docker build -t registry.fly.io/call-backend:latest .
fly auth docker
docker push registry.fly.io/call-backend:latest
fly deploy --local-only -a call-backend
```

### Erro de conexão com banco de dados

**Verificar:**
1. Secrets estão configurados corretamente
2. IP do Fly.io está liberado no Render (se houver firewall)
3. Logs da aplicação: `fly logs -a call-backend`

**Debug:**
```bash
# Ver logs de conexão com banco
fly logs -a call-backend | grep -i "database\|connection\|postgres"

# Ver logs de debug JWT
fly logs -a call-backend | grep "JWT\|AUTH"

# Ver logs de matchmaking
fly logs -a call-backend | grep "TRY_MATCH\|JOIN_QUEUE"
```

### Aplicação não inicia

```bash
# Ver todos os logs
fly logs -a call-backend

# Reiniciar aplicação
fly apps restart call-backend

# Ver máquinas ativas
fly machines list -a call-backend
```

## 🔄 Atualizar Aplicação

### 1. Fazer mudanças no código

```bash
# Editar código...
```

### 2. Rebuild da imagem

```bash
docker build -t registry.fly.io/call-backend:latest .
```

### 3. Push e deploy

```bash
docker push registry.fly.io/call-backend:latest
fly deploy --local-only -a call-backend
```

## 📊 Comandos Úteis

```bash
# Ver configuração da app
fly config show -a call-backend

# Escalar aplicação (aumentar memória/CPU)
fly scale memory 2048 -a call-backend

# Ver métricas
fly dashboard -a call-backend

# Destruir aplicação (cuidado!)
fly apps destroy call-backend
```

## 🔒 Segurança

### Recomendações:

1. ✅ **Secrets configurados** via `fly secrets` (não no código)
2. ✅ **HTTPS forçado** (configurado em `fly.toml`)
3. ✅ **Variáveis de ambiente** para credenciais
4. ⬜ **CORS** já está liberado (pode restringir para produção)
5. ⬜ **Rate limiting** (considere adicionar)

### Restringir CORS (Opcional)

Edite `SecurityConfig.java`:
```java
configuration.setAllowedOriginPatterns(List.of("https://seu-frontend.com"));
```

## 📝 Checklist de Deploy

- [x] Fly.io CLI instalado
- [x] Login no Fly.io (`fly auth login`)
- [x] Secrets do banco configurados
- [x] JWT secret configurado
- [x] Imagem Docker construída
- [x] Docker autenticado com Fly.io
- [ ] Push da imagem para registry
- [ ] Deploy realizado
- [ ] Health check OK
- [ ] Testes de API funcionando

## 🆘 Suporte

- **Documentação Fly.io:** https://fly.io/docs/
- **Logs detalhados:** `fly logs -a call-backend`
- **Dashboard:** https://fly.io/dashboard

---

**Pronto para deploy! 🚀**

Execute os comandos na ordem e monitore os logs para garantir que tudo está funcionando.
