import { useEffect, useState } from 'react';
import { deductionItemApi } from '../api/apiClient';
import type { DeductionItem } from '../types/Company';

interface Props {
  companyId: string | null;
}

const typeLabels: Record<string, string> = {
  FIXED: 'фиксирана сума',
  CALCULATED: 'изчисляемо',
};

const codeGroups: { label: string; from: number; to: number }[] = [
  { label: 'Доброволно осигуряване', from: 160, to: 199 },
  { label: 'ЗО - работник', from: 200, to: 209 },
  { label: 'ДОО - работник', from: 210, to: 289 },
  { label: 'Самоосигуряващи се', from: 290, to: 399 },
  { label: 'Аванси, запори, заеми', from: 400, to: 499 },
  { label: 'Данъци (ДОД)', from: 500, to: 599 },
  { label: 'ЗО - работодател', from: 600, to: 619 },
  { label: 'ДОО - работодател', from: 620, to: 699 },
  { label: 'Държавни служители', from: 700, to: 799 },
  { label: 'Други', from: 800, to: 899 },
];

function getGroup(code: string): string {
  const num = parseInt(code);
  for (const g of codeGroups) {
    if (num >= g.from && num <= g.to) return g.label;
  }
  return 'Други';
}

export default function DeductionItemsPage({ companyId }: Props) {
  const [items, setItems] = useState<DeductionItem[]>([]);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ code: '', name: '', type: 'FIXED' as DeductionItem['type'] });
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');

  const loadItems = async () => {
    if (!companyId) return;
    const data = await deductionItemApi.getAll(companyId);
    setItems(data);
  };

  useEffect(() => { loadItems(); }, [companyId]);

  const handleSeed = async () => {
    if (!companyId) return;
    if (items.length > 0 && !confirm('Вече има удръжки. Заредените данни няма да се презапишат.')) return;
    setLoading(true);
    try {
      await deductionItemApi.seed(companyId);
      await loadItems();
    } catch (error) {
      alert('Грешка при зареждане.');
    }
    setLoading(false);
  };

  const handleCreate = async () => {
    if (!companyId || !form.code || !form.name) return;
    await deductionItemApi.create(companyId, { ...form, system: false, active: true });
    setForm({ code: '', name: '', type: 'FIXED' });
    setShowForm(false);
    await loadItems();
  };

  const handleDelete = async (id: string) => {
    if (!companyId) return;
    if (!confirm('Изтриване на перото?')) return;
    await deductionItemApi.delete(companyId, id);
    await loadItems();
  };

  if (!companyId) return <div className="page"><p>Моля, изберете фирма.</p></div>;

  const sorted = [...items].sort((a, b) => a.code.localeCompare(b.code, undefined, { numeric: true }));
  const filtered = sorted.filter(item =>
    !search || item.code.includes(search) || item.name.toLowerCase().includes(search.toLowerCase())
  );

  // Group items
  const grouped: Record<string, DeductionItem[]> = {};
  for (const item of filtered) {
    const g = getGroup(item.code);
    if (!grouped[g]) grouped[g] = [];
    grouped[g].push(item);
  }

  return (
    <div className="page">
      <h1>Удръжки</h1>

      <div className="toolbar">
        <button className="btn-success" onClick={handleSeed} disabled={loading}>
          {loading ? 'Зареждане...' : 'Зареди стандартни'}
        </button>
        <button onClick={() => setShowForm(!showForm)}>
          {showForm ? 'Отказ' : '+ Добави перо'}
        </button>
        <input
          type="text"
          placeholder="Търси по код или име..."
          value={search}
          onChange={e => setSearch(e.target.value)}
          style={{ minWidth: '220px' }}
        />
        <span style={{ marginLeft: 'auto', fontWeight: 600 }}>Общо: {items.length} пера</span>
      </div>

      {showForm && (
        <div className="form-inline" style={{ marginBottom: '10px' }}>
          <input placeholder="Код" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} style={{ width: '80px' }} />
          <input placeholder="Наименование" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          <select value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value as DeductionItem['type'] })}>
            <option value="FIXED">фиксирана сума</option>
            <option value="CALCULATED">изчисляемо</option>
          </select>
          <button className="btn-success" onClick={handleCreate}>Запази</button>
        </div>
      )}

      <table className="data-table">
        <thead>
          <tr>
            <th style={{ width: '70px' }}>Код</th>
            <th>Наименование</th>
            <th style={{ width: '120px' }}>Тип</th>
            <th style={{ width: '80px' }}>Системно</th>
            <th style={{ width: '80px' }}>Действия</th>
          </tr>
        </thead>
        <tbody>
          {codeGroups.map(g => {
            const groupItems = grouped[g.label];
            if (!groupItems || groupItems.length === 0) return null;
            return [
              <tr key={`hdr-${g.label}`}>
                <td colSpan={5} style={{ background: '#fce4ec', fontWeight: 700, fontSize: '13px', padding: '6px 10px' }}>
                  {g.label} ({groupItems.length})
                </td>
              </tr>,
              ...groupItems.map((item) => (
                <tr key={item.id} style={{ opacity: item.active ? 1 : 0.5 }}>
                  <td style={{ fontWeight: 600 }}>{item.code}</td>
                  <td>{item.name}</td>
                  <td>{typeLabels[item.type] || item.type}</td>
                  <td>{item.system ? 'Да' : '—'}</td>
                  <td>
                    {!item.system && (
                      <button className="btn-danger" onClick={() => handleDelete(item.id)} style={{ fontSize: '12px', padding: '2px 8px' }}>Изтрий</button>
                    )}
                  </td>
                </tr>
              ))
            ];
          })}
          {filtered.length === 0 && (
            <tr>
              <td colSpan={5} style={{ textAlign: 'center', padding: '20px' }}>
                {items.length === 0 ? 'Няма удръжки. Натиснете "Зареди стандартни".' : 'Няма намерени резултати.'}
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
