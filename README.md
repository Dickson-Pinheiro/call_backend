# 📞 Call Backend

Backend da aplicação de chamadas de vídeo em tempo real com pareamento aleatório.

## 🚀 Tecnologias

- **Java 21** - Linguagem de programação
- **Spring Boot 4.0.0** - Framework principal
- **PostgreSQL** - Banco de dados relacional
- **Redis** - Cache e Pub/Sub para comunicação cross-server
- **WebSocket (STOMP)** - Comunicação em tempo real
- **Maven** - Gerenciamento de dependências
- **Docker** - Containerização

## 🏗️ Arquitetura

- **Distribuída**: Suporte para múltiplas instâncias com comunicação via Redis Pub/Sub
- **WebSocket**: Comunicação bidirecional em tempo real
- **WebRTC**: Sinalização para chamadas de vídeo P2P
- **Matchmaking**: Sistema de pareamento aleatório centralizado no Redis

## 📋 Pré-requisitos

### Desenvolvimento Local

- Java 21 ou superior
- Maven 3.9+
- Docker e Docker Compose (para executar infraestrutura)

### Produção

- Docker
- Fly.io CLI (para deploy)

## 🐳 Executar com Docker (Recomendado)

### Opção 1: Usando o script helper

```bash
# Ver comandos disponíveis
./docker.sh help

# Iniciar tudo (PostgreSQL + Redis + Aplicação)
./docker.sh start

# Ver logs
./docker.sh logs

# Parar tudo
./docker.sh stop
```

### Opção 2: Usando Make

```bash
# Ver comandos disponíveis
make help

# Iniciar tudo
make start

# Ver logs da aplicação
make app-logs

# Parar tudo
make stop
```

### Opção 3: Docker Compose direto

```bash
# Iniciar tudo
docker-compose up -d

# Ver logs
docker-compose logs -f

# Parar tudo
docker-compose down
```

## 💻 Desenvolvimento Local (sem Docker para a app)

Se você quiser executar a aplicação na sua IDE:

### 1. Iniciar apenas a infraestrutura (PostgreSQL + Redis)

```bash
# Usando script helper
./docker.sh dev

# Ou usando make
make dev

# Ou usando docker-compose
docker-compose up -d postgres redis
```

### 2. Executar a aplicação

```bash
# Via Maven
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Ou na sua IDE
# Adicione a variável de ambiente: SPRING_PROFILES_ACTIVE=local
```

### 3. Acessar

- **API**: http://localhost:8080
- **PostgreSQL**: localhost:5432
- **Redis**: localhost:6379

## 📦 Build

### Compilar

```bash
./mvnw clean package -DskipTests
```

### Executar

```bash
java -jar target/call_backend-0.0.1-SNAPSHOT.jar
```

## 🧪 Testes

### Executar testes

```bash
./mvnw test
```

### Testar infraestrutura Docker

```bash
# Usando script
./docker.sh test

# Usando make
make test
```

## 🌐 Deploy

### Fly.io (Produção)

```bash
# Build e deploy
./deploy.sh

# Ou manualmente
./mvnw clean package -DskipTests
docker build -t registry.fly.io/call-backend:latest .
docker push registry.fly.io/call-backend:latest
flyctl deploy --image registry.fly.io/call-backend:latest
```

## 🔧 Configuração

### Variáveis de Ambiente

#### Desenvolvimento Local (application-local.properties)

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/call_backend
spring.datasource.username=postgres
spring.datasource.password=postgres

spring.data.redis.host=localhost
spring.data.redis.port=6379
```

#### Produção (Fly.io)

As variáveis são configuradas via Fly.io secrets e `fly.toml`.

### Profiles

- `local` - Desenvolvimento local
- `prod` - Produção (Fly.io)
- `default` - Configurações padrão

## 📚 Documentação Adicional

- [README-DOCKER.md](README-DOCKER.md) - Guia completo do Docker
- [docs.md](docs.md) - Documentação técnica
- [REDIS_SETUP.md](REDIS_SETUP.md) - Configuração do Redis

## 🛠️ Ferramentas Úteis

### Acessar banco de dados

```bash
# Via Docker
./docker.sh db

# Ou
docker-compose exec postgres psql -U postgres -d call_backend
```

### Acessar Redis

```bash
# Via Docker
./docker.sh redis

# Ou
docker-compose exec redis redis-cli
```

### Logs

```bash
# Todos os serviços
./docker.sh logs

# Apenas aplicação
./docker.sh logs app

# Apenas PostgreSQL
./docker.sh logs postgres

# Apenas Redis
./docker.sh logs redis
```

## 🐛 Troubleshooting

### Porta já em uso

Edite `docker-compose.yml` e altere as portas:

```yaml
ports:
  - "5433:5432"  # PostgreSQL
  - "6380:6379"  # Redis
  - "8081:8080"  # Aplicação
```

### Limpar tudo e recomeçar

```bash
# Usando script
./docker.sh clean

# Usando make
make clean

# Ou manualmente
docker-compose down -v
docker system prune -f
```

### Problemas de conexão

```bash
# Verificar status
./docker.sh status

# Testar conectividade
./docker.sh test

# Ver logs detalhados
./docker.sh logs
```

## 📊 Endpoints Principais

### WebSocket

- `/ws` - Conexão WebSocket (com autenticação via header)

### Destinos STOMP

- `/app/join-queue` - Entrar na fila de pareamento
- `/app/leave-queue` - Sair da fila
- `/app/webrtc-signal` - Sinalização WebRTC
- `/app/chat-message` - Enviar mensagem de chat
- `/app/typing` - Notificar que está digitando

### Subscrições

- `/user/queue/match-found` - Pareamento encontrado
- `/user/queue/webrtc-signal` - Sinais WebRTC
- `/user/queue/chat` - Mensagens de chat
- `/user/queue/typing` - Notificações de digitação
- `/user/queue/error` - Mensagens de erro

## 🔐 Autenticação

Todas as conexões WebSocket requerem token JWT no header:

```
Authorization: Bearer <token>
```

## 📈 Escalabilidade

O sistema suporta múltiplas instâncias através de:

- **Redis Pub/Sub** - Sincronização de mensagens entre servidores
- **Redis Sets** - Fila de matchmaking centralizada
- **PostgreSQL** - Fonte de verdade para dados

## 🤝 Contribuindo

1. Fork o projeto
2. Crie uma branch para sua feature (`git checkout -b feature/AmazingFeature`)
3. Commit suas mudanças (`git commit -m 'Add some AmazingFeature'`)
4. Push para a branch (`git push origin feature/AmazingFeature`)
5. Abra um Pull Request

## 📝 Licença

Este projeto está sob a licença MIT.

## ✨ Autores

- **Dickson Pinheiro** - *Desenvolvimento* - [Dickson-Pinheiro](https://github.com/Dickson-Pinheiro)
