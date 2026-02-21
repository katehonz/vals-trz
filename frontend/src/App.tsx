import { useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './auth/AuthContext';
import Sidebar from './components/layout/Sidebar';
import Header from './components/layout/Header';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import CompanySettingsPage from './pages/CompanySettingsPage';
import PayItemsPage from './pages/PayItemsPage';
import WorkSchedulesPage from './pages/WorkSchedulesPage';
import DepartmentsPage from './pages/DepartmentsPage';
import EmployeesPage from './pages/EmployeesPage';
import EmployeeDetailPage from './pages/EmployeeDetailPage';
import PayrollPage from './pages/PayrollPage';
import SalarySlipPage from './pages/SalarySlipPage';
import PayrollReportsPage from './pages/PayrollReportsPage';
import DocumentTemplatesPage from './pages/DocumentTemplatesPage';
import DocumentGeneratePage from './pages/DocumentGeneratePage';
import DeclarationsPage from './pages/DeclarationsPage';
import PersonnelTypesPage from './pages/PersonnelTypesPage';
import CalendarPage from './pages/CalendarPage';
import InsurancePage from './pages/InsurancePage';
import DeductionItemsPage from './pages/DeductionItemsPage';
import NomenclaturesPage from './pages/NomenclaturesPage';
import BankPaymentsPage from './pages/BankPaymentsPage';
import UserManagementPage from './pages/UserManagementPage';
import ShiftSchedulesPage from './pages/ShiftSchedulesPage';
import AccountingPage from './pages/AccountingPage';
import SeniorityBonusConfigPage from './pages/SeniorityBonusConfigPage';
import CertificatesPage from './pages/CertificatesPage';
import PersonnelReportsPage from './pages/PersonnelReportsPage';
import EconomicActivitiesPage from './pages/EconomicActivitiesPage';
import InsuranceThresholdsPage from './pages/InsuranceThresholdsPage';
import AdminPage from './pages/AdminPage';
import './App.css';

function AppContent() {
  const { isAuthenticated, user, logout } = useAuth();

  const [companyId, setCompanyId] = useState<string | null>(
    localStorage.getItem('currentCompanyId')
  );

  const handleCompanyChange = (id: string) => {
    setCompanyId(id || null);
    if (id) {
      localStorage.setItem('currentCompanyId', id);
    } else {
      localStorage.removeItem('currentCompanyId');
    }
  };

  if (!isAuthenticated) {
    return <LoginPage />;
  }

  return (
    <div className="app-layout">
      <Sidebar />
      <div className="app-main">
        <Header
          currentCompanyId={companyId}
          onCompanyChange={handleCompanyChange}
          user={user}
          onLogout={logout}
        />
        <main className="app-content">
          <Routes>
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<DashboardPage companyId={companyId} />} />
            <Route path="/settings/company" element={<CompanySettingsPage companyId={companyId} />} />
            <Route path="/settings/pay-items" element={<PayItemsPage companyId={companyId} />} />
            <Route path="/settings/work-schedules" element={<WorkSchedulesPage companyId={companyId} />} />
            <Route path="/settings/personnel-types" element={<PersonnelTypesPage companyId={companyId} />} />
            <Route path="/settings/calendar" element={<CalendarPage companyId={companyId} />} />
            <Route path="/settings/economic-activities" element={<EconomicActivitiesPage companyId={companyId} />} />
            <Route path="/settings/insurance-thresholds" element={<InsuranceThresholdsPage companyId={companyId} />} />
            <Route path="/settings/insurance" element={<InsurancePage companyId={companyId} />} />
            <Route path="/settings/deduction-items" element={<DeductionItemsPage companyId={companyId} />} />
            <Route path="/settings/nomenclatures" element={<NomenclaturesPage companyId={companyId} />} />
            <Route path="/settings/shift-schedules" element={<ShiftSchedulesPage companyId={companyId} />} />
            <Route path="/settings/seniority" element={<SeniorityBonusConfigPage companyId={companyId} />} />
            <Route path="/settings/*" element={<div className="page"><h1>В разработка</h1></div>} />
            <Route path="/personnel/departments" element={<DepartmentsPage companyId={companyId} />} />
            <Route path="/personnel/employees" element={<EmployeesPage companyId={companyId} />} />
            <Route path="/personnel/employees/:employeeId" element={<EmployeeDetailPage companyId={companyId} />} />
            <Route path="/personnel/*" element={<div className="page"><h1>В разработка</h1></div>} />
            <Route path="/payroll" element={<PayrollPage companyId={companyId} />} />
            <Route path="/payroll/slip/:employeeId" element={<SalarySlipPage companyId={companyId} />} />
            <Route path="/payroll/reports" element={<PayrollReportsPage companyId={companyId} />} />
            <Route path="/documents/templates" element={<DocumentTemplatesPage companyId={companyId} />} />
            <Route path="/documents/generate" element={<DocumentGeneratePage companyId={companyId} />} />
            <Route path="/documents/certificates" element={<CertificatesPage companyId={companyId} />} />
            <Route path="/declarations" element={<DeclarationsPage companyId={companyId} />} />
            <Route path="/payments/bank" element={<BankPaymentsPage companyId={companyId} />} />
            <Route path="/accounting" element={<AccountingPage companyId={companyId} />} />
            <Route path="/admin/users" element={<UserManagementPage companyId={companyId} />} />
            <Route path="/admin" element={<AdminPage companyId={companyId} />} />
            <Route path="/reports/personnel" element={<PersonnelReportsPage companyId={companyId} />} />
            <Route path="/reports/*" element={<div className="page"><h1>В разработка</h1></div>} />
          </Routes>
        </main>
      </div>
    </div>
  );
}

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppContent />
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
