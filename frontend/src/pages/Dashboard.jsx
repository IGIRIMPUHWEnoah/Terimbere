import React, { useState, useEffect } from 'react';
import { TrendingUp, TrendingDown, ArrowUpRight } from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';
import Card from '../components/ui/Card';
import { debtService, budgetService } from '../services/api';
import './Dashboard.css';

const PIE_COLORS = ['#1a1a1a', '#e2e8f0'];

const Dashboard = () => {
  const [metrics, setMetrics] = useState({
    totalBalance: 0,
    monthlyIncome: 0,
    monthlyExpenses: 0,
    activeDebts: 0
  });
  
  const [revenueData, setRevenueData] = useState([]);
  const [savingsData, setSavingsData] = useState([{ name: 'Saved', value: 0 }, { name: 'Remaining', value: 100 }]);
  const [transactions, setTransactions] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        setIsLoading(true);
        
        // Fetch debts for the metric card
        const payablesReq = await debtService.getRemainingSum('I_OWE_THEM');
        const activeDebtsValue = payablesReq.data || 0;

        // In a full implementation, these would come from budgetService and incomeService.
        // For now, we clear the dump data and fetch what we can, initializing the rest to 0.
        // If budgetService has an endpoint for metrics, we'd use it here.
        
        let budgets = [];
        try {
          const budgetReq = await budgetService.getBudgets();
          budgets = budgetReq.data || [];
        } catch (e) {
          console.log("Budgets API not yet fully returning data or empty", e);
        }

        // Calculate a simple mock of income/expenses from budgets if they exist, otherwise 0
        let expenses = 0;
        let income = 0;
        budgets.forEach(b => {
           expenses += b.totalAllocated || 0;
           // assuming totalAllocated represents expenses for this basic view
        });

        setMetrics({
          totalBalance: income - expenses - activeDebtsValue,
          monthlyIncome: income,
          monthlyExpenses: expenses,
          activeDebts: activeDebtsValue
        });

        // Clear the hardcoded chart data until we build the real aggregation backend endpoint
        setRevenueData([
          { name: 'Current', income: income, expenses: expenses }
        ]);

        setTransactions([]); // Clear hardcoded transactions

      } catch (error) {
        console.error("Failed to fetch dashboard data:", error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchDashboardData();
  }, []);

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-RW', { style: 'currency', currency: 'RWF' }).format(amount);
  };

  return (
    <div className="dashboard">
      <div className="dashboard-controls">
        <div className="time-filters">
          <button className="filter-btn">Day</button>
          <button className="filter-btn">Week</button>
          <button className="filter-btn active">Month</button>
          <button className="filter-btn">Year</button>
        </div>
        <div className="date-range">
          <span>Current Period</span>
        </div>
      </div>

      <div className="stats-grid">
        <Card dark>
          <div className="card-title">Total Balance</div>
          <div className="card-value">{formatCurrency(metrics.totalBalance)}</div>
          <div className="card-trend">
            <span className="trend-positive"><TrendingUp size={14} /> 0%</span> from last month
          </div>
        </Card>
        <Card>
          <div className="card-title">Monthly Income</div>
          <div className="card-value">{formatCurrency(metrics.monthlyIncome)}</div>
          <div className="card-trend">
            <span className="trend-positive"><TrendingUp size={14} /> 0%</span> from last month
          </div>
        </Card>
        <Card>
          <div className="card-title">Monthly Expenses</div>
          <div className="card-value">{formatCurrency(metrics.monthlyExpenses)}</div>
          <div className="card-trend">
            <span className="trend-negative"><TrendingDown size={14} /> 0%</span> from last month
          </div>
        </Card>
        <Card>
          <div className="card-title">Active Debts</div>
          <div className="card-value">{formatCurrency(metrics.activeDebts)}</div>
          <div className="card-trend">
            <span className="trend-negative"><TrendingDown size={14} /> 0%</span> from last month
          </div>
        </Card>
      </div>

      <div className="middle-grid">
        <Card className="chart-card">
          <div className="card-header">
            <h3>Revenue & Expenses</h3>
            <button className="icon-btn-small"><ArrowUpRight size={16} /></button>
          </div>
          <div style={{ width: '100%', height: 250 }}>
            {revenueData.length > 0 && (revenueData[0].income > 0 || revenueData[0].expenses > 0) ? (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={revenueData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                  <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{fill: 'var(--text-tertiary)', fontSize: 12}} dy={10} />
                  <YAxis axisLine={false} tickLine={false} tick={{fill: 'var(--text-tertiary)', fontSize: 12}} />
                  <Tooltip cursor={{fill: 'var(--surface-hover)'}} contentStyle={{borderRadius: '8px', border: 'none', boxShadow: 'var(--shadow-md)'}} />
                  <Bar dataKey="expenses" fill="var(--primary-color)" radius={[4, 4, 4, 4]} barSize={20} />
                  <Bar dataKey="income" fill="#a1a1aa" radius={[4, 4, 4, 4]} barSize={20} />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div style={{ height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-tertiary)' }}>
                No chart data available yet.
              </div>
            )}
          </div>
        </Card>

        <Card className="calendar-card">
           <div className="card-header">
             <span className="nav-arrow">&lt;</span>
             <h3>Current</h3>
             <span className="nav-arrow">&gt;</span>
           </div>
           
           <div className="community-growth" style={{ marginTop: '2rem' }}>
              <div className="cg-info">
                <h4>Savings Goal</h4>
                <div className="card-trend">
                  <span className="trend-positive"><TrendingUp size={14} /> 0%</span> from last month
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
          <h3>Recent Transactions</h3>
          <div className="header-actions">
            <button className="icon-btn-small">↻</button>
            <button className="icon-btn-small"><ArrowUpRight size={16} /></button>
          </div>
        </div>
        <div className="table-wrapper">
          {transactions.length > 0 ? (
            <table className="data-table">
              <thead>
                <tr>
                  <th>Description</th>
                  <th>Category</th>
                  <th>Date</th>
                  <th>Amount</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {transactions.map((t, i) => (
                  <tr key={i}>
                    <td>{t.description}</td>
                    <td>{t.category}</td>
                    <td>{t.date}</td>
                    <td className={`amount ${t.amount > 0 ? 'positive' : 'negative'}`}>{formatCurrency(t.amount)}</td>
                    <td><span className={`status-badge ${t.status.toLowerCase()}`}>{t.status}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <div style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-tertiary)', fontStyle: 'italic' }}>
              No recent transactions found. Start by recording an entry.
            </div>
          )}
        </div>
      </Card>
    </div>
  );
};

export default Dashboard;
