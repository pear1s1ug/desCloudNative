import { Link } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { formatPrice } from '../utils/formatters';
import { Trash2, Plus, Minus, ArrowLeft, ShoppingCart, CheckCircle2 } from 'lucide-react';
import { useState } from 'react';

export const CartPage = () => {
  const { cart, removeFromCart, updateQuantity, clearCart, totalPrice } = useCart();
  const [completed, setCompleted] = useState(false);

  const handleCheckout = () => {
    setCompleted(true);
    clearCart();
  };

  if (completed) {
    return (
      <div className="max-w-md mx-auto my-16 glass-card p-8 text-center space-y-4">
        <CheckCircle2 className="w-16 h-16 text-[#00f2fe] mx-auto animate-bounce" />
        <h1 className="text-2xl font-bold text-white">¡Compra Realizada con Éxito!</h1>
        <p className="text-xs text-slate-400">
          Tu pedido ha sido procesado por el backend de la Tienda Gamer. Recibirás tu comprobante pronto.
        </p>
        <Link to="/catalogo" className="btn-gamer-primary inline-flex mt-4">
          Volver a la Tienda
        </Link>
      </div>
    );
  }

  if (cart.length === 0) {
    return (
      <div className="max-w-md mx-auto my-16 glass-card p-12 text-center space-y-4">
        <ShoppingCart className="w-16 h-16 text-slate-600 mx-auto" />
        <h1 className="text-xl font-bold text-white">Tu carrito está vacío</h1>
        <p className="text-xs text-slate-400">Explora nuestro catálogo gamer y añade tus productos preferidos.</p>
        <Link to="/catalogo" className="btn-gamer-primary inline-flex mt-4">
          Explorar Productos
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <div className="flex items-center gap-4">
        <Link to="/catalogo" className="text-slate-400 hover:text-[#00f2fe]">
          <ArrowLeft className="w-6 h-6" />
        </Link>
        <h1 className="text-3xl font-extrabold text-white">
          Carrito de <span className="text-[#00f2fe]">Compras</span>
        </h1>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Lista de Productos */}
        <div className="lg:col-span-2 space-y-4">
          {cart.map((item) => (
            <div key={item.id} className="glass-card p-4 flex flex-col sm:flex-row items-center justify-between gap-4">
              <div className="flex items-center gap-4">
                <img
                  src={item.imagenUrl}
                  alt={item.nombre}
                  className="w-20 h-20 object-cover rounded-xl bg-slate-900 border border-white/10"
                />
                <div>
                  <h3 className="font-bold text-sm text-white">{item.nombre}</h3>
                  <span className="text-xs text-[#00f2fe] font-bold block mt-1">
                    {formatPrice(item.precio)}
                  </span>
                </div>
              </div>

              {/* Controles Cantidad */}
              <div className="flex items-center gap-6">
                <div className="flex items-center gap-2 bg-[#121622] px-3 py-1.5 rounded-lg border border-white/10">
                  <button
                    onClick={() => updateQuantity(item.id, item.quantity - 1)}
                    className="text-slate-400 hover:text-white"
                  >
                    <Minus className="w-4 h-4" />
                  </button>
                  <span className="text-sm font-bold text-white w-6 text-center">{item.quantity}</span>
                  <button
                    onClick={() => updateQuantity(item.id, item.quantity + 1)}
                    className="text-slate-400 hover:text-white"
                  >
                    <Plus className="w-4 h-4" />
                  </button>
                </div>

                <button
                  onClick={() => removeFromCart(item.id)}
                  className="text-slate-500 hover:text-red-400 transition-colors"
                >
                  <Trash2 className="w-5 h-5" />
                </button>
              </div>
            </div>
          ))}
        </div>

        {/* Resumen de Pago */}
        <div className="glass-card p-6 h-fit space-y-6">
          <h2 className="text-lg font-bold text-white border-b border-white/10 pb-3">Resumen de Orden</h2>
          
          <div className="space-y-3 text-xs text-slate-300">
            <div className="flex justify-between">
              <span>Subtotal</span>
              <span>{formatPrice(totalPrice)}</span>
            </div>
            <div className="flex justify-between">
              <span>Despacho</span>
              <span className="text-emerald-400 font-bold">GRATIS</span>
            </div>
            <div className="flex justify-between text-sm font-bold text-white pt-3 border-t border-white/10">
              <span>Total a Pagar</span>
              <span className="text-[#00f2fe]">{formatPrice(totalPrice)}</span>
            </div>
          </div>

          <button onClick={handleCheckout} className="btn-gamer-primary w-full justify-center">
            Pagar Pedido
          </button>
        </div>
      </div>
    </div>
  );
};
