# 🚀 Configuração Redis para Matchmaking Distribuído

## ✅ Implementação Concluída

O sistema agora usa **Redis** para gerenciar a fila de matchmaking, permitindo:
- ✅ **Múltiplas máquinas** rodando simultaneamente
- ✅ **Estado compartilhado** entre todas as instâncias
- ✅ **Alta disponibilidade** e escalabilidade

## 📋 Como Configurar Redis

### Opção 1: Upstash Redis (GRATUITO - Recomendado)

1. **Criar conta grátis**: https://upstash.com
2. **Criar Redis Database**:
   - Region: São Paulo (sa-east-1) ou mais próxima
   - Type: Regional (gratuito)
   - Eviction: Yes (para cache)

3. **Copiar credenciais**:
   ```
   Endpoint: redis-xxxxx.upstash.io
   Port: 6379
   Password: AxxxxxxxxxxxxxxxxxxxxxxxxxxxQ
   ```

4. **Configurar secrets no Fly.io**:
   ```bash
   flyctl secrets set REDIS_HOST=redis-xxxxx.upstash.io -a call-backend
   flyctl secrets set REDIS_PORT=6379 -a call-backend
   flyctl secrets set REDIS_PASSWORD=AxxxxxxxxxxxxxxxxxxxxxxxxxxxQ -a call-backend
   flyctl secrets set REDIS_SSL=true -a call-backend
   ```

5. **Deploy**:
   ```bash
   flyctl deploy --local-only -a call-backend
   ```

### Opção 2: Fly.io Redis (Requer cartão de crédito)

```bash
# Criar Redis no Fly.io
flyctl redis create --name call-backend-redis --region gru

# Conectar ao app
flyctl redis connect call-backend-redis -a call-backend

# Deploy
flyctl deploy --local-only -a call-backend
```

### Opção 3: Redis Local (apenas para desenvolvimento)

```bash
# Instalar Redis
sudo apt install redis-server  # Ubuntu/Debian
brew install redis             # macOS

# Iniciar Redis
redis-server

# Variáveis locais (application.properties já configurado)
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_SSL=false
```

## 🎯 Benefícios da Implementação

### Antes (1 máquina):
```
┌─────────────────────────────┐
│      Máquina Única          │
│                             │
│  Fila: [userId=1, userId=2] │
│                             │
└─────────────────────────────┘
       ↑            ↑
       │            │
    User 1       User 2
```
- ❌ Single point of failure
- ❌ Limite de recursos de 1 máquina

### Depois (N máquinas + Redis):
```
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│  Máquina 1   │  │  Máquina 2   │  │  Máquina N   │
└──────┬───────┘  └──────┬───────┘  └──────┬───────┘
       │                 │                 │
       └─────────────────┼─────────────────┘
                         ↓
              ┌──────────────────────┐
              │    Redis (Cloud)     │
              │ Fila: [1, 2, 3, ...] │
              └──────────────────────┘
```
- ✅ Alta disponibilidade
- ✅ Escalabilidade horizontal
- ✅ Fila compartilhada entre todas as máquinas

## 📊 Dados Armazenados no Redis

```
matchmaking:queue          → Lista de userIds esperando match
matchmaking:in_call:<id>   → Usuário em chamada (TTL: 30min)
matchmaking:session:<id>   → Sessão WebSocket (TTL: 30min)
```

## 🧪 Testar Localmente

1. **Iniciar Redis**:
   ```bash
   redis-server
   ```

2. **Executar aplicação**:
   ```bash
   ./mvnw spring-boot:run
   ```

3. **Verificar fila Redis**:
   ```bash
   redis-cli
   > LRANGE matchmaking:queue 0 -1
   > LLEN matchmaking:queue
   ```

## 🔧 Comandos Úteis

```bash
# Ver secrets configurados
flyctl secrets list -a call-backend

# Remover secret
flyctl secrets unset REDIS_HOST -a call-backend

# Ver logs em tempo real
flyctl logs -a call-backend

# Escalar para múltiplas máquinas (após configurar Redis)
flyctl scale count 2 -a call-backend
```

## 🚀 Próximos Passos

1. **Configure o Redis** (Opção 1 - Upstash recomendada)
2. **Set secrets no Fly.io**
3. **Deploy da aplicação**
4. **Escale para 2+ máquinas**
5. **Teste com múltiplos usuários**

---

**Nota**: Sem Redis configurado, a aplicação continuará funcionando com 1 máquina, mas falhará ao escalar horizontalmente.
