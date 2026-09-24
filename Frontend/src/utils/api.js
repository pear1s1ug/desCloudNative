import { InteractionRequiredAuthError } from '@azure/msal-browser';
import { msalInstance } from '../auth/msalInstance';
import { tokenRequest } from '../auth/msalConfig';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8081';

export class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

// Obtiene el access token para la API (silencioso; si expiró la sesión, redirige al login)
export const getAccessToken = async () => {
  const account = msalInstance.getActiveAccount() || msalInstance.getAllAccounts()[0];
  if (!account) throw new ApiError(401, 'No hay sesión activa');

  try {
    const result = await msalInstance.acquireTokenSilent({ ...tokenRequest, account });
    return result.accessToken;
  } catch (error) {
    if (error instanceof InteractionRequiredAuthError) {
      await msalInstance.acquireTokenRedirect({ ...tokenRequest, account });
    }
    throw error;
  }
};

// "Interceptor": toda llamada al backend lleva Authorization: Bearer <token>
export const apiFetch = async (path, options = {}) => {
  const token = await getAccessToken();

  const res = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
      Authorization: `Bearer ${token}`,
    },
  });

  if (res.status === 204) return null;

  const text = await res.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = { mensaje: text };
    }
  }

  if (!res.ok) {
    throw new ApiError(res.status, data?.mensaje || `Error ${res.status}`);
  }
  return data;
};

// Productos
export const fetchProductosAPI = () => apiFetch('/api/productos');
export const crearProductoAPI = (producto) =>
  apiFetch('/api/productos', { method: 'POST', body: JSON.stringify(producto) });
export const eliminarProductoAPI = (id) =>
  apiFetch(`/api/productos/${id}`, { method: 'DELETE' });

// Usuarios
export const fetchUsuariosAPI = () => apiFetch('/api/usuarios');
export const fetchMiPerfilAPI = () => apiFetch('/api/usuarios/me');

// Claims validados por el BFF
export const fetchClaimsBackendAPI = () => apiFetch('/api/me');