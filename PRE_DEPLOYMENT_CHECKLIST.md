# ✅ Pre-Deployment Checklist

Use this checklist before deploying your reconciliation system to production.

---

## 🔧 Backend Checklist

### Environment Configuration
- [ ] `application.properties` configured for production profile
- [ ] Paystack API key obtained and ready
- [ ] `system.properties` specifies Java 17
- [ ] Database connection pooling configured
- [ ] CORS origins include production frontend URL

### Heroku Setup
- [ ] Heroku account created
- [ ] Heroku CLI installed and logged in
- [ ] PostgreSQL add-on provisioned
- [ ] RabbitMQ add-on provisioned (CloudAMQP)
- [ ] Environment variables set:
  - [ ] `SPRING_PROFILES_ACTIVE=prod`
  - [ ] `PAYSTACK_SECRET_KEY`
  - [ ] `DATABASE_URL` (auto-set)
  - [ ] `CLOUDAMQP_URL` (auto-set)

### Code Verification
- [ ] All tests pass locally
- [ ] Maven/Gradle build succeeds
- [ ] Flyway migrations tested
- [ ] No hardcoded secrets in code
- [ ] Logging configured appropriately

---

## 🎨 Frontend Checklist

### Environment Configuration
- [ ] `.env` file created with local API URL
- [ ] `.env.production` updated with actual Heroku backend URL
- [ ] No secrets committed to Git
- [ ] Environment variables use `VITE_` prefix

### Build Verification
- [ ] `npm install` runs without errors
- [ ] `npm run build` succeeds
- [ ] `npm run dev` works locally
- [ ] All TypeScript types are correct
- [ ] No console errors in browser

### Vercel Setup
- [ ] Vercel account created
- [ ] GitHub repository connected
- [ ] Build settings configured:
  - [ ] Root Directory: `frontend`
  - [ ] Build Command: `npm run build`
  - [ ] Output Directory: `dist`
  - [ ] Install Command: `npm install`
- [ ] Environment variable `VITE_API_URL` added

---

## 🔌 Integration Checklist

### API Endpoints
- [ ] All backend endpoints exist and respond
- [ ] `/api/ingest/csv` accepts multipart files
- [ ] `/api/reconciliations/run` triggers reconciliation
- [ ] `/api/disputes` returns dispute list
- [ ] `/api/disputes/:id` returns dispute details
- [ ] `/api/disputes/:id/approve` works
- [ ] `/api/disputes/:id/reject` works

### Optional Endpoints (may need implementation)
- [ ] `/api/transactions/compare` implemented
- [ ] `/api/webhooks` implemented
- [ ] `/api/metrics` implemented

### CORS Configuration
- [ ] Backend allows frontend origin
- [ ] Credentials enabled if needed
- [ ] Appropriate HTTP methods allowed
- [ ] Tested with browser DevTools

---

## 🧪 Testing Checklist

### Local Testing
- [ ] Backend runs on port 8080
- [ ] Frontend runs on port 5173
- [ ] Can upload CSV file
- [ ] Can trigger reconciliation
- [ ] Can view disputes
- [ ] Can approve/reject disputes
- [ ] Navigation works between all pages
- [ ] No console errors

### Production Testing (After Deployment)
- [ ] Frontend loads at Vercel URL
- [ ] API calls succeed (check Network tab)
- [ ] CSV upload works
- [ ] Reconciliation triggers
- [ ] Disputes display correctly
- [ ] Webhook monitor shows data
- [ ] Metrics dashboard displays

---

## 🔐 Security Checklist

### Secrets Management
- [ ] No API keys in frontend code
- [ ] No database credentials in Git
- [ ] Environment variables used for all secrets
- [ ] `.env` files in `.gitignore`

### HTTPS & Security
- [ ] Backend uses HTTPS (Heroku default)
- [ ] Frontend uses HTTPS (Vercel default)
- [ ] CORS configured correctly
- [ ] Rate limiting enabled
- [ ] Input validation on backend
- [ ] SQL injection prevention (JPA/Hibernate)

### Optional (Recommended for Production)
- [ ] Authentication/Authorization implemented
- [ ] JWT tokens for API access
- [ ] Role-based access control
- [ ] Audit logging enabled
- [ ] Error tracking (Sentry, etc.)

---

## 📊 Monitoring Checklist

### Logging
- [ ] Backend logs configured
- [ ] Log level appropriate for production
- [ ] Sensitive data not logged
- [ ] Heroku logs accessible via CLI

### Metrics & Alerts
- [ ] Heroku metrics dashboard reviewed
- [ ] Vercel analytics enabled
- [ ] Database performance monitored
- [ ] Alert thresholds configured

---

## 💰 Cost Checklist

### Free Tier Limitations
- [ ] Heroku free dyno limitations understood (sleeps after 30min)
- [ ] PostgreSQL free tier limits known (1GB storage)
- [ ] RabbitMQ free tier limits known (1 connection)
- [ ] Vercel bandwidth limits understood (100GB/month)

### Upgrade Plan
- [ ] Know when to upgrade dyno
- [ ] Database scaling plan ready
- [ ] Cost monitoring enabled

---

## 📝 Documentation Checklist

- [ ] README.md updated with deployment info
- [ ] DEPLOYMENT.md reviewed
- [ ] IMPLEMENTATION_SUMMARY.md reviewed
- [ ] Backend API endpoints documented
- [ ] Environment variables documented
- [ ] Known issues documented

---

## 🚀 Deployment Steps

### 1. Backend Deployment
```bash
# Navigate to backend
cd backend

# Create Heroku app
heroku create your-app-backend

# Add addons
heroku addons:create heroku-postgresql:mini
heroku addons:create cloudamqp:lemur

# Set environment variables
heroku config:set SPRING_PROFILES_ACTIVE=prod
heroku config:set PAYSTACK_SECRET_KEY=your_key

# Deploy
git push heroku main

# Get backend URL
heroku info
```

### 2. Update Frontend Config
```bash
# Edit .env.production
VITE_API_URL=https://your-app-backend.herokuapp.com
```

### 3. Frontend Deployment
```bash
# Commit changes
git add frontend/.env.production
git commit -m "Configure production API URL"
git push origin main

# Deploy to Vercel (via dashboard or CLI)
vercel --prod
```

### 4. Post-Deployment
- [ ] Test all features in production
- [ ] Update CORS if needed
- [ ] Monitor logs for errors
- [ ] Share URLs with team

---

## ✅ Final Verification

After deployment, verify:
- [ ] Backend responds at Heroku URL
- [ ] Frontend loads at Vercel URL
- [ ] API calls work end-to-end
- [ ] CSV upload functional
- [ ] Reconciliation triggers
- [ ] Disputes manageable
- [ ] No critical errors in logs

---

## 📞 Support

If you encounter issues:
1. Check Heroku logs: `heroku logs --tail`
2. Check Vercel deployment logs in dashboard
3. Review browser console (F12)
4. Check Network tab for failed requests
5. Verify environment variables
6. Review DEPLOYMENT.md troubleshooting section

---

**🎉 Ready to Deploy!**

Once all checkboxes are complete, you're ready to deploy your reconciliation system to production!
