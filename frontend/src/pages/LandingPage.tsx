import { useRef } from 'react'
import { Link } from 'react-router-dom'
import { motion, useInView, type Variants } from 'framer-motion'
import { FileText, Users, BarChart2, Package, MessageSquare } from 'lucide-react'
import './LandingPage.css'

// ── Animation helpers ──────────────────────────────────────────
const EASE: [number, number, number, number] = [0.22, 1, 0.36, 1]

const fadeUp: Variants = {
  hidden: { opacity: 0, y: 28 },
  visible: (i: number = 0) => ({
    opacity: 1,
    y: 0,
    transition: { duration: 0.52, delay: i * 0.08, ease: EASE },
  }),
}

function Section({ children, className = '' }: { children: React.ReactNode; className?: string }) {
  const ref = useRef<HTMLDivElement>(null)
  const inView = useInView(ref, { once: true, margin: '-80px' })
  return (
    <section
      ref={ref}
      className={`lp-section ${className}`}
      style={{ opacity: inView ? 1 : 0, transition: 'opacity 0.1s' }}
    >
      {children}
    </section>
  )
}

// ── Feature data ───────────────────────────────────────────────
const features: { icon: React.ReactNode; title: string; desc: string }[] = [
  {
    icon: <FileText size={22} strokeWidth={1.8} />,
    title: 'Orçamentos precisos',
    desc: 'Selecione tipo de vidro, informe as dimensões e obtenha o preço instantaneamente. Confirme a venda com um clique.',
  },
  {
    icon: <Package size={22} strokeWidth={1.8} />,
    title: 'Gestão de kits e vidros',
    desc: 'Cadastre tipos de vidro por m², crie kits completos e vincule cores a cada produto.',
  },
  {
    icon: null,
    title: 'Controle de estoque',
    desc: 'Baixa automática ao confirmar venda, alertas de estoque mínimo e cálculo de aproveitamento de chapa.',
  },
  {
    icon: <Users size={22} strokeWidth={1.8} />,
    title: 'Clientes e fornecedores',
    desc: 'Histórico completo por cliente, gestão de fornecedores e vínculo por tipo de vidro.',
  },
  {
    icon: <MessageSquare size={22} strokeWidth={1.8} />,
    title: 'WhatsApp automático',
    desc: 'Envie orçamentos confirmados ao fornecedor e ao cliente diretamente pelo sistema.',
  },
  {
    icon: <BarChart2 size={22} strokeWidth={1.8} />,
    title: 'Dashboards e relatórios',
    desc: 'Vendas por mês, ranking de produtos, ticket médio e exportação em PDF/Excel.',
  },
]

const steps = [
  { num: '01', label: 'Cadastre vidros, kits e cores com seus respectivos preços por m².' },
  { num: '02', label: 'Gere um orçamento para o cliente com múltiplos itens e descontos.' },
  { num: '03', label: 'Confirme a venda — o estoque baixa e o fornecedor é notificado via WhatsApp.' },
  { num: '04', label: 'Acompanhe tudo no dashboard: entregas, finanças e desempenho por vendedor.' },
]

// ── Page ───────────────────────────────────────────────────────
export function LandingPage() {
  return (
    <div className="lp-root">
      {/* ── NAV ── */}
      <header className="lp-nav">
        <div className="lp-nav__inner">
          <span className="lp-brand">
            <span className="lp-brand__mark" aria-hidden="true">G</span>
            Glazr
          </span>
          <Link to="/login" className="lp-btn lp-btn--ghost">Entrar no sistema</Link>
        </div>
      </header>

      {/* ── HERO ── */}
      <div className="lp-hero">
        <div className="lp-hero__inner">
          <motion.p
            className="lp-eyebrow"
            variants={fadeUp}
            initial="hidden"
            animate="visible"
            custom={0}
          >
            Gestão para vidraçarias
          </motion.p>
          <motion.h1
            className="lp-hero__title"
            variants={fadeUp}
            initial="hidden"
            animate="visible"
            custom={1}
          >
            Seu negócio mais claro,<br />do primeiro contato à entrega.
          </motion.h1>
          <motion.p
            className="lp-hero__sub"
            variants={fadeUp}
            initial="hidden"
            animate="visible"
            custom={2}
          >
            Centralize orçamentos, estoque, clientes e pedidos em um único sistema feito para o dia a dia da vidraçaria.
          </motion.p>
          <motion.div
            className="lp-hero__actions"
            variants={fadeUp}
            initial="hidden"
            animate="visible"
            custom={3}
          >
            <Link to="/login" className="lp-btn lp-btn--primary">Acessar o sistema</Link>
          </motion.div>
        </div>

        {/* decorative grid overlay */}
        <div className="lp-hero__grid" aria-hidden="true" />
      </div>

      {/* ── FEATURES ── */}
      <Section className="lp-features-section">
        <div className="lp-container">
          <motion.p className="lp-eyebrow" variants={fadeUp} initial="hidden" whileInView="visible" viewport={{ once: true }} custom={0}>
            Funcionalidades
          </motion.p>
          <motion.h2 className="lp-section__title" variants={fadeUp} initial="hidden" whileInView="visible" viewport={{ once: true }} custom={1}>
            Tudo que você precisa em um só lugar
          </motion.h2>

          <div className="lp-feature-grid">
            {features.map((f, i) => (
              <motion.div
                key={f.title}
                className="lp-feature-card"
                variants={fadeUp}
                initial="hidden"
                whileInView="visible"
                viewport={{ once: true }}
                custom={i * 0.5 + 2}
              >
                {f.icon != null && <div className="lp-feature-card__icon">{f.icon}</div>}
                <h3 className="lp-feature-card__title">{f.title}</h3>
                <p className="lp-feature-card__desc">{f.desc}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </Section>

      {/* ── HOW IT WORKS ── */}
      <Section className="lp-steps-section">
        <div className="lp-container">
          <motion.p className="lp-eyebrow lp-eyebrow--light" variants={fadeUp} initial="hidden" whileInView="visible" viewport={{ once: true }} custom={0}>
            Como funciona
          </motion.p>
          <motion.h2 className="lp-section__title lp-section__title--light" variants={fadeUp} initial="hidden" whileInView="visible" viewport={{ once: true }} custom={1}>
            Do orçamento à entrega
          </motion.h2>

          <div className="lp-steps-grid">
            {steps.map((s, i) => (
              <motion.div
                key={s.num}
                className="lp-step"
                variants={fadeUp}
                initial="hidden"
                whileInView="visible"
                viewport={{ once: true }}
                custom={i * 0.5 + 2}
              >
                <span className="lp-step__num">{s.num}</span>
                <p className="lp-step__label">{s.label}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </Section>

      {/* ── CTA ── */}
      <Section className="lp-cta-section">
        <div className="lp-container lp-cta-inner">
          <motion.h2 className="lp-cta__title" variants={fadeUp} initial="hidden" whileInView="visible" viewport={{ once: true }} custom={0}>
            Pronto para organizar sua vidraçaria?
          </motion.h2>
          <motion.p className="lp-cta__sub" variants={fadeUp} initial="hidden" whileInView="visible" viewport={{ once: true }} custom={1}>
            Acesse o sistema agora e comece a usar todas as funcionalidades.
          </motion.p>
          <motion.div variants={fadeUp} initial="hidden" whileInView="visible" viewport={{ once: true }} custom={2}>
            <Link to="/login" className="lp-btn lp-btn--primary lp-btn--lg">Acessar o sistema</Link>
          </motion.div>
        </div>
      </Section>

      {/* ── FOOTER ── */}
      <footer className="lp-footer">
        <div className="lp-nav__inner">
          <span className="lp-footer__brand">
            <span className="lp-brand__mark lp-brand__mark--sm" aria-hidden="true">G</span>
            Glazr
          </span>
          <span className="lp-footer__copy">© {new Date().getFullYear()} Glazr. Todos os direitos reservados.</span>
        </div>
      </footer>
    </div>
  )
}
