import { useEffect, useState, useCallback } from 'react';
import { payrollApi } from '../api/apiClient';
import type { PayrollSnapshot, PayrollLine } from '../types/Payroll';

interface EmployeeRef {
  id: string;
  name: string;
  egn: string;
  jobTitle: string;
}

interface Props {
  companyId: string;
  year: number;
  month: number;
  employees: EmployeeRef[];
  initialIndex: number;
  onClose: () => void;
  onCalculated: () => void;
}

const monthNames = ['', 'Януари', 'Февруари', 'Март', 'Април', 'Май', 'Юни',
  'Юли', 'Август', 'Септември', 'Октомври', 'Ноември', 'Декември'];

const fmt = (n: number | null | undefined) =>
  n != null ? n.toLocaleString('bg-BG', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '0,00';

function LineItems({ lines, color }: { lines: PayrollLine[]; color?: string }) {
  if (!lines || lines.length === 0) return <div style={{ color: '#999', fontSize: '0.82rem', padding: '0.5rem' }}>Няма данни</div>;
  return (
    <>
      {lines.map((l, i) => (
        <div key={i} className="line-item">
          <span className="line-code">{l.code}</span>
          <span className="line-name">
            {l.name}
            {l.rate != null && l.rate > 0 && (
              <span className="line-detail">({l.rate}%)</span>
            )}
            {l.quantity != null && l.quantity > 0 && l.type !== 'PERCENT' && (
              <span className="line-detail">[{l.quantity}]</span>
            )}
          </span>
          <span className="line-amount" style={{ color: color || '#333' }}>{fmt(l.amount)}</span>
        </div>
      ))}
    </>
  );
}

export default function PayrollEntryModal({ companyId, year, month, employees, initialIndex, onClose, onCalculated }: Props) {
  const [index, setIndex] = useState(initialIndex);
  const [snapshot, setSnapshot] = useState<PayrollSnapshot | null>(null);
  const [loading, setLoading] = useState(false);
  const [calculating, setCalculating] = useState(false);
  const [error, setError] = useState('');

  const emp = employees[index];
  const total = employees.length;

  const loadSnapshot = useCallback(async (employeeId: string) => {
    setLoading(true);
    setError('');
    try {
      const s = await payrollApi.getEmployeeSnapshot(companyId, employeeId, year, month);
      setSnapshot(s);
    } catch {
      setSnapshot(null);
    }
    setLoading(false);
  }, [companyId, year, month]);

  useEffect(() => {
    if (emp) loadSnapshot(emp.id);
  }, [emp, loadSnapshot]);

  const handleCalculate = async () => {
    if (!emp) return;
    setCalculating(true);
    setError('');
    try {
      const s = await payrollApi.calculateEmployee(companyId, emp.id, year, month);
      setSnapshot(s);
      onCalculated();
    } catch (e: any) {
      setError(`Грешка: ${e.message}`);
    }
    setCalculating(false);
  };

  const goPrev = useCallback(() => {
    if (index > 0) setIndex(index - 1);
  }, [index]);

  const goNext = useCallback(() => {
    if (index < total - 1) setIndex(index + 1);
  }, [index, total]);

  // Keyboard navigation
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
      if (e.key === 'ArrowLeft') goPrev();
      if (e.key === 'ArrowRight') goNext();
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [onClose, goPrev, goNext]);

  if (!emp) return null;

  const s = snapshot;
  const empData = s?.employeeData || {};
  const earnings = s?.earnings || [];
  const deductions = s?.deductions || [];
  const employerContribs = s?.employerContributions || [];

  // Split deductions: insurance vs other
  const insuranceCodes = new Set(['221','222','223','224','231','232','233','234','255','256','261','262','281','282','201','202','203','500','505']);
  const insuranceDeductions = deductions.filter(d => insuranceCodes.has(d.code));
  const otherDeductions = deductions.filter(d => !insuranceCodes.has(d.code));

  const earningsTotal = earnings.reduce((sum, l) => sum + (l.amount || 0), 0);
  const deductionsTotal = deductions.reduce((sum, l) => sum + (l.amount || 0), 0);

  const isNetNegative = s != null && (s.netSalary || 0) < 0;
  const isZeroGross = s != null && (s.grossSalary || 0) === 0;
  const hasNoTimesheet = s != null && !s.timesheetData;

  return (
    <div className="payroll-modal-overlay" onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="payroll-modal">
        {/* Header */}
        <div className="payroll-modal-header">
          <button className="nav-btn" onClick={goPrev} disabled={index === 0}>
            &#9664; Назад
          </button>
          <div className="emp-info">
            <div className="emp-name">
              {empData.fullName || emp.name} &mdash; {monthNames[month]} {year}
            </div>
            <div className="emp-details">
              ({index + 1}/{total}) | ЕГН: {empData.egn || emp.egn} | {empData.jobTitle || emp.jobTitle}
              {empData.baseSalary != null && <> | Заплата: {fmt(empData.baseSalary)} EUR</>}
              {empData.seniorityBonusPercent != null && empData.seniorityBonusPercent > 0 && <> | Клас: {empData.seniorityBonusPercent}%</>}
            </div>
          </div>
          <button className="nav-btn" onClick={goNext} disabled={index === total - 1}>
            Напред &#9654;
          </button>
          <button className="close-btn" onClick={onClose} title="Затвори (Esc)">&times;</button>
        </div>

        {/* Body */}
        <div className="payroll-modal-body">
          {loading ? (
            <div style={{ gridColumn: '1 / -1', textAlign: 'center', padding: '3rem', color: '#999' }}>
              Зареждане...
            </div>
          ) : !s ? (
            <div style={{ gridColumn: '1 / -1', textAlign: 'center', padding: '3rem' }}>
              <p style={{ color: '#999', marginBottom: '1rem' }}>Няма изчислена заплата за този служител.</p>
              <button className="calc-btn" onClick={handleCalculate} disabled={calculating}
                style={{ padding: '0.6rem 2rem', background: '#1a237e', color: 'white', border: 'none', borderRadius: '4px', fontSize: '1rem' }}>
                {calculating ? 'Изчисляване...' : 'Изчисли заплата'}
              </button>
              {error && <p style={{ color: '#c62828', marginTop: '0.5rem' }}>{error}</p>}
            </div>
          ) : (
            <>
              {/* Left column: Начисления */}
              <div className="payroll-col">
                {hasNoTimesheet && (
                  <div className="payroll-warning">Липсва присъствена форма за този месец!</div>
                )}
                {isZeroGross && (
                  <div className="payroll-warning">Брутното възнаграждение е 0.00 EUR!</div>
                )}
                <h3>Начисления</h3>
                <LineItems lines={earnings} />
                <div className="line-total">
                  <span>Брутно</span>
                  <span>{fmt(earningsTotal)} EUR</span>
                </div>

                {employerContribs.length > 0 && (
                  <div className="payroll-col-sub">
                    <h4>Осигуровки работодател</h4>
                    <LineItems lines={employerContribs} color="#666" />
                    <div className="line-total" style={{ background: '#f0f0f0' }}>
                      <span>Общо работодател</span>
                      <span>{fmt(s.totalEmployerInsurance)} EUR</span>
                    </div>
                  </div>
                )}
              </div>

              {/* Right column: Удръжки */}
              <div className="payroll-col">
                <h3>Удръжки</h3>
                {insuranceDeductions.length > 0 && (
                  <>
                    <LineItems lines={insuranceDeductions} color="#c62828" />
                    {otherDeductions.length > 0 && <hr className="section-divider" />}
                  </>
                )}
                {otherDeductions.length > 0 && (
                  <LineItems lines={otherDeductions} color="#e65100" />
                )}
                <div className="line-total">
                  <span>Общо удръжки</span>
                  <span style={{ color: '#c62828' }}>{fmt(deductionsTotal)} EUR</span>
                </div>
              </div>
            </>
          )}
        </div>

        {/* Footer */}
        <div className="payroll-modal-footer">
          {s ? (
            <>
              <div className="summary-items">
                <div className="summary-item">
                  <div className="label">Брутно</div>
                  <div className="value">{fmt(s.grossSalary)}</div>
                </div>
                <div className="summary-item">
                  <div className="label">Осиг. доход</div>
                  <div className="value">{fmt(s.insurableIncome)}</div>
                </div>
                <div className="summary-item">
                  <div className="label">Осигуровки</div>
                  <div className="value">{fmt(s.totalEmployeeInsurance)}</div>
                </div>
                <div className="summary-item">
                  <div className="label">Дан. основа</div>
                  <div className="value">{fmt(s.taxBase)}</div>
                </div>
                <div className="summary-item">
                  <div className="label">ДОД</div>
                  <div className="value">{fmt(s.incomeTax)}</div>
                </div>
              </div>
              <div className={`net-box ${isNetNegative ? 'warning' : ''}`}>
                <div className="label">Нето за получаване</div>
                <div className="value">{fmt(s.netSalary)} EUR</div>
              </div>
              <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                <button className="calc-btn" onClick={handleCalculate} disabled={calculating}>
                  {calculating ? 'Изчисляване...' : 'Преизчисли'}
                </button>
                <div className="summary-item" style={{ marginLeft: '0.5rem' }}>
                  <div className="label">Разход раб.</div>
                  <div className="value">{fmt(s.totalEmployerCost)}</div>
                </div>
              </div>
            </>
          ) : (
            <div style={{ color: '#999' }}>Натиснете "Изчисли заплата" за да видите резултат.</div>
          )}
        </div>
      </div>
    </div>
  );
}
