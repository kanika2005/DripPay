import { useEffect, useRef } from 'react';
import { NavLink } from 'react-router-dom';
import { X } from 'lucide-react';

export default function MobileMoreNav({ open, onClose, sections, footerItems, adminItem }) {
  const panelRef = useRef(null);

  useEffect(() => {
    if (!open) return undefined;
    const onKey = (e) => {
      if (e.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', onKey);
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', onKey);
      document.body.style.overflow = '';
    };
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 lg:hidden" role="dialog" aria-modal="true" aria-label="More navigation">
      <button
        type="button"
        className="absolute inset-0 bg-black/50"
        aria-label="Close menu"
        onClick={onClose}
      />
      <div
        ref={panelRef}
        className="absolute bottom-0 left-0 right-0 max-h-[85vh] overflow-y-auto rounded-t-2xl border-t border-drip-border bg-drip-surface p-4 pb-8 shadow-xl"
      >
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-sm font-semibold text-drip-foreground">More</h2>
          <button type="button" className="btn-ghost !p-2" onClick={onClose} aria-label="Close">
            <X size={20} />
          </button>
        </div>
        <div className="space-y-5">
          {sections.map((section) => (
            <div key={section.label}>
              <p className="mb-2 px-1 text-[10px] font-semibold uppercase tracking-wider text-drip-muted">
                {section.label}
              </p>
              <ul className="space-y-0.5">
                {section.items.map((item) => (
                  <li key={item.to}>
                    <NavLink
                      to={item.to}
                      onClick={onClose}
                      className={({ isActive }) =>
                        `flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm ${
                          isActive
                            ? 'bg-drip-primary/15 font-medium text-drip-primary'
                            : 'text-drip-muted hover:bg-drip-border/25 hover:text-drip-foreground'
                        }`
                      }
                    >
                      <item.icon size={18} aria-hidden="true" />
                      {item.label}
                    </NavLink>
                  </li>
                ))}
              </ul>
            </div>
          ))}
          {adminItem && (
            <div>
              <p className="mb-2 px-1 text-[10px] font-semibold uppercase tracking-wider text-drip-muted">
                Admin
              </p>
              <NavLink
                to={adminItem.to}
                onClick={onClose}
                className={({ isActive }) =>
                  `flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm ${
                    isActive
                      ? 'bg-drip-reversed/15 font-medium text-drip-reversed'
                      : 'text-drip-muted hover:bg-drip-border/25'
                  }`
                }
              >
                <adminItem.icon size={18} />
                {adminItem.label}
              </NavLink>
            </div>
          )}
        </div>
        <div className="mt-6 space-y-0.5 border-t border-drip-border pt-4">
          {footerItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              onClick={onClose}
              className="flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm text-drip-muted hover:bg-drip-border/25 hover:text-drip-foreground"
            >
              <item.icon size={18} />
              {item.label}
            </NavLink>
          ))}
        </div>
      </div>
    </div>
  );
}
