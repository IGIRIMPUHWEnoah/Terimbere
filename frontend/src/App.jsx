import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Dashboard from './pages/Dashboard';
import Login from './pages/Login';
import Register from './pages/Register';
import Sidebar from './components/Sidebar';
import Topbar from './components/Topbar';
import Debts from './pages/Debts';
import Placeholder from './pages/Placeholder';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import './App.css';

// A simple layout wrapper for authenticated pages
const MainLayout = ({ children }) => {
  return (
    <div className="app-container">
      <Sidebar />
      <div className="main-content">
        <Topbar />
        <div className="page-content">
          {children}
        </div>
      </div>
    </div>
  );
};

const ProtectedRoute = ({ children }) => {
  const { user } = useAuth();
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  return children;
};

function App() {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          
          {/* Protected Routes */}
          <Route path="/" element={<ProtectedRoute><MainLayout><Dashboard /></MainLayout></ProtectedRoute>} />
          <Route path="/budgets" element={<ProtectedRoute><MainLayout><Placeholder title="Budgets" description="Create and manage your specific budgets here." /></MainLayout></ProtectedRoute>} />
          <Route path="/debts" element={<ProtectedRoute><MainLayout><Debts /></MainLayout></ProtectedRoute>} />
          <Route path="/bills" element={<ProtectedRoute><MainLayout><Placeholder title="Bills Management" description="Keep track of your recurring utility and service bills." /></MainLayout></ProtectedRoute>} />
          <Route path="/income" element={<ProtectedRoute><MainLayout><Placeholder title="Income Plan" description="Strategize and monitor your income sources." /></MainLayout></ProtectedRoute>} />
          <Route path="/contacts" element={<ProtectedRoute><MainLayout><Placeholder title="Contacts & Users" description="Manage your debtors, creditors, and system users." /></MainLayout></ProtectedRoute>} />
          <Route path="/reports" element={<ProtectedRoute><MainLayout><Placeholder title="Reports & Exports" description="Generate and download Excel and PDF financial reports." /></MainLayout></ProtectedRoute>} />
          
          <Route path="/settings" element={<ProtectedRoute><MainLayout><Placeholder title="Settings" description="Configure your application preferences." /></MainLayout></ProtectedRoute>} />
          <Route path="/help" element={<ProtectedRoute><MainLayout><Placeholder title="Help Center" description="Get assistance and learn how to use Terimbere." /></MainLayout></ProtectedRoute>} />
          
          {/* Catch all */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;
