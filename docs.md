# Planejamento Detalhado - Sistema de Chamadas de Vídeo (Backend)

## 1. Visão Geral da Arquitetura

### Tecnologias Base
- **Spring Boot 3.x** (com Java 17+)
- **H2 Database** (modo arquivo persistente)
- **WebSocket** (Spring WebSocket + STOMP)
- **WebRTC** (signaling via WebSocket)
- **Maven** como gerenciador de dependências

---

## 2. Estrutura do Banco de Dados H2

### Tabelas Principais

#### **USER (Usuários)**
```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    is_online BOOLEAN DEFAULT FALSE
);
```

#### **CALL (Chamadas)**
```sql
CREATE TABLE calls (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user1_id BIGINT NOT NULL,
    user2_id BIGINT NOT NULL,
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    duration_seconds INT,
    call_type VARCHAR(20) DEFAULT 'VIDEO', -- VIDEO, AUDIO
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, COMPLETED, CANCELLED
    FOREIGN KEY (user1_id) REFERENCES users(id),
    FOREIGN KEY (user2_id) REFERENCES users(id)
);
```

#### **CHAT_MESSAGE (Mensagens do Chat)**
```sql
CREATE TABLE chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    call_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    message_text TEXT NOT NULL,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (call_id) REFERENCES calls(id),
    FOREIGN KEY (sender_id) REFERENCES users(id)
);
```

#### **CALL_RATING (Avaliações)**
```sql
CREATE TABLE call_ratings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    call_id BIGINT NOT NULL UNIQUE,
    rater_id BIGINT NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (call_id) REFERENCES calls(id),
    FOREIGN KEY (rater_id) REFERENCES users(id)
);
```

---

## 3. Dependências Maven (pom.xml)

```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <!-- H2 Database -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>
    
    <!-- Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    
    <!-- Security (para senha) -->
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-crypto</artifactId>
    </dependency>
    
    <!-- JWT (autenticação) -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.11.5</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.11.5</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.11.5</version>
        <scope>runtime</scope>
    </dependency>
    
    <!-- Lombok (opcional, facilita código) -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

---

## 4. Configuração do H2 (application.properties)

```properties
# H2 Database (arquivo persistente)
spring.datasource.url=jdbc:h2:file:./data/videocall_db
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=admin
spring.datasource.password=admin

# JPA/Hibernate
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# H2 Console (desenvolvimento)
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# Server
server.port=8080
```

---

## 5. Estrutura de Pacotes (Package Structure)

```
com.group_call
├── config
│   ├── WebSocketConfig.java
│   ├── SecurityConfig.java
│   └── CorsConfig.java
├── entity
│   ├── UserEntity.java
│   ├── CallEntity.java
│   ├── ChatMessageEntity.java
│   └── CallRatingEntity.java
├── repository
│   ├── UserRepository.java
│   ├── CallRepository.java
│   ├── ChatMessageRepository.java
│   └── CallRatingRepository.java
├── tree (camada AVL)
│   ├── UserTree.java
│   ├── CallTree.java (já existe)
│   ├── ChatMessageTree.java
│   └── CallRatingTree.java
├── service
│   ├── UserService.java
│   ├── CallService.java
│   ├── ChatMessageService.java
│   ├── CallRatingService.java
│   ├── MatchmakingService.java (pareamento aleatório)
│   └── WebRTCSignalingService.java
├── controller
│   ├── AuthController.java
│   ├── UserController.java
│   ├── CallController.java
│   ├── ChatMessageController.java
│   └── CallRatingController.java
├── websocket
│   ├── WebRTCHandler.java
│   └── ChatHandler.java
├── dto
│   ├── request
│   │   ├── RegisterRequest.java
│   │   ├── LoginRequest.java
│   │   ├── ChatMessageRequest.java
│   │   └── RatingRequest.java
│   └── response
│       ├── AuthResponse.java
│       ├── UserResponse.java
│       ├── CallResponse.java
│       └── ErrorResponse.java
└── util
    ├── JwtUtil.java
    └── PasswordEncoder.java
```

---

## 6. Fluxo de Funcionamento (WebRTC + Pareamento)

### 6.1 Sistema de Pareamento Aleatório
```
1. Usuário se conecta via WebSocket
2. Entra em uma "fila de espera"
3. MatchmakingService pareia dois usuários aleatórios
4. Cria registro de Call no banco
5. Inicia troca de sinais WebRTC (offer/answer/ICE candidates)
6. Estabelece conexão peer-to-peer
```

### 6.2 Mensagens WebSocket (STOMP)

**Topics/Endpoints:**
- `/app/join-queue` - Entrar na fila de pareamento
- `/app/leave-queue` - Sair da fila
- `/app/webrtc-signal` - Sinais WebRTC (offer, answer, ice)
- `/app/chat-message` - Enviar mensagem no chat
- `/app/end-call` - Encerrar chamada
- `/topic/match-found/{userId}` - Notificação de pareamento
- `/topic/webrtc-signal/{callId}` - Sinais WebRTC para o peer
- `/topic/chat/{callId}` - Mensagens do chat

---

## 7. Endpoints REST

### Autenticação
- `POST /api/auth/register` - Cadastrar usuário
- `POST /api/auth/login` - Login (retorna JWT)

### Usuários
- `GET /api/users/me` - Dados do usuário logado
- `PUT /api/users/me` - Atualizar perfil

### Chamadas
- `GET /api/calls/history` - Histórico de chamadas
- `GET /api/calls/{id}` - Detalhes de uma chamada
- `POST /api/calls/{id}/end` - Finalizar chamada manualmente

### Mensagens
- `GET /api/calls/{callId}/messages` - Mensagens de uma chamada

### Avaliações
- `POST /api/calls/{callId}/rate` - Avaliar uma chamada
- `GET /api/calls/{callId}/rating` - Ver avaliação

---

## 8. Implementação das Árvores AVL

Você já tem `CallTree.java` implementada. Seguir o mesmo padrão para:

### Características das Árvores:
1. **Carregamento inicial** do banco via `@EventListener(ApplicationReadyEvent.class)`
2. **Balanceamento** ao construir (`buildBalanced`)
3. **Cache em memória** com `ConcurrentHashMap<Long, Node>`
4. **Thread-safety** com `ReentrantReadWriteLock`
5. **Sincronização** com banco (save/update/delete)

### Árvores a implementar:
- `UserTree` - Índice por `id` e por `email`
- `ChatMessageTree` - Índice por `id` e por `callId`
- `CallRatingTree` - Índice por `callId`

---

## 9. Sistema de Pareamento (MatchmakingService)

```java
@Service
public class MatchmakingService {
    private final Queue<Long> waitingQueue = new ConcurrentLinkedQueue<>();
    private final Map<Long, String> userSessions = new ConcurrentHashMap<>();
    
    public void joinQueue(Long userId, String sessionId);
    public void leaveQueue(Long userId);
    private void tryMatch(); // Pareia 2 usuários da fila
    private void notifyMatch(Long user1, Long user2, Long callId);
}
```

---

## 10. Segurança e Autenticação

### JWT Token
- Gerado no login
- Contém `userId` e `email`
- Válido por 24 horas
- Enviado no header: `Authorization: Bearer <token>`

### WebSocket Authentication
- Token JWT enviado no handshake
- Validado antes de permitir conexão
- Sessão WebSocket associada ao userId

---

## 11. Ordem de Implementação Sugerida

### Fase 1 - Base (Semana 1)
1. Configurar projeto Spring Boot
2. Criar entities e repositories
3. Configurar H2 e gerar schema
4. Implementar UserTree e autenticação JWT
5. Endpoints de registro e login

### Fase 2 - Árvores AVL (Semana 1-2)
6. Adaptar CallTree existente
7. Implementar ChatMessageTree
8. Implementar CallRatingTree
9. Testes unitários das árvores

### Fase 3 - WebSocket e WebRTC (Semana 2)
10. Configurar WebSocket (STOMP)
11. Implementar WebRTCHandler (signaling)
12. Criar MatchmakingService
13. Testar pareamento e signaling

### Fase 4 - Features Completas (Semana 3)
14. Sistema de chat em tempo real
15. Histórico de chamadas
16. Sistema de avaliações
17. Endpoints REST completos

### Fase 5 - Refinamento (Semana 3-4)
18. Tratamento de erros
19. Logs e monitoramento
20. Documentação API (Swagger)
21. Testes de integração

---

## 12. Considerações Importantes

### Performance
- AVL Trees mantêm O(log n) para buscas
- `ConcurrentHashMap` para acesso rápido por ID
- WebRTC peer-to-peer reduz carga do servidor

### Escalabilidade
- Para produção, considerar Redis para fila de pareamento
- WebSocket com STOMP permite clustering
- H2 é adequado para protótipo, migrar para PostgreSQL em produção

### Limitações do H2
- Arquivo único pode ter problemas com muitos acessos concorrentes
- Sem replicação nativa
- Adequado para desenvolvimento e pequena escala

---

## 13. Fluxo Completo de Uma Chamada

1. **Usuário A faz login** → Recebe JWT token
2. **Usuário A conecta WebSocket** → Envia token no handshake
3. **Usuário A entra na fila** → `/app/join-queue`
4. **Usuário B faz o mesmo** → Entra na fila
5. **MatchmakingService pareia** → Cria Call no banco e AVL
6. **Ambos recebem notificação** → `/topic/match-found/{userId}`
7. **Usuário A cria offer** → Envia via `/app/webrtc-signal`
8. **Usuário B recebe offer** → Via `/topic/webrtc-signal/{callId}`
9. **Usuário B cria answer** → Envia de volta
10. **Troca de ICE candidates** → Estabelece conexão P2P
11. **Chamada ativa** → Chat via WebSocket, vídeo via WebRTC
12. **Fim da chamada** → Atualiza duração no banco e AVL
13. **Avaliação** → POST `/api/calls/{callId}/rate`

---

## 14. Próximos Passos

1. Revisar e aprovar este planejamento
2. Criar estrutura inicial do projeto
3. Implementar fase por fase
4. Testes contínuos durante desenvolvimento
5. Preparar para integração com frontend