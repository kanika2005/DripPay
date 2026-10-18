import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/client';
import { useAuth } from './AuthContext';

const NotificationContext = createContext(null);

export function NotificationProvider({ children }) {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [items, setItems] = useState([]);
  const [open, setOpen] = useState(false);
  const seenIdsRef = useRef(new Set());

  const refresh = useCallback(async () => {
    if (!isAuthenticated) return;
    const { data } = await api.get('/notifications');
    setItems(data);
    if (typeof window !== 'undefined' && 'Notification' in window && Notification.permission === 'granted') {
      for (const n of data) {
        if (seenIdsRef.current.has(n.id)) continue;
        if (!n.read) {
          seenIdsRef.current.add(n.id);
          const desktop = new Notification(n.title, {
            body: n.body,
            tag: `drippay-${n.id}`,
          });
          desktop.onclick = () => {
            window.focus();
            openNotification(n, navigate);
          };
        }
      }
    }
  }, [isAuthenticated, navigate]);

  useEffect(() => {
    if (!isAuthenticated) {
      setItems([]);
      return undefined;
    }
    if (typeof window !== 'undefined' && 'Notification' in window && Notification.permission === 'default') {
      Notification.requestPermission().catch(() => {});
    }
    refresh();
    const t = setInterval(refresh, 15000);
    return () => clearInterval(t);
  }, [isAuthenticated, refresh]);

  const value = useMemo(
    () => ({
      items,
      open,
      setOpen,
      refresh,
      unreadCount: items.filter((n) => !n.read).length,
      openNotification: (n) => openNotification(n, navigate),
    }),
    [items, open, refresh, navigate],
  );

  return <NotificationContext.Provider value={value}>{children}</NotificationContext.Provider>;
}

function openNotification(n, navigate) {
  if (n.groupId && n.groupExpenseId) {
    navigate(`/groups?group=${n.groupId}&expense=${n.groupExpenseId}`);
  } else if (n.groupId) {
    navigate(`/groups?group=${n.groupId}`);
  } else {
    navigate('/groups');
  }
}

export function useNotifications() {
  return useContext(NotificationContext);
}
