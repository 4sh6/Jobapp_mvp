import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useSidebar } from '../context/SidebarContext';

const navItems = [
  { to: '/dashboard',  label: 'Dashboard',  icon: '📊' },
  { to: '/users',      label: 'Users',      icon: '👤' },
  { to: '/jobs',       label: 'Jobs',       icon: '💼' },
  { to: '/recruiters', label: 'Recruiters', icon: '🏢' },
  { to: '/referrals',  label: 'Referrals',  icon: '🎁' },
];

export default function Sidebar() {
  const { username, signOut } = useAuth();
  const { collapsed, toggle } = useSidebar();
  const navigate = useNavigate();

  function handleLogout() {
    signOut();
    navigate('/login');
  }

  return (
    <aside
      className={`fixed top-0 left-0 h-screen bg-slate-900 flex flex-col z-50 transition-all duration-300 ${
        collapsed ? 'w-16' : 'w-56'
      }`}
    >
      {/* Brand + toggle */}
      <div className="flex items-center justify-between px-3 py-5 border-b border-white/10 min-h-[64px]">
        {!collapsed && (
          <div className="flex items-center gap-2.5 overflow-hidden">
            <div className="w-8 h-8 rounded-lg bg-indigo-500 flex items-center justify-center text-white font-black text-sm flex-shrink-0">
              H
            </div>
            <div>
              <div className="text-white font-bold text-sm leading-tight">HireFlow</div>
              <div className="text-indigo-300 text-xs font-semibold tracking-wider">ADMIN</div>
            </div>
          </div>
        )}
        {collapsed && (
          <div className="w-8 h-8 rounded-lg bg-indigo-500 flex items-center justify-center text-white font-black text-sm mx-auto">
            H
          </div>
        )}
        <button
          onClick={toggle}
          title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          className={`flex-shrink-0 w-7 h-7 rounded-md flex items-center justify-center text-slate-400 hover:bg-white/10 hover:text-white transition-all ${
            collapsed ? 'hidden' : ''
          }`}
        >
          ☰
        </button>
      </div>

      {/* Expand button when collapsed */}
      {collapsed && (
        <button
          onClick={toggle}
          title="Expand sidebar"
          className="mx-auto mt-3 w-8 h-8 rounded-md flex items-center justify-center text-slate-400 hover:bg-white/10 hover:text-white transition-all"
        >
          ☰
        </button>
      )}

      {/* Nav */}
      <nav className="flex-1 px-2 py-4 space-y-0.5">
        {!collapsed && (
          <div className="text-slate-500 text-xs font-bold uppercase tracking-widest px-2 pb-2">
            Menu
          </div>
        )}
        {navItems.map(({ to, label, icon }) => (
          <NavLink
            key={to}
            to={to}
            title={collapsed ? label : undefined}
            className={({ isActive }) =>
              `flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm font-medium transition-all ` +
              (collapsed ? 'justify-center' : '') + ' ' +
              (isActive
                ? 'bg-indigo-500/20 text-indigo-300'
                : 'text-slate-400 hover:bg-white/5 hover:text-white')
            }
          >
            <span className="w-4 text-center flex-shrink-0">{icon}</span>
            {!collapsed && <span>{label}</span>}
          </NavLink>
        ))}
      </nav>

      {/* Footer */}
      <div className="px-2 py-4 border-t border-white/10">
        {!collapsed && (
          <div className="flex items-center gap-2.5 px-2 mb-3">
            <div className="w-7 h-7 rounded-full bg-gradient-to-br from-indigo-500 to-purple-500 flex items-center justify-center text-white text-xs font-bold flex-shrink-0">
              {username?.[0]?.toUpperCase() ?? 'A'}
            </div>
            <div className="overflow-hidden">
              <div className="text-slate-200 text-xs font-semibold truncate">{username ?? 'Admin'}</div>
              <div className="text-slate-500 text-xs">Administrator</div>
            </div>
          </div>
        )}
        {collapsed && (
          <div className="flex justify-center mb-3">
            <div
              title={username ?? 'Admin'}
              className="w-7 h-7 rounded-full bg-gradient-to-br from-indigo-500 to-purple-500 flex items-center justify-center text-white text-xs font-bold"
            >
              {username?.[0]?.toUpperCase() ?? 'A'}
            </div>
          </div>
        )}
        <button
          onClick={handleLogout}
          title={collapsed ? 'Sign out' : undefined}
          className={`w-full flex items-center gap-2 px-3 py-2 rounded-lg text-red-400 text-sm font-medium hover:bg-red-500/10 transition-all ${
            collapsed ? 'justify-center' : ''
          }`}
        >
          <span>↪</span>
          {!collapsed && 'Sign out'}
        </button>
      </div>
    </aside>
  );
}