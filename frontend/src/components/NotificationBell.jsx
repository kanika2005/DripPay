import { Bell } from 'lucide-react';
import { useNotifications } from '../context/NotificationContext';
import api from '../api/client';

export default function NotificationBell() {
  const { items, open, setOpen, unreadCount, openNotification, refresh } = useNotifications();

  async function onClickItem(n) {
    await api.patch(`/notifications/${n.id}/read`).catch(() => {});
    setOpen(false);
    openNotification(n);
    refresh();
  }

  return (
    <div className="relative">
      <button
        type="button"
        className="relative rounded-xl p-2 text-drip-muted hover:bg-white/5 hover:text-white"
        aria-label="Notifications"
        onClick={() => setOpen((v) => !v)}
      >
        <Bell size={20} />
        {unreadCount > 0 && (
          <span className="absolute right-1 top-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-drip-lime px-1 text-[10px] font-bold text-black">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>
      {open && (
        <>
          <button type="button" className="fixed inset-0 z-40" aria-label="Close" onClick={() => setOpen(false)} />
          <div className="absolute right-0 z-50 mt-2 w-80 max-h-96 overflow-y-auto rounded-2xl border border-white/10 bg-drip-bg shadow-glow">
            <p className="border-b border-white/5 px-4 py-3 text-sm font-semibold">Notifications</p>
            {items.length === 0 ? (
              <p className="p-4 text-sm text-drip-muted">No notifications yet.</p>
            ) : (
              <ul>
                {items.map((n) => (
                  <li key={n.id}>
                    <button
                      type="button"
                      className={`w-full px-4 py-3 text-left text-sm hover:bg-white/5 ${n.read ? 'opacity-70' : ''}`}
                      onClick={() => onClickItem(n)}
                    >
                      <p className="font-medium">{n.title}</p>
                      <p className="mt-1 text-xs text-drip-muted line-clamp-3">{n.body}</p>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </>
      )}
    </div>
  );
}
