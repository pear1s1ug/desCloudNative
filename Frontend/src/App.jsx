import { useState } from 'react';
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { CartProvider } from './context/CartContext';
import { MainLayout } from './layouts/MainLayout';
import { AppRouter } from './router/AppRouter';

export function App() {
  const [searchTerm, setSearchTerm] = useState('');

  return (
    <BrowserRouter>
      <AuthProvider>
        <CartProvider>
          <MainLayout searchTerm={searchTerm} setSearchTerm={setSearchTerm}>
            <AppRouter searchTerm={searchTerm} />
          </MainLayout>
        </CartProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
