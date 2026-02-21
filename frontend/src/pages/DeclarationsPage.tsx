import { useEffect, useState } from 'react';
import { declarationApi } from '../api/apiClient';
import type { Declaration1Record, Declaration6Data, Article62Record, NapSubmission, ValidationError } from '../types/Declaration';

interface Props {
  companyId: string | null;
}

type Tab = 'd1' | 'd6' | 'art62' | 'art123' | 'art73' | 'history';

const fmt = (n: number | null | undefined) =>
  n != null ? n.toFixed(2) : '0.00';

const monthNames = ['', 'Януари', 'Февруари', 'Март', 'Април', 'Май', 'Юни',
  'Юли', 'Август', 'Септември', 'Октомври', 'Ноември', 'Декември'];

const typeLabels: Record<string, string> = {
  D1: 'Декл. 1',
  D6_INS: 'Декл. 6 (осиг.)',
  D6_TAX: 'Декл. 6 (данък)',
  ART62: 'Чл. 62',
  ART123: 'Чл. 123',
  ART73: 'Чл. 73 ЗДДФЛ',
};

const changeTypes = [
  { value: 1, label: 'Сливане' },
  { value: 2, label: 'Вливане' },
  { value: 3, label: 'Разделяне' },
  { value: 4, label: 'Отделяне' },
  { value: 5, label: 'Преотстъпване/прехвърляне на дейност' },
  { value: 6, label: 'Промяна на правноорг. форма' },
];

const correctionLabels = [
  { value: 0, label: 'Редовна' },
  { value: 1, label: 'Коригираща' },
  { value: 8, label: 'Заличаваща' },
];

export default function DeclarationsPage({ companyId }: Props) {
  const now = new Date();
  const [tab, setTab] = useState<Tab>('d1');
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [correctionCode, setCorrectionCode] = useState(0);

  // D1
  const [d1Records, setD1Records] = useState<Declaration1Record[]>([]);
  const [d1Errors, setD1Errors] = useState<ValidationError[]>([]);

  // D6
  const [d6Data, setD6Data] = useState<Declaration6Data | null>(null);

  // Art 62
  const [fromDate, setFromDate] = useState(new Date(now.getFullYear(), now.getMonth(), 1).toISOString().slice(0, 10));
  const [toDate, setToDate] = useState(now.toISOString().slice(0, 10));
  const [art62Records, setArt62Records] = useState<Article62Record[]>([]);

  // Art 123
  const [art123ChangeType, setArt123ChangeType] = useState(1);
  const [art123NewBulstat, setArt123NewBulstat] = useState('');
  const [art123NewName] = useState('');
  const [art123Date, setArt123Date] = useState(now.toISOString().slice(0, 10));
  const [art123Records, setArt123Records] = useState<any[]>([]);

  // Art 73
  const [art73Year, setArt73Year] = useState(now.getFullYear() - 1);
  const [art73Data, setArt73Data] = useState<any>(null);

  // History
  const [submissions, setSubmissions] = useState<NapSubmission[]>([]);

  // Status
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');

  // Load data on tab/params change
  useEffect(() => {
    if (!companyId) return;
    setMessage('');
    if (tab === 'history') loadHistory();
  }, [companyId, tab]);

  if (!companyId) return <div className="page"><p>Моля, изберете фирма.</p></div>;

  // --- Actions ---

  async function loadD1Preview() {
    setLoading(true); setMessage('');
    try {
      const data = await declarationApi.d1Preview(companyId!, year, month);
      setD1Records(data);
      setD1Errors([]);
      setMessage(`Заредени ${data.length} записа.`);
    } catch (e: any) { setMessage('Грешка: ' + e.message); }
    setLoading(false);
  }

  async function validateD1() {
    setLoading(true); setMessage('');
    try {
      const errors = await declarationApi.d1Validate(companyId!, year, month);
      setD1Errors(errors);
      setMessage(errors.length === 0 ? 'Няма грешки.' : `Намерени ${errors.length} грешки.`);
    } catch (e: any) { setMessage('Грешка: ' + e.message); }
    setLoading(false);
  }

  async function generateD1() {
    setLoading(true); setMessage('');
    try {
      const sub = await declarationApi.d1Generate(companyId!, year, month, correctionCode);
      setMessage(`Файл генериран: ${sub.fileName} (${sub.recordCount} записа)`);
      loadHistory();
    } catch (e: any) { setMessage('Грешка: ' + e.message); }
    setLoading(false);
  }

  async function loadD6Preview() {
    setLoading(true); setMessage('');
    try {
      const data = await declarationApi.d6Preview(companyId!, year, month);
      setD6Data(data);
      setMessage('Данните са заредени.');
    } catch (e: any) { setMessage('Грешка: ' + e.message); setD6Data(null); }
    setLoading(false);
  }

  async function generateD6() {
    setLoading(true); setMessage('');
    try {
      const sub = await declarationApi.d6Generate(companyId!, year, month);
      setMessage(`Файл генериран: ${sub.fileName}`);
      loadHistory();
    } catch (e: any) { setMessage('Грешка: ' + e.message); }
    setLoading(false);
  }

  async function loadArt62Preview() {
    setLoading(true); setMessage('');
    try {
      const data = await declarationApi.art62Preview(companyId!, fromDate, toDate);
      setArt62Records(data);
      setMessage(`Намерени ${data.length} уведомления.`);
    } catch (e: any) { setMessage('Грешка: ' + e.message); }
    setLoading(false);
  }

  async function generateArt62() {
    setLoading(true); setMessage('');
    try {
      const sub = await declarationApi.art62Generate(companyId!, fromDate, toDate);
      setMessage(`Файл генериран: ${sub.fileName} (${sub.recordCount} записа)`);
      loadHistory();
    } catch (e: any) { setMessage('Грешка: ' + e.message); }
    setLoading(false);
  }

  function art123RequestBody() {
    return { changeType: art123ChangeType, newEmployerBulstat: art123NewBulstat, newEmployerName: art123NewName, changeDate: art123Date, employeeIds: null };
  }

  async function loadArt123Preview() {
    setLoading(true); setMessage('');
    try {
      const data = await declarationApi.art123Preview(companyId!, art123RequestBody());
      setArt123Records(data);
      setMessage(`Намерени ${data.length} записа.`);
    } catch (e: any) { setMessage('Грешка: ' + e.message); }
    setLoading(false);
  }

  async function generateArt123() {
    setLoading(true); setMessage('');
    try {
      const sub = await declarationApi.art123Generate(companyId!, art123RequestBody());
      setMessage(`Файл генериран: ${sub.fileName} (${sub.recordCount} записа)`);
      loadHistory();
    } catch (e: any) { setMessage('Грешка: ' + e.message); }
    setLoading(false);
  }

  async function loadArt73Preview() {
    setLoading(true); setMessage('');
    try {
      const data = await declarationApi.art73Preview(companyId!, art73Year);
      setArt73Data(data);
      setMessage(`Заредени данни за ${data.totalEmployees} служители.`);
    } catch (e: any) { setMessage('Грешка: ' + e.message); }
    setLoading(false);
  }

  async function generateArt73() {
    setLoading(true); setMessage('');
    try {
      const sub = await declarationApi.art73Generate(companyId!, art73Year);
      setMessage(`Файл генериран: ${sub.fileName} (${sub.recordCount} записа)`);
      loadHistory();
    } catch (e: any) { setMessage('Грешка: ' + e.message); }
    setLoading(false);
  }

  async function loadHistory() {
    try {
      const data = await declarationApi.getSubmissions(companyId!);
      setSubmissions(data);
    } catch { setSubmissions([]); }
  }

  async function handleDownload(id: string, fileName: string) {
    try {
      const blob = await declarationApi.downloadSubmission(companyId!, id);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = fileName;
      a.click();
      window.URL.revokeObjectURL(url);
    } catch (e: any) { setMessage('Грешка при сваляне: ' + e.message); }
  }

  // --- Month/Year selector ---
  const MonthYearSelector = () => (
    <div className="toolbar-right">
      <select value={month} onChange={e => setMonth(Number(e.target.value))}>
        {monthNames.slice(1).map((name, i) => (
          <option key={i + 1} value={i + 1}>{name}</option>
        ))}
      </select>
      <input type="number" value={year} style={{ width: 80 }}
        onChange={e => setYear(Number(e.target.value))} />
    </div>
  );

  return (
    <div className="page" style={{ maxWidth: 1400 }}>
      <h1>Декларации НАП</h1>

      <div className="toolbar">
        <div className="tabs" style={{ marginBottom: 0, borderBottom: 'none' }}>
          <button className={tab === 'd1' ? 'active' : ''} onClick={() => setTab('d1')}>Декл. 1</button>
          <button className={tab === 'd6' ? 'active' : ''} onClick={() => setTab('d6')}>Декл. 6</button>
          <button className={tab === 'art62' ? 'active' : ''} onClick={() => setTab('art62')}>Чл. 62</button>
          <button className={tab === 'art123' ? 'active' : ''} onClick={() => setTab('art123')}>Чл. 123</button>
          <button className={tab === 'art73' ? 'active' : ''} onClick={() => setTab('art73')}>Чл. 73 ЗДДФЛ</button>
          <button className={tab === 'history' ? 'active' : ''} onClick={() => { setTab('history'); loadHistory(); }}>История</button>
        </div>
        {(tab === 'd1' || tab === 'd6') && <MonthYearSelector />}
        {tab === 'art73' && (
          <div className="toolbar-right">
            <input type="number" value={art73Year} style={{ width: 80 }}
              onChange={e => setArt73Year(Number(e.target.value))} />
          </div>
        )}
      </div>

      {message && <p style={{ padding: '8px 12px', background: '#f0f0f0', borderRadius: 4, margin: '8px 0' }}>{message}</p>}

      {/* === Декларация 1 === */}
      {tab === 'd1' && (
        <>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center', margin: '12px 0' }}>
            <button className="btn" onClick={loadD1Preview} disabled={loading}>Преглед</button>
            <button className="btn" onClick={validateD1} disabled={loading}>Валидирай</button>
            <select value={correctionCode} onChange={e => setCorrectionCode(Number(e.target.value))}>
              {correctionLabels.map(c => (
                <option key={c.value} value={c.value}>{c.label}</option>
              ))}
            </select>
            <button className="btn btn-primary" onClick={generateD1} disabled={loading}>Генерирай файл</button>
          </div>

          {d1Errors.length > 0 && (
            <div style={{ background: '#fff3cd', padding: 12, borderRadius: 4, marginBottom: 12 }}>
              <strong>Грешки при валидация:</strong>
              <ul style={{ margin: '4px 0', paddingLeft: 20 }}>
                {d1Errors.map((err, i) => (
                  <li key={i}>{err.employeeName}: {err.message}</li>
                ))}
              </ul>
            </div>
          )}

          <div style={{ overflowX: 'auto' }}>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Служител</th>
                  <th>ЕГН</th>
                  <th style={{ textAlign: 'right' }}>Дни</th>
                  <th style={{ textAlign: 'right' }}>Осиг. доход</th>
                  <th style={{ textAlign: 'right' }}>Брутно</th>
                  <th style={{ textAlign: 'right' }}>Облагаем</th>
                  <th style={{ textAlign: 'right' }}>ДОД</th>
                  <th style={{ textAlign: 'right' }}>Нето</th>
                  <th>Грешки</th>
                </tr>
              </thead>
              <tbody>
                {d1Records.map((r, i) => (
                  <tr key={i}>
                    <td>{r.employeeName}</td>
                    <td>{r.egn}</td>
                    <td style={{ textAlign: 'right' }}>{r.fields[19] || '0'}</td>
                    <td style={{ textAlign: 'right' }}>{r.fields[31] || '0.00'}</td>
                    <td style={{ textAlign: 'right' }}>{r.fields[45] || '0.00'}</td>
                    <td style={{ textAlign: 'right' }}>{r.fields[47] || '0.00'}</td>
                    <td style={{ textAlign: 'right' }}>{r.fields[48] || '0.00'}</td>
                    <td style={{ textAlign: 'right' }}>{r.fields[49] || '0.00'}</td>
                    <td style={{ color: r.validationErrors.length > 0 ? '#d32f2f' : '#4caf50' }}>
                      {r.validationErrors.length > 0 ? r.validationErrors.join('; ') : 'OK'}
                    </td>
                  </tr>
                ))}
                {d1Records.length === 0 && <tr><td colSpan={9}>Натиснете "Преглед" за зареждане на данни.</td></tr>}
              </tbody>
            </table>
          </div>
        </>
      )}

      {/* === Декларация 6 === */}
      {tab === 'd6' && (
        <>
          <div style={{ display: 'flex', gap: 8, margin: '12px 0' }}>
            <button className="btn" onClick={loadD6Preview} disabled={loading}>Преглед</button>
            <button className="btn btn-primary" onClick={generateD6} disabled={loading}>Генерирай файл</button>
          </div>

          {d6Data && (
            <table className="data-table" style={{ maxWidth: 700 }}>
              <thead>
                <tr>
                  <th>Фонд</th>
                  <th style={{ textAlign: 'right' }}>Работодател</th>
                  <th style={{ textAlign: 'right' }}>Работник</th>
                  <th style={{ textAlign: 'right' }}>Общо</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>Пенсии (ДОО)</td>
                  <td style={{ textAlign: 'right' }}>{fmt(d6Data.totalPensionEmployer)}</td>
                  <td style={{ textAlign: 'right' }}>{fmt(d6Data.totalPensionEmployee)}</td>
                  <td style={{ textAlign: 'right' }}>{fmt(d6Data.totalPensionEmployer + d6Data.totalPensionEmployee)}</td>
                </tr>
                <tr>
                  <td>ОЗМ</td>
                  <td style={{ textAlign: 'right' }}>{fmt(d6Data.totalSicknessEmployer)}</td>
                  <td style={{ textAlign: 'right' }}>{fmt(d6Data.totalSicknessEmployee)}</td>
                  <td style={{ textAlign: 'right' }}>{fmt(d6Data.totalSicknessEmployer + d6Data.totalSicknessEmployee)}</td>
                </tr>
                <tr>
                  <td>Безработица</td>
                  <td style={{ textAlign: 'right' }}>{fmt(d6Data.totalUnemploymentEmployer)}</td>
                  <td style={{ textAlign: 'right' }}>{fmt(d6Data.totalUnemploymentEmployee)}</td>
                  <td style={{ textAlign: 'right' }}>{fmt(d6Data.totalUnemploymentEmployer + d6Data.totalUnemploymentEmployee)}</td>
                </tr>
                <tr>
                  <td>ДЗПО (УПФ)</td>
                  <td style={{ textAlign: 'right' }}>{fmt(d6Data.totalSupplementaryEmployer)}</td>
                  <td style={{ textAlign: 'right' }}>{fmt(d6Data.totalSupplementaryEmployee)}</td>
                  <td style={{ textAlign: 'right' }}>{fmt(d6Data.totalSupplementaryEmployer + d6Data.totalSupplementaryEmployee)}</td>
                </tr>
                <tr>
                  <td>Здравно осигуряване</td>
                  <td style={{ textAlign: 'right' }}>{fmt(d6Data.totalHealthEmployer)}</td>
                  <td style={{ textAlign: 'right' }}>{fmt(d6Data.totalHealthEmployee)}</td>
                  <td style={{ textAlign: 'right' }}>{fmt(d6Data.totalHealthEmployer + d6Data.totalHealthEmployee)}</td>
                </tr>
                <tr>
                  <td>ТЗПБ</td>
                  <td style={{ textAlign: 'right' }}>{fmt(d6Data.totalWorkAccident)}</td>
                  <td style={{ textAlign: 'right' }}>-</td>
                  <td style={{ textAlign: 'right' }}>{fmt(d6Data.totalWorkAccident)}</td>
                </tr>
                <tr>
                  <td>ДОД (данък)</td>
                  <td style={{ textAlign: 'right' }}>-</td>
                  <td style={{ textAlign: 'right' }}>{fmt(d6Data.totalIncomeTax)}</td>
                  <td style={{ textAlign: 'right' }}>{fmt(d6Data.totalIncomeTax)}</td>
                </tr>
              </tbody>
              <tfoot>
                <tr style={{ fontWeight: 700, background: '#f5f5f5' }}>
                  <td>Общо осигуровки</td>
                  <td colSpan={2}></td>
                  <td style={{ textAlign: 'right' }}>{fmt(d6Data.grandTotalInsurance)}</td>
                </tr>
                <tr style={{ fontWeight: 700 }}>
                  <td>Общо данък</td>
                  <td colSpan={2}></td>
                  <td style={{ textAlign: 'right' }}>{fmt(d6Data.grandTotalTax)}</td>
                </tr>
                <tr>
                  <td colSpan={3}>Брой служители</td>
                  <td style={{ textAlign: 'right' }}>{d6Data.employeeCount}</td>
                </tr>
              </tfoot>
            </table>
          )}

          {!d6Data && <p>Натиснете "Преглед" за зареждане на данни.</p>}
        </>
      )}

      {/* === Чл. 62 === */}
      {tab === 'art62' && (
        <>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center', margin: '12px 0' }}>
            <label>От: <input type="date" value={fromDate} onChange={e => setFromDate(e.target.value)} /></label>
            <label>До: <input type="date" value={toDate} onChange={e => setToDate(e.target.value)} /></label>
            <button className="btn" onClick={loadArt62Preview} disabled={loading}>Преглед</button>
            <button className="btn btn-primary" onClick={generateArt62} disabled={loading}>Генерирай файл</button>
          </div>

          <table className="data-table">
            <thead>
              <tr>
                <th>Служител</th>
                <th>ЕГН</th>
                <th>Тип</th>
                <th>No ТД</th>
                <th>Дата</th>
                <th>Основание</th>
                <th>НКПД</th>
                <th>ЕКАТТЕ</th>
                <th>Край/Пр.</th>
              </tr>
            </thead>
            <tbody>
              {art62Records.map((r, i) => (
                <tr key={i}>
                  <td>{r.employeeName}</td>
                  <td>{r.egn}</td>
                  <td>{r.eventTypeName}</td>
                  <td>{r.fields[5]}</td>
                  <td>{r.fields[4]}</td>
                  <td>{r.fields[3]}</td>
                  <td>{r.fields[6]}</td>
                  <td>{r.fields[8]}</td>
                  <td>{r.eventType === '03' ? r.fields[11] : (r.fields[9] || '-')}</td>
                </tr>
              ))}
              {art62Records.length === 0 && <tr><td colSpan={9}>Натиснете "Преглед" за зареждане.</td></tr>}
            </tbody>
          </table>
        </>
      )}

      {/* === Чл. 123 === */}
      {tab === 'art123' && (
        <>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center', margin: '12px 0', flexWrap: 'wrap' }}>
            <label>Вид промяна:
              <select value={art123ChangeType} onChange={e => setArt123ChangeType(Number(e.target.value))}>
                {changeTypes.map(ct => (
                  <option key={ct.value} value={ct.value}>{ct.label}</option>
                ))}
              </select>
            </label>
            <label>ЕИК нов работодател:
              <input type="text" value={art123NewBulstat} onChange={e => setArt123NewBulstat(e.target.value)} style={{ width: 120 }} />
            </label>
            <label>Дата на промяна:
              <input type="date" value={art123Date} onChange={e => setArt123Date(e.target.value)} />
            </label>
          </div>
          <div style={{ display: 'flex', gap: 8, margin: '0 0 12px' }}>
            <button className="btn" onClick={loadArt123Preview} disabled={loading}>Преглед</button>
            <button className="btn btn-primary" onClick={generateArt123} disabled={loading || !art123NewBulstat}>Генерирай файл</button>
          </div>

          <table className="data-table">
            <thead>
              <tr>
                <th>Служител</th>
                <th>ЕГН</th>
                <th>Вид промяна</th>
                <th>Нов ЕИК</th>
                <th>Дата</th>
                <th>No ТД</th>
                <th>НКПД</th>
              </tr>
            </thead>
            <tbody>
              {art123Records.map((r: any, i: number) => (
                <tr key={i}>
                  <td>{r.employeeName}</td>
                  <td>{r.egn}</td>
                  <td>{r.changeTypeName}</td>
                  <td>{r.fields[3]}</td>
                  <td>{r.fields[5]}</td>
                  <td>{r.fields[6]}</td>
                  <td>{r.fields[8]}</td>
                </tr>
              ))}
              {art123Records.length === 0 && <tr><td colSpan={7}>Натиснете "Преглед" за зареждане.</td></tr>}
            </tbody>
          </table>
        </>
      )}

      {/* === Чл. 73 ЗДДФЛ === */}
      {tab === 'art73' && (
        <>
          <div style={{ display: 'flex', gap: 8, margin: '12px 0' }}>
            <button className="btn" onClick={loadArt73Preview} disabled={loading}>Преглед</button>
            <button className="btn btn-primary" onClick={generateArt73} disabled={loading}>Генерирай файл</button>
          </div>

          {art73Data && (
            <>
              <div style={{ display: 'flex', gap: 24, marginBottom: 12, flexWrap: 'wrap' }}>
                <div><strong>Фирма:</strong> {art73Data.companyName}</div>
                <div><strong>ЕИК:</strong> {art73Data.bulstat}</div>
                <div><strong>Година:</strong> {art73Data.year}</div>
                <div><strong>Служители:</strong> {art73Data.totalEmployees}</div>
              </div>
              <div style={{ display: 'flex', gap: 24, marginBottom: 12, flexWrap: 'wrap', background: '#f5f5f5', padding: 8, borderRadius: 4 }}>
                <div><strong>Общо брутно:</strong> {fmt(art73Data.totalGross)}</div>
                <div><strong>Общо осигуровки:</strong> {fmt(art73Data.totalInsurance)}</div>
                <div><strong>Общо данък:</strong> {fmt(art73Data.totalTax)}</div>
                <div><strong>Общо нето:</strong> {fmt(art73Data.totalNet)}</div>
              </div>

              <div style={{ overflowX: 'auto' }}>
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Служител</th>
                      <th>ЕГН/ЛНЧ</th>
                      <th style={{ textAlign: 'right' }}>Месеци</th>
                      <th style={{ textAlign: 'right' }}>Брутно</th>
                      <th style={{ textAlign: 'right' }}>Осиг. доход</th>
                      <th style={{ textAlign: 'right' }}>Осиг. вноски</th>
                      <th style={{ textAlign: 'right' }}>Облаг. доход</th>
                      <th style={{ textAlign: 'right' }}>Данък</th>
                      <th style={{ textAlign: 'right' }}>Нето</th>
                    </tr>
                  </thead>
                  <tbody>
                    {art73Data.records.map((r: any) => (
                      <tr key={r.employeeId}>
                        <td>{r.employeeName}</td>
                        <td>{r.egn}</td>
                        <td style={{ textAlign: 'right' }}>{r.monthsWorked}</td>
                        <td style={{ textAlign: 'right' }}>{fmt(r.totalGross)}</td>
                        <td style={{ textAlign: 'right' }}>{fmt(r.totalInsurableIncome)}</td>
                        <td style={{ textAlign: 'right' }}>{fmt(r.totalEmployeeInsurance)}</td>
                        <td style={{ textAlign: 'right' }}>{fmt(r.totalTaxBase)}</td>
                        <td style={{ textAlign: 'right' }}>{fmt(r.totalIncomeTax)}</td>
                        <td style={{ textAlign: 'right' }}>{fmt(r.totalNet)}</td>
                      </tr>
                    ))}
                    {art73Data.records.length === 0 && <tr><td colSpan={9}>Няма данни за годината.</td></tr>}
                  </tbody>
                  {art73Data.records.length > 0 && (
                    <tfoot>
                      <tr style={{ fontWeight: 700, background: '#f5f5f5' }}>
                        <td colSpan={3}>Общо</td>
                        <td style={{ textAlign: 'right' }}>{fmt(art73Data.totalGross)}</td>
                        <td></td>
                        <td style={{ textAlign: 'right' }}>{fmt(art73Data.totalInsurance)}</td>
                        <td></td>
                        <td style={{ textAlign: 'right' }}>{fmt(art73Data.totalTax)}</td>
                        <td style={{ textAlign: 'right' }}>{fmt(art73Data.totalNet)}</td>
                      </tr>
                    </tfoot>
                  )}
                </table>
              </div>
            </>
          )}

          {!art73Data && <p>Натиснете "Преглед" за зареждане на годишни данни.</p>}
        </>
      )}

      {/* === История === */}
      {tab === 'history' && (
        <table className="data-table">
          <thead>
            <tr>
              <th>Тип</th>
              <th>Период</th>
              <th>Файл</th>
              <th style={{ textAlign: 'right' }}>Записи</th>
              <th>Статус</th>
              <th>Генериран</th>
              <th>Действия</th>
            </tr>
          </thead>
          <tbody>
            {submissions.map(s => (
              <tr key={s.id}>
                <td>{typeLabels[s.type] || s.type}</td>
                <td>{monthNames[s.month]} {s.year}</td>
                <td>{s.fileName}</td>
                <td style={{ textAlign: 'right' }}>{s.recordCount}</td>
                <td>{s.status}</td>
                <td>{s.generatedAt ? new Date(s.generatedAt).toLocaleString('bg-BG') : ''}</td>
                <td>
                  <button className="btn btn-sm" onClick={() => handleDownload(s.id, s.fileName)}>Свали</button>
                </td>
              </tr>
            ))}
            {submissions.length === 0 && <tr><td colSpan={7}>Няма генерирани файлове.</td></tr>}
          </tbody>
        </table>
      )}
    </div>
  );
}
