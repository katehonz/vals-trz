import { useEffect, useState } from 'react';
import { shiftScheduleApi } from '../api/apiClient';

interface Props {
  companyId: string | null;
}

interface ShiftDef {
  index: number;
  name: string;
  startTime: string;
  endTime: string;
  totalHours: number;
  nightHours: number;
}

interface ShiftSchedule {
  id: string;
  name: string;
  code: string;
  type: string;
  referenceMonths: number;
  shifts: ShiftDef[];
  rotationPattern: number[];
  active: boolean;
}

const typeLabels: Record<string, string> = {
  ROTATING: 'Ротационен',
  FIXED: 'Фиксиран',
  FLEXIBLE: 'Гъвкав',
};

export default function ShiftSchedulesPage({ companyId }: Props) {
  const [items, setItems] = useState<ShiftSchedule[]>([]);
  const [selected, setSelected] = useState<ShiftSchedule | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({
    name: '', code: '', type: 'ROTATING', referenceMonths: 1,
    shifts: [{ index: 1, name: 'Смяна 1', startTime: '08:00', endTime: '16:00', totalHours: 8, nightHours: 0 }] as ShiftDef[],
    rotationPattern: [1, 1, 1, 1, 1, 0, 0] as number[],
    active: true,
  });

  const load = async () => {
    if (!companyId) return;
    const data = await shiftScheduleApi.getAll(companyId);
    setItems(data);
  };

  useEffect(() => { load(); }, [companyId]);

  const handleSeed = async () => {
    if (!companyId) return;
    await shiftScheduleApi.seed(companyId);
    await load();
  };

  const handleSave = async () => {
    if (!companyId || !form.name) return;
    if (selected) {
      await shiftScheduleApi.update(companyId, selected.id, form);
    } else {
      await shiftScheduleApi.create(companyId, form);
    }
    setShowForm(false);
    setSelected(null);
    await load();
  };

  const handleEdit = (item: ShiftSchedule) => {
    setSelected(item);
    setForm({
      name: item.name, code: item.code, type: item.type,
      referenceMonths: item.referenceMonths,
      shifts: item.shifts || [],
      rotationPattern: item.rotationPattern || [],
      active: item.active,
    });
    setShowForm(true);
  };

  const handleDelete = async (id: string) => {
    if (!companyId || !window.confirm('Сигурни ли сте?')) return;
    await shiftScheduleApi.delete(companyId, id);
    await load();
  };

  const addShift = () => {
    const idx = form.shifts.length + 1;
    setForm({ ...form, shifts: [...form.shifts, { index: idx, name: `Смяна ${idx}`, startTime: '08:00', endTime: '16:00', totalHours: 8, nightHours: 0 }] });
  };

  const updateShift = (i: number, field: string, value: any) => {
    const shifts = [...form.shifts];
    (shifts[i] as any)[field] = value;
    setForm({ ...form, shifts });
  };

  const removeShift = (i: number) => {
    setForm({ ...form, shifts: form.shifts.filter((_, idx) => idx !== i) });
  };

  const patternDisplay = (pattern: number[], shifts: ShiftDef[]) => {
    return pattern.map(p => p === 0 ? 'П' : (shifts.find(s => s.index === p)?.name || `С${p}`)).join(' → ');
  };

  if (!companyId) return <div className="page"><p>Моля, изберете фирма.</p></div>;

  return (
    <div className="page">
      <h1>Графици на смени (СИРВ)</h1>

      <div className="toolbar">
        <button className="btn-success" onClick={handleSeed}>Зареди стандартни графици</button>
        <button onClick={() => { setSelected(null); setForm({ name: '', code: '', type: 'ROTATING', referenceMonths: 1, shifts: [{ index: 1, name: 'Смяна 1', startTime: '08:00', endTime: '16:00', totalHours: 8, nightHours: 0 }], rotationPattern: [1, 1, 1, 1, 1, 0, 0], active: true }); setShowForm(!showForm); }}>
          {showForm && !selected ? 'Отказ' : '+ Нов график'}
        </button>
      </div>

      {showForm && (
        <div className="form-section" style={{ margin: '12px 0', padding: 16, background: '#f9f9f9', borderRadius: 6 }}>
          <h3>{selected ? 'Редакция' : 'Нов график'}</h3>
          <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', marginBottom: 12 }}>
            <label>Наименование: <input type="text" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} /></label>
            <label>Код: <input type="text" value={form.code} onChange={e => setForm({ ...form, code: e.target.value })} style={{ width: 100 }} /></label>
            <label>Тип:
              <select value={form.type} onChange={e => setForm({ ...form, type: e.target.value })}>
                <option value="ROTATING">Ротационен</option>
                <option value="FIXED">Фиксиран</option>
                <option value="FLEXIBLE">Гъвкав</option>
              </select>
            </label>
            <label>СИРВ период (мес.): <input type="number" value={form.referenceMonths} onChange={e => setForm({ ...form, referenceMonths: parseInt(e.target.value) || 1 })} style={{ width: 60 }} /></label>
            <label style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
              <input type="checkbox" checked={form.active} onChange={e => setForm({ ...form, active: e.target.checked })} /> Активен
            </label>
          </div>

          <h4>Смени</h4>
          <table className="data-table" style={{ marginBottom: 12 }}>
            <thead>
              <tr><th>#</th><th>Наименование</th><th>Начало</th><th>Край</th><th>Часове</th><th>Нощни</th><th></th></tr>
            </thead>
            <tbody>
              {form.shifts.map((s, i) => (
                <tr key={i}>
                  <td>{s.index}</td>
                  <td><input type="text" value={s.name} onChange={e => updateShift(i, 'name', e.target.value)} /></td>
                  <td><input type="time" value={s.startTime} onChange={e => updateShift(i, 'startTime', e.target.value)} /></td>
                  <td><input type="time" value={s.endTime} onChange={e => updateShift(i, 'endTime', e.target.value)} /></td>
                  <td><input type="number" value={s.totalHours} onChange={e => updateShift(i, 'totalHours', parseFloat(e.target.value) || 0)} style={{ width: 60 }} /></td>
                  <td><input type="number" value={s.nightHours} onChange={e => updateShift(i, 'nightHours', parseFloat(e.target.value) || 0)} style={{ width: 60 }} /></td>
                  <td><button className="btn-danger btn-sm" onClick={() => removeShift(i)}>X</button></td>
                </tr>
              ))}
            </tbody>
          </table>
          <button onClick={addShift} style={{ marginBottom: 12 }}>+ Добави смяна</button>

          <div style={{ marginBottom: 12 }}>
            <label>Ротационен модел (индекси, 0=почивен, разделени с запетая):
              <input type="text" value={form.rotationPattern.join(',')}
                onChange={e => setForm({ ...form, rotationPattern: e.target.value.split(',').map(v => parseInt(v.trim()) || 0) })}
                style={{ width: 300 }} />
            </label>
            <div style={{ color: '#666', fontSize: '0.85rem', marginTop: 4 }}>
              Визуализация: {patternDisplay(form.rotationPattern, form.shifts)}
            </div>
          </div>

          <div style={{ display: 'flex', gap: 8 }}>
            <button className="btn-success" onClick={handleSave}>Запази</button>
            <button onClick={() => { setShowForm(false); setSelected(null); }}>Отказ</button>
          </div>
        </div>
      )}

      <table className="data-table">
        <thead>
          <tr>
            <th>Наименование</th>
            <th>Код</th>
            <th>Тип</th>
            <th>СИРВ (мес.)</th>
            <th>Смени</th>
            <th>Ротация</th>
            <th>Активен</th>
            <th>Действия</th>
          </tr>
        </thead>
        <tbody>
          {items.map(item => (
            <tr key={item.id}>
              <td>{item.name}</td>
              <td>{item.code}</td>
              <td>{typeLabels[item.type] || item.type}</td>
              <td>{item.referenceMonths}</td>
              <td>{item.shifts?.map(s => s.name).join(', ') || '-'}</td>
              <td style={{ fontSize: '0.85rem' }}>{patternDisplay(item.rotationPattern || [], item.shifts || [])}</td>
              <td>{item.active ? 'Да' : 'Не'}</td>
              <td>
                <button className="btn-sm" onClick={() => handleEdit(item)}>Редактирай</button>
                <button className="btn-danger btn-sm" onClick={() => handleDelete(item.id)} style={{ marginLeft: 4 }}>Изтрий</button>
              </td>
            </tr>
          ))}
          {items.length === 0 && <tr><td colSpan={8}>Няма графици. Натиснете "Зареди стандартни графици" за начало.</td></tr>}
        </tbody>
      </table>
    </div>
  );
}
