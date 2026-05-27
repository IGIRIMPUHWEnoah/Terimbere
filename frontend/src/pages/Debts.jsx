import React, { useState, useEffect } from 'react';
import { ArrowUpRight, ArrowDownLeft, AlertCircle, Plus } from 'lucide-react';
import Card from '../components/ui/Card';
import { debtService } from '../services/api';
import './Debts.css';

const Debts = () => {
  const [activeTab, setActiveTab] = useState('DEBTORS');
  const [debtors, setDebtors] = useState([]);
  const [creditors, setCreditors] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  // Mocking metrics for now since API might not have them calculated exactly this way yet,
  // but ideally these come from the backend's /remaining-sum and /overdue
  const [metrics, setMetrics] = useState({ receivables: 0, payables: 0, overdue: 0 });

  useEffect(() => {
    const fetchDebts = async () => {
      try {
        setIsLoading(true);
        // Fetch all debt records
        const response = await debtService.getDebtRecords();
        const records = response.data;
        
        // Split them by direction
        const theyOweMe = records.filter(r => r.debtDirection === 'THEY_OWE_ME');
        const iOweThem = records.filter(r => r.debtDirection === 'I_OWE_THEM');
        
        setDebtors(theyOweMe);
        setCreditors(iOweThem);
        
        // Fetch metrics
        const resSum = await debtService.getRemainingSum('THEY_OWE_ME');
        const paySum = await debtService.getRemainingSum('I_OWE_THEM');
        const overdueReq = await debtService.getOverdue();
        
        // Safely set metrics
        setMetrics({
          receivables: resSum.data || 0,
          payables: paySum.data || 0,
          overdue: overdueReq.data?.length > 0 ? overdueReq.data.reduce((acc, curr) => acc + curr.remainingAmount, 0) : 0
        });

      } catch (error) {
        console.error("Failed to load debts", error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchDebts();
  }, []);

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-RW', { style: 'currency', currency: 'RWF' }).format(amount);
  };

  return (
    <div className="debts-portal">
      <div className="portal-header">
        <div>
          <h2 className="portal-title">Debts Portal</h2>
          <p className="portal-subtitle">Manage who owes you and who you owe.</p>
        </div>
        <div className="header-actions">
          <button className="btn-secondary">Add Contact</button>
          <button className="btn-primary"><Plus size={18} /> Record Debt</button>
        </div>
      </div>

      <div className="stats-grid">
        <Card dark>
          <div className="card-title">Money Owed to Me</div>
          <div className="card-value">{formatCurrency(metrics.receivables)}</div>
          <div className="card-trend">
            <span className="trend-positive"><ArrowDownLeft size={14} /> Receivables</span>
          </div>
        </Card>
        <Card>
          <div className="card-title">Money I Owe</div>
          <div className="card-value">{formatCurrency(metrics.payables)}</div>
          <div className="card-trend">
            <span className="trend-negative"><ArrowUpRight size={14} /> Payables</span>
          </div>
        </Card>
        <Card className="warning-card">
          <div className="card-title">Overdue Amount</div>
          <div className="card-value">{formatCurrency(metrics.overdue)}</div>
          <div className="card-trend">
            <span className="trend-negative"><AlertCircle size={14} /> Requires action</span>
          </div>
        </Card>
      </div>

      <Card className="debts-main-card">
        <div className="tabs-container">
          <button 
            className={`tab-btn ${activeTab === 'DEBTORS' ? 'active' : ''}`}
            onClick={() => setActiveTab('DEBTORS')}
          >
            Debtors (Money I will receive)
          </button>
          <button 
            className={`tab-btn ${activeTab === 'CREDITORS' ? 'active' : ''}`}
            onClick={() => setActiveTab('CREDITORS')}
          >
            Creditors (Money I have to pay)
          </button>
        </div>

        <div className="table-wrapper">
          {isLoading ? (
            <div className="empty-state">Loading records...</div>
          ) : (
            <table className="data-table debts-table">
              <thead>
                <tr>
                  <th>Contact Name</th>
                  <th>Remaining Amount</th>
                  <th>Due Date</th>
                  <th>Status</th>
                  <th className="text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {activeTab === 'DEBTORS' ? (
                  debtors.map(debt => (
                    <tr key={debt.id}>
                      <td><span className="contact-name">{debt.contact?.fullName || 'Unknown'}</span></td>
                      <td className="amount positive">+ {formatCurrency(debt.remainingAmount)}</td>
                      <td>{debt.dueDate}</td>
                      <td><span className={`status-badge ${debt.status.toLowerCase()}`}>{debt.status.replace('_', ' ')}</span></td>
                      <td className="text-right">
                        <button className="btn-text">Log Payment</button>
                      </td>
                    </tr>
                  ))
                ) : (
                  creditors.map(debt => (
                    <tr key={debt.id}>
                      <td><span className="contact-name">{debt.contact?.fullName || 'Unknown'}</span></td>
                      <td className="amount negative">- {formatCurrency(debt.remainingAmount)}</td>
                      <td>{debt.dueDate}</td>
                      <td><span className={`status-badge ${debt.status.toLowerCase()}`}>{debt.status.replace('_', ' ')}</span></td>
                      <td className="text-right">
                        <button className="btn-text">Log Payment</button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          )}
          
          {!isLoading && ((activeTab === 'DEBTORS' && debtors.length === 0) || (activeTab === 'CREDITORS' && creditors.length === 0)) ? (
            <div className="empty-state">No active records found.</div>
          ) : null}
        </div>
      </Card>
    </div>
  );
};

export default Debts;
