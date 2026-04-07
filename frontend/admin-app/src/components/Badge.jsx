export default function Badge({ text, variant = 'default' }) {
  const variants = {
    success:  'bg-emerald-50 text-emerald-700 border-emerald-200',
    danger:   'bg-red-50 text-red-700 border-red-200',
    warning:  'bg-amber-50 text-amber-700 border-amber-200',
    info:     'bg-indigo-50 text-indigo-700 border-indigo-200',
    default:  'bg-slate-100 text-slate-600 border-slate-200',
    active:   'bg-emerald-50 text-emerald-700 border-emerald-200',
    blocked:  'bg-red-50 text-red-700 border-red-200',
    pending:  'bg-amber-50 text-amber-700 border-amber-200',
    rejected: 'bg-red-50 text-red-700 border-red-200',
    suspended:'bg-orange-50 text-orange-700 border-orange-200',
  };

  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold border ${variants[variant] ?? variants.default}`}>
      {text}
    </span>
  );
}