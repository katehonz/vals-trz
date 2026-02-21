import { useEffect, useState } from 'react';
import { departmentApi } from '../api/apiClient';
import type { Department } from '../types/Personnel';

interface Props {
  companyId: string | null;
}

export default function DepartmentsPage({ companyId }: Props) {
  const [departments, setDepartments] = useState<Department[]>([]);
  const [showForm, setShowForm] = useState(false);
  const [editId, setEditId] = useState<string | null>(null);
  const [form, setForm] = useState({ code: '', name: '', parentId: '', sortOrder: 0 });

  const load = async () => {
    if (!companyId) return;
    const data = await departmentApi.getAll(companyId);
    setDepartments(data);
  };

  useEffect(() => { load(); }, [companyId]);

  const resetForm = () => {
    setForm({ code: '', name: '', parentId: '', sortOrder: 0 });
    setEditId(null);
    setShowForm(false);
  };

  const handleSave = async () => {
    if (!companyId || !form.code || !form.name) return;
    const data = { ...form, parentId: form.parentId || null };
    if (editId) {
      await departmentApi.update(companyId, editId, data);
    } else {
      await departmentApi.create(companyId, data);
    }
    resetForm();
    await load();
  };

  const handleEdit = (dept: Department) => {
    setForm({ code: dept.code, name: dept.name, parentId: dept.parentId || '', sortOrder: dept.sortOrder });
    setEditId(dept.id);
    setShowForm(true);
  };

  const handleDelete = async (id: string) => {
    if (!companyId) return;
    await departmentApi.delete(companyId, id);
    await load();
  };

  const getParentName = (parentId: string | null) => {
    if (!parentId) return '—';
    const parent = departments.find(d => d.id === parentId);
    return parent ? parent.name : parentId;
  };

  // Build tree-like indentation
  const getDepth = (dept: Department): number => {
    if (!dept.parentId) return 0;
    const parent = departments.find(d => d.id === dept.parentId);
    return parent ? getDepth(parent) + 1 : 0;
  };

  const sorted = [...departments].sort((a, b) => {
    const da = getDepth(a), db = getDepth(b);
    if (da !== db) return da - db;
    return a.sortOrder - b.sortOrder;
  });

  if (!companyId) return <div className="page"><p>Моля, изберете фирма.</p></div>;

  return (
    <div className="page">
      <h1>Организационна структура</h1>

      <button onClick={() => { if (showForm) resetForm(); else setShowForm(true); }}>
        {showForm ? 'Отказ' : '+ Добави звено'}
      </button>

      {showForm && (
        <div className="form-inline">
          <input placeholder="Код" value={form.code}
            onChange={(e) => setForm({ ...form, code: e.target.value })} />
          <input placeholder="Наименование" value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })} />
          <select value={form.parentId}
            onChange={(e) => setForm({ ...form, parentId: e.target.value })}>
            <option value="">— Корен —</option>
            {departments.map(d => (
              <option key={d.id} value={d.id}>{d.code} - {d.name}</option>
            ))}
          </select>
          <input type="number" placeholder="Ред" value={form.sortOrder} style={{ width: 60 }}
            onChange={(e) => setForm({ ...form, sortOrder: Number(e.target.value) })} />
          <button onClick={handleSave}>{editId ? 'Обнови' : 'Запази'}</button>
        </div>
      )}

      <table className="data-table">
        <thead>
          <tr>
            <th>Код</th>
            <th>Наименование</th>
            <th>Горно звено</th>
            <th>Ред</th>
            <th>Действия</th>
          </tr>
        </thead>
        <tbody>
          {sorted.map((dept) => {
            const depth = getDepth(dept);
            return (
              <tr key={dept.id}>
                <td>{dept.code}</td>
                <td style={{ paddingLeft: `${depth * 1.5 + 0.75}rem` }}>{dept.name}</td>
                <td>{getParentName(dept.parentId)}</td>
                <td>{dept.sortOrder}</td>
                <td>
                  <button className="btn-small" onClick={() => handleEdit(dept)}>Редакция</button>{' '}
                  <button className="btn-danger" onClick={() => handleDelete(dept.id)}>Изтрий</button>
                </td>
              </tr>
            );
          })}
          {departments.length === 0 && <tr><td colSpan={5}>Няма звена.</td></tr>}
        </tbody>
      </table>
    </div>
  );
}
