# saga-orchestrator-service

Orquestrador da saga distribuída de criação de pedidos: `order -> stock -> payment -> confirm`,
com compensação reversa (rollback distribuído) em caso de falha em qualquer etapa.

A comunicação com os demais serviços (order, stock, payment) é assíncrona via **AWS SNS/SQS**
(fan-out: cada evento é publicado num tópico SNS e consumido de uma fila SQS correspondente).
A API HTTP deste serviço é **somente leitura** — ela não dispara nem avança a saga, apenas
expõe o estado atual para consulta/observabilidade.

## Fluxo

1. `OrderCreatedEvent` chega → cria a saga → publica `ReserveStockCommand`.
2. `StockReservedEvent` → publica `ProcessPaymentCommand`.
3. `PaymentProcessedEvent` → publica `ConfirmOrderCommand`.
4. `OrderConfirmedEvent` → saga concluída (`COMPLETED`).

Se qualquer etapa falhar (`StockReservationFailedEvent`, `PaymentFailedEvent`) ou a saga
exceder o timeout configurado, o `CompensationService` desfaz, em ordem reversa, os passos
que já haviam sido concluídos (release de estoque, refund de pagamento, cancelamento do pedido),
com retry limitado (`saga.max-retry`) e log de intervenção manual quando o limite é atingido.

## Rodando localmente

Pré-requisitos: Java 21, Maven (ou use o wrapper `./mvnw`), Docker.

1. Suba as dependências (MySQL + LocalStack simulando SQS/SNS):
   ```bash
   docker compose -f src/main/resources/docker-compose.yml up -d
   ```
2. Rode a aplicação:
   ```bash
   ./mvnw spring-boot:run
   ```
   A API sobe em `http://localhost:8087`.

### Rodando como container

```bash
docker build -t saga-orchestrator-service .
docker run -p 8087:8087 \
  -e DB_USERNAME=root -e DB_PASSWORD=root \
  -e AWS_ACCESS_KEY=test -e AWS_SECRET_KEY=test \
  saga-orchestrator-service
```

> O `docker-compose.yml` atual sobe apenas as dependências (MySQL/LocalStack), não o próprio
> serviço — para rodar tudo junto num único compose, adicione um serviço `app` usando este
> Dockerfile e aponte o datasource/AWS endpoint para os hostnames dos containers (`mysql`,
> `localstack`) em vez de `localhost`.

## Variáveis de ambiente principais

| Variável | Padrão (dev) | Descrição |
|---|---|---|
| `DB_USERNAME` / `DB_PASSWORD` | `root` / `root` | Credenciais do MySQL |
| `AWS_ACCESS_KEY` / `AWS_SECRET_KEY` / `AWS_REGION` | placeholders | Credenciais AWS (SNS) |
| `ACTUATOR_USERNAME` / `ACTUATOR_PASSWORD` | `admin` / `changeit` | Basic Auth do Actuator e da API `/api/v1/sagas/**` |
| `SAGA_ORDER_TOPIC` / `SAGA_PAYMENT_TOPIC` / `SAGA_INVENTORY_TOPIC` / `SAGA_RETRY_TOPIC` | ARNs fake (`000000000000`, padrão LocalStack) | Tópicos SNS de destino dos comandos |
| `SAGA_QUEUE_*` | nomes de fila padrão | Filas SQS consumidas por cada listener |

Veja a lista completa em `src/main/resources/application.yml`.

**Nunca use as senhas/credenciais padrão acima em produção** — defina as variáveis reais no
ambiente de deploy.

## API

Documentação interativa (Swagger UI): `http://localhost:8087/swagger-ui.html`
(spec OpenAPI em `/v3/api-docs`).

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| GET | `/api/v1/health` | pública | Status do serviço |
| GET | `/api/v1/sagas/{sagaId}` | Basic Auth (ADMIN) | Estado atual da saga |
| GET | `/api/v1/sagas/order/{orderId}` | Basic Auth (ADMIN) | Estado da saga pelo id do pedido |
| GET | `/api/v1/sagas/{sagaId}/steps` | Basic Auth (ADMIN) | Histórico de steps (execução e compensação) |

## Limitações conhecidas

- Sem cobertura de testes automatizados além do smoke test de contexto.
- API somente leitura — não há endpoint para forçar retry, cancelamento manual ou listagem
  paginada de sagas.
- `AwsConfig` não configura `endpointOverride` para o `SnsClient` — em dev local, isso precisa
  ser ajustado para apontar ao LocalStack em vez da AWS real.
