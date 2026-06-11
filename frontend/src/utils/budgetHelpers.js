export const numberValue = (value) => {
  const numeric = Number(value);
  return Number.isFinite(numeric) ? numeric : 0;
};

export const formatAmount = (value) => numberValue(value).toLocaleString('en-US');

export const formatCurrency = (amount) =>
  new Intl.NumberFormat('en-RW', { style: 'currency', currency: 'RWF' }).format(numberValue(amount));

export const toApiPeriodType = (period) => period.toUpperCase();

export const fromApiPeriodType = (period) => {
  if (!period) return 'Custom';
  return period.charAt(0) + period.slice(1).toLowerCase();
};

export const mapBudgetFromApi = (budget) => {
  const entries = budget.entries || [];
  return {
    id: budget.id,
    name: budget.name,
    type: budget.budgetType || 'PERSONAL',
    periodType: fromApiPeriodType(budget.periodType),
    startDate: budget.startDate,
    endDate: budget.endDate,
    description: budget.description || '',
    notes: budget.notes || '',
    status: budget.status || 'ACTIVE',
    savingsGoal: numberValue(budget.savingsGoal),
    projectTotalBudget: numberValue(budget.projectTotalBudget),
    incomeEntries: entries
      .filter((entry) => entry.entryType === 'INCOME')
      .map(mapEntryFromApi),
    expenseEntries: entries
      .filter((entry) => entry.entryType === 'EXPENSE')
      .map(mapEntryFromApi)
  };
};

export const mapEntryFromApi = (entry) => ({
  id: entry.id,
  category: entry.category,
  description: entry.description || '',
  plannedAmount: numberValue(entry.plannedAmount),
  actualAmount: numberValue(entry.actualAmount),
  amountSaved: numberValue(entry.amountSaved),
  date: entry.entryDate
});

export const buildBudgetPayload = (budget) => ({
  name: budget.name,
  description: budget.description || '',
  periodType: toApiPeriodType(budget.periodType),
  startDate: budget.startDate,
  endDate: budget.endDate,
  status: budget.status || 'ACTIVE',
  budgetType: budget.type,
  notes: budget.notes || '',
  savingsGoal: budget.type === 'SAVINGS' ? numberValue(budget.savingsGoal) : null,
  projectTotalBudget: budget.type === 'PROJECT' ? numberValue(budget.projectTotalBudget) : null
});

export const buildEntryPayload = (entry, entryType, budgetType) => ({
  entryType,
  category: entry.category,
  description: entry.description || '',
  plannedAmount: numberValue(entry.plannedAmount),
  actualAmount: numberValue(entry.actualAmount),
  entryDate: entry.date,
  amountSaved: budgetType === 'SAVINGS' && entryType === 'INCOME' ? numberValue(entry.amountSaved) : null
});

export const calculateBudgetSummary = (budget) => {
  const totalPlannedIncome = budget.incomeEntries.reduce((sum, row) => sum + row.plannedAmount, 0);
  const totalActualIncome = budget.incomeEntries.reduce((sum, row) => sum + row.actualAmount, 0);
  const totalPlannedExpenses = budget.expenseEntries.reduce((sum, row) => sum + row.plannedAmount, 0);
  const totalActualExpenses = budget.expenseEntries.reduce((sum, row) => sum + row.actualAmount, 0);
  const plannedNetBalance = totalPlannedIncome - totalPlannedExpenses;
  const actualNetBalance = totalActualIncome - totalActualExpenses;
  const totalSaved = budget.type === 'SAVINGS'
    ? budget.incomeEntries.reduce((sum, row) => sum + row.amountSaved, 0)
    : 0;

  return {
    totalPlannedIncome,
    totalActualIncome,
    totalPlannedExpenses,
    totalActualExpenses,
    plannedNetBalance,
    actualNetBalance,
    variance: plannedNetBalance - actualNetBalance,
    totalSaved
  };
};
