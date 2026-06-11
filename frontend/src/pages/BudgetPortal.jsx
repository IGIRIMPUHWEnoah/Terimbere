import { useState, useEffect, useCallback } from 'react';
import { budgetService } from '../services/api';
import {
  formatAmount,
  mapBudgetFromApi,
  buildBudgetPayload,
  buildEntryPayload,
  calculateBudgetSummary
} from '../utils/budgetHelpers';
import './BudgetPortal.css';

const BUDGET_TYPES = [
  { value: 'PERSONAL', label: 'Personal Budget', help: 'For individual daily living expenses' },
  { value: 'BUSINESS', label: 'Business Budget', help: 'For a small business or side income tracking' },
  { value: 'PROJECT', label: 'Project Budget', help: 'For a one-time project or event' },
  { value: 'SAVINGS', label: 'Savings Budget', help: 'Focused on saving goals and tracking set-aside amounts' },
  { value: 'FAMILY', label: 'Family Budget', help: 'For household shared expenses' }
];

const PERIOD_TYPES = ['Monthly', 'Quarterly', 'Yearly', 'Custom'];

const createEmptyBudgetForm = () => ({
  name: '',
  type: 'PERSONAL',
  periodType: 'Monthly',
  startDate: '',
  endDate: '',
  description: ''
});

const createEmptyEntryForm = () => ({
  category: '',
  description: '',
  plannedAmount: '',
  actualAmount: '',
  amountSaved: '',
  date: ''
});

const BudgetPortal = () => {
  const [budgets, setBudgets] = useState([]);
  const [selectedBudget, setSelectedBudget] = useState(null);
  const [showNewBudgetForm, setShowNewBudgetForm] = useState(false);
  const [newBudgetForm, setNewBudgetForm] = useState(createEmptyBudgetForm());
  const [incomeForm, setIncomeForm] = useState(createEmptyEntryForm());
  const [expenseForm, setExpenseForm] = useState(createEmptyEntryForm());
  const [editingIncomeId, setEditingIncomeId] = useState(null);
  const [editingExpenseId, setEditingExpenseId] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState('');

  const loadBudgets = useCallback(async () => {
    try {
      setIsLoading(true);
      setError('');
      const response = await budgetService.getBudgets();
      const mapped = (response.data || []).map(mapBudgetFromApi);
      setBudgets(mapped);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load budgets.');
      setBudgets([]);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const loadBudgetById = useCallback(async (budgetId) => {
    try {
      setIsSaving(true);
      setError('');
      const response = await budgetService.getBudgetById(budgetId);
      setSelectedBudget(mapBudgetFromApi(response.data));
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load budget workspace.');
      setSelectedBudget(null);
    } finally {
      setIsSaving(false);
    }
  }, []);

  useEffect(() => {
    let active = true;

    const fetchInitialBudgets = async () => {
      try {
        setIsLoading(true);
        setError('');
        const response = await budgetService.getBudgets();
        if (!active) return;
        const mapped = (response.data || []).map(mapBudgetFromApi);
        setBudgets(mapped);
      } catch (err) {
        if (!active) return;
        setError(err.response?.data?.message || 'Failed to load budgets.');
        setBudgets([]);
      } finally {
        if (active) setIsLoading(false);
      }
    };

    fetchInitialBudgets();
    return () => {
      active = false;
    };
  }, []);

  const resetIncomeForm = () => {
    setIncomeForm(createEmptyEntryForm());
    setEditingIncomeId(null);
  };

  const resetExpenseForm = () => {
    setExpenseForm(createEmptyEntryForm());
    setEditingExpenseId(null);
  };

  const handleNewBudgetInput = (event) => {
    const { name, value } = event.target;
    setNewBudgetForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleCreateBudget = async (event) => {
    event.preventDefault();
    if (!newBudgetForm.name || !newBudgetForm.startDate || !newBudgetForm.endDate) return;

    try {
      setIsSaving(true);
      setError('');
      const payload = buildBudgetPayload({
        ...newBudgetForm,
        type: newBudgetForm.type,
        status: 'ACTIVE',
        notes: '',
        savingsGoal: 0,
        projectTotalBudget: 0
      });
      const response = await budgetService.createBudget(payload);
      const created = mapBudgetFromApi(response.data);
      setNewBudgetForm(createEmptyBudgetForm());
      setShowNewBudgetForm(false);
      await loadBudgets();
      await loadBudgetById(created.id);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create budget.');
    } finally {
      setIsSaving(false);
    }
  };

  const persistBudgetMeta = async (budget) => {
    await budgetService.updateBudget(budget.id, buildBudgetPayload(budget));
    await loadBudgetById(budget.id);
    await loadBudgets();
  };

  const handleIncomeInput = (event) => {
    const { name, value } = event.target;
    setIncomeForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleExpenseInput = (event) => {
    const { name, value } = event.target;
    setExpenseForm((prev) => ({ ...prev, [name]: value }));
  };

  const saveIncomeEntry = async (event) => {
    event.preventDefault();
    if (!selectedBudget || !incomeForm.category || !incomeForm.date) return;

    try {
      setIsSaving(true);
      setError('');
      const payload = buildEntryPayload(incomeForm, 'INCOME', selectedBudget.type);
      if (editingIncomeId) {
        await budgetService.updateEntry(selectedBudget.id, editingIncomeId, payload);
      } else {
        await budgetService.addEntry(selectedBudget.id, payload);
      }
      resetIncomeForm();
      await loadBudgetById(selectedBudget.id);
      await loadBudgets();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save income entry.');
    } finally {
      setIsSaving(false);
    }
  };

  const saveExpenseEntry = async (event) => {
    event.preventDefault();
    if (!selectedBudget || !expenseForm.category || !expenseForm.date) return;

    try {
      setIsSaving(true);
      setError('');
      const payload = buildEntryPayload(expenseForm, 'EXPENSE', selectedBudget.type);
      if (editingExpenseId) {
        await budgetService.updateEntry(selectedBudget.id, editingExpenseId, payload);
      } else {
        await budgetService.addEntry(selectedBudget.id, payload);
      }
      resetExpenseForm();
      await loadBudgetById(selectedBudget.id);
      await loadBudgets();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save expense entry.');
    } finally {
      setIsSaving(false);
    }
  };

  const editIncomeEntry = (entry) => {
    setEditingIncomeId(entry.id);
    setIncomeForm({
      category: entry.category,
      description: entry.description,
      plannedAmount: String(entry.plannedAmount),
      actualAmount: String(entry.actualAmount),
      amountSaved: String(entry.amountSaved || 0),
      date: entry.date
    });
  };

  const editExpenseEntry = (entry) => {
    setEditingExpenseId(entry.id);
    setExpenseForm({
      category: entry.category,
      description: entry.description,
      plannedAmount: String(entry.plannedAmount),
      actualAmount: String(entry.actualAmount),
      amountSaved: '',
      date: entry.date
    });
  };

  const deleteIncomeEntry = async (entryId) => {
    if (!selectedBudget) return;
    try {
      setIsSaving(true);
      await budgetService.deleteEntry(selectedBudget.id, entryId);
      if (editingIncomeId === entryId) resetIncomeForm();
      await loadBudgetById(selectedBudget.id);
      await loadBudgets();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete income entry.');
    } finally {
      setIsSaving(false);
    }
  };

  const deleteExpenseEntry = async (entryId) => {
    if (!selectedBudget) return;
    try {
      setIsSaving(true);
      await budgetService.deleteEntry(selectedBudget.id, entryId);
      if (editingExpenseId === entryId) resetExpenseForm();
      await loadBudgetById(selectedBudget.id);
      await loadBudgets();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete expense entry.');
    } finally {
      setIsSaving(false);
    }
  };

  const updateNotes = async (event) => {
    if (!selectedBudget) return;
    const nextBudget = { ...selectedBudget, notes: event.target.value };
    setSelectedBudget(nextBudget);
  };

  const saveNotes = async () => {
    if (!selectedBudget) return;
    try {
      setIsSaving(true);
      await persistBudgetMeta(selectedBudget);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save notes.');
    } finally {
      setIsSaving(false);
    }
  };

  const updateTypeSpecificValue = async (event) => {
    if (!selectedBudget) return;
    const { name, value } = event.target;
    const nextBudget = { ...selectedBudget, [name]: Number(value) || 0 };
    setSelectedBudget(nextBudget);
    try {
      setIsSaving(true);
      await budgetService.updateBudget(nextBudget.id, buildBudgetPayload(nextBudget));
      await loadBudgets();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update budget settings.');
    } finally {
      setIsSaving(false);
    }
  };

  const openBudget = async (budgetId) => {
    await loadBudgetById(budgetId);
  };

  const closeWorkspace = () => {
    setSelectedBudget(null);
    resetIncomeForm();
    resetExpenseForm();
    loadBudgets();
  };

  const summary = selectedBudget ? calculateBudgetSummary(selectedBudget) : null;

  if (!selectedBudget) {
    return (
      <div className="budget-portal">
        <div className="budget-header">
          <div>
            <h2>My Budgets</h2>
            <p>Create and manage your budget books.</p>
          </div>
          <button className="primary-btn" type="button" onClick={() => setShowNewBudgetForm((prev) => !prev)}>
            {showNewBudgetForm ? 'Close Form' : 'New Budget'}
          </button>
        </div>

        {error && <div className="portal-error">{error}</div>}

        {showNewBudgetForm && (
          <form className="budget-form-card" onSubmit={handleCreateBudget}>
            <h3>Create a New Budget</h3>
            <div className="grid two-columns">
              <label>
                Budget Name
                <input name="name" value={newBudgetForm.name} onChange={handleNewBudgetInput} required />
              </label>
              <label>
                Budget Type
                <select name="type" value={newBudgetForm.type} onChange={handleNewBudgetInput}>
                  {BUDGET_TYPES.map((type) => (
                    <option key={type.value} value={type.value}>{type.label}</option>
                  ))}
                </select>
                <small>{BUDGET_TYPES.find((type) => type.value === newBudgetForm.type)?.help}</small>
              </label>
              <label>
                Period Type
                <select name="periodType" value={newBudgetForm.periodType} onChange={handleNewBudgetInput}>
                  {PERIOD_TYPES.map((period) => (
                    <option key={period} value={period}>{period}</option>
                  ))}
                </select>
              </label>
              <label>
                Start Date
                <input type="date" name="startDate" value={newBudgetForm.startDate} onChange={handleNewBudgetInput} required />
              </label>
              <label>
                End Date
                <input type="date" name="endDate" value={newBudgetForm.endDate} onChange={handleNewBudgetInput} required />
              </label>
            </div>
            <label>
              Description (Optional)
              <textarea name="description" value={newBudgetForm.description} onChange={handleNewBudgetInput} rows={3} />
            </label>
            <button className="primary-btn" type="submit" disabled={isSaving}>
              {isSaving ? 'Saving...' : 'Save Budget'}
            </button>
          </form>
        )}

        {isLoading ? (
          <div className="empty-state"><p>Loading budgets...</p></div>
        ) : budgets.length === 0 ? (
          <div className="empty-state">
            <h3>No budgets yet</h3>
            <p>Create your first budget to start planning and tracking finances.</p>
          </div>
        ) : (
          <div className="budget-cards">
            {budgets.map((budget) => {
              const cardSummary = calculateBudgetSummary(budget);
              return (
                <button key={budget.id} type="button" className="budget-card" onClick={() => openBudget(budget.id)}>
                  <h3>{budget.name}</h3>
                  <p>{BUDGET_TYPES.find((type) => type.value === budget.type)?.label} - {budget.periodType}</p>
                  <p>{budget.startDate} to {budget.endDate}</p>
                  <div className="card-summary">
                    <span>Total Income: {formatAmount(cardSummary.totalActualIncome)}</span>
                    <span>Total Expenses: {formatAmount(cardSummary.totalActualExpenses)}</span>
                  </div>
                </button>
              );
            })}
          </div>
        )}
      </div>
    );
  }

  return (
    <div className="budget-portal">
      <div className="budget-header">
        <div>
          <button type="button" className="text-link" onClick={closeWorkspace}>Back to Budgets</button>
          <h2>{selectedBudget.name}</h2>
          <p>
            {BUDGET_TYPES.find((type) => type.value === selectedBudget.type)?.label} - {selectedBudget.periodType} ({selectedBudget.startDate} to {selectedBudget.endDate})
          </p>
        </div>
      </div>

      {error && <div className="portal-error">{error}</div>}
      {isSaving && <div className="portal-info">Saving changes...</div>}

      {selectedBudget.type === 'PROJECT' && (
        <div className="type-summary">
          <label>
            Project Total Budget
            <input type="number" name="projectTotalBudget" value={selectedBudget.projectTotalBudget} onChange={updateTypeSpecificValue} min="0" />
          </label>
          <div>Spent so far: <strong>{formatAmount(summary.totalActualExpenses)}</strong></div>
          <div>Remaining ceiling: <strong>{formatAmount(selectedBudget.projectTotalBudget - summary.totalActualExpenses)}</strong></div>
        </div>
      )}

      {selectedBudget.type === 'SAVINGS' && (
        <div className="type-summary">
          <label>
            Savings Goal
            <input type="number" name="savingsGoal" value={selectedBudget.savingsGoal} onChange={updateTypeSpecificValue} min="0" />
          </label>
          <div>Total Saved: <strong>{formatAmount(summary.totalSaved)}</strong></div>
          <div>
            Progress: <strong>{selectedBudget.savingsGoal > 0 ? `${Math.min((summary.totalSaved / selectedBudget.savingsGoal) * 100, 100).toFixed(1)}%` : '0%'}</strong>
          </div>
        </div>
      )}

      <section className="summary-grid">
        <article><h4>Total Planned Income</h4><p>{formatAmount(summary.totalPlannedIncome)}</p></article>
        <article><h4>Total Actual Income</h4><p>{formatAmount(summary.totalActualIncome)}</p></article>
        <article><h4>Total Planned Expenses</h4><p>{formatAmount(summary.totalPlannedExpenses)}</p></article>
        <article><h4>Total Actual Expenses</h4><p>{formatAmount(summary.totalActualExpenses)}</p></article>
        <article><h4>Net Balance Planned</h4><p>{formatAmount(summary.plannedNetBalance)}</p></article>
        <article><h4>Net Balance Actual</h4><p>{formatAmount(summary.actualNetBalance)}</p></article>
        <article><h4>Variance</h4><p>{formatAmount(summary.variance)}</p></article>
      </section>

      <section className="table-section">
        <h3>Income</h3>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>No.</th><th>Category</th><th>Description</th><th>Planned Amount</th><th>Actual Amount</th>
                {selectedBudget.type === 'SAVINGS' && <th>Amount Saved</th>}
                <th>Difference (Actual - Planned)</th><th>Date</th><th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {selectedBudget.incomeEntries.map((entry, index) => {
                const difference = entry.actualAmount - entry.plannedAmount;
                return (
                  <tr key={entry.id}>
                    <td>{index + 1}</td>
                    <td>{entry.category}</td>
                    <td>{entry.description || '-'}</td>
                    <td>{formatAmount(entry.plannedAmount)}</td>
                    <td>{formatAmount(entry.actualAmount)}</td>
                    {selectedBudget.type === 'SAVINGS' && <td>{formatAmount(entry.amountSaved)}</td>}
                    <td className={difference < 0 ? 'negative' : 'positive'}>{formatAmount(difference)}</td>
                    <td>{entry.date}</td>
                    <td>
                      <button type="button" className="small-btn" onClick={() => editIncomeEntry(entry)}>Edit</button>
                      <button type="button" className="small-btn danger" onClick={() => deleteIncomeEntry(entry.id)}>Delete</button>
                    </td>
                  </tr>
                );
              })}
              <tr className="totals-row">
                <td colSpan={3}>Totals</td>
                <td>{formatAmount(summary.totalPlannedIncome)}</td>
                <td>{formatAmount(summary.totalActualIncome)}</td>
                {selectedBudget.type === 'SAVINGS' && <td>{formatAmount(summary.totalSaved)}</td>}
                <td>{formatAmount(summary.totalActualIncome - summary.totalPlannedIncome)}</td>
                <td colSpan={2} />
              </tr>
            </tbody>
          </table>
        </div>

        <form className="entry-form" onSubmit={saveIncomeEntry}>
          <h4>{editingIncomeId ? 'Edit Income Entry' : 'Add Income Entry'}</h4>
          <div className="grid five-columns">
            <label>Category<input name="category" value={incomeForm.category} onChange={handleIncomeInput} required /></label>
            <label>Description<input name="description" value={incomeForm.description} onChange={handleIncomeInput} /></label>
            <label>Planned Amount<input type="number" name="plannedAmount" value={incomeForm.plannedAmount} onChange={handleIncomeInput} min="0" /></label>
            <label>Actual Amount<input type="number" name="actualAmount" value={incomeForm.actualAmount} onChange={handleIncomeInput} min="0" /></label>
            {selectedBudget.type === 'SAVINGS' && (
              <label>Amount Saved<input type="number" name="amountSaved" value={incomeForm.amountSaved} onChange={handleIncomeInput} min="0" /></label>
            )}
            <label>Date of Entry<input type="date" name="date" value={incomeForm.date} onChange={handleIncomeInput} required /></label>
          </div>
          <div className="actions-row">
            <button className="primary-btn" type="submit" disabled={isSaving}>{editingIncomeId ? 'Update Income' : 'Add Income'}</button>
            {editingIncomeId && <button type="button" className="secondary-btn" onClick={resetIncomeForm}>Cancel Edit</button>}
          </div>
        </form>
      </section>

      <section className="table-section">
        <h3>Expenses</h3>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>No.</th><th>Category</th><th>Description</th><th>Planned Amount</th><th>Actual Amount</th>
                <th>Difference (Planned - Actual)</th><th>Date</th><th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {selectedBudget.expenseEntries.map((entry, index) => {
                const difference = entry.plannedAmount - entry.actualAmount;
                return (
                  <tr key={entry.id}>
                    <td>{index + 1}</td>
                    <td>{entry.category}</td>
                    <td>{entry.description || '-'}</td>
                    <td>{formatAmount(entry.plannedAmount)}</td>
                    <td>{formatAmount(entry.actualAmount)}</td>
                    <td className={difference < 0 ? 'negative' : 'positive'}>{formatAmount(difference)}</td>
                    <td>{entry.date}</td>
                    <td>
                      <button type="button" className="small-btn" onClick={() => editExpenseEntry(entry)}>Edit</button>
                      <button type="button" className="small-btn danger" onClick={() => deleteExpenseEntry(entry.id)}>Delete</button>
                    </td>
                  </tr>
                );
              })}
              <tr className="totals-row">
                <td colSpan={3}>Totals</td>
                <td>{formatAmount(summary.totalPlannedExpenses)}</td>
                <td>{formatAmount(summary.totalActualExpenses)}</td>
                <td>{formatAmount(summary.totalPlannedExpenses - summary.totalActualExpenses)}</td>
                <td colSpan={2} />
              </tr>
            </tbody>
          </table>
        </div>

        <form className="entry-form" onSubmit={saveExpenseEntry}>
          <h4>{editingExpenseId ? 'Edit Expense Entry' : 'Add Expense Entry'}</h4>
          <div className="grid five-columns">
            <label>Category<input name="category" value={expenseForm.category} onChange={handleExpenseInput} required /></label>
            <label>Description<input name="description" value={expenseForm.description} onChange={handleExpenseInput} /></label>
            <label>Planned Amount<input type="number" name="plannedAmount" value={expenseForm.plannedAmount} onChange={handleExpenseInput} min="0" /></label>
            <label>Actual Amount<input type="number" name="actualAmount" value={expenseForm.actualAmount} onChange={handleExpenseInput} min="0" /></label>
            <label>Date of Entry<input type="date" name="date" value={expenseForm.date} onChange={handleExpenseInput} required /></label>
          </div>
          <div className="actions-row">
            <button className="primary-btn" type="submit" disabled={isSaving}>{editingExpenseId ? 'Update Expense' : 'Add Expense'}</button>
            {editingExpenseId && <button type="button" className="secondary-btn" onClick={resetExpenseForm}>Cancel Edit</button>}
          </div>
        </form>
      </section>

      <section className="notes-section">
        <h3>Notes / Remarks</h3>
        <textarea rows={5} value={selectedBudget.notes} onChange={updateNotes} onBlur={saveNotes} placeholder="Write your observations about this budget..." />
      </section>
    </div>
  );
};

export default BudgetPortal;
