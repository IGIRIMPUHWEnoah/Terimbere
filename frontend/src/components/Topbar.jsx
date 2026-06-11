import { Search, Bell } from 'lucide-react';
import { useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import './Topbar.css';

const PAGE_TITLES = {
  '/': 'Dashboard',
  '/budgets': 'My Budgets',
  '/debts': 'Debts Portal',
  '/bills': 'Bills Management',
  '/income': 'Income Plan',
  '/contacts': 'Contacts',
  '/reports': 'Reports',
  '/settings': 'Settings',
  '/help': 'Help Center'
};

const Topbar = () => {
  const { user } = useAuth();
  const location = useLocation();
  const pageTitle = PAGE_TITLES[location.pathname] || 'Terimbere';

  const profileInitial = (user?.fullName?.trim()?.[0] || user?.email?.trim()?.[0] || 'U').toUpperCase();
  const profileLabel = user?.fullName || user?.email || 'User';

  return (
    <header className="topbar">
      <div className="topbar-title">
        <h1>{pageTitle}</h1>
      </div>

      <div className="topbar-actions">
        <div className="search-bar">
          <Search size={18} className="search-icon" />
          <input type="text" placeholder="Search..." />
        </div>

        <button className="icon-btn notification-btn" type="button" aria-label="Notifications">
          <Bell size={20} />
          <span className="notification-dot"></span>
        </button>

        <div className="user-profile" title={profileLabel}>
          <div className="avatar-circle">{profileInitial}</div>
        </div>
      </div>
    </header>
  );
};

export default Topbar;
