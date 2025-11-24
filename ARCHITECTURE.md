# Application Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         USER INTERFACE (Vercel)                      │
│                     React + Vite + TypeScript                        │
└─────────────────────────────────────────────────────────────────────┘
                                   │
                                   │ HTTPS
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│                           FRONTEND PAGES                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│   Reconciliation Dashboard     CSV Upload                        │
│  ├─ Stats Cards                 ├─ Drag-and-drop                     │
│  ├─ Run Reconciliation          ├─ Bank Selection                    │
│  └─ Recent Activity             └─ Format Validation                 │
│                                                                       │
│      Dispute Management          Transaction Comparison            │
│  ├─ Dispute List                ├─ Search by Reference               │
│  ├─ Confidence Scores           ├─ Provider Data                     │
│  ├─ Dispute Detail              ├─ Bank Data                         │
│  ├─ Rules Breakdown             └─ Ledger Data                       │
│  └─ Audit Trail Timeline                                             │
│                                                                       │
│   Webhook Monitor              Metrics Dashboard                 │
│  ├─ Delivery Status             ├─ Performance Metrics               │
│  ├─ Retry Tracking              ├─ Business Impact                   │
│  └─ Health Metrics              └─ Success Targets                   │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
                                   │
                            API Client (Axios)
                  Environment-based URL Configuration
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      BACKEND API (Heroku)                            │
│                  Java Spring Boot 3.x + PostgreSQL                   │
└─────────────────────────────────────────────────────────────────────┘
                                   │
                    ┌──────────────┼──────────────┐
                    │              │              │
                    ▼              ▼              ▼
         ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
         │   Ingestion  │ │Reconciliation│ │   Disputes   │
         │  Controller  │ │  Controller  │ │  Controller  │
         └──────────────┘ └──────────────┘ └──────────────┘
                    │              │              │
                    └──────────────┼──────────────┘
                                   │
                                   ▼
         ┌───────────────────────────────────────────┐
         │          BUSINESS LOGIC LAYER             │
         ├───────────────────────────────────────────┤
         │  • IngestionService                       │
         │  • ReconciliationEngine                   │
         │  • DisputeService                         │
         │  • WebhookRecoveryService                 │
         │  • RefundService                          │
         │  • AuditService                           │
         └───────────────────────────────────────────┘
                                   │
                    ┌──────────────┼──────────────┐
                    │              │              │
                    ▼              ▼              ▼
         ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
         │  PostgreSQL  │ │   RabbitMQ   │ │   Paystack   │
         │   Database   │ │    Queue     │ │   Webhooks   │
         │              │ │              │ │              │
         │ • Paystack   │ │ • Async Jobs │ │ • Payment    │
         │ • Bank       │ │ • Webhooks   │ │   Events     │
         │ • Ledger     │ │ • Retries    │ │              │
         │ • Disputes   │ │              │ │              │
         │ • Audit      │ │              │ │              │
         └──────────────┘ └──────────────┘ └──────────────┘
```

---

## Data Flow: CSV Upload & Reconciliation

```
┌──────────────┐
│     User     │
└──────┬───────┘
       │ 1. Upload CSV
       ▼
┌──────────────────────────┐
│   CSVUpload Component    │
└──────────┬───────────────┘
           │ POST /api/ingest/csv
           ▼
┌──────────────────────────┐
│  IngestionController     │
└──────────┬───────────────┘
           │
           ▼
┌──────────────────────────┐
│   IngestionService       │
│  • Parse CSV             │
│  • Validate format       │
│  • Store in DB           │
└──────────┬───────────────┘
           │
           ▼
┌──────────────────────────┐
│   PostgreSQL Database    │
│  bank_settlements table  │
└──────────────────────────┘
```

---

## Data Flow: Three-Way Reconciliation

```
┌──────────────┐
│     User     │
└──────┬───────┘
       │ 1. Click "Run Reconciliation"
       ▼
┌────────────────────────────────┐
│ ReconciliationDashboard        │
└──────────┬─────────────────────┘
           │ POST /api/reconciliations/run
           ▼
┌────────────────────────────────┐
│   ReconciliationController     │
└──────────┬─────────────────────┘
           │
           ▼
┌────────────────────────────────┐
│    ReconciliationEngine        │
│  1. Fetch Provider (Paystack)  │
│  2. Fetch Bank Settlements     │
│  3. Fetch Ledger Entries       │
│  4. Compare amounts/refs       │
│  5. Apply tolerance rules      │
│  6. Calculate confidence       │
└──────────┬─────────────────────┘
           │
           ├─ Matched → Update status
           │
           └─ Mismatch → Create Dispute
                         │
                         ▼
                ┌────────────────────┐
                │  Dispute Created   │
                │  • Confidence score│
                │  • Rules fired     │
                │  • Audit log       │
                └────────────────────┘
```

---

##   Data Flow: Dispute Management

```
┌──────────────┐
│     User     │
└──────┬───────┘
       │ 1. View disputes
       ▼
┌────────────────────────────────┐
│     DisputeList Component      │
└──────────┬─────────────────────┘
           │ GET /api/disputes
           ▼
┌────────────────────────────────┐
│     DisputeController          │
└──────────┬─────────────────────┘
           │
           ▼
┌────────────────────────────────┐
│     DisputeService             │
│  • Fetch all disputes          │
│  • Filter by status            │
│  • Sort by confidence          │
└──────────┬─────────────────────┘
           │
           ▼
┌────────────────────────────────┐
│    DisputeList Display         │
│  • Confidence badges           │
│  • Status indicators           │
│  • Quick actions               │
└────────────────────────────────┘
           │
           │ 2. Click dispute
           ▼
┌────────────────────────────────┐
│   DisputeDetail Component      │
└──────────┬─────────────────────┘
           │ GET /api/disputes/:id
           ▼
┌────────────────────────────────┐
│     DisputeDetail Display      │
│  • Transaction info            │
│  • Rules breakdown             │
│  • Audit trail timeline        │
│  • Approve/Reject buttons      │
└──────────┬─────────────────────┘
           │
           │ 3. Approve/Reject
           ▼
┌────────────────────────────────┐
│ POST /api/disputes/:id/approve │
│ POST /api/disputes/:id/reject  │
└──────────┬─────────────────────┘
           │
           ▼
┌────────────────────────────────┐
│      DisputeService            │
│  • Update status               │
│  • Log decision                │
│  • If approved → Trigger refund│
│  • Create audit entry          │
└────────────────────────────────┘
```

---

## Data Flow: Webhook Recovery

```
┌──────────────┐
│   Paystack   │
└──────┬───────┘
       │ Webhook Event
       ▼
┌────────────────────────────────┐
│   WebhookController            │
│  POST /api/webhooks/paystack   │
└──────────┬─────────────────────┘
           │
           ▼
┌────────────────────────────────┐
│   WebhookRecoveryService       │
│  • Validate signature          │
│  • Parse payload               │
│  • Store in database           │
│  • Update transaction          │
└──────────┬─────────────────────┘
           │
           ├─ Success → Mark delivered
           │
           └─ Failure → Queue retry
                         │
                         ▼
                ┌────────────────────┐
                │    RabbitMQ        │
                │  • Retry queue     │
                │  • Exponential     │
                │    backoff         │
                └─────┬──────────────┘
                      │
                      │ Scheduled retry
                      ▼
                ┌────────────────────┐
                │  Retry Delivery    │
                │  • Max 5 attempts  │
                │  • Track status    │
                └────────────────────┘
```

---

## Technology Stack

### Frontend
- **Framework:** React 18
- **Build Tool:** Vite 5
- **Language:** TypeScript 5
- **Routing:** React Router 6
- **HTTP Client:** Axios
- **Styling:** CSS (Apple-inspired design system)
- **Hosting:** Vercel

### Backend
- **Framework:** Spring Boot 3.x
- **Language:** Java 17
- **Database:** PostgreSQL 15
- **Migration:** Flyway
- **Queue:** RabbitMQ (CloudAMQP)
- **API:** RESTful JSON
- **Hosting:** Heroku

### External Services
- **Payment Gateway:** Paystack API
- **Webhooks:** Paystack webhook events
- **Monitoring:** Heroku metrics + Vercel analytics

---

## Security Architecture

```
┌─────────────────────────────────────────────┐
│            Frontend (Vercel)                 │
│  • HTTPS enforced                            │
│  • No secrets in code                        │
│  • Environment variables                     │
│  • CORS handled by browser                   │
└──────────────┬──────────────────────────────┘
               │ HTTPS
               ▼
┌─────────────────────────────────────────────┐
│            Backend (Heroku)                  │
│  • HTTPS enforced                            │
│  • CORS whitelist                            │
│  • Input validation                          │
│  • SQL injection prevention (JPA)            │
│  • Rate limiting                             │
│  • Environment variables                     │
└──────────────┬──────────────────────────────┘
               │
    ┌──────────┼──────────┐
    ▼          ▼          ▼
┌────────┐ ┌────────┐ ┌────────┐
│Database│ │RabbitMQ│ │Paystack│
│SSL/TLS │ │TLS 1.2+│ │HTTPS   │
└────────┘ └────────┘ └────────┘
```

---

## Scalability Architecture

```
Current (MVP):
┌───────────┐      ┌──────────────┐
│  Vercel   │─────▶│   Heroku     │
│  Frontend │      │  1x Standard │
└───────────┘      │    Dyno      │
                   └──────┬───────┘
                          │
                   ┌──────┴───────┐
                   │  PostgreSQL  │
                   │  Standard    │
                   └──────────────┘

Future (Scale):
┌───────────┐      ┌──────────────┐      ┌──────────────┐
│  Vercel   │─────▶│  Load        │─────▶│   Heroku     │
│  Frontend │      │  Balancer    │      │  Multiple    │
│  + CDN    │      │              │      │  Dynos       │
└───────────┘      └──────────────┘      └──────┬───────┘
                                                 │
                                          ┌──────┴───────┐
                                          │  PostgreSQL  │
                                          │  Premium +   │
                                          │  Read        │
                                          │  Replicas    │
                                          └──────────────┘
```

---

## [x] Implementation Status

- [x] Frontend: 100% complete
- [x] Backend: 100% complete  
- [x] Integration: 100% complete
- [x] Design System: 100% complete
- [x] Documentation: 100% complete
-     Deployment: Pending (ready to deploy)
-     Testing: Pending (end-to-end production testing)

---

**Status: Architecture complete and ready for deployment! **
