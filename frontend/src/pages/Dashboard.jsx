import { useState, useEffect } from 'react';
import { TrendingUp, TrendingDown, ArrowUpRight } from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';
import Card from '../components/ui/Card';
import { debtService, budgetService } from '../services/api';
import { formatCurrency, mapBudgetFromApi, numberValue } from '../utils/budgetHelpers';
import './Dashboard.css';

const PIE_COLORS = ['#1a1a1a', '#e2e8f0'];

const Dashboard = () => {
  const [metrics, setMetrics] = useState({
    totalBalance: 0,
    monthlyIncome: 0,
    monthlyExpenses: 0,
    activeDebts: 0,
    receivables: 0
  });
  const [revenueData, setRevenueData] = useState([]);
  const [savingsData, setSavingsData] = useState([
    { name: 'Saved', value: 0 },
    { name: 'Remaining', value: 100 }
  ]);
  const [transactions, setTransactions] = useState([]);
  const [sortConfig, setSortConfig] = useState({ key: 'date', direction: 'desc' });
  const [currentPage, setCurrentPage] = useState(1);
  const pageSize = 5;
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        setIsLoading(true);
        setError('');

        const [payablesReq, receivablesReq, budgetReq, debtsReq] = await Promise.all([
          debtService.getRemainingSum('I_OWE_THEM'),
          debtService.getRemainingSum('THEY_OWE_ME'),
          budgetService.getBudgets(),
          debtService.getDebtRecords()
        ]);

        const payables = numberValue(payablesReq.data);
        const receivables = numberValue(receivablesReq.data);
        const budgets = (budgetReq.data || []).map(mapBudgetFromApi);
        const debtRecords = Array.isArray(debtsReq.data) ? debtsReq.data : [];

        let income = 0;
        let expenses = 0;
        let totalSaved = 0;
        let savingsGoal = 0;
        const recentEntries = [];

        budgets.forEach((budget) => {
          budget.incomeEntries.forEach((entry) => {
            income += entry.actualAmount;
            totalSaved += entry.amountSaved;
            recentEntries.push({
              description: entry.description || entry.category,
              category: entry.category,
              date: entry.date,
              amount: entry.actualAmount,
              status: 'Income',
              budgetName: budget.name
            });
          });
          budget.expenseEntries.forEach((entry) => {
            expenses += entry.actualAmount;
            recentEntries.push({
              description: entry.description || entry.category,
              category: entry.category,
              date: entry.date,
              amount: -entry.actualAmount,
              status: 'Expense',
              budgetName: budget.name
            });
          });
          if (budget.type === 'SAVINGS') {
            savingsGoal += numberValue(budget.savingsGoal);
          }
        });

        debtRecords.slice(0, 5).forEach((debt) => {
          recentEntries.push({
            description: `${debt.contact?.fullName || 'Contact'} debt`,
            category: debt.debtDirection === 'THEY_OWE_ME' ? 'Receivable' : 'Payable',
            date: debt.dueDate,
            amount: debt.debtDirection === 'THEY_OWE_ME' ? numberValue(debt.remainingAmount) : -numberValue(debt.remainingAmount),
            status: debt.status || 'ACTIVE',
            budgetName: 'Debts'
          });
        });

        recentEntries.sort((a, b) => new Date(b.date) - new Date(a.date));

        const savedPercent = savingsGoal > 0 ? Math.min((totalSaved / savingsGoal) * 100, 100) : 0;

        setMetrics({
          totalBalance: income - expenses + receivables - payables,
          monthlyIncome: income,
          monthlyExpenses: expenses,
          activeDebts: payables,
          receivables
        });
        setRevenueData([{ name: 'Current', income, expenses }]);
        setSavingsData([
          { name: 'Saved', value: savedPercent },
          { name: 'Remaining', value: Math.max(100 - savedPercent, 0) }
        ]);
        setTransactions(recentEntries);
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to load dashboard data.');
      } finally {
        setIsLoading(false);
      }
    };

    fetchDashboardData();
  }, []);

  const sortTransactions = (items, key, direction) => {
    const sorted = [...items].sort((a, b) => {
      if (key === 'amount') {
        return (a.amount - b.amount) * (direction === 'asc' ? 1 : -1);
      }
      if (key === 'date') {
        return (new Date(a.date) - new Date(b.date)) * (direction === 'asc' ? 1 : -1);
      }
      return String(a[key]).localeCompare(String(b[key])) * (direction === 'asc' ? 1 : -1);
    });
    return sorted;
  };

  const sortedTransactions = sortTransactions(transactions, sortConfig.key, sortConfig.direction);
  const totalPages = Math.max(1, Math.ceil(sortedTransactions.length / pageSize));
  const paginatedTransactions = sortedTransactions.slice((currentPage - 1) * pageSize, currentPage * pageSize);

  const requestSort = (key) => {
    setCurrentPage(1);
    setSortConfig((prev) => {
      if (prev.key === key) {
        return { key, direction: prev.direction === 'asc' ? 'desc' : 'asc' };
      }
      return { key, direction: 'asc' };
    });
  };

  const sortIndicator = (key) => {
    if (sortConfig.key !== key) return '';
    return sortConfig.direction === 'asc' ? ' ↑' : ' ↓';
  };

  if (isLoading) {
    return (
      <div className="dashboard loading-state">
        Loading dashboard...
      </div>
    );
  }

  return (
    <div className="dashboard">
      {error && <div className="portal-error">{error}</div>}

      <div className="dashboard-controls">
        <div className="time-filters">
          <button type="button" className="filter-btn">Day</button>
          <button type="button" className="filter-btn">Week</button>
          <button type="button" className="filter-btn active">Month</button>
          <button type="button" className="filter-btn">Year</button>
        </div>
        <div className="date-range"><span>Current Period</span></div>
      </div>

      <div className="stats-grid">
        <Card dark>
          <div className="card-title">Total Balance</div>
          <div className="card-value">{formatCurrency(metrics.totalBalance)}</div>
          <div className="card-trend">
            <span className={metrics.totalBalance >= 0 ? 'trend-positive' : 'trend-negative'}>
              <TrendingUp size={14} /> Net position
            </span>
          </div>
        </Card>
        <Card>
          <div className="card-title">Actual Income</div>
          <div className="card-value">{formatCurrency(metrics.monthlyIncome)}</div>
          <div className="card-trend"><span className="trend-positive"><TrendingUp size={14} /> From budgets</span></div>
        </Card>
        <Card>
          <div className="card-title">Actual Expenses</div>
          <div className="card-value">{formatCurrency(metrics.monthlyExpenses)}</div>
          <div className="card-trend"><span className="trend-negative"><TrendingDown size={14} /> From budgets</span></div>
        </Card>
        <Card>
          <div className="card-title">Money I Owe</div>
          <div className="card-value">{formatCurrency(metrics.activeDebts)}</div>
          <div className="card-trend"><span className="trend-negative"><TrendingDown size={14} /> Creditors</span></div>
        </Card>
      </div>

      <div className="middle-grid">
        <Card className="chart-card">
          <div className="card-header">
            <h3>Revenue & Expenses</h3>
            <button type="button" className="icon-btn-small"><ArrowUpRight size={16} /></button>
          </div>
          <div style={{ width: '100%', height: 250 }}>
            {revenueData.length > 0 && (revenueData[0].income > 0 || revenueData[0].expenses > 0) ? (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={revenueData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                  <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fill: 'var(--text-tertiary)', fontSize: 12 }} dy={10} />
                  <YAxis axisLine={false} tickLine={false} tick={{ fill: 'var(--text-tertiary)', fontSize: 12 }} />
                  <Tooltip cursor={{ fill: 'var(--surface-hover)' }} contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: 'var(--shadow-md)' }} />
                  <Bar dataKey="expenses" fill="var(--primary-color)" radius={[4, 4, 4, 4]} barSize={20} />
                  <Bar dataKey="income" fill="#a1a1aa" radius={[4, 4, 4, 4]} barSize={20} />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div className="chart-empty">No chart data available yet.</div>
            )}
          </div>
        </Card>

        <Card className="calendar-card">
          <div className="card-header">
            <span className="nav-arrow">&lt;</span>
            <h3>Savings Overview</h3>
            <span className="nav-arrow">&gt;</span>
          </div>
          <div className="community-growth savings-overview">
            <div className="cg-info">
              <h4>Savings Goal Progress</h4>
              <div className="card-trend">
                <span className="trend-positive"><TrendingUp size={14} /> {savingsData[0].value.toFixed(1)}%</span>
              </div>
            </div>
            <div style={{ width: 80, height: 80 }}>
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie data={savingsData} cx="50%" cy="50%" innerRadius={25} outerRadius={35} startAngle={90} endAngle={-270} dataKey="value" stroke="none">
                    {savingsData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={PIE_COLORS[index % PIE_COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            </div>
          </div>
        </Card>
      </div>

      <Card className="table-card">
        <div className="card-header">
          <h3>Recent Activity</h3>
          <div className="table-meta">
            <span>{sortedTransactions.length} records</span>
          </div>
        </div>
        <div className="table-wrapper compact-table">
          {transactions.length > 0 ? (
            <table className="data-table">
              <thead>
                <tr>
                  <th>
                    <button type="button" className="sort-btn" onClick={() => requestSort('description')}>
                      Description{sortIndicator('description')}
                    </button>
                  </th>
                  <th>
                    <button type="button" className="sort-btn" onClick={() => requestSort('category')}>
                      Category{sortIndicator('category')}
                    </button>
                  </th>
                  <th>Source</th>
                  <th>
                    <button type="button" className="sort-btn" onClick={() => requestSort('date')}>
                      Date{sortIndicator('date')}
                    </button>
                  </th>
                  <th>
                    <button type="button" className="sort-btn" onClick={() => requestSort('amount')}>
                      Amount{sortIndicator('amount')}
                    </button>
                  </th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {paginatedTransactions.map((transaction, index) => (
                  <tr key={`${transaction.description}-${index}`}>
                    <td>{transaction.description}</td>
                    <td>{transaction.category}</td>
                    <td>{transaction.budgetName}</td>
                    <td>{transaction.date}</td>
                    <td className={`amount ${transaction.amount >= 0 ? 'positive' : 'negative'}`}>{formatCurrency(transaction.amount)}</td>
                    <td><span className={`status-badge ${String(transaction.status).toLowerCase()}`}>{transaction.status}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <div className="chart-empty">No recent activity found. Start by creating a budget or recording a debt.</div>
          )}
        </div>
        {transactions.length > pageSize && (
          <div className="pagination">
            <button type="button" className="page-btn" onClick={() => setCurrentPage((p) => Math.max(1, p - 1))} disabled={currentPage === 1}>
              Previous
            </button>
            <span>
              Page {currentPage} of {totalPages}
            </span>
            <button
              type="button"
              className="page-btn"
              onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
              disabled={currentPage === totalPages}
            >
              Next
            </button>
          </div>
        )}
      </Card>
    </div>
  );
};

export default Dashboard;
