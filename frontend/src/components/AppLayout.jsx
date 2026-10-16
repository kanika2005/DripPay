import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard,
  Wallet,
  Send,
  List,
  PieChart,
  Target,
  Users,
  Shield,
  User,
  LogOut,
  Settings,
} from 'lucide-react';
import Logo from './Logo';
import { useAuth } from '../context/AuthContext';

const nav = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/wallet', label: 'Wallet', icon: Wallet },
  { to: '/send', label: 'Send', icon: Send },
  { to: '/transactions', label: 'Transactions', icon: List },
  { to: '/analytics', label: 'SpendSense', icon: PieChart },
  { to: '/budgets', label: 'Budgets', icon: Target },
  { to: '/groups', label: 'SplitSmart', icon: Users },
  { to: '/security', label: 'FraudWatch', icon: Shield },
  { to: '/profile', label: 'Profile', icon: User },
];

export default function AppLayout() {
  const { user, logout, isAdmin } = useAuth();
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-drip-bg lg:flex">
      <aside className="hidden w-64 flex-col border-r border-white/5 bg-black/20 p-4 lg:flex">
        <Logo />
        <nav className="mt-8 flex flex-1 flex-col gap-1">
          {nav.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm transition ${
                  isActive ? 'bg-drip-lime/15 text-drip-lime' : 'text-drip-muted hover:bg-white/5 hover:text-white'
                }`
              }
            >
              <item.icon size={18} />
              {item.label}
            </NavLink>
          ))}
          {isAdmin && (
            <NavLink
              to="/admin"
              className={({ isActive }) =>
                `mt-2 flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm ${
                  isActive ? 'bg-drip-purple/20 text-drip-purple' : 'text-drip-muted hover:bg-white/5'
                }`
              }
            >
              <Settings size={18} /> Admin
            </NavLink>
          )}
        </nav>
        <button type="button" className="btn-ghost w-full justify-start" onClick={() => { logout(); navigate('/'); }}>
          <LogOut size={18} /> Log out
        </button>
      </aside>

      <div className="flex min-h-screen flex-1 flex-col">
        <header className="flex items-center justify-between border-b border-white/5 px-4 py-3 lg:px-8">
          <div className="lg:hidden"><Logo compact /></div>
          <p className="text-sm text-drip-muted">
            Hey, <span className="text-white">{user?.fullName}</span>
          </p>
          <p className="hidden text-xs text-drip-muted sm:block">{user?.publicWalletId}</p>
        </header>

        <main className="flex-1 p-4 pb-24 lg:p-8 lg:pb-8">
          <Outlet />
        </main>
      </div>

      <nav className="fixed bottom-0 left-0 right-0 flex border-t border-white/10 bg-drip-bg/95 backdrop-blur lg:hidden">
        {nav.slice(0, 5).map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              `flex flex-1 flex-col items-center gap-1 py-2 text-[10px] ${
                isActive ? 'text-drip-lime' : 'text-drip-muted'
              }`
            }
          >
            <item.icon size={18} />
            {item.label.split(' ')[0]}
          </NavLink>
        ))}
      </nav>
    </div>
  );
}
