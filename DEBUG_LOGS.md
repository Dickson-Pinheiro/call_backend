# 🔍 Debug de Pareamento - Logs Detalhados

## 🎯 Problema Identificado

Em produção, o sistema estava mostrando logs como "usuario 1 entrou na fila" mesmo quando usuários diferentes entravam na fila, sugerindo que o mesmo usuário estava sendo processado duas vezes.

## ✅ Correções Implementadas

### 1. **Logs Detalhados Adicionados**

#### WebSocketAuthInterceptor
- `>>> [WS_AUTH]` - Logs de autenticação JWT
- Mostra userId e email extraídos do token
- Identifica problemas de token inválido ou ausente

#### WebSocketController
- `>>> [WS_CONNECT]` - Conexão WebSocket estabelecida
- `>>> [WS_JOIN_QUEUE]` - Requisição para entrar na fila
- Mostra userId e principal.name para validar identidade

#### MatchmakingService
- `>>> [JOIN_QUEUE]` - Tentativa de entrada na fila
- `>>> [TRY_MATCH]` - Tentativa de pareamento
- Mostra estado completo da fila
- Valida se o mesmo usuário está duplicado

### 2. **Validação de Duplicação**

Adicionado check explícito no `tryMatch()`:

```java
if (user1Id.equals(user2Id)) {
    logger.error(">>> [TRY_MATCH] ERRO CRÍTICO: Mesmo usuário duas vezes! userId={}", user1Id);
    waitingQueue.offer(user1Id); // Recoloca na fila
    return;
}
```

### 3. **Sincronização Thread-Safe**

Adicionado `synchronized` no método `tryMatch()` para evitar race conditions:

```java
private void tryMatch() {
    synchronized (matchLock) {
        // ... código de pareamento
    }
}
```

### 4. **Logs Incluem Conteúdo da Fila**

```java
logger.info(">>> [JOIN_QUEUE] Usuário adicionado à fila - userId={}, Total na fila: {}, Fila atual: {}", 
            userId, waitingQueue.size(), waitingQueue);
```

Agora você verá exatamente quais IDs estão na fila.

## 📊 Como Interpretar os Logs

### Fluxo Normal de Pareamento

```
>>> [WS_AUTH] Token validado com sucesso - userId=1, email=user1@test.com
>>> [WS_CONNECT] Usuário conectado - userId=1, sessionId=abc123
>>> [WS_JOIN_QUEUE] Recebida requisição - userId=1, principal.name=1
>>> [JOIN_QUEUE] Tentativa de entrada - userId=1
>>> [JOIN_QUEUE] Usuário adicionado à fila - userId=1, Total na fila: 1, Fila atual: [1]
>>> [TRY_MATCH] Iniciando tentativa de pareamento - Fila: 1
>>> [TRY_MATCH] Fila insuficiente - size=1

>>> [WS_AUTH] Token validado com sucesso - userId=2, email=user2@test.com
>>> [WS_CONNECT] Usuário conectado - userId=2, sessionId=def456
>>> [WS_JOIN_QUEUE] Recebida requisição - userId=2, principal.name=2
>>> [JOIN_QUEUE] Tentativa de entrada - userId=2
>>> [JOIN_QUEUE] Usuário adicionado à fila - userId=2, Total na fila: 2, Fila atual: [1, 2]
>>> [TRY_MATCH] Iniciando tentativa de pareamento - Fila: 2
>>> [TRY_MATCH] Usuários retirados da fila - user1Id=1, user2Id=2
>>> [TRY_MATCH] Criando chamada - user1: 1 (João), user2: 2 (Maria)
>>> [TRY_MATCH] Pareamento concluído! CallID=1, User1=1 (João), User2=2 (Maria)
```

### Problema: Mesmo Usuário Duplicado

```
>>> [JOIN_QUEUE] Usuário adicionado à fila - userId=1, Fila atual: [1]
>>> [JOIN_QUEUE] Usuário adicionado à fila - userId=1, Fila atual: [1, 1]  ⚠️ PROBLEMA!
>>> [TRY_MATCH] Usuários retirados da fila - user1Id=1, user2Id=1
>>> [TRY_MATCH] ERRO CRÍTICO: Mesmo usuário duas vezes! userId=1  ❌
```

### Problema: Token JWT Incorreto

```
>>> [WS_AUTH] Token validado com sucesso - userId=1, email=user1@test.com
>>> [WS_CONNECT] Usuário conectado - userId=1, sessionId=abc123
>>> [WS_AUTH] Token validado com sucesso - userId=1, email=user1@test.com  ⚠️
>>> [WS_CONNECT] Usuário conectado - userId=1, sessionId=xyz789  ⚠️
```

Isso indicaria que o frontend está usando o **mesmo token JWT** para ambas as conexões.

## 🔍 Checklist de Debug

### 1. Verifique os Tokens JWT
```bash
# No frontend, imprima os tokens antes de conectar
console.log('User 1 Token:', user1Token);
console.log('User 2 Token:', user2Token);
```

Os tokens devem ser **diferentes** e conter userIds diferentes.

### 2. Verifique os Logs de Autenticação
Procure por:
```
>>> [WS_AUTH] Token validado com sucesso - userId=X
```

Se aparecer o mesmo userId para ambos os usuários, o problema está no **frontend** (tokens iguais).

### 3. Verifique a Fila
Procure por:
```
>>> [JOIN_QUEUE] Fila atual: [1, 1]  ❌ RUIM
>>> [JOIN_QUEUE] Fila atual: [1, 2]  ✅ BOM
```

### 4. Verifique o Pareamento
Procure por:
```
>>> [TRY_MATCH] ERRO CRÍTICO: Mesmo usuário duas vezes!  ❌
```

Ou:
```
>>> [TRY_MATCH] Pareamento concluído! User1=1, User2=2  ✅
```

## 🎯 Possíveis Causas do Problema

### 1. **Frontend usando mesmo token** (Mais Provável)
```javascript
// ❌ ERRADO - Ambos usam o mesmo token
const token = localStorage.getItem('token');
socket1.connect({ Authorization: `Bearer ${token}` });
socket2.connect({ Authorization: `Bearer ${token}` });

// ✅ CORRETO - Cada usuário tem seu próprio token
const user1Token = loginUser1(); // Retorna token do user 1
const user2Token = loginUser2(); // Retorna token do user 2
socket1.connect({ Authorization: `Bearer ${user1Token}` });
socket2.connect({ Authorization: `Bearer ${user2Token}` });
```

### 2. **Cache de sessão** (Menos Provável)
O navegador pode estar reutilizando cookies/sessão.

**Solução**: Use abas anônimas diferentes ou navegadores diferentes para testar.

### 3. **Race Condition** (Improvável agora)
Dois requests simultâneos processando o mesmo usuário.

**Solução**: Já implementado `synchronized` no `tryMatch()`.

## 🚀 Como Testar

### Teste Local

1. **Build e deploy**:
```bash
./mvnw clean package -DskipTests
docker-compose up -d --build
```

2. **Monitore os logs**:
```bash
docker-compose logs -f call-backend | grep ">>>"
```

3. **Teste com 2 usuários reais**:
   - Aba 1: Faça login com user1@test.com
   - Aba 2: Faça login com user2@test.com
   - Ambos entram na fila

4. **Verifique os logs**:
   - Deve mostrar `userId=1` e `userId=2`
   - Não deve mostrar `userId=1` duas vezes

### Teste em Produção

```bash
# SSH no servidor
ssh user@seu-servidor.com

# Ver logs em tempo real
docker logs -f call-backend | grep ">>>"

# Ou se usar fly.io
fly logs -a seu-app | grep ">>>"
```

## 📋 Resumo das Mudanças

| Arquivo | Mudança | Objetivo |
|---------|---------|----------|
| `MatchmakingService.java` | Logs detalhados em `joinQueue()` | Ver quais IDs entram na fila |
| `MatchmakingService.java` | Logs detalhados em `tryMatch()` | Ver IDs sendo pareados |
| `MatchmakingService.java` | Validação `user1Id.equals(user2Id)` | Detectar duplicação |
| `MatchmakingService.java` | `synchronized` em `tryMatch()` | Prevenir race conditions |
| `MatchmakingService.java` | Log mostra fila completa | Ver estado exato da fila |
| `WebSocketController.java` | Logs em `joinQueue()` | Ver requisições recebidas |
| `WebSocketAuthInterceptor.java` | Logs de autenticação | Ver tokens e IDs extraídos |

## 🔧 Próximos Passos

1. ✅ Deploy da nova versão em produção
2. ⬜ Monitorar logs com filtro `>>> [TRY_MATCH]`
3. ⬜ Verificar se mensagem de erro aparece
4. ⬜ Se aparecer "ERRO CRÍTICO: Mesmo usuário", problema está no frontend (tokens)
5. ⬜ Se não aparecer erro mas ainda empareIha errado, enviar logs completos

## 📞 Comandos Úteis

```bash
# Ver apenas logs de autenticação
docker logs call-backend | grep "WS_AUTH"

# Ver apenas logs de fila
docker logs call-backend | grep "JOIN_QUEUE"

# Ver apenas logs de pareamento
docker logs call-backend | grep "TRY_MATCH"

# Ver todos os logs de debug
docker logs call-backend | grep ">>>"

# Salvar logs em arquivo
docker logs call-backend > debug.log
```

---

**Com esses logs detalhados, será possível identificar exatamente onde está o problema!** 🎯
