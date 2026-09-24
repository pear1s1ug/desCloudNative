import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { fetchProductosAPI } from '../utils/api';
import { ProductCard } from '../components/ProductCard';
import { CategoryFilter } from '../components/CategoryFilter';

export const Catalog = ({ searchTerm }) => {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchParams] = useSearchParams();
  const [selectedCategory, setSelectedCategory] = useState(searchParams.get('cat') || 'Todas');

  useEffect(() => {
    fetchProductosAPI()
      .then(setProducts)
      .catch((e) => setError(`${e.status ?? ''} ${e.message}`))
      .finally(() => setLoading(false));
  }, []);

  const categories = ['Todas', 'Teclados', 'Periféricos', 'Pantallas', 'Muebles Gamer'];

  const filtered = products.filter((p) => {
    const matchesCat = selectedCategory === 'Todas' || p.categoria === selectedCategory;
    const term = searchTerm?.toLowerCase();
    const matchesSearch =
      !term ||
      p.nombre.toLowerCase().includes(term) ||
      (p.descripcion || '').toLowerCase().includes(term);
    return matchesCat && matchesSearch;
  });

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-3xl font-extrabold text-white">
          Catálogo <span className="text-[#00f2fe]">Gamer</span>
        </h1>
        <p className="text-xs text-slate-400 mt-1">
          Filtra por categoría o encuentra el producto ideal para tu setup.
        </p>
      </div>

      <CategoryFilter
        categories={categories}
        selectedCategory={selectedCategory}
        onSelectCategory={setSelectedCategory}
      />

      {loading ? (
        <div className="text-center py-12 text-slate-400">Cargando catálogo...</div>
      ) : error ? (
        <div className="glass-card p-12 text-center text-red-400 text-sm">
          No se pudo cargar el catálogo: {error}
        </div>
      ) : filtered.length === 0 ? (
        <div className="glass-card p-12 text-center text-slate-400">
          No se encontraron productos en esta categoría o búsqueda.
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {filtered.map((prod) => (
            <ProductCard key={prod.id} product={prod} />
          ))}
        </div>
      )}
    </div>
  );
};