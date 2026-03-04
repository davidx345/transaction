import React, { useEffect, useState, useRef } from 'react';
import { Link } from 'react-router-dom';
import { motion, useScroll, useTransform } from 'framer-motion';
import {
  ArrowRight,
  CheckCircle2,
  Shield,
  BarChart3,
  Globe,
  Lock,
  ChevronRight,
  RefreshCw,
  Database,
  Building,
  CreditCard,
  FileText,
  Zap,
  TrendingUp,
  AlertTriangle,
  Activity,
  ArrowUpRight,
  Layers,
} from 'lucide-react';

/* ─────────────────────────── Logo SVG ─────────────────────────── */
const NexLedgerLogo: React.FC<{ size?: number }> = ({ size = 32 }) => (
  <svg width={size} height={size} viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
    <defs>
      <linearGradient id="logoGrad" x1="0" y1="0" x2="40" y2="40" gradientUnits="userSpaceOnUse">
        <stop offset="0%" stopColor="#3B82F6" />
        <stop offset="100%" stopColor="#7C3AED" />
      </linearGradient>
    </defs>
    <rect width="40" height="40" rx="10" fill="url(#logoGrad)" />
    <rect x="9" y="12" width="14" height="18" rx="2" fill="rgba(255,255,255,0.25)" />
    <rect x="17" y="10" width="14" height="18" rx="2" fill="rgba(255,255,255,0.55)" />
    <line x1="20" y1="15" x2="28" y2="15" stroke="white" strokeWidth="1.5" strokeLinecap="round" />
    <line x1="20" y1="19" x2="28" y2="19" stroke="white" strokeWidth="1.5" strokeLinecap="round" />
    <line x1="20" y1="23" x2="25" y2="23" stroke="white" strokeWidth="1.5" strokeLinecap="round" />
  </svg>
);

/* ─────────────────────────── Animated stat counter ────────────── */
const StatCounter: React.FC<{
  target: number;
  suffix?: string;
  prefix?: string;
  label: string;
}> = ({ target, suffix = '', prefix = '', label }) => {
  const [count, setCount] = useState(0);
  const ref = useRef<HTMLDivElement>(null);
  const started = useRef(false);

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting && !started.current) {
          started.current = true;
          const duration = 1800;
          const start = performance.now();
          const animate = (now: number) => {
            const progress = Math.min((now - start) / duration, 1);
            const ease = 1 - Math.pow(1 - progress, 3);
            setCount(Math.round(ease * target));
            if (progress < 1) requestAnimationFrame(animate);
          };
          requestAnimationFrame(animate);
        }
      },
      { threshold: 0.5 }
    );
    if (ref.current) observer.observe(ref.current);
    return () => observer.disconnect();
  }, [target]);

  return (
    <div ref={ref} style={{ textAlign: 'center' }}>
      <div
        style={{
          fontSize: 'clamp(2rem, 4vw, 2.75rem)',
          fontWeight: 800,
          letterSpacing: '-0.03em',
          color: 'white',
          lineHeight: 1,
        }}
      >
        {prefix}
        {count.toLocaleString()}
        {suffix}
      </div>
      <div
        style={{
          marginTop: '0.5rem',
          fontSize: '0.875rem',
          color: '#6B7280',
          fontWeight: 500,
        }}
      >
        {label}
      </div>
    </div>
  );
};

/* ─────────────────────────── Live ticker strip ─────────────────── */
const LiveTicker: React.FC = () => {
  const items = [
    '₦4,200,000  MATCHED',
    '$18,500  RECONCILED',
    '₦780,000  EXCEPTION RESOLVED',
    '$4,000  MATCHED',
    '₦6,300,000  RECONCILED',
  ];
  return (
    <div
      style={{
        overflow: 'hidden',
        width: '100%',
        borderTop: '1px solid rgba(255,255,255,0.04)',
        borderBottom: '1px solid rgba(255,255,255,0.04)',
        backgroundColor: 'rgba(255,255,255,0.02)',
        padding: '0.75rem 0',
      }}
    >
      <motion.div
        animate={{ x: ['0%', '-50%'] }}
        transition={{ duration: 24, ease: 'linear', repeat: Infinity }}
        style={{
          display: 'flex',
          gap: '3rem',
          whiteSpace: 'nowrap',
          width: 'max-content',
        }}
      >
        {[...items, ...items].map((item, i) => (
          <span
            key={i}
            style={{
              fontSize: '0.75rem',
              fontWeight: 600,
              color: '#22C55E',
              letterSpacing: '0.08em',
              display: 'flex',
              alignItems: 'center',
              gap: '0.4rem',
            }}
          >
            <span
              style={{
                width: '6px',
                height: '6px',
                borderRadius: '50%',
                backgroundColor: '#22C55E',
                display: 'inline-block',
                boxShadow: '0 0 6px #22C55E',
              }}
            />
            {item}
          </span>
        ))}
      </motion.div>
    </div>
  );
};

/* ─────────────────────────── Main Component ────────────────────── */
export const LandingPage: React.FC = () => {
  const { scrollY } = useScroll();
  const y1 = useTransform(scrollY, [0, 600], [0, 120]);
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const fn = () => setScrolled(window.scrollY > 40);
    window.addEventListener('scroll', fn);
    return () => window.removeEventListener('scroll', fn);
  }, []);

  return (
    <div
      style={{
        minHeight: '100vh',
        backgroundColor: '#080810',
        color: 'white',
        overflowX: 'hidden',
        fontFamily:
          '-apple-system, BlinkMacSystemFont, "SF Pro Display", "Segoe UI", Roboto, sans-serif',
      }}
    >
      {/* ── Navigation ── */}
      <nav
        style={{
          position: 'fixed',
          top: 0,
          width: '100%',
          zIndex: 50,
          padding: '0 1.5rem',
          transition: 'all 0.4s',
          ...(scrolled
            ? {
                backgroundColor: 'rgba(8,8,16,0.85)',
                backdropFilter: 'blur(24px)',
                borderBottom: '1px solid rgba(255,255,255,0.06)',
              }
            : {}),
        }}
      >
        <div
          style={{
            maxWidth: '78rem',
            margin: '0 auto',
            height: '5rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          {/* Logo */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
            <NexLedgerLogo size={36} />
            <span
              style={{
                fontSize: '1.2rem',
                fontWeight: 700,
                letterSpacing: '-0.03em',
                color: 'white',
              }}
            >
              Nex<span style={{ color: '#60A5FA' }}>Ledger</span>
            </span>
          </div>

          {/* Nav links */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '2.5rem' }}>
            {['Features', 'How it Works'].map((l) => (
              <a
                key={l}
                href={`#${l.toLowerCase().replace(/\s/g, '-')}`}
                style={{
                  fontSize: '0.875rem',
                  fontWeight: 500,
                  color: '#9CA3AF',
                  textDecoration: 'none',
                  transition: 'color 0.2s',
                }}
                onMouseEnter={(e) => (e.currentTarget.style.color = 'white')}
                onMouseLeave={(e) => (e.currentTarget.style.color = '#9CA3AF')}
              >
                {l}
              </a>
            ))}
          </div>

          {/* Auth */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <Link
              to="/login"
              style={{
                fontSize: '0.875rem',
                fontWeight: 500,
                color: '#D1D5DB',
                textDecoration: 'none',
              }}
            >
              Log in
            </Link>
            <Link
              to="/login"
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.4rem',
                padding: '0.6rem 1.25rem',
                background: 'linear-gradient(135deg, #2563EB, #7C3AED)',
                color: 'white',
                borderRadius: '9999px',
                fontSize: '0.875rem',
                fontWeight: 600,
                textDecoration: 'none',
                boxShadow: '0 4px 20px rgba(37,99,235,0.35)',
              }}
            >
              Get Started <ArrowRight size={14} />
            </Link>
          </div>
        </div>
      </nav>

      {/* ── Hero ── */}
      <section
        style={{
          position: 'relative',
          paddingTop: '9rem',
          paddingBottom: '4rem',
          paddingLeft: '1.5rem',
          paddingRight: '1.5rem',
        }}
      >
        {/* Background glows */}
        <div
          style={{
            position: 'absolute',
            top: '-4rem',
            left: '50%',
            transform: 'translateX(-60%)',
            width: '900px',
            height: '700px',
            background:
              'radial-gradient(ellipse at center, rgba(37,99,235,0.18), transparent 65%)',
            pointerEvents: 'none',
            zIndex: 0,
          }}
        />
        <div
          style={{
            position: 'absolute',
            top: '10rem',
            right: '-8rem',
            width: '600px',
            height: '500px',
            background:
              'radial-gradient(ellipse at center, rgba(124,58,237,0.14), transparent 65%)',
            pointerEvents: 'none',
            zIndex: 0,
          }}
        />

        <div
          style={{
            maxWidth: '78rem',
            margin: '0 auto',
            position: 'relative',
            zIndex: 10,
          }}
        >
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7, ease: [0.22, 1, 0.36, 1] }}
            style={{ textAlign: 'center' }}
          >
            <h1
              style={{
                fontSize: 'clamp(2.8rem, 7vw, 5.5rem)',
                fontWeight: 900,
                letterSpacing: '-0.04em',
                lineHeight: 1.05,
                marginBottom: '1.75rem',
                color: 'white',
              }}
            >
              Reconcile every
              <br />
              <span style={{ color: '#60A5FA' }}>naira &amp; dollar</span>{' '}
              instantly.
            </h1>

            <p
              style={{
                fontSize: '1.2rem',
                color: '#9CA3AF',
                maxWidth: '38rem',
                margin: '0 auto 2.5rem',
                lineHeight: 1.7,
              }}
            >
              NexLedger automatically matches transactions across all your banks
              and payment gateways — flagging discrepancies before they become
              problems.
            </p>

            <div
              style={{
                display: 'flex',
                flexWrap: 'wrap',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '1rem',
                marginBottom: '4rem',
              }}
            >
              <Link
                to="/login"
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                  padding: '1rem 2rem',
                  background: 'linear-gradient(135deg, #2563EB, #7C3AED)',
                  color: 'white',
                  borderRadius: '9999px',
                  fontWeight: 700,
                  textDecoration: 'none',
                  boxShadow: '0 8px 32px rgba(37,99,235,0.4)',
                  fontSize: '1rem',
                }}
              >
                Start Free Trial <ChevronRight size={18} />
              </Link>
            </div>

            <p
              style={{
                fontSize: '0.8rem',
                color: '#4B5563',
                letterSpacing: '0.06em',
                marginBottom: '4rem',
              }}
            >
              TRUSTED BY FINANCE TEAMS AT &nbsp;·&nbsp; ACCESS BANK &nbsp;·&nbsp; ZENITH
              &nbsp;·&nbsp; PAYSTACK &nbsp;·&nbsp; FLUTTERWAVE &nbsp;·&nbsp; GTB
            </p>
          </motion.div>

          {/* ── Dashboard mock ── */}
          <motion.div
            initial={{ opacity: 0, y: 60, scale: 0.96 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            transition={{ duration: 0.9, delay: 0.25, ease: [0.22, 1, 0.36, 1] }}
            style={{ maxWidth: '70rem', margin: '0 auto' }}
          >
            <div
              style={{
                borderRadius: '1.25rem',
                border: '1px solid rgba(255,255,255,0.1)',
                overflow: 'hidden',
                boxShadow: '0 40px 80px -20px rgba(0,0,0,0.7)',
                background: 'linear-gradient(160deg, #111118, #0D0D14)',
              }}
            >
              {/* Browser chrome */}
              <div
                style={{
                  padding: '1rem 1.5rem',
                  borderBottom: '1px solid rgba(255,255,255,0.06)',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '1rem',
                  backgroundColor: 'rgba(255,255,255,0.02)',
                }}
              >
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  {['#EF4444', '#F59E0B', '#10B981'].map((c) => (
                    <div
                      key={c}
                      style={{
                        width: '0.65rem',
                        height: '0.65rem',
                        borderRadius: '50%',
                        backgroundColor: c,
                        opacity: 0.7,
                      }}
                    />
                  ))}
                </div>
                <div
                  style={{
                    flex: 1,
                    height: '1.5rem',
                    backgroundColor: 'rgba(255,255,255,0.05)',
                    borderRadius: '0.35rem',
                    display: 'flex',
                    alignItems: 'center',
                    paddingLeft: '0.75rem',
                  }}
                >
                  <span style={{ fontSize: '0.7rem', color: '#4B5563' }}>
                    app.nexledger.io/dashboard
                  </span>
                </div>
              </div>

              {/* Content */}
              <div
                style={{
                  padding: '2rem',
                  display: 'grid',
                  gridTemplateColumns: '3fr 2fr',
                  gap: '1.5rem',
                  minHeight: '28rem',
                }}
              >
                {/* Left column */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
                  <div
                    style={{
                      display: 'grid',
                      gridTemplateColumns: 'repeat(3, 1fr)',
                      gap: '1rem',
                    }}
                  >
                    {[
                      { label: 'Matched', value: '₦845M', color: '#22C55E', icon: CheckCircle2 },
                      { label: 'Pending', value: '23', color: '#F59E0B', icon: Activity },
                      { label: 'Exceptions', value: '3', color: '#EF4444', icon: AlertTriangle },
                    ].map(({ label, value, color, icon: Icon }) => (
                      <div
                        key={label}
                        style={{
                          backgroundColor: 'rgba(255,255,255,0.04)',
                          borderRadius: '0.875rem',
                          padding: '1rem',
                          border: '1px solid rgba(255,255,255,0.06)',
                        }}
                      >
                        <div
                          style={{
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'flex-start',
                            marginBottom: '0.5rem',
                          }}
                        >
                          <span style={{ fontSize: '0.7rem', color: '#6B7280', fontWeight: 600 }}>
                            {label}
                          </span>
                          <Icon size={14} color={color} />
                        </div>
                        <div
                          style={{
                            fontSize: '1.4rem',
                            fontWeight: 800,
                            color,
                            letterSpacing: '-0.02em',
                          }}
                        >
                          {value}
                        </div>
                      </div>
                    ))}
                  </div>

                  {/* Bar chart */}
                  <div
                    style={{
                      flex: 1,
                      backgroundColor: 'rgba(255,255,255,0.03)',
                      borderRadius: '0.875rem',
                      border: '1px solid rgba(255,255,255,0.06)',
                      padding: '1.25rem',
                      position: 'relative',
                      overflow: 'hidden',
                    }}
                  >
                    <div
                      style={{
                        fontSize: '0.75rem',
                        color: '#6B7280',
                        fontWeight: 600,
                        marginBottom: '1rem',
                      }}
                    >
                      RECONCILIATION VOLUME
                    </div>
                    <div
                      style={{
                        display: 'flex',
                        alignItems: 'flex-end',
                        gap: '0.5rem',
                        height: '6rem',
                      }}
                    >
                      {[55, 80, 45, 90, 70, 95, 60, 85, 40, 100, 75, 88].map((h, i) => (
                        <div
                          key={i}
                          style={{
                            flex: 1,
                            background:
                              i === 9
                                ? 'linear-gradient(to top, #2563EB, #7C3AED)'
                                : 'rgba(37,99,235,0.25)',
                            height: `${h}%`,
                            borderRadius: '0.25rem 0.25rem 0 0',
                          }}
                        />
                      ))}
                    </div>
                    <div
                      style={{
                        position: 'absolute',
                        bottom: '1rem',
                        right: '1rem',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '0.3rem',
                      }}
                    >
                      <TrendingUp size={12} color="#22C55E" />
                      <span style={{ fontSize: '0.7rem', color: '#22C55E', fontWeight: 600 }}>
                        +3.1% this week
                      </span>
                    </div>
                  </div>
                </div>

                {/* Right column — recent matches */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                  <div
                    style={{
                      fontSize: '0.7rem',
                      color: '#6B7280',
                      fontWeight: 600,
                      letterSpacing: '0.06em',
                    }}
                  >
                    RECENT MATCHES
                  </div>
                  {[
                    { ref: 'TXN-0041', amount: '₦250,000', bank: 'GTBank', status: 'matched' },
                    { ref: 'TXN-0042', amount: '$4,800', bank: 'Paystack', status: 'matched' },
                    { ref: 'TXN-0043', amount: '₦1,100,000', bank: 'Access', status: 'exception' },
                    { ref: 'TXN-0044', amount: '€2,300', bank: 'Flutterwave', status: 'matched' },
                    { ref: 'TXN-0045', amount: '₦95,000', bank: 'Zenith', status: 'pending' },
                  ].map(({ ref, amount, bank, status }) => (
                    <div
                      key={ref}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        padding: '0.75rem',
                        backgroundColor: 'rgba(255,255,255,0.03)',
                        borderRadius: '0.625rem',
                        border: '1px solid rgba(255,255,255,0.05)',
                      }}
                    >
                      <div>
                        <div style={{ fontSize: '0.75rem', fontWeight: 600, color: '#D1D5DB' }}>
                          {ref}
                        </div>
                        <div style={{ fontSize: '0.65rem', color: '#6B7280' }}>{bank}</div>
                      </div>
                      <div style={{ textAlign: 'right' }}>
                        <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'white' }}>
                          {amount}
                        </div>
                        <div
                          style={{
                            fontSize: '0.65rem',
                            fontWeight: 600,
                            color:
                              status === 'matched'
                                ? '#22C55E'
                                : status === 'exception'
                                ? '#EF4444'
                                : '#F59E0B',
                          }}
                        >
                          {status}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </motion.div>
        </div>
      </section>

      {/* ── Live Ticker ── */}
      <LiveTicker />

      {/* ── Stats ── */}
      <section style={{ padding: '6rem 1.5rem' }}>
        <div style={{ maxWidth: '78rem', margin: '0 auto' }}>
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
              gap: '3rem 2rem',
            }}
          >
            <StatCounter target={94} suffix="%" label="Auto-match accuracy" />
            <StatCounter target={8} prefix="₦" suffix="B+" label="Volume reconciled monthly" />
            <StatCounter target={6} label="Nigerian banks integrated" />
            <StatCounter target={4} suffix="s" label="Average match latency" />
          </div>
        </div>
      </section>

      {/* ── Features ── */}
      <section id="features" style={{ padding: '6rem 1.5rem 8rem', backgroundColor: '#05050D' }}>
        <div style={{ maxWidth: '78rem', margin: '0 auto' }}>
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.6 }}
            style={{ marginBottom: '4rem' }}
          >
            <div
              style={{
                display: 'inline-block',
                padding: '0.25rem 0.75rem',
                borderRadius: '9999px',
                border: '1px solid rgba(167,139,250,0.3)',
                backgroundColor: 'rgba(167,139,250,0.08)',
                marginBottom: '1.25rem',
              }}
            >
              <span
                style={{
                  fontSize: '0.7rem',
                  fontWeight: 700,
                  color: '#A78BFA',
                  letterSpacing: '0.08em',
                }}
              >
                WHAT WE DO
              </span>
            </div>
            <h2
              style={{
                fontSize: 'clamp(2rem, 4vw, 3rem)',
                fontWeight: 800,
                letterSpacing: '-0.03em',
                color: 'white',
                marginBottom: '1rem',
              }}
            >
              One platform. Every reconciliation.
            </h2>
            <p style={{ fontSize: '1.125rem', color: '#6B7280', maxWidth: '36rem' }}>
              Built specifically for Nigerian fintechs and banks handling high transaction volumes
              every day.
            </p>
          </motion.div>

          <div
            style={{ display: 'grid', gridTemplateColumns: 'repeat(12, 1fr)', gap: '1.25rem' }}
          >
            {/* Card 1 — Real-time Matching */}
            <motion.div
              initial={{ opacity: 0, y: 24 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.55 }}
              style={{
                gridColumn: 'span 7',
                backgroundColor: '#0F0F1A',
                borderRadius: '1.5rem',
                border: '1px solid rgba(255,255,255,0.08)',
                padding: '2.5rem',
                position: 'relative',
                overflow: 'hidden',
              }}
            >
              <div
                style={{
                  position: 'absolute',
                  top: 0,
                  right: 0,
                  width: '20rem',
                  height: '20rem',
                  background: 'radial-gradient(ellipse, rgba(37,99,235,0.12), transparent 70%)',
                  pointerEvents: 'none',
                }}
              />
              <div
                style={{
                  width: '3rem',
                  height: '3rem',
                  borderRadius: '0.875rem',
                  backgroundColor: 'rgba(37,99,235,0.15)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  marginBottom: '1.5rem',
                  border: '1px solid rgba(37,99,235,0.25)',
                }}
              >
                <RefreshCw size={22} color="#60A5FA" />
              </div>
              <h3
                style={{
                  fontSize: '1.5rem',
                  fontWeight: 800,
                  marginBottom: '0.75rem',
                  color: 'white',
                  letterSpacing: '-0.02em',
                }}
              >
                Real-time Matching Engine
              </h3>
              <p
                style={{
                  color: '#6B7280',
                  lineHeight: 1.7,
                  maxWidth: '30rem',
                  marginBottom: '2rem',
                }}
              >
                Our engine ingests raw CSV exports from any Nigerian bank or payment processor and
                cross-references them against your internal ledger — in under 3 seconds.
              </p>
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '1rem',
                  padding: '1.5rem',
                  backgroundColor: 'rgba(0,0,0,0.3)',
                  borderRadius: '1rem',
                  border: '1px solid rgba(255,255,255,0.05)',
                }}
              >
                {[
                  { Icon: Building, label: 'Bank CSV', color: '#60A5FA' },
                  { Icon: CreditCard, label: 'Gateway', color: '#A78BFA' },
                  { Icon: Database, label: 'Ledger', color: '#F472B6' },
                ].map(({ Icon, label, color }, i) => (
                  <React.Fragment key={label}>
                    <div
                      style={{
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        gap: '0.4rem',
                      }}
                    >
                      <div
                        style={{
                          padding: '0.75rem',
                          backgroundColor: `${color}18`,
                          borderRadius: '0.75rem',
                          border: `1px solid ${color}30`,
                        }}
                      >
                        <Icon size={20} color={color} />
                      </div>
                      <span style={{ fontSize: '0.65rem', color: '#6B7280', fontWeight: 600 }}>
                        {label}
                      </span>
                    </div>
                    {i < 2 && (
                      <div
                        style={{
                          flex: 1,
                          height: '1px',
                          backgroundColor: 'rgba(255,255,255,0.1)',
                        }}
                      />
                    )}
                  </React.Fragment>
                ))}
                <ArrowRight size={18} color="rgba(255,255,255,0.3)" />
                <div
                  style={{
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    gap: '0.4rem',
                  }}
                >
                  <div
                    style={{
                      padding: '0.75rem',
                      backgroundColor: 'rgba(34,197,94,0.15)',
                      borderRadius: '0.75rem',
                      border: '1px solid rgba(34,197,94,0.3)',
                    }}
                  >
                    <CheckCircle2 size={20} color="#22C55E" />
                  </div>
                  <span style={{ fontSize: '0.65rem', color: '#22C55E', fontWeight: 700 }}>
                    MATCHED
                  </span>
                </div>
              </div>
            </motion.div>

            {/* Card 2 — Security */}
            <motion.div
              initial={{ opacity: 0, y: 24 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.55, delay: 0.1 }}
              style={{
                gridColumn: 'span 5',
                backgroundColor: '#0F0F1A',
                borderRadius: '1.5rem',
                border: '1px solid rgba(255,255,255,0.08)',
                padding: '2.5rem',
                position: 'relative',
                overflow: 'hidden',
              }}
            >

              <div
                style={{
                  width: '3rem',
                  height: '3rem',
                  borderRadius: '0.875rem',
                  backgroundColor: 'rgba(124,58,237,0.15)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  marginBottom: '1.5rem',
                  border: '1px solid rgba(124,58,237,0.25)',
                }}
              >
                <Shield size={22} color="#A78BFA" />
              </div>
              <h3
                style={{
                  fontSize: '1.5rem',
                  fontWeight: 800,
                  marginBottom: '0.75rem',
                  color: 'white',
                  letterSpacing: '-0.02em',
                }}
              >
                Bank-Grade Security
              </h3>
              <p style={{ color: '#6B7280', lineHeight: 1.7, marginBottom: '2rem' }}>
                SOC 2 Type II certified. All data encrypted end-to-end, at rest and in transit.
              </p>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                {['AES-256 Encryption', 'Immutable Audit Logs', 'Role-Based Access', 'SSO / 2FA Support'].map(
                  (item) => (
                    <div
                      key={item}
                      style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}
                    >
                      <CheckCircle2 size={16} color="#22C55E" />
                      <span style={{ fontSize: '0.875rem', color: '#D1D5DB' }}>{item}</span>
                    </div>
                  )
                )}
              </div>
            </motion.div>

            {/* Card 3 — Analytics */}
            <motion.div
              initial={{ opacity: 0, y: 24 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.55, delay: 0.15 }}
              style={{
                gridColumn: 'span 4',
                backgroundColor: '#0F0F1A',
                borderRadius: '1.5rem',
                border: '1px solid rgba(255,255,255,0.08)',
                padding: '2rem',
                position: 'relative',
                overflow: 'hidden',
              }}
            >
              <div
                style={{
                  width: '2.75rem',
                  height: '2.75rem',
                  borderRadius: '0.875rem',
                  backgroundColor: 'rgba(255,255,255,0.06)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  marginBottom: '1.25rem',
                  border: '1px solid rgba(255,255,255,0.1)',
                }}
              >
                <BarChart3 size={20} color="#9CA3AF" />
              </div>
              <h3
                style={{
                  fontSize: '1.25rem',
                  fontWeight: 800,
                  marginBottom: '0.5rem',
                  color: 'white',
                  letterSpacing: '-0.02em',
                }}
              >
                Deep Analytics
              </h3>
              <p style={{ color: '#6B7280', fontSize: '0.9rem', lineHeight: 1.6 }}>
                Daily, weekly, and monthly reconciliation reports with drill-down capability.
              </p>
              <div
                style={{
                  marginTop: '1.5rem',
                  padding: '0.75rem 1rem',
                  backgroundColor: 'rgba(255,255,255,0.04)',
                  borderRadius: '0.75rem',
                  border: '1px solid rgba(255,255,255,0.08)',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                }}
              >
                <TrendingUp size={16} color="#6B7280" />
                <span style={{ fontSize: '0.8rem', color: '#9CA3AF', fontWeight: 600 }}>
                  94.2% accuracy this month
                </span>
              </div>
            </motion.div>

            {/* Card 4 — Multi-source */}
            <motion.div
              initial={{ opacity: 0, y: 24 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.55, delay: 0.2 }}
              style={{
                gridColumn: 'span 4',
                backgroundColor: '#0F0F1A',
                borderRadius: '1.5rem',
                border: '1px solid rgba(255,255,255,0.08)',
                padding: '2rem',
                position: 'relative',
                overflow: 'hidden',
              }}
            >
              <div
                style={{
                  width: '2.75rem',
                  height: '2.75rem',
                  borderRadius: '0.875rem',
                  backgroundColor: 'rgba(255,255,255,0.06)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  marginBottom: '1.25rem',
                  border: '1px solid rgba(255,255,255,0.1)',
                }}
              >
                <Globe size={20} color="#9CA3AF" />
              </div>
              <h3
                style={{
                  fontSize: '1.25rem',
                  fontWeight: 800,
                  marginBottom: '0.5rem',
                  color: 'white',
                  letterSpacing: '-0.02em',
                }}
              >
                Multi-Source Ingestion
              </h3>
              <p style={{ color: '#6B7280', fontSize: '0.9rem', lineHeight: 1.6 }}>
                Connect GTB, Access, Zenith, Paystack, Flutterwave, and more — all natively
                supported.
              </p>
              <div
                style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem', marginTop: '1.5rem' }}
              >
                {['GTB', 'Access', 'Zenith', 'Paystack', 'FW', 'UBA'].map((b) => (
                  <span
                    key={b}
                    style={{
                      padding: '0.25rem 0.6rem',
                      backgroundColor: 'rgba(255,255,255,0.05)',
                      borderRadius: '0.375rem',
                      fontSize: '0.7rem',
                      fontWeight: 600,
                      color: '#9CA3AF',
                      border: '1px solid rgba(255,255,255,0.08)',
                    }}
                  >
                    {b}
                  </span>
                ))}
              </div>
            </motion.div>

            {/* Card 5 — AI Dispute */}
            <motion.div
              initial={{ opacity: 0, y: 24 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.55, delay: 0.25 }}
              style={{
                gridColumn: 'span 4',
                backgroundColor: '#0F0F1A',
                borderRadius: '1.5rem',
                border: '1px solid rgba(255,255,255,0.08)',
                padding: '2rem',
                position: 'relative',
                overflow: 'hidden',
              }}
            >
              <div
                style={{
                  width: '2.75rem',
                  height: '2.75rem',
                  borderRadius: '0.875rem',
                  backgroundColor: 'rgba(255,255,255,0.06)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  marginBottom: '1.25rem',
                  border: '1px solid rgba(255,255,255,0.1)',
                }}
              >
                <Layers size={20} color="#9CA3AF" />
              </div>
              <h3
                style={{
                  fontSize: '1.25rem',
                  fontWeight: 800,
                  marginBottom: '0.5rem',
                  color: 'white',
                  letterSpacing: '-0.02em',
                }}
              >
                AI Dispute Resolution
              </h3>
              <p style={{ color: '#6B7280', fontSize: '0.9rem', lineHeight: 1.6 }}>
                Exceptions are automatically triaged and routed with suggested resolutions.
              </p>
              <div
                style={{
                  marginTop: '1.5rem',
                  height: '0.5rem',
                  backgroundColor: 'rgba(255,255,255,0.05)',
                  borderRadius: '9999px',
                  overflow: 'hidden',
                }}
              >
                <div
                  style={{
                    height: '100%',
                    width: '78%',
                    backgroundColor: '#22C55E',
                    borderRadius: '9999px',
                  }}
                />
              </div>
              <div style={{ marginTop: '0.5rem', fontSize: '0.7rem', color: '#22C55E', fontWeight: 600 }}>
                78% auto-resolved
              </div>
            </motion.div>
          </div>
        </div>
      </section>

      {/* ── How it Works ── */}
      <section id="how-it-works" style={{ padding: '8rem 1.5rem', backgroundColor: '#080810' }}>
        <div style={{ maxWidth: '78rem', margin: '0 auto' }}>
          <motion.div
            initial={{ opacity: 0, y: 16 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            style={{ marginBottom: '5rem', textAlign: 'center' }}
          >
            <div
              style={{
                display: 'inline-block',
                padding: '0.25rem 0.75rem',
                borderRadius: '9999px',
                border: '1px solid rgba(96,165,250,0.3)',
                backgroundColor: 'rgba(96,165,250,0.08)',
                marginBottom: '1.25rem',
              }}
            >
              <span
                style={{
                  fontSize: '0.7rem',
                  fontWeight: 700,
                  color: '#60A5FA',
                  letterSpacing: '0.08em',
                }}
              >
                THE PROCESS
              </span>
            </div>
            <h2
              style={{
                fontSize: 'clamp(1.875rem, 4vw, 3rem)',
                fontWeight: 800,
                letterSpacing: '-0.03em',
                color: 'white',
              }}
            >
              Up and running in minutes.
            </h2>
          </motion.div>

          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))',
              gap: '1.5rem',
            }}
          >
            {[
              {
                step: '01',
                Icon: Lock,
                color: '#60A5FA',
                bg: 'rgba(37,99,235,0.12)',
                title: 'Connect your sources',
                desc: 'Upload CSV exports from any bank, or connect directly via API. We support all major Nigerian banks out of the box.',
              },
              {
                step: '02',
                Icon: RefreshCw,
                color: '#A78BFA',
                bg: 'rgba(124,58,237,0.12)',
                title: 'Let the engine run',
                desc: 'NexLedger cross-checks every entry across your sources and flags mismatches with zero manual effort.',
              },
              {
                step: '03',
                Icon: AlertTriangle,
                color: '#F59E0B',
                bg: 'rgba(245,158,11,0.12)',
                title: 'Review exceptions',
                desc: 'The small fraction of unresolved items are queued with AI suggestions so your team resolves them fast.',
              },
              {
                step: '04',
                Icon: FileText,
                color: '#22C55E',
                bg: 'rgba(34,197,94,0.12)',
                title: 'Export clean reports',
                desc: 'Download audit-ready reconciliation reports for any period, formatted for regulators or internal review.',
              },
            ].map(({ step, Icon, color, bg, title, desc }, idx) => (
              <motion.div
                key={step}
                initial={{ opacity: 0, y: 24 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: idx * 0.12, duration: 0.5 }}
                style={{
                  padding: '2rem',
                  backgroundColor: '#0F0F1A',
                  borderRadius: '1.5rem',
                  border: '1px solid rgba(255,255,255,0.07)',
                }}
              >
                <div
                  style={{
                    fontSize: '0.65rem',
                    fontWeight: 800,
                    color,
                    letterSpacing: '0.12em',
                    marginBottom: '1.5rem',
                    opacity: 0.8,
                  }}
                >
                  STEP {step}
                </div>
                <div
                  style={{
                    width: '3rem',
                    height: '3rem',
                    borderRadius: '0.875rem',
                    backgroundColor: bg,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    marginBottom: '1.25rem',
                    border: `1px solid ${color}30`,
                  }}
                >
                  <Icon size={20} color={color} />
                </div>
                <h3
                  style={{
                    fontSize: '1.125rem',
                    fontWeight: 700,
                    color: 'white',
                    marginBottom: '0.75rem',
                    letterSpacing: '-0.01em',
                  }}
                >
                  {title}
                </h3>
                <p style={{ fontSize: '0.875rem', color: '#6B7280', lineHeight: 1.65 }}>{desc}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* ── CTA Banner ── */}
      <section style={{ padding: '6rem 1.5rem' }}>
        <div style={{ maxWidth: '60rem', margin: '0 auto' }}>
          <motion.div
            initial={{ opacity: 0, scale: 0.97 }}
            whileInView={{ opacity: 1, scale: 1 }}
            viewport={{ once: true }}
            transition={{ duration: 0.6 }}
            style={{
              borderRadius: '2rem',
              padding: '4rem',
              textAlign: 'center',
              position: 'relative',
              overflow: 'hidden',
              border: '1px solid rgba(96,165,250,0.2)',
              background:
                'linear-gradient(135deg, rgba(37,99,235,0.15) 0%, rgba(124,58,237,0.15) 100%)',
            }}
          >
            <div
              style={{
                position: 'absolute',
                top: '-4rem',
                right: '-4rem',
                width: '20rem',
                height: '20rem',
                background: 'radial-gradient(ellipse, rgba(124,58,237,0.3), transparent 70%)',
                pointerEvents: 'none',
              }}
            />
            <div
              style={{
                position: 'absolute',
                bottom: '-4rem',
                left: '-4rem',
                width: '18rem',
                height: '18rem',
                background: 'radial-gradient(ellipse, rgba(37,99,235,0.3), transparent 70%)',
                pointerEvents: 'none',
              }}
            />
            <div style={{ position: 'relative', zIndex: 10 }}>
              <h2
                style={{
                  fontSize: 'clamp(2rem, 5vw, 3.5rem)',
                  fontWeight: 900,
                  letterSpacing: '-0.03em',
                  color: 'white',
                  marginBottom: '1.25rem',
                  lineHeight: 1.1,
                }}
              >
                Close your books.
                <br />
                <span style={{ color: '#60A5FA' }}>Not the weekend.</span>
              </h2>
              <p
                style={{
                  fontSize: '1.125rem',
                  color: 'rgba(209,213,219,0.85)',
                  marginBottom: '2.5rem',
                  maxWidth: '36rem',
                  margin: '0 auto 2.5rem',
                  lineHeight: 1.6,
                }}
              >
                Finance teams that switch to NexLedger cut reconciliation time by 97%. Join them.
              </p>
              <Link
                to="/login"
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                  padding: '1rem 2rem',
                  backgroundColor: 'white',
                  color: '#1E1B4B',
                  borderRadius: '9999px',
                  fontWeight: 700,
                  textDecoration: 'none',
                  fontSize: '1rem',
                  boxShadow: '0 4px 20px rgba(0,0,0,0.3)',
                }}
              >
                Get Started Now <ArrowUpRight size={18} />
              </Link>
            </div>
          </motion.div>
        </div>
      </section>

      {/* ── Footer ── */}
      <footer
        style={{
          padding: '3rem 1.5rem',
          borderTop: '1px solid rgba(255,255,255,0.05)',
          backgroundColor: '#050509',
        }}
      >
        <div
          style={{
            maxWidth: '78rem',
            margin: '0 auto',
            display: 'flex',
            flexWrap: 'wrap',
            justifyContent: 'space-between',
            alignItems: 'center',
            gap: '2rem',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
            <NexLedgerLogo size={28} />
            <span
              style={{
                fontWeight: 700,
                fontSize: '1rem',
                letterSpacing: '-0.02em',
                color: '#D1D5DB',
              }}
            >
              Nex<span style={{ color: '#60A5FA' }}>Ledger</span>
            </span>
          </div>
          <div style={{ display: 'flex', gap: '2rem' }}>
            {['Privacy', 'Terms', 'Security', 'Status'].map((l) => (
              <a
                key={l}
                href="#"
                style={{ fontSize: '0.875rem', color: '#4B5563', textDecoration: 'none' }}
              >
                {l}
              </a>
            ))}
          </div>
          <div style={{ fontSize: '0.8rem', color: '#374151' }}>
            © 2026 NexLedger Inc. All rights reserved.
          </div>
        </div>
      </footer>
    </div>
  );
};
