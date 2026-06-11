import { NavLink, useNavigate } from 'react-router-dom';
import { LayoutDashboard, Wallet, CreditCard, ReceiptText, TrendingUp, FileText, Settings, HelpCircle, LogOut, Users } from 'lucide-react';
import { useAuth } from '../hooks/useAuth';
import './Sidebar.css';

const Sidebar = () => {
  const { logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <aside className="sidebar">
      <div className="sidebar-header">
        <h2>Terimbere</h2>
      </div>
      
      <nav className="sidebar-nav">
        <NavLink to="/" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`} end>
          <LayoutDashboard size={20} />
          <span>Dashboard</span>
        </NavLink>
        <NavLink to="/budgets" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
          <Wallet size={20} />
          <span>Budgets</span>
        </NavLink>
        <NavLink to="/debts" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
          <CreditCard size={20} />
          <span>Debts</span>
        </NavLink>
        <NavLink to="/bills" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
          <ReceiptText size={20} />
          <span>Bills</span>
        </NavLink>
        <NavLink to="/income" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
          <TrendingUp size={20} />
          <span>Income Plan</span>
        </NavLink>
        <NavLink to="/contacts" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
          <Users size={20} />
          <span>Contacts</span>
        </NavLink>
        <NavLink to="/reports" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
          <FileText size={20} />
          <span>Reports</span>
        </NavLink>
      </nav>

      <div className="sidebar-footer">
        <nav className="sidebar-bottom-nav">
          <NavLink to="/settings" className="nav-item">
            <Settings size={20} />
            <span>Settings</span>
          </NavLink>
          <NavLink to="/help" className="nav-item">
            <HelpCircle size={20} />
            <span>Help Center</span>
          </NavLink>
          <button className="nav-item logout-btn" type="button" onClick={handleLogout}>
            <LogOut size={20} />
            <span>Log out</span>
          </button>
        </nav>
      </div>
    </aside>
  );
};

export default Sidebar;
