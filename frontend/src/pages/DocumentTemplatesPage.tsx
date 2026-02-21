import { useEffect, useState } from 'react';
import { documentApi } from '../api/apiClient';
import type { DocumentTemplate } from '../types/Document';

interface Props {
  companyId: string | null;
}

const categories = [
  { value: '', label: 'Всички' },
  { value: 'LABOR_CONTRACT', label: 'Трудови договори' },
  { value: 'AMENDMENT', label: 'Доп. споразумения' },
  { value: 'TERMINATION', label: 'Прекратяване' },
  { value: 'LEAVE_ORDER', label: 'Отпуски' },
  { value: 'CERTIFICATE', label: 'Удостоверения' },
  { value: 'OTHER', label: 'Други' },
];

const emptyTemplate: Partial<DocumentTemplate> = {
  name: '', category: 'LABOR_CONTRACT', documentType: '', content: '', active: true,
};

export default function DocumentTemplatesPage({ companyId }: Props) {
  const [templates, setTemplates] = useState<DocumentTemplate[]>([]);
  const [filterCat, setFilterCat] = useState('');
  const [editing, setEditing] = useState<Partial<DocumentTemplate> | null>(null);
  const [editId, setEditId] = useState<string | null>(null);
  const [placeholders, setPlaceholders] = useState<Record<string, string[]>>({});
  const [showPlaceholders, setShowPlaceholders] = useState(false);
  const [msg, setMsg] = useState('');

  const load = () => {
    if (!companyId) return;
    documentApi.getTemplates(companyId, filterCat || undefined).then(setTemplates).catch(() => setTemplates([]));
  };

  useEffect(load, [companyId, filterCat]);

  useEffect(() => {
    if (!companyId || !editing?.category) return;
    documentApi.getPlaceholders(companyId, editing.category).then(setPlaceholders).catch(() => setPlaceholders({}));
  }, [companyId, editing?.category]);

  if (!companyId) return <div className="page"><p>Моля, изберете фирма.</p></div>;

  const handleSeed = () => {
    documentApi.seedTemplates(companyId).then(() => { setMsg('Системните шаблони са създадени.'); load(); });
  };

  const handleSave = () => {
    if (!editing) return;
    const promise = editId
      ? documentApi.updateTemplate(companyId, editId, editing)
      : documentApi.createTemplate(companyId, editing);
    promise.then(() => { setEditing(null); setEditId(null); setMsg('Записано.'); load(); });
  };

  const handleDelete = (id: string) => {
    if (!confirm('Изтриване?')) return;
    documentApi.deleteTemplate(companyId, id).then(() => { setMsg('Изтрито.'); load(); });
  };

  const startEdit = (t: DocumentTemplate) => {
    setEditing({ ...t });
    setEditId(t.id);
    setShowPlaceholders(false);
  };

  const startNew = () => {
    setEditing({ ...emptyTemplate });
    setEditId(null);
    setShowPlaceholders(false);
  };

  return (
    <div className="page" style={{ maxWidth: 1200 }}>
      <h1>Шаблони за документи</h1>

      <div className="toolbar">
        <div className="tabs" style={{ marginBottom: 0, borderBottom: 'none' }}>
          {categories.map(c => (
            <button key={c.value} className={filterCat === c.value ? 'active' : ''}
              onClick={() => setFilterCat(c.value)}>{c.label}</button>
          ))}
        </div>
        <div className="toolbar-right">
          <button className="btn-small" onClick={startNew}>+ Нов шаблон</button>
          <button className="btn-small" onClick={handleSeed} style={{ background: '#555' }}>Seed системни</button>
        </div>
      </div>

      {msg && <p className="message">{msg}</p>}

      {!editing && (
        <table className="data-table">
          <thead>
            <tr>
              <th>Име</th>
              <th>Категория</th>
              <th>Тип документ</th>
              <th>Системен</th>
              <th>Активен</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {templates.map(t => (
              <tr key={t.id} className="clickable-row" onClick={() => startEdit(t)}>
                <td>{t.name}</td>
                <td>{categories.find(c => c.value === t.category)?.label || t.category}</td>
                <td>{t.documentType}</td>
                <td>{t.system ? 'Да' : 'Не'}</td>
                <td><span className={`status-badge ${t.active ? 'active' : 'inactive'}`}>
                  {t.active ? 'Да' : 'Не'}</span></td>
                <td>{!t.system && <button className="btn-danger" onClick={e => { e.stopPropagation(); handleDelete(t.id); }}>Изтрий</button>}</td>
              </tr>
            ))}
            {templates.length === 0 && <tr><td colSpan={6}>Няма шаблони. Натиснете "Seed системни" за начални шаблони.</td></tr>}
          </tbody>
        </table>
      )}

      {editing && (
        <div className="form-section">
          <h3>{editId ? 'Редакция на шаблон' : 'Нов шаблон'}</h3>
          <div className="form-grid">
            <label>Име
              <input value={editing.name || ''} onChange={e => setEditing({ ...editing, name: e.target.value })} />
            </label>
            <label>Категория
              <select value={editing.category || ''} onChange={e => setEditing({ ...editing, category: e.target.value })}>
                {categories.filter(c => c.value).map(c => (
                  <option key={c.value} value={c.value}>{c.label}</option>
                ))}
              </select>
            </label>
            <label>Тип документ
              <input value={editing.documentType || ''} onChange={e => setEditing({ ...editing, documentType: e.target.value })} />
            </label>
            <label>Активен
              <select value={editing.active ? 'true' : 'false'} onChange={e => setEditing({ ...editing, active: e.target.value === 'true' })}>
                <option value="true">Да</option>
                <option value="false">Не</option>
              </select>
            </label>
          </div>

          <div style={{ marginTop: '1rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <strong>Съдържание (HTML)</strong>
              <button className="btn-small" style={{ background: '#666' }}
                onClick={() => setShowPlaceholders(!showPlaceholders)}>
                {showPlaceholders ? 'Скрий' : 'Покажи'} placeholder-и
              </button>
            </div>

            {showPlaceholders && (
              <div style={{ background: '#f5f5f5', padding: '0.75rem', borderRadius: 4, margin: '0.5rem 0', fontSize: '0.82rem' }}>
                {Object.entries(placeholders).map(([group, keys]) => (
                  <div key={group} style={{ marginBottom: '0.5rem' }}>
                    <strong>{group}:</strong>{' '}
                    {keys.map(k => (
                      <code key={k} style={{ background: '#e0e0e0', padding: '0.1rem 0.3rem', borderRadius: 3, marginRight: 4, cursor: 'pointer' }}
                        onClick={() => {
                          const ta = document.getElementById('template-content') as HTMLTextAreaElement;
                          if (ta) {
                            const pos = ta.selectionStart;
                            const val = ta.value;
                            const insert = `{{${k}}}`;
                            const newVal = val.slice(0, pos) + insert + val.slice(pos);
                            setEditing({ ...editing, content: newVal });
                          }
                        }}
                      >{`{{${k}}}`}</code>
                    ))}
                  </div>
                ))}
              </div>
            )}

            <textarea id="template-content" value={editing.content || ''}
              onChange={e => setEditing({ ...editing, content: e.target.value })}
              style={{ width: '100%', minHeight: 300, fontFamily: 'monospace', fontSize: '0.85rem', marginTop: '0.5rem' }} />
          </div>

          <div className="form-actions">
            <button onClick={handleSave}>Запис</button>
            <button onClick={() => { setEditing(null); setEditId(null); }} style={{ background: '#999' }}>Отказ</button>
          </div>
        </div>
      )}
    </div>
  );
}
