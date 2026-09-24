import { useEffect, useState } from 'react';
import { fetchClaimsBackendAPI, fetchMiPerfilAPI } from '../utils/api';
import { useAuth } from '../context/AuthContext';

const Chip = ({ children }) => (
  <span className="badge-neon mr-2 mb-2 inline-block">{children}</span>
);

export const Profile = () => {
  const { user, roles, scopes, accessClaims } = useAuth();
  const [backend, setBackend] = useState(null);
  const [perfil, setPerfil] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([fetchClaimsBackendAPI(), fetchMiPerfilAPI()])
      .then(([claims, miPerfil]) => {
        setBackend(claims);
        setPerfil(miPerfil);
      })
      .catch((e) => setError(`${e.status ?? ''} ${e.message}`));
  }, []);

  const exp = accessClaims.exp ? new Date(accessClaims.exp * 1000).toLocaleString('es-CL') : '-';

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-extrabold text-white">
        Mi <span className="text-[#00f2fe]">Perfil</span>
      </h1>

      {error && (
        <div className="bg-red-500/10 border border-red-500/30 text-red-400 p-3 rounded-xl text-xs">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="glass-card p-6 space-y-4">
          <h2 className="font-bold text-white">Claims del token (frontend)</h2>
          <p className="text-sm text-slate-300">{user?.nombre} · {user?.email}</p>
          <div>
            <span className="text-xs text-slate-400 block mb-1">Roles</span>
            {roles.length
              ? roles.map((r) => <Chip key={r}>{r}</Chip>)
              : <span className="text-xs text-slate-500">Sin rol asignado</span>}
          </div>
          <div>
            <span className="text-xs text-slate-400 block mb-1">Scopes (scp)</span>
            {scopes.map((s) => <Chip key={s}>{s}</Chip>)}
          </div>
          <div className="text-xs text-slate-400 space-y-1 break-all">
            <p><b className="text-slate-200">aud:</b> {String(accessClaims.aud ?? '-')}</p>
            <p><b className="text-slate-200">iss:</b> {accessClaims.iss ?? '-'}</p>
            <p><b className="text-slate-200">exp:</b> {exp}</p>
          </div>
        </div>

        <div className="glass-card p-6 space-y-4">
          <h2 className="font-bold text-white">Respuesta del BFF (token validado)</h2>
          <pre className="text-[11px] text-slate-300 bg-[#0b0d14] p-3 rounded-xl overflow-auto max-h-64">
            {backend ? JSON.stringify(backend, null, 2) : 'Cargando...'}
          </pre>
          <h2 className="font-bold text-white">Perfil en base de datos</h2>
          <pre className="text-[11px] text-slate-300 bg-[#0b0d14] p-3 rounded-xl overflow-auto">
            {perfil ? JSON.stringify(perfil, null, 2) : 'Cargando...'}
          </pre>
        </div>
      </div>
    </div>
  );
};