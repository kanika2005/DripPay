import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard,
  Send,
  QrCode,
  List,
  Target,
  Users,
  Settings,
  Sparkles,
  LogOut,
  User,
} from 'lucide-react';
import Logo from './Logo';
import NotificationBell from './NotificationBell';
import ThemeToggle from './ThemeToggle';
import SimulatedMoneyFooter from './SimulatedMoneyFooter';
import { useAuth } from '../context/AuthContext';

const nav = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/send', label: 'Send', icon: Send },
  { to: '/collect', label: 'Collect', icon: QrCode },
  { to: '/transactions', label: 'History', icon: List },
  { to: '/budgets', label: 'Budgets', icon: Target },
  { to: '/groups', label: 'Groups', icon: Users },
  { to: '/insights/chat', label: 'Assistant', icon: Sparkles },
];

export default function AppLayout() {
  const { user, logout, isAdmin } = useAuth();
  const navigate = useNavigate();

  return (
    <div className="flex min-h-screen bg-drip-bg lg:flex">
      <aside className="hidden w-60 flex-col border-r border-drip-border bg-drip-surface/80 p-4 lg:flex">
        <Logo />
        <nav className="mt-8 flex flex-1 flex-col gap-0.5" aria-label="Main">
          {nav.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm transition ${
                  isActive
                    ? 'bg-drip-primary/15 font-medium text-drip-primary'
                    : 'text-drip-muted hover:bg-drip-border/25 hover:text-drip-foreground'
                }`
              }
            >
              <item.icon size={18} aria-hidden="true" />
              {item.label}
            </NavLink>
          ))}
          {isAdmin && (
            <NavLink
              to="/admin"
              className={({ isActive }) =>
                `mt-2 flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm ${
                  isActive
                    ? 'bg-drip-reversed/15 font-medium text-drip-reversed'
                    : 'text-drip-muted hover:bg-drip-border/25'
                }`
              }
            >
              <Settings size={18} /> Admin
            </NavLink>
          )}
        </nav>
        <div className="space-y-2 border-t border-drip-border pt-4">
          <NavLink to="/profile" className="btn-ghost w-full justify-start !text-sm">
            <User size={18} /> Profile
          </NavLink>
          <button
            type="button"
            className="btn-ghost w-full justify-start !text-sm"
            onClick={() => {
              logout();
              navigate('/');
            }}
          >
            <LogOut size={18} /> Log out
          </button>
        </div>
      </aside>

      <div className="flex min-h-screen flex-1 flex-col">
        <header className="flex items-center justify-between border-b border-drip-border px-4 py-3 lg:px-8">
          <div className="lg:hidden">
            <Logo compact />
          </div>
          <p className="hidden text-sm text-drip-muted sm:block">
            <span className="text-drip-foreground">{user?.fullName}</span>
            <span className="mx-2 text-drip-border">·</span>
            <span className="font-mono text-xs">{user?.publicWalletId}</span>
          </p>
          <div className="flex items-center gap-1">
            <ThemeToggle />
            <NotificationBell />
          </div>
        </header>

        <main className="flex-1 p-4 pb-24 lg:p-8 lg:pb-6">
          <Outlet />
        </main>
        <SimulatedMoneyFooter />
      </div>

      <nav
        className="fixed bottom-0 left-0 right-0 flex border-t border-drip-border bg-drip-surface/95 backdrop-blur lg:hidden"
        aria-label="Mobile"
      >
        {nav.slice(0, 5).map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              `flex flex-1 flex-col items-center gap-1 py-2 text-[10px] ${
                isActive ? 'font-medium text-drip-primary' : 'text-drip-muted'
              }`
            }
          >
            <item.icon size={18} />
            {item.label}
          </NavLink>
        ))}
      </nav>
    </div>
  );
}
