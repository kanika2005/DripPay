import { useState } from 'react';
import api from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';

export default function Profile() {
  const { user } = useAuth();
  const { show } = useToast();
  const [fullName, setFullName] = useState(user?.fullName || '');
  const [pin, setPin] = useState('');

  async function save() {
    await api.patch('/users/me', { fullName });
    show('Profile updated');
  }

  return (
    <div className="mx-auto max-w-lg card space-y-4 p-6">
      <h1 className="text-2xl font-bold">Profile</h1>
      <div>
        <label className="text-sm text-drip-muted">Email</label>
        <p>{user?.email}</p>
      </div>
      <div>
        <label className="text-sm text-drip-muted">Wallet ID</label>
        <p className="font-mono text-drip-lime">{user?.publicWalletId}</p>
      </div>
      {user?.dripHandle && (
        <div>
          <label className="text-sm text-drip-muted">Collect handle</label>
          <p className="font-mono text-drip-primary">{user.dripHandle}</p>
        </div>
      )}
      <div>
        <label className="text-sm text-drip-muted">Full name</label>
        <input className="input mt-1" value={fullName} onChange={(e) => setFullName(e.target.value)} />
      </div>
      <button type="button" className="btn-primary" onClick={save}>Save</button>
      <hr className="border-drip-border" />
      <h2 className="font-semibold">Transfer PIN</h2>
      <p className="text-sm text-drip-muted">
        {user?.hasTransferPin ? 'PIN is set. Enter a new 4–6 digit PIN to replace it.' : 'Required to confirm payment drafts.'}
      </p>
      <input
        className="input"
        type="password"
        inputMode="numeric"
        maxLength={6}
        value={pin}
        onChange={(e) => setPin(e.target.value)}
        placeholder="4–6 digits"
      />
      <button
        type="button"
        className="btn-ghost"
        onClick={async () => {
          await api.put('/users/me/transfer-pin', { pin });
          show('Transfer PIN saved');
          setPin('');
        }}
      >
        Save PIN
      </button>
    </div>
  );
}
