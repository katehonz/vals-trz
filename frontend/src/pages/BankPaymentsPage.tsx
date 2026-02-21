import { useState } from 'react';
import { bankPaymentApi } from '../api/apiClient';

interface Props {
  companyId: string | null;
}

interface PaymentRecord {
  employeeId: string;
  employeeName: string;
  iban: string;
  bic: string;
  amount: number;
  description: string;
  warnings: string[];
}

interface PaymentFileResult {
  fileName: string;
  fileContent: string;
  recordCount: number;
  totalAmount: number;
  records: PaymentRecord[];
}

export default function BankPaymentsPage({ companyId }: Props) {
  const now = new Date();
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [records, setRecords] = useState<PaymentRecord[]>([]);
  const [result, setResult] = useState<PaymentFileResult | null>(null);
  const [loading, setLoading] = useState(false);

  const handlePreview = async () => {
    if (!companyId) return;
    setLoading(true);
    setResult(null);
    try {
      const data = await bankPaymentApi.preview(companyId, year, month);
      setRecords(data);
    } finally {
      setLoading(false);
    }
  };

  const handleGenerate = async () => {
    if (!companyId) return;
    setLoading(true);
    try {
      const data = await bankPaymentApi.generate(companyId, year, month);
      setResult(data);
    } finally {
      setLoading(false);
    }
  };

  const handleDownload = async () => {
    if (!companyId) return;
    try {
      const blob = await bankPaymentApi.download(companyId, year, month);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = result?.fileName || `SALARY_${year}_${String(month).padStart(2, '0')}.CSV`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
    } catch {
      alert('Грешка при сваляне на файла');
    }
  };

  if (!companyId) return <div className="page"><p>Моля, изберете фирма.</p></div>;

  const totalAmount = records.reduce((sum, r) => sum + r.amount, 0);
  const withWarnings = records.filter((r) => r.warnings.length > 0);

  return (
    <div className="page">
      <h1>Банкови плащания</h1>

      <div className="toolbar">
        <label>Година:
          <select value={year} onChange={(e) => setYear(parseInt(e.target.value))}>
            {[year - 1, year, year + 1].map((y) => (
              <option key={y} value={y}>{y}</option>
            ))}
          </select>
        </label>
        <label>Месец:
          <select value={month} onChange={(e) => setMonth(parseInt(e.target.value))}>
            {Array.from({ length: 12 }, (_, i) => (
              <option key={i + 1} value={i + 1}>{String(i + 1).padStart(2, '0')}</option>
            ))}
          </select>
        </label>
        <button onClick={handlePreview} disabled={loading}>Преглед</button>
        <button onClick={handleGenerate} disabled={loading || records.length === 0}>Генерирай файл</button>
        {result && <button onClick={handleDownload}>Свали CSV</button>}
      </div>

      {withWarnings.length > 0 && (
        <div className="warning-box">
          <strong>Предупреждения:</strong>
          <ul>
            {withWarnings.map((r) => (
              <li key={r.employeeId}>{r.employeeName}: {r.warnings.join(', ')}</li>
            ))}
          </ul>
        </div>
      )}

      {result && (
        <div className="info-box">
          Генериран файл: <strong>{result.fileName}</strong> | Записи: {result.recordCount} | Обща сума: {result.totalAmount.toFixed(2)} лв.
        </div>
      )}

      <table className="data-table">
        <thead>
          <tr>
            <th>Служител</th>
            <th>IBAN</th>
            <th>BIC</th>
            <th>Сума</th>
            <th>Основание</th>
            <th>Статус</th>
          </tr>
        </thead>
        <tbody>
          {records.map((r) => (
            <tr key={r.employeeId} className={r.warnings.length > 0 ? 'row-warning' : ''}>
              <td>{r.employeeName}</td>
              <td>{r.iban || '—'}</td>
              <td>{r.bic || '—'}</td>
              <td className="text-right">{r.amount.toFixed(2)}</td>
              <td>{r.description}</td>
              <td>{r.warnings.length > 0 ? r.warnings.join(', ') : 'OK'}</td>
            </tr>
          ))}
          {records.length === 0 && <tr><td colSpan={6}>Натиснете "Преглед" за визуализация.</td></tr>}
        </tbody>
        {records.length > 0 && (
          <tfoot>
            <tr>
              <td colSpan={3}><strong>Общо</strong></td>
              <td className="text-right"><strong>{totalAmount.toFixed(2)}</strong></td>
              <td colSpan={2}></td>
            </tr>
          </tfoot>
        )}
      </table>
    </div>
  );
}
