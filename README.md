# saga-orchestrator-service

Orquestrador da saga distribuída de criação de pedidos: `order -> stock -> payment -> confirm`,
com compensação reversa (rollback distribuído) em caso de falha em qualquer etapa.

A comunicação com os demais serviços (order, stock, payment) é assíncrona via **RabbitMQ**
(um topic exchange por domínio - `order-exchange`, `payment-exchange`, `inventory-exchange`,
`retry-exchange` - com uma routing key por comando publicado/evento consumido; ver
`config/RabbitMQConfig.java`). O fluxo principal da saga é disparado e avançado via mensageria;
a API HTTP serve para consulta e para um punhado de operações administrativas (retry manual,
forçar compensação).

## Fluxo

1. `OrderCreatedEvent` chega → cria a saga (guarda `customerId`/`totalAmount` do pedido) →
   publica `ReserveStockCommand` (`inventory-exchange`, routing key `stock.reserve`).
2. `StockReservedEvent` → publica `ProcessPaymentCommand` (`payment-exchange`,
   `payment.process`), usando o `customerId`/`totalAmount` guardados no passo 1.
3. `PaymentProcessedEvent` → publica `ConfirmOrderCommand` (`order-exchange`, `order.confirm`).
4. `OrderConfirmedEvent` → saga concluída (`COMPLETED`).

Se qualquer etapa falhar (`StockReservationFailedEvent`, `PaymentFailedEvent`) ou a saga
exceder o timeout configurado, o `CompensationService` desfaz, em ordem reversa, os passos
que já haviam sido concluídos (release de estoque, refund de pagamento, cancelamento do pedido),
com retry limitado (`saga.max-retry`) e log de intervenção manual quando o limite é atingido.
Todas as transições de estado (incluindo timeout e compensação) passam por uma state machine
central (`statemachine/`), que rejeita transições inválidas em vez de sobrescrever o estado.

## Rodando localmente

Pré-requisitos: Java 21, Maven (ou use o wrapper `./mvnw`), Docker.

### Opção A: dependências no Docker, app na sua máquina (perfil `dev`)

```bash
docker compose up -d mysql rabbitmq
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

O perfil `dev` (`application-dev.yml`) usa os defaults de conexão (MySQL em `localhost:3306`,
RabbitMQ em `localhost:5672` com `guest`/`guest`), que já batem com o que o compose expõe.
A API sobe em `http://localhost:8087`. UI de administração do RabbitMQ em
`http://localhost:15672` (`guest`/`guest`).

### Opção B: tudo no Docker

```bash
docker compose up -d
```

Sobe MySQL, RabbitMQ e o próprio serviço (`app`), já com as variáveis de ambiente certas
para os containers se enxergarem pelo nome (`mysql`, `rabbitmq`).

### Rodando a imagem isoladamente

```bash
docker build -t saga-orchestrator-service .
docker run -p 8087:8087 \
  -e DB_HOST=host.docker.internal -e DB_USERNAME=root -e DB_PASSWORD=root \
  -e RABBITMQ_HOST=host.docker.internal -e RABBITMQ_USERNAME=guest -e RABBITMQ_PASSWORD=guest \
  saga-orchestrator-service
```

## Variáveis de ambiente principais

| Variável | Padrão | Descrição |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `3306` / `saga_db` | Host/porta/nome do MySQL |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / `root` | Credenciais do MySQL |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` | `localhost` / `5672` | Host/porta do broker RabbitMQ |
| `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | `guest` / `guest` | Credenciais do RabbitMQ |
| `ACTUATOR_USERNAME` / `ACTUATOR_PASSWORD` | `admin` / `changeit` | Basic Auth do Actuator e da API `/api/v1/sagas/**` |
| `SAGA_ORDER_EXCHANGE` / `SAGA_PAYMENT_EXCHANGE` / `SAGA_INVENTORY_EXCHANGE` / `SAGA_RETRY_EXCHANGE` | `order-exchange` / `payment-exchange` / `inventory-exchange` / `retry-exchange` | Nomes dos topic exchanges de destino dos comandos |
| `SAGA_QUEUE_*` | nomes de fila padrão | Filas RabbitMQ consumidas por cada listener |

Veja a lista completa em `src/main/resources/application.yml` e nos perfis
`application-dev.yml`/`application-prod.yml`.

**Nunca use as senhas/credenciais padrão acima em produção** — defina as variáveis reais no
ambiente de deploy (perfil `prod`, `SPRING_PROFILES_ACTIVE=prod`).

## API

Documentação interativa (Swagger UI): `http://localhost:8087/swagger-ui.html`
(spec OpenAPI em `/v3/api-docs`).

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| GET | `/api/v1/health` | pública | Status do serviço |
| GET | `/api/v1/sagas` | Basic Auth (ADMIN) | Lista sagas, paginado, opcionalmente filtrado por `status` |
| GET | `/api/v1/sagas/{sagaId}` | Basic Auth (ADMIN) | Estado atual da saga |
| GET | `/api/v1/sagas/order/{orderId}` | Basic Auth (ADMIN) | Estado da saga pelo id do pedido |
| GET | `/api/v1/sagas/{sagaId}/steps` | Basic Auth (ADMIN) | Histórico de steps (execução e compensação) |
| GET | `/api/v1/sagas/{sagaId}/events` | Basic Auth (ADMIN) | Histórico bruto de eventos recebidos |
| POST | `/api/v1/sagas/{sagaId}/compensate` | Basic Auth (ADMIN) | Força uma saga travada a entrar em compensação, sem esperar o timeout |
| POST | `/api/v1/sagas/{sagaId}/compensation/{handlerName}/retry` | Basic Auth (ADMIN) | Reexecuta manualmente um handler de compensação abandonado (esgotou `saga.max-retry`) |

## Testes

```bash
./mvnw test
```

Cobre a state machine (transições válidas/inválidas, incluindo timeout e compensação manual),
a idempotência da compensação em `SagaService` (redelivery do RabbitMQ não conta step duas
vezes), a ordenação/retry do `CompensationExecutor`, o `OrderSagaOrchestrator` (idempotência de
`OrderCreatedEvent` e montagem do `PaymentPayload`) e a segurança/mapeamento HTTP do
`SagaController`. O teste de contexto (`SagaOrchestratorServiceApplicationTests`) sobe com
H2 em memória e o RabbitMQ em modo passivo (`src/test/resources/application.yml`), sem
depender de MySQL/RabbitMQ reais.

## Limitações conhecidas

- Cobertura de testes é unitária/de contexto — não há testes de integração ponta a ponta
  contra um MySQL/RabbitMQ reais (ex. `@Testcontainers`).
- Sem endpoint para cancelar uma saga definitivamente (só forçar compensação ou retry).
