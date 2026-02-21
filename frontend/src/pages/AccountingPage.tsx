import { useEffect, useState } from 'react';
import { accountingApi } from '../api/apiClient';

interface Props {
  companyId: string | null;
}

interface AccountingEntry {
  id: string;
  accountCode: string;
  accountName: string;
  debit: number | null;
  credit: number | null;
  description: string;
  category: string;
}

const fmt = (n: number | null | undefined) =>
  n != null && n !== 0 ? n.toFixed(2) : '';

const monthNames = ['', 'Януари', 'Февруари', 'Март', 'Април', 'Май', 'Юни',
  'Юли', 'Август', 'Септември', 'Октомври', 'Ноември', 'Декември'];

const categoryLabels: Record<string, string> = {
  SALARY: 'Заплати',
  INSURANCE_EMPLOYER: 'Осиг. работодател',
  INSURANCE_EMPLOYEE: 'Осиг. работник',
  TAX: 'ДОД',
  NET_PAY: 'Нето',
};

export default function AccountingPage({ companyId }: Props) {
  const now = new Date();
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [entries, setEntries] = useState<AccountingEntry[]>([]);

  const load = async () => {
    if (!companyId) return;
    const data = await accountingApi.getEntries(companyId, year, month);
    setEntries(data);
  };

  useEffect(() => { load(); }, [companyId, year, month]);

  const handleGenerate = async () => {
    if (!companyId) return;
    const data = await accountingApi.generate(companyId, year, month);
    setEntries(data);
  };

  if (!companyId) return <div className="page"><p>Моля, изберете фирма.</p></div>;

  const totalDebit = entries.reduce((s, e) => s + (e.debit || 0), 0);
  const totalCredit = entries.reduce((s, e) => s + (e.credit || 0), 0);

  return (
    <div className="page">
      <h1>Счетоводни операции — {monthNames[month]} {year}</h1>

      <div className="toolbar">
        <select value={month} onChange={e => setMonth(Number(e.target.value))}>
          {monthNames.slice(1).map((name, i) => (
            <option key={i + 1} value={i + 1}>{name}</option>
          ))}
        </select>
        <input type="number" value={year} style={{ width: 80 }}
          onChange={e => setYear(Number(e.target.value))} />
        <button className="btn-success" onClick={handleGenerate}>
          Генерирай контировки
        </button>
      </div>

      <table className="data-table">
        <thead>
          <tr>
            <th>Операция</th>
            <th>Сметка</th>
            <th>Наименование</th>
            <th style={{ textAlign: 'right' }}>Дебит</th>
            <th style={{ textAlign: 'right' }}>Кредит</th>
            <th>Описание</th>
          </tr>
        </thead>
        <tbody>
          {entries.map((e, i) => (
            <tr key={e.id || i}>
              <td>{categoryLabels[e.category] || e.category}</td>
              <td><strong>{e.accountCode}</strong></td>
              <td>{e.accountName}</td>
              <td style={{ textAlign: 'right' }}>{fmt(e.debit)}</td>
              <td style={{ textAlign: 'right' }}>{fmt(e.credit)}</td>
              <td>{e.description}</td>
            </tr>
          ))}
          {entries.length === 0 && <tr><td colSpan={6}>Няма контировки. Натиснете "Генерирай контировки".</td></tr>}
        </tbody>
        {entries.length > 0 && (
          <tfoot>
            <tr style={{ fontWeight: 700, background: '#f5f5f5' }}>
              <td colSpan={3}>Общо</td>
              <td style={{ textAlign: 'right' }}>{totalDebit.toFixed(2)}</td>
              <td style={{ textAlign: 'right' }}>{totalCredit.toFixed(2)}</td>
              <td>{Math.abs(totalDebit - totalCredit) < 0.01 ? 'Балансирано' : 'ДИСБАЛАНС!'}</td>
            </tr>
          </tfoot>
        )}
      </table>
    </div>
  );
}
