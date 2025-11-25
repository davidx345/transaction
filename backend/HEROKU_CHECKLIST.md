# ✅ Heroku Deployment Checklist

## Pre-Deployment Checklist

### 1. Required Files Created ✓
- [x] `Procfile` - Tells Heroku how to run the app
- [x] `system.properties` - Specifies Java version (17)
- [x] `application-prod.yml` - Production configuration
- [x] `HerokuDatabaseConfig.java` - Handles Heroku's DATABASE_URL format
- [x] `deploy-heroku.sh` - Automated deployment script (Linux/Mac)
- [x] `deploy-heroku.bat` - Automated deployment script (Windows)

### 2. Configuration Verified ✓
- [x] Environment variables use `${VAR_NAME:default}` pattern
- [x] Port uses `${PORT:8080}` for Heroku dynamic port assignment
- [x] Database URL configured for Heroku Postgres
- [x] Flyway migrations ready (`V1__init_schema.sql`, `V2__add_refunds_table.sql`)
- [x] CORS enabled on all controllers (`@CrossOrigin(origins = "*")`)
- [x] Spring profile set to `prod` in deployment

### 3. Dependencies Verified ✓
- [x] Spring Boot 3.2.0
- [x] Java 17
- [x] PostgreSQL driver
- [x] Flyway for migrations
- [x] Lombok for boilerplate reduction
- [x] Spring Web for REST APIs
- [x] Spring Data JPA for database access

### 4. Build Configuration ✓
- [x] Maven build configured in `pom.xml`
- [x] Spring Boot plugin with repackage goal
- [x] Final JAR name: `recon-engine-0.0.1-SNAPSHOT.jar`

## Deployment Steps

### Option 1: Automated Deployment (Recommended)

**Windows:**
```bash
cd backend
deploy-heroku.bat
```

**Linux/Mac:**
```bash
cd backend
chmod +x deploy-heroku.sh
./deploy-heroku.sh
```

This script will:
1. ✓ Check Heroku CLI installation
2. ✓ Login to Heroku
3. ✓ Create app
4. ✓ Add PostgreSQL database
5. ✓ Configure environment variables
6. ✓ Optional: Add CloudAMQP (RabbitMQ)
7. ✓ Build and deploy

### Option 2: Manual Deployment

Follow these commands:

```bash
# 1. Login to Heroku
heroku login

# 2. Create app
heroku create your-app-name

# 3. Add PostgreSQL
heroku addons:create heroku-postgresql:essential-0

# 4. Set environment variables
heroku config:set SPRING_PROFILES_ACTIVE=prod
heroku config:set JAVA_OPTS="-Xmx512m -Xms256m"

# 5. (Optional) Add RabbitMQ
heroku addons:create cloudamqp:lemur
# OR skip and set dummy values:
heroku config:set RABBITMQ_HOST=localhost
heroku config:set RABBITMQ_PORT=5672

# 6. Deploy
git add .
git commit -m "Deploy to Heroku"
git push heroku main
```

## Post-Deployment Verification

### 1. Check App Status
```bash
heroku ps
heroku logs --tail
```

**Expected:**
- State: `up`
- No crash errors in logs
- Flyway migrations completed successfully

### 2. Verify Database Connection
```bash
heroku pg:info
heroku pg:psql
```

**In psql:**
```sql
\dt  -- List tables
SELECT COUNT(*) FROM reconciliations;
SELECT COUNT(*) FROM webhook_log;
\q
```

**Expected:**
- Tables created: `reconciliations`, `webhook_log`, `refund`, `flyway_schema_history`

### 3. Test API Endpoints

```bash
# Replace YOUR_APP_NAME with your actual app name

# Health check
curl https://YOUR_APP_NAME.herokuapp.com/actuator/health

# List disputes
curl https://YOUR_APP_NAME.herokuapp.com/api/disputes

# Trigger reconciliation
curl -X POST https://YOUR_APP_NAME.herokuapp.com/api/reconciliations/run \
  -H "Content-Type: application/json" \
  -d '{"source":"all","dateRange":{"start":"2025-11-01T00:00:00","end":"2025-11-25T23:59:59"}}'

# Get webhooks
curl https://YOUR_APP_NAME.herokuapp.com/api/webhooks

# Get metrics
curl https://YOUR_APP_NAME.herokuapp.com/api/metrics?range=7d

# Compare transactions
curl "https://YOUR_APP_NAME.herokuapp.com/api/transactions/compare?ref=TEST123"
```

**Expected:**
- All endpoints return 200 OK (or 404 if no data exists)
- No 500 Internal Server Errors
- Valid JSON responses

### 4. Frontend Integration

**Update frontend environment variables:**

**Vercel:**
```bash
vercel env add VITE_API_BASE_URL production
# Enter: https://YOUR_APP_NAME.herokuapp.com
vercel --prod
```

**Netlify:**
```bash
# In Netlify dashboard: Site settings → Environment variables
VITE_API_BASE_URL = https://YOUR_APP_NAME.herokuapp.com
```

**Local Testing:**
```bash
# frontend/.env.local
VITE_API_BASE_URL=https://YOUR_APP_NAME.herokuapp.com

cd frontend
npm run dev
```

**Test all pages:**
- [x] Reconciliation Dashboard
- [x] Dispute List
- [x] Dispute Detail (approve/reject)
- [x] CSV Upload
- [x] Transaction Comparison
- [x] Webhook Monitor
- [x] Metrics Dashboard

### 5. Performance Check

```bash
# Check response times
time curl https://YOUR_APP_NAME.herokuapp.com/api/disputes

# Monitor memory usage
heroku ps
```

**Expected:**
- Response time < 2 seconds for simple queries
- Memory usage < 512MB
- No dyno restarts

## Troubleshooting

### Issue: App crashes on startup

**Check:**
```bash
heroku logs --tail
```

**Common Causes:**
- Database connection failed
  - **Fix:** Verify DATABASE_URL is set: `heroku config:get DATABASE_URL`
- Port binding issue
  - **Fix:** Ensure Procfile uses `$PORT`
- Flyway migration failure
  - **Fix:** Check migration SQL syntax, run manually if needed

### Issue: "Application error" in browser

**Check:**
```bash
heroku logs --tail --source app
```

**Fix:**
```bash
heroku restart
heroku ps:scale web=1
```

### Issue: Database connection timeout

**Fix:**
```bash
# Ensure SSL is enabled for Postgres
heroku config:set SPRING_DATASOURCE_URL="jdbc:postgresql://[host]:[port]/[db]?sslmode=require"
```

### Issue: Out of memory

**Fix:**
```bash
# Optimize Java memory
heroku config:set JAVA_OPTS="-Xmx400m -Xms256m"

# Or upgrade dyno
heroku ps:resize web=standard-1x
```

### Issue: Slow cold starts

**Causes:**
- Free dynos sleep after 30 minutes of inactivity

**Fix:**
- Upgrade to Hobby dyno ($7/month) for 24/7 uptime
- Or use a ping service (not recommended for production)

## Monitoring & Maintenance

### Daily Checks
```bash
# View logs
heroku logs --tail -n 100

# Check dyno status
heroku ps

# Database metrics
heroku pg:info
```

### Weekly Checks
```bash
# Check database size
heroku pg:info | grep Rows

# Review error logs
heroku logs --tail | grep ERROR

# Update dependencies
mvn versions:display-dependency-updates
```

### Alerts Setup (Optional)

Enable Heroku alerts:
```bash
# Email on app crash
heroku notifications:enable
```

## Scaling

### Current Setup (Free Tier)
- 1 web dyno
- Heroku Postgres Mini (10k rows)
- CloudAMQP Lemur (1M messages/month)

### Scale Up When Needed

```bash
# More web dynos (horizontal scaling)
heroku ps:scale web=2

# Better dyno type (vertical scaling)
heroku ps:resize web=standard-1x

# Better database
heroku addons:upgrade heroku-postgresql:standard-0
```

## Cost Estimate

### Free Tier (Current)
- **Web Dyno:** 550 hours/month free
- **Postgres:** 10,000 rows free
- **CloudAMQP:** 1M messages/month free
- **Total:** $0/month

### Production Tier (Recommended)
- **Hobby Dyno:** $7/month (24/7 uptime)
- **Postgres Standard-0:** $50/month (10M rows)
- **CloudAMQP Bunny:** $9/month (10M messages)
- **Total:** ~$66/month

## Security Best Practices

- [x] HTTPS enforced (automatic on Heroku)
- [x] Database SSL required
- [x] CORS configured (review for production)
- [ ] Environment variables for secrets (no hardcoded passwords)
- [ ] Rate limiting (consider adding Spring Security)
- [ ] API authentication (consider JWT tokens)

## Next Steps After Deployment

1. **Monitor First 24 Hours**
   - Watch logs for errors
   - Verify all endpoints work
   - Check database growth

2. **Set Up CI/CD**
   - Connect GitHub repo
   - Enable automatic deploys on push to main

3. **Custom Domain (Optional)**
   ```bash
   heroku domains:add www.yourapp.com
   heroku certs:auto:enable
   ```

4. **Backup Strategy**
   ```bash
   heroku pg:backups:schedule --at '02:00 America/Los_Angeles'
   ```

5. **Performance Monitoring**
   - Add Heroku Metrics addon
   - Set up New Relic or Datadog

## Quick Commands Reference

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
heroku config:set KEY=value

# Status
heroku ps
heroku apps:info

# Open app
heroku open

# Shell access
heroku run bash
```

## Support

- **Heroku Docs:** https://devcenter.heroku.com/
- **Spring Boot Heroku:** https://devcenter.heroku.com/articles/deploying-spring-boot-apps-to-heroku
- **Heroku Postgres:** https://devcenter.heroku.com/articles/heroku-postgresql

---

## ✅ Deployment Complete!

Once you've followed this checklist, your backend will be:
- ✅ Deployed to Heroku
- ✅ Database configured and migrated
- ✅ All endpoints accessible
- ✅ Ready for frontend integration
- ✅ Production-ready and scalable

**Your API URL:** `https://YOUR_APP_NAME.herokuapp.com`

**Next:** Deploy your frontend and update the API URL! 🚀
