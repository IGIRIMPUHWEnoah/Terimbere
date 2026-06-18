import { useState, useEffect, useCallback } from 'react';
import { ArrowUpRight, ArrowDownLeft, AlertCircle, Plus, X, Clock, RefreshCw, Settings, Play } from 'lucide-react';
import Card from '../components/ui/Card';
import { debtService } from '../services/api';
import { formatCurrency, numberValue } from '../utils/budgetHelpers';
import './Debts.css';

const emptyContactForm = () => ({
  fullName: '',
  phone: '',
  email: '',
  address: '',
  contactType: 'DEBTOR',
  notes: ''
});

const emptyDebtForm = () => ({
  contactId: '',
  originalAmount: '',
  dueDate: '',
  schedulingMode: 'SINGLE',
  frequency: 'MONTHLY',
  numberOfInstallments: ''
});

const emptyPaymentForm = () => ({
  amountPaid: '',
  paymentMethod: 'Mobile Money',
  notes: ''
});

const Debts = () => {
  const [activeTab, setActiveTab] = useState('DEBTORS');
  const [debtorsPage, setDebtorsPage] = useState({ content: [], totalPages: 0, number: 0 });
  const [creditorsPage, setCreditorsPage] = useState({ content: [], totalPages: 0, number: 0 });
  const [page, setPage] = useState(0);
  const [contacts, setContacts] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState('');
  const [metrics, setMetrics] = useState({ receivables: 0, payables: 0, overdue: 0 });

  const [showContactModal, setShowContactModal] = useState(false);
  const [showDebtModal, setShowDebtModal] = useState(false);
  const [showPaymentModal, setShowPaymentModal] = useState(false);
  const [contactForm, setContactForm] = useState(emptyContactForm());
  const [debtForm, setDebtForm] = useState(emptyDebtForm());
  const [paymentForm, setPaymentForm] = useState(emptyPaymentForm());
  const [selectedDebt, setSelectedDebt] = useState(null);

  // Scheduler state
  const [schedulerConfig, setSchedulerConfig] = useState({ hour: 0, minute: 0, displayTime: '00:00', cronExpression: '' });
  const [schedulerTime, setSchedulerTime] = useState('00:00');
  const [isSchedulerSaving, setIsSchedulerSaving] = useState(false);
  const [isRunningNow, setIsRunningNow] = useState(false);
  const [schedulerMessage, setSchedulerMessage] = useState('');
  const [showSchedulerSettings, setShowSchedulerSettings] = useState(false);

  const loadDebtsData = useCallback(async (currentPage = page) => {
    try {
      setIsLoading(true);
      setError('');
      const direction = activeTab === 'DEBTORS' ? 'THEY_OWE_ME' : 'I_OWE_THEM';
      const [recordsRes, contactsRes, receivablesRes, payablesRes, overdueRes] = await Promise.all([
        debtService.getDebtsByDirection(direction, currentPage, 10, 'createdAt,desc'),
        debtService.getContacts(),
        debtService.getRemainingSum('THEY_OWE_ME'),
        debtService.getRemainingSum('I_OWE_THEM'),
        debtService.getOverdue()
      ]);

      if (activeTab === 'DEBTORS') {
        setDebtorsPage(recordsRes.data);
      } else {
        setCreditorsPage(recordsRes.data);
      }
      setContacts(Array.isArray(contactsRes.data) ? contactsRes.data : []);

      const overdueList = Array.isArray(overdueRes.data) ? overdueRes.data : [];
      setMetrics({
        receivables: numberValue(receivablesRes.data),
        payables: numberValue(payablesRes.data),
        overdue: overdueList.reduce((sum, record) => sum + numberValue(record.remainingAmount), 0)
      });
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load debts portal data.');
    } finally {
      setIsLoading(false);
    }
  }, [activeTab, page]);

  useEffect(() => {
    let active = true;

    const fetchInitialDebts = async () => {
      try {
        setIsLoading(true);
        setError('');
        const direction = activeTab === 'DEBTORS' ? 'THEY_OWE_ME' : 'I_OWE_THEM';
        const [recordsRes, contactsRes, receivablesRes, payablesRes, overdueRes] = await Promise.all([
          debtService.getDebtsByDirection(direction, page, 10, 'createdAt,desc'),
          debtService.getContacts(),
          debtService.getRemainingSum('THEY_OWE_ME'),
          debtService.getRemainingSum('I_OWE_THEM'),
          debtService.getOverdue()
        ]);

        if (!active) return;

        if (activeTab === 'DEBTORS') {
          setDebtorsPage(recordsRes.data);
        } else {
          setCreditorsPage(recordsRes.data);
        }
        setContacts(Array.isArray(contactsRes.data) ? contactsRes.data : []);

        const overdueList = Array.isArray(overdueRes.data) ? overdueRes.data : [];
        setMetrics({
          receivables: numberValue(receivablesRes.data),
          payables: numberValue(payablesRes.data),
          overdue: overdueList.reduce((sum, record) => sum + numberValue(record.remainingAmount), 0)
        });
      } catch (err) {
        if (!active) return;
        setError(err.response?.data?.message || 'Failed to load debts portal data.');
      } finally {
        if (active) setIsLoading(false);
      }
    };

    fetchInitialDebts();
    return () => {
      active = false;
    };
  }, [activeTab, page]);

  // Load scheduler config on mount
  useEffect(() => {
    const loadSchedulerConfig = async () => {
      try {
        const res = await debtService.getSchedulerConfig();
        setSchedulerConfig(res.data);
        setSchedulerTime(res.data.displayTime || '00:00');
      } catch (err) {
        // Silently fail — defaults to midnight
      }
    };
    loadSchedulerConfig();
  }, []);

  const handleSaveScheduler = async () => {
    const [h, m] = schedulerTime.split(':').map(Number);
    if (isNaN(h) || isNaN(m) || h < 0 || h > 23 || m < 0 || m > 59) {
      setSchedulerMessage('Invalid time. Use HH:MM format (00-23 : 00-59).');
      return;
    }
    try {
      setIsSchedulerSaving(true);
      setSchedulerMessage('');
      const res = await debtService.updateSchedulerConfig({ hour: h, minute: m });
      setSchedulerConfig(res.data);
      setSchedulerMessage(`Scheduler updated! Overdue check will run daily at ${res.data.displayTime}.`);
    } catch (err) {
      setSchedulerMessage(err.response?.data?.message || 'Failed to update scheduler.');
    } finally {
      setIsSchedulerSaving(false);
    }
  };

  const handleRunNow = async () => {
    try {
      setIsRunningNow(true);
      setSchedulerMessage('');
      await debtService.runSchedulerNow();
      setSchedulerMessage('Overdue check completed successfully!');
      await loadDebtsData();
    } catch (err) {
      setSchedulerMessage(err.response?.data?.message || 'Failed to run scheduler.');
    } finally {
      setIsRunningNow(false);
    }
  };

  const openDebtModal = () => {
    setDebtForm({
      ...emptyDebtForm(),
      contactId: contacts[0]?.id || ''
    });
    setShowDebtModal(true);
  };

  const handleCreateContact = async (event) => {
    event.preventDefault();
    try {
      setIsSaving(true);
      setError('');
      await debtService.createContact(contactForm);
      setShowContactModal(false);
      setContactForm(emptyContactForm());
      await loadDebtsData();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create contact.');
    } finally {
      setIsSaving(false);
    }
  };

  const handleCreateDebt = async (event) => {
    event.preventDefault();
    if (!debtForm.contactId || !debtForm.originalAmount || !debtForm.dueDate) return;

    try {
      setIsSaving(true);
      setError('');
      await debtService.createDebtRecord({
        contactId: debtForm.contactId,
        debtDirection: activeTab === 'DEBTORS' ? 'THEY_OWE_ME' : 'I_OWE_THEM',
        originalAmount: numberValue(debtForm.originalAmount),
        dueDate: debtForm.dueDate,
        status: 'ACTIVE',
        schedulingMode: debtForm.schedulingMode,
        frequency: debtForm.schedulingMode === 'SCHEDULED' ? debtForm.frequency : null,
        numberOfInstallments: debtForm.schedulingMode === 'SCHEDULED' ? parseInt(debtForm.numberOfInstallments, 10) : null
      });
      setShowDebtModal(false);
      setDebtForm(emptyDebtForm());
      await loadDebtsData();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to record debt.');
    } finally {
      setIsSaving(false);
    }
  };

  const openPaymentModal = (debt) => {
    setSelectedDebt(debt);
    setPaymentForm(emptyPaymentForm());
    setShowPaymentModal(true);
  };

  const handleRecordPayment = async (event) => {
    event.preventDefault();
    if (!selectedDebt || !paymentForm.amountPaid) return;

    try {
      setIsSaving(true);
      setError('');
      await debtService.recordPayment(selectedDebt.id, {
        amountPaid: numberValue(paymentForm.amountPaid),
        paymentMethod: paymentForm.paymentMethod,
        notes: paymentForm.notes
      });
      setShowPaymentModal(false);
      setSelectedDebt(null);
      setPaymentForm(emptyPaymentForm());
      await loadDebtsData();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to log payment.');
    } finally {
      setIsSaving(false);
    }
  };

  const renderDebtRows = (records, positive) => {
    if (records.length === 0) {
      return (
        <tr>
          <td colSpan={5} className="empty-row">No active records found.</td>
        </tr>
      );
    }

    return records.map((debt) => {
      const statusKey = (debt.status || 'ACTIVE').toLowerCase();
      return (
        <tr key={debt.id}>
          <td><span className="contact-name">{debt.contact?.fullName || 'Unknown'}</span></td>
          <td className={`amount ${positive ? 'positive' : 'negative'}`}>
            {positive ? '+' : '-'} {formatCurrency(debt.remainingAmount)}
          </td>
          <td>{debt.dueDate}</td>
          <td>
            <span className={`status-badge ${statusKey}`}>
              {(debt.status || 'ACTIVE').replace(/_/g, ' ')}
            </span>
            {debt.schedulingMode === 'SCHEDULED' && <div style={{ fontSize: '0.75rem', marginTop: '4px', color: 'var(--text-secondary)' }}>Scheduled ({debt.frequency})</div>}
          </td>
          <td className="text-right">
            {debt.status !== 'PAID' && (
              <button type="button" className="btn-text" onClick={() => openPaymentModal(debt)}>
                Log Payment
              </button>
            )}
          </td>
        </tr>
      );
    });
  };

  return (
    <div className="debts-portal">
      <div className="portal-header">
        <div>
          <h2 className="portal-title">Debts Portal</h2>
          <p className="portal-subtitle">Manage debtors (money owed to you) and creditors (money you owe).</p>
        </div>
        <div className="header-actions">
          <button type="button" className="btn-icon-outline" title="Scheduler Settings" onClick={() => setShowSchedulerSettings(!showSchedulerSettings)}>
            <Settings size={18} />
          </button>
          <button type="button" className="btn-secondary" onClick={() => setShowContactModal(true)}>Add Contact</button>
          <button type="button" className="btn-primary" onClick={openDebtModal} disabled={contacts.length === 0}>
            <Plus size={18} /> Record Debt
          </button>
        </div>
      </div>

      {error && <div className="portal-error">{error}</div>}

      {/* Scheduler Settings Panel */}
      {showSchedulerSettings && (
        <Card className="scheduler-card">
          <div className="scheduler-header">
            <div className="scheduler-title-row">
              <Clock size={20} className="scheduler-icon" />
              <div>
                <h3 className="scheduler-title">Due Date Checker Schedule</h3>
                <p className="scheduler-subtitle">Configure when the system checks for overdue debts daily</p>
              </div>
            </div>
          </div>
          <div className="scheduler-body">
            <div className="scheduler-current">
              <span className="scheduler-label">Current Schedule</span>
              <div className="scheduler-badge">
                <Clock size={14} />
                <span>Daily at {schedulerConfig.displayTime || '00:00'}</span>
              </div>
              {schedulerConfig.cronExpression && (
                <span className="scheduler-cron">Cron: {schedulerConfig.cronExpression}</span>
              )}
            </div>
            <div className="scheduler-form">
              <label className="scheduler-input-label">
                Set Check Time (24h format)
                <input
                  type="time"
                  value={schedulerTime}
                  onChange={(e) => setSchedulerTime(e.target.value)}
                  className="scheduler-time-input"
                />
              </label>
              <div className="scheduler-actions">
                <button
                  type="button"
                  className="btn-primary"
                  onClick={handleSaveScheduler}
                  disabled={isSchedulerSaving}
                >
                  {isSchedulerSaving ? 'Saving...' : 'Save Schedule'}
                </button>
                <button
                  type="button"
                  className="btn-run-now"
                  onClick={handleRunNow}
                  disabled={isRunningNow}
                  title="Run the overdue check right now"
                >
                  {isRunningNow ? <RefreshCw size={16} className="spin" /> : <Play size={16} />}
                  {isRunningNow ? 'Running...' : 'Run Now'}
                </button>
              </div>
            </div>
            {schedulerMessage && (
              <div className={`scheduler-message ${schedulerMessage.includes('Failed') || schedulerMessage.includes('Invalid') ? 'error' : 'success'}`}>
                {schedulerMessage}
              </div>
            )}
          </div>
        </Card>
      )}

      <div className="stats-grid">
        <Card dark>
          <div className="card-title">Money Owed to Me</div>
          <div className="card-value">{formatCurrency(metrics.receivables)}</div>
          <div className="card-trend">
            <span className="trend-positive"><ArrowDownLeft size={14} /> Debtors / Receivables</span>
          </div>
        </Card>
        <Card>
          <div className="card-title">Money I Owe</div>
          <div className="card-value">{formatCurrency(metrics.payables)}</div>
          <div className="card-trend">
            <span className="trend-negative"><ArrowUpRight size={14} /> Creditors / Payables</span>
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
          <button type="button" className={`tab-btn ${activeTab === 'DEBTORS' ? 'active' : ''}`} onClick={() => setActiveTab('DEBTORS')}>
            Debtors (Money I will receive)
          </button>
          <button type="button" className={`tab-btn ${activeTab === 'CREDITORS' ? 'active' : ''}`} onClick={() => setActiveTab('CREDITORS')}>
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
                {activeTab === 'DEBTORS' ? renderDebtRows(debtorsPage.content || [], true) : renderDebtRows(creditorsPage.content || [], false)}
              </tbody>
            </table>
          )}
          {!isLoading && (
            <div className="pagination-controls" style={{ display: 'flex', justifyContent: 'center', gap: '10px', marginTop: '15px' }}>
              <button className="btn-secondary" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button>
              <span style={{ display: 'flex', alignItems: 'center' }}>Page {page + 1} of {activeTab === 'DEBTORS' ? (debtorsPage.totalPages || 1) : (creditorsPage.totalPages || 1)}</span>
              <button className="btn-secondary" disabled={page >= ((activeTab === 'DEBTORS' ? debtorsPage.totalPages : creditorsPage.totalPages) - 1) || (activeTab === 'DEBTORS' ? debtorsPage.totalPages : creditorsPage.totalPages) === 0} onClick={() => setPage(page + 1)}>Next</button>
            </div>
          )}
        </div>
      </Card>

      {showContactModal && (
        <div className="modal-overlay">
          <div className="modal-card">
            <div className="modal-header">
              <h3>Add Contact</h3>
              <button type="button" className="icon-close" onClick={() => setShowContactModal(false)}><X size={18} /></button>
            </div>
            <form onSubmit={handleCreateContact} className="modal-form">
              <label>Full Name<input value={contactForm.fullName} onChange={(e) => setContactForm({ ...contactForm, fullName: e.target.value })} required /></label>
              <label>Phone<input value={contactForm.phone} onChange={(e) => setContactForm({ ...contactForm, phone: e.target.value })} /></label>
              <label>Email<input type="email" value={contactForm.email} onChange={(e) => setContactForm({ ...contactForm, email: e.target.value })} /></label>
              <label>Contact Type
                <select value={contactForm.contactType} onChange={(e) => setContactForm({ ...contactForm, contactType: e.target.value })}>
                  <option value="DEBTOR">Debtor</option>
                  <option value="CREDITOR">Creditor</option>
                  <option value="BOTH">Both</option>
                </select>
              </label>
              <label>Notes<textarea rows={3} value={contactForm.notes} onChange={(e) => setContactForm({ ...contactForm, notes: e.target.value })} /></label>
              <button type="submit" className="btn-primary" disabled={isSaving}>{isSaving ? 'Saving...' : 'Save Contact'}</button>
            </form>
          </div>
        </div>
      )}

      {showDebtModal && (
        <div className="modal-overlay">
          <div className="modal-card">
            <div className="modal-header">
              <h3>Record Debt - {activeTab === 'DEBTORS' ? 'Debtor' : 'Creditor'}</h3>
              <button type="button" className="icon-close" onClick={() => setShowDebtModal(false)}><X size={18} /></button>
            </div>
            <form onSubmit={handleCreateDebt} className="modal-form">
              <label>Contact
                <select value={debtForm.contactId} onChange={(e) => setDebtForm({ ...debtForm, contactId: e.target.value })} required>
                  <option value="">Select contact</option>
                  {contacts.map((contact) => (
                    <option key={contact.id} value={contact.id}>{contact.fullName}</option>
                  ))}
                </select>
              </label>
              <label>Original Amount<input type="number" min="1" value={debtForm.originalAmount} onChange={(e) => setDebtForm({ ...debtForm, originalAmount: e.target.value })} required /></label>
              <label>Scheduling Mode
                <select value={debtForm.schedulingMode} onChange={(e) => setDebtForm({ ...debtForm, schedulingMode: e.target.value })}>
                  <option value="SINGLE">Single Payment</option>
                  <option value="SCHEDULED">Scheduled Installments</option>
                </select>
              </label>
              <label>{debtForm.schedulingMode === 'SCHEDULED' ? 'Start Date' : 'Due Date'}<input type="date" value={debtForm.dueDate} onChange={(e) => setDebtForm({ ...debtForm, dueDate: e.target.value })} required /></label>
              {debtForm.schedulingMode === 'SCHEDULED' && (
                <>
                  <label>Frequency
                    <select value={debtForm.frequency} onChange={(e) => setDebtForm({ ...debtForm, frequency: e.target.value })}>
                      <option value="WEEKLY">Weekly</option>
                      <option value="BI_WEEKLY">Bi-Weekly</option>
                      <option value="MONTHLY">Monthly</option>
                    </select>
                  </label>
                  <label>Number of Installments<input type="number" min="2" value={debtForm.numberOfInstallments} onChange={(e) => setDebtForm({ ...debtForm, numberOfInstallments: e.target.value })} required /></label>
                </>
              )}
              <button type="submit" className="btn-primary" disabled={isSaving}>{isSaving ? 'Saving...' : 'Save Debt Record'}</button>
            </form>
          </div>
        </div>
      )}

      {showPaymentModal && selectedDebt && (
        <div className="modal-overlay">
          <div className="modal-card">
            <div className="modal-header">
              <h3>Log Payment - {selectedDebt.contact?.fullName || 'Contact'}</h3>
              <button type="button" className="icon-close" onClick={() => setShowPaymentModal(false)}><X size={18} /></button>
            </div>
            <form onSubmit={handleRecordPayment} className="modal-form">
              <p className="modal-subtext">Remaining balance: {formatCurrency(selectedDebt.remainingAmount)}</p>
              <label>Amount Paid<input type="number" min="1" value={paymentForm.amountPaid} onChange={(e) => setPaymentForm({ ...paymentForm, amountPaid: e.target.value })} required /></label>
              <label>Payment Method
                <select value={paymentForm.paymentMethod} onChange={(e) => setPaymentForm({ ...paymentForm, paymentMethod: e.target.value })}>
                  <option value="Mobile Money">Mobile Money</option>
                  <option value="Cash">Cash</option>
                  <option value="Bank Transfer">Bank Transfer</option>
                </select>
              </label>
              <label>Notes<textarea rows={3} value={paymentForm.notes} onChange={(e) => setPaymentForm({ ...paymentForm, notes: e.target.value })} /></label>
              <button type="submit" className="btn-primary" disabled={isSaving}>{isSaving ? 'Saving...' : 'Record Payment'}</button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Debts;
