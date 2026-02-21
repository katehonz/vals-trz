import { useEffect, useState } from 'react';
import { employeeApi, certificateApi } from '../api/apiClient';

interface Props {
  companyId: string | null;
}

type Tab = 'up2' | 'up3';

interface Employee {
  id: string;
  firstName: string;
  middleName: string;
  lastName: string;
  egn?: string;
}

export default function CertificatesPage({ companyId }: Props) {
  const now = new Date();
  const [tab, setTab] = useState<Tab>('up2');
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [selectedEmployeeId, setSelectedEmployeeId] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [htmlResult, setHtmlResult] = useState('');

  // UP-2 params
  const [fromYear, setFromYear] = useState(now.getFullYear());
  const [fromMonth, setFromMonth] = useState(1);
  const [toYear, setToYear] = useState(now.getFullYear());
  const [toMonth, setToMonth] = useState(now.getMonth() + 1);

  // UP-3 params
  const [fromDate, setFromDate] = useState(`${now.getFullYear()}-01-01`);
  const [toDate, setToDate] = useState(now.toISOString().slice(0, 10));

  useEffect(() => {
    if (!companyId) return;
    employeeApi.getAll(companyId).then(setEmployees).catch(() => setEmployees([]));
  }, [companyId]);

  if (!companyId) return <div className="page"><p>Моля, изберете фирма.</p></div>;

  async function generateUP2() {
    if (!selectedEmployeeId) { setMessage('Моля, изберете служител.'); return; }
    setLoading(true); setMessage(''); setHtmlResult('');
    try {
      const data = await certificateApi.generateUP2(companyId!, {
        employeeId: selectedEmployeeId,
        fromYear, fromMonth, toYear, toMonth,
      });
      setHtmlResult(data.html || '');
      setMessage(`УП-2 генерирано за ${data.employeeName} (${data.totalDays} осиг. дни, ${fmt(data.totalIncome)} лв.)`);
    } catch (e: any) { setMessage('Грешка: ' + e.message); }
    setLoading(false);
  }

  async function generateUP3() {
    if (!selectedEmployeeId) { setMessage('Моля, изберете служител.'); return; }
    setLoading(true); setMessage(''); setHtmlResult('');
    try {
      const data = await certificateApi.generateUP3(companyId!, {
        employeeId: selectedEmployeeId,
        fromDate, toDate,
      });
      setHtmlResult(data.html || '');
      setMessage(`УП-3 генерирано за ${data.employeeName} (${data.totalYears}г. ${data.totalMonths}м. ${data.totalDays}д.)`);
    } catch (e: any) { setMessage('Грешка: ' + e.message); }
    setLoading(false);
  }

  function fmt(n: number | null | undefined) {
    return n != null ? n.toFixed(2) : '0.00';
  }

  function printResult() {
    const w = window.open('', '_blank');
    if (w) {
      w.document.write(`<html><head><title>Удостоверение</title>
        <style>body{font-family:Arial,sans-serif;font-size:12px;margin:20px}table{border-collapse:collapse;width:100%}td,th{border:1px solid #000;padding:4px 8px;text-align:center}h2,h3{text-align:center}</style>
        </head><body>${htmlResult}</body></html>`);
      w.document.close();
      w.print();
    }
  }

  const monthNames = ['', 'Януари', 'Февруари', 'Март', 'Април', 'Май', 'Юни',
    'Юли', 'Август', 'Септември', 'Октомври', 'Ноември', 'Декември'];

  return (
    <div className="page" style={{ maxWidth: 1100 }}>
      <h1>Удостоверения УП-2 / УП-3</h1>

      <div className="toolbar">
        <div className="tabs" style={{ marginBottom: 0, borderBottom: 'none' }}>
          <button className={tab === 'up2' ? 'active' : ''} onClick={() => { setTab('up2'); setHtmlResult(''); setMessage(''); }}>
            УП-2 (Осиг. доход)
          </button>
          <button className={tab === 'up3' ? 'active' : ''} onClick={() => { setTab('up3'); setHtmlResult(''); setMessage(''); }}>
            УП-3 (Трудов стаж)
          </button>
        </div>
      </div>

      <div style={{ margin: '12px 0' }}>
        <label>Служител:
          <select value={selectedEmployeeId} onChange={e => setSelectedEmployeeId(e.target.value)}
            style={{ marginLeft: 8, minWidth: 300 }}>
            <option value="">-- Избери --</option>
            {employees.map(emp => (
              <option key={emp.id} value={emp.id}>
                {emp.firstName} {emp.middleName} {emp.lastName} {emp.egn ? `(${emp.egn})` : ''}
              </option>
            ))}
          </select>
        </label>
      </div>

      {message && (
        <p style={{ padding: '8px 12px', background: '#f0f0f0', borderRadius: 4, margin: '8px 0' }}>
          {message}
        </p>
      )}

      {/* UP-2 */}
      {tab === 'up2' && (
        <>
          <div style={{ display: 'flex', gap: 12, alignItems: 'center', margin: '12px 0', flexWrap: 'wrap' }}>
            <label>От:
              <select value={fromMonth} onChange={e => setFromMonth(Number(e.target.value))} style={{ marginLeft: 4 }}>
                {monthNames.slice(1).map((n, i) => <option key={i + 1} value={i + 1}>{n}</option>)}
              </select>
              <input type="number" value={fromYear} onChange={e => setFromYear(Number(e.target.value))}
                style={{ width: 70, marginLeft: 4 }} />
            </label>
            <label>До:
              <select value={toMonth} onChange={e => setToMonth(Number(e.target.value))} style={{ marginLeft: 4 }}>
                {monthNames.slice(1).map((n, i) => <option key={i + 1} value={i + 1}>{n}</option>)}
              </select>
              <input type="number" value={toYear} onChange={e => setToYear(Number(e.target.value))}
                style={{ width: 70, marginLeft: 4 }} />
            </label>
            <button className="btn btn-primary" onClick={generateUP2} disabled={loading}>
              {loading ? 'Генериране...' : 'Генерирай УП-2'}
            </button>
          </div>
        </>
      )}

      {/* UP-3 */}
      {tab === 'up3' && (
        <>
          <div style={{ display: 'flex', gap: 12, alignItems: 'center', margin: '12px 0' }}>
            <label>От: <input type="date" value={fromDate} onChange={e => setFromDate(e.target.value)} /></label>
            <label>До: <input type="date" value={toDate} onChange={e => setToDate(e.target.value)} /></label>
            <button className="btn btn-primary" onClick={generateUP3} disabled={loading}>
              {loading ? 'Генериране...' : 'Генерирай УП-3'}
            </button>
          </div>
        </>
      )}

      {/* Result preview */}
      {htmlResult && (
        <div style={{ marginTop: 16 }}>
          <div style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
            <button className="btn" onClick={printResult}>Печат</button>
          </div>
          <div
            style={{
              border: '1px solid #ccc',
              padding: 20,
              background: '#fff',
              color: '#000',
              maxHeight: 600,
              overflow: 'auto',
            }}
            dangerouslySetInnerHTML={{ __html: htmlResult }}
          />
        </div>
      )}
    </div>
  );
}
