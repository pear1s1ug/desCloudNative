import { Routes, Route } from 'react-router-dom';
import { Home } from '../pages/Home';
import { Catalog } from '../pages/Catalog';
import { Login } from '../pages/Login';
import { CartPage } from '../pages/CartPage';
import { Profile } from '../pages/Profile';
import { Admin } from '../pages/Admin';
import { ProtectedRoute } from './ProtectedRoute';
import { ROLES } from '../auth/msalConfig';

export const AppRouter = ({ searchTerm }) => {
  return (
    <Routes>
      <Route path="/" element={<Home searchTerm={searchTerm} />} />
      <Route path="/login" element={<Login />} />

      <Route
        path="/catalogo"
        element={
          <ProtectedRoute>
            <Catalog searchTerm={searchTerm} />
          </ProtectedRoute>
        }
      />
      <Route
        path="/carrito"
        element={
          <ProtectedRoute>
            <CartPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/perfil"
        element={
          <ProtectedRoute>
            <Profile />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin"
        element={
          <ProtectedRoute roles={[ROLES.ADMIN]}>
            <Admin />
          </ProtectedRoute>
        }
      />

      <Route path="*" element={<Home searchTerm={searchTerm} />} />
    </Routes>
  );
};