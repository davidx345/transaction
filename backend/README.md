# Backend - Automated Transaction Reconciliation Engine

This is the backend service for the Automated Transaction Reconciliation & Dispute Triage Engine.

## Tech Stack
- Java 17
- Spring Boot 3.x
- PostgreSQL 14+
- RabbitMQ
- Flyway

## Setup
1. Ensure PostgreSQL and RabbitMQ are running.
2. Configure `application.yml` with your database and broker credentials.
3. Run `mvn spring-boot:run`.
