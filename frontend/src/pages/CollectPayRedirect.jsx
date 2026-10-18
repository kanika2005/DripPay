import { Navigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { parseCollectPayload } from '../utils/collectPayload';

/** Landing route for QR codes: /collect/pay?handle=demo@drippay */
export default function CollectPayRedirect() {
  const { isAuthenticated, loading } = useAuth();
  const [params] = useSearchParams();
  const raw = params.get('handle') || params.get('h') || '';
  const handle = parseCollectPayload(raw) || parseCollectPayload(`drippay://collect?handle=${raw}`);

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-drip-bg">
        <div className="h-10 w-10 animate-pulse rounded-full bg-drip-primary/30" />
      </div>
    );
  }

  if (!handle) {
    return <Navigate to="/send" replace />;
  }

  const payPath = `/send?handle=${encodeURIComponent(handle)}`;

  if (!isAuthenticated) {
    return <Navigate to={`/login?redirect=${encodeURIComponent(payPath)}`} replace />;
  }

  return <Navigate to={payPath} replace />;
}
