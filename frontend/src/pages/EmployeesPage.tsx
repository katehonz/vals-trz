import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { employeeApi } from '../api/apiClient';
import type { Employee } from '../types/Personnel';

interface Props {
  companyId: string | null;
}

export default function EmployeesPage({ companyId }: Props) {
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [filter, setFilter] = useState<'all' | 'active' | 'inactive'>('active');
  const [search, setSearch] = useState('');
  const navigate = useNavigate();

  const load = async () => {
    if (!companyId) return;
    const params = filter === 'all' ? '' : `active=${filter === 'active'}`;
    const data = await employeeApi.getAll(companyId, params);
    setEmployees(data);
  };

  useEffect(() => { load(); }, [companyId, filter]);

  const filtered = search
    ? employees.filter(e =>
        `${e.firstName} ${e.middleName} ${e.lastName}`.toLowerCase().includes(search.toLowerCase()) ||
        (e.egn && e.egn.includes(search))
      )
    : employees;

  if (!companyId) return <div className="page"><p>Моля, изберете фирма.</p></div>;

  return (
    <div className="page">
      <h1>Служители</h1>

      <div className="toolbar">
        <button onClick={() => navigate('/personnel/employees/new')}>+ Нов служител</button>

        <div className="toolbar-right">
          <input
            placeholder="Търси по име или ЕГН..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="search-input"
          />
          <select value={filter} onChange={(e) => setFilter(e.target.value as any)}>
            <option value="active">Активни</option>
            <option value="inactive">Напуснали</option>
            <option value="all">Всички</option>
          </select>
        </div>
      </div>

      <table className="data-table">
        <thead>
          <tr>
            <th>ЕГН</th>
            <th>Име</th>
            <th>Телефон</th>
            <th>Email</th>
            <th>Статус</th>
          </tr>
        </thead>
        <tbody>
          {filtered.map((emp) => (
            <tr key={emp.id} className="clickable-row"
              onClick={() => navigate(`/personnel/employees/${emp.id}`)}>
              <td>{emp.egn}</td>
              <td>{emp.firstName} {emp.middleName} {emp.lastName}</td>
              <td>{emp.phone}</td>
              <td>{emp.email}</td>
              <td>
                <span className={`status-badge ${emp.active ? 'active' : 'inactive'}`}>
                  {emp.active ? 'Активен' : 'Напуснал'}
                </span>
              </td>
            </tr>
          ))}
          {filtered.length === 0 && <tr><td colSpan={5}>Няма служители.</td></tr>}
        </tbody>
      </table>
    </div>
  );
}
