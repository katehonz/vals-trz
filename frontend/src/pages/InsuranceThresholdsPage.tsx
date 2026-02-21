import { useEffect, useState } from 'react';
import { insuranceThresholdApi } from '../api/apiClient';

interface Props {
  companyId: string | null;
}

interface InsuranceThreshold {
  id: string;
  year: number;
  nkidCode: string;
  personnelGroup: number;
  minInsurableIncome: number;
}

const MRZ_EUR = 550.66; // Минимална работна заплата 2026 в евро

export default function InsuranceThresholdsPage({ companyId }: Props) {
  const [year, setYear] = useState(2026);
  const [thresholds, setThresholds] = useState<InsuranceThreshold[]>([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');
  const [editingCell, setEditingCell] = useState<{ nkidCode: string, group: number } | null>(null);
  const [editValue, setEditValue] = useState<string>('');

  const loadThresholds = async () => {
    if (!companyId) return;
    try {
      const data = await insuranceThresholdApi.getAll(companyId, `year=${year}`);
      setThresholds(data);
    } catch (error) {
      console.error('Error loading thresholds:', error);
    }
  };

  useEffect(() => { loadThresholds(); }, [companyId, year]);

  const handleSeed = async () => {
    if (!companyId) return;
    if (thresholds.length > 0 && !confirm(`Вече има данни за ${year} г. Сигурни ли сте, че искате да заредите стандартните?`)) return;
    setLoading(true);
    try {
      await insuranceThresholdApi.seed(companyId, year);
      await loadThresholds();
    } catch (error) {
      alert('Грешка при зареждане на данни.');
    }
    setLoading(false);
  };

  const handleCellClick = (nkidCode: string, group: number, value: number) => {
    setEditingCell({ nkidCode, group });
    setEditValue(value.toString());
  };

  const handleSaveEdit = async () => {
    if (!companyId || !editingCell) return;
    const item = thresholds.find(t => t.nkidCode === editingCell.nkidCode && t.personnelGroup === editingCell.group);
    if (!item) return;

    try {
      const updatedValue = parseFloat(editValue);
      if (isNaN(updatedValue)) return;

      await insuranceThresholdApi.update(companyId, item.id, {
        ...item,
        minInsurableIncome: updatedValue
      });
      
      setThresholds(thresholds.map(t => 
        t.id === item.id ? { ...t, minInsurableIncome: updatedValue } : t
      ));
      setEditingCell(null);
    } catch (error) {
      alert('Грешка при запис.');
    }
  };

  if (!companyId) return <div className="page"><p>Моля, изберете фирма.</p></div>;

  // Pivot data: nkidCode -> { group: value }
  const pivoted: Record<string, Record<number, InsuranceThreshold>> = {};
  thresholds.forEach(t => {
    if (!pivoted[t.nkidCode]) pivoted[t.nkidCode] = {};
    pivoted[t.nkidCode][t.personnelGroup] = t;
  });

  const nkidCodes = Object.keys(pivoted).sort((a, b) => a.localeCompare(b, undefined, { numeric: true }));
  const filteredCodes = nkidCodes.filter(code => !search || code.includes(search));

  return (
    <div className="page">
      <h1>Минимален осигурителен доход (МОД)</h1>
      <p style={{ color: '#666', marginBottom: '15px' }}>
        Минимални осигурителни прагове по икономическа дейност и квалификационни групи персонал (1-9).
      </p>

      <div className="toolbar">
        <label>Година:
          <select value={year} onChange={(e) => setYear(parseInt(e.target.value))}>
            {[2025, 2026, 2027].map((y) => (
              <option key={y} value={y}>{y}</option>
            ))}
          </select>
        </label>
        <button className="btn-success" onClick={handleSeed} disabled={loading}>
          {loading ? 'Зареждане...' : `Зареди МОД за ${year} г.`}
        </button>
        <input
          type="text"
          placeholder="Търси по КИД раздел..."
          value={search}
          onChange={e => setSearch(e.target.value)}
          style={{ minWidth: '220px' }}
        />
        <span style={{ marginLeft: 'auto', fontWeight: 600 }}>Общо: {nkidCodes.length} раздела</span>
      </div>

      <style dangerouslySetInnerHTML={{ __html: `
        .mod-table th { font-size: 12px; padding: 8px 4px !important; text-align: center; }
        .mod-table td { padding: 4px !important; text-align: right; cursor: pointer; border: 1px solid #eee; }
        .mod-table td:hover { background: #f0f7ff; }
        .mod-table td.editing { padding: 0 !important; }
        .mod-table input { width: 100%; border: 2px solid #1565c0; padding: 4px; text-align: right; box-sizing: border-box; }
        .val-mrz { color: #999; }
        .val-above { font-weight: bold; color: #333; }
      `}} />

      <table className="data-table mod-table">
        <thead>
          <tr>
            <th style={{ width: '80px', textAlign: 'left' }} rowSpan={2}>Раздел КИД</th>
            <th>1</th>
            <th>2</th>
            <th>3</th>
            <th>4</th>
            <th>5</th>
            <th>6</th>
            <th>7</th>
            <th>8</th>
            <th>9</th>
          </tr>
          <tr>
            <th title="Ръководители">Ръковод.</th>
            <th title="Специалисти">Специал.</th>
            <th title="Техници и приложни специалисти">Техници</th>
            <th title="Помощен административен персонал">Пом. адм.</th>
            <th title="Персонал, зает с услуги за населението, търговията и охраната">Услуги</th>
            <th title="Квалифицирани работници в селското, горското, ловното и рибното стопанство">Сел. стоп.</th>
            <th title="Квалифицирани работници и сродни на тях занаятчии">Занаятч.</th>
            <th title="Машинни оператори и монтажници">Оператори</th>
            <th title="Професии, неизискващи специална квалификация">Неквалиф.</th>
          </tr>
        </thead>
        <tbody>
          {filteredCodes.map(code => (
            <tr key={code}>
              <td style={{ textAlign: 'left', fontWeight: 600, background: '#f5f5f5' }}>{code}</td>
              {[1, 2, 3, 4, 5, 6, 7, 8, 9].map(group => {
                const item = pivoted[code][group];
                const value = item?.minInsurableIncome || 0;
                const isEditing = editingCell?.nkidCode === code && editingCell?.group === group;

                if (isEditing) {
                  return (
                    <td key={group} className="editing">
                      <input
                        autoFocus
                        value={editValue}
                        onChange={e => setEditValue(e.target.value)}
                        onBlur={handleSaveEdit}
                        onKeyDown={e => {
                          if (e.key === 'Enter') handleSaveEdit();
                          if (e.key === 'Escape') setEditingCell(null);
                        }}
                      />
                    </td>
                  );
                }

                return (
                  <td
                    key={group}
                    onClick={() => handleCellClick(code, group, value)}
                    className={value > MRZ_EUR ? 'val-above' : 'val-mrz'}
                  >
                    {value.toLocaleString('bg-BG', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} &euro;
                  </td>
                );
              })}
            </tr>
          ))}
          {filteredCodes.length === 0 && (
            <tr>
              <td colSpan={10} style={{ textAlign: 'center', padding: '20px' }}>
                {thresholds.length === 0 ? `Няма данни за ${year} г. Използвайте бутона "Зареди МОД".` : 'Няма намерени резултати.'}
              </td>
            </tr>
          )}
        </tbody>
      </table>

      <div style={{ marginTop: '20px', fontSize: '13px', color: '#666' }}>
        <p>* Групи 1-9 съответстват на квалификационните групи по НКПД. Задръжте мишката върху заглавието за пълно име.</p>
        <p>* Кликнете върху клетка, за да редактирате стойността.</p>
        <p>* Стойности равни на МРЗ ({MRZ_EUR.toLocaleString('bg-BG', { minimumFractionDigits: 2 })} &euro;) са в сиво, по-високите са в <b>bold</b>.</p>
      </div>
    </div>
  );
}
