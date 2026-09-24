import { Navbar } from '../components/Navbar';
import { Footer } from '../components/Footer';

export const MainLayout = ({ children, searchTerm, setSearchTerm }) => {
  return (
    <div className="min-h-screen flex flex-col justify-between w-full bg-[#090a10] text-slate-100 overflow-x-hidden">
      <Navbar searchTerm={searchTerm} setSearchTerm={setSearchTerm} />
      <main className="flex-1 w-full py-8 md:py-12">
        <div className="max-w-7xl mx-auto px-3 sm:px-4 lg:px-3">
          {children}
        </div>
      </main>
      <Footer />
    </div>
  );
};
