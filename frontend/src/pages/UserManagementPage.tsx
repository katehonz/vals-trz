import { useEffect, useState } from 'react';
import { userApi } from '../api/apiClient';

interface Props {
  companyId: string | null;
}

interface User {
  id: string;
  username: string;
  fullName: string;
  email: string;
  roles: string[];
  active: boolean;
  password?: string;
}

const ROLES = ['ADMIN', 'ACCOUNTANT', 'HR_MANAGER', 'VIEWER'];

export default function UserManagementPage({ companyId }: Props) {
  const [users, setUsers] = useState<User[]>([]);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState<Partial<User>>({
    username: '',
    fullName: '',
    email: '',
    roles: ['VIEWER'],
    active: true,
    password: ''
  });

  const loadUsers = async () => {
    if (!companyId) return;
    try {
      const data = await userApi.getAll(companyId);
      setUsers(data);
    } catch (err) {
      console.error('Failed to load users:', err);
    }
  };

  useEffect(() => {
    loadUsers();
  }, [companyId]);

  const handleSave = async () => {
    if (!companyId || !form.username || !form.fullName) {
      alert('Моля, попълнете потребителско име и три имена.');
      return;
    }

    try {
      if (editingId) {
        // Prepare update data, exclude username if backend doesn't allow changing it
        const updateData = {
          fullName: form.fullName,
          email: form.email,
          roles: form.roles,
          active: form.active,
          passwordHash: form.password // UserController will hash it if not empty
        };
        await userApi.update(companyId, editingId, updateData);
      } else {
        const createData = {
          ...form,
          passwordHash: form.password
        };
        await userApi.create(companyId, createData);
      }
      resetForm();
      loadUsers();
    } catch (err: any) {
      alert('Грешка при запис: ' + err.message);
    }
  };

  const handleEdit = (user: User) => {
    setEditingId(user.id);
    setForm({
      username: user.username,
      fullName: user.fullName,
      email: user.email,
      roles: user.roles,
      active: user.active,
      password: ''
    });
    setShowForm(true);
  };

  const toggleStatus = async (user: User) => {
    if (!companyId) return;
    try {
      await userApi.update(companyId, user.id, { ...user, active: !user.active });
      loadUsers();
    } catch (err: any) {
      alert('Грешка при промяна на статус: ' + err.message);
    }
  };

  const resetForm = () => {
    setShowForm(false);
    setEditingId(null);
    setForm({
      username: '',
      fullName: '',
      email: '',
      roles: ['VIEWER'],
      active: true,
      password: ''
    });
  };

  if (!companyId) return <div className="page"><p>Моля, изберете фирма.</p></div>;

  return (
    <div className="page admin-section">
      <h1>Управление на потребители</h1>

      <div className="toolbar">
        <button onClick={() => setShowForm(true)}>+ Нов потребител</button>
      </div>

      {showForm && (
        <div className="form-card" style={{ marginBottom: '20px', padding: '15px', border: '1px solid #ddd', borderRadius: '4px' }}>
          <h3>{editingId ? 'Редакция на потребител' : 'Нов потребител'}</h3>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
            <div>
              <label>Потребителско име</label>
              <input
                disabled={!!editingId}
                value={form.username}
                onChange={(e) => setForm({ ...form, username: e.target.value })}
              />
            </div>
            <div>
              <label>Парола {editingId && '(остави празно за запазване)'}</label>
              <input
                type="password"
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
              />
            </div>
            <div>
              <label>Пълно име</label>
              <input
                value={form.fullName}
                onChange={(e) => setForm({ ...form, fullName: e.target.value })}
              />
            </div>
            <div>
              <label>Email</label>
              <input
                type="email"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
              />
            </div>
            <div>
              <label>Роли</label>
              <select
                multiple
                value={form.roles}
                onChange={(e) => setForm({ ...form, roles: Array.from(e.target.selectedOptions, opt => opt.value) })}
                style={{ height: '80px' }}
              >
                {ROLES.map(role => (
                  <option key={role} value={role}>{role}</option>
                ))}
              </select>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <label>
                <input
                  type="checkbox"
                  checked={form.active}
                  onChange={(e) => setForm({ ...form, active: e.target.checked })}
                />
                Активен
              </label>
            </div>
          </div>
          <div style={{ marginTop: '15px', display: 'flex', gap: '10px' }}>
            <button onClick={handleSave}>Запази</button>
            <button className="btn-secondary" onClick={resetForm}>Отказ</button>
          </div>
        </div>
      )}

      <table className="data-table">
        <thead>
          <tr>
            <th>Потребителско име</th>
            <th>Пълно име</th>
            <th>Email</th>
            <th>Роли</th>
            <th>Статус</th>
            <th>Действия</th>
          </tr>
        </thead>
        <tbody>
          {users.map((user) => (
            <tr key={user.id}>
              <td>{user.username}</td>
              <td>{user.fullName}</td>
              <td>{user.email}</td>
              <td>{user.roles.join(', ')}</td>
              <td>
                <span className={`status-badge ${user.active ? 'active' : 'inactive'}`}>
                  {user.active ? 'Активен' : 'Неактивен'}
                </span>
              </td>
              <td>
                <div style={{ display: 'flex', gap: '5px' }}>
                  <button className="btn-small" onClick={() => handleEdit(user)}>Редакция</button>
                  <button
                    className={`btn-small ${user.active ? 'btn-danger' : 'btn-success'}`}
                    onClick={() => toggleStatus(user)}
                  >
                    {user.active ? 'Деактивирай' : 'Активирай'}
                  </button>
                </div>
              </td>
            </tr>
          ))}
          {users.length === 0 && (
            <tr>
              <td colSpan={6} style={{ textAlign: 'center' }}>Няма намерени потребители.</td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
