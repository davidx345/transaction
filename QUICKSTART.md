# 🚀 Quick Start Guide

Get your reconciliation system running locally in 5 minutes!

---

## Prerequisites

- ✅ Java 17 installed
- ✅ Node.js 16+ installed
- ✅ PostgreSQL running locally
- ✅ Git installed

---

## Step 1: Clone & Setup Backend (2 min)

```bash
# Navigate to backend directory
cd backend

# Set up database (PostgreSQL should be running)
# Create database
psql -U postgres
CREATE DATABASE recon_db;
\q

# Configure application.properties
# Edit src/main/resources/application.properties
# Update these values:
spring.datasource.url=jdbc:postgresql://localhost:5432/recon_db
spring.datasource.username=postgres
spring.datasource.password=your_password

# Build and run
./mvnw spring-boot:run
# or
mvn spring-boot:run
```

Backend should start on **http://localhost:8080**

---

## Step 2: Setup Frontend (2 min)

```bash
# Open a new terminal
cd frontend

# Install dependencies
npm install

# Backend should be running on localhost:8080
# .env already configured for this

# Start dev server
npm run dev
```

Frontend should start on **http://localhost:5173**

---

## Step 3: Test the Application (1 min)

### Open Browser
Navigate to: http://localhost:5173

### Test Features

1. **📊 Dashboard** - Click "Dashboard" in sidebar
   - View stats
   - Click "Run Reconciliation"

2. **📤 CSV Upload** - Click "CSV Upload"
   - Drag and drop a CSV file (or create a sample)
   - Select bank
   - Upload

3. **⚠️ Disputes** - Click "Disputes"
   - View any disputes
   - Click on a dispute
   - Approve or reject

4. **🔍 Transactions** - Click "Transactions"
   - Search by reference
   - View comparison

5. **🔗 Webhooks** - Click "Webhooks"
   - View webhook logs

6. **📈 Metrics** - Click "Metrics"
   - View operational metrics

---

## Sample CSV Format

Create a file `test_settlement.csv`:

```csv
Reference,Amount,Date,Status
PAY-001,5000.00,2024-01-15,SUCCESS
PAY-002,3500.00,2024-01-15,SUCCESS
PAY-003,12000.00,2024-01-16,SUCCESS
```

---

## Troubleshooting

### Backend won't start
```bash
# Check Java version
java -version  # Should be 17+

# Check PostgreSQL is running
psql -U postgres -c "SELECT version();"

# Check database exists
psql -U postgres -l
```

### Frontend won't start
```bash
# Check Node version
node -v  # Should be 16+

# Clear and reinstall
rm -rf node_modules package-lock.json
npm install
```

### API calls fail
- Verify backend is running on port 8080
- Check `frontend/.env` has `VITE_API_URL=http://localhost:8080`
- Open browser DevTools (F12) → Console for errors
- Check Network tab for failed requests

---

## Default Ports

- **Backend:** http://localhost:8080
- **Frontend:** http://localhost:5173
- **PostgreSQL:** localhost:5432

---

## Environment Variables

### Backend (`application.properties`)
```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/recon_db
spring.datasource.username=postgres
spring.datasource.password=your_password

# Paystack (optional for testing)
paystack.api.key=your_test_key

# RabbitMQ (optional for local testing)
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
```

### Frontend (`.env`)
```env
VITE_API_URL=http://localhost:8080
```

---

## Next Steps

Once running locally:

1. **Test all features** - Go through each page
2. **Upload real data** - Try with actual bank CSV
3. **Trigger reconciliation** - See disputes created
4. **Review PRD** - Read `fintech_recon_prd.md`
5. **Deploy** - Follow `DEPLOYMENT.md`

---

## Development Workflow

### Backend Changes
```bash
# Make code changes
# Restart Spring Boot (or use Spring DevTools for hot reload)
./mvnw spring-boot:run
```

### Frontend Changes
```bash
# Vite has hot reload by default
# Just save your file and see changes instantly
```

---

## Useful Commands

### Backend
```bash
# Run tests
./mvnw test

# Build JAR
./mvnw clean package

# Check database
psql -U postgres -d recon_db
```

### Frontend
```bash
# Run dev server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

---

## Sample Data

### Create sample transactions in database:

```sql
-- Paystack transactions
INSERT INTO paystack_transactions (reference, amount, status, created_at)
VALUES 
  ('PAY-001', 5000.00, 'SUCCESS', NOW()),
  ('PAY-002', 3500.00, 'SUCCESS', NOW()),
  ('PAY-003', 12000.00, 'SUCCESS', NOW());

-- Bank settlements
INSERT INTO bank_settlements (reference, amount, settlement_date)
VALUES 
  ('PAY-001', 5000.00, NOW()),
  ('PAY-002', 3499.00, NOW()),  -- Intentional mismatch for testing
  ('PAY-003', 12000.00, NOW());

-- Ledger entries
INSERT INTO ledger_entries (reference, amount, recorded_at)
VALUES 
  ('PAY-001', 5000.00, NOW()),
  ('PAY-002', 3500.00, NOW()),
  ('PAY-003', 12000.00, NOW());
```

Then run reconciliation to see PAY-002 create a dispute!

---

## Architecture Overview

```
Frontend (React)  ──HTTPS──▶  Backend (Spring Boot)
     │                              │
     │                              ├─▶ PostgreSQL
     │                              ├─▶ RabbitMQ (optional)
     │                              └─▶ Paystack API
     │
     └─────────── Vercel (Production)
```

---

## Key Files

```
project/
├── backend/
│   ├── src/main/resources/
│   │   └── application.properties   ← Configure database
│   └── pom.xml                      ← Dependencies
│
├── frontend/
│   ├── .env                         ← Local API URL
│   ├── src/
│   │   ├── api/client.ts           ← API configuration
│   │   ├── App.tsx                 ← Main app + routing
│   │   └── pages/                  ← All page components
│   └── package.json                 ← Dependencies
│
└── Documentation/
    ├── DEPLOYMENT.md                ← Production deployment
    ├── ARCHITECTURE.md              ← System architecture
    └── IMPLEMENTATION_SUMMARY.md    ← Feature checklist
```

---

## 🎉 You're Ready!

Your local development environment is now set up. Start building and testing!

For deployment to production, see **DEPLOYMENT.md**.

For architecture details, see **ARCHITECTURE.md**.

For complete feature list, see **IMPLEMENTATION_SUMMARY.md**.

---

## Support

If you get stuck:
1. Check browser console (F12)
2. Check backend logs
3. Review troubleshooting section above
4. Check PostgreSQL connection
5. Verify environment variables

**Happy coding! 🚀**
