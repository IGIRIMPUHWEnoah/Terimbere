import { useState, useEffect, useCallback } from 'react';
import { Plus, X, Calendar, DollarSign, AlertCircle, RefreshCw } from 'lucide-react';
import Card from '../components/ui/Card';
import { billService } from '../services/api';
import { formatCurrency, numberValue } from '../utils/budgetHelpers';
import './Bills.css';

const emptyBillForm = () => ({
  provider: '',
  description: '',
  amount: '',
  dueDate: '',
  isRecurring: false,
  recurringCycle: 'MONTHLY'
});

const emptyPaymentForm = () => ({
  amountPaid: '',
  paymentMethod: 'Mobile Money',
  notes: ''
});

const Bills = () => {
  const [bills, setBills] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState('');
  const [metrics, setMetrics] = useState({ upcoming: 0, paid: 0, overdue: 0 });

  const [showBillModal, setShowBillModal] = useState(false);
  const [showPaymentModal, setShowPaymentModal] = useState(false);
  const [billForm, setBillForm] = useState(emptyBillForm());
  const [paymentForm, setPaymentForm] = useState(emptyPaymentForm());
  const [selectedBill, setSelectedBill] = useState(null);

  const loadBillsData = useCallback(async () => {
    try {
      setIsLoading(true);
      setError('');
      const res = await billService.getBills();
      const records = Array.isArray(res.data) ? res.data : [];
      setBills(records);

      let upcoming = 0;
      let paid = 0;
      let overdue = 0;

      records.forEach(bill => {
        const amt = numberValue(bill.amount);
        if (bill.status === 'PAID') paid += amt;
        else if (bill.status === 'OVERDUE') overdue += amt;
        else upcoming += amt; // PENDING
      });

      setMetrics({ upcoming, paid, overdue });
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load bills.');
      setBills([]);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadBillsData();
  }, [loadBillsData]);

  const handleCreateBill = async (event) => {
    event.preventDefault();
    try {
      setIsSaving(true);
      setError('');
      await billService.createBill({
        ...billForm,
        amount: numberValue(billForm.amount)
      });
      setShowBillModal(false);
      setBillForm(emptyBillForm());
      await loadBillsData();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to schedule bill.');
    } finally {
      setIsSaving(false);
    }
  };

  const openPaymentModal = (bill) => {
    setSelectedBill(bill);
    setPaymentForm({ ...emptyPaymentForm(), amountPaid: bill.amount });
    setShowPaymentModal(true);
  };

  const handleRecordPayment = async (event) => {
    event.preventDefault();
    if (!selectedBill || !paymentForm.amountPaid) return;

    try {
      setIsSaving(true);
      setError('');
      await billService.payBill(selectedBill.id, {
        amountPaid: numberValue(paymentForm.amountPaid),
        paymentMethod: paymentForm.paymentMethod,
        notes: paymentForm.notes
      });
      setShowPaymentModal(false);
      setSelectedBill(null);
      setPaymentForm(emptyPaymentForm());
      await loadBillsData();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to record bill payment.');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="bills-portal">
      <div className="portal-header">
        <div>
          <h2 className="portal-title">Bills Portal</h2>
          <p className="portal-subtitle">Manage your utilities, subscriptions, and recurring payments.</p>
        </div>
        <div className="header-actions">
          <button type="button" className="btn-primary" onClick={() => setShowBillModal(true)}>
            <Plus size={18} /> Schedule Bill
          </button>
        </div>
      </div>

      {error && <div className="portal-error">{error}</div>}

      <div className="stats-grid">
        <Card dark>
          <div className="card-title">Upcoming / Pending</div>
          <div className="card-value">{formatCurrency(metrics.upcoming)}</div>
          <div className="card-trend">
            <span className="trend-positive"><Calendar size={14} /> Scheduled</span>
          </div>
        </Card>
        <Card>
          <div className="card-title">Paid (This Month)</div>
          <div className="card-value">{formatCurrency(metrics.paid)}</div>
          <div className="card-trend">
            <span className="trend-positive"><DollarSign size={14} /> Settled</span>
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

      <Card className="bills-main-card">
        <div className="table-wrapper">
          {isLoading ? (
            <div className="empty-state">Loading bills...</div>
          ) : bills.length === 0 ? (
            <div className="empty-state">No bills scheduled yet. Click 'Schedule Bill' to start tracking.</div>
          ) : (
            <table className="data-table bills-table">
              <thead>
                <tr>
                  <th>Provider / Service</th>
                  <th>Amount</th>
                  <th>Due Date</th>
                  <th>Status</th>
                  <th className="text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {bills.map((bill) => {
                  const statusKey = (bill.status || 'PENDING').toLowerCase();
                  return (
                    <tr key={bill.id}>
                      <td>
                        <span className="provider-name">{bill.provider}</span>
                        {bill.isRecurring && (
                          <span className="recurring-badge" title={`Recurs ${bill.recurringCycle}`}>
                            <RefreshCw size={10} style={{marginRight: '2px'}}/> {bill.recurringCycle}
                          </span>
                        )}
                        <div style={{ fontSize: '0.8rem', color: 'var(--text-tertiary)' }}>{bill.description}</div>
                      </td>
                      <td className="amount negative">
                        - {formatCurrency(bill.amount)}
                      </td>
                      <td>{bill.dueDate}</td>
                      <td>
                        <span className={`status-badge ${statusKey}`}>
                          {bill.status || 'PENDING'}
                        </span>
                      </td>
                      <td className="text-right">
                        {bill.status !== 'PAID' && (
                          <button type="button" className="btn-text" onClick={() => openPaymentModal(bill)}>
                            Pay Now
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>
      </Card>

      {showBillModal && (
        <div className="modal-overlay">
          <div className="modal-card">
            <div className="modal-header">
              <h3>Schedule a New Bill</h3>
              <button type="button" className="icon-close" onClick={() => setShowBillModal(false)}><X size={18} /></button>
            </div>
            <form onSubmit={handleCreateBill} className="modal-form">
              <label>Provider / Service Name
                <input value={billForm.provider} onChange={(e) => setBillForm({ ...billForm, provider: e.target.value })} placeholder="e.g., Electricity, Netflix" required />
              </label>
              <label>Description (Optional)
                <input value={billForm.description} onChange={(e) => setBillForm({ ...billForm, description: e.target.value })} />
              </label>
              <label>Amount
                <input type="number" min="1" value={billForm.amount} onChange={(e) => setBillForm({ ...billForm, amount: e.target.value })} required />
              </label>
              <label>Due Date
                <input type="date" value={billForm.dueDate} onChange={(e) => setBillForm({ ...billForm, dueDate: e.target.value })} required />
              </label>
              <label style={{ flexDirection: 'row', alignItems: 'center', gap: '0.5rem', marginTop: '0.5rem' }}>
                <input type="checkbox" checked={billForm.isRecurring} onChange={(e) => setBillForm({ ...billForm, isRecurring: e.target.checked })} style={{ width: 'auto' }} />
                Is this a recurring bill?
              </label>
              {billForm.isRecurring && (
                <label>Recurring Cycle
                  <select value={billForm.recurringCycle} onChange={(e) => setBillForm({ ...billForm, recurringCycle: e.target.value })}>
                    <option value="WEEKLY">Weekly</option>
                    <option value="MONTHLY">Monthly</option>
                    <option value="YEARLY">Yearly</option>
                  </select>
                </label>
              )}
              <button type="submit" className="btn-primary" disabled={isSaving} style={{ marginTop: '0.5rem' }}>
                {isSaving ? 'Saving...' : 'Save Bill'}
              </button>
            </form>
          </div>
        </div>
      )}

      {showPaymentModal && selectedBill && (
        <div className="modal-overlay">
          <div className="modal-card">
            <div className="modal-header">
              <h3>Pay Bill - {selectedBill.provider}</h3>
              <button type="button" className="icon-close" onClick={() => setShowPaymentModal(false)}><X size={18} /></button>
            </div>
            <form onSubmit={handleRecordPayment} className="modal-form">
              <p className="modal-subtext">Due Amount: {formatCurrency(selectedBill.amount)}</p>
              <label>Amount Paid
                <input type="number" min="1" value={paymentForm.amountPaid} onChange={(e) => setPaymentForm({ ...paymentForm, amountPaid: e.target.value })} required />
              </label>
              <label>Payment Method
                <select value={paymentForm.paymentMethod} onChange={(e) => setPaymentForm({ ...paymentForm, paymentMethod: e.target.value })}>
                  <option value="Mobile Money">Mobile Money</option>
                  <option value="Cash">Cash</option>
                  <option value="Bank Transfer">Bank Transfer</option>
                  <option value="Card">Card</option>
                </select>
              </label>
              <label>Notes
                <textarea rows={2} value={paymentForm.notes} onChange={(e) => setPaymentForm({ ...paymentForm, notes: e.target.value })} />
              </label>
              <button type="submit" className="btn-primary" disabled={isSaving} style={{ marginTop: '0.5rem' }}>
                {isSaving ? 'Processing...' : 'Confirm Payment'}
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Bills;
