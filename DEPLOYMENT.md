# 🚀 Complete Deployment Guide

This guide covers deploying the FinTech Reconciliation System with:
- **Backend:** Java Spring Boot on Heroku
- **Frontend:** React on Vercel

## 📋 Prerequisites

- GitHub account
- Heroku account (free tier works)
- Vercel account (free tier works)
- PostgreSQL database (Heroku provides free tier)
- RabbitMQ instance (CloudAMQP free tier or Heroku add-on)

---

## 🗄️ Backend Deployment (Heroku)

### 1. Prepare Backend

Ensure your backend has:
- `src/main/resources/application.properties` with environment variables
- `system.properties` (if not exists, create it):

```properties
java.runtime.version=17
```

### 2. Create Heroku App

```bash
# Login to Heroku
heroku login

# Navigate to backend directory
cd backend

# Create Heroku app
heroku create your-recon-app-backend

# Add PostgreSQL
heroku addons:create heroku-postgresql:mini

# Add RabbitMQ (CloudAMQP)
heroku addons:create cloudamqp:lemur

# Set environment variables
heroku config:set SPRING_PROFILES_ACTIVE=prod
heroku config:set PAYSTACK_SECRET_KEY=your_paystack_secret_key
```

### 3. Deploy Backend

```bash
# Add Heroku remote (if not already added)
heroku git:remote -a your-recon-app-backend

# Deploy
git push heroku main

# Check logs
heroku logs --tail
```

### 4. Database Setup

Heroku PostgreSQL is automatically provisioned. Database migrations run on startup via Flyway.

To manually check database:
```bash
heroku pg:psql
```

### 5. Get Your Backend URL

```bash
heroku info
```

Look for "Web URL" - this will be something like:
`https://your-recon-app-backend-abc123.herokuapp.com`

**Save this URL - you'll need it for frontend configuration!**

---

## 🎨 Frontend Deployment (Vercel)

### 1. Update Production Environment

Edit `frontend/.env.production`:

```env
VITE_API_URL=https://your-recon-app-backend-abc123.herokuapp.com
```

Replace with your actual Heroku backend URL from step 5 above.

### 2. Push to GitHub

```bash
# Commit the updated .env.production
git add frontend/.env.production
git commit -m "Configure production API URL"
git push origin main
```

### 3. Deploy to Vercel

**Option A: Vercel Dashboard (Recommended)**

1. Go to [vercel.com](https://vercel.com)
2. Click "Add New Project"
3. Import your GitHub repository
4. Configure project:
   - **Framework Preset:** Vite
   - **Root Directory:** `frontend`
   - **Build Command:** `npm run build`
   - **Output Directory:** `dist`
   - **Install Command:** `npm install`
5. Add Environment Variable:
   - **Name:** `VITE_API_URL`
   - **Value:** Your Heroku backend URL
6. Click "Deploy"

**Option B: Vercel CLI**

```bash
# Install Vercel CLI
npm install -g vercel

# Login
vercel login

# Navigate to frontend
cd frontend

# Deploy
vercel --prod
```

### 4. Get Your Frontend URL

After deployment, Vercel provides a URL like:
`https://your-recon-app.vercel.app`

---

## 🔧 Post-Deployment Configuration

### 1. CORS Configuration

Ensure backend allows requests from your Vercel frontend URL.

In `backend/src/main/java/config/WebConfig.java`:

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                    "https://your-recon-app.vercel.app",
                    "http://localhost:5173"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowCredentials(true);
    }
}
```

Redeploy backend after updating:
```bash
git add .
git commit -m "Update CORS configuration"
git push heroku main
```

### 2. Test Connection

1. Open your Vercel frontend URL
2. Open browser DevTools (F12)
3. Navigate to different pages
4. Check Console for any errors
5. Verify API calls succeed in Network tab

---

## 🔍 Troubleshooting

### Backend Issues

**Problem:** App crashes on Heroku
```bash
heroku logs --tail
```
Common causes:
- Missing environment variables
- Database connection issues
- Port binding (ensure using `$PORT` from Heroku)

**Problem:** Database migrations fail
```bash
# Check database status
heroku pg:info

# Reset database (⚠️ destroys data)
heroku pg:reset DATABASE
heroku restart
```

**Problem:** Out of memory
```bash
# Scale to larger dyno
heroku ps:scale web=1:standard-1x
```

### Frontend Issues

**Problem:** API calls fail with 404
- Check `VITE_API_URL` in Vercel environment variables
- Verify backend is deployed and running
- Check CORS configuration in backend

**Problem:** Build fails on Vercel
- Check Vercel build logs
- Verify `package.json` has all dependencies
- Try building locally: `npm run build`

**Problem:** Environment variables not working
- In Vercel: Settings → Environment Variables
- Must start with `VITE_` prefix
- Redeploy after adding variables

---

## 📊 Monitoring & Maintenance

### Backend Monitoring

```bash
# Check app status
heroku ps

# View logs
heroku logs --tail

# Check database
heroku pg:info

# Check RabbitMQ
heroku addons:info cloudamqp
```

### Frontend Monitoring

- Vercel Dashboard shows:
  - Deployment status
  - Build logs
  - Analytics (visits, performance)
  - Error tracking

### Performance Optimization

**Backend:**
- Use Heroku Standard dyno for better performance
- Add Redis for caching
- Enable database connection pooling

**Frontend:**
- Vercel automatically optimizes assets
- Consider adding React lazy loading for code splitting
- Use CDN for static assets

---

## 💰 Cost Breakdown

### Free Tier (Getting Started)
- **Heroku Mini PostgreSQL:** Free (1GB storage)
- **CloudAMQP Lemur:** Free (1 connection)
- **Vercel:** Free (100GB bandwidth/month)
- **Total:** $0/month

### Production Tier (Recommended)
- **Heroku Standard-1X:** $7/month
- **Heroku Standard PostgreSQL:** $50/month
- **CloudAMQP (or Heroku RabbitMQ):** $9/month
- **Vercel Pro:** $20/month (optional)
- **Total:** ~$66-86/month

---

## 🔐 Security Checklist

- [ ] Environment variables stored securely
- [ ] CORS properly configured
- [ ] Paystack API key protected
- [ ] Database uses SSL connection
- [ ] HTTPS enforced on all endpoints
- [ ] Rate limiting enabled
- [ ] Authentication implemented (if required)

---

## 📝 Environment Variables Reference

### Backend (Heroku)
```
DATABASE_URL=<auto-set by Heroku>
CLOUDAMQP_URL=<auto-set by addon>
SPRING_PROFILES_ACTIVE=prod
PAYSTACK_SECRET_KEY=<your-secret-key>
PORT=<auto-set by Heroku>
```

### Frontend (Vercel)
```
VITE_API_URL=<your-heroku-backend-url>
```

---

## 🚀 Continuous Deployment

### Automatic Deployments

**Backend:**
- Push to `main` branch → Auto-deploys to Heroku

**Frontend:**
- Push to `main` branch → Auto-deploys to Vercel

### Manual Deployments

**Backend:**
```bash
git push heroku main
```

**Frontend:**
```bash
cd frontend
vercel --prod
```

---

## 📞 Support Resources

- **Heroku Docs:** https://devcenter.heroku.com
- **Vercel Docs:** https://vercel.com/docs
- **Spring Boot Docs:** https://spring.io/projects/spring-boot
- **React Docs:** https://react.dev

---

## ✅ Deployment Checklist

- [ ] Backend deployed to Heroku
- [ ] PostgreSQL provisioned
- [ ] RabbitMQ provisioned
- [ ] Environment variables configured
- [ ] Backend URL obtained
- [ ] Frontend `.env.production` updated
- [ ] Frontend deployed to Vercel
- [ ] CORS configured
- [ ] All features tested in production
- [ ] Error monitoring configured
- [ ] DNS configured (optional)

---

**🎉 Congratulations! Your reconciliation system is now live!**
