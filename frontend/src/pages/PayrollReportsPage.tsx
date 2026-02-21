import { useEffect, useState } from 'react';
import { payrollApi, extendedReportsApi } from '../api/apiClient';
import type { PayrollReportData, RecapLine } from '../types/Payroll';

interface Props {
  companyId: string | null;
}

type Tab = 'general' | 'recap' | 'department' | 'insurance' | 'comparison' | 'statistics' | 'attendance';

const fmt = (n: number | null | undefined) =>
  n != null ? n.toFixed(2) : '0.00';

const monthNames = ['', 'Януари', 'Февруари', 'Март', 'Април', 'Май', 'Юни',
  'Юли', 'Август', 'Септември', 'Октомври', 'Ноември', 'Декември'];

interface InsuranceIncomeRow {
  employeeId: string;
  employeeName: string;
  insurableIncome: number;
  workedDays: number;
  totalInsuredDays: number;
  employeeInsurance: number;
  employerInsurance: number;
  totalInsurance: number;
}

interface ComparisonRow {
  employeeId: string;
  employeeName: string;
  currentGross: number;
  previousGross: number;
  grossDifference: number;
  currentNet: number;
  previousNet: number;
  netDifference: number;
}

interface ComparisonReport {
  currentYear: number;
  currentMonth: number;
  previousYear: number;
  previousMonth: number;
  rows: ComparisonRow[];
}

interface StatisticsReport {
  year: number;
  month: number;
  headcount: number;
  totalGross: number;
  totalNet: number;
  averageGross: number;
  averageNet: number;
  totalEmployeeInsurance: number;
  totalEmployerInsurance: number;
  totalIncomeTax: number;
  totalLaborCost: number;
}

export default function PayrollReportsPage({ companyId }: Props) {
  const now = new Date();
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [tab, setTab] = useState<Tab>('general');
  const [generalData, setGeneralData] = useState<PayrollReportData | null>(null);
  const [recapData, setRecapData] = useState<RecapLine[]>([]);
  const [departmentData, setDepartmentData] = useState<Record<string, PayrollReportData> | null>(null);
  const [insuranceData, setInsuranceData] = useState<InsuranceIncomeRow[]>([]);
  const [comparisonData, setComparisonData] = useState<ComparisonReport | null>(null);
  const [statisticsData, setStatisticsData] = useState<StatisticsReport | null>(null);
  const [attendanceData, setAttendanceData] = useState<any>(null);

  useEffect(() => {
    if (!companyId) return;
    if (tab === 'general') {
      payrollApi.generalReport(companyId, year, month)
        .then(setGeneralData).catch(() => setGeneralData(null));
    } else if (tab === 'recap') {
      payrollApi.recapReport(companyId, year, month)
        .then(setRecapData).catch(() => setRecapData([]));
    } else if (tab === 'department') {
      payrollApi.byDepartmentReport(companyId, year, month)
        .then(setDepartmentData).catch(() => setDepartmentData(null));
    } else if (tab === 'insurance') {
      extendedReportsApi.insuranceIncome(companyId, year, month)
        .then(setInsuranceData).catch(() => setInsuranceData([]));
    } else if (tab === 'comparison') {
      extendedReportsApi.comparison(companyId, year, month)
        .then(setComparisonData).catch(() => setComparisonData(null));
    } else if (tab === 'statistics') {
      extendedReportsApi.statistics(companyId, year, month)
        .then(setStatisticsData).catch(() => setStatisticsData(null));
    } else if (tab === 'attendance') {
      payrollApi.attendanceReport(companyId, year, month)
        .then(setAttendanceData).catch(() => setAttendanceData(null));
    }
  }, [companyId, year, month, tab]);

  if (!companyId) return <div className="page"><p>Моля, изберете фирма.</p></div>;

  return (
    <div className="page" style={{ maxWidth: 1400 }}>
      <h1>Справки за заплати — {monthNames[month]} {year}</h1>

      <div className="toolbar">
        <div className="tabs" style={{ marginBottom: 0, borderBottom: 'none' }}>
          <button className={tab === 'general' ? 'active' : ''} onClick={() => setTab('general')}>Обща ведомост</button>
          <button className={tab === 'recap' ? 'active' : ''} onClick={() => setTab('recap')}>Рекапитулация</button>
          <button className={tab === 'department' ? 'active' : ''} onClick={() => setTab('department')}>По отдели</button>
          <button className={tab === 'insurance' ? 'active' : ''} onClick={() => setTab('insurance')}>Осиг. доход</button>
          <button className={tab === 'comparison' ? 'active' : ''} onClick={() => setTab('comparison')}>Сравнение</button>
          <button className={tab === 'statistics' ? 'active' : ''} onClick={() => setTab('statistics')}>Статистика</button>
          <button className={tab === 'attendance' ? 'active' : ''} onClick={() => setTab('attendance')}>Присъствие</button>
        </div>
        <div className="toolbar-right">
          <select value={month} onChange={e => setMonth(Number(e.target.value))}>
            {monthNames.slice(1).map((name, i) => (
              <option key={i + 1} value={i + 1}>{name}</option>
            ))}
          </select>
          <input type="number" value={year} style={{ width: 80 }}
            onChange={e => setYear(Number(e.target.value))} />
        </div>
      </div>

      {/* Обща ведомост */}
      {tab === 'general' && generalData && (
        <div style={{ overflowX: 'auto' }}>
          <table className="data-table">
            <thead>
              <tr>
                <th>Служител</th>
                {generalData.columnNames.map((name, i) => (
                  <th key={i} style={{ textAlign: 'right' }}>{name}</th>
                ))}
                <th style={{ textAlign: 'right' }}>Брутно</th>
                <th style={{ textAlign: 'right' }}>Осигуровки</th>
                <th style={{ textAlign: 'right' }}>ДОД</th>
                <th style={{ textAlign: 'right' }}>Нето</th>
              </tr>
            </thead>
            <tbody>
              {generalData.rows.map((row, i) => (
                <tr key={i}>
                  <td>{row.employeeName}</td>
                  {generalData.columnCodes.map((code, j) => (
                    <td key={j} style={{ textAlign: 'right' }}>{fmt(row.values[code])}</td>
                  ))}
                  <td style={{ textAlign: 'right' }}>{fmt(row.gross)}</td>
                  <td style={{ textAlign: 'right' }}>{fmt(row.insurance)}</td>
                  <td style={{ textAlign: 'right' }}>{fmt(row.tax)}</td>
                  <td style={{ textAlign: 'right', fontWeight: 600 }}>{fmt(row.net)}</td>
                </tr>
              ))}
            </tbody>
            {generalData.rows.length > 0 && (
              <tfoot>
                <tr style={{ fontWeight: 700, background: '#f5f5f5' }}>
                  <td>Общо</td>
                  {generalData.columnCodes.map((code, j) => (
                    <td key={j} style={{ textAlign: 'right' }}>{fmt(generalData.totals[code])}</td>
                  ))}
                  <td style={{ textAlign: 'right' }}>{fmt(generalData.rows.reduce((s, r) => s + r.gross, 0))}</td>
                  <td style={{ textAlign: 'right' }}>{fmt(generalData.rows.reduce((s, r) => s + r.insurance, 0))}</td>
                  <td style={{ textAlign: 'right' }}>{fmt(generalData.rows.reduce((s, r) => s + r.tax, 0))}</td>
                  <td style={{ textAlign: 'right' }}>{fmt(generalData.rows.reduce((s, r) => s + r.net, 0))}</td>
                </tr>
              </tfoot>
            )}
          </table>
        </div>
      )}
      {tab === 'general' && !generalData && <p>Няма данни за този период.</p>}

      {/* Рекапитулация */}
      {tab === 'recap' && (
        <table className="data-table">
          <thead>
            <tr>
              <th>Код</th>
              <th>Наименование</th>
              <th style={{ textAlign: 'right' }}>Обща сума</th>
              <th style={{ textAlign: 'right' }}>Бр. служители</th>
            </tr>
          </thead>
          <tbody>
            {recapData.map((line, i) => (
              <tr key={i}>
                <td>{line.code}</td>
                <td>{line.name}</td>
                <td style={{ textAlign: 'right' }}>{fmt(line.totalAmount)}</td>
                <td style={{ textAlign: 'right' }}>{line.employeeCount}</td>
              </tr>
            ))}
            {recapData.length === 0 && <tr><td colSpan={4}>Няма данни.</td></tr>}
          </tbody>
          {recapData.length > 0 && (
            <tfoot>
              <tr style={{ fontWeight: 700, background: '#f5f5f5' }}>
                <td colSpan={2}>Общо</td>
                <td style={{ textAlign: 'right' }}>{fmt(recapData.reduce((s, l) => s + l.totalAmount, 0))}</td>
                <td></td>
              </tr>
            </tfoot>
          )}
        </table>
      )}

      {/* По отдели */}
      {tab === 'department' && departmentData && (
        <div>
          {Object.entries(departmentData).map(([dept, data]) => (
            <div key={dept} style={{ marginBottom: 24 }}>
              <h3>{dept}</h3>
              <div style={{ overflowX: 'auto' }}>
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Служител</th>
                      <th style={{ textAlign: 'right' }}>Брутно</th>
                      <th style={{ textAlign: 'right' }}>Осигуровки</th>
                      <th style={{ textAlign: 'right' }}>ДОД</th>
                      <th style={{ textAlign: 'right' }}>Нето</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.rows.map((row: any, i: number) => (
                      <tr key={i}>
                        <td>{row.employeeName}</td>
                        <td style={{ textAlign: 'right' }}>{fmt(row.gross)}</td>
                        <td style={{ textAlign: 'right' }}>{fmt(row.insurance)}</td>
                        <td style={{ textAlign: 'right' }}>{fmt(row.tax)}</td>
                        <td style={{ textAlign: 'right', fontWeight: 600 }}>{fmt(row.net)}</td>
                      </tr>
                    ))}
                  </tbody>
                  <tfoot>
                    <tr style={{ fontWeight: 700, background: '#f5f5f5' }}>
                      <td>Общо {dept}</td>
                      <td style={{ textAlign: 'right' }}>{fmt(data.rows.reduce((s: number, r: any) => s + r.gross, 0))}</td>
                      <td style={{ textAlign: 'right' }}>{fmt(data.rows.reduce((s: number, r: any) => s + r.insurance, 0))}</td>
                      <td style={{ textAlign: 'right' }}>{fmt(data.rows.reduce((s: number, r: any) => s + r.tax, 0))}</td>
                      <td style={{ textAlign: 'right' }}>{fmt(data.rows.reduce((s: number, r: any) => s + r.net, 0))}</td>
                    </tr>
                  </tfoot>
                </table>
              </div>
            </div>
          ))}
        </div>
      )}
      {tab === 'department' && !departmentData && <p>Няма данни по отдели.</p>}

      {/* Осигурителен доход */}
      {tab === 'insurance' && (
        <table className="data-table">
          <thead>
            <tr>
              <th>Служител</th>
              <th style={{ textAlign: 'right' }}>Осиг. доход</th>
              <th style={{ textAlign: 'right' }}>Отраб. дни</th>
              <th style={{ textAlign: 'right' }}>Осиг. дни</th>
              <th style={{ textAlign: 'right' }}>Осиг. работник</th>
              <th style={{ textAlign: 'right' }}>Осиг. работодател</th>
              <th style={{ textAlign: 'right' }}>Общо осиг.</th>
            </tr>
          </thead>
          <tbody>
            {insuranceData.map((row, i) => (
              <tr key={i}>
                <td>{row.employeeName}</td>
                <td style={{ textAlign: 'right' }}>{fmt(row.insurableIncome)}</td>
                <td style={{ textAlign: 'right' }}>{row.workedDays}</td>
                <td style={{ textAlign: 'right' }}>{row.totalInsuredDays}</td>
                <td style={{ textAlign: 'right' }}>{fmt(row.employeeInsurance)}</td>
                <td style={{ textAlign: 'right' }}>{fmt(row.employerInsurance)}</td>
                <td style={{ textAlign: 'right', fontWeight: 600 }}>{fmt(row.totalInsurance)}</td>
              </tr>
            ))}
            {insuranceData.length === 0 && <tr><td colSpan={7}>Няма данни.</td></tr>}
          </tbody>
          {insuranceData.length > 0 && (
            <tfoot>
              <tr style={{ fontWeight: 700, background: '#f5f5f5' }}>
                <td>Общо</td>
                <td style={{ textAlign: 'right' }}>{fmt(insuranceData.reduce((s, r) => s + r.insurableIncome, 0))}</td>
                <td></td><td></td>
                <td style={{ textAlign: 'right' }}>{fmt(insuranceData.reduce((s, r) => s + r.employeeInsurance, 0))}</td>
                <td style={{ textAlign: 'right' }}>{fmt(insuranceData.reduce((s, r) => s + r.employerInsurance, 0))}</td>
                <td style={{ textAlign: 'right' }}>{fmt(insuranceData.reduce((s, r) => s + r.totalInsurance, 0))}</td>
              </tr>
            </tfoot>
          )}
        </table>
      )}

      {/* Сравнение */}
      {tab === 'comparison' && comparisonData && (
        <table className="data-table">
          <thead>
            <tr>
              <th>Служител</th>
              <th style={{ textAlign: 'right' }}>Брутно т.м.</th>
              <th style={{ textAlign: 'right' }}>Брутно п.м.</th>
              <th style={{ textAlign: 'right' }}>Разлика брутно</th>
              <th style={{ textAlign: 'right' }}>Нето т.м.</th>
              <th style={{ textAlign: 'right' }}>Нето п.м.</th>
              <th style={{ textAlign: 'right' }}>Разлика нето</th>
            </tr>
          </thead>
          <tbody>
            {comparisonData.rows.map((row, i) => (
              <tr key={i}>
                <td>{row.employeeName}</td>
                <td style={{ textAlign: 'right' }}>{fmt(row.currentGross)}</td>
                <td style={{ textAlign: 'right' }}>{fmt(row.previousGross)}</td>
                <td style={{ textAlign: 'right', color: row.grossDifference > 0 ? 'green' : row.grossDifference < 0 ? 'red' : undefined }}>
                  {row.grossDifference > 0 ? '+' : ''}{fmt(row.grossDifference)}
                </td>
                <td style={{ textAlign: 'right' }}>{fmt(row.currentNet)}</td>
                <td style={{ textAlign: 'right' }}>{fmt(row.previousNet)}</td>
                <td style={{ textAlign: 'right', color: row.netDifference > 0 ? 'green' : row.netDifference < 0 ? 'red' : undefined }}>
                  {row.netDifference > 0 ? '+' : ''}{fmt(row.netDifference)}
                </td>
              </tr>
            ))}
            {comparisonData.rows.length === 0 && <tr><td colSpan={7}>Няма данни.</td></tr>}
          </tbody>
        </table>
      )}
      {tab === 'comparison' && !comparisonData && <p>Няма данни за сравнение.</p>}

      {/* Статистика */}
      {tab === 'statistics' && statisticsData && (
        <div className="statistics-grid">
          <table className="data-table" style={{ maxWidth: 600 }}>
            <tbody>
              <tr><td><strong>Брой служители</strong></td><td style={{ textAlign: 'right' }}>{statisticsData.headcount}</td></tr>
              <tr><td><strong>Общо брутни заплати</strong></td><td style={{ textAlign: 'right' }}>{fmt(statisticsData.totalGross)} лв.</td></tr>
              <tr><td><strong>Общо нетни заплати</strong></td><td style={{ textAlign: 'right' }}>{fmt(statisticsData.totalNet)} лв.</td></tr>
              <tr><td><strong>Средна брутна заплата</strong></td><td style={{ textAlign: 'right' }}>{fmt(statisticsData.averageGross)} лв.</td></tr>
              <tr><td><strong>Средна нетна заплата</strong></td><td style={{ textAlign: 'right' }}>{fmt(statisticsData.averageNet)} лв.</td></tr>
              <tr><td><strong>Осигуровки работник</strong></td><td style={{ textAlign: 'right' }}>{fmt(statisticsData.totalEmployeeInsurance)} лв.</td></tr>
              <tr><td><strong>Осигуровки работодател</strong></td><td style={{ textAlign: 'right' }}>{fmt(statisticsData.totalEmployerInsurance)} лв.</td></tr>
              <tr><td><strong>ДОД (данък общ доход)</strong></td><td style={{ textAlign: 'right' }}>{fmt(statisticsData.totalIncomeTax)} лв.</td></tr>
              <tr style={{ fontWeight: 700, background: '#f5f5f5' }}>
                <td><strong>Общ разход за труд</strong></td>
                <td style={{ textAlign: 'right' }}>{fmt(statisticsData.totalLaborCost)} лв.</td>
              </tr>
            </tbody>
          </table>
        </div>
      )}
      {tab === 'statistics' && !statisticsData && <p>Няма данни за статистика.</p>}

      {/* Присъствена ведомост */}
      {tab === 'attendance' && attendanceData && (
        <div style={{ overflowX: 'auto' }}>
          <table className="data-table" style={{ fontSize: '0.75rem' }}>
            <thead>
              <tr>
                <th style={{ position: 'sticky', left: 0, background: '#f5f5f5', zIndex: 1 }}>Служител</th>
                {Array.from({ length: attendanceData.daysInMonth }, (_, i) => (
                  <th key={i + 1} style={{ textAlign: 'center', minWidth: 30 }}>{i + 1}</th>
                ))}
                <th style={{ textAlign: 'center' }}>Раб.</th>
                <th style={{ textAlign: 'center' }}>Отс.</th>
              </tr>
            </thead>
            <tbody>
              {attendanceData.rows?.map((row: any) => (
                <tr key={row.employeeId}>
                  <td style={{ position: 'sticky', left: 0, background: '#fff', zIndex: 1, whiteSpace: 'nowrap' }}>
                    {row.employeeName}
                  </td>
                  {row.dayCodes?.map((code: string, i: number) => {
                    let bg = undefined;
                    if (code === 'П') bg = '#e0e0e0';
                    else if (code === 'Пр') bg = '#ffcdd2';
                    else if (code && isNaN(Number(code))) bg = '#fff9c4';
                    return (
                      <td key={i} style={{ textAlign: 'center', background: bg, padding: '2px 4px' }}>
                        {code}
                      </td>
                    );
                  })}
                  <td style={{ textAlign: 'center', fontWeight: 600 }}>{row.workedDays}</td>
                  <td style={{ textAlign: 'center', fontWeight: 600 }}>{row.absenceDays}</td>
                </tr>
              ))}
              {(!attendanceData.rows || attendanceData.rows.length === 0) &&
                <tr><td colSpan={attendanceData.daysInMonth + 3}>Няма данни.</td></tr>
              }
            </tbody>
          </table>
          <div style={{ marginTop: '10px', fontSize: '0.8rem', color: '#666' }}>
            Легенда: числа = отработени часове, П = почивен, Пр = празничен, код = вид отсъствие
          </div>
        </div>
      )}
      {tab === 'attendance' && !attendanceData && <p>Няма данни за присъствие.</p>}
    </div>
  );
}
