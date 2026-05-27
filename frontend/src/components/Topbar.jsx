import React from 'react';
import { Search, Bell } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import './Topbar.css';

const Topbar = () => {
  const { user } = useAuth();
  
  // Use fullName if available, otherwise email prefix, otherwise 'U'
  const displayName = user?.fullName || user?.email?.split('@')[0] || 'User';

  return (
    <header className="topbar">
      <div className="topbar-title">
        <h1>Dashboard</h1>
      </div>
      
      <div className="topbar-actions">
        <div className="search-bar">
          <Search size={18} className="search-icon" />
          <input type="text" placeholder="Search..." />
        </div>
        
        <button className="icon-btn notification-btn">
          <Bell size={20} />
          <span className="notification-dot"></span>
        </button>
        
        <div className="user-profile">
          <img src={`https://ui-avatars.com/api/?name=${encodeURIComponent(displayName)}&background=1a1a1a&color=fff`} alt={`${displayName} Profile`} title={user?.email} />
        </div>
      </div>
    </header>
  );
};

export default Topbar;
