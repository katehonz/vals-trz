import { useEffect, useState } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { payrollApi } from '../api/apiClient';
import type { PayrollSnapshot, PayrollLine } from '../types/Payroll';

interface Props {
  companyId: string | null;
}

const fmt = (n: number | null | undefined) =>
  n != null ? n.toFixed(2) : '—';

function LinesTable({ lines, title }: { lines: PayrollLine[]; title: string }) {
  if (!lines || lines.length === 0) return null;
  const total = lines.reduce((s, l) => s + (l.amount || 0), 0);
  return (
    <div className="form-section">
      <h3>{title}</h3>
      <table className="data-table">
        <thead>
          <tr>
            <th>Код</th>
            <th>Наименование</th>
            <th style={{ textAlign: 'right' }}>База</th>
            <th style={{ textAlign: 'right' }}>%/Коеф.</th>
            <th style={{ textAlign: 'right' }}>Кол-во</th>
            <th style={{ textAlign: 'right' }}>Сума (EUR)</th>
          </tr>
        </thead>
        <tbody>
          {lines.map((l, i) => (
            <tr key={i}>
              <td>{l.code}</td>
              <td>{l.name}</td>
              <td style={{ textAlign: 'right' }}>{l.base != null ? fmt(l.base) : '—'}</td>
              <td style={{ textAlign: 'right' }}>{l.rate != null ? l.rate : '—'}</td>
              <td style={{ textAlign: 'right' }}>{l.quantity != null ? l.quantity : '—'}</td>
              <td style={{ textAlign: 'right', fontWeight: 600 }}>{fmt(l.amount)}</td>
            </tr>
          ))}
        </tbody>
        <tfoot>
          <tr style={{ fontWeight: 700, background: '#f5f5f5' }}>
            <td colSpan={5}>Общо</td>
            <td style={{ textAlign: 'right' }}>{fmt(total)}</td>
          </tr>
        </tfoot>
      </table>
    </div>
  );
}

export default function SalarySlipPage({ companyId }: Props) {
  const { employeeId } = useParams();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const year = Number(searchParams.get('year')) || new Date().getFullYear();
  const month = Number(searchParams.get('month')) || new Date().getMonth() + 1;

  const [snapshot, setSnapshot] = useState<PayrollSnapshot | null>(null);

  useEffect(() => {
    if (!companyId || !employeeId) return;
    payrollApi.getEmployeeSnapshot(companyId, employeeId, year, month)
      .then(setSnapshot)
      .catch(() => setSnapshot(null));
  }, [companyId, employeeId, year, month]);

  if (!companyId) return <div className="page"><p>Моля, изберете фирма.</p></div>;

  if (!snapshot) return (
    <div className="page">
      <button className="btn-back" onClick={() => navigate('/payroll')}>&larr; Назад</button>
      <p>Няма данни.</p>
    </div>
  );

  const empData = snapshot.employeeData || {};
  const monthNames = ['', 'Януари', 'Февруари', 'Март', 'Април', 'Май', 'Юни',
    'Юли', 'Август', 'Септември', 'Октомври', 'Ноември', 'Декември'];

  return (
    <div className="page salary-slip">
      <div className="page-header-row no-print">
        <button className="btn-back" onClick={() => navigate('/payroll')}>&larr; Назад</button>
        <h1>Фиш за заплата — {monthNames[month]} {year}</h1>
        <button className="btn-success" onClick={() => window.print()} style={{ marginLeft: 'auto' }}>🖨 Печат</button>
      </div>
      <h1 className="print-only" style={{ textAlign: 'center', marginBottom: 16 }}>Фиш за заплата — {monthNames[month]} {year}</h1>

      <div className="slip-header">
        <div><strong>{empData.fullName}</strong></div>
        <div>ЕГН: {empData.egn} | Длъжност: {empData.jobTitle} | НКПД: {empData.nkpdCode}</div>
        <div>Осн. заплата: {fmt(empData.baseSalary)} EUR | ДТВ ТСПО: {empData.seniorityBonusPercent}%</div>
      </div>

      <LinesTable lines={snapshot.earnings} title="Начисления" />
      <LinesTable lines={snapshot.deductions} title="Удръжки" />
      <LinesTable lines={snapshot.employerContributions} title="Осигуровки работодател" />

      <div className="slip-summary">
        <table className="data-table">
          <tbody>
            <tr><td>Брутно възнаграждение</td><td style={{ textAlign: 'right', fontWeight: 600 }}>{fmt(snapshot.grossSalary)} EUR</td></tr>
            <tr><td>Осигурителен доход</td><td style={{ textAlign: 'right' }}>{fmt(snapshot.insurableIncome)} EUR</td></tr>
            <tr><td>Осигуровки работник</td><td style={{ textAlign: 'right' }}>{fmt(snapshot.totalEmployeeInsurance)} EUR</td></tr>
            <tr><td>Данъчна основа</td><td style={{ textAlign: 'right' }}>{fmt(snapshot.taxBase)} EUR</td></tr>
            <tr><td>Данък общ доход</td><td style={{ textAlign: 'right' }}>{fmt(snapshot.incomeTax)} EUR</td></tr>
            <tr style={{ fontSize: '1.1rem', fontWeight: 700, background: '#e8f5e9' }}>
              <td>Нето за получаване</td><td style={{ textAlign: 'right' }}>{fmt(snapshot.netSalary)} EUR</td>
            </tr>
            <tr><td colSpan={2} style={{ height: 8, border: 'none' }} /></tr>
            <tr><td>Осигуровки работодател</td><td style={{ textAlign: 'right' }}>{fmt(snapshot.totalEmployerInsurance)} EUR</td></tr>
            <tr><td>Общ разход за работодателя</td><td style={{ textAlign: 'right', fontWeight: 600 }}>{fmt(snapshot.totalEmployerCost)} EUR</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  );
}
