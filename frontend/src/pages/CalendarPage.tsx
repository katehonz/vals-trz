import { useEffect, useState } from 'react';
import { calendarApi, annualCalendarApi } from '../api/apiClient';
import type { MonthlyCalendar } from '../types/Company';

interface Props {
  companyId: string | null;
}

interface HolidayEntry {
  date: string;
  name: string;
  official: boolean;
}

interface AnnualCalendar {
  id: string;
  year: number;
  holidays: HolidayEntry[];
}

const monthNames = [
  'Януари', 'Февруари', 'Март', 'Април', 'Май', 'Юни',
  'Юли', 'Август', 'Септември', 'Октомври', 'Ноември', 'Декември',
];

const dayNames = ['Нд', 'Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб'];

function formatDate(dateStr: string): string {
  const d = new Date(dateStr);
  const day = d.getDate().toString().padStart(2, '0');
  const mon = (d.getMonth() + 1).toString().padStart(2, '0');
  return `${day}.${mon}`;
}

function getDayName(dateStr: string): string {
  const d = new Date(dateStr);
  return dayNames[d.getDay()];
}

export default function CalendarPage({ companyId }: Props) {
  const now = new Date();
  const [year, setYear] = useState(now.getFullYear());
  const [items, setItems] = useState<MonthlyCalendar[]>([]);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({
    year: now.getFullYear(), month: 1,
    workingDays: 22, calendarDays: 31, holidays: 0,
    workingHoursPerDay: 8, totalWorkingHours: 176,
  });

  // Annual calendar state
  const [annualCal, setAnnualCal] = useState<AnnualCalendar | null>(null);
  const [showAddHoliday, setShowAddHoliday] = useState(false);
  const [newHoliday, setNewHoliday] = useState({ date: '', name: '', official: false });

  const loadItems = async () => {
    if (!companyId) return;
    const data = await calendarApi.getAll(companyId, `year=${year}`);
    setItems(data.sort((a: MonthlyCalendar, b: MonthlyCalendar) => a.month - b.month));
  };

  const loadAnnualCalendar = async () => {
    if (!companyId) return;
    try {
      const data = await annualCalendarApi.getAll(companyId, `year=${year}`);
      setAnnualCal(data && data.length > 0 ? data[0] : null);
    } catch {
      setAnnualCal(null);
    }
  };

  useEffect(() => { loadItems(); loadAnnualCalendar(); }, [companyId, year]);

  const handleCreate = async () => {
    if (!companyId) return;
    await calendarApi.create(companyId, form);
    setShowForm(false);
    await loadItems();
  };

  const handleDelete = async (id: string) => {
    if (!companyId) return;
    await calendarApi.delete(companyId, id);
    await loadItems();
  };

  const handleGenerateYear = async () => {
    if (!companyId) return;
    if (!window.confirm(`Сигурни ли сте, че искате да генерирате автоматично календара за ${year} г.? Това ще презапише съществуващите данни.`)) return;
    try {
      await calendarApi.generateYear(companyId, year);
      await loadItems();
    } catch (err: any) {
      alert('Грешка: ' + err.message);
    }
  };

  const handleSeedHolidays = async () => {
    if (!companyId) return;
    try {
      await annualCalendarApi.seedHolidays(companyId, year);
      await loadAnnualCalendar();
    } catch (err: any) {
      alert('Грешка: ' + err.message);
    }
  };

  const handleAddHoliday = async () => {
    if (!companyId || !newHoliday.date || !newHoliday.name) return;
    const holidays = annualCal ? [...annualCal.holidays] : [];
    holidays.push({ date: newHoliday.date, name: newHoliday.name, official: newHoliday.official });
    holidays.sort((a, b) => a.date.localeCompare(b.date));

    if (annualCal) {
      await annualCalendarApi.update(companyId, annualCal.id, { ...annualCal, holidays });
    } else {
      await annualCalendarApi.create(companyId, { year, holidays });
    }
    setNewHoliday({ date: '', name: '', official: false });
    setShowAddHoliday(false);
    await loadAnnualCalendar();
  };

  const handleRemoveHoliday = async (index: number) => {
    if (!companyId || !annualCal) return;
    const holidays = annualCal.holidays.filter((_, i) => i !== index);
    await annualCalendarApi.update(companyId, annualCal.id, { ...annualCal, holidays });
    await loadAnnualCalendar();
  };

  if (!companyId) return <div className="page"><p>Моля, изберете фирма.</p></div>;

  return (
    <div className="page">
      <h1>Времеви данни (Календар)</h1>

      <div className="toolbar">
        <label>Година:
          <select value={year} onChange={(e) => setYear(parseInt(e.target.value))}>
            {[year - 1, year, year + 1].map((y) => (
              <option key={y} value={y}>{y}</option>
            ))}
          </select>
        </label>
        <button className="btn-success" onClick={handleGenerateYear}>Генерирай автоматично за {year} г.</button>
        <button onClick={() => { setForm({ ...form, year }); setShowForm(!showForm); }}>
          {showForm ? 'Отказ' : '+ Добави месец'}
        </button>
      </div>

      {showForm && (
        <div className="form-inline">
          <select value={form.month} onChange={(e) => setForm({ ...form, month: parseInt(e.target.value) })}>
            {monthNames.map((name, i) => (
              <option key={i + 1} value={i + 1}>{name}</option>
            ))}
          </select>
          <input type="number" placeholder="Раб. дни" value={form.workingDays}
            onChange={(e) => setForm({ ...form, workingDays: parseInt(e.target.value) || 0 })} />
          <input type="number" placeholder="Календ. дни" value={form.calendarDays}
            onChange={(e) => setForm({ ...form, calendarDays: parseInt(e.target.value) || 0 })} />
          <input type="number" placeholder="Празници" value={form.holidays}
            onChange={(e) => setForm({ ...form, holidays: parseInt(e.target.value) || 0 })} />
          <input type="number" placeholder="Часове/ден" value={form.workingHoursPerDay}
            onChange={(e) => setForm({ ...form, workingHoursPerDay: parseFloat(e.target.value) || 0 })} />
          <input type="number" placeholder="Общо часове" value={form.totalWorkingHours}
            onChange={(e) => setForm({ ...form, totalWorkingHours: parseFloat(e.target.value) || 0 })} />
          <button onClick={handleCreate}>Запази</button>
        </div>
      )}

      <table className="data-table">
        <thead>
          <tr>
            <th>Месец</th>
            <th>Раб. дни</th>
            <th>Календ. дни</th>
            <th>Празници</th>
            <th>Часове/ден</th>
            <th>Общо часове</th>
            <th>Действия</th>
          </tr>
        </thead>
        <tbody>
          {items.map((item) => (
            <tr key={item.id}>
              <td>{monthNames[item.month - 1]}</td>
              <td>{item.workingDays}</td>
              <td>{item.calendarDays}</td>
              <td>{item.holidays}</td>
              <td>{item.workingHoursPerDay}</td>
              <td>{item.totalWorkingHours}</td>
              <td>
                <button className="btn-danger" onClick={() => handleDelete(item.id)}>Изтрий</button>
              </td>
            </tr>
          ))}
          {items.length === 0 && <tr><td colSpan={7}>Няма данни за {year} г.</td></tr>}
        </tbody>
      </table>

      {/* === Годишен календар — Официални празници === */}
      <h2 style={{ marginTop: 32 }}>Официални празници — {year} г.</h2>

      <div className="toolbar">
        <button className="btn-success" onClick={handleSeedHolidays}>
          Зареди БГ празници за {year} г.
        </button>
        <button onClick={() => setShowAddHoliday(!showAddHoliday)}>
          {showAddHoliday ? 'Отказ' : '+ Добави празник'}
        </button>
      </div>

      {showAddHoliday && (
        <div className="form-inline">
          <input type="date" value={newHoliday.date}
            onChange={(e) => setNewHoliday({ ...newHoliday, date: e.target.value })} />
          <input type="text" placeholder="Наименование" value={newHoliday.name}
            onChange={(e) => setNewHoliday({ ...newHoliday, name: e.target.value })} style={{ minWidth: 250 }} />
          <label style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
            <input type="checkbox" checked={newHoliday.official}
              onChange={(e) => setNewHoliday({ ...newHoliday, official: e.target.checked })} />
            Официален
          </label>
          <button onClick={handleAddHoliday}>Запази</button>
        </div>
      )}

      {annualCal && annualCal.holidays && annualCal.holidays.length > 0 ? (
        <table className="data-table">
          <thead>
            <tr>
              <th style={{ width: 60 }}>Дата</th>
              <th style={{ width: 40 }}>Ден</th>
              <th>Наименование</th>
              <th style={{ width: 80 }}>Тип</th>
              <th style={{ width: 80 }}>Действия</th>
            </tr>
          </thead>
          <tbody>
            {annualCal.holidays.map((h, i) => (
              <tr key={i}>
                <td>{formatDate(h.date)}</td>
                <td>{getDayName(h.date)}</td>
                <td>{h.name}</td>
                <td>{h.official ? 'Официален' : 'Фирмен'}</td>
                <td>
                  <button className="btn-danger btn-sm" onClick={() => handleRemoveHoliday(i)}>✕</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : (
        <p style={{ color: '#888', marginTop: 8 }}>Няма заредени празници за {year} г. Натиснете "Зареди БГ празници" за автоматично зареждане.</p>
      )}
    </div>
  );
}
