import { useState, useEffect, useCallback } from 'react';
import { Plus, X, Trash2, Edit2, CheckCircle, Circle, Target, TrendingUp } from 'lucide-react';
import Card from '../components/ui/Card';
import { incomePlanService } from '../services/api';
import { formatCurrency, numberValue } from '../utils/budgetHelpers';
import './IncomePlanPortal.css';

const emptyPlanForm = () => ({
  title: '',
  description: '',
  targetAmount: '',
  startDate: '',
  endDate: ''
});

const emptySourceForm = () => ({
  sourceName: '',
  expectedAmount: '',
  sourceType: 'ACTIVE'
});

const IncomePlanPortal = () => {
  const [plans, setPlans] = useState([]);
  const [activePlan, setActivePlan] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState('');
  
  const [metrics, setMetrics] = useState({ totalTarget: 0, totalActual: 0 });

  const [showPlanModal, setShowPlanModal] = useState(false);
  const [showSourceModal, setShowSourceModal] = useState(false);
  const [planForm, setPlanForm] = useState(emptyPlanForm());
  const [sourceForm, setSourceForm] = useState(emptySourceForm());

  const loadPlans = useCallback(async () => {
    try {
      setIsLoading(true);
      setError('');
      const res = await incomePlanService.getIncomePlans();
      const records = Array.isArray(res.data) ? res.data : [];
      setPlans(records);

      let target = 0;
      let actual = 0;

      records.forEach(p => {
        target += numberValue(p.targetAmount);
        actual += numberValue(p.actualAmount);
        
        // Update active plan reference if it exists
        if (activePlan && p.id === activePlan.id) {
          setActivePlan(p);
        }
      });

      setMetrics({ totalTarget: target, totalActual: actual });
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load income plans.');
    } finally {
      setIsLoading(false);
    }
  }, [activePlan]);

  useEffect(() => {
    loadPlans();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleCreatePlan = async (e) => {
    e.preventDefault();
    try {
      setIsSaving(true);
      setError('');
      await incomePlanService.createIncomePlan({
        ...planForm,
        targetAmount: numberValue(planForm.targetAmount)
      });
      setShowPlanModal(false);
      setPlanForm(emptyPlanForm());
      await loadPlans();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create plan.');
    } finally {
      setIsSaving(false);
    }
  };

  const handleDeletePlan = async (e, id) => {
    e.stopPropagation();
    if (!window.confirm("Are you sure you want to delete this entire plan?")) return;
    try {
      await incomePlanService.deleteIncomePlan(id);
      if (activePlan?.id === id) setActivePlan(null);
      await loadPlans();
    } catch (err) {
      setError('Failed to delete plan.');
    }
  };

  const handleAddSource = async (e) => {
    e.preventDefault();
    if (!activePlan) return;
    try {
      setIsSaving(true);
      setError('');
      await incomePlanService.addSourceToPlan(activePlan.id, {
        ...sourceForm,
        expectedAmount: numberValue(sourceForm.expectedAmount)
      });
      setShowSourceModal(false);
      setSourceForm(emptySourceForm());
      await loadPlans();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to add source.');
    } finally {
      setIsSaving(false);
    }
  };

  const toggleSourceStatus = async (source) => {
    try {
      await incomePlanService.updateIncomeSource(activePlan.id, source.id, {
        sourceName: source.sourceName,
        expectedAmount: source.expectedAmount,
        sourceType: source.sourceType,
        isReceived: !source.isReceived
      });
      await loadPlans();
    } catch (err) {
      setError('Failed to update source status.');
    }
  };

  const deleteSource = async (sourceId) => {
    if (!window.confirm("Remove this source?")) return;
    try {
      await incomePlanService.deleteIncomeSource(activePlan.id, sourceId);
      await loadPlans();
    } catch (err) {
      setError('Failed to delete source.');
    }
  };

  return (
    <div className="income-portal">
      <div className="portal-header">
        <div>
          <h2 className="portal-title">Income Plan</h2>
          <p className="portal-subtitle">Strategize and monitor your income sources.</p>
        </div>
        <div className="header-actions">
          <button type="button" className="btn-primary" onClick={() => setShowPlanModal(true)}>
            <Plus size={18} /> New Plan
          </button>
        </div>
      </div>

      {error && <div className="portal-error">{error}</div>}

      <div className="stats-grid" style={{ gridTemplateColumns: '1fr 1fr', marginBottom: '1rem' }}>
        <Card dark>
          <div className="card-title">Total Target Income</div>
          <div className="card-value">{formatCurrency(metrics.totalTarget)}</div>
          <div className="card-trend">
            <span className="trend-positive"><Target size={14} /> Across all active plans</span>
          </div>
        </Card>
        <Card>
          <div className="card-title">Total Actually Received</div>
          <div className="card-value">{formatCurrency(metrics.totalActual)}</div>
          <div className="card-trend">
            <span className="trend-positive"><TrendingUp size={14} /> {(metrics.totalTarget > 0 ? (metrics.totalActual / metrics.totalTarget * 100).toFixed(1) : 0)}% Achieved</span>
          </div>
        </Card>
      </div>

      {isLoading && <div className="empty-state">Loading your strategy...</div>}
      
      {!isLoading && plans.length === 0 && (
        <div className="empty-state">No income plans found. Click 'New Plan' to strategize your earnings.</div>
      )}

      <div className="plans-grid">
        {plans.map(plan => {
          const target = numberValue(plan.targetAmount);
          const actual = numberValue(plan.actualAmount);
          const pct = target > 0 ? Math.min((actual / target) * 100, 100) : 0;
          
          return (
            <Card 
              key={plan.id} 
              className={`plan-card ${activePlan?.id === plan.id ? 'active-plan' : ''}`}
              onClick={() => setActivePlan(plan)}
            >
              <div className="plan-header">
                <h3>{plan.title}</h3>
                <button type="button" className="btn-text" onClick={(e) => handleDeletePlan(e, plan.id)} style={{color: 'var(--danger-color)'}}>
                  <Trash2 size={16} />
                </button>
              </div>
              <p style={{fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '1rem'}}>{plan.description}</p>
              
              <div className="plan-amount">{formatCurrency(target)}</div>
              
              <div className="plan-progress-container">
                <div className="progress-labels">
                  <span>Actual: {formatCurrency(actual)}</span>
                  <span>{pct.toFixed(0)}%</span>
                </div>
                <div className="progress-bar-bg">
                  <div className="progress-bar-fill" style={{ width: `${pct}%` }}></div>
                </div>
              </div>
            </Card>
          );
        })}
      </div>

      {activePlan && (
        <Card className="plan-detail-card">
          <div className="detail-header">
            <div>
              <h3 style={{margin: 0}}>{activePlan.title} - Sources</h3>
              <p style={{margin: 0, fontSize: '0.85rem', color: 'var(--text-secondary)'}}>Manage the pipelines for this plan.</p>
            </div>
            <button type="button" className="btn-secondary" onClick={() => setShowSourceModal(true)}>
              <Plus size={16} /> Add Source
            </button>
          </div>
          
          <div className="table-wrapper">
            {(!activePlan.sources || activePlan.sources.length === 0) ? (
              <div className="empty-row" style={{padding: '2rem'}}>No sources added to this plan yet.</div>
            ) : (
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Pipeline Name</th>
                    <th>Type</th>
                    <th>Expected Amount</th>
                    <th>Status</th>
                    <th className="text-right">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {activePlan.sources.map(source => (
                    <tr key={source.id}>
                      <td style={{fontWeight: 500}}>{source.sourceName}</td>
                      <td>{source.sourceType}</td>
                      <td>{formatCurrency(source.expectedAmount)}</td>
                      <td>
                        <span className={`source-status ${source.isReceived ? 'received' : 'pending'}`}>
                          {source.isReceived ? 'Received' : 'Pending'}
                        </span>
                      </td>
                      <td className="text-right">
                        <button type="button" className="btn-text" style={{marginRight: '1rem'}} onClick={() => toggleSourceStatus(source)}>
                          {source.isReceived ? 'Mark Pending' : 'Mark Received'}
                        </button>
                        <button type="button" className="btn-text" style={{color: 'var(--danger-color)'}} onClick={() => deleteSource(source.id)}>
                          Remove
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </Card>
      )}

      {showPlanModal && (
        <div className="modal-overlay">
          <div className="modal-card">
            <div className="modal-header">
              <h3>Create Income Plan</h3>
              <button type="button" className="icon-close" onClick={() => setShowPlanModal(false)}><X size={18} /></button>
            </div>
            <form onSubmit={handleCreatePlan} className="modal-form">
              <label>Plan Title
                <input value={planForm.title} onChange={(e) => setPlanForm({...planForm, title: e.target.value})} placeholder="e.g. Q3 2026 Strategy" required />
              </label>
              <label>Description
                <textarea rows={2} value={planForm.description} onChange={(e) => setPlanForm({...planForm, description: e.target.value})} />
              </label>
              <label>Target Amount
                <input type="number" min="1" value={planForm.targetAmount} onChange={(e) => setPlanForm({...planForm, targetAmount: e.target.value})} required />
              </label>
              <div style={{display: 'flex', gap: '1rem'}}>
                <label style={{flex: 1}}>Start Date
                  <input type="date" value={planForm.startDate} onChange={(e) => setPlanForm({...planForm, startDate: e.target.value})} required />
                </label>
                <label style={{flex: 1}}>End Date
                  <input type="date" value={planForm.endDate} onChange={(e) => setPlanForm({...planForm, endDate: e.target.value})} required />
                </label>
              </div>
              <button type="submit" className="btn-primary" disabled={isSaving} style={{ marginTop: '0.5rem' }}>
                {isSaving ? 'Creating...' : 'Create Plan'}
              </button>
            </form>
          </div>
        </div>
      )}

      {showSourceModal && activePlan && (
        <div className="modal-overlay">
          <div className="modal-card">
            <div className="modal-header">
              <h3>Add Source to {activePlan.title}</h3>
              <button type="button" className="icon-close" onClick={() => setShowSourceModal(false)}><X size={18} /></button>
            </div>
            <form onSubmit={handleAddSource} className="modal-form">
              <label>Source Name / Pipeline
                <input value={sourceForm.sourceName} onChange={(e) => setSourceForm({...sourceForm, sourceName: e.target.value})} placeholder="e.g. June Salary, Upwork Client" required />
              </label>
              <label>Expected Amount
                <input type="number" min="1" value={sourceForm.expectedAmount} onChange={(e) => setSourceForm({...sourceForm, expectedAmount: e.target.value})} required />
              </label>
              <label>Source Type
                <select value={sourceForm.sourceType} onChange={(e) => setSourceForm({...sourceForm, sourceType: e.target.value})}>
                  <option value="ACTIVE">Active (e.g. Salary, Consulting)</option>
                  <option value="PASSIVE">Passive (e.g. Dividends, Rentals)</option>
                  <option value="WINDFALL">Windfall (e.g. Bonus, Gift)</option>
                </select>
              </label>
              <button type="submit" className="btn-primary" disabled={isSaving} style={{ marginTop: '0.5rem' }}>
                {isSaving ? 'Adding...' : 'Add Source'}
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default IncomePlanPortal;
