import { Link } from 'react-router-dom';
import { Gamepad2, ShieldCheck, Truck, Headphones, CreditCard, Send, Globe, Share2, Disc as Discord } from 'lucide-react';

export const Footer = () => {
  return (
    <footer className="mt-28 border-t border-white/10 bg-[#050608] text-slate-300 w-full">
      <div style={{ maxWidth: '80rem', marginLeft: 'auto', marginRight: 'auto', paddingLeft: '0.5rem', paddingRight: '0.5rem' }}>
        
        {/* 1. Barra de Ventajas Gamer */}
        <div className="border-b border-white/10 bg-[#08090e]/60 py-8 rounded-2xl mt-8 w-full border border-white/5" style={{ paddingLeft: '1.5rem', paddingRight: '1.5rem' }}>
          <div className="w-full grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8">
            
            <div className="flex items-center gap-4 p-4 rounded-2xl bg-[#0e121e]/60 border border-white/5 hover:border-[#00f2fe]/40 transition-colors">
              <div className="p-3 rounded-xl bg-[#00f2fe]/10 text-[#00f2fe] shrink-0">
                <Truck className="w-7 h-7" />
              </div>
              <div>
                <h4 className="font-bold text-sm text-white uppercase tracking-wider font-gamer">Envíos a todo Chile</h4>
                <p className="text-xs text-slate-400 mt-0.5">Despacho express a regiones</p>
              </div>
            </div>

            <div className="flex items-center gap-4 p-4 rounded-2xl bg-[#0e121e]/60 border border-white/5 hover:border-[#7f00ff]/40 transition-colors">
              <div className="p-3 rounded-xl bg-[#7f00ff]/10 text-[#7f00ff] shrink-0">
                <ShieldCheck className="w-7 h-7" />
              </div>
              <div>
                <h4 className="font-bold text-sm text-white uppercase tracking-wider font-gamer">Garantía Gamer 100%</h4>
                <p className="text-xs text-slate-400 mt-0.5">12 meses de soporte técnico</p>
              </div>
            </div>

            <div className="flex items-center gap-4 p-4 rounded-2xl bg-[#0e121e]/60 border border-white/5 hover:border-[#f107a3]/40 transition-colors">
              <div className="p-3 rounded-xl bg-[#f107a3]/10 text-[#f107a3] shrink-0">
                <Headphones className="w-7 h-7" />
              </div>
              <div>
                <h4 className="font-bold text-sm text-white uppercase tracking-wider font-gamer">Soporte 24/7</h4>
                <p className="text-xs text-slate-400 mt-0.5">Atención especializada para ti</p>
              </div>
            </div>

            <div className="flex items-center gap-4 p-4 rounded-2xl bg-[#0e121e]/60 border border-white/5 hover:border-[#00f2fe]/40 transition-colors">
              <div className="p-3 rounded-xl bg-[#00f2fe]/10 text-[#00f2fe] shrink-0">
                <CreditCard className="w-7 h-7" />
              </div>
              <div>
                <h4 className="font-bold text-sm text-white uppercase tracking-wider font-gamer">Pago Seguro Webpay</h4>
                <p className="text-xs text-slate-400 mt-0.5">Tarjetas de crédito y débito</p>
              </div>
            </div>

          </div>
        </div>

        {/* 2. Cuerpo Principal del Footer */}
        <div className="py-16 w-full">
          <div className="w-full grid grid-cols-1 md:grid-cols-2 lg:grid-cols-12 gap-10">
            
            {/* Columna 1: Branding (Col 4) */}
            <div className="lg:col-span-4 space-y-5">
              <Link to="/" className="flex items-center gap-3">
                <div className="p-2.5 rounded-xl bg-gradient-to-tr from-[#00f2fe] to-[#7f00ff] text-black shadow-lg shadow-[#00f2fe]/30">
                  <Gamepad2 className="w-7 h-7 stroke-[2.5]" />
                </div>
                <span className="font-gamer text-3xl font-bold tracking-wider bg-gradient-to-r from-white via-[#00f2fe] to-[#7f00ff] bg-clip-text text-transparent">
                  NEXUS<span className="text-[#00f2fe]">GAMING</span>
                </span>
              </Link>
              <p className="text-xs text-slate-400 leading-relaxed max-w-sm">
                La tienda especialista en equipamiento pro para gamers y streamers. Teclados custom, monitores OLED de 240Hz, mouse ultraligeros y setups completos.
              </p>
              <div className="flex items-center gap-3 pt-2">
                <a href="#discord" className="p-2.5 rounded-xl bg-[#121622] hover:bg-[#00f2fe]/20 hover:text-[#00f2fe] transition-colors border border-white/5" title="Discord">
                  <Discord className="w-4 h-4" />
                </a>
                <a href="#community" className="p-2.5 rounded-xl bg-[#121622] hover:bg-[#7f00ff]/20 hover:text-[#7f00ff] transition-colors border border-white/5" title="Comunidad">
                  <Share2 className="w-4 h-4" />
                </a>
                <a href="#web" className="p-2.5 rounded-xl bg-[#121622] hover:bg-[#f107a3]/20 hover:text-[#f107a3] transition-colors border border-white/5" title="Website">
                  <Globe className="w-4 h-4" />
                </a>
              </div>
            </div>

            {/* Columna 2: Categorías (Col 2) */}
            <div className="lg:col-span-2 space-y-4">
              <h4 className="font-gamer text-sm font-bold text-white uppercase tracking-wider border-b border-[#00f2fe]/30 pb-2 inline-block">
                Categorías
              </h4>
              <ul className="space-y-2.5 text-xs text-slate-400">
                <li><Link to="/catalogo?cat=Teclados" className="hover:text-[#00f2fe] transition-colors">Teclados Mecánicos</Link></li>
                <li><Link to="/catalogo?cat=Pantallas" className="hover:text-[#00f2fe] transition-colors">Monitores OLED & 240Hz</Link></li>
                <li><Link to="/catalogo?cat=Periféricos" className="hover:text-[#00f2fe] transition-colors">Mouse & Audífonos Pro</Link></li>
                <li><Link to="/catalogo?cat=Muebles Gamer" className="hover:text-[#00f2fe] transition-colors">Sillas Ergonómicas</Link></li>
              </ul>
            </div>

            {/* Columna 3: Información (Col 2) */}
            <div className="lg:col-span-2 space-y-4">
              <h4 className="font-gamer text-sm font-bold text-white uppercase tracking-wider border-b border-[#7f00ff]/30 pb-2 inline-block">
                Soporte & Ayuda
              </h4>
              <ul className="space-y-2.5 text-xs text-slate-400">
                <li><a href="#envios" className="hover:text-[#7f00ff] transition-colors">Estado de tu Pedido</a></li>
                <li><a href="#garantia" className="hover:text-[#7f00ff] transition-colors">Políticas de Garantía</a></li>
                <li><a href="#faq" className="hover:text-[#7f00ff] transition-colors">Preguntas Frecuentes</a></li>
                <li><a href="#contacto" className="hover:text-[#7f00ff] transition-colors">Contacto Directo</a></li>
              </ul>
            </div>

            {/* Columna 4: Newsletter Gamer (Col 4) */}
            <div className="lg:col-span-4 space-y-4">
              <h4 className="font-gamer text-sm font-bold text-white uppercase tracking-wider border-b border-[#f107a3]/30 pb-2 inline-block">
                Comunidad & Ofertas
              </h4>
              <p className="text-xs text-slate-400">
                Suscríbete para recibir lanzamientos exclusivos de stock y cupones de descuento.
              </p>
              <form onSubmit={(e) => e.preventDefault()} className="flex items-center gap-2">
                <input
                  type="email"
                  placeholder="Tu email gamer..."
                  className="bg-[#121622] border border-white/10 rounded-xl px-3.5 py-2.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-[#00f2fe] flex-1"
                />
                <button type="submit" className="btn-gamer-primary py-2.5 px-4 text-xs">
                  <Send className="w-3.5 h-3.5" />
                </button>
              </form>
            </div>

          </div>
        </div>

        {/* 3. Barra Inferior Copyright */}
        <div className="border-t border-white/10 py-6 w-full flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-slate-500">
          <div className="flex items-center gap-2">
            <Gamepad2 className="w-4 h-4 text-[#00f2fe]" />
            <span>© 2026 NEXUS GAMING STORE - Proyecto Desarrollo Cloud</span>
          </div>
          <p className="text-[11px] text-slate-500">
            Arquitectura Microservicios Spring Boot & React Vite
          </p>
        </div>

      </div>
    </footer>
  );
};
