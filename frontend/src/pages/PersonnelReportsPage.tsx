import { useEffect, useState } from 'react';
import { dashboardApi } from '../api/apiClient';

interface Props {
  companyId: string | null;
}

interface PersonnelReport {
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

function DistributionBar({ data, colors }: { data: Record<string, number>; colors: string[] }) {
  const total = Object.values(data).reduce((s, v) => s + v, 0);
  if (total === 0) return <p style={{ color: '#999' }}>Няма данни</p>;

  return (
    <div>
      <div style={{ display: 'flex', height: 28, borderRadius: 4, overflow: 'hidden', marginBottom: 8 }}>
        {Object.entries(data).map(([label, value], i) => {
          const pct = (value / total) * 100;
          if (pct === 0) return null;
          return (
            <div
              key={label}
              title={`${label}: ${value} (${pct.toFixed(1)}%)`}
              style={{
                width: `${pct}%`,
                background: colors[i % colors.length],
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#fff',
                fontSize: 11,
                fontWeight: 600,
                minWidth: pct > 5 ? undefined : 2,
              }}
            >
              {pct > 8 ? `${value}` : ''}
            </div>
          );
        })}
      </div>
      <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', fontSize: 13 }}>
        {Object.entries(data).map(([label, value], i) => (
          <span key={label} style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
            <span style={{ width: 10, height: 10, borderRadius: 2, background: colors[i % colors.length], display: 'inline-block' }} />
            {label}: <strong>{value}</strong> ({total > 0 ? ((value / total) * 100).toFixed(1) : 0}%)
          </span>
        ))}
      </div>
    </div>
  );
}

export default function PersonnelReportsPage({ companyId }: Props) {
  const [report, setReport] = useState<PersonnelReport | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!companyId) return;
    setLoading(true);
    dashboardApi.analytics(companyId).then(setReport).catch(() => setReport(null)).finally(() => setLoading(false));
  }, [companyId]);

  if (!companyId) return <div className="page"><p>Моля, изберете фирма.</p></div>;
  if (loading) return <div className="page"><p>Зареждане...</p></div>;
  if (!report) return <div className="page"><p>Няма данни за анализ.</p></div>;

  const turnoverRate = report.activeEmployees > 0
    ? ((report.terminatedThisYear / report.activeEmployees) * 100).toFixed(1)
    : '0.0';

  const genderColors = ['#1976d2', '#e91e63', '#9e9e9e'];
  const ageColors = ['#42a5f5', '#66bb6a', '#ffa726', '#ef5350', '#ab47bc', '#8d6e63'];
  const eduColors = ['#78909c', '#8d6e63', '#66bb6a', '#42a5f5', '#ab47bc', '#ffa726', '#ef5350'];

  return (
    <div className="page" style={{ maxWidth: 1100 }}>
      <h1>Анализ на персонала</h1>

      {/* Summary cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 12, marginBottom: 24 }}>
        <SummaryCard label="Общо служители" value={report.totalEmployees} color="#1976d2" />
        <SummaryCard label="Активни" value={report.activeEmployees} color="#4caf50" />
        <SummaryCard label="Неактивни" value={report.inactiveEmployees} color="#9e9e9e" />
        <SummaryCard label="Наети тази година" value={report.hiredThisYear} color="#2e7d32" />
        <SummaryCard label="Напуснали тази година" value={report.terminatedThisYear} color="#d32f2f" />
        <SummaryCard label="Текучество" value={`${turnoverRate}%`} color="#e65100" />
        <SummaryCard label="Средна възраст" value={`${report.averageAge} г.`} color="#7b1fa2" />
        <SummaryCard label="Среден стаж" value={`${report.averageSeniority} г.`} color="#00838f" />
      </div>

      {/* Gender */}
      <div style={{ marginBottom: 24 }}>
        <h3 style={{ marginBottom: 8 }}>Разпределение по пол</h3>
        <DistributionBar data={report.genderDistribution} colors={genderColors} />
      </div>

      {/* Age */}
      <div style={{ marginBottom: 24 }}>
        <h3 style={{ marginBottom: 8 }}>Разпределение по възраст</h3>
        <DistributionBar data={report.ageDistribution} colors={ageColors} />
      </div>

      {/* Education */}
      <div style={{ marginBottom: 24 }}>
        <h3 style={{ marginBottom: 8 }}>Разпределение по образование</h3>
        <DistributionBar data={report.educationDistribution} colors={eduColors} />
      </div>

      {/* Detailed tables */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24 }}>
        <div>
          <h3>Възрастови групи</h3>
          <table className="data-table">
            <thead>
              <tr>
                <th>Група</th>
                <th style={{ textAlign: 'right' }}>Брой</th>
                <th style={{ textAlign: 'right' }}>%</th>
              </tr>
            </thead>
            <tbody>
              {Object.entries(report.ageDistribution).map(([group, count]) => (
                <tr key={group}>
                  <td>{group}</td>
                  <td style={{ textAlign: 'right' }}>{count}</td>
                  <td style={{ textAlign: 'right' }}>
                    {report.activeEmployees > 0 ? ((count / report.activeEmployees) * 100).toFixed(1) : '0.0'}%
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div>
          <h3>Образование</h3>
          <table className="data-table">
            <thead>
              <tr>
                <th>Ниво</th>
                <th style={{ textAlign: 'right' }}>Брой</th>
                <th style={{ textAlign: 'right' }}>%</th>
              </tr>
            </thead>
            <tbody>
              {Object.entries(report.educationDistribution).map(([edu, count]) => (
                <tr key={edu}>
                  <td>{edu}</td>
                  <td style={{ textAlign: 'right' }}>{count}</td>
                  <td style={{ textAlign: 'right' }}>
                    {report.activeEmployees > 0 ? ((count / report.activeEmployees) * 100).toFixed(1) : '0.0'}%
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

function SummaryCard({ label, value, color }: { label: string; value: string | number; color: string }) {
  return (
    <div style={{
      background: '#fff',
      border: '1px solid #e0e0e0',
      borderLeft: `4px solid ${color}`,
      borderRadius: 6,
      padding: '12px 16px',
    }}>
      <div style={{ fontSize: 13, color: '#666' }}>{label}</div>
      <div style={{ fontSize: 24, fontWeight: 700, color }}>{value}</div>
    </div>
  );
}
