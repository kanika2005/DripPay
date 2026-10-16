import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function ProtectedRoute({ children, adminOnly = false }) {
  const { isAuthenticated, loading, isAdmin } = useAuth();
  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-drip-bg">
        <div className="h-10 w-10 animate-pulse rounded-full bg-drip-lime/30" />
      </div>
    );
  }
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (adminOnly && !isAdmin) return <Navigate to="/dashboard" replace />;
  return children;
}
