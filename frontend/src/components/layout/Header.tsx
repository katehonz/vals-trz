import { useEffect, useState } from 'react';
import { companyApi } from '../../api/apiClient';
import type { Company } from '../../types/Company';

interface HeaderProps {
  currentCompanyId: string | null;
  onCompanyChange: (id: string) => void;
  user?: { fullName: string; username: string; roles: string[] } | null;
  onLogout?: () => void;
}

export default function Header({ currentCompanyId, onCompanyChange, user, onLogout }: HeaderProps) {
  const [companies, setCompanies] = useState<Company[]>([]);

  useEffect(() => {
    companyApi.getAll().then(setCompanies).catch(console.error);
  }, []);

  return (
    <header className="app-header">
      <div className="header-left">
        <strong>vals-trz</strong> | Заплати и Човешки ресурси
      </div>
      <div className="header-right" style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
        <label>
          Фирма:&nbsp;
          <select
            value={currentCompanyId || ''}
            onChange={(e) => onCompanyChange(e.target.value)}
          >
            <option value="">-- Изберете фирма --</option>
            {companies.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name} ({c.bulstat})
              </option>
            ))}
          </select>
        </label>
        {user && (
          <span style={{ fontSize: '0.82rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            {user.fullName}
            {onLogout && (
              <button onClick={onLogout} style={{
                padding: '0.2rem 0.6rem', background: 'rgba(255,255,255,0.15)',
                color: 'white', border: '1px solid rgba(255,255,255,0.3)',
                borderRadius: '3px', cursor: 'pointer', fontSize: '0.78rem',
              }}>
                Изход
              </button>
            )}
          </span>
        )}
      </div>
    </header>
  );
}
