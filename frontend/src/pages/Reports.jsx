import { useState, useEffect, useCallback } from 'react';
import { FileText, Download, FileSpreadsheet, Loader2 } from 'lucide-react';
import Card from '../components/ui/Card';
import { budgetService, reportService } from '../services/api';
import { formatCurrency } from '../utils/budgetHelpers';
import './Reports.css';

const Reports = () => {
  const [budgets, setBudgets] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [downloadingId, setDownloadingId] = useState(null);

  const loadBudgets = useCallback(async () => {
    try {
      setIsLoading(true);
      setError('');
      const res = await budgetService.getBudgets();
      setBudgets(Array.isArray(res.data) ? res.data : []);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load budgets for reporting.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadBudgets();
  }, [loadBudgets]);

  const handleDownload = async (budget, format) => {
    try {
      setDownloadingId(`${budget.id}-${format}`);
      setError('');
      
      let response;
      let filename = `${budget.title.replace(/\s+/g, '_')}_Report`;
      
      if (format === 'PDF') {
        response = await reportService.downloadBudgetPdf(budget.id);
        filename += '.pdf';
      } else {
        response = await reportService.downloadBudgetExcel(budget.id);
        filename += '.xlsx';
      }

      // Create a blob from the response data
      const blob = new Blob([response.data], { 
        type: response.headers['content-type'] || 'application/octet-stream' 
      });
      
      // Create a temporary link element to trigger the download
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      
      // Cleanup
      link.parentNode.removeChild(link);
      window.URL.revokeObjectURL(url);
      
    } catch (err) {
      console.error(err);
      setError(`Failed to download ${format} report for ${budget.title}.`);
    } finally {
      setDownloadingId(null);
    }
  };

  return (
    <div className="reports-portal">
      <div className="portal-header">
        <div>
          <h2 className="portal-title">Reports & Exports</h2>
          <p className="portal-subtitle">Generate formatted documents of your financial data.</p>
        </div>
      </div>

      {error && <div className="portal-error">{error}</div>}

      <Card>
        <h3 style={{ marginTop: 0, marginBottom: '1.5rem', fontSize: '1.25rem', color: 'var(--text-primary)' }}>
          Budget Summaries
        </h3>
        
        {isLoading ? (
          <div className="empty-state">Loading available data...</div>
        ) : budgets.length === 0 ? (
          <div className="empty-state">
            No budgets found. Create a budget in the Budgets portal first to generate reports.
          </div>
        ) : (
          <div className="reports-list">
            {budgets.map(budget => (
              <div key={budget.id} className="report-item">
                <div className="report-info">
                  <span className="report-title">{budget.title}</span>
                  <span className="report-meta">
                    {budget.period} • Target: {formatCurrency(budget.totalAmount)}
                  </span>
                </div>
                <div className="report-actions">
                  <button 
                    className="btn-download pdf"
                    disabled={downloadingId !== null}
                    onClick={() => handleDownload(budget, 'PDF')}
                  >
                    {downloadingId === `${budget.id}-PDF` ? (
                      <Loader2 size={16} className="lucide-spin" />
                    ) : (
                      <FileText size={16} />
                    )}
                    PDF
                  </button>
                  <button 
                    className="btn-download excel"
                    disabled={downloadingId !== null}
                    onClick={() => handleDownload(budget, 'EXCEL')}
                  >
                    {downloadingId === `${budget.id}-EXCEL` ? (
                      <Loader2 size={16} className="lucide-spin" />
                    ) : (
                      <FileSpreadsheet size={16} />
                    )}
                    Excel
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>
    </div>
  );
};

export default Reports;
