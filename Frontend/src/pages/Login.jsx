import { Navigate, useLocation } from 'react-router-dom';
import { Gamepad2, Shield, UserPlus } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export const Login = () => {
  const { login, register, isAuthenticated, isLoading } = useAuth();
  const location = useLocation();
  const from = location.state?.from || '/';

  if (isAuthenticated) return <Navigate to={from} replace />;

  return (
    <div className="max-w-md mx-auto my-12 glass-card p-8 border-cyan-500/30">
      <div className="text-center space-y-3 mb-8">
        <div className="inline-flex p-3 rounded-2xl bg-gradient-to-tr from-[#00f2fe] to-[#7f00ff] text-black shadow-lg shadow-[#00f2fe]/30">
          <Gamepad2 className="w-8 h-8 stroke-[2.5]" />
        </div>
        <h1 className="text-2xl font-bold text-white">Iniciar Sesión</h1>
        <p className="text-xs text-slate-400">
          Autenticación con Microsoft (OpenID Connect + OAuth 2.0 PKCE)
        </p>
      </div>

      <button
        onClick={() => login(from)}
        disabled={isLoading}
        className="w-full bg-[#1e2436] hover:bg-[#283048] border border-white/10 text-white font-semibold py-3 px-4 rounded-xl text-sm flex items-center justify-center gap-3 transition-colors disabled:opacity-50"
      >
        <svg className="w-4 h-4" viewBox="0 0 21 21">
          <rect x="1" y="1" width="9" height="9" fill="#f25022" />
          <rect x="11" y="1" width="9" height="9" fill="#7fba00" />
          <rect x="1" y="11" width="9" height="9" fill="#00a4ef" />
          <rect x="11" y="11" width="9" height="9" fill="#ffb900" />
        </svg>
        {isLoading ? 'Procesando...' : 'Iniciar sesión con Microsoft'}
      </button>

      <button
        onClick={register}
        disabled={isLoading}
        className="btn-gamer-outline w-full justify-center mt-3 text-xs py-3 disabled:opacity-50"
      >
        <UserPlus className="w-4 h-4" /> Crear cuenta
      </button>

      <div className="text-center mt-6 text-xs text-slate-500">
        <Shield className="w-3.5 h-3.5 inline mr-1 text-[#00f2fe]" />
        Identidad gestionada por Azure AD · API protegida con JWT
      </div>
    </div>
  );
};