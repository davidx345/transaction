# Backend - Automated Transaction Reconciliation Engine

This is the backend service for the Automated Transaction Reconciliation & Dispute Triage Engine.

## Tech Stack
- Java 17
- Spring Boot 3.2.0
- PostgreSQL 14+
- RabbitMQ (Optional)
- Flyway Migrations
- Maven

## API Endpoints

### Disputes
- `GET /api/disputes` - List all disputes
- `GET /api/disputes/{id}` - Get dispute details
- `POST /api/disputes/{id}/approve` - Approve dispute
- `POST /api/disputes/{id}/reject` - Reject dispute

### Reconciliation
- `POST /api/reconciliations/run` - Trigger reconciliation

### Transactions
- `GET /api/transactions/compare?ref={ref}` - Compare transaction sources

### Webhooks
- `GET /api/webhooks` - List webhook logs
- `POST /api/webhooks/{provider}` - Receive webhook

### Metrics
- `GET /api/metrics?range={range}` - Get operational metrics

### Ingestion
- `POST /api/ingest/csv` - Upload bank CSV file

## Local Development

### Prerequisites
- Java 17
- PostgreSQL 14+
- Maven 3.8+
- (Optional) RabbitMQ

### Setup

1. **Start PostgreSQL**
```bash
# Create database
createdb recon_db
```

2. **Configure Application**
```bash
# Edit src/main/resources/application.yml
# Update database credentials
```

3. **Run Application**
```bash
mvn spring-boot:run
```

4. **Access API**
```
http://localhost:8080/api/disputes
```

## Heroku Deployment 🚀

### Quick Deploy (Automated)

**Windows:**
```bash
deploy-heroku.bat
```

**Linux/Mac:**
```bash
chmod +x deploy-heroku.sh
./deploy-heroku.sh
```

### Manual Deploy

```bash
# 1. Login
heroku login

# 2. Create app
heroku create your-app-name

# 3. Add database
heroku addons:create heroku-postgresql:essential-0

# 4. Configure
heroku config:set SPRING_PROFILES_ACTIVE=prod

# 5. Deploy
git push heroku main
```

### Documentation

- **Quick Start:** See [QUICK_START_HEROKU.md](./QUICK_START_HEROKU.md)
- **Full Guide:** See [HEROKU_DEPLOYMENT.md](./HEROKU_DEPLOYMENT.md)
- **Checklist:** See [HEROKU_CHECKLIST.md](./HEROKU_CHECKLIST.md)

## Project Structure

```
backend/
├── src/
│   └── main/
│       ├── java/com/fintech/recon/
│       │   ├── config/          # Configuration classes
│       │   ├── domain/          # JPA Entities
│       │   ├── infrastructure/  # Repositories
│       │   ├── service/         # Business logic
│       │   │   └── reconciliation/  # Reconciliation engine
│       │   └── web/            # REST Controllers
│       └── resources/
│           ├── application.yml
│           ├── application-prod.yml
│           └── db/migration/   # Flyway SQL migrations
├── Procfile                    # Heroku process file
├── system.properties           # Java version
├── deploy-heroku.sh/.bat       # Deployment scripts
└── pom.xml                     # Maven dependencies

```

## Database Schema

### Tables
- `reconciliations` - Main reconciliation records with audit trail
- `webhook_log` - Webhook delivery tracking
- `refund` - Refund processing records
- `flyway_schema_history` - Migration history

### Migrations
Flyway automatically runs migrations on startup:
- `V1__init_schema.sql` - Initial tables
- `V2__add_refunds_table.sql` - Refund table

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DATABASE_URL` | PostgreSQL connection | Auto-set by Heroku |
| `SPRING_PROFILES_ACTIVE` | Active profile | `prod` |
| `PORT` | Server port | `8080` |
| `RABBITMQ_HOST` | RabbitMQ host | `localhost` |
| `JAVA_OPTS` | JVM options | `-Xmx512m` |

See [.env.example](./.env.example) for full list.

## Testing

```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report

# Skip tests during build
mvn package -DskipTests
```

## Monitoring

### Heroku Logs
```bash
heroku logs --tail
```

### Database
```bash
heroku pg:psql
```

### App Status
```bash
heroku ps
```

## Troubleshooting

### App won't start
```bash
heroku logs --tail
heroku restart
```

### Database connection issues
```bash
heroku config:get DATABASE_URL
heroku pg:info
```

### Build failures
```bash
# Check Java version
cat system.properties

# Verify Maven build locally
mvn clean package
```

## Support

- **Issues:** Check [HEROKU_CHECKLIST.md](./HEROKU_CHECKLIST.md) troubleshooting section
- **Heroku Docs:** https://devcenter.heroku.com/
- **Spring Boot:** https://spring.io/projects/spring-boot
