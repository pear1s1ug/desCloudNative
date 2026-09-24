import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { fetchProductosAPI } from '../utils/api';
import { ProductCard } from '../components/ProductCard';
import { useAuth } from '../context/AuthContext';
import { Sparkles, ArrowRight, Monitor, Keyboard, Mouse, Tv, Lock } from 'lucide-react';

export const Home = ({ searchTerm }) => {
  const { isAuthenticated, isLoading } = useAuth();
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isLoading) return;
    if (!isAuthenticated) {
      setLoading(false);
      return;
    }
    fetchProductosAPI()
      .then(setProducts)
      .catch((e) => setError(`${e.status ?? ''} ${e.message}`))
      .finally(() => setLoading(false));
  }, [isAuthenticated, isLoading]);

  const term = searchTerm?.toLowerCase();
  const filteredProducts = products.filter((p) =>
    term ? p.nombre.toLowerCase().includes(term) || (p.categoria || '').toLowerCase().includes(term) : true
  );

  const categorias = [
    { cat: 'Teclados', label: 'Teclados Mecánicos', Icon: Keyboard, color: '#00f2fe' },
    { cat: 'Pantallas', label: 'Pantallas & Monitores', Icon: Tv, color: '#7f00ff' },
    { cat: 'Periféricos', label: 'Mouse & Audio', Icon: Mouse, color: '#f107a3' },
    { cat: 'Muebles Gamer', label: 'Sillas & Escritorios', Icon: Monitor, color: '#00f2fe' },
  ];

  return (
    <div className="w-full space-y-14 md:space-y-20">

      <section className="relative glass-card p-8 sm:p-12 md:p-14 overflow-hidden border-cyan-500/20 w-full">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 items-center w-full">
          <div className="lg:col-span-7 space-y-6 z-10">
            <div className="inline-flex items-center gap-2 badge-neon">
              <Sparkles className="w-3.5 h-3.5" /> Equipamiento de Alto Rendimiento
            </div>
            <h1 className="text-3xl sm:text-5xl lg:text-6xl font-extrabold text-white leading-tight">
              ELEVA TU <span className="bg-gradient-to-r from-[#00f2fe] via-[#4facfe] to-[#7f00ff] bg-clip-text text-transparent">SETUP GAMER</span>
            </h1>
            <p className="text-slate-300 text-sm sm:text-base leading-relaxed">
              Explora nuestra colección de periféricos pro, pantallas 240Hz, teclados mecánicos personalizados y accesorios diseñados para ganar.
            </p>
            <div className="flex flex-wrap items-center gap-4 pt-2">
              <Link to="/catalogo" className="btn-gamer-primary">
                Ver Catálogo <ArrowRight className="w-4 h-4" />
              </Link>
            </div>
          </div>

          <div className="lg:col-span-5 relative flex justify-center w-full">
            <div className="relative w-full aspect-video lg:aspect-[4/3] max-h-[380px] overflow-hidden rounded-2xl border border-white/10 shadow-2xl shadow-[#00f2fe]/20">
              <img
                src="https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=800&q=80"
                alt="Setup Gamer"
                className="w-full h-full object-cover transform hover:scale-105 transition-transform duration-500"
              />
            </div>
          </div>
        </div>
      </section>

      <section className="space-y-6 w-full">
        <h2 className="text-xl sm:text-2xl font-bold text-white flex items-center gap-2">
          Categorías <span className="text-[#00f2fe]">Destacadas</span>
        </h2>
        <div className="grid grid-cols-2 sm:grid-cols-2 lg:grid-cols-4 gap-6 w-full">
          {categorias.map(({ cat, label, Icon, color }) => (
            <Link key={cat} to={`/catalogo?cat=${cat}`} className="glass-card p-6 flex flex-col items-center gap-4 text-center hover:border-[#00f2fe] group">
              <div className="p-4 rounded-2xl group-hover:scale-110 transition-transform" style={{ backgroundColor: `${color}1a`, color }}>
                <Icon className="w-8 h-8" />
              </div>
              <span className="font-bold text-sm sm:text-base text-white">{label}</span>
            </Link>
          ))}
        </div>
      </section>

      <section className="space-y-6 w-full">
        <div className="flex items-center justify-between">
          <h2 className="text-xl sm:text-2xl font-bold text-white">
            Productos <span className="text-[#00f2fe]">Gamer</span>
          </h2>
          <Link to="/catalogo" className="text-xs font-bold text-[#00f2fe] hover:underline flex items-center gap-1">
            Ver Todos <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        {!isAuthenticated && !isLoading ? (
          <div className="glass-card p-12 text-center space-y-4">
            <Lock className="w-10 h-10 text-[#00f2fe] mx-auto" />
            <p className="text-sm text-slate-300">Inicia sesión para ver los productos.</p>
            <Link to="/login" className="btn-gamer-primary inline-flex">Ingresar</Link>
          </div>
        ) : loading ? (
          <div className="text-center py-12 text-slate-400">Cargando productos gamer...</div>
        ) : error ? (
          <div className="glass-card p-12 text-center text-red-400 text-sm">{error}</div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 2xl:grid-cols-6 gap-8 w-full">
            {filteredProducts.map((prod) => (
              <ProductCard key={prod.id} product={prod} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
};