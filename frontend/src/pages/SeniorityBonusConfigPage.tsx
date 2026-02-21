import { useEffect, useState } from 'react';

interface Props {
  companyId: string | null;
}

interface SeniorityBracket {
  fromYears: number;
  toYears: number;
  percent: number;
}

interface SeniorityConfig {
  id?: string;
  percentPerYear: number | null;
  autoUpdateOnMonthClose: boolean;
  brackets: SeniorityBracket[];
}

const emptyConfig: SeniorityConfig = {
  percentPerYear: 0.6,
  autoUpdateOnMonthClose: true,
  brackets: [],
};

const defaultBrackets: SeniorityBracket[] = [
  { fromYears: 0, toYears: 1, percent: 0 },
  { fromYears: 1, toYears: 2, percent: 0.6 },
  { fromYears: 2, toYears: 3, percent: 1.2 },
  { fromYears: 3, toYears: 4, percent: 1.8 },
  { fromYears: 4, toYears: 5, percent: 2.4 },
  { fromYears: 5, toYears: 10, percent: 3.0 },
  { fromYears: 10, toYears: 15, percent: 4.0 },
  { fromYears: 15, toYears: 20, percent: 5.0 },
  { fromYears: 20, toYears: 99, percent: 6.0 },
];

export default function SeniorityBonusConfigPage({ companyId }: Props) {
  const [config, setConfig] = useState<SeniorityConfig>(emptyConfig);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [mode, setMode] = useState<'percent' | 'brackets'>('percent');

  useEffect(() => {
    if (!companyId) return;
    loadConfig();
  }, [companyId]);

  if (!companyId) return <div className="page"><p>Моля, изберете фирма.</p></div>;

  async function loadConfig() {
    setLoading(true);
    try {
      const res = await fetch(`/api/companies/${companyId}/seniority-config`, {
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${localStorage.getItem('jwt_token')}` },
      });
      if (res.status === 204) {
        setConfig(emptyConfig);
      } else if (res.ok) {
        const data = await res.json();
        setConfig(data);
        setMode(data.brackets && data.brackets.length > 0 ? 'brackets' : 'percent');
      }
    } catch (e: any) {
      setMessage('Грешка при зареждане: ' + e.message);
    }
    setLoading(false);
  }

  async function saveConfig() {
    setLoading(true);
    setMessage('');
    try {
      const payload = {
        ...config,
        percentPerYear: mode === 'percent' ? config.percentPerYear : null,
        brackets: mode === 'brackets' ? config.brackets : [],
      };
      const res = await fetch(`/api/companies/${companyId}/seniority-config`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${localStorage.getItem('jwt_token')}` },
        body: JSON.stringify(payload),
      });
      if (res.ok) {
        const data = await res.json();
        setConfig(data);
        setMessage('Настройките са записани.');
      } else {
        setMessage('Грешка при запис.');
      }
    } catch (e: any) {
      setMessage('Грешка: ' + e.message);
    }
    setLoading(false);
  }

  function loadDefaultBrackets() {
    setConfig({ ...config, brackets: [...defaultBrackets] });
    setMode('brackets');
  }

  function addBracket() {
    const last = config.brackets[config.brackets.length - 1];
    const fromYears = last ? last.toYears : 0;
    setConfig({
      ...config,
      brackets: [...config.brackets, { fromYears, toYears: fromYears + 5, percent: 0 }],
    });
  }

  function removeBracket(index: number) {
    setConfig({ ...config, brackets: config.brackets.filter((_, i) => i !== index) });
  }

  function updateBracket(index: number, field: keyof SeniorityBracket, value: number) {
    const updated = [...config.brackets];
    updated[index] = { ...updated[index], [field]: value };
    setConfig({ ...config, brackets: updated });
  }

  return (
    <div className="page" style={{ maxWidth: 900 }}>
      <h1>ДТВ за трудов стаж и професионален опит</h1>
      <p style={{ color: '#666', marginBottom: 16 }}>
        Настройки за допълнително трудово възнаграждение за ТСПО (клас прослужено време).
      </p>

      {message && (
        <p style={{ padding: '8px 12px', background: '#f0f0f0', borderRadius: 4, margin: '8px 0' }}>
          {message}
        </p>
      )}

      <div style={{ marginBottom: 16 }}>
        <label style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
          <input
            type="checkbox"
            checked={config.autoUpdateOnMonthClose}
            onChange={e => setConfig({ ...config, autoUpdateOnMonthClose: e.target.checked })}
          />
          Автоматично обновяване на класа при приключване на месец
        </label>
      </div>

      <div style={{ marginBottom: 16 }}>
        <label style={{ marginRight: 16 }}>
          <input
            type="radio"
            name="mode"
            checked={mode === 'percent'}
            onChange={() => setMode('percent')}
          /> Единен процент на година
        </label>
        <label>
          <input
            type="radio"
            name="mode"
            checked={mode === 'brackets'}
            onChange={() => setMode('brackets')}
          /> Таблица по диапазони
        </label>
      </div>

      {mode === 'percent' && (
        <div style={{ marginBottom: 16 }}>
          <label>Процент за 1 година стаж (%):
            <input
              type="number"
              step="0.1"
              value={config.percentPerYear ?? 0}
              onChange={e => setConfig({ ...config, percentPerYear: parseFloat(e.target.value) || 0 })}
              style={{ width: 100, marginLeft: 8 }}
            />
          </label>
          <p style={{ color: '#666', fontSize: 13, marginTop: 4 }}>
            Напр. 0.6 означава 0.6% за всяка пълна година стаж.
          </p>
        </div>
      )}

      {mode === 'brackets' && (
        <div style={{ marginBottom: 16 }}>
          <div style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
            <button className="btn" onClick={addBracket}>+ Добави диапазон</button>
            <button className="btn" onClick={loadDefaultBrackets}>Зареди стандартни</button>
          </div>

          {config.brackets.length > 0 && (
            <table className="data-table" style={{ maxWidth: 600 }}>
              <thead>
                <tr>
                  <th style={{ textAlign: 'right' }}>От (години)</th>
                  <th style={{ textAlign: 'right' }}>До (години)</th>
                  <th style={{ textAlign: 'right' }}>Процент (%)</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {config.brackets.map((b, i) => (
                  <tr key={i}>
                    <td>
                      <input type="number" step="0.01" value={b.fromYears}
                        onChange={e => updateBracket(i, 'fromYears', parseFloat(e.target.value) || 0)}
                        style={{ width: 80, textAlign: 'right' }} />
                    </td>
                    <td>
                      <input type="number" step="0.01" value={b.toYears}
                        onChange={e => updateBracket(i, 'toYears', parseFloat(e.target.value) || 0)}
                        style={{ width: 80, textAlign: 'right' }} />
                    </td>
                    <td>
                      <input type="number" step="0.1" value={b.percent}
                        onChange={e => updateBracket(i, 'percent', parseFloat(e.target.value) || 0)}
                        style={{ width: 80, textAlign: 'right' }} />
                    </td>
                    <td>
                      <button className="btn btn-sm" style={{ background: '#d32f2f', color: '#fff' }}
                        onClick={() => removeBracket(i)}>X</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          {config.brackets.length === 0 && (
            <p style={{ color: '#999' }}>Няма диапазони. Натиснете "Зареди стандартни" за примерна таблица.</p>
          )}
        </div>
      )}

      <div style={{ display: 'flex', gap: 8 }}>
        <button className="btn btn-primary" onClick={saveConfig} disabled={loading}>
          {loading ? 'Записване...' : 'Запиши'}
        </button>
      </div>
    </div>
  );
}
