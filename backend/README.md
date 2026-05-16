# PGE Rides — Backend

API REST para gerenciamento de corridas corporativas, desenvolvida em Java 21 + Spring Boot 3.2.5.

---

## Demo

- Vídeo de demonstração: https://drive.google.com/file/d/1K4awBtZ8L5f8Jao4DO_UZSrkG9lBDGuZ/view?usp=sharing
- Repositório frontend público: https://github.com/joaogabriel343/TESTE-PGE-CE-FRONT.git
- Repositório backend público: https://github.com/joaogabriel343/TESTE-PGE-CE-BACK.git

---

## Stack

| Tecnologia | Versão | Função |
|---|---|---|
| Java | 21 | Linguagem |
| Spring Boot | 3.2.5 | Framework principal |
| Spring Data JPA | — | Persistência |
| Spring WebSocket | — | Notificações em tempo real (STOMP) |
| Spring AMQP | — | Integração com RabbitMQ |
| Spring Data Redis | — | Cache de corridas em andamento |
| MySQL | 8.0 | Banco de dados |
| RabbitMQ | 3.13 | Fila de pedidos |
| Redis | 7 | Cache |
| Springdoc OpenAPI | 2.5.0 | Documentação automática |

---

## Como Executar

> **Pré-requisito:** [Docker Desktop](https://www.docker.com/products/docker-desktop/) instalado e em execução.

```bash
docker-compose up --build
```

Acesse em: **http://localhost:4200**

### Local (sem Docker)

Pré-requisitos: Java 21, Maven, MySQL 8, Redis e RabbitMQ rodando localmente.

```bash
./mvnw spring-boot:run
```

---

## Serviços e Portas

| Serviço | URL |
|---|---|
| API REST | http://localhost:8081/api |
| Swagger UI | http://localhost:8081/swagger-ui.html |
| RabbitMQ Admin | http://localhost:15673 (guest/guest) |
| MySQL | localhost:3307 |
| Redis | localhost:6380 |

---

## Variáveis de Ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `DB_HOST` | `localhost` | Host do MySQL |
| `DB_NAME` | `ridesdb` | Nome do banco |
| `DB_USER` | `rides_user` | Usuário do banco |
| `DB_PASSWORD` | `rides_pass` | Senha do banco |
| `REDIS_HOST` | `localhost` | Host do Redis |
| `RABBITMQ_HOST` | `localhost` | Host do RabbitMQ |
| `RABBITMQ_USER` | `guest` | Usuário do RabbitMQ |
| `RABBITMQ_PASS` | `guest` | Senha do RabbitMQ |

---

## Endpoints da API

### Corridas

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/rides` | Cria uma nova corrida (status: PENDING) |
| `GET` | `/api/rides` | Lista todas as corridas |
| `GET` | `/api/rides/pending` | Lista corridas aguardando aceitação |
| `GET` | `/api/rides/{id}` | Busca por ID (Redis primeiro, fallback banco) |
| `POST` | `/api/rides/{id}/accept` | Motorista aceita a corrida |
| `POST` | `/api/rides/{id}/reject` | Motorista rejeita a corrida |
| `POST` | `/api/rides/{id}/complete` | Motorista finaliza a corrida |

### Motoristas

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/drivers` | Lista motoristas mockados |

### Exemplo — Criar corrida

```bash
curl -X POST http://localhost:8081/api/rides \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-1",
    "pickupAddress": "Av. Beira Mar, 100, Meireles, Fortaleza/CE",
    "destinationAddress": "Av. Washington Soares, 200, Edson Queiroz, Fortaleza/CE"
  }'
```

### Exemplo — Aceitar corrida

```bash
curl -X POST http://localhost:8081/api/rides/1/accept \
  -H "Content-Type: application/json" \
  -d '{ "driverId": "driver-1" }'
```

---

## Fluxo Interno

```
POST /api/rides
  → Persiste no MySQL (status: PENDING)
  → Publica na fila RabbitMQ (rides.exchange → rides.queue)
      → RideConsumer consome
      → NotificationService envia via WebSocket /topic/rides
              → Motoristas conectados recebem em tempo real

POST /api/rides/{id}/accept
  → Atualiza status para IN_PROGRESS no MySQL
  → Grava no Redis com TTL de 24h
  → Corrida some de GET /api/rides/pending
```

---

## Testes

```bash
# Roda todos os testes unitários (usa H2 em memória, sem serviços externos)
./mvnw test        # Linux/Mac
mvnw.cmd test      # Windows
```

| Classe | Cenários |
|---|---|
| `RideControllerTest` | 16 testes — 201, 400, 404, 409 em todos os endpoints |
| `RideServiceTest` | 16 testes — sucesso, erros, resiliência Redis |
| `RideExpirationSchedulerTest` | 4 testes — expiração automática de corridas PENDING |

---

## Estrutura de Diretórios

```
src/main/java/br/gov/pgece/rides/
├── config/        # CORS, Redis, RabbitMQ, WebSocket, OpenAPI
├── controller/    # RideController, DriverController
├── service/       # RideService, NotificationService, RideExpirationScheduler
├── messaging/     # RideProducer, RideConsumer
├── repository/    # RideRepository (Spring Data JPA)
├── model/         # Ride, RideStatus, MockedDrivers
├── dto/           # CreateRideRequest, AcceptRideRequest, RideResponse
└── exception/     # GlobalExceptionHandler + exceções customizadas
```
