import { useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard,
  Send,
  QrCode,
  Wallet,
  List,
  Target,
  BarChart3,
  Users,
  Sparkles,
  FilePenLine,
  Shield,
  Settings,
  LogOut,
  User,
  Menu,
} from 'lucide-react';
import Logo from './Logo';
import NotificationBell from './NotificationBell';
import ThemeToggle from './ThemeToggle';
import SimulatedMoneyFooter from './SimulatedMoneyFooter';
import MobileMoreNav from './MobileMoreNav';
import { useAuth } from '../context/AuthContext';

const moneyNav = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/send', label: 'Send', icon: Send },
  { to: '/collect', label: 'Collect', icon: QrCode },
  { to: '/wallet', label: 'Wallet', icon: Wallet },
  { to: '/transactions', label: 'History', icon: List },
];

const planNav = [
  { to: '/budgets', label: 'Budgets', icon: Target },
  { to: '/analytics', label: 'Analytics', icon: BarChart3 },
];

const socialNav = [{ to: '/groups', label: 'Groups', icon: Users }];

const helpNav = [
  { to: '/insights/chat', label: 'Assistant', icon: Sparkles },
  { to: '/payment-drafts', label: 'Drafts', icon: FilePenLine },
  { to: '/security', label: 'FraudWatch', icon: Shield },
];

const desktopSections = [
  { label: 'Money', items: moneyNav },
  { label: 'Plan', items: planNav },
  { label: 'Social', items: socialNav },
  { label: 'Help', items: helpNav },
];

const mobilePrimaryNav = [
  { to: '/dashboard', label: 'Home', icon: LayoutDashboard },
  { to: '/send', label: 'Send', icon: Send },
  { to: '/collect', label: 'Collect', icon: QrCode },
  { to: '/transactions', label: 'History', icon: List },
];

const mobileMoreSections = [
  { label: 'Money', items: [{ to: '/wallet', label: 'Wallet', icon: Wallet }] },
  { label: 'Plan', items: planNav },
  { label: 'Social', items: socialNav },
  { label: 'Help', items: helpNav },
];

const adminNavItem = { to: '/admin', label: 'Admin', icon: Settings };
const profileNavItem = { to: '/profile', label: 'Profile', icon: User };

function NavItem({ item }) {
  return (
    <NavLink
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
  );
}

export default function AppLayout() {
  const { user, logout, isAdmin } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [moreOpen, setMoreOpen] = useState(false);

  const moreRoutes = new Set(
    mobileMoreSections.flatMap((s) => s.items.map((i) => i.to)).concat(isAdmin ? [adminNavItem.to] : [], [
      profileNavItem.to,
    ]),
  );
  const moreActive = moreRoutes.has(location.pathname);

  return (
    <div className="flex min-h-screen bg-drip-bg lg:flex">
      <aside className="hidden w-60 flex-col border-r border-drip-border bg-drip-surface/80 p-4 lg:flex">
        <Logo />
        <nav className="mt-8 flex flex-1 flex-col gap-5 overflow-y-auto" aria-label="Main">
          {desktopSections.map((section) => (
            <div key={section.label}>
              <p className="mb-1.5 px-3 text-[10px] font-semibold uppercase tracking-wider text-drip-muted">
                {section.label}
              </p>
              <div className="flex flex-col gap-0.5">
                {section.items.map((item) => (
                  <NavItem key={item.to} item={item} />
                ))}
              </div>
            </div>
          ))}
          {isAdmin && (
            <div>
              <p className="mb-1.5 px-3 text-[10px] font-semibold uppercase tracking-wider text-drip-muted">
                Admin
              </p>
              <NavLink
                to={adminNavItem.to}
                className={({ isActive }) =>
                  `flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm ${
                    isActive
                      ? 'bg-drip-reversed/15 font-medium text-drip-reversed'
                      : 'text-drip-muted hover:bg-drip-border/25'
                  }`
                }
              >
                <adminNavItem.icon size={18} /> {adminNavItem.label}
              </NavLink>
            </div>
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
        className="fixed bottom-0 left-0 right-0 z-40 flex border-t border-drip-border bg-drip-surface/95 backdrop-blur lg:hidden"
        aria-label="Mobile"
      >
        {mobilePrimaryNav.map((item) => (
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
        <button
          type="button"
          onClick={() => setMoreOpen(true)}
          className={`flex flex-1 flex-col items-center gap-1 py-2 text-[10px] ${
            moreActive ? 'font-medium text-drip-primary' : 'text-drip-muted'
          }`}
          aria-expanded={moreOpen}
          aria-haspopup="dialog"
        >
          <Menu size={18} />
          More
        </button>
      </nav>

      <MobileMoreNav
        open={moreOpen}
        onClose={() => setMoreOpen(false)}
        sections={mobileMoreSections}
        footerItems={[profileNavItem]}
        adminItem={isAdmin ? adminNavItem : null}
      />
    </div>
  );
}
