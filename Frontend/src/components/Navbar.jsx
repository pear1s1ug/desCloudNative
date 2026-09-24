import { Link } from 'react-router-dom';
import { ShoppingBag, Gamepad2, User, LogOut, Search, ShieldCheck } from 'lucide-react';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';
import { ROLES } from '../auth/msalConfig';

export const Navbar = ({ searchTerm, setSearchTerm }) => {
  const { totalItems } = useCart();
  const { user, logout, isAuthenticated, hasRole } = useAuth();

  return (
    <header className="sticky top-0 z-50 backdrop-blur-md bg-[#090a10]/90 border-b border-white/10 w-full py-4">
      <div className="max-w-7xl mx-auto px-3 sm:px-4 lg:px-3 flex items-center justify-between gap-4 md:gap-8">

        <Link to="/" className="flex items-center gap-3 shrink-0 group">
          <div className="p-2.5 rounded-xl bg-gradient-to-tr from-[#00f2fe] to-[#7f00ff] text-black shadow-lg shadow-[#00f2fe]/30 group-hover:scale-105 transition-transform">
            <Gamepad2 className="w-6 h-6 stroke-[2.5]" />
          </div>
          <span className="font-gamer text-2xl sm:text-3xl font-bold tracking-wider bg-gradient-to-r from-white via-[#00f2fe] to-[#7f00ff] bg-clip-text text-transparent">
            NEXUS<span className="text-[#00f2fe]">GAMING</span>
          </span>
        </Link>

        <div className="flex-1 max-w-xl relative hidden md:block mx-4">
          <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="Buscar teclado, pantalla, mouse, accesorios..."
            value={searchTerm || ''}
            onChange={(e) => setSearchTerm && setSearchTerm(e.target.value)}
            className="w-full bg-[#121622] border border-white/10 rounded-full py-2.5 pl-10 pr-4 text-sm text-slate-200 placeholder-slate-400 focus:outline-none focus:border-[#00f2fe] focus:ring-1 focus:ring-[#00f2fe] transition-all"
          />
        </div>

        <nav className="flex items-center gap-4 sm:gap-6 shrink-0">
          <Link to="/" className="text-sm font-semibold hover:text-[#00f2fe] transition-colors hidden sm:inline">
            Inicio
          </Link>
          <Link to="/catalogo" className="text-sm font-semibold hover:text-[#00f2fe] transition-colors">
            Catálogo
          </Link>
          {hasRole(ROLES.ADMIN) && (
            <Link to="/admin" className="text-sm font-semibold text-[#7f00ff] hover:text-[#00f2fe] transition-colors flex items-center gap-1">
              <ShieldCheck className="w-4 h-4" /> Admin
            </Link>
          )}

          <Link to="/carrito" className="relative p-2.5 bg-[#121622] border border-white/10 rounded-xl hover:border-[#00f2fe] hover:text-[#00f2fe] transition-all">
            <ShoppingBag className="w-5 h-5" />
            {totalItems > 0 && (
              <span className="absolute -top-1.5 -right-1.5 bg-gradient-to-r from-[#00f2fe] to-[#7f00ff] text-black font-extrabold text-[11px] w-5 h-5 rounded-full flex items-center justify-center shadow-md shadow-[#00f2fe]/50">
                {totalItems}
              </span>
            )}
          </Link>

          {isAuthenticated ? (
            <div className="flex items-center gap-3 bg-[#121622] border border-white/10 rounded-xl px-3.5 py-1.5">
              <Link to="/perfil" className="text-xs font-semibold text-[#00f2fe] truncate max-w-[120px] hover:underline">
                {user?.nombre}
              </Link>
              <button
                onClick={logout}
                className="p-1 text-slate-400 hover:text-red-400 transition-colors"
                title="Cerrar sesión"
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          ) : (
            <Link to="/login" className="btn-gamer-outline text-xs px-4 py-2">
              <User className="w-4 h-4" /> Ingresar
            </Link>
          )}
        </nav>
      </div>
    </header>
  );
};