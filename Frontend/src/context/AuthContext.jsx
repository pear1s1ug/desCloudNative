import { createContext, useContext, useEffect, useState } from 'react';
import { useMsal, useIsAuthenticated } from '@azure/msal-react';
import { InteractionStatus } from '@azure/msal-browser';
import { loginRequest } from '../auth/msalConfig';
import { getAccessToken } from '../utils/api';
import { decodeJwt } from '../utils/jwt';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const { instance, accounts, inProgress } = useMsal();
  const isAuthenticated = useIsAuthenticated();
  const account = instance.getActiveAccount() || accounts[0] || null;
  const idClaims = account?.idTokenClaims || {};
  const [accessClaims, setAccessClaims] = useState({});

  // Lee los claims del access token (scp, roles, aud, iss, exp)
  useEffect(() => {
    if (!isAuthenticated || inProgress !== InteractionStatus.None) return;
    let active = true;
    getAccessToken()
      .then((token) => active && setAccessClaims(decodeJwt(token)))
      .catch(() => active && setAccessClaims({}));
    return () => {
      active = false;
    };
  }, [isAuthenticated, inProgress, account?.homeAccountId]);

  const roles = idClaims.roles || accessClaims.roles || [];
  const scopes = accessClaims.scp ? accessClaims.scp.split(' ') : [];

  const user = account
    ? {
        nombre: idClaims.name || account.name || account.username,
        email: idClaims.email || idClaims.preferred_username || account.username,
      }
    : null;

  // Login con Microsoft (redirect + Authorization Code con PKCE)
  const login = (from = '/') =>
    instance.loginRedirect({ ...loginRequest, redirectStartPage: window.location.origin + from });

  // Registro (flujo sign-up; aplica en tenant External ID)
  const register = () => instance.loginRedirect({ ...loginRequest, prompt: 'create' });

  const logout = () => instance.logoutRedirect({ account });

  const hasRole = (role) => roles.includes(role);

  return (
    <AuthContext.Provider
      value={{
        user,
        roles,
        scopes,
        idClaims,
        accessClaims: isAuthenticated ? accessClaims : {},
        isAuthenticated,
        isLoading: inProgress !== InteractionStatus.None,
        login,
        register,
        logout,
        hasRole,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);