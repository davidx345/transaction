# 🚀 Quick Start: Deploy to Heroku in 5 Minutes

## Prerequisites

1. **Heroku CLI installed** → [Download here](https://devcenter.heroku.com/articles/heroku-cli)
2. **Git installed** → [Download here](https://git-scm.com/)
3. **Heroku account** → [Sign up here](https://signup.heroku.com/)

---

## Automated Deployment (Easiest)

### Windows

```bash
cd backend
deploy-heroku.bat
```

### Linux/Mac

```bash
cd backend
chmod +x deploy-heroku.sh
./deploy-heroku.sh
```

**That's it!** The script handles everything automatically.

---

## Manual Deployment (5 Steps)

### 1. Login to Heroku

```bash
heroku login
```

### 2. Create App & Database

```bash
cd backend
heroku create your-app-name
heroku addons:create heroku-postgresql:essential-0
```

### 3. Configure Environment

```bash
heroku config:set SPRING_PROFILES_ACTIVE=prod
heroku config:set RABBITMQ_HOST=localhost
heroku config:set RABBITMQ_PORT=5672
```

### 4. Deploy

```bash
git init
git add .
git commit -m "Deploy to Heroku"
heroku git:remote -a your-app-name
git push heroku main
```

### 5. Test

```bash
heroku open
# Visit: https://your-app-name.herokuapp.com/api/disputes
```

---

## What Happens During Deployment?

1. ✅ Heroku detects Java project (via `pom.xml`)
2. ✅ Reads Java version from `system.properties` (Java 17)
3. ✅ Builds project with Maven
4. ✅ Creates JAR file: `recon-engine-0.0.1-SNAPSHOT.jar`
5. ✅ Starts app using `Procfile` command
6. ✅ Runs Flyway database migrations automatically
7. ✅ App is live at `https://your-app-name.herokuapp.com`

---

## Verify Deployment

### Check if app is running

```bash
heroku ps
heroku logs --tail
```

### Test API endpoints

```bash
# Replace YOUR_APP_NAME
curl https://YOUR_APP_NAME.herokuapp.com/api/disputes
curl https://YOUR_APP_NAME.herokuapp.com/api/webhooks
curl https://YOUR_APP_NAME.herokuapp.com/api/metrics?range=7d
```

### Access database

```bash
heroku pg:psql
# Run: \dt to see tables
```

---

## Update Frontend

After backend is deployed, update your frontend:

```bash
# In frontend/.env.production or Vercel/Netlify environment variables
VITE_API_BASE_URL=https://YOUR_APP_NAME.herokuapp.com
```

Then redeploy frontend.

---

## Common Issues & Fixes

### Issue: "Application error"

```bash
heroku logs --tail
heroku restart
```

### Issue: Database connection failed

```bash
heroku config:get DATABASE_URL
# Ensure DATABASE_URL is set
```

### Issue: Build failed

```bash
# Check Java version
cat system.properties
# Should be: java.runtime.version=17
```

---

## Important Files Created

| File | Purpose |
|------|---------|
| `Procfile` | Tells Heroku how to start your app |
| `system.properties` | Specifies Java 17 |
| `application-prod.yml` | Production Spring config |
| `HerokuDatabaseConfig.java` | Handles Heroku's DATABASE_URL format |
| `deploy-heroku.sh/.bat` | Automated deployment scripts |

---

## Cost

**Free Tier Includes:**
- 550 dyno hours/month
- PostgreSQL (10k rows)
- Automatic SSL
- Custom domain support

**Perfect for MVP/Testing!**

---

## Next Steps

1. ✅ Deploy backend (you're doing this now!)
2. ⏭️ Test all API endpoints
3. ⏭️ Update frontend with Heroku URL
4. ⏭️ Deploy frontend to Vercel/Netlify
5. ⏭️ Test end-to-end

---

## Need Help?

- 📖 **Full Guide:** See `HEROKU_DEPLOYMENT.md`
- ✅ **Checklist:** See `HEROKU_CHECKLIST.md`
- 🌐 **Heroku Docs:** https://devcenter.heroku.com/

---

**Your backend is ready for Heroku!** 🎉

Just run the deployment script or follow the 5 manual steps above.
