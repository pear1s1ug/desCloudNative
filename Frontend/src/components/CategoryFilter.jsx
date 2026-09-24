export const CategoryFilter = ({ categories, selectedCategory, onSelectCategory }) => {
  return (
    <div className="flex items-center gap-2 overflow-x-auto pb-4 no-scrollbar">
      {categories.map((cat) => (
        <button
          key={cat}
          onClick={() => onSelectCategory(cat)}
          className={`px-5 py-2.5 rounded-full text-xs font-bold transition-all whitespace-nowrap ${
            selectedCategory === cat
              ? 'bg-gradient-to-r from-[#00f2fe] to-[#7f00ff] text-black shadow-lg shadow-[#00f2fe]/30'
              : 'bg-[#121622] text-slate-300 hover:bg-[#1a2032] border border-white/10'
          }`}
        >
          {cat}
        </button>
      ))}
    </div>
  );
};
