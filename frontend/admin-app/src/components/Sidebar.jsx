import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const navItems = [
  { to: '/dashboard', label: 'Dashboard',  icon: '📊' },
  { to: '/users',     label: 'Users',      icon: '👤' },
  { to: '/jobs',      label: 'Jobs',       icon: '💼' },
  { to: '/recruiters',label: 'Recruiters', icon: '🏢' },
];

export default function Sidebar() {
  const { username, signOut } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    signOut();
    navigate('/login');
  }

  return (
    <aside className="fixed top-0 left-0 h-screen w-56 bg-slate-900 flex flex-col z-50">
      {/* Brand */}
      <div className="flex items-center gap-2.5 px-5 py-5 border-b border-white/10">
        <div className="w-8 h-8 rounded-lg bg-indigo-500 flex items-center justify-center text-white font-black text-sm">
          H
        </div>
        <div>
          <div className="text-white font-bold text-sm leading-tight">HireFlow</div>
          <div className="text-indigo-300 text-xs font-semibold tracking-wider">ADMIN</div>
        </div>
      </div>

      {/* Nav */}
      <nav className="flex-1 px-3 py-4 space-y-0.5">
        <div className="text-slate-500 text-xs font-bold uppercase tracking-widest px-2 pb-2">
          Menu
        </div>
        {navItems.map(({ to, label, icon }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              `flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm font-medium transition-all ` +
              (isActive
                ? 'bg-indigo-500/20 text-indigo-300'
                : 'text-slate-400 hover:bg-white/5 hover:text-white')
            }
          >
            <span className="w-4 text-center">{icon}</span>
            {label}
          </NavLink>
        ))}
      </nav>

      {/* Footer */}
      <div className="px-3 py-4 border-t border-white/10">
        <div className="flex items-center gap-2.5 px-2 mb-3">
          <div className="w-7 h-7 rounded-full bg-gradient-to-br from-indigo-500 to-purple-500 flex items-center justify-center text-white text-xs font-bold">
            {username?.[0]?.toUpperCase() ?? 'A'}
          </div>
          <div>
            <div className="text-slate-200 text-xs font-semibold">{username ?? 'Admin'}</div>
            <div className="text-slate-500 text-xs">Administrator</div>
          </div>
        </div>
        <button
          onClick={handleLogout}
          className="w-full flex items-center gap-2 px-3 py-2 rounded-lg text-red-400 text-sm font-medium hover:bg-red-500/10 transition-all"
        >
          <span>↪</span> Sign out
        </button>
      </div>
    </aside>
  );
}