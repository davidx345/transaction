# Fintech Reconciliation Engine - Heroku Deployment Guide

## Prerequisites

1. **Heroku Account**: Sign up at [heroku.com](https://heroku.com)
2. **Heroku CLI**: Install from [devcenter.heroku.com/articles/heroku-cli](https://devcenter.heroku.com/articles/heroku-cli)
3. **Git**: Ensure git is installed and this project is a git repository

## Step-by-Step Deployment

### 1. Login to Heroku

```bash
heroku login
```

This will open a browser window for authentication.

### 2. Create Heroku App

```bash
cd backend
heroku create your-recon-app-name
```

Replace `your-recon-app-name` with your preferred app name (must be unique across Heroku).

**Example:**
```bash
heroku create fintech-recon-prod
```

### 3. Add PostgreSQL Database

```bash
heroku addons:create heroku-postgresql:mini
```

This creates a PostgreSQL database and automatically sets the `DATABASE_URL` environment variable.

**Check database info:**
```bash
heroku pg:info
```

### 4. Add CloudAMQP (RabbitMQ) - Optional

If you need RabbitMQ for async processing:

```bash
heroku addons:create cloudamqp:lemur
```

This sets `CLOUDAMQP_URL` automatically.

**Or skip RabbitMQ if not needed:**
```bash
heroku config:set RABBITMQ_HOST=localhost RABBITMQ_PORT=5672 RABBITMQ_USERNAME=guest RABBITMQ_PASSWORD=guest
```

### 5. Configure Environment Variables

```bash
# Set active Spring profile to production
heroku config:set SPRING_PROFILES_ACTIVE=prod

# Optional: Add custom database credentials if needed
# heroku config:set DATABASE_USERNAME=your_username
# heroku config:set DATABASE_PASSWORD=your_password

# Verify configuration
heroku config
```

### 6. Configure Heroku to Use Backend Subdirectory

Since your backend is in a subdirectory, you have two options:

#### Option A: Deploy from backend directory (Recommended)

```bash
cd backend

# Initialize git if not already done
git init
git add .
git commit -m "Initial backend commit"

# Add Heroku remote
heroku git:remote -a your-recon-app-name

# Deploy
git push heroku main
```

#### Option B: Deploy entire repo with buildpack path

```bash
# From project root
heroku buildpacks:set heroku/java
heroku config:set PROJECT_PATH=backend

# Create .heroku directory in project root
mkdir -p .heroku
echo "java.runtime.version=17" > .heroku/system.properties

# Deploy
git push heroku main
```

### 7. Run Database Migrations

Flyway will run automatically on startup, but you can verify:

```bash
heroku logs --tail
```

Look for Flyway migration logs like:
```
Flyway: Successfully applied 2 migrations
```

### 8. Scale the App

```bash
heroku ps:scale web=1
```

### 9. Open Your App

```bash
heroku open
```

Or visit: `https://your-recon-app-name.herokuapp.com`

### 10. Test API Endpoints

```bash
# Check health
curl https://your-recon-app-name.herokuapp.com/actuator/health

# Test disputes endpoint
curl https://your-recon-app-name.herokuapp.com/api/disputes

# Test reconciliation trigger
curl -X POST https://your-recon-app-name.herokuapp.com/api/reconciliations/run \
  -H "Content-Type: application/json" \
  -d '{"source":"all","dateRange":{"start":"2025-11-01T00:00:00","end":"2025-11-25T23:59:59"}}'
```

## Monitoring & Debugging

### View Logs

```bash
# Real-time logs
heroku logs --tail

# Last 200 lines
heroku logs -n 200

# Filter by source
heroku logs --source app --tail
```

### Access PostgreSQL Database

```bash
# Connect to database
heroku pg:psql

# Inside psql, run queries:
\dt                           # List tables
SELECT * FROM reconciliations LIMIT 5;
SELECT * FROM webhook_log LIMIT 5;
\q                            # Quit
```

### Check App Status

```bash
heroku ps
heroku apps:info
```

### Restart App

```bash
heroku restart
```

## Environment Variables Reference

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `DATABASE_URL` | PostgreSQL connection URL | Auto-set by Heroku | Yes |
| `SPRING_PROFILES_ACTIVE` | Spring profile | `prod` | Yes |
| `PORT` | Server port | Auto-set by Heroku | Yes |
| `RABBITMQ_HOST` | RabbitMQ host | localhost | No |
| `RABBITMQ_PORT` | RabbitMQ port | 5672 | No |
| `RABBITMQ_USERNAME` | RabbitMQ user | guest | No |
| `RABBITMQ_PASSWORD` | RabbitMQ password | guest | No |

## Heroku Configuration Files

### `Procfile`
Tells Heroku how to run your app:
```
web: java -Dserver.port=$PORT $JAVA_OPTS -jar target/recon-engine-0.0.1-SNAPSHOT.jar
```

### `system.properties`
Specifies Java version:
```
java.runtime.version=17
```

### `application-prod.yml`
Production Spring configuration with environment variable support.

## Database Schema

Flyway migrations will automatically create these tables:
- `reconciliations` - Main reconciliation records
- `webhook_log` - Webhook tracking
- `refund` - Refund processing

### Manual Migration (if needed)

```bash
heroku pg:psql

-- Add audit_trail column if not exists
ALTER TABLE reconciliations 
ADD COLUMN IF NOT EXISTS audit_trail JSONB DEFAULT '[]'::jsonb;

\q
```

## Troubleshooting

### Issue: App crashes on startup

**Check logs:**
```bash
heroku logs --tail
```

**Common causes:**
- Database connection failed → Check `DATABASE_URL`
- Port binding issue → Ensure `$PORT` is used in application
- Migration failure → Check Flyway logs

### Issue: Database connection timeout

**Solution:**
```bash
# Heroku Postgres requires SSL
heroku config:set SPRING_DATASOURCE_URL="jdbc:postgresql://[host]:[port]/[db]?sslmode=require"
```

### Issue: Out of memory

**Increase dyno size:**
```bash
heroku ps:resize web=standard-1x
```

Or optimize Java memory:
```bash
heroku config:set JAVA_OPTS="-Xmx512m -Xms256m"
```

### Issue: Slow startup

**Check dyno type:**
```bash
heroku ps
```

Free dynos sleep after 30 minutes of inactivity. Consider upgrading to Hobby or Standard dyno.

## Frontend Configuration

After backend is deployed, update your frontend environment variables:

**Vercel/Netlify:**
```bash
VITE_API_BASE_URL=https://your-recon-app-name.herokuapp.com
```

**Local development:**
```bash
# frontend/.env.local
VITE_API_BASE_URL=https://your-recon-app-name.herokuapp.com
```

## CI/CD Setup (Optional)

### Automatic Deployments from GitHub

1. Connect GitHub repo:
```bash
heroku git:remote -a your-recon-app-name
```

2. Enable GitHub integration in Heroku Dashboard:
   - Go to app → Deploy tab
   - Connect to GitHub
   - Enable automatic deploys from `main` branch

### Manual Deploy

```bash
git add .
git commit -m "Update backend"
git push heroku main
```

## Cost Optimization

### Free Tier Limits
- 550-1000 dyno hours/month (free)
- Heroku Postgres Mini: Free (10,000 rows)
- CloudAMQP Lemur: Free (1M messages/month)

### Scaling

```bash
# Scale down to save hours
heroku ps:scale web=0

# Scale up
heroku ps:scale web=1
```

## Security Checklist

- [ ] Set strong database password (if custom)
- [ ] Review CORS settings in `@CrossOrigin` annotations
- [ ] Enable HTTPS only (Heroku provides SSL automatically)
- [ ] Set up monitoring alerts
- [ ] Review logs for security issues
- [ ] Keep dependencies updated

## Next Steps

1. ✅ Deploy backend to Heroku
2. ✅ Verify all endpoints work
3. ✅ Update frontend to use Heroku backend URL
4. ✅ Deploy frontend to Vercel/Netlify
5. ✅ Test end-to-end integration
6. ✅ Set up monitoring and alerts
7. ✅ Configure custom domain (optional)

## Support Resources

- [Heroku Java Support](https://devcenter.heroku.com/articles/java-support)
- [Heroku Postgres](https://devcenter.heroku.com/articles/heroku-postgresql)
- [Spring Boot on Heroku](https://devcenter.heroku.com/articles/deploying-spring-boot-apps-to-heroku)
- [Heroku CLI Reference](https://devcenter.heroku.com/articles/heroku-cli-commands)

## Quick Reference Commands

```bash
# Deploy
git push heroku main

# Logs
heroku logs --tail

# Restart
heroku restart

# Database
heroku pg:psql

# Config
heroku config

# Status
heroku ps

# Open app
heroku open
```

---

**Your backend is now ready for Heroku deployment!** 🚀

Follow the steps above, and your API will be live in minutes.
