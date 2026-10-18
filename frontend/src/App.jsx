import { Navigate, Route, Routes } from 'react-router-dom';
import AppLayout from './components/AppLayout';
import ProtectedRoute from './components/ProtectedRoute';
import Landing from './pages/Landing';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import WalletPage from './pages/WalletPage';
import SendMoney from './pages/SendMoney';
import Transactions from './pages/Transactions';
import TransactionDetail from './pages/TransactionDetail';
import Analytics from './pages/Analytics';
import InsightsChat from './pages/InsightsChat';
import Budgets from './pages/Budgets';
import Groups from './pages/Groups';
import Security from './pages/Security';
import Profile from './pages/Profile';
import Admin from './pages/Admin';
import Collect from './pages/Collect';
import CollectPayRedirect from './pages/CollectPayRedirect';
import PaymentDrafts from './pages/PaymentDrafts';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Landing />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/collect/pay" element={<CollectPayRedirect />} />
      <Route
        element={
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/wallet" element={<WalletPage />} />
        <Route path="/send" element={<SendMoney />} />
        <Route path="/collect" element={<Collect />} />
        <Route path="/payment-drafts" element={<PaymentDrafts />} />
        <Route path="/transactions" element={<Transactions />} />
        <Route path="/transactions/:id" element={<TransactionDetail />} />
        <Route path="/analytics" element={<Analytics />} />
        <Route path="/insights/chat" element={<InsightsChat />} />
        <Route path="/budgets" element={<Budgets />} />
        <Route path="/groups" element={<Groups />} />
        <Route path="/security" element={<Security />} />
        <Route path="/profile" element={<Profile />} />
        <Route
          path="/admin"
          element={
            <ProtectedRoute adminOnly>
              <Admin />
            </ProtectedRoute>
          }
        />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
