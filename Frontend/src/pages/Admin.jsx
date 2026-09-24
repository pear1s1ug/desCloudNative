import { useEffect, useState } from 'react';
import { Trash2, Plus } from 'lucide-react';
import {
  fetchUsuariosAPI,
  fetchProductosAPI,
  crearProductoAPI,
  eliminarProductoAPI,
} from '../utils/api';
import { formatPrice } from '../utils/formatters';

const PRODUCTO_VACIO = {
  nombre: '',
  descripcion: '',
  precio: '',
  stock: '',
  categoria: 'Teclados',
  imagenUrl: '',
};

const CAMPOS = [
  ['nombre', 'Nombre', 'text'],
  ['descripcion', 'Descripción', 'text'],
  ['precio', 'Precio', 'number'],
  ['stock', 'Stock', 'number'],
  ['imagenUrl', 'URL imagen', 'text'],
];

export const Admin = () => {
  const [usuarios, setUsuarios] = useState([]);
  const [productos, setProductos] = useState([]);
  const [form, setForm] = useState(PRODUCTO_VACIO);
  const [error, setError] = useState('');

  const cargar = async () => {
    try {
      const [u, p] = await Promise.all([fetchUsuariosAPI(), fetchProductosAPI()]);
      setUsuarios(u);
      setProductos(p);
      setError('');
    } catch (e) {
      setError(`${e.status ?? ''} ${e.message}`);
    }
  };

  useEffect(() => {
    cargar();
  }, []);

  const crear = async (e) => {
    e.preventDefault();
    try {
      await crearProductoAPI({ ...form, precio: Number(form.precio), stock: Number(form.stock) });
      setForm(PRODUCTO_VACIO);
      cargar();
    } catch (err) {
      setError(`${err.status ?? ''} ${err.message}`);
    }
  };

  const eliminar = async (id) => {
    try {
      await eliminarProductoAPI(id);
      cargar();
    } catch (err) {
      setError(`${err.status ?? ''} ${err.message}`);
    }
  };

  const input = 'w-full bg-[#121622] border border-white/10 rounded-xl px-3 py-2 text-sm text-white focus:outline-none focus:border-[#00f2fe]';

  return (
    <div className="space-y-8">
      <h1 className="text-3xl font-extrabold text-white">
        Panel <span className="text-[#00f2fe]">Admin</span>
      </h1>

      {error && (
        <div className="bg-red-500/10 border border-red-500/30 text-red-400 p-3 rounded-xl text-xs">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <form onSubmit={crear} className="glass-card p-6 space-y-3">
          <h2 className="font-bold text-white">Nuevo producto</h2>
          {CAMPOS.map(([campo, label, type]) => (
            <input
              key={campo}
              type={type}
              placeholder={label}
              required={campo !== 'descripcion' && campo !== 'imagenUrl'}
              value={form[campo]}
              onChange={(e) => setForm({ ...form, [campo]: e.target.value })}
              className={input}
            />
          ))}
          <select
            value={form.categoria}
            onChange={(e) => setForm({ ...form, categoria: e.target.value })}
            className={input}
          >
            {['Teclados', 'Periféricos', 'Pantallas', 'Muebles Gamer'].map((c) => (
              <option key={c}>{c}</option>
            ))}
          </select>
          <button type="submit" className="btn-gamer-primary w-full justify-center">
            <Plus className="w-4 h-4" /> Crear
          </button>
        </form>

        <div className="glass-card p-6 lg:col-span-2 space-y-3">
          <h2 className="font-bold text-white">Productos</h2>
          {productos.map((p) => (
            <div key={p.id} className="flex items-center justify-between text-sm border-b border-white/5 pb-2">
              <span className="text-slate-200">{p.nombre}</span>
              <div className="flex items-center gap-4">
                <span className="text-[#00f2fe]">{formatPrice(p.precio)}</span>
                <button onClick={() => eliminar(p.id)} className="text-slate-500 hover:text-red-400">
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="glass-card p-6 space-y-3">
        <h2 className="font-bold text-white">Usuarios registrados</h2>
        <table className="w-full text-xs text-left text-slate-300">
          <thead className="text-slate-400">
            <tr><th className="py-2">Nombre</th><th>Email</th><th>Rol</th></tr>
          </thead>
          <tbody>
            {usuarios.map((u) => (
              <tr key={u.id} className="border-t border-white/5">
                <td className="py-2">{u.nombre}</td>
                <td>{u.email}</td>
                <td>{u.rol}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};