# FinTech Reconciliation Frontend

Modern, Apple-inspired UI for the comprehensive reconciliation engine built with React + Vite + TypeScript.

## Features

### Complete PRD Implementation
- [x] **CSV Upload** - Bank settlement file ingestion with drag-and-drop
- [x] **Reconciliation Dashboard** - Main operations center with stats and quick actions
- [x] **Transaction Comparison** - Side-by-side Provider/Bank/Ledger comparison
- [x] **Dispute Management** - Triage dashboard with confidence-based scoring
- [x] **Webhook Monitor** - Delivery health tracking and recovery metrics
- [x] **Metrics & Analytics** - Operational KPIs and business impact metrics

### Design System
- Apple-inspired minimalistic aesthetic
- SF Pro Display typography
- Custom CSS variables for consistent theming
- Smooth animations and transitions
- Responsive layout

## [x] Tech Stack

- **React 18** - UI library
- **Vite** - Build tool
- **TypeScript** - Type safety
- **React Router** - Client-side routing
- **Axios** - HTTP client

## Installation

```bash
cd frontend
npm install
```

## Configuration

### Environment Variables

Create `.env` for local development:
```env
VITE_API_URL=http://localhost:8080
```

For production (Vercel), update `.env.production`:
```env
VITE_API_URL=https://your-backend-app.herokuapp.com
```

## Running Locally

```bash
npm run dev
```

Open [http://localhost:5173](http://localhost:5173) in your browser.

**Important:** Make sure the backend is running on port 8080.

## [x] Building for Production

```bash
npm run build
```

This generates optimized static files in the `dist/` directory.

## Deployment

### Vercel Deployment

1. **Connect to GitHub**
   - Push your code to GitHub
   - Connect your repository to Vercel

2. **Configure Environment Variables**
   - In Vercel dashboard, add `VITE_API_URL` environment variable
   - Set value to your Heroku backend URL

3. **Deploy**
   - Vercel auto-deploys on push to main branch
   - Or manually trigger deployment from dashboard

### Required Settings
- **Build Command:** `npm run build`
- **Output Directory:** `dist`
- **Install Command:** `npm install`

## Project Structure

```
src/
├── api/
│   └── client.ts           # Centralized Axios instance
├── pages/
│   ├── CSVUpload.tsx       # Bank CSV upload interface
│   ├── ReconciliationDashboard.tsx  # Main dashboard
│   ├── TransactionComparison.tsx    # Side-by-side comparison
│   ├── DisputeList.tsx     # Dispute triage
│   ├── DisputeDetail.tsx   # Dispute detail with audit trail
│   ├── WebhookMonitor.tsx  # Webhook health tracking
│   └── MetricsDashboard.tsx # Operational metrics
├── App.tsx                 # Main app with routing & navigation
├── index.css              # Global styles & design system
└── main.tsx               # Entry point
```

## API Integration

All API calls use the centralized client (`src/api/client.ts`) which:
- Automatically uses correct base URL based on environment
- Includes error interceptors
- Has 30-second timeout
- Handles authentication (if configured)

### Backend Endpoints Used
- `POST /api/ingest/csv` - Upload bank settlement CSV
- `POST /api/reconciliations/run` - Trigger reconciliation
- `GET /api/disputes` - List all disputes
- `GET /api/disputes/:id` - Get dispute details
- `POST /api/disputes/:id/approve` - Approve dispute
- `POST /api/disputes/:id/reject` - Reject dispute
- `GET /api/transactions/compare?ref=xxx` - Compare transaction sources
- `GET /api/webhooks` - Get webhook logs
- `GET /api/metrics?range=7d` - Get operational metrics

## Design System

### Colors
```css
--primary: #007AFF        /* Blue - primary actions */
--success: #34C759        /* Green - success states */
--warning: #FF9500        /* Orange - warnings */
--danger: #FF3B30         /* Red - errors */
--text-primary: #1d1d1f   /* Primary text */
--text-secondary: #6e6e73 /* Secondary text */
--bg-primary: #ffffff     /* Main background */
--bg-secondary: #f5f5f7   /* Card background */
--bg-tertiary: #e8e8ed    /* Hover states */
```

### Typography
- **Font:** SF Pro Display (system fallback)
- **Weights:** 400 (regular), 500 (medium), 600 (semibold), 700 (bold)

## Troubleshooting

### API Connection Issues
- Verify backend is running
- Check `.env` file has correct `VITE_API_URL`
- Check browser console for CORS errors

### Build Errors
- Clear `node_modules` and reinstall: `rm -rf node_modules && npm install`
- Verify Node.js version (16+ required)

### Vercel Deployment Issues
- Ensure `.env.production` has correct production API URL
- Check Vercel logs for build errors
- Verify environment variables are set in Vercel dashboard

## Next Steps

1. **Update Backend URL** - Replace placeholder in `.env.production` with actual Heroku URL
2. **Test All Features** - Verify each page loads and interacts with backend
3. **Customize Branding** - Update colors/logos to match your brand
4. **Add Authentication** - Implement auth flow if required

## Contributing

This frontend implements the complete PRD specification. When adding features:
- Maintain Apple-inspired design aesthetic
- Use centralized `api/client.ts` for all API calls
- Follow TypeScript best practices
- Add loading states and error handling

## License

MIT
