import { useEffect, useState } from 'react';
import { economicActivityApi } from '../api/apiClient';

interface Props {
  companyId: string | null;
}

interface EconomicActivity {
  id: string;
  code: string;
  name: string;
  tzpbPercent: number;
  modAmount?: number;
  year: number;
  active: boolean;
}

export default function EconomicActivitiesPage({ companyId }: Props) {
  const now = new Date();
  const [year, setYear] = useState(now.getFullYear());
  const [items, setItems] = useState<EconomicActivity[]>([]);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState({ code: '', name: '', tzpbPercent: 0.4, year: now.getFullYear() });
  const [loading, setLoading] = useState(false);
  const [showAll, setShowAll] = useState(false);
  const [search, setSearch] = useState('');

  const loadItems = async () => {
    if (!companyId) return;
    const data = await economicActivityApi.getAll(companyId, `year=${year}`);
    setItems(data.sort((a: EconomicActivity, b: EconomicActivity) => a.code.localeCompare(b.code, undefined, { numeric: true })));
  };

  useEffect(() => { loadItems(); }, [companyId, year]);

  const handleSeed = async () => {
    if (!companyId) return;
    setLoading(true);
    await economicActivityApi.seed(companyId, year);
    await loadItems();
    setLoading(false);
  };

  const handleSave = async () => {
    if (!companyId || !form.code || !form.name) return;
    const payload = { ...form, year };
    if (editingId) {
      await economicActivityApi.update(companyId, editingId, payload);
    } else {
      await economicActivityApi.create(companyId, payload);
    }
    setForm({ code: '', name: '', tzpbPercent: 0.4, year });
    setEditingId(null);
    setShowForm(false);
    await loadItems();
  };

  const handleEdit = (item: EconomicActivity) => {
    setForm({ code: item.code, name: item.name, tzpbPercent: item.tzpbPercent, year: item.year });
    setEditingId(item.id);
    setShowForm(true);
  };

  const handleDelete = async (id: string) => {
    if (!companyId || !confirm('Изтриване на дейността?')) return;
    await economicActivityApi.delete(companyId, id);
    await loadItems();
  };

  const handleToggleActive = async (id: string) => {
    if (!companyId) return;
    await economicActivityApi.toggleActive(companyId, id);
    await loadItems();
  };

  const handleCancel = () => {
    setShowForm(false);
    setEditingId(null);
    setForm({ code: '', name: '', tzpbPercent: 0.4, year });
  };

  if (!companyId) return <div className="page"><p>Моля, изберете фирма.</p></div>;

  const activeItems = items.filter(i => i.active);
  const q = search.toLowerCase();
  const filteredItems = (showAll ? items : activeItems).filter(i =>
    !q || i.code.toLowerCase().includes(q) || i.name.toLowerCase().includes(q)
  );

  const tzpbGroups: Record<string, number> = {};
  activeItems.forEach(i => {
    const key = `${i.tzpbPercent}%`;
    tzpbGroups[key] = (tzpbGroups[key] || 0) + 1;
  });

  return (
    <div className="page">
      <h1>КИД / ТЗПБ</h1>
      <p style={{ color: '#666', marginBottom: '15px' }}>
        Класификация на икономическите дейности и фонд "Трудова злополука и професионална болест"
      </p>

      <div className="toolbar">
        <label>Година:
          <select value={year} onChange={(e) => setYear(parseInt(e.target.value))}>
            {[year - 1, year, year + 1].map((y) => (
              <option key={y} value={y}>{y}</option>
            ))}
          </select>
        </label>
        <button className="btn-success" onClick={handleSeed} disabled={loading}>
          {loading ? 'Зареждане...' : `Зареди стандартни за ${year} г.`}
        </button>
        <button onClick={() => { setForm({ code: '', name: '', tzpbPercent: 0.4, year }); setEditingId(null); setShowForm(!showForm); }}>
          {showForm && !editingId ? 'Отказ' : '+ Добави дейност'}
        </button>
        {items.length > 0 && (
          <button onClick={() => setShowAll(!showAll)} style={{ background: showAll ? '#666' : '#1565c0' }}>
            {showAll ? `Само активни (${activeItems.length})` : `Покажи всички (${items.length})`}
          </button>
        )}
        {items.length > 0 && (
          <input
            type="text"
            placeholder="Търси по код или име..."
            value={search}
            onChange={e => setSearch(e.target.value)}
            style={{ minWidth: '220px' }}
          />
        )}
      </div>

      {items.length > 0 && (
        <div style={{ display: 'flex', gap: '12px', marginBottom: '15px', flexWrap: 'wrap' }}>
          <span style={{ fontWeight: 600, color: '#555' }}>ТЗПБ групи:</span>
          {Object.entries(tzpbGroups).sort(([a], [b]) => parseFloat(a) - parseFloat(b)).map(([pct, count]) => (
            <span key={pct} style={{ background: '#e3f2fd', padding: '2px 10px', borderRadius: '12px', fontSize: '13px' }}>
              {pct} ({count})
            </span>
          ))}
          <span style={{ fontWeight: 600, color: '#555' }}>Активни: {activeItems.length}</span>
        </div>
      )}

      {showForm && (
        <div style={{ background: '#f9f9f9', padding: '15px', borderRadius: '4px', marginBottom: '20px', border: '1px solid #eee' }}>
          <div className="form-grid">
            <label>КИД код
              <input value={form.code} onChange={e => setForm({ ...form, code: e.target.value })} placeholder="напр. 62" />
            </label>
            <label>Наименование
              <input value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} placeholder="Наименование на дейността" style={{ minWidth: '300px' }} />
            </label>
            <label>ТЗПБ %
              <input type="number" step="0.1" min="0" max="5" value={form.tzpbPercent}
                onChange={e => setForm({ ...form, tzpbPercent: parseFloat(e.target.value) || 0 })} />
            </label>
          </div>
          <div style={{ marginTop: '10px', display: 'flex', gap: '10px' }}>
            <button onClick={handleSave}>{editingId ? 'Обнови' : 'Запази'}</button>
            <button onClick={handleCancel} style={{ background: '#666' }}>Отказ</button>
          </div>
        </div>
      )}

      <table className="data-table">
        <thead>
          <tr>
            <th style={{ width: '80px' }}>КИД код</th>
            <th>Наименование на дейността</th>
            <th style={{ width: '100px' }}>ТЗПБ %</th>
            <th style={{ width: '80px' }}>Статус</th>
            <th style={{ width: '180px' }}>Действия</th>
          </tr>
        </thead>
        <tbody>
          {filteredItems.map(item => (
            <tr key={item.id} style={{ opacity: item.active ? 1 : 0.5 }}>
              <td style={{ fontWeight: 600 }}>{item.code}</td>
              <td>{item.name}</td>
              <td style={{ textAlign: 'center', fontWeight: 600 }}>{item.tzpbPercent}%</td>
              <td style={{ textAlign: 'center' }}>
                <span style={{
                  background: item.active ? '#c8e6c9' : '#eee',
                  color: item.active ? '#2e7d32' : '#999',
                  padding: '2px 8px', borderRadius: '10px', fontSize: '12px', fontWeight: 600
                }}>
                  {item.active ? 'Активна' : 'Неактивна'}
                </span>
              </td>
              <td>
                <button className="btn-small" style={{ marginRight: '5px', background: item.active ? '#666' : '#2e7d32' }}
                  onClick={() => handleToggleActive(item.id)}>
                  {item.active ? 'Деактивирай' : 'Активирай'}
                </button>
                <button className="btn-small" style={{ marginRight: '5px' }} onClick={() => handleEdit(item)}>Редакция</button>
                <button className="btn-small" style={{ background: '#c62828' }} onClick={() => handleDelete(item.id)}>Изтрий</button>
              </td>
            </tr>
          ))}
          {filteredItems.length === 0 && items.length > 0 && (
            <tr><td colSpan={5}>Няма активни дейности. Натиснете "Покажи всички" и активирайте нужните.</td></tr>
          )}
          {items.length === 0 && (
            <tr><td colSpan={5}>Няма дейности за {year} г. Натиснете "Зареди стандартни" за зареждане.</td></tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
