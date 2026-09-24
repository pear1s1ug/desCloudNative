import { ShoppingCart } from 'lucide-react';
import { formatPrice } from '../utils/formatters';
import { useCart } from '../context/CartContext';

export const ProductCard = ({ product }) => {
  const { addToCart } = useCart();

  return (
    <div className="glass-card flex flex-col justify-between overflow-hidden group h-full">
      {/* Imagen & Badge con Aspect Ratio Fijo */}
      <div className="relative w-full h-48 sm:h-52 overflow-hidden bg-slate-950">
        <img
          src={product.imagenUrl}
          alt={product.nombre}
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
        />
        <span className="absolute top-3 left-3 badge-neon shadow-md shadow-black/50">
          {product.categoria}
        </span>
      </div>

      {/* Contenido Card */}
      <div className="p-5 flex-1 flex flex-col justify-between gap-4">
        <div>
          <h3 className="font-bold text-base text-white group-hover:text-[#00f2fe] transition-colors line-clamp-1">
            {product.nombre}
          </h3>
          <p className="text-xs text-slate-400 mt-2 line-clamp-2 leading-relaxed">
            {product.descripcion}
          </p>
        </div>

        {/* Precio & Botón de Acción */}
        <div className="flex items-center justify-between pt-3 border-t border-white/10 mt-auto">
          <div>
            <span className="text-[10px] text-slate-400 uppercase font-bold tracking-wider block">Precio</span>
            <span className="font-gamer text-lg font-bold text-[#00f2fe]">
              {formatPrice(product.precio)}
            </span>
          </div>

          <button
            onClick={() => addToCart(product)}
            className="btn-gamer-primary p-2.5 rounded-lg text-xs"
            title="Agregar al Carrito"
          >
            <ShoppingCart className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
};
