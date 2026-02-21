const BASE_URL = '/api';

function authHeaders(): Record<string, string> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  const token = localStorage.getItem('jwt_token');
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  return headers;
}

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${BASE_URL}${url}`, {
    headers: authHeaders(),
    ...options,
  });
  if (response.status === 401) {
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('jwt_user');
    window.location.href = '/';
    throw new Error('Сесията е изтекла.');
  }
  if (!response.ok) {
    throw new Error(`API error: ${response.status} ${response.statusText}`);
  }
  if (response.status === 204) return undefined as T;
  const text = await response.text();
  if (!text) return undefined as T;
  return JSON.parse(text);
}

// --- Companies ---

export const companyApi = {
  getAll: () => request<any[]>('/companies'),
  getById: (id: string) => request<any>(`/companies/${id}`),
  create: (data: any) => request<any>('/companies', { method: 'POST', body: JSON.stringify(data) }),
  update: (id: string, data: any) => request<any>(`/companies/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (id: string) => request<void>(`/companies/${id}`, { method: 'DELETE' }),
};

// --- Tenant-scoped APIs ---

function tenantApi<T>(resource: string) {
  return {
    getAll: (tenantId: string, params?: string) =>
      request<T[]>(`/companies/${tenantId}/${resource}${params ? `?${params}` : ''}`),
    getById: (tenantId: string, id: string) =>
      request<T>(`/companies/${tenantId}/${resource}/${id}`),
    create: (tenantId: string, data: Partial<T>) =>
      request<T>(`/companies/${tenantId}/${resource}`, { method: 'POST', body: JSON.stringify(data) }),
    update: (tenantId: string, id: string, data: Partial<T>) =>
      request<T>(`/companies/${tenantId}/${resource}/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (tenantId: string, id: string) =>
      request<void>(`/companies/${tenantId}/${resource}/${id}`, { method: 'DELETE' }),
  };
}

export const personnelTypeApi = tenantApi<any>('personnel-types');
export const payItemApi = {
  ...tenantApi<any>('pay-items'),
  seed: (tenantId: string) =>
    request<any[]>(`/companies/${tenantId}/pay-items/seed`, { method: 'POST' }),
};
export const deductionItemApi = {
  ...tenantApi<any>('deduction-items'),
  seed: (tenantId: string) =>
    request<any[]>(`/companies/${tenantId}/deduction-items/seed`, { method: 'POST' }),
};
export const workScheduleApi = tenantApi<any>('work-schedules');
export const calendarApi = {
  ...tenantApi<any>('calendars'),
  generateYear: (tenantId: string, year: number) =>
    request<void>(`/companies/${tenantId}/calendars/generate-year?year=${year}`, { method: 'POST' }),
};
export const annualCalendarApi = {
  ...tenantApi<any>('annual-calendars'),
  seedHolidays: (tenantId: string, year: number) =>
    request<any>(`/companies/${tenantId}/annual-calendars/seed?year=${year}`, { method: 'POST' }),
};
export const shiftScheduleApi = {
  ...tenantApi<any>('shift-schedules'),
  seed: (tenantId: string) =>
    request<any[]>(`/companies/${tenantId}/shift-schedules/seed`, { method: 'POST' }),
};
export const nomenclatureApi = tenantApi<any>('nomenclatures');

export const departmentApi = tenantApi<any>('departments');
export const employeeApi = tenantApi<any>('employees');
export const userApi = tenantApi<any>('users');

// --- Nested employee APIs ---

function employeeNestedApi<T>(resource: string) {
  return {
    getAll: (tenantId: string, employeeId: string, params?: string) =>
      request<T[]>(`/companies/${tenantId}/employees/${employeeId}/${resource}${params ? `?${params}` : ''}`),
    getById: (tenantId: string, employeeId: string, id: string) =>
      request<T>(`/companies/${tenantId}/employees/${employeeId}/${resource}/${id}`),
    create: (tenantId: string, employeeId: string, data: Partial<T>) =>
      request<T>(`/companies/${tenantId}/employees/${employeeId}/${resource}`, { method: 'POST', body: JSON.stringify(data) }),
    update: (tenantId: string, employeeId: string, id: string, data: Partial<T>) =>
      request<T>(`/companies/${tenantId}/employees/${employeeId}/${resource}/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (tenantId: string, employeeId: string, id: string) =>
      request<void>(`/companies/${tenantId}/employees/${employeeId}/${resource}/${id}`, { method: 'DELETE' }),
  };
}

export const employmentApi = employeeNestedApi<any>('employments');
export const absenceApi = employeeNestedApi<any>('absences');
export const amendmentApi = employeeNestedApi<any>('amendments');
export const terminationApi = employeeNestedApi<any>('terminations');
export const leaveEntitlementApi = employeeNestedApi<any>('leave-entitlements');

export const employeePayItemApi = employeeNestedApi<any>('pay-items');
export const employeeDeductionApi = employeeNestedApi<any>('deductions');
export const garnishmentApi = employeeNestedApi<any>('garnishments');

export const timesheetApi = {
  getByMonth: (tenantId: string, year: number, month: number) =>
    request<any[]>(`/companies/${tenantId}/timesheets?year=${year}&month=${month}`),
  getByEmployee: (tenantId: string, employeeId: string, year: number, month: number) =>
    request<any[]>(`/companies/${tenantId}/timesheets/employee/${employeeId}?year=${year}&month=${month}`),
  getById: (tenantId: string, id: string) =>
    request<any>(`/companies/${tenantId}/timesheets/${id}`),
  create: (tenantId: string, data: any) =>
    request<any>(`/companies/${tenantId}/timesheets`, { method: 'POST', body: JSON.stringify(data) }),
  update: (tenantId: string, id: string, data: any) =>
    request<any>(`/companies/${tenantId}/timesheets/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
};

// --- Payroll API ---

export const payrollApi = {
  getPayroll: (tenantId: string, year: number, month: number) =>
    request<any>(`/companies/${tenantId}/payroll?year=${year}&month=${month}`),
  startNewMonth: (tenantId: string, year: number, month: number) =>
    request<void>(`/companies/${tenantId}/payroll/start-new?year=${year}&month=${month}`, { method: 'POST' }),
  calculateAll: (tenantId: string, year: number, month: number) =>
    request<any[]>(`/companies/${tenantId}/payroll/calculate?year=${year}&month=${month}`, { method: 'POST' }),
  calculateEmployee: (tenantId: string, employeeId: string, year: number, month: number) =>
    request<any>(`/companies/${tenantId}/payroll/calculate/${employeeId}?year=${year}&month=${month}`, { method: 'POST' }),
  getSnapshots: (tenantId: string, year: number, month: number) =>
    request<any[]>(`/companies/${tenantId}/payroll/snapshots?year=${year}&month=${month}`),
  getEmployeeSnapshot: (tenantId: string, employeeId: string, year: number, month: number) =>
    request<any>(`/companies/${tenantId}/payroll/snapshots/${employeeId}?year=${year}&month=${month}`),
  closeMonth: (tenantId: string, year: number, month: number) =>
    request<any>(`/companies/${tenantId}/payroll/close?year=${year}&month=${month}`, { method: 'POST' }),
  recalculateMonth: (tenantId: string, year: number, month: number) =>
    request<any[]>(`/companies/${tenantId}/payroll/recalculate?year=${year}&month=${month}`, { method: 'POST' }),
  reopenMonth: (tenantId: string, year: number, month: number) =>
    request<void>(`/companies/${tenantId}/payroll/reopen?year=${year}&month=${month}`, { method: 'POST' }),
  generalReport: (tenantId: string, year: number, month: number) =>
    request<any>(`/companies/${tenantId}/payroll/reports/general?year=${year}&month=${month}`),
  recapReport: (tenantId: string, year: number, month: number) =>
    request<any[]>(`/companies/${tenantId}/payroll/reports/recap?year=${year}&month=${month}`),
  byDepartmentReport: (tenantId: string, year: number, month: number) =>
    request<Record<string, any>>(`/companies/${tenantId}/payroll/reports/by-department?year=${year}&month=${month}`),
  attendanceReport: (tenantId: string, year: number, month: number) =>
    request<any>(`/companies/${tenantId}/payroll/reports/attendance?year=${year}&month=${month}`),
};

// --- Documents API ---

export const documentApi = {
  getTemplates: (tenantId: string, category?: string) =>
    request<any[]>(`/companies/${tenantId}/documents/templates${category ? `?category=${category}` : ''}`),
  getTemplate: (tenantId: string, id: string) =>
    request<any>(`/companies/${tenantId}/documents/templates/${id}`),
  createTemplate: (tenantId: string, data: any) =>
    request<any>(`/companies/${tenantId}/documents/templates`, { method: 'POST', body: JSON.stringify(data) }),
  updateTemplate: (tenantId: string, id: string, data: any) =>
    request<any>(`/companies/${tenantId}/documents/templates/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  deleteTemplate: (tenantId: string, id: string) =>
    request<void>(`/companies/${tenantId}/documents/templates/${id}`, { method: 'DELETE' }),
  seedTemplates: (tenantId: string) =>
    request<any[]>(`/companies/${tenantId}/documents/templates/seed`, { method: 'POST' }),
  generate: (tenantId: string, data: { templateId: string; employeeId: string; contextId?: string }) =>
    request<{ html: string }>(`/companies/${tenantId}/documents/generate`, { method: 'POST', body: JSON.stringify(data) }),
  getPlaceholders: (tenantId: string, category?: string) =>
    request<Record<string, string[]>>(`/companies/${tenantId}/documents/placeholders${category ? `?category=${category}` : ''}`),
};

// --- Declarations API (НАП) ---

export const declarationApi = {
  d1Preview: (tenantId: string, year: number, month: number) =>
    request<any[]>(`/companies/${tenantId}/declarations/d1/preview?year=${year}&month=${month}`),
  d1Generate: (tenantId: string, year: number, month: number, correctionCode = 0) =>
    request<any>(`/companies/${tenantId}/declarations/d1/generate?year=${year}&month=${month}&correctionCode=${correctionCode}`, { method: 'POST' }),
  d1Validate: (tenantId: string, year: number, month: number) =>
    request<any[]>(`/companies/${tenantId}/declarations/d1/validate?year=${year}&month=${month}`),
  d6Preview: (tenantId: string, year: number, month: number) =>
    request<any>(`/companies/${tenantId}/declarations/d6/preview?year=${year}&month=${month}`),
  d6Generate: (tenantId: string, year: number, month: number) =>
    request<any>(`/companies/${tenantId}/declarations/d6/generate?year=${year}&month=${month}`, { method: 'POST' }),
  art62Preview: (tenantId: string, fromDate: string, toDate: string) =>
    request<any[]>(`/companies/${tenantId}/declarations/art62/preview?fromDate=${fromDate}&toDate=${toDate}`),
  art62Generate: (tenantId: string, fromDate: string, toDate: string) =>
    request<any>(`/companies/${tenantId}/declarations/art62/generate?fromDate=${fromDate}&toDate=${toDate}`, { method: 'POST' }),
  art123Preview: (tenantId: string, data: any) =>
    request<any[]>(`/companies/${tenantId}/declarations/art123/preview`, { method: 'POST', body: JSON.stringify(data) }),
  art123Generate: (tenantId: string, data: any) =>
    request<any>(`/companies/${tenantId}/declarations/art123/generate`, { method: 'POST', body: JSON.stringify(data) }),
  art73Preview: (tenantId: string, year: number) =>
    request<any>(`/companies/${tenantId}/declarations/art73/preview?year=${year}`),
  art73Generate: (tenantId: string, year: number) =>
    request<any>(`/companies/${tenantId}/declarations/art73/generate?year=${year}`, { method: 'POST' }),
  getSubmissions: (tenantId: string, type?: string, year?: number, month?: number) => {
    const params = new URLSearchParams();
    if (type) params.set('type', type);
    if (year) params.set('year', year.toString());
    if (month) params.set('month', month.toString());
    const qs = params.toString();
    return request<any[]>(`/companies/${tenantId}/declarations/submissions${qs ? `?${qs}` : ''}`);
  },
  getSubmission: (tenantId: string, id: string) =>
    request<any>(`/companies/${tenantId}/declarations/submissions/${id}`),
  downloadSubmission: (tenantId: string, id: string) =>
    fetch(`/api/companies/${tenantId}/declarations/submissions/${id}/download`).then(r => r.blob()),
};

// --- Bank Payments API ---

export const bankPaymentApi = {
  preview: (tenantId: string, year: number, month: number) =>
    request<any[]>(`/companies/${tenantId}/bank-payments/preview?year=${year}&month=${month}`),
  generate: (tenantId: string, year: number, month: number) =>
    request<any>(`/companies/${tenantId}/bank-payments/generate?year=${year}&month=${month}`, { method: 'POST' }),
  download: (tenantId: string, year: number, month: number) =>
    fetch(`/api/companies/${tenantId}/bank-payments/download?year=${year}&month=${month}`, { method: 'POST' }).then(r => r.blob()),
};

// --- Certificate API (УП-2) ---

export const certificateApi = {
  generateUP2: (tenantId: string, data: { employeeId: string; fromYear: number; fromMonth: number; toYear: number; toMonth: number }) =>
    request<any>(`/companies/${tenantId}/documents/generate-up2`, { method: 'POST', body: JSON.stringify(data) }),
  generateUP3: (tenantId: string, data: { employeeId: string; fromDate: string; toDate: string }) =>
    request<any>(`/companies/${tenantId}/documents/generate-up3`, { method: 'POST', body: JSON.stringify(data) }),
};

// --- Extended Reports API ---

export const extendedReportsApi = {
  insuranceIncome: (tenantId: string, year: number, month: number) =>
    request<any[]>(`/companies/${tenantId}/payroll/reports/insurance-income?year=${year}&month=${month}`),
  comparison: (tenantId: string, year: number, month: number) =>
    request<any>(`/companies/${tenantId}/payroll/reports/comparison?year=${year}&month=${month}`),
  statistics: (tenantId: string, year: number, month: number) =>
    request<any>(`/companies/${tenantId}/payroll/reports/statistics?year=${year}&month=${month}`),
};

// --- Export API ---

export const exportApi = {
  payrollCsv: (tenantId: string, year: number, month: number) =>
    fetch(`/api/companies/${tenantId}/export/payroll/csv?year=${year}&month=${month}`).then(r => r.blob()),
  payrollExcel: (tenantId: string, year: number, month: number) =>
    fetch(`/api/companies/${tenantId}/export/payroll/excel?year=${year}&month=${month}`).then(r => r.blob()),
  payrollPdf: (tenantId: string, year: number, month: number) =>
    fetch(`/api/companies/${tenantId}/export/payroll/pdf?year=${year}&month=${month}`).then(r => r.blob()),
  employeeSlipPdf: (tenantId: string, employeeId: string, year: number, month: number) =>
    fetch(`/api/companies/${tenantId}/export/payroll/pdf/${employeeId}?year=${year}&month=${month}`).then(r => r.blob()),
  employeesCsv: (tenantId: string) =>
    fetch(`/api/companies/${tenantId}/export/employees/csv`).then(r => r.blob()),
};

// --- Audit API ---

// --- Accounting API ---

export const accountingApi = {
  getEntries: (tenantId: string, year: number, month: number) =>
    request<any[]>(`/companies/${tenantId}/accounting/entries?year=${year}&month=${month}`),
  generate: (tenantId: string, year: number, month: number) =>
    request<any[]>(`/companies/${tenantId}/accounting/generate?year=${year}&month=${month}`, { method: 'POST' }),
};

export const auditApi = {
  getAll: (tenantId: string, action?: string) =>
    request<any[]>(`/companies/${tenantId}/audit${action ? `?action=${action}` : ''}`),
};

export const dashboardApi = {
  get: (tenantId: string) => request<any>(`/companies/${tenantId}/dashboard`),
  analytics: (tenantId: string) => request<any>(`/companies/${tenantId}/dashboard/analytics`),
};

// --- NSSI (НОИ) API ---

export const nssiApi = {
  exportPril9: (tenantId: string, employeeId?: string) => {
    const url = `/api/companies/${tenantId}/nssi/export/pril9${employeeId ? `?employeeId=${employeeId}` : ''}`;
    return fetch(url, { headers: authHeaders() }).then(r => r.blob());
  }
};

// --- Year closing ---

export const yearApi = {
  closeYear: (tenantId: string, year: number) =>
    request<any>(`/companies/${tenantId}/payroll/close-year?year=${year}`, { method: 'POST' }),
};

export const economicActivityApi = {
  ...tenantApi<any>('economic-activities'),
  seed: (tenantId: string, year: number) =>
    request<any[]>(`/companies/${tenantId}/economic-activities/seed?year=${year}`, { method: 'POST' }),
  toggleActive: (tenantId: string, id: string) =>
    request<any>(`/companies/${tenantId}/economic-activities/${id}/toggle-active`, { method: 'PATCH' }),
};

export const insuranceApi = {
  getRates: (tenantId: string, year?: number) =>
    request<any[]>(`/companies/${tenantId}/insurance/rates${year ? `?year=${year}` : ''}`),
  createRates: (tenantId: string, data: any) =>
    request<any>(`/companies/${tenantId}/insurance/rates`, { method: 'POST', body: JSON.stringify(data) }),
  updateRates: (tenantId: string, id: string, data: any) =>
    request<any>(`/companies/${tenantId}/insurance/rates/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  getContributions: (tenantId: string, year?: number) =>
    request<any[]>(`/companies/${tenantId}/insurance/contributions${year ? `?year=${year}` : ''}`),
  createContributions: (tenantId: string, data: any) =>
    request<any>(`/companies/${tenantId}/insurance/contributions`, { method: 'POST', body: JSON.stringify(data) }),
  seedContributions: (tenantId: string, year: number) =>
    request<any[]>(`/companies/${tenantId}/insurance/contributions/seed?year=${year}`, { method: 'POST' }),
};

export const insuranceThresholdApi = {
  ...tenantApi<any>('insurance-thresholds'),
  seed: (tenantId: string, year: number) =>
    request<any[]>(`/companies/${tenantId}/insurance-thresholds/seed?year=${year}`, { method: 'POST' }),
};
