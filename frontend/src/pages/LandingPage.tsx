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

export const LandingPage: React.FC = () => {
  const { scrollY } = useScroll();
  const y1 = useTransform(scrollY, [0, 500], [0, 200]);
  const y2 = useTransform(scrollY, [0, 500], [0, -150]);
  
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const handleScroll = () => setScrolled(window.scrollY > 50);
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <div className="min-h-screen bg-[#0A0A0B] text-white overflow-x-hidden selection:bg-blue-500/30">
      {/* Navigation */}
      <nav className={`fixed top-0 w-full z-50 transition-all duration-300 ${
        scrolled ? 'bg-[#0A0A0B]/80 backdrop-blur-xl border-b border-white/5' : 'bg-transparent'
      }`}>
        <div className="max-w-7xl mx-auto px-6 h-20 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 bg-gradient-to-br from-blue-500 to-purple-600 rounded-lg flex items-center justify-center">
              <span className="font-bold text-white">R</span>
            </div>
            <span className="text-xl font-bold tracking-tight">ReconPlatform</span>
          </div>
          
          <div className="hidden md:flex items-center gap-8">
            <a href="#features" className="text-sm font-medium text-gray-400 hover:text-white transition-colors">Features</a>
            <a href="#how-it-works" className="text-sm font-medium text-gray-400 hover:text-white transition-colors">How it Works</a>
            <a href="#pricing" className="text-sm font-medium text-gray-400 hover:text-white transition-colors">Pricing</a>
          </div>

          <div className="flex items-center gap-4">
            <Link to="/login" className="text-sm font-medium text-white hover:text-gray-300 transition-colors">
              Log in
            </Link>
            <Link 
              to="/register" 
              className="group relative px-5 py-2.5 bg-white text-black rounded-full text-sm font-semibold hover:bg-gray-100 transition-all overflow-hidden"
            >
              <span className="relative z-10 flex items-center gap-2">
                Get Started <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
              </span>
            </Link>
          </div>
        </div>
      </nav>

      {/* Hero Section */}
      <section className="relative pt-32 pb-20 lg:pt-48 lg:pb-32 px-6">
        <div className="absolute inset-0 overflow-hidden pointer-events-none">
          <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[1000px] h-[600px] bg-blue-500/20 rounded-full blur-[120px] opacity-50" />
          <div className="absolute bottom-0 right-0 w-[800px] h-[600px] bg-purple-500/10 rounded-full blur-[100px] opacity-30" />
        </div>

        <div className="max-w-7xl mx-auto relative z-10">
          <div className="text-center max-w-4xl mx-auto mb-16">
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6 }}
              className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/5 border border-white/10 mb-8"
            >
              <span className="flex h-2 w-2 rounded-full bg-blue-500 animate-pulse" />
              <span className="text-xs font-medium text-blue-200">New: AI-Powered Dispute Resolution</span>
            </motion.div>

            <motion.h1
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6, delay: 0.1 }}
              className="text-5xl lg:text-7xl font-bold tracking-tight mb-8 text-white"
            >
              Financial reconciliation <br />
              reimagined for scale.
            </motion.h1>

            <motion.p
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6, delay: 0.2 }}
              className="text-xl text-gray-400 mb-10 max-w-2xl mx-auto leading-relaxed"
            >
              Automate 99% of your transaction matching. Detect anomalies in real-time. 
              Close your books faster with the world's most advanced reconciliation engine.
            </motion.p>

            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6, delay: 0.3 }}
              className="flex flex-col sm:flex-row items-center justify-center gap-4"
            >
              <Link 
                to="/register"
                className="w-full sm:w-auto px-8 py-4 bg-blue-600 hover:bg-blue-500 text-white rounded-full font-semibold transition-all flex items-center justify-center gap-2 shadow-lg shadow-blue-500/25"
              >
                Start Free Trial <ChevronRight className="w-4 h-4" />
              </Link>
              <button className="w-full sm:w-auto px-8 py-4 bg-white/5 hover:bg-white/10 text-white rounded-full font-semibold transition-all flex items-center justify-center gap-2 border border-white/10 backdrop-blur-sm">
                <PlayCircle className="w-4 h-4" /> Watch Demo
              </button>
            </motion.div>
          </div>

          {/* Hero Dashboard Preview */}
          <motion.div
            style={{ y: y1 }}
            className="relative mx-auto max-w-6xl"
          >
            <div className="relative rounded-xl bg-[#1A1A1C] border border-white/10 shadow-2xl overflow-hidden aspect-video">
              <div className="absolute inset-0 bg-gradient-to-br from-blue-500/5 to-purple-500/5" />
              {/* Mock UI Elements */}
              <div className="p-6 border-b border-white/5 flex items-center justify-between">
                <div className="flex gap-2">
                  <div className="w-3 h-3 rounded-full bg-red-500/20 border border-red-500/50" />
                  <div className="w-3 h-3 rounded-full bg-yellow-500/20 border border-yellow-500/50" />
                  <div className="w-3 h-3 rounded-full bg-green-500/20 border border-green-500/50" />
                </div>
                <div className="h-2 w-32 bg-white/10 rounded-full" />
              </div>
              <div className="p-8 grid grid-cols-3 gap-8">
                <div className="col-span-2 space-y-4">
                  <div className="h-32 rounded-lg bg-white/5 border border-white/5 p-4">
                    <div className="flex justify-between mb-4">
                      <div className="h-4 w-24 bg-white/10 rounded" />
                      <div className="h-4 w-16 bg-green-500/20 rounded text-green-400 text-xs flex items-center justify-center">Live</div>
                    </div>
                    <div className="space-y-2">
                      <div className="h-2 w-full bg-white/5 rounded" />
                      <div className="h-2 w-3/4 bg-white/5 rounded" />
                      <div className="h-2 w-1/2 bg-white/5 rounded" />
                    </div>
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    <div className="h-24 rounded-lg bg-white/5 border border-white/5" />
                    <div className="h-24 rounded-lg bg-white/5 border border-white/5" />
                  </div>
                </div>
                <div className="space-y-4">
                  <div className="h-full rounded-lg bg-white/5 border border-white/5 p-4">
                    <div className="h-4 w-20 bg-white/10 rounded mb-4" />
                    {[1,2,3,4].map(i => (
                      <div key={i} className="flex items-center gap-3 mb-3">
                        <div className="w-8 h-8 rounded-full bg-white/5" />
                        <div className="flex-1">
                          <div className="h-2 w-16 bg-white/10 rounded mb-1" />
                          <div className="h-2 w-10 bg-white/5 rounded" />
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          </motion.div>
        </div>
      </section>

      {/* Social Proof */}
      <section className="py-10 border-y border-white/5 bg-white/[0.02]">
        <div className="max-w-7xl mx-auto px-6">
          <p className="text-center text-sm font-medium text-gray-500 mb-8">TRUSTED BY FINANCE TEAMS AT</p>
          <div className="flex flex-wrap justify-center gap-12 opacity-50 grayscale">
            {['Acme Corp', 'GlobalBank', 'FinTech Inc', 'SecurePay', 'CloudScale'].map((brand) => (
              <span key={brand} className="text-xl font-bold text-white">{brand}</span>
            ))}
          </div>
        </div>
      </section>

      {/* Bento Grid Features */}
      <section id="features" className="py-32 px-6">
        <div className="max-w-7xl mx-auto">
          <div className="text-center mb-20">
            <h2 className="text-3xl md:text-5xl font-bold mb-6">Everything you need to <br />stay balanced.</h2>
            <p className="text-xl text-gray-400 max-w-2xl mx-auto">
              Powerful tools designed to handle the complexity of modern financial stacks.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {/* Large Card */}
            <motion.div 
              whileHover={{ y: -5 }}
              className="md:col-span-2 rounded-3xl bg-[#151516] border border-white/10 p-8 md:p-12 relative overflow-hidden group"
            >
              <div className="absolute top-0 right-0 w-64 h-64 bg-blue-500/10 rounded-full blur-[80px] group-hover:bg-blue-500/20 transition-colors" />
              <div className="relative z-10">
                <div className="w-12 h-12 rounded-xl bg-blue-500/20 flex items-center justify-center mb-6">
                  <Zap className="w-6 h-6 text-blue-400" />
                </div>
                <h3 className="text-2xl font-bold mb-4">Real-time Reconciliation</h3>
                <p className="text-gray-400 max-w-md mb-8">
                  Process millions of transactions in seconds. Our engine automatically matches entries across multiple sources with 99.9% accuracy.
                </p>
                <div className="h-48 rounded-xl bg-black/40 border border-white/5 p-4">
                  {/* Abstract Visualization */}
                  <div className="flex items-center justify-between mb-4">
                    <div className="flex gap-2">
                      <div className="w-2 h-2 rounded-full bg-green-500" />
                      <span className="text-xs text-green-500">Matched</span>
                    </div>
                    <span className="text-xs text-gray-500">Just now</span>
                  </div>
                  <div className="space-y-2">
                    {[1,2,3].map(i => (
                      <div key={i} className="flex items-center justify-between p-2 rounded bg-white/5">
                        <div className="h-2 w-24 bg-white/10 rounded" />
                        <div className="h-2 w-12 bg-white/10 rounded" />
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </motion.div>

            {/* Tall Card */}
            <motion.div 
              whileHover={{ y: -5 }}
              className="md:row-span-2 rounded-3xl bg-[#151516] border border-white/10 p-8 relative overflow-hidden group"
            >
              <div className="absolute bottom-0 left-0 w-full h-1/2 bg-gradient-to-t from-purple-900/20 to-transparent" />
              <div className="relative z-10">
                <div className="w-12 h-12 rounded-xl bg-purple-500/20 flex items-center justify-center mb-6">
                  <Shield className="w-6 h-6 text-purple-400" />
                </div>
                <h3 className="text-2xl font-bold mb-4">Bank-Grade Security</h3>
                <p className="text-gray-400 mb-8">
                  SOC2 Type II certified. End-to-end encryption for all data in transit and at rest.
                </p>
                <div className="space-y-4">
                  {['Encryption', 'Audit Logs', 'SSO Support', 'Role Access'].map((item) => (
                    <div key={item} className="flex items-center gap-3">
                      <CheckCircle2 className="w-5 h-5 text-green-500" />
                      <span className="text-sm text-gray-300">{item}</span>
                    </div>
                  ))}
                </div>
              </div>
            </motion.div>

            {/* Small Card 1 */}
            <motion.div 
              whileHover={{ y: -5 }}
              className="rounded-3xl bg-[#151516] border border-white/10 p-8 group"
            >
              <div className="w-12 h-12 rounded-xl bg-orange-500/20 flex items-center justify-center mb-6">
                <BarChart3 className="w-6 h-6 text-orange-400" />
              </div>
              <h3 className="text-xl font-bold mb-2">Deep Analytics</h3>
              <p className="text-sm text-gray-400">
                Visualize cash flow and spot trends instantly.
              </p>
            </motion.div>

            {/* Small Card 2 */}
            <motion.div 
              whileHover={{ y: -5 }}
              className="rounded-3xl bg-[#151516] border border-white/10 p-8 group"
            >
              <div className="w-12 h-12 rounded-xl bg-pink-500/20 flex items-center justify-center mb-6">
                <Globe className="w-6 h-6 text-pink-400" />
              </div>
              <h3 className="text-xl font-bold mb-2">Global Coverage</h3>
              <p className="text-sm text-gray-400">
                Support for 150+ currencies and 50+ gateways.
              </p>
            </motion.div>
          </div>
        </div>
      </section>

      {/* Interactive Timeline */}
      <section id="how-it-works" className="py-32 bg-[#0F0F10] relative overflow-hidden">
        <div className="max-w-7xl mx-auto px-6">
          <div className="mb-20">
            <h2 className="text-3xl md:text-5xl font-bold mb-6">How it works</h2>
          </div>
          
          <div className="relative">
            <div className="absolute left-8 top-0 bottom-0 w-px bg-gradient-to-b from-blue-500 via-purple-500 to-transparent opacity-30" />
            
            {[
              { title: 'Connect Sources', desc: 'Link your bank accounts, payment gateways, and ERP in one click.', icon: Lock },
              { title: 'Auto-Match', desc: 'Our AI engine matches 99% of transactions automatically.', icon: Zap },
              { title: 'Resolve Exceptions', desc: 'Handle the remaining 1% with guided workflows and team collaboration.', icon: CheckCircle2 }
            ].map((step, idx) => (
              <motion.div 
                key={idx}
                initial={{ opacity: 0, x: -20 }}
                whileInView={{ opacity: 1, x: 0 }}
                viewport={{ once: true }}
                transition={{ delay: idx * 0.2 }}
                className="relative pl-24 pb-16 last:pb-0"
              >
                <div className="absolute left-0 w-16 h-16 rounded-2xl bg-[#1A1A1C] border border-white/10 flex items-center justify-center z-10">
                  <step.icon className="w-6 h-6 text-blue-400" />
                </div>
                <h3 className="text-2xl font-bold mb-2">{step.title}</h3>
                <p className="text-gray-400 max-w-xl">{step.desc}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-32 px-6">
        <div className="max-w-5xl mx-auto rounded-[2.5rem] bg-gradient-to-br from-blue-600 to-purple-700 p-12 md:p-24 text-center relative overflow-hidden">
          <div className="absolute inset-0 bg-[url('https://grainy-gradients.vercel.app/noise.svg')] opacity-20" />
          <div className="relative z-10">
            <h2 className="text-4xl md:text-6xl font-bold mb-8">Ready to automate your <br /> financial operations?</h2>
            <p className="text-xl text-blue-100 mb-10 max-w-2xl mx-auto">
              Join forward-thinking finance teams who have switched to ReconPlatform.
            </p>
            <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
              <Link 
                to="/register"
                className="px-8 py-4 bg-white text-blue-600 rounded-full font-bold hover:bg-gray-100 transition-colors"
              >
                Get Started Now
              </Link>
              <button className="px-8 py-4 bg-transparent border border-white/30 text-white rounded-full font-bold hover:bg-white/10 transition-colors">
                Contact Sales
              </button>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="py-12 border-t border-white/5 bg-[#050505]">
        <div className="max-w-7xl mx-auto px-6 flex flex-col md:flex-row justify-between items-center gap-8">
          <div className="flex items-center gap-2">
            <div className="w-6 h-6 bg-gradient-to-br from-blue-500 to-purple-600 rounded-md flex items-center justify-center">
              <span className="font-bold text-white text-xs">R</span>
            </div>
            <span className="font-bold text-gray-300">ReconPlatform</span>
          </div>
          <div className="flex gap-8 text-sm text-gray-500">
            <a href="#" className="hover:text-white transition-colors">Privacy</a>
            <a href="#" className="hover:text-white transition-colors">Terms</a>
            <a href="#" className="hover:text-white transition-colors">Security</a>
            <a href="#" className="hover:text-white transition-colors">Status</a>
          </div>
          <div className="text-sm text-gray-600">
            © 2025 ReconPlatform Inc.
          </div>
        </div>
      </footer>
    </div>
  );
};
