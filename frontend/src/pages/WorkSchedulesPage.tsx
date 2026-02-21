import { useEffect, useState } from 'react';
import { workScheduleApi } from '../api/apiClient';
import type { WorkSchedule } from '../types/Company';

interface Props {
  companyId: string | null;
}

export default function WorkSchedulesPage({ companyId }: Props) {
  const [schedules, setSchedules] = useState<WorkSchedule[]>([]);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ code: '', name: '', hoursPerDay: 8 });

  const load = async () => {
    if (!companyId) return;
    setSchedules(await workScheduleApi.getAll(companyId));
  };

  useEffect(() => { load(); }, [companyId]);

  const handleCreate = async () => {
    if (!companyId || !form.code) return;
    await workScheduleApi.create(companyId, form);
    setForm({ code: '', name: '', hoursPerDay: 8 });
    setShowForm(false);
    await load();
  };

  const handleDelete = async (id: string) => {
    if (!companyId) return;
    await workScheduleApi.delete(companyId, id);
    await load();
  };

  if (!companyId) return <div className="page"><p>Моля, изберете фирма.</p></div>;

  return (
    <div className="page">
      <h1>Часови схеми на работа</h1>

      <button onClick={() => setShowForm(!showForm)}>
        {showForm ? 'Отказ' : '+ Добави схема'}
      </button>

      {showForm && (
        <div className="form-inline">
          <input placeholder="Код" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} />
          <input placeholder="Наименование" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          <input type="number" placeholder="Часове/ден" value={form.hoursPerDay} onChange={(e) => setForm({ ...form, hoursPerDay: parseFloat(e.target.value) })} />
          <button onClick={handleCreate}>Запази</button>
        </div>
      )}

      <table className="data-table">
        <thead>
          <tr>
            <th>Код</th>
            <th>Наименование</th>
            <th>Часове/ден</th>
            <th>Действия</th>
          </tr>
        </thead>
        <tbody>
          {schedules.map((s) => (
            <tr key={s.id}>
              <td>{s.code}</td>
              <td>{s.name}</td>
              <td>{s.hoursPerDay}</td>
              <td><button className="btn-danger" onClick={() => handleDelete(s.id)}>Изтрий</button></td>
            </tr>
          ))}
          {schedules.length === 0 && <tr><td colSpan={4}>Няма схеми.</td></tr>}
        </tbody>
      </table>
    </div>
  );
}
