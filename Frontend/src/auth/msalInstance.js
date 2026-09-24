import { PublicClientApplication, EventType } from '@azure/msal-browser';
import { msalConfig } from './msalConfig';

// Instancia única de MSAL, usada por React y por las llamadas a la API
export const msalInstance = new PublicClientApplication(msalConfig);

// Al iniciar sesión (o renovar token), se marca esa cuenta como activa
msalInstance.addEventCallback((event) => {
  const isSuccess =
    event.eventType === EventType.LOGIN_SUCCESS ||
    event.eventType === EventType.ACQUIRE_TOKEN_SUCCESS;

  if (isSuccess && event.payload?.account) {
    msalInstance.setActiveAccount(event.payload.account);
  }
});