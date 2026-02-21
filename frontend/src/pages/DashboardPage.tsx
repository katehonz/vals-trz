import { useEffect, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { dashboardApi } from '../api/apiClient';

interface DashboardData {
  employeeCount: number;
  departmentCount: number;
  currentMonthYear: number;
  currentMonthMonth: number;
  currentMonthStatus: string;
  employeeCountCalculated: number;
  calculatedAt: string;
  totalGross: number;
  totalNet: number;
  totalEmployerCost: number;
  pendingAbsences: number;
  recentAuditLogs: any[];
}

const monthNames = [
  'Януари', 'Февруари', 'Март', 'Април', 'Май', 'Юни',
  'Юли', 'Август', 'Септември', 'Октомври', 'Ноември', 'Декември'
];

interface AnalyticsData {
  totalEmployees: number;
  activeEmployees: number;
  inactiveEmployees: number;
  genderDistribution: Record<string, number>;
  ageDistribution: Record<string, number>;
  educationDistribution: Record<string, number>;
  averageAge: number;
  averageSeniority: number;
  hiredThisYear: number;
  terminatedThisYear: number;
}

export default function DashboardPage({ companyId }: { companyId: string | null }) {
  const [data, setData] = useState<DashboardData | null>(null);
  const [analytics, setAnalytics] = useState<AnalyticsData | null>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    if (!companyId) return;
    loadData();
  }, [companyId]);

  const loadData = async () => {
    try {
      setLoading(true);
      const [res, ana] = await Promise.all([
        dashboardApi.get(companyId!),
        dashboardApi.analytics(companyId!).catch(() => null),
      ]);
      setData(res);
      setAnalytics(ana);
    } catch (err) {
      console.error('Failed to load dashboard data:', err);
    } finally {
      setLoading(false);
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'CLOSED': return <span className="status-badge active">Приключена</span>;
      case 'CALCULATED': return <span className="status-badge" style={{ background: '#fbc02d', color: '#000' }}>Изчислена</span>;
      case 'OPEN': return <span className="status-badge" style={{ background: '#1976d2', color: '#fff' }}>Отворена</span>;
      default: return <span className="status-badge inactive">Нестартирала</span>;
    }
  };

  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('bg-BG', { style: 'currency', currency: 'BGN' }).format(val || 0);
  };

  if (!companyId) return <div className="page"><p>Моля, изберете фирма.</p></div>;
  if (loading) return <div className="page"><p>Зареждане...</p></div>;
  if (!data) return <div className="page"><p>Няма данни за показване.</p></div>;

  return (
    <div className="page">
      <h1>Табло (Dashboard)</h1>
      <p style={{ marginBottom: '20px', color: '#666' }}>
        Обзор на {monthNames[data.currentMonthMonth - 1]} {data.currentMonthYear} г.
      </p>

      <div className="dashboard-cards">
        <div className="dashboard-card">
          <div className="card-value">{data.employeeCount}</div>
          <div className="card-label">Активни служители</div>
        </div>
        <div className="dashboard-card">
          <div className="card-value">{data.departmentCount}</div>
          <div className="card-label">Отдели</div>
        </div>
        <div className="dashboard-card">
          <div className="card-value" style={{ fontSize: '1.2rem', padding: '0.6rem 0' }}>
            {getStatusBadge(data.currentMonthStatus)}
          </div>
          <div className="card-label">Статус на ведомост</div>
        </div>
        <div className="dashboard-card">
          <div className="card-value" style={{ fontSize: '1.5rem' }}>{formatCurrency(data.totalNet)}</div>
          <div className="card-label">Общо Нето</div>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '20px' }}>
        <div className="admin-section">
          <h3>Текущ месец - Статистика</h3>
          <div className="form-grid" style={{ background: '#f5f5f5', padding: '15px', borderRadius: '4px' }}>
            <div>
              <strong>Последно изчисление:</strong><br />
              {data.calculatedAt ? new Date(data.calculatedAt).toLocaleString('bg-BG') : '—'}
            </div>
            <div>
              <strong>Изчислени служители:</strong><br />
              {data.employeeCountCalculated || 0}
            </div>
            <div>
              <strong>Общо Бруто:</strong><br />
              {formatCurrency(data.totalGross)}
            </div>
            <div>
              <strong>Разход за работодател:</strong><br />
              {formatCurrency(data.totalEmployerCost)}
            </div>
            <div>
              <strong>Чакащи отсъствия:</strong><br />
              <span style={{ color: data.pendingAbsences > 0 ? 'red' : 'inherit', fontWeight: data.pendingAbsences > 0 ? 'bold' : 'normal' }}>
                {data.pendingAbsences}
              </span>
            </div>
          </div>

          <h3 style={{ marginTop: '30px' }}>Последни действия (Одит)</h3>
          <table className="data-table">
            <thead>
              <tr>
                <th>Дата</th>
                <th>Действие</th>
                <th>Описание</th>
              </tr>
            </thead>
            <tbody>
              {data.recentAuditLogs?.map((log, i) => (
                <tr key={log.id || i}>
                  <td>{new Date(log.performedAt).toLocaleString('bg-BG')}</td>
                  <td><span className="status-badge" style={{ background: '#e0e0e0', color: '#333' }}>{log.action}</span></td>
                  <td>{log.description}</td>
                </tr>
              ))}
              {(!data.recentAuditLogs || data.recentAuditLogs.length === 0) && (
                <tr><td colSpan={3} style={{ textAlign: 'center' }}>Няма данни.</td></tr>
              )}
            </tbody>
          </table>
        </div>

        <div className="admin-section">
          <h3>Бързи линкове</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            <button style={{ width: '100%', textAlign: 'left' }} onClick={() => navigate('/payroll')}>
              💸 Изчисли заплати
            </button>
            <button style={{ width: '100%', textAlign: 'left' }} onClick={() => navigate('/personnel/employees')}>
              👥 Списък служители
            </button>
            <button style={{ width: '100%', textAlign: 'left' }} onClick={() => navigate('/declarations')}>
              📄 Декларации (НАП)
            </button>
            <button style={{ width: '100%', textAlign: 'left' }} onClick={() => navigate('/payments/bank')}>
              🏦 Банкови плащания
            </button>
            <button style={{ width: '100%', textAlign: 'left' }} onClick={() => navigate('/admin/users')}>
              👤 Потребители
            </button>
          </div>

          <div style={{ marginTop: '30px', padding: '15px', border: '1px dashed #ccc', borderRadius: '4px' }}>
            <h4>Помощ</h4>
            <p style={{ fontSize: '0.9rem' }}>
              За въпроси относно системата, проверете <Link to="/help">документацията</Link> или се свържете с поддръжката.
            </p>
          </div>
        </div>
      </div>

      {/* Анализ на персонала */}
      {analytics && (
        <div style={{ marginTop: 24 }}>
          <h2>Анализ на персонала</h2>
          <div className="dashboard-cards">
            <div className="dashboard-card">
              <div className="card-value">{analytics.averageAge}</div>
              <div className="card-label">Средна възраст (години)</div>
            </div>
            <div className="dashboard-card">
              <div className="card-value">{analytics.averageSeniority}</div>
              <div className="card-label">Среден стаж (години)</div>
            </div>
            <div className="dashboard-card">
              <div className="card-value" style={{ color: '#2e7d32' }}>+{analytics.hiredThisYear}</div>
              <div className="card-label">Наети тази година</div>
            </div>
            <div className="dashboard-card">
              <div className="card-value" style={{ color: '#d32f2f' }}>-{analytics.terminatedThisYear}</div>
              <div className="card-label">Напуснали тази година</div>
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 20, marginTop: 16 }}>
            <div className="admin-section">
              <h3>По пол</h3>
              <table className="data-table">
                <tbody>
                  {Object.entries(analytics.genderDistribution).map(([k, v]) => (
                    <tr key={k}>
                      <td>{k === 'M' ? 'Мъже' : k === 'F' ? 'Жени' : k}</td>
                      <td style={{ textAlign: 'right', fontWeight: 600 }}>{v}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="admin-section">
              <h3>По възраст</h3>
              <table className="data-table">
                <tbody>
                  {Object.entries(analytics.ageDistribution).map(([k, v]) => (
                    <tr key={k}>
                      <td>{k}</td>
                      <td style={{ textAlign: 'right', fontWeight: 600 }}>{v}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="admin-section">
              <h3>По образование</h3>
              <table className="data-table">
                <tbody>
                  {Object.entries(analytics.educationDistribution).map(([k, v]) => (
                    <tr key={k}>
                      <td>{k}</td>
                      <td style={{ textAlign: 'right', fontWeight: 600 }}>{v}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
