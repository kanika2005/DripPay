import { useState } from 'react';
import { Lock, Sparkles } from 'lucide-react';
import api from '../api/client';

export default function InsightsChat() {
  const [message, setMessage] = useState('');
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(false);

  async function send(e) {
    e.preventDefault();
    if (!message.trim()) return;
    const userMsg = message.trim();
    setMessage('');
    setMessages((m) => [...m, { role: 'user', text: userMsg }]);
    setLoading(true);
    try {
      const { data } = await api.post('/insights/chat', { message: userMsg });
      setMessages((m) => [
        ...m,
        {
          role: 'assistant',
          text: data.reply,
          paymentDraftId: data.paymentDraftId,
        },
      ]);
    } catch (err) {
      const msg = err.response?.data?.message || 'Something went wrong. Try again.';
      setMessages((m) => [...m, { role: 'assistant', text: msg }]);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="mx-auto max-w-2xl">
      <div className="flex items-center gap-2">
        <Sparkles className="text-drip-accent" size={22} aria-hidden="true" />
        <h1 className="text-2xl font-bold tracking-tight">Assistant</h1>
      </div>
      <div
        className="mt-3 flex items-center gap-2 rounded-xl border border-drip-accent/30 bg-drip-accent/10 px-3 py-2 text-sm text-drip-foreground"
        role="note"
      >
        <Lock size={16} className="shrink-0 text-drip-accent" aria-hidden="true" />
        <span>
          <strong className="font-semibold text-drip-accent">Read-only insights</strong>
          {' — '}
          Ledger insights plus payment drafts (confirm with PIN on Payment drafts). Set PIN under Profile.
        </span>
      </div>
      <div className="card mt-6 flex min-h-[320px] flex-col p-4">
        <div className="flex-1 space-y-3 overflow-y-auto">
          {messages.length === 0 && (
            <p className="text-sm text-drip-muted">
              This assistant reads your wallet data (not a general chatbot). Try: &quot;Tell me about the
              app&quot;, &quot;How much did I spend on food this month?&quot;, or &quot;Compare to last
              month.&quot;
            </p>
          )}
          {messages.map((m, i) => (
            <div
              key={i}
              className={`rounded-xl px-3 py-2 text-sm ${
                m.role === 'user'
                  ? 'ml-8 border border-drip-primary/20 bg-drip-primary/10 text-drip-foreground'
                  : 'mr-8 border border-drip-border bg-drip-bg/50 text-drip-muted'
              }`}
            >
              {m.text}
              {m.paymentDraftId && (
                <p className="mt-2 text-xs text-drip-accent">
                  Draft #{m.paymentDraftId} — confirm under Payment drafts with your PIN.
                </p>
              )}
            </div>
          ))}
        </div>
        <form onSubmit={send} className="mt-4 flex gap-2">
          <input
            className="input flex-1"
            placeholder="Ask about spending…"
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            maxLength={1000}
          />
          <button type="submit" className="btn-primary" disabled={loading}>
            Send
          </button>
        </form>
      </div>
    </div>
  );
}
