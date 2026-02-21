import { useEffect, useState } from 'react';
import { nomenclatureApi } from '../api/apiClient';

interface Props {
  companyId: string | null;
}

interface NomenclatureEntry {
  code: string;
  value: string;
  description: string;
  active: boolean;
}

interface Nomenclature {
  id: string;
  tenantId: string;
  code: string;
  name: string;
  type: 'SYSTEM' | 'USER';
  entries: NomenclatureEntry[];
}

export default function NomenclaturesPage({ companyId }: Props) {
  const [items, setItems] = useState<Nomenclature[]>([]);
  const [selected, setSelected] = useState<Nomenclature | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ code: '', name: '' });

  const loadItems = async () => {
    if (!companyId) return;
    const data = await nomenclatureApi.getAll(companyId);
    setItems(data);
  };

  useEffect(() => { loadItems(); }, [companyId]);

  const handleCreate = async () => {
    if (!companyId || !form.code || !form.name) return;
    await nomenclatureApi.create(companyId, { ...form, type: 'USER', entries: [] });
    setForm({ code: '', name: '' });
    setShowForm(false);
    await loadItems();
  };

  const handleDelete = async (id: string) => {
    if (!companyId) return;
    await nomenclatureApi.delete(companyId, id);
    if (selected?.id === id) setSelected(null);
    await loadItems();
  };

  if (!companyId) return <div className="page"><p>Моля, изберете фирма.</p></div>;

  return (
    <div className="page">
      <h1>Номенклатури</h1>

      <button onClick={() => setShowForm(!showForm)}>
        {showForm ? 'Отказ' : '+ Добави номенклатура'}
      </button>

      {showForm && (
        <div className="form-inline">
          <input placeholder="Код" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} />
          <input placeholder="Наименование" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          <button onClick={handleCreate}>Запази</button>
        </div>
      )}

      <table className="data-table">
        <thead>
          <tr>
            <th>Код</th>
            <th>Наименование</th>
            <th>Тип</th>
            <th>Записи</th>
            <th>Действия</th>
          </tr>
        </thead>
        <tbody>
          {items.map((item) => (
            <tr key={item.id} className={selected?.id === item.id ? 'selected-row' : ''}
              onClick={() => setSelected(item)} style={{ cursor: 'pointer' }}>
              <td>{item.code}</td>
              <td>{item.name}</td>
              <td>{item.type === 'SYSTEM' ? 'Системна' : 'Потребителска'}</td>
              <td>{item.entries?.length || 0}</td>
              <td>
                {item.type !== 'SYSTEM' && (
                  <button className="btn-danger" onClick={(e) => { e.stopPropagation(); handleDelete(item.id); }}>Изтрий</button>
                )}
              </td>
            </tr>
          ))}
          {items.length === 0 && <tr><td colSpan={5}>Няма номенклатури.</td></tr>}
        </tbody>
      </table>

      {selected && (
        <div style={{ marginTop: '20px' }}>
          <h2>Записи: {selected.name}</h2>
          <table className="data-table">
            <thead>
              <tr>
                <th>Код</th>
                <th>Стойност</th>
                <th>Описание</th>
                <th>Активен</th>
              </tr>
            </thead>
            <tbody>
              {(selected.entries || []).map((entry, i) => (
                <tr key={i}>
                  <td>{entry.code}</td>
                  <td>{entry.value}</td>
                  <td>{entry.description}</td>
                  <td>{entry.active ? 'Да' : 'Не'}</td>
                </tr>
              ))}
              {(!selected.entries || selected.entries.length === 0) && (
                <tr><td colSpan={4}>Няма записи.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
