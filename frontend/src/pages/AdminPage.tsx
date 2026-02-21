import { useState } from 'react';

interface Props {
  companyId: string | null;
}

const BASE_URL = '/api';

export default function AdminPage({ companyId }: Props) {
  const now = new Date();
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [messageType, setMessageType] = useState<'success' | 'error'>('success');
  const [auditLogs, setAuditLogs] = useState<any[]>([]);
  const [showAudit, setShowAudit] = useState(false);

  const showMsg = (msg: string, type: 'success' | 'error' = 'success') => {
    setMessage(msg);
    setMessageType(type);
  };

  // ── Imports ──

  const handleFileImport = async (endpoint: string, label: string, extraParams = '') => {
    if (!companyId) return;
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.csv,.txt';
    input.onchange = async (e) => {
      const file = (e.target as HTMLInputElement).files?.[0];
      if (!file) return;
      setLoading(true);
      setMessage('');
      try {
        const formData = new FormData();
        formData.append('file', file);
        const url = `${BASE_URL}/companies/${companyId}/${endpoint}${extraParams}`;
        const headers: Record<string, string> = {};
        const token = localStorage.getItem('jwt_token');
        if (token) headers['Authorization'] = `Bearer ${token}`;
        const resp = await fetch(url, { method: 'POST', body: formData, headers });
        if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
        showMsg(`${label}: импортът е успешен (${file.name}).`);
      } catch (e: any) {
        showMsg(`Грешка при импорт: ${e.message}`, 'error');
      }
      setLoading(false);
    };
    input.click();
  };

  // ── Exports ──

  const handleExport = async (endpoint: string, filename: string) => {
    if (!companyId) return;
    setLoading(true);
    try {
      const url = `${BASE_URL}/companies/${companyId}/${endpoint}`;
      const token = localStorage.getItem('jwt_token');
      const resp = await fetch(url, {
        headers: token ? { 'Authorization': `Bearer ${token}` } : {},
      });
      if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
      const blob = await resp.blob();
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = filename;
      a.click();
      URL.revokeObjectURL(a.href);
      showMsg(`Файлът ${filename} е изтеглен.`);
    } catch (e: any) {
      showMsg(`Грешка при експорт: ${e.message}`, 'error');
    }
    setLoading(false);
  };

  // ── Year close ──

  const handleCloseYear = async () => {
    if (!companyId) return;
    if (!confirm(`Сигурни ли сте, че искате да направите годишно приключване за ${year}?`)) return;
    setLoading(true);
    try {
      const token = localStorage.getItem('jwt_token');
      const resp = await fetch(
        `${BASE_URL}/companies/${companyId}/payroll/close-year?year=${year}`,
        { method: 'POST', headers: token ? { 'Authorization': `Bearer ${token}` } : {} }
      );
      if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
      const result = await resp.json();
      showMsg(`Годишно приключване ${year}: ${result.actions.join(' ')}`);
    } catch (e: any) {
      showMsg(`Грешка: ${e.message}`, 'error');
    }
    setLoading(false);
  };

  // ── Audit log ──

  const loadAuditLogs = async () => {
    if (!companyId) return;
    try {
      const token = localStorage.getItem('jwt_token');
      const resp = await fetch(`${BASE_URL}/companies/${companyId}/audit`, {
        headers: token ? { 'Authorization': `Bearer ${token}` } : {},
      });
      if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
      const data = await resp.json();
      setAuditLogs(Array.isArray(data) ? data : []);
      setShowAudit(true);
    } catch (e: any) {
      showMsg(`Грешка при зареждане на одит лог: ${e.message}`, 'error');
    }
  };

  if (!companyId) return <div className="page"><p>Моля, изберете фирма.</p></div>;

  return (
    <div className="page" style={{ maxWidth: 1000 }}>
      <h1>Администриране</h1>

      {message && (
        <p className="message" style={{ color: messageType === 'error' ? '#d32f2f' : '#2e7d32' }}>
          {message}
        </p>
      )}

      {/* Import section */}
      <section className="admin-section">
        <h2>Импорт на данни</h2>
        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
          <button onClick={() => handleFileImport('nomenclatures/import/nkpd', 'НКПД')} disabled={loading}>
            Импорт НКПД
          </button>
          <button onClick={() => handleFileImport(`insurance/import/mod?year=${year}`, 'МОД/ТЗПБ')} disabled={loading}>
            Импорт МОД/ТЗПБ ({year})
          </button>
          <button onClick={() => handleFileImport('employees/import', 'Служители')} disabled={loading}>
            Импорт служители
          </button>
        </div>
        <p className="hint">Формат: CSV файл, разделител запетая или таб. Първият ред може да е заглавен.</p>
      </section>

      {/* Export section */}
      <section className="admin-section">
        <h2>Експорт на данни</h2>
        <div className="toolbar" style={{ marginBottom: '0.75rem' }}>
          <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
            <label>Месец:</label>
            <select value={month} onChange={e => setMonth(Number(e.target.value))}>
              {[1,2,3,4,5,6,7,8,9,10,11,12].map(m => (
                <option key={m} value={m}>{m}</option>
              ))}
            </select>
            <label>Година:</label>
            <input type="number" value={year} style={{ width: 80 }}
              onChange={e => setYear(Number(e.target.value))} />
          </div>
        </div>
        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
          <button onClick={() => handleExport(
            `export/payroll/csv?year=${year}&month=${month}`,
            `payroll_${year}_${String(month).padStart(2, '0')}.csv`
          )} disabled={loading}>
            Експорт ведомост (CSV)
          </button>
          <button onClick={() => handleExport(
            `export/payroll/excel?year=${year}&month=${month}`,
            `payroll_${year}_${String(month).padStart(2, '0')}.xlsx`
          )} disabled={loading}>
            Експорт ведомост (Excel)
          </button>
          <button onClick={() => handleExport(
            'export/employees/csv', 'employees.csv'
          )} disabled={loading}>
            Експорт служители (CSV)
          </button>
        </div>
      </section>

      {/* Year operations */}
      <section className="admin-section">
        <h2>Годишни операции</h2>
        <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
          <label>Година за приключване:</label>
          <input type="number" value={year} style={{ width: 80 }}
            onChange={e => setYear(Number(e.target.value))} />
          <button onClick={handleCloseYear} disabled={loading}
            style={{ background: '#e65100' }}>
            Годишно приключване {year}
          </button>
        </div>
        <p className="hint">
          Прехвърля неизползвани отпуски, генерира календар за Януари на следващата година.
        </p>
      </section>

      {/* Audit log */}
      <section className="admin-section">
        <h2>Одит лог</h2>
        <button onClick={loadAuditLogs} disabled={loading}>
          {showAudit ? 'Обнови' : 'Покажи'} одит лог
        </button>

        {showAudit && (
          <table className="data-table" style={{ marginTop: '0.75rem' }}>
            <thead>
              <tr>
                <th>Дата/час</th>
                <th>Действие</th>
                <th>Описание</th>
                <th>Потребител</th>
              </tr>
            </thead>
            <tbody>
              {auditLogs.length === 0 && (
                <tr><td colSpan={4}>Няма записи.</td></tr>
              )}
              {auditLogs.map((log, i) => (
                <tr key={log.id || i}>
                  <td style={{ whiteSpace: 'nowrap' }}>
                    {log.performedAt ? new Date(log.performedAt).toLocaleString('bg-BG') : '-'}
                  </td>
                  <td><code>{log.action}</code></td>
                  <td>{log.description}</td>
                  <td>{log.performedBy || 'system'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}
