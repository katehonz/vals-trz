export interface PayrollLine {
  code: string;
  name: string;
  type: string;
  base: number;
  rate: number;
  quantity: number;
  amount: number;
}

export interface PayrollSnapshot {
  id: string;
  tenantId: string;
  employeeId: string;
  year: number;
  month: number;
  calculatedAt: string;
  closedAt: string | null;
  status: string;
  employeeData: Record<string, any>;
  legislationParams: Record<string, any>;
  timesheetData: Record<string, any>;
  earnings: PayrollLine[];
  deductions: PayrollLine[];
  employerContributions: PayrollLine[];
  grossSalary: number;
  insurableIncome: number;
  totalEmployeeInsurance: number;
  taxBase: number;
  incomeTax: number;
  totalDeductions: number;
  netSalary: number;
  totalEmployerCost: number;
  totalEmployerInsurance: number;
}

export interface Payroll {
  id: string;
  tenantId: string;
  year: number;
  month: number;
  status: string;
  calculatedAt: string | null;
  closedAt: string | null;
  closedBy: string | null;
  employeeCount: number;
}

export interface PayrollReportRow {
  employeeId: string;
  employeeName: string;
  department: string;
  values: Record<string, number>;
  gross: number;
  insurance: number;
  tax: number;
  net: number;
}

export interface PayrollReportData {
  columnCodes: string[];
  columnNames: string[];
  rows: PayrollReportRow[];
  totals: Record<string, number>;
}

export interface RecapLine {
  code: string;
  name: string;
  totalAmount: number;
  employeeCount: number;
}
