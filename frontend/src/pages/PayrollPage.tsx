import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { payrollApi, exportApi } from '../api/apiClient';
import type { Payroll, PayrollSnapshot } from '../types/Payroll';
import PayrollEntryModal from '../components/PayrollEntryModal';

interface Props {
  companyId: string | null;
}

const monthNames = ['', 'Януари', 'Февруари', 'Март', 'Април', 'Май', 'Юни',
  'Юли', 'Август', 'Септември', 'Октомври', 'Ноември', 'Декември'];

const statusLabels: Record<string, string> = {
  OPEN: 'Отворен', CALCULATED: 'Изчислен', CLOSED: 'Затворен',
};

const fmt = (n: number | null | undefined) =>
  n != null ? n.toFixed(2) : '0.00';

export default function PayrollPage({ companyId }: Props) {
  const now = new Date();
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [payroll, setPayroll] = useState<Payroll | null>(null);
  const [snapshots, setSnapshots] = useState<PayrollSnapshot[]>([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [modalIndex, setModalIndex] = useState<number | null>(null);
  const navigate = useNavigate();

  const load = async () => {
    if (!companyId) return;
    try {
      const p = await payrollApi.getPayroll(companyId, year, month);
      setPayroll(p);
      const s = await payrollApi.getSnapshots(companyId, year, month);
      setSnapshots(s);
    } catch { /* empty month */ }
  };

  useEffect(() => { load(); }, [companyId, year, month]);

  const handleCalculate = async () => {
    if (!companyId) return;
    setLoading(true);
    setMessage('');
    try {
      const result = await payrollApi.calculateAll(companyId, year, month);
      setSnapshots(result);
      setMessage(`Изчислени ${result.length} служител(и).`);
      await load();
    } catch (e: any) {
      setMessage(`Грешка: ${e.message}`);
    }
    setLoading(false);
  };

  const handleClose = async () => {
    if (!companyId) return;
    setLoading(true);
    try {
      await payrollApi.closeMonth(companyId, year, month);
      setMessage('Месецът е затворен.');
      await load();
    } catch (e: any) {
      setMessage(`Грешка: ${e.message}`);
    }
    setLoading(false);
  };

  const handleReopen = async () => {
    if (!companyId) return;
    setLoading(true);
    try {
      await payrollApi.reopenMonth(companyId, year, month);
      setMessage('Месецът е отворен отново.');
      await load();
    } catch (e: any) {
      setMessage(`Грешка: ${e.message}`);
    }
    setLoading(false);
  };

  const handleStartNew = async () => {
    if (!companyId) return;
    setLoading(true);
    setMessage('');
    try {
      await payrollApi.startNewMonth(companyId, year, month);
      setMessage('Месецът е подготвен успешно.');
      await load();
    } catch (e: any) {
      setMessage(`Грешка: ${e.message}`);
    }
    setLoading(false);
  };

  // Totals
  const totalGross = snapshots.reduce((s, e) => s + (e.grossSalary || 0), 0);
  const totalIns = snapshots.reduce((s, e) => s + (e.totalEmployeeInsurance || 0), 0);
  const totalTax = snapshots.reduce((s, e) => s + (e.incomeTax || 0), 0);
  const totalNet = snapshots.reduce((s, e) => s + (e.netSalary || 0), 0);

  if (!companyId) return <div className="page"><p>Моля, изберете фирма.</p></div>;

  return (
    <div className="page" style={{ maxWidth: 1200 }}>
      <h1>Месечна ведомост</h1>

      <div className="toolbar">
        <div className="toolbar-right">
          <select value={month} onChange={e => setMonth(Number(e.target.value))}>
            {monthNames.slice(1).map((name, i) => (
              <option key={i + 1} value={i + 1}>{name}</option>
            ))}
          </select>
          <input type="number" value={year} style={{ width: 80 }}
            onChange={e => setYear(Number(e.target.value))} />
          {payroll && (
            <span className={`status-badge ${payroll.status === 'CLOSED' ? 'inactive' : 'active'}`}>
              {statusLabels[payroll.status] || payroll.status}
            </span>
          )}
        </div>

        <div style={{ display: 'flex', gap: '0.5rem' }}>
          {(!payroll || (payroll.status === 'OPEN' && snapshots.length === 0)) && (
            <button onClick={handleStartNew} disabled={loading} style={{ background: '#0288d1' }}>
              Подготви месец
            </button>
          )}
          {payroll?.status !== 'CLOSED' && (
            <button onClick={handleCalculate} disabled={loading}>
              {loading ? 'Изчисляване...' : 'Изчисли заплати'}
            </button>
          )}
          {payroll?.status === 'CALCULATED' && (
            <button onClick={handleClose} disabled={loading} style={{ background: '#2e7d32' }}>
              Затвори месец
            </button>
          )}
          {payroll?.status === 'CLOSED' && (
            <>
              <button onClick={handleReopen} disabled={loading} className="btn-danger">
                Отвори месец
              </button>
              <button onClick={async () => {
                if (!companyId || !confirm('Преизчисляване на затворен месец?')) return;
                setLoading(true);
                try {
                  const result = await payrollApi.recalculateMonth(companyId, year, month);
                  setSnapshots(result);
                  setMessage(`Преизчислени ${result.length} служител(и).`);
                  await load();
                } catch (e: any) { setMessage(`Грешка: ${e.message}`); }
                setLoading(false);
              }} disabled={loading} style={{ background: '#e65100' }}>
                Преизчисли
              </button>
            </>
          )}
          {snapshots.length > 0 && (
            <button onClick={async () => {
              if (!companyId) return;
              const blob = await exportApi.payrollPdf(companyId, year, month);
              const url = window.URL.createObjectURL(blob);
              const a = document.createElement('a');
              a.href = url;
              a.download = `payroll_${year}_${String(month).padStart(2, '0')}.pdf`;
              document.body.appendChild(a);
              a.click();
              window.URL.revokeObjectURL(url);
            }} style={{ background: '#6a1b9a' }}>
              PDF Фишове
            </button>
          )}
        </div>
      </div>

      {message && <p className="message">{message}</p>}

      <table className="data-table">
        <thead>
          <tr>
            <th>Служител</th>
            <th>ЕГН</th>
            <th>Длъжност</th>
            <th style={{ textAlign: 'right' }}>Брутно (EUR)</th>
            <th style={{ textAlign: 'right' }}>Осигуровки</th>
            <th style={{ textAlign: 'right' }}>ДОД</th>
            <th style={{ textAlign: 'right' }}>Нето (EUR)</th>
          </tr>
        </thead>
        <tbody>
          {snapshots.map((s, idx) => (
            <tr key={s.id} className="clickable-row"
              onClick={() => setModalIndex(idx)}>
              <td>{s.employeeData?.fullName}</td>
              <td>{s.employeeData?.egn}</td>
              <td>{s.employeeData?.jobTitle}</td>
              <td style={{ textAlign: 'right' }}>{fmt(s.grossSalary)}</td>
              <td style={{ textAlign: 'right' }}>{fmt(s.totalEmployeeInsurance)}</td>
              <td style={{ textAlign: 'right' }}>{fmt(s.incomeTax)}</td>
              <td style={{ textAlign: 'right', fontWeight: 600 }}>{fmt(s.netSalary)}</td>
            </tr>
          ))}
          {snapshots.length === 0 && <tr><td colSpan={7}>Няма изчислени заплати.</td></tr>}
        </tbody>
        {snapshots.length > 0 && (
          <tfoot>
            <tr style={{ fontWeight: 700, background: '#f5f5f5' }}>
              <td colSpan={3}>Общо ({snapshots.length} служители)</td>
              <td style={{ textAlign: 'right' }}>{fmt(totalGross)}</td>
              <td style={{ textAlign: 'right' }}>{fmt(totalIns)}</td>
              <td style={{ textAlign: 'right' }}>{fmt(totalTax)}</td>
              <td style={{ textAlign: 'right' }}>{fmt(totalNet)}</td>
            </tr>
          </tfoot>
        )}
      </table>
      {modalIndex != null && companyId && (
        <PayrollEntryModal
          companyId={companyId}
          year={year}
          month={month}
          employees={snapshots.map(s => ({
            id: s.employeeId,
            name: s.employeeData?.fullName || '',
            egn: s.employeeData?.egn || '',
            jobTitle: s.employeeData?.jobTitle || '',
          }))}
          initialIndex={modalIndex}
          onClose={() => setModalIndex(null)}
          onCalculated={load}
        />
      )}
    </div>
  );
}
