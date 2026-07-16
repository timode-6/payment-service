# Payment Service

Processes payments for orders, asynchronously.

Part of a five-service stack — see [`k8s`](https://github.com/timode-6/k8s) for how it's deployed alongside [`api-gateway`](https://github.com/timode-6/api-gateway), [`auth-service`](https://github.com/timode-6/auth-service), [`user-service`](https://github.com/timode-6/user-service), and [`order-service`](https://github.com/timode-6/order-service).

## Responsibilities

- Consumes `order.created` events from Kafka and processes payment for each order.
- Delegates the actual payment decision to `random-number-service`, a small Go sidecar living in this repo: it draws a random number and marks the transaction successful if it's even, failed if it's odd — a stand-in for a real payment provider.
- Stores payment records in MongoDB.

## Stack

- Java 21, Spring Boot
- Kafka consumer (`@KafkaListener`)
- MongoDB
- `random-number-service` — Go, simulates a payment gateway's success/failure

## Running locally

```bash
./gradlew bootRun
```

`random-number-service` has its own Dockerfile under `random-number-service/` and comes up alongside everything else via `docker-compose.yml`.

## Notes

- Because processing happens off a Kafka topic, taking this service down doesn't affect order creation — events queue up and get replayed, in order, once it's back.