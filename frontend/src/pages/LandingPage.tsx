import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { motion, useScroll, useTransform } from 'framer-motion';
import { 
  ArrowRight, 
  CheckCircle2, 
  Shield, 
  Zap, 
  BarChart3, 
  Globe, 
  Lock, 
  ChevronRight,
  PlayCircle
} from 'lucide-react';

// Inline styles to ensure they work regardless of Tailwind config
const styles = {
  page: {
    minHeight: '100vh',
    backgroundColor: '#0A0A0B',
    color: 'white',
    overflowX: 'hidden' as const,
    fontFamily: '-apple-system, BlinkMacSystemFont, "SF Pro Display", "Segoe UI", Roboto, sans-serif',
  },
  nav: {
    position: 'fixed' as const,
    top: 0,
    width: '100%',
    zIndex: 50,
    transition: 'all 0.3s',
    padding: '0 1.5rem',
  },
  navScrolled: {
    backgroundColor: 'rgba(10, 10, 11, 0.8)',
    backdropFilter: 'blur(20px)',
    borderBottom: '1px solid rgba(255,255,255,0.05)',
  },
  navInner: {
    maxWidth: '80rem',
    margin: '0 auto',
    height: '5rem',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  logo: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.5rem',
  },
  logoIcon: {
    width: '2rem',
    height: '2rem',
    background: 'linear-gradient(135deg, #3B82F6, #9333EA)',
    borderRadius: '0.5rem',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontWeight: 'bold',
    color: 'white',
  },
  logoText: {
    fontSize: '1.25rem',
    fontWeight: 'bold',
    letterSpacing: '-0.02em',
  },
  navLinks: {
    display: 'flex',
    alignItems: 'center',
    gap: '2rem',
  },
  navLink: {
    fontSize: '0.875rem',
    fontWeight: 500,
    color: '#9CA3AF',
    textDecoration: 'none',
    transition: 'color 0.2s',
  },
  ctaButton: {
    display: 'inline-flex',
    alignItems: 'center',
    gap: '0.5rem',
    padding: '0.625rem 1.25rem',
    backgroundColor: 'white',
    color: 'black',
    borderRadius: '9999px',
    fontSize: '0.875rem',
    fontWeight: 600,
    textDecoration: 'none',
    transition: 'all 0.2s',
  },
  hero: {
    position: 'relative' as const,
    paddingTop: '10rem',
    paddingBottom: '5rem',
    paddingLeft: '1.5rem',
    paddingRight: '1.5rem',
  },
  heroGlow1: {
    position: 'absolute' as const,
    top: 0,
    left: '50%',
    transform: 'translateX(-50%)',
    width: '1000px',
    height: '600px',
    background: 'radial-gradient(ellipse, rgba(59, 130, 246, 0.2), transparent 70%)',
    pointerEvents: 'none' as const,
  },
  heroGlow2: {
    position: 'absolute' as const,
    bottom: 0,
    right: 0,
    width: '800px',
    height: '600px',
    background: 'radial-gradient(ellipse, rgba(147, 51, 234, 0.15), transparent 70%)',
    pointerEvents: 'none' as const,
  },
  heroContent: {
    maxWidth: '80rem',
    margin: '0 auto',
    position: 'relative' as const,
    zIndex: 10,
    textAlign: 'center' as const,
  },
  badge: {
    display: 'inline-flex',
    alignItems: 'center',
    gap: '0.5rem',
    padding: '0.25rem 0.75rem',
    borderRadius: '9999px',
    backgroundColor: 'rgba(255,255,255,0.05)',
    border: '1px solid rgba(255,255,255,0.1)',
    marginBottom: '2rem',
  },
  badgeDot: {
    width: '0.5rem',
    height: '0.5rem',
    borderRadius: '50%',
    backgroundColor: '#3B82F6',
    animation: 'pulse 2s infinite',
  },
  badgeText: {
    fontSize: '0.75rem',
    fontWeight: 500,
    color: '#93C5FD',
  },
  heroTitle: {
    fontSize: 'clamp(2.5rem, 8vw, 4.5rem)',
    fontWeight: 'bold',
    letterSpacing: '-0.02em',
    marginBottom: '1.5rem',
    lineHeight: 1.1,
    color: 'white',
  },
  heroSubtitle: {
    fontSize: '1.25rem',
    color: '#9CA3AF',
    marginBottom: '2.5rem',
    maxWidth: '42rem',
    margin: '0 auto 2.5rem',
    lineHeight: 1.6,
  },
  heroButtons: {
    display: 'flex',
    flexWrap: 'wrap' as const,
    alignItems: 'center',
    justifyContent: 'center',
    gap: '1rem',
  },
  primaryButton: {
    display: 'inline-flex',
    alignItems: 'center',
    gap: '0.5rem',
    padding: '1rem 2rem',
    backgroundColor: '#2563EB',
    color: 'white',
    borderRadius: '9999px',
    fontWeight: 600,
    textDecoration: 'none',
    boxShadow: '0 10px 40px rgba(37, 99, 235, 0.3)',
    transition: 'all 0.2s',
    border: 'none',
    cursor: 'pointer',
  },
  secondaryButton: {
    display: 'inline-flex',
    alignItems: 'center',
    gap: '0.5rem',
    padding: '1rem 2rem',
    backgroundColor: 'rgba(255,255,255,0.05)',
    color: 'white',
    borderRadius: '9999px',
    fontWeight: 600,
    textDecoration: 'none',
    border: '1px solid rgba(255,255,255,0.1)',
    cursor: 'pointer',
    transition: 'all 0.2s',
  },
  dashboardPreview: {
    marginTop: '4rem',
    maxWidth: '72rem',
    margin: '4rem auto 0',
  },
  dashboardFrame: {
    backgroundColor: '#1A1A1C',
    borderRadius: '0.75rem',
    border: '1px solid rgba(255,255,255,0.1)',
    boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.5)',
    overflow: 'hidden',
    aspectRatio: '16/9',
  },
  dashboardHeader: {
    padding: '1rem 1.5rem',
    borderBottom: '1px solid rgba(255,255,255,0.05)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  windowDots: {
    display: 'flex',
    gap: '0.5rem',
  },
  windowDot: {
    width: '0.75rem',
    height: '0.75rem',
    borderRadius: '50%',
  },
  dashboardBody: {
    padding: '2rem',
    display: 'grid',
    gridTemplateColumns: '2fr 1fr',
    gap: '2rem',
  },
  socialProof: {
    padding: '2.5rem 1.5rem',
    borderTop: '1px solid rgba(255,255,255,0.05)',
    borderBottom: '1px solid rgba(255,255,255,0.05)',
    backgroundColor: 'rgba(255,255,255,0.02)',
  },
  socialProofInner: {
    maxWidth: '80rem',
    margin: '0 auto',
    textAlign: 'center' as const,
  },
  socialProofLabel: {
    fontSize: '0.75rem',
    fontWeight: 500,
    color: '#6B7280',
    letterSpacing: '0.1em',
    marginBottom: '1.5rem',
  },
  socialProofLogos: {
    display: 'flex',
    flexWrap: 'wrap' as const,
    justifyContent: 'center',
    gap: '3rem',
    opacity: 0.5,
  },
  socialProofLogo: {
    fontSize: '1.25rem',
    fontWeight: 'bold',
    color: 'white',
  },
  features: {
    padding: '8rem 1.5rem',
  },
  featuresInner: {
    maxWidth: '80rem',
    margin: '0 auto',
  },
  sectionHeader: {
    textAlign: 'center' as const,
    marginBottom: '5rem',
  },
  sectionTitle: {
    fontSize: 'clamp(1.875rem, 5vw, 3rem)',
    fontWeight: 'bold',
    marginBottom: '1rem',
    color: 'white',
  },
  sectionSubtitle: {
    fontSize: '1.25rem',
    color: '#9CA3AF',
    maxWidth: '42rem',
    margin: '0 auto',
  },
  featureGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
    gap: '1.5rem',
  },
  featureCard: {
    backgroundColor: '#151516',
    borderRadius: '1.5rem',
    border: '1px solid rgba(255,255,255,0.1)',
    padding: '2rem',
    position: 'relative' as const,
    overflow: 'hidden',
    transition: 'transform 0.3s',
  },
  featureCardLarge: {
    gridColumn: 'span 2',
    padding: '3rem',
  },
  featureCardTall: {
    gridRow: 'span 2',
  },
  featureIcon: {
    width: '3rem',
    height: '3rem',
    borderRadius: '0.75rem',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: '1.5rem',
  },
  featureTitle: {
    fontSize: '1.5rem',
    fontWeight: 'bold',
    marginBottom: '0.5rem',
    color: 'white',
  },
  featureDesc: {
    color: '#9CA3AF',
    lineHeight: 1.6,
  },
  howItWorks: {
    padding: '8rem 1.5rem',
    backgroundColor: '#0F0F10',
  },
  howItWorksInner: {
    maxWidth: '80rem',
    margin: '0 auto',
  },
  timeline: {
    position: 'relative' as const,
  },
  timelineLine: {
    position: 'absolute' as const,
    left: '2rem',
    top: 0,
    bottom: 0,
    width: '1px',
    background: 'linear-gradient(to bottom, #3B82F6, #9333EA, transparent)',
    opacity: 0.3,
  },
  timelineItem: {
    position: 'relative' as const,
    paddingLeft: '6rem',
    paddingBottom: '4rem',
  },
  timelineIcon: {
    position: 'absolute' as const,
    left: 0,
    width: '4rem',
    height: '4rem',
    borderRadius: '1rem',
    backgroundColor: '#1A1A1C',
    border: '1px solid rgba(255,255,255,0.1)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
  timelineTitle: {
    fontSize: '1.5rem',
    fontWeight: 'bold',
    marginBottom: '0.5rem',
    color: 'white',
  },
  timelineDesc: {
    color: '#9CA3AF',
    maxWidth: '36rem',
  },
  cta: {
    padding: '8rem 1.5rem',
  },
  ctaCard: {
    maxWidth: '64rem',
    margin: '0 auto',
    borderRadius: '2.5rem',
    background: 'linear-gradient(135deg, #2563EB, #7C3AED)',
    padding: '4rem',
    textAlign: 'center' as const,
    position: 'relative' as const,
    overflow: 'hidden',
  },
  ctaTitle: {
    fontSize: 'clamp(2rem, 5vw, 3.5rem)',
    fontWeight: 'bold',
    marginBottom: '1.5rem',
    color: 'white',
  },
  ctaSubtitle: {
    fontSize: '1.25rem',
    color: 'rgba(219, 234, 254, 0.9)',
    marginBottom: '2.5rem',
    maxWidth: '42rem',
    margin: '0 auto 2.5rem',
  },
  ctaButtons: {
    display: 'flex',
    flexWrap: 'wrap' as const,
    alignItems: 'center',
    justifyContent: 'center',
    gap: '1rem',
  },
  ctaPrimaryButton: {
    padding: '1rem 2rem',
    backgroundColor: 'white',
    color: '#2563EB',
    borderRadius: '9999px',
    fontWeight: 'bold',
    textDecoration: 'none',
    transition: 'all 0.2s',
  },
  ctaSecondaryButton: {
    padding: '1rem 2rem',
    backgroundColor: 'transparent',
    color: 'white',
    borderRadius: '9999px',
    fontWeight: 'bold',
    border: '1px solid rgba(255,255,255,0.3)',
    cursor: 'pointer',
    transition: 'all 0.2s',
  },
  footer: {
    padding: '3rem 1.5rem',
    borderTop: '1px solid rgba(255,255,255,0.05)',
    backgroundColor: '#050505',
  },
  footerInner: {
    maxWidth: '80rem',
    margin: '0 auto',
    display: 'flex',
    flexWrap: 'wrap' as const,
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: '2rem',
  },
  footerLogo: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.5rem',
  },
  footerLogoIcon: {
    width: '1.5rem',
    height: '1.5rem',
    background: 'linear-gradient(135deg, #3B82F6, #9333EA)',
    borderRadius: '0.375rem',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: '0.625rem',
    fontWeight: 'bold',
    color: 'white',
  },
  footerLogoText: {
    fontWeight: 'bold',
    color: '#D1D5DB',
  },
  footerLinks: {
    display: 'flex',
    gap: '2rem',
  },
  footerLink: {
    fontSize: '0.875rem',
    color: '#6B7280',
    textDecoration: 'none',
    transition: 'color 0.2s',
  },
  footerCopy: {
    fontSize: '0.875rem',
    color: '#4B5563',
  },
  checkItem: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.75rem',
    marginBottom: '1rem',
  },
  checkText: {
    fontSize: '0.875rem',
    color: '#D1D5DB',
  },
};

export const LandingPage: React.FC = () => {
  const { scrollY } = useScroll();
  const y1 = useTransform(scrollY, [0, 500], [0, 100]);
  
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const handleScroll = () => setScrolled(window.scrollY > 50);
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <div style={styles.page}>
      {/* Navigation */}
      <nav style={{...styles.nav, ...(scrolled ? styles.navScrolled : {})}}>
        <div style={styles.navInner}>
          <div style={styles.logo}>
            <div style={styles.logoIcon}>R</div>
            <span style={styles.logoText}>ReconPlatform</span>
          </div>
          
          <div style={{...styles.navLinks, display: 'none'}}>
            <a href="#features" style={styles.navLink}>Features</a>
            <a href="#how-it-works" style={styles.navLink}>How it Works</a>
            <a href="#pricing" style={styles.navLink}>Pricing</a>
          </div>

          <div style={{display: 'flex', alignItems: 'center', gap: '1rem'}}>
            <Link to="/login" style={{...styles.navLink, color: 'white'}}>
              Log in
            </Link>
            <Link to="/login" style={styles.ctaButton}>
              Get Started <ArrowRight size={16} />
            </Link>
          </div>
        </div>
      </nav>

      {/* Hero Section */}
      <section style={styles.hero}>
        <div style={styles.heroGlow1} />
        <div style={styles.heroGlow2} />
        
        <div style={styles.heroContent}>
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6 }}
          >
            <div style={styles.badge}>
              <span style={styles.badgeDot} />
              <span style={styles.badgeText}>New: AI-Powered Dispute Resolution</span>
            </div>

            <h1 style={styles.heroTitle}>
              Financial reconciliation<br />
              reimagined for scale.
            </h1>

            <p style={styles.heroSubtitle}>
              Automate 99% of your transaction matching. Detect anomalies in real-time. 
              Close your books faster with the world's most advanced reconciliation engine.
            </p>

            <div style={styles.heroButtons}>
              <Link to="/login" style={styles.primaryButton}>
                Start Free Trial <ChevronRight size={18} />
              </Link>
              <button style={styles.secondaryButton}>
                <PlayCircle size={18} /> Watch Demo
              </button>
            </div>
          </motion.div>

          {/* Dashboard Preview */}
          <motion.div style={styles.dashboardPreview}>
            <div style={styles.dashboardFrame}>
              <div style={styles.dashboardHeader}>
                <div style={styles.windowDots}>
                  <div style={{...styles.windowDot, backgroundColor: 'rgba(239, 68, 68, 0.5)', border: '1px solid rgba(239, 68, 68, 0.8)'}} />
                  <div style={{...styles.windowDot, backgroundColor: 'rgba(234, 179, 8, 0.5)', border: '1px solid rgba(234, 179, 8, 0.8)'}} />
                  <div style={{...styles.windowDot, backgroundColor: 'rgba(34, 197, 94, 0.5)', border: '1px solid rgba(34, 197, 94, 0.8)'}} />
                </div>
                <div style={{height: '0.5rem', width: '8rem', backgroundColor: 'rgba(255,255,255,0.1)', borderRadius: '9999px'}} />
              </div>
              <div style={styles.dashboardBody}>
                <div style={{display: 'flex', flexDirection: 'column', gap: '1rem'}}>
                  <div style={{height: '8rem', borderRadius: '0.5rem', backgroundColor: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.05)', padding: '1rem'}}>
                    <div style={{display: 'flex', justifyContent: 'space-between', marginBottom: '1rem'}}>
                      <div style={{height: '1rem', width: '6rem', backgroundColor: 'rgba(255,255,255,0.1)', borderRadius: '0.25rem'}} />
                      <div style={{padding: '0.25rem 0.75rem', backgroundColor: 'rgba(34, 197, 94, 0.2)', borderRadius: '0.25rem', color: '#4ADE80', fontSize: '0.75rem'}}>Live</div>
                    </div>
                    <div style={{display: 'flex', flexDirection: 'column', gap: '0.5rem'}}>
                      <div style={{height: '0.5rem', width: '100%', backgroundColor: 'rgba(255,255,255,0.05)', borderRadius: '0.25rem'}} />
                      <div style={{height: '0.5rem', width: '75%', backgroundColor: 'rgba(255,255,255,0.05)', borderRadius: '0.25rem'}} />
                      <div style={{height: '0.5rem', width: '50%', backgroundColor: 'rgba(255,255,255,0.05)', borderRadius: '0.25rem'}} />
                    </div>
                  </div>
                  <div style={{display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem'}}>
                    <div style={{height: '6rem', borderRadius: '0.5rem', backgroundColor: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.05)'}} />
                    <div style={{height: '6rem', borderRadius: '0.5rem', backgroundColor: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.05)'}} />
                  </div>
                </div>
                <div style={{borderRadius: '0.5rem', backgroundColor: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.05)', padding: '1rem'}}>
                  <div style={{height: '1rem', width: '5rem', backgroundColor: 'rgba(255,255,255,0.1)', borderRadius: '0.25rem', marginBottom: '1rem'}} />
                  {[1,2,3,4].map(i => (
                    <div key={i} style={{display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.75rem'}}>
                      <div style={{width: '2rem', height: '2rem', borderRadius: '50%', backgroundColor: 'rgba(255,255,255,0.05)'}} />
                      <div style={{flex: 1}}>
                        <div style={{height: '0.5rem', width: '4rem', backgroundColor: 'rgba(255,255,255,0.1)', borderRadius: '0.25rem', marginBottom: '0.25rem'}} />
                        <div style={{height: '0.5rem', width: '2.5rem', backgroundColor: 'rgba(255,255,255,0.05)', borderRadius: '0.25rem'}} />
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </motion.div>
        </div>
      </section>

      {/* Social Proof */}
      <section style={styles.socialProof}>
        <div style={styles.socialProofInner}>
          <p style={styles.socialProofLabel}>TRUSTED BY FINANCE TEAMS AT</p>
          <div style={styles.socialProofLogos}>
            {['Acme Corp', 'GlobalBank', 'FinTech Inc', 'SecurePay', 'CloudScale'].map((brand) => (
              <span key={brand} style={styles.socialProofLogo}>{brand}</span>
            ))}
          </div>
        </div>
      </section>

      {/* Features */}
      <section id="features" style={styles.features}>
        <div style={styles.featuresInner}>
          <div style={styles.sectionHeader}>
            <h2 style={styles.sectionTitle}>Everything you need to stay balanced.</h2>
            <p style={styles.sectionSubtitle}>
              Powerful tools designed to handle the complexity of modern financial stacks.
            </p>
          </div>

          <div style={styles.featureGrid}>
            {/* Large Card */}
            <div style={{...styles.featureCard, ...styles.featureCardLarge}}>
              <div style={{position: 'absolute', top: 0, right: 0, width: '16rem', height: '16rem', background: 'radial-gradient(ellipse, rgba(59, 130, 246, 0.15), transparent 70%)'}} />
              <div style={{position: 'relative', zIndex: 10}}>
                <div style={{...styles.featureIcon, backgroundColor: 'rgba(59, 130, 246, 0.2)'}}>
                  <Zap size={24} color="#60A5FA" />
                </div>
                <h3 style={styles.featureTitle}>Real-time Reconciliation</h3>
                <p style={{...styles.featureDesc, maxWidth: '28rem', marginBottom: '2rem'}}>
                  Process millions of transactions in seconds. Our engine automatically matches entries across multiple sources with 99.9% accuracy.
                </p>
                <div style={{height: '12rem', borderRadius: '0.75rem', backgroundColor: 'rgba(0,0,0,0.4)', border: '1px solid rgba(255,255,255,0.05)', padding: '1rem'}}>
                  <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem'}}>
                    <div style={{display: 'flex', alignItems: 'center', gap: '0.5rem'}}>
                      <div style={{width: '0.5rem', height: '0.5rem', borderRadius: '50%', backgroundColor: '#22C55E'}} />
                      <span style={{fontSize: '0.75rem', color: '#22C55E'}}>Matched</span>
                    </div>
                    <span style={{fontSize: '0.75rem', color: '#6B7280'}}>Just now</span>
                  </div>
                  {[1,2,3].map(i => (
                    <div key={i} style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.5rem', borderRadius: '0.25rem', backgroundColor: 'rgba(255,255,255,0.05)', marginBottom: '0.5rem'}}>
                      <div style={{height: '0.5rem', width: '6rem', backgroundColor: 'rgba(255,255,255,0.1)', borderRadius: '0.25rem'}} />
                      <div style={{height: '0.5rem', width: '3rem', backgroundColor: 'rgba(255,255,255,0.1)', borderRadius: '0.25rem'}} />
                    </div>
                  ))}
                </div>
              </div>
            </div>

            {/* Tall Card */}
            <div style={{...styles.featureCard, ...styles.featureCardTall}}>
              <div style={{position: 'absolute', bottom: 0, left: 0, width: '100%', height: '50%', background: 'linear-gradient(to top, rgba(88, 28, 135, 0.2), transparent)'}} />
              <div style={{position: 'relative', zIndex: 10}}>
                <div style={{...styles.featureIcon, backgroundColor: 'rgba(147, 51, 234, 0.2)'}}>
                  <Shield size={24} color="#A78BFA" />
                </div>
                <h3 style={styles.featureTitle}>Bank-Grade Security</h3>
                <p style={{...styles.featureDesc, marginBottom: '2rem'}}>
                  SOC2 Type II certified. End-to-end encryption for all data in transit and at rest.
                </p>
                <div>
                  {['Encryption', 'Audit Logs', 'SSO Support', 'Role Access'].map((item) => (
                    <div key={item} style={styles.checkItem}>
                      <CheckCircle2 size={20} color="#22C55E" />
                      <span style={styles.checkText}>{item}</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            {/* Small Card 1 */}
            <div style={styles.featureCard}>
              <div style={{...styles.featureIcon, backgroundColor: 'rgba(249, 115, 22, 0.2)'}}>
                <BarChart3 size={24} color="#FB923C" />
              </div>
              <h3 style={{...styles.featureTitle, fontSize: '1.25rem'}}>Deep Analytics</h3>
              <p style={{...styles.featureDesc, fontSize: '0.875rem'}}>
                Visualize cash flow and spot trends instantly.
              </p>
            </div>

            {/* Small Card 2 */}
            <div style={styles.featureCard}>
              <div style={{...styles.featureIcon, backgroundColor: 'rgba(236, 72, 153, 0.2)'}}>
                <Globe size={24} color="#F472B6" />
              </div>
              <h3 style={{...styles.featureTitle, fontSize: '1.25rem'}}>Global Coverage</h3>
              <p style={{...styles.featureDesc, fontSize: '0.875rem'}}>
                Support for 150+ currencies and 50+ gateways.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* How it Works */}
      <section id="how-it-works" style={styles.howItWorks}>
        <div style={styles.howItWorksInner}>
          <h2 style={{...styles.sectionTitle, marginBottom: '4rem'}}>How it works</h2>
          
          <div style={styles.timeline}>
            <div style={styles.timelineLine} />
            
            {[
              { title: 'Connect Sources', desc: 'Link your bank accounts, payment gateways, and ERP in one click.', Icon: Lock },
              { title: 'Auto-Match', desc: 'Our AI engine matches 99% of transactions automatically.', Icon: Zap },
              { title: 'Resolve Exceptions', desc: 'Handle the remaining 1% with guided workflows and team collaboration.', Icon: CheckCircle2 }
            ].map((step, idx) => (
              <motion.div 
                key={idx}
                initial={{ opacity: 0, x: -20 }}
                whileInView={{ opacity: 1, x: 0 }}
                viewport={{ once: true }}
                transition={{ delay: idx * 0.2 }}
                style={styles.timelineItem}
              >
                <div style={styles.timelineIcon}>
                  <step.Icon size={24} color="#60A5FA" />
                </div>
                <h3 style={styles.timelineTitle}>{step.title}</h3>
                <p style={styles.timelineDesc}>{step.desc}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section style={styles.cta}>
        <div style={styles.ctaCard}>
          <h2 style={styles.ctaTitle}>Ready to automate your<br />financial operations?</h2>
          <p style={styles.ctaSubtitle}>
            Join forward-thinking finance teams who have switched to ReconPlatform.
          </p>
          <div style={styles.ctaButtons}>
            <Link to="/login" style={styles.ctaPrimaryButton}>
              Get Started Now
            </Link>
            <button style={styles.ctaSecondaryButton}>
              Contact Sales
            </button>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer style={styles.footer}>
        <div style={styles.footerInner}>
          <div style={styles.footerLogo}>
            <div style={styles.footerLogoIcon}>R</div>
            <span style={styles.footerLogoText}>ReconPlatform</span>
          </div>
          <div style={styles.footerLinks}>
            <a href="#" style={styles.footerLink}>Privacy</a>
            <a href="#" style={styles.footerLink}>Terms</a>
            <a href="#" style={styles.footerLink}>Security</a>
            <a href="#" style={styles.footerLink}>Status</a>
          </div>
          <div style={styles.footerCopy}>
            © 2025 ReconPlatform Inc.
          </div>
        </div>
      </footer>
    </div>
  );
};
