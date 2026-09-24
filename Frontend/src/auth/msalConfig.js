const apiScope = import.meta.env.VITE_API_SCOPE;
const knownAuthority = import.meta.env.VITE_AZURE_KNOWN_AUTHORITY;
const redirectUri = import.meta.env.VITE_REDIRECT_URI || window.location.origin;

// MSAL usa Authorization Code Flow con PKCE (code_verifier / code_challenge S256)
// y valida state y nonce automáticamente. No se usa Implicit Flow.
export const msalConfig = {
  auth: {
    clientId: import.meta.env.VITE_AZURE_CLIENT_ID,
    authority: import.meta.env.VITE_AZURE_AUTHORITY,
    knownAuthorities: knownAuthority ? [knownAuthority] : [],
    redirectUri,
    postLogoutRedirectUri: redirectUri,
  },
  cache: {
    cacheLocation: 'sessionStorage',
  },
};

// Scope de nuestra API (api://<CLIENT_ID>/access_as_user)
export const loginRequest = { scopes: [apiScope] };
export const tokenRequest = { scopes: [apiScope] };

// Valores de los App roles definidos en Azure
export const ROLES = {
  ADMIN: 'Admin',
  CLIENTE: 'Cliente',
};