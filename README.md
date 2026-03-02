# Notification-consumer

Spring Boot service that consumes notification events from Kafka, renders an HTML email using a Thymeleaf template, sends the email (SendGrid API or SMTP), and (optionally) stores an audit row in Postgres for each successfully sent notification.

## What this project does

- **Kafka consumer**: listens to `notification.kafka.main-topic` and processes `NotificationEvent` messages.
- **Email templating**: renders `consumer/src/main/resources/templates/email-notification.html` using Thymeleaf.
- **Email delivery**:
  - `notification.email.delivery-mode: sendgrid` (default) sends via SendGrid v3 `POST /v3/mail/send`.
  - `notification.email.delivery-mode: smtp` sends via SMTP using Spring's `JavaMailSender`.
- **Audit log**: after a successful send, inserts a row into Postgres table `notification_sent` with:
  - `notification_id` (PK)
  - `event_type`
  - `customer_name`

## Repo layout

- `consumer/` - Spring Boot application (Maven wrapper included).
- `docker-compose.yml` - local Postgres on host port `5433`.

## Tech stack

- Java 17
- Spring Boot (see `consumer/pom.xml`)
- Spring for Apache Kafka
- Thymeleaf
- Spring Boot Mail (SMTP)
- Spring Data JPA + PostgreSQL driver

## Prerequisites

- Java 17 installed (`java -version`)
- Docker + Docker Compose
- A Kafka broker reachable at `notification.kafka.bootstrap-servers` (default in config is `localhost:9092`)

## Configuration

This project uses both:

- `consumer/src/main/resources/application.yml` (Kafka + email delivery settings)
- `consumer/src/main/resources/application.properties` (server port + Postgres datasource)

### Kafka

Configure in `application.yml`:

- `notification.kafka.bootstrap-servers`
- `notification.kafka.group-id`
- `notification.kafka.main-topic`
- `notification.kafka.dlq-topic`

### Email delivery mode

In `application.yml`:

- `notification.email.delivery-mode`: `sendgrid` or `smtp`
- `notification.third-party.from-email`: sender email address
- `notification.template.sender-name` / `notification.template.footer`: template values

#### SendGrid

In `application.yml`:

- `notification.third-party.base-url` (typically `https://api.sendgrid.com`)
- `notification.third-party.send-path` (typically `/v3/mail/send`)
- `notification.third-party.api-key` (SendGrid API key)

#### SMTP

In `application.yml`:

- `spring.mail.host`
- `spring.mail.port`
- `spring.mail.username`
- `spring.mail.password`
- `spring.mail.properties.mail.smtp.auth`
- `spring.mail.properties.mail.smtp.starttls.enable`

Important: do **not** commit real API keys / SMTP passwords. Prefer environment variables or a local profile file (for example `application-local.yml`) for secrets.

### Postgres (audit log)

In `consumer/src/main/resources/application.properties`:

- `spring.datasource.url=jdbc:postgresql://localhost:5433/notification_consumer`
- `spring.datasource.username=notification`
- `spring.datasource.password=notification`
- `spring.jpa.hibernate.ddl-auto=update`

## Running locally

### 1) Start Postgres

From repo root:

```bash
docker compose up -d
```

Postgres will be available on `localhost:5433`.

### 2) Start Kafka

Run Kafka however you normally do (local install, Docker, etc.). The app expects it at `localhost:9092` unless you change `notification.kafka.bootstrap-servers`.

### 3) Run the Spring Boot app

From `consumer/`:

```bash
./mvnw spring-boot:run
```

or build:

```bash
./mvnw -DskipTests package
```

## Event contract

Kafka payload class: `consumer/src/main/java/com/notification/consumer/event/NotificationEvent.java`

Fields used by this consumer:

- `notificationId` (UUID) — required for de-dup + audit insert
- `userEmail` — recipient address
- `customerName`
- `eventType`
- `referenceId`
- `metadata` (map) — used by the email template

Example JSON (field names must match):

```json
{
  "notificationId": "7b2c52c0-7b0b-4b2f-9c1a-7e3d3b2a0b8a",
  "userEmail": "user@example.com",
  "customerName": "Tharun",
  "eventType": "Order-Placed",
  "referenceId": "ref1334",
  "metadata": {
    "price": 100000,
    "order": "Order placed for iPhone 17 Pro"
  }
}
```

## Email template

Template: `consumer/src/main/resources/templates/email-notification.html`

It renders:

- Greeting (`customerName`)
- `eventType`
- `metadata.order` and `metadata.price` (if present)
- A generic “Details” list of all `metadata` entries
- Footer/signature (`senderName`, `footer`)

## Audit log behavior

Code:

- Entity: `consumer/src/main/java/com/notification/consumer/persistence/NotificationSent.java`
- Repo: `consumer/src/main/java/com/notification/consumer/persistence/NotificationSentRepository.java`
- Insert/skip logic: `consumer/src/main/java/com/notification/consumer/kafka/listener/EmailNotificationListener.java`

Rules:

- If `notificationId` already exists in DB, the consumer **skips** sending the email.
- If DB is down/unreachable:
  - The consumer proceeds with sending email (no de-dup).
  - The audit insert is best-effort and logs on failure (no Kafka retry due to DB errors).

## Logging

Central logger helper: `consumer/src/main/java/com/notification/consumer/logging/Logger.java`

All app logs should go through this helper (backed by SLF4J).
