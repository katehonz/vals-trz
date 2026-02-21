import { useEffect, useState } from 'react';
import { personnelTypeApi } from '../api/apiClient';
import type { PersonnelType } from '../types/Company';

interface Props {
  companyId: string | null;
}

export default function PersonnelTypesPage({ companyId }: Props) {
  const [items, setItems] = useState<PersonnelType[]>([]);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ number: 0, name: '', nkpdCode: '', minInsurableIncome: 0 });

  const loadItems = async () => {
    if (!companyId) return;
    const data = await personnelTypeApi.getAll(companyId);
    setItems(data);
  };

  useEffect(() => { loadItems(); }, [companyId]);

  const handleCreate = async () => {
    if (!companyId || !form.name) return;
    await personnelTypeApi.create(companyId, form);
    setForm({ number: 0, name: '', nkpdCode: '', minInsurableIncome: 0 });
    setShowForm(false);
    await loadItems();
  };

  const handleDelete = async (id: string) => {
    if (!companyId) return;
    await personnelTypeApi.delete(companyId, id);
    await loadItems();
  };

  if (!companyId) return <div className="page"><p>Моля, изберете фирма.</p></div>;

  return (
    <div className="page">
      <h1>Видове персонал</h1>

      <button onClick={() => setShowForm(!showForm)}>
        {showForm ? 'Отказ' : '+ Добави вид'}
      </button>

      {showForm && (
        <div className="form-inline">
          <input type="number" placeholder="Номер" value={form.number || ''}
            onChange={(e) => setForm({ ...form, number: parseInt(e.target.value) || 0 })} />
          <input placeholder="Наименование" value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })} />
          <input placeholder="НКПД код" value={form.nkpdCode}
            onChange={(e) => setForm({ ...form, nkpdCode: e.target.value })} />
          <input type="number" placeholder="Мин. осиг. доход" value={form.minInsurableIncome || ''}
            onChange={(e) => setForm({ ...form, minInsurableIncome: parseFloat(e.target.value) || 0 })} />
          <button onClick={handleCreate}>Запази</button>
        </div>
      )}

      <table className="data-table">
        <thead>
          <tr>
            <th>Номер</th>
            <th>Наименование</th>
            <th>НКПД код</th>
            <th>Мин. осиг. доход</th>
            <th>Действия</th>
          </tr>
        </thead>
        <tbody>
          {items.map((item) => (
            <tr key={item.id}>
              <td>{item.number}</td>
              <td>{item.name}</td>
              <td>{item.nkpdCode}</td>
              <td>{item.minInsurableIncome}</td>
              <td>
                <button className="btn-danger" onClick={() => handleDelete(item.id)}>Изтрий</button>
              </td>
            </tr>
          ))}
          {items.length === 0 && <tr><td colSpan={5}>Няма видове персонал.</td></tr>}
        </tbody>
      </table>
    </div>
  );
}
