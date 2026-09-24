import { Navigate, useLocation, Link } from 'react-router-dom';
import { ShieldAlert } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

// Equivalente al MsalGuard de Angular: exige sesión y, opcionalmente, un rol.
export const ProtectedRoute = ({ children, roles = [] }) => {
  const { isAuthenticated, isLoading, hasRole } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return <div className="text-center py-12 text-slate-400">Verificando sesión...</div>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />;
  }

  if (roles.length > 0 && !roles.some(hasRole)) {
    return (
      <div className="max-w-md mx-auto my-16 glass-card p-8 text-center space-y-4">
        <ShieldAlert className="w-14 h-14 text-red-400 mx-auto" />
        <h1 className="text-xl font-bold text-white">403 · Acceso denegado</h1>
        <p className="text-xs text-slate-400">
          Tu cuenta no tiene el rol requerido: {roles.join(', ')}
        </p>
        <Link to="/" className="btn-gamer-primary inline-flex">Volver al inicio</Link>
      </div>
    );
  }

  return children;
};