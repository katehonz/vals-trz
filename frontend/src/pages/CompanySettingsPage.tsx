import { useEffect, useState } from 'react';
import { companyApi } from '../api/apiClient';
import type { Company, PersonInfo } from '../types/Company';

interface Props {
  companyId: string | null;
}

const emptyPerson: PersonInfo = {
  name: '', title: '', egn: '', citizenship: '', idCardNumber: '',
  idCardDate: '', idCardIssuedBy: '', address: '', municipality: '', region: '',
};

const emptyCompany: Partial<Company> = {
  name: '', bulstat: '', city: '', postalCode: '', ekatte: '',
  address: '', correspondenceAddress: '', phone: '', email: '',
  napTerritorialDirectorate: '', napOffice: '', noiTerritorialUnit: '',
  budgetOrganization: false, insuranceFund: false, insolvencyEligible: false,
  cukSystem: false, disabilityEnterprise: false,
  paymentDeadlineDay: 20, noticePeriodDays: 30, electronicSubmission: false,
  director: { ...emptyPerson }, napContact: { ...emptyPerson }, noiContact: { ...emptyPerson },
  hrManagerTitle: '', hrManagerName: '', chiefAccountantName: '',
  nkidCode: '', nkidSerialNumber: 0, nkpdClassifier: '',
};

export default function CompanySettingsPage({ companyId }: Props) {
  const [company, setCompany] = useState<Partial<Company>>(emptyCompany);
  const [tab, setTab] = useState<'company' | 'personnel' | 'work'>('company');
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    if (companyId) {
      companyApi.getById(companyId).then(setCompany).catch(console.error);
    }
  }, [companyId]);

  const handleSave = async () => {
    setSaving(true);
    setMessage('');
    try {
      if (company.id) {
        await companyApi.update(company.id, company);
        setMessage('Запазено успешно.');
      } else {
        const saved = await companyApi.create(company);
        setCompany(saved);
        setMessage('Фирмата е създадена.');
      }
    } catch (err) {
      setMessage('Грешка при запазване.');
    }
    setSaving(false);
  };

  const updateField = (field: string, value: string | number | boolean) => {
    setCompany((prev) => ({ ...prev, [field]: value }));
  };

  const updatePerson = (field: 'director' | 'napContact' | 'noiContact', key: string, value: string) => {
    setCompany((prev) => ({
      ...prev,
      [field]: { ...(prev[field] || emptyPerson), [key]: value },
    }));
  };

  return (
    <div className="page">
      <h1>Данни и настройки за фирмата</h1>

      <div className="tabs">
        <button className={tab === 'company' ? 'active' : ''} onClick={() => setTab('company')}>
          За фирмата
        </button>
        <button className={tab === 'personnel' ? 'active' : ''} onClick={() => setTab('personnel')}>
          Персонални данни
        </button>
        <button className={tab === 'work' ? 'active' : ''} onClick={() => setTab('work')}>
          За работа
        </button>
      </div>

      {tab === 'company' && (
        <div className="form-section">
          <div className="form-grid">
            <label>Име на осигурителя:
              <input value={company.name || ''} onChange={(e) => updateField('name', e.target.value)} />
            </label>
            <label>БУЛСТАТ/ЕИК:
              <input value={company.bulstat || ''} onChange={(e) => updateField('bulstat', e.target.value)} />
            </label>
            <label>Населено място:
              <input value={company.city || ''} onChange={(e) => updateField('city', e.target.value)} />
            </label>
            <label>Пощенски код:
              <input value={company.postalCode || ''} onChange={(e) => updateField('postalCode', e.target.value)} />
            </label>
            <label>ЕКАТТЕ:
              <input value={company.ekatte || ''} onChange={(e) => updateField('ekatte', e.target.value)} />
            </label>
            <label>Седалище и адрес:
              <input value={company.address || ''} onChange={(e) => updateField('address', e.target.value)} />
            </label>
            <label>Адрес за кореспонденция:
              <input value={company.correspondenceAddress || ''} onChange={(e) => updateField('correspondenceAddress', e.target.value)} />
            </label>
            <label>Телефон:
              <input value={company.phone || ''} onChange={(e) => updateField('phone', e.target.value)} />
            </label>
            <label>Електронен адрес:
              <input value={company.email || ''} onChange={(e) => updateField('email', e.target.value)} />
            </label>
            <label>ТД на НАП по регистрация:
              <input value={company.napTerritorialDirectorate || ''} onChange={(e) => updateField('napTerritorialDirectorate', e.target.value)} />
            </label>
            <label>Код на осн. икон. дейност (НКИД):
              <input value={company.nkidCode || ''} onChange={(e) => updateField('nkidCode', e.target.value)} />
            </label>
          </div>

          <h3>Флагове</h3>
          <div className="form-checks">
            <label><input type="checkbox" checked={company.budgetOrganization || false} onChange={(e) => updateField('budgetOrganization', e.target.checked)} /> Бюджетно предприятие</label>
            <label><input type="checkbox" checked={company.insuranceFund || false} onChange={(e) => updateField('insuranceFund', e.target.checked)} /> Осигурителна каса</label>
            <label><input type="checkbox" checked={company.insolvencyEligible || false} onChange={(e) => updateField('insolvencyEligible', e.target.checked)} /> Може да изпадне в несъстоятелност (по ЗГВРС)</label>
            <label><input type="checkbox" checked={company.disabilityEnterprise || false} onChange={(e) => updateField('disabilityEnterprise', e.target.checked)} /> Спец. предприятие за инвалиди (ЗРСОИ)</label>
            <label><input type="checkbox" checked={company.electronicSubmission || false} onChange={(e) => updateField('electronicSubmission', e.target.checked)} /> Подаване по електронен път</label>
          </div>
        </div>
      )}

      {tab === 'personnel' && (
        <div className="form-section">
          <h3>Ръководител на предприятието</h3>
          <div className="form-grid">
            <label>Име: <input value={company.director?.name || ''} onChange={(e) => updatePerson('director', 'name', e.target.value)} /></label>
            <label>Длъжност: <input value={company.director?.title || ''} onChange={(e) => updatePerson('director', 'title', e.target.value)} /></label>
            <label>ЕГН: <input value={company.director?.egn || ''} onChange={(e) => updatePerson('director', 'egn', e.target.value)} /></label>
            <label>Адрес: <input value={company.director?.address || ''} onChange={(e) => updatePerson('director', 'address', e.target.value)} /></label>
          </div>

          <h3>Лице подаващо в НАП</h3>
          <div className="form-grid">
            <label>Име: <input value={company.napContact?.name || ''} onChange={(e) => updatePerson('napContact', 'name', e.target.value)} /></label>
            <label>Длъжност: <input value={company.napContact?.title || ''} onChange={(e) => updatePerson('napContact', 'title', e.target.value)} /></label>
            <label>ЕГН: <input value={company.napContact?.egn || ''} onChange={(e) => updatePerson('napContact', 'egn', e.target.value)} /></label>
          </div>

          <h3>Лице подаващо в НОИ</h3>
          <div className="form-grid">
            <label>Име: <input value={company.noiContact?.name || ''} onChange={(e) => updatePerson('noiContact', 'name', e.target.value)} /></label>
            <label>Длъжност: <input value={company.noiContact?.title || ''} onChange={(e) => updatePerson('noiContact', 'title', e.target.value)} /></label>
            <label>ЕГН: <input value={company.noiContact?.egn || ''} onChange={(e) => updatePerson('noiContact', 'egn', e.target.value)} /></label>
          </div>

          <h3>Завеждащ ЛС / ЧР</h3>
          <div className="form-grid">
            <label>Длъжност: <input value={company.hrManagerTitle || ''} onChange={(e) => updateField('hrManagerTitle', e.target.value)} /></label>
            <label>Име: <input value={company.hrManagerName || ''} onChange={(e) => updateField('hrManagerName', e.target.value)} /></label>
            <label>Гл. счетоводител: <input value={company.chiefAccountantName || ''} onChange={(e) => updateField('chiefAccountantName', e.target.value)} /></label>
          </div>
        </div>
      )}

      {tab === 'work' && (
        <div className="form-section">
          <div className="form-grid">
            <label>Срок за изплащане (число от месеца):
              <input type="number" value={company.paymentDeadlineDay || 20} onChange={(e) => updateField('paymentDeadlineDay', parseInt(e.target.value))} />
            </label>
            <label>Срок за предизвестие (дни):
              <input type="number" value={company.noticePeriodDays || 30} onChange={(e) => updateField('noticePeriodDays', parseInt(e.target.value))} />
            </label>
          </div>
        </div>
      )}

      <div className="form-actions">
        <button onClick={handleSave} disabled={saving}>
          {saving ? 'Запазване...' : 'Запази'}
        </button>
        {message && <span className="message">{message}</span>}
      </div>
    </div>
  );
}
