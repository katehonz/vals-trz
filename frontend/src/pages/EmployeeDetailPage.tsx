import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { employeeApi, employmentApi, absenceApi, leaveEntitlementApi, nssiApi, employeePayItemApi, employeeDeductionApi, garnishmentApi, economicActivityApi, insuranceApi, insuranceThresholdApi } from '../api/apiClient';
import type { Employee, Employment, Absence, LeaveEntitlement, Garnishment } from '../types/Personnel';

interface Props {
  companyId: string | null;
}

type Tab = 'personal' | 'employment' | 'absences' | 'leave' | 'payItems' | 'garnishments';

const educationLabels: Record<number, string> = {
  1: 'Начално', 2: 'Прогимназиално', 3: 'Основно', 4: 'Средно',
  5: 'Полувисше', 6: 'Висше (бакалавър/магистър)', 7: 'Докторат',
};

const contractBasisOptions = [
  { code: 'чл.67', label: 'Чл.67 ал.1 т.1 КТ - Безсрочен' },
  { code: 'чл.68', label: 'Чл.68 КТ - Срочен' },
  { code: 'чл.70', label: 'Чл.70 КТ - Със срок за изпитване' },
  { code: 'чл.110', label: 'Чл.110 КТ - Допълнителен труд' },
  { code: 'чл.111', label: 'Чл.111 КТ - При друг работодател' },
  { code: 'чл.114', label: 'Чл.114 КТ - За определени дни' },
];

const workTimeOptions = [
  { code: 'FULL', label: 'Пълно работно време' },
  { code: 'PART', label: 'Непълно работно време' },
  { code: 'IRREGULAR', label: 'Ненормирано работно време' },
];

const insuredTypeLabels: Record<string, string> = {
  '01': '01 - Трета категория',
  '02': '02 - Първа категория',
  '03': '03 - Втора категория',
  '05': '05 - Държавни служители',
  '08': '08 - Учители',
  '10': '10 - Без безработица',
  '12': '12 - Самоосигуряващи се',
  '14': '14 - Без трудово правоотн.',
};

function EmploymentDetailForm({ employment, economicActivities, insuranceRates, onSave, companyId }: {
  employment: Employment;
  economicActivities: any[];
  insuranceRates: any | null;
  onSave: (data: Employment) => Promise<void>;
  companyId: string | null;
}) {
  const [form, setForm] = useState<Employment>({ ...employment });
  const [saving, setSaving] = useState(false);
  const [currentMod, setCurrentMod] = useState<number | null>(null);

  const set = (field: string, value: any) => setForm(prev => ({ ...prev, [field]: value }));

  const handleSubmit = async () => {
    setSaving(true);
    await onSave(form);
    setSaving(false);
  };

  useEffect(() => {
    const fetchMod = async () => {
      if (!companyId || !form.kidCode || !form.personnelGroup) {
        setCurrentMod(null);
        return;
      }
      const division = form.kidCode.replace('.', '').substring(0, 2);
      const year = new Date().getFullYear();
      try {
        const data = await insuranceThresholdApi.getAll(companyId, `year=${year}&nkidCode=${division}&personnelGroup=${form.personnelGroup}`);
        if (data && data.length > 0) {
          setCurrentMod(data[0].minInsurableIncome);
        } else {
          setCurrentMod(null);
        }
      } catch (err) {
        setCurrentMod(null);
      }
    };
    fetchMod();
  }, [companyId, form.kidCode, form.personnelGroup]);

  const kidActivity = economicActivities.find(a => a.code === form.kidCode);

  return (
    <div style={{ background: '#f9f9f9', padding: '20px', borderRadius: '4px', marginTop: '15px', border: '1px solid #ddd' }}>
      <h4 style={{ marginTop: 0 }}>Детайли на договор №{form.contractNumber}</h4>

      <div className="form-section">
        <h3>Договор</h3>
        <div className="form-grid">
          <label>Номер на ТД <input value={form.contractNumber || ''} onChange={e => set('contractNumber', e.target.value)} /></label>
          <label>Дата на ТД <input type="date" value={form.contractDate || ''} onChange={e => set('contractDate', e.target.value)} /></label>
          <label>Дата на постъпване <input type="date" value={form.startDate || ''} onChange={e => set('startDate', e.target.value)} /></label>
          <label>Основание
            <select value={form.contractBasis || ''} onChange={e => set('contractBasis', e.target.value)}>
              <option value="">-- Изберете --</option>
              {contractBasisOptions.map(o => <option key={o.code} value={o.code}>{o.label}</option>)}
            </select>
          </label>
          <label>Вид договор
            <select value={form.contractType || ''} onChange={e => set('contractType', e.target.value)}>
              <option value="">-- Изберете --</option>
              <option value="безсрочен">Безсрочен</option>
              <option value="срочен">Срочен</option>
              <option value="допълнителен">Допълнителен</option>
            </select>
          </label>
          <label>Дата на изтичане <input type="date" value={form.contractEndDate || ''} onChange={e => set('contractEndDate', e.target.value)} /></label>
          <label>Срок на предизвестие (дни) <input type="number" value={form.noticePeriodDays || 0} onChange={e => set('noticePeriodDays', parseInt(e.target.value) || 0)} /></label>
        </div>
      </div>

      <div className="form-section">
        <h3>Длъжност и класификация</h3>
        <div className="form-grid">
          <label>Длъжност <input value={form.jobTitle || ''} onChange={e => set('jobTitle', e.target.value)} /></label>
          <label>НКПД код <input value={form.nkpdCode || ''} onChange={e => set('nkpdCode', e.target.value)} placeholder="напр. 72142007" /></label>
          <label>Категория персонал
            <select value={form.personnelGroup || 0} onChange={e => set('personnelGroup', parseInt(e.target.value) || 0)}>
              <option value="0">-- Изберете --</option>
              <option value="1">1 - Ръководители</option>
              <option value="2">2 - Специалисти</option>
              <option value="3">3 - Техници и приложни специалисти</option>
              <option value="4">4 - Помощен адм. персонал</option>
              <option value="5">5 - Персонал в услугите</option>
              <option value="6">6 - Квалиф. работници в земеделие</option>
              <option value="7">7 - Квалиф. работници и сродни</option>
              <option value="8">8 - Машинни оператори</option>
              <option value="9">9 - Нискоквалифицирани работници</option>
            </select>
          </label>
          <label>КИД (икон. дейност)
            <select value={form.kidCode || ''} onChange={e => set('kidCode', e.target.value)}>
              <option value="">-- Изберете КИД --</option>
              {economicActivities
                .sort((a: any, b: any) => a.code.localeCompare(b.code, undefined, { numeric: true }))
                .map((a: any) => (
                  <option key={a.id} value={a.code}>{a.code} - {a.name} (ТЗПБ {a.tzpbPercent}%)</option>
                ))}
            </select>
          </label>
          {kidActivity && (
            <label>ТЗПБ %
              <input value={`${kidActivity.tzpbPercent}%`} disabled style={{ background: '#e3f2fd', fontWeight: 600, color: '#1565c0' }} />
            </label>
          )}
          {currentMod !== null && (
            <div style={{ gridColumn: '1 / -1', background: '#fff9c4', padding: '10px 15px', borderRadius: '4px', border: '1px solid #fbc02d', color: '#856404', fontSize: '14px', fontWeight: 600 }}>
              МОД: {currentMod.toLocaleString('bg-BG', { minimumFractionDigits: 2 })} € (КИД раздел {form.kidCode?.substring(0, 2)}, Група {form.personnelGroup})
            </div>
          )}
        </div>
      </div>

      <div className="form-section">
        <h3>Осигуряване</h3>
        <div className="form-grid">
          <label>Вид осигурен
            <select value={form.insuredType || ''} onChange={e => set('insuredType', e.target.value)}>
              <option value="">-- Изберете --</option>
              {Object.entries(insuredTypeLabels).map(([code, label]) => (
                <option key={code} value={code}>{label}</option>
              ))}
            </select>
          </label>
          <label style={{ display: 'flex', alignItems: 'center', gap: '10px', flexDirection: 'row' }}>
            <input type="checkbox" checked={form.pensioner || false} onChange={e => set('pensioner', e.target.checked)} />
            Пенсионер
          </label>
          <label style={{ display: 'flex', alignItems: 'center', gap: '10px', flexDirection: 'row' }}>
            <input type="checkbox" checked={form.disability50Plus || false} onChange={e => set('disability50Plus', e.target.checked)} />
            Инвалидност &gt; 50%
          </label>
          {form.disability50Plus && insuranceRates && (
            <div style={{ background: '#e8f5e9', padding: '8px 14px', borderRadius: '4px', gridColumn: '1 / -1', fontSize: '13px' }}>
              <strong>Необлагаема сума (ЗДДФЛ чл.18, ал.2):</strong>{' '}
              {insuranceRates.disabilityTaxExemption
                ? <>{Number(insuranceRates.disabilityTaxExemption).toFixed(2)} лв./мес. ({(Number(insuranceRates.disabilityTaxExemption) * 12).toFixed(2)} лв./год.) — приспада се от данъчната основа преди изчисляване на ДОД</>
                : <span style={{ color: '#c62828' }}>Не е конфигурирана! Задайте я в Осигурителни вноски → Основни параметри.</span>
              }
            </div>
          )}
        </div>
      </div>

      <div className="form-section">
        <h3>Работно време</h3>
        <div className="form-grid">
          <label>Вид работно време
            <select value={form.workTimeType || ''} onChange={e => set('workTimeType', e.target.value)}>
              <option value="">-- Изберете --</option>
              {workTimeOptions.map(o => <option key={o.code} value={o.code}>{o.label}</option>)}
            </select>
          </label>
          <label>Работни часове на ден <input type="number" step="0.5" min="0" max="24" value={form.workingHoursPerDay || ''} onChange={e => set('workingHoursPerDay', parseFloat(e.target.value) || null)} /></label>
          <label>Часова схема <input value={form.workScheduleCode || ''} onChange={e => set('workScheduleCode', e.target.value)} /></label>
        </div>
      </div>

      <div className="form-section">
        <h3>Възнаграждение</h3>
        <div className="form-grid">
          <label>Основно месечно възнаграждение <input type="number" step="0.01" value={form.baseSalary || ''} onChange={e => set('baseSalary', parseFloat(e.target.value) || null)} /></label>
          <label>За клас % <input type="number" step="0.01" value={form.seniorityBonusPercent || ''} onChange={e => set('seniorityBonusPercent', parseFloat(e.target.value) || null)} /></label>
          <label>За стаж (години) <input type="number" step="0.01" value={form.seniorityBonusYears || ''} onChange={e => set('seniorityBonusYears', parseFloat(e.target.value) || null)} /></label>
          <label>Доп. възнаграждение - начисление <input value={form.additionalPayItemName || ''} onChange={e => set('additionalPayItemName', e.target.value)} /></label>
          <label>Доп. възнаграждение - сума <input type="number" step="0.01" value={form.additionalPayAmount || ''} onChange={e => set('additionalPayAmount', parseFloat(e.target.value) || null)} /></label>
        </div>
      </div>

      <div className="form-section">
        <h3>Отпуски (по договор)</h3>
        <div className="form-grid">
          <label>Основен платен годишен отпуск (дни) <input type="number" value={form.basicAnnualLeaveDays || 20} onChange={e => set('basicAnnualLeaveDays', parseInt(e.target.value) || 0)} /></label>
          <label>Допълнителен платен годишен отпуск (дни) <input type="number" value={form.additionalAnnualLeaveDays || 0} onChange={e => set('additionalAnnualLeaveDays', parseInt(e.target.value) || 0)} /></label>
        </div>
      </div>

      <div className="form-section">
        <h3>Други условия</h3>
        <div className="form-grid">
          <label>Текст 1 <input value={form.text1 || ''} onChange={e => set('text1', e.target.value)} /></label>
          <label>Текст 2 <input value={form.text2 || ''} onChange={e => set('text2', e.target.value)} /></label>
          <label>Текст 3 <input value={form.text3 || ''} onChange={e => set('text3', e.target.value)} /></label>
          <label>Текст 4 <input value={form.text4 || ''} onChange={e => set('text4', e.target.value)} /></label>
        </div>
      </div>

      <div className="form-actions">
        <button onClick={handleSubmit} disabled={saving}>{saving ? 'Записване...' : 'Запази промените'}</button>
      </div>
    </div>
  );
}

export default function EmployeeDetailPage({ companyId }: Props) {
  const { employeeId } = useParams();
  const navigate = useNavigate();
  const isNew = employeeId === 'new';

  const [tab, setTab] = useState<Tab>('personal');
  const [employee, setEmployee] = useState<Partial<Employee>>({ active: true, gender: 'M', educationCode: 4, childrenCount: 0 });
  const [employments, setEmployments] = useState<Employment[]>([]);
  const [absences, setAbsences] = useState<Absence[]>([]);
  const [showAbsenceForm, setShowAbsenceForm] = useState(false);
  const [absenceForm, setAbsenceForm] = useState<Partial<Absence>>({
    type: 'PAID_ANNUAL',
    fromDate: '',
    toDate: '',
    status: 'APPROVED'
  });
  const [leave, setLeave] = useState<LeaveEntitlement[]>([]);
  const [payItems, setPayItems] = useState<any[]>([]);
  const [deductions, setDeductions] = useState<any[]>([]);
  const [garnishments, setGarnishments] = useState<Garnishment[]>([]);
  const [showPayItemForm, setShowPayItemForm] = useState(false);
  const [showDeductionForm, setShowDeductionForm] = useState(false);
  const [showGarnishmentForm, setShowGarnishmentForm] = useState(false);
  const [payItemForm, setPayItemForm] = useState<any>({ payItemCode: '', payItemName: '', type: 'FIXED', value: 0, fromYear: new Date().getFullYear(), fromMonth: 1, toYear: 0, toMonth: 0, active: true });
  const [deductionForm, setDeductionForm] = useState<any>({ deductionCode: '', deductionName: '', amount: 0, fromYear: new Date().getFullYear(), fromMonth: 1, toYear: 0, toMonth: 0, active: true });
  const [garnishmentForm, setGarnishmentForm] = useState<Partial<Garnishment>>({ type: 'CHSI', description: '', creditorName: '', bailiffName: '', totalAmount: null, paidAmount: 0, monthlyAmount: null, priority: 1, startDate: new Date().toISOString().split('T')[0], active: true, hasChildren: false });
  const [editingPayItemId, setEditingPayItemId] = useState<string | null>(null);
  const [editingDeductionId, setEditingDeductionId] = useState<string | null>(null);
  const [editingGarnishmentId, setEditingGarnishmentId] = useState<string | null>(null);
  const [economicActivities, setEconomicActivities] = useState<any[]>([]);
  const [currentInsuranceRates, setCurrentInsuranceRates] = useState<any | null>(null);
  const [editingEmployment, setEditingEmployment] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    if (!companyId || isNew) return;
    loadEmployee();
  }, [companyId, employeeId]);

  useEffect(() => {
    if (!companyId || isNew || !employeeId) return;
    if (tab === 'employment') { loadEmployments(); loadEconomicActivities(); }
    if (tab === 'absences') loadAbsences();
    if (tab === 'leave') loadLeave();
    if (tab === 'payItems') { loadPayItems(); loadDeductions(); }
    if (tab === 'garnishments') loadGarnishments();
  }, [tab, companyId, employeeId]);

  const loadEmployee = async () => {
    if (!companyId || !employeeId) return;
    const data = await employeeApi.getById(companyId, employeeId);
    setEmployee(data);
  };

  const loadGarnishments = async () => {
    if (!companyId || !employeeId) return;
    const data = await garnishmentApi.getAll(companyId, employeeId);
    setGarnishments(data);
  };

  const loadEmployments = async () => {
    if (!companyId || !employeeId) return;
    const data = await employmentApi.getAll(companyId, employeeId);
    setEmployments(data);
  };

  const loadEconomicActivities = async () => {
    if (!companyId) return;
    const year = new Date().getFullYear();
    const [activities, rates] = await Promise.all([
      economicActivityApi.getAll(companyId, `year=${year}&active=true`),
      insuranceApi.getRates(companyId, year),
    ]);
    setEconomicActivities(activities);
    if (rates.length > 0) setCurrentInsuranceRates(rates[0]);
  };

  const loadAbsences = async () => {
    if (!companyId || !employeeId) return;
    const data = await absenceApi.getAll(companyId, employeeId);
    setAbsences(data);
  };

  const handleSaveAbsence = async () => {
    if (!companyId || !employeeId || !absenceForm.fromDate || !absenceForm.toDate) {
      alert('Моля, попълнете дати.');
      return;
    }
    try {
      // Basic auto-calculation of working days (naive: doesn't skip weekends yet)
      const start = new Date(absenceForm.fromDate);
      const end = new Date(absenceForm.toDate);
      let count = 0;
      let cur = new Date(start);
      while (cur <= end) {
        if (cur.getDay() !== 0 && cur.getDay() !== 6) count++;
        cur.setDate(cur.getDate() + 1);
      }
      
      const payload = {
        ...absenceForm,
        workingDays: count,
        calendarDays: Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)) + 1,
        typeName: absenceTypes.find(t => t.id === absenceForm.type)?.label
      };

      await absenceApi.create(companyId, employeeId, payload);
      setShowAbsenceForm(false);
      setAbsenceForm({ type: 'PAID_ANNUAL', fromDate: '', toDate: '', status: 'APPROVED' });
      loadAbsences();
    } catch (e: any) {
      alert(`Грешка: ${e.message}`);
    }
  };

  const handleExportNssi = async () => {
    if (!companyId || !employeeId) return;
    try {
      const blob = await nssiApi.exportPril9(companyId, employeeId);
      if (blob.size === 0) {
        alert('Няма намерени болнични за експорт.');
        return;
      }
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `Pril9_${employeeId}.xml`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
    } catch (err: any) {
      alert('Грешка при експорт: ' + err.message);
    }
  };

  const absenceTypes = [
    { id: 'PAID_ANNUAL', label: 'Платен годишен отпуск' },
    { id: 'UNPAID_EXP', label: 'Неплатен отпуск със стаж' },
    { id: 'UNPAID_NO_EXP', label: 'Неплатен отпуск без стаж' },
    { id: 'SICK_GENERAL', label: 'Болничен (Общо заболяване)' },
    { id: 'SICK_CHILD', label: 'Болничен (Гледане на дете)' },
    { id: 'PREGNANCY', label: 'Бременност и раждане' },
  ];

  const loadLeave = async () => {
    if (!companyId || !employeeId) return;
    const year = new Date().getFullYear();
    const data = await leaveEntitlementApi.getAll(companyId, employeeId, `year=${year}`);
    setLeave(data);
  };

  const loadPayItems = async () => {
    if (!companyId || !employeeId) return;
    const data = await employeePayItemApi.getAll(companyId, employeeId);
    setPayItems(data);
  };

  const loadDeductions = async () => {
    if (!companyId || !employeeId) return;
    const data = await employeeDeductionApi.getAll(companyId, employeeId);
    setDeductions(data);
  };

  const handleSavePayItem = async () => {
    if (!companyId || !employeeId) return;
    try {
      if (editingPayItemId) {
        await employeePayItemApi.update(companyId, employeeId, editingPayItemId, payItemForm);
      } else {
        await employeePayItemApi.create(companyId, employeeId, payItemForm);
      }
      setShowPayItemForm(false);
      setEditingPayItemId(null);
      setPayItemForm({ payItemCode: '', payItemName: '', type: 'FIXED', value: 0, fromYear: new Date().getFullYear(), fromMonth: 1, toYear: 0, toMonth: 0, active: true });
      loadPayItems();
    } catch (e: any) { alert(`Грешка: ${e.message}`); }
  };

  const handleDeletePayItem = async (id: string) => {
    if (!companyId || !employeeId || !confirm('Изтриване на перото?')) return;
    await employeePayItemApi.delete(companyId, employeeId, id);
    loadPayItems();
  };

  const handleSaveDeduction = async () => {
    if (!companyId || !employeeId) return;
    try {
      if (editingDeductionId) {
        await employeeDeductionApi.update(companyId, employeeId, editingDeductionId, deductionForm);
      } else {
        await employeeDeductionApi.create(companyId, employeeId, deductionForm);
      }
      setShowDeductionForm(false);
      setEditingDeductionId(null);
      setDeductionForm({ deductionCode: '', deductionName: '', amount: 0, fromYear: new Date().getFullYear(), fromMonth: 1, toYear: 0, toMonth: 0, active: true });
      loadDeductions();
    } catch (e: any) { alert(`Грешка: ${e.message}`); }
  };

  const handleDeleteDeduction = async (id: string) => {
    if (!companyId || !employeeId || !confirm('Изтриване на удръжката?')) return;
    await employeeDeductionApi.delete(companyId, employeeId, id);
    loadDeductions();
  };

  const handleSaveGarnishment = async () => {
    if (!companyId || !employeeId) return;
    try {
      if (editingGarnishmentId) {
        await garnishmentApi.update(companyId, employeeId, editingGarnishmentId, garnishmentForm);
      } else {
        await garnishmentApi.create(companyId, employeeId, garnishmentForm);
      }
      setShowGarnishmentForm(false);
      setEditingGarnishmentId(null);
      setGarnishmentForm({ type: 'CHSI', description: '', creditorName: '', bailiffName: '', totalAmount: null, paidAmount: 0, monthlyAmount: null, priority: 1, startDate: new Date().toISOString().split('T')[0], active: true, hasChildren: false });
      loadGarnishments();
    } catch (e: any) { alert(`Грешка: ${e.message}`); }
  };

  const handleDeleteGarnishment = async (id: string) => {
    if (!companyId || !employeeId || !confirm('Изтриване на запора?')) return;
    await garnishmentApi.delete(companyId, employeeId, id);
    loadGarnishments();
  };

  const handleSave = async () => {
    if (!companyId) return;
    setSaving(true);
    setMessage('');
    try {
      if (isNew) {
        const created = await employeeApi.create(companyId, employee);
        navigate(`/personnel/employees/${created.id}`, { replace: true });
        setMessage('Служителят е създаден.');
      } else if (employeeId) {
        await employeeApi.update(companyId, employeeId, employee);
        setMessage('Данните са записани.');
      }
    } catch (e: any) {
      setMessage(`Грешка: ${e.message}`);
    }
    setSaving(false);
  };

  const set = (field: string, value: any) => setEmployee(prev => ({ ...prev, [field]: value }));

  if (!companyId) return <div className="page"><p>Моля, изберете фирма.</p></div>;

  return (
    <div className="page">
      <div className="page-header-row">
        <button className="btn-back" onClick={() => navigate('/personnel/employees')}>&larr; Назад</button>
        <h1>{isNew ? 'Нов служител' : `${employee.firstName || ''} ${employee.lastName || ''}`}</h1>
      </div>

      <div className="tabs">
        <button className={tab === 'personal' ? 'active' : ''} onClick={() => setTab('personal')}>Лични данни</button>
        {!isNew && <>
          <button className={tab === 'employment' ? 'active' : ''} onClick={() => setTab('employment')}>Трудови договори</button>
          <button className={tab === 'absences' ? 'active' : ''} onClick={() => setTab('absences')}>Отсъствия</button>
          <button className={tab === 'leave' ? 'active' : ''} onClick={() => setTab('leave')}>Отпуски</button>
          <button className={tab === 'payItems' ? 'active' : ''} onClick={() => setTab('payItems')}>Пера / Удръжки</button>
          <button className={tab === 'garnishments' ? 'active' : ''} onClick={() => setTab('garnishments')}>Запори</button>
        </>}
      </div>

      {tab === 'personal' && (
        <>
          <div className="form-section">
            <h3>Идентификация</h3>
            <div className="form-grid">
              <label>ЕГН <input value={employee.egn || ''} onChange={e => set('egn', e.target.value)} /></label>
              <label>ЛНЧ <input value={employee.lnch || ''} onChange={e => set('lnch', e.target.value)} /></label>
              <label>Име <input value={employee.firstName || ''} onChange={e => set('firstName', e.target.value)} /></label>
              <label>Презиме <input value={employee.middleName || ''} onChange={e => set('middleName', e.target.value)} /></label>
              <label>Фамилия <input value={employee.lastName || ''} onChange={e => set('lastName', e.target.value)} /></label>
              <label>Дата на раждане <input type="date" value={employee.birthDate || ''} onChange={e => set('birthDate', e.target.value)} /></label>
              <label>Пол
                <select value={employee.gender || 'M'} onChange={e => set('gender', e.target.value)}>
                  <option value="M">Мъж</option>
                  <option value="F">Жена</option>
                </select>
              </label>
              <label>Гражданство <input value={employee.citizenship || ''} onChange={e => set('citizenship', e.target.value)} /></label>
            </div>
          </div>

          <div className="form-section">
            <h3>Документ за самоличност</h3>
            <div className="form-grid">
              <label>Номер на ЛК <input value={employee.idCardNumber || ''} onChange={e => set('idCardNumber', e.target.value)} /></label>
              <label>Дата на издаване <input type="date" value={employee.idCardDate || ''} onChange={e => set('idCardDate', e.target.value)} /></label>
              <label>Издадена от <input value={employee.idCardIssuedBy || ''} onChange={e => set('idCardIssuedBy', e.target.value)} /></label>
            </div>
          </div>

          <div className="form-section">
            <h3>Постоянен адрес</h3>
            <div className="form-grid">
              <label>Адрес <input value={employee.permanentAddress || ''} onChange={e => set('permanentAddress', e.target.value)} /></label>
              <label>Град <input value={employee.permanentCity || ''} onChange={e => set('permanentCity', e.target.value)} /></label>
              <label>Пощенски код <input value={employee.permanentPostalCode || ''} onChange={e => set('permanentPostalCode', e.target.value)} /></label>
              <label>Община <input value={employee.permanentMunicipality || ''} onChange={e => set('permanentMunicipality', e.target.value)} /></label>
              <label>Област <input value={employee.permanentRegion || ''} onChange={e => set('permanentRegion', e.target.value)} /></label>
              <label>Настоящ адрес <input value={employee.currentAddress || ''} onChange={e => set('currentAddress', e.target.value)} /></label>
            </div>
          </div>

          <div className="form-section">
            <h3>Контакти</h3>
            <div className="form-grid">
              <label>Телефон <input value={employee.phone || ''} onChange={e => set('phone', e.target.value)} /></label>
              <label>Email <input value={employee.email || ''} onChange={e => set('email', e.target.value)} /></label>
            </div>
          </div>

          <div className="form-section">
            <h3>Образование</h3>
            <div className="form-grid">
              <label>Степен
                <select value={employee.educationCode || 4} onChange={e => set('educationCode', Number(e.target.value))}>
                  {Object.entries(educationLabels).map(([k, v]) => (
                    <option key={k} value={k}>{v}</option>
                  ))}
                </select>
              </label>
              <label>Специалност <input value={employee.specialty || ''} onChange={e => set('specialty', e.target.value)} /></label>
              <label>Учебно заведение <input value={employee.school || ''} onChange={e => set('school', e.target.value)} /></label>
              <label>Диплома No <input value={employee.diplomaNumber || ''} onChange={e => set('diplomaNumber', e.target.value)} /></label>
            </div>
          </div>

          <div className="form-section">
            <h3>Семейно положение и банка</h3>
            <div className="form-grid">
              <label>Семейно положение <input value={employee.maritalStatus || ''} onChange={e => set('maritalStatus', e.target.value)} /></label>
              <label>Брой деца <input type="number" value={employee.childrenCount || 0} onChange={e => set('childrenCount', Number(e.target.value))} /></label>
              <label>IBAN <input value={employee.iban || ''} onChange={e => set('iban', e.target.value)} /></label>
              <label>BIC <input value={employee.bic || ''} onChange={e => set('bic', e.target.value)} /></label>
            </div>
          </div>

          <div className="form-actions">
            <button onClick={handleSave} disabled={saving}>{saving ? 'Записване...' : 'Запази'}</button>
            {message && <span className="message">{message}</span>}
          </div>
        </>
      )}

      {tab === 'employment' && (
        <div className="form-section">
          <h3>Трудови договори</h3>
          <table className="data-table">
            <thead>
              <tr>
                <th>No</th>
                <th>Дата</th>
                <th>Длъжност</th>
                <th>Осн. заплата</th>
                <th>КИД / ТЗПБ</th>
                <th>Вид</th>
                <th>Статус</th>
                <th>Действия</th>
              </tr>
            </thead>
            <tbody>
              {employments.map(emp => {
                const kidActivity = economicActivities.find(a => a.code === emp.kidCode);
                return (
                  <tr key={emp.id}>
                    <td>{emp.contractNumber}</td>
                    <td>{emp.contractDate}</td>
                    <td>{emp.jobTitle}</td>
                    <td>{emp.baseSalary?.toFixed(2)}</td>
                    <td>
                      {kidActivity
                        ? <span>{emp.kidCode} - {kidActivity.name} <span style={{ color: '#1565c0', fontWeight: 600 }}>({kidActivity.tzpbPercent}%)</span></span>
                        : emp.kidCode || '—'}
                    </td>
                    <td>{emp.contractType}</td>
                    <td>
                      <span className={`status-badge ${emp.current ? 'active' : 'inactive'}`}>
                        {emp.current ? 'Активен' : 'Прекратен'}
                      </span>
                    </td>
                    <td>
                      <button className="btn-small" onClick={() => setEditingEmployment(editingEmployment === emp.id ? null : emp.id)}>
                        {editingEmployment === emp.id ? 'Затвори' : 'Детайли'}
                      </button>
                    </td>
                  </tr>
                );
              })}
              {employments.length === 0 && <tr><td colSpan={8}>Няма договори.</td></tr>}
            </tbody>
          </table>

          {employments.filter(e => e.id === editingEmployment).map(emp => (
            <EmploymentDetailForm
              key={emp.id}
              employment={emp}
              economicActivities={economicActivities}
              insuranceRates={currentInsuranceRates}
              companyId={companyId}
              onSave={async (updated) => {
                if (!companyId || !employeeId) return;
                await employmentApi.update(companyId, employeeId, emp.id, updated);
                loadEmployments();
                setMessage('Договорът е обновен.');
              }}
            />
          ))}
        </div>
      )}

      {tab === 'absences' && (
        <div className="form-section">
          <div className="section-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '15px' }}>
            <h3>Отсъствия</h3>
            <div style={{ display: 'flex', gap: '10px' }}>
              <button className="btn-small" style={{ background: '#2e7d32' }} onClick={handleExportNssi}>
                Експорт за НОИ (Пр. 9)
              </button>
              <button className="btn-small" onClick={() => setShowAbsenceForm(!showAbsenceForm)}>
                {showAbsenceForm ? 'Отказ' : '+ Добави отсъствие'}
              </button>
            </div>
          </div>

          {showAbsenceForm && (
            <div className="form-card" style={{ background: '#f9f9f9', padding: '15px', borderRadius: '4px', marginBottom: '20px', border: '1px solid #eee' }}>
              <div className="form-grid">
                <label>Вид
                  <select value={absenceForm.type} onChange={e => setAbsenceForm({ ...absenceForm, type: e.target.value })}>
                    {absenceTypes.map(t => <option key={t.id} value={t.id}>{t.label}</option>)}
                  </select>
                </label>
                <label>От дата <input type="date" value={absenceForm.fromDate} onChange={e => setAbsenceForm({ ...absenceForm, fromDate: e.target.value })} /></label>
                <label>До дата <input type="date" value={absenceForm.toDate} onChange={e => setAbsenceForm({ ...absenceForm, toDate: e.target.value })} /></label>
                <label>Статус
                  <select value={absenceForm.status} onChange={e => setAbsenceForm({ ...absenceForm, status: e.target.value })}>
                    <option value="REQUESTED">Заявено</option>
                    <option value="APPROVED">Одобрено</option>
                  </select>
                </label>
                {absenceForm.type?.startsWith('SICK') && (
                  <label>No на болничен лист <input placeholder="12 цифри" onChange={e => setAbsenceForm({ ...absenceForm, sickLeaveData: { ...absenceForm.sickLeaveData, chartNumber: e.target.value } })} /></label>
                )}
                <label>Заповед No <input value={absenceForm.orderNumber || ''} onChange={e => setAbsenceForm({ ...absenceForm, orderNumber: e.target.value })} /></label>
              </div>
              <div style={{ marginTop: '10px' }}>
                <button onClick={handleSaveAbsence}>Запази отсъствие</button>
              </div>
            </div>
          )}

          <table className="data-table">
            <thead>
              <tr>
                <th>Вид</th>
                <th>От</th>
                <th>До</th>
                <th>Раб. дни</th>
                <th>Раб/НОИ</th>
                <th>Статус</th>
                <th>Заповед</th>
              </tr>
            </thead>
            <tbody>
              {absences.map(abs => (
                <tr key={abs.id}>
                  <td>{abs.typeName || abs.type}</td>
                  <td>{abs.fromDate}</td>
                  <td>{abs.toDate}</td>
                  <td>{abs.workingDays}</td>
                  <td>{abs.type?.startsWith('SICK') ? `${abs.employerDays}/${abs.nssiDays}` : '—'}</td>
                  <td>{abs.status}</td>
                  <td>{abs.orderNumber}</td>
                </tr>
              ))}
              {absences.length === 0 && <tr><td colSpan={6}>Няма отсъствия.</td></tr>}
            </tbody>
          </table>
        </div>
      )}

      {tab === 'leave' && (
        <div className="form-section">
          <h3>Полагаеми отпуски ({new Date().getFullYear()} г.)</h3>
          <table className="data-table">
            <thead>
              <tr>
                <th>Вид</th>
                <th>Полагаеми</th>
                <th>Използвани</th>
                <th>Остатък</th>
              </tr>
            </thead>
            <tbody>
              {leave.map(le => (
                <tr key={le.id}>
                  <td colSpan={4} style={{ padding: 0 }}>
                    <table className="data-table" style={{ margin: 0 }}>
                      <tbody>
                        <tr><td>Основен (чл.155 КТ)</td><td>{le.basicLeaveDays}</td><td>{le.basicLeaveUsed}</td><td>{le.basicLeaveRemaining}</td></tr>
                        <tr><td>Допълнителен (чл.156 ал.1)</td><td>{le.additionalLeaveDays}</td><td>{le.additionalLeaveUsed}</td><td>{le.additionalLeaveDays - le.additionalLeaveUsed}</td></tr>
                        <tr><td>Ненормиран (чл.156 ал.2)</td><td>{le.irregularLeaveDays}</td><td>{le.irregularLeaveUsed}</td><td>{le.irregularLeaveDays - le.irregularLeaveUsed}</td></tr>
                        <tr><td>По споразумение (чл.156а)</td><td>{le.agreedLeaveDays}</td><td>{le.agreedLeaveUsed}</td><td>{le.agreedLeaveDays - le.agreedLeaveUsed}</td></tr>
                        <tr><td>Прехвърлени от мин. година</td><td>{le.carriedOverDays}</td><td>—</td><td>—</td></tr>
                        <tr style={{ fontWeight: 600 }}><td>Общо</td><td>{le.totalEntitled}</td><td>{le.totalUsed}</td><td>{le.totalEntitled - le.totalUsed}</td></tr>
                      </tbody>
                    </table>
                  </td>
                </tr>
              ))}
              {leave.length === 0 && <tr><td colSpan={4}>Няма данни за отпуски.</td></tr>}
            </tbody>
          </table>
        </div>
      )}

      {tab === 'payItems' && (
        <>
          {/* Пера за възнаграждение */}
          <div className="form-section">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '15px' }}>
              <h3>Пера за възнаграждение</h3>
              <button className="btn-small" onClick={() => { setShowPayItemForm(!showPayItemForm); setEditingPayItemId(null); setPayItemForm({ payItemCode: '', payItemName: '', type: 'FIXED', value: 0, fromYear: new Date().getFullYear(), fromMonth: 1, toYear: 0, toMonth: 0, active: true }); }}>
                {showPayItemForm ? 'Отказ' : '+ Добави перо'}
              </button>
            </div>

            {showPayItemForm && (
              <div style={{ background: '#f9f9f9', padding: '15px', borderRadius: '4px', marginBottom: '20px', border: '1px solid #eee' }}>
                <div className="form-grid">
                  <label>Код <input value={payItemForm.payItemCode} onChange={e => setPayItemForm({ ...payItemForm, payItemCode: e.target.value })} /></label>
                  <label>Наименование <input value={payItemForm.payItemName} onChange={e => setPayItemForm({ ...payItemForm, payItemName: e.target.value })} /></label>
                  <label>Тип
                    <select value={payItemForm.type} onChange={e => setPayItemForm({ ...payItemForm, type: e.target.value })}>
                      <option value="FIXED">Фиксирана сума</option>
                      <option value="PERCENT">Процент</option>
                      <option value="PER_UNIT">За единица</option>
                    </select>
                  </label>
                  <label>Стойност <input type="number" step="0.01" value={payItemForm.value} onChange={e => setPayItemForm({ ...payItemForm, value: parseFloat(e.target.value) || 0 })} /></label>
                  <label>От година <input type="number" value={payItemForm.fromYear} onChange={e => setPayItemForm({ ...payItemForm, fromYear: parseInt(e.target.value) || 0 })} /></label>
                  <label>От месец <input type="number" min="1" max="12" value={payItemForm.fromMonth} onChange={e => setPayItemForm({ ...payItemForm, fromMonth: parseInt(e.target.value) || 1 })} /></label>
                  <label>До година (0=безср.) <input type="number" value={payItemForm.toYear} onChange={e => setPayItemForm({ ...payItemForm, toYear: parseInt(e.target.value) || 0 })} /></label>
                  <label>До месец (0=безср.) <input type="number" min="0" max="12" value={payItemForm.toMonth} onChange={e => setPayItemForm({ ...payItemForm, toMonth: parseInt(e.target.value) || 0 })} /></label>
                </div>
                <div style={{ marginTop: '10px' }}>
                  <button onClick={handleSavePayItem}>{editingPayItemId ? 'Обнови' : 'Запази'}</button>
                </div>
              </div>
            )}

            <table className="data-table">
              <thead>
                <tr>
                  <th>Код</th>
                  <th>Наименование</th>
                  <th>Тип</th>
                  <th>Стойност</th>
                  <th>Период</th>
                  <th>Статус</th>
                  <th>Действия</th>
                </tr>
              </thead>
              <tbody>
                {payItems.map(pi => (
                  <tr key={pi.id}>
                    <td>{pi.payItemCode}</td>
                    <td>{pi.payItemName}</td>
                    <td>{pi.type === 'PERCENT' ? '%' : pi.type === 'PER_UNIT' ? 'За ед.' : 'Фикс.'}</td>
                    <td>{pi.type === 'PERCENT' ? `${pi.value}%` : Number(pi.value).toFixed(2)}</td>
                    <td>{pi.fromMonth}/{pi.fromYear} - {pi.toYear > 0 ? `${pi.toMonth}/${pi.toYear}` : 'безсрочно'}</td>
                    <td><span className={`status-badge ${pi.active ? 'active' : 'inactive'}`}>{pi.active ? 'Активно' : 'Неактивно'}</span></td>
                    <td>
                      <button className="btn-small" style={{ marginRight: '5px' }} onClick={() => { setPayItemForm(pi); setEditingPayItemId(pi.id); setShowPayItemForm(true); }}>Редакция</button>
                      <button className="btn-small" style={{ background: '#c62828' }} onClick={() => handleDeletePayItem(pi.id)}>Изтрий</button>
                    </td>
                  </tr>
                ))}
                {payItems.length === 0 && <tr><td colSpan={7}>Няма добавени пера.</td></tr>}
              </tbody>
            </table>
          </div>

          {/* Удръжки */}
          <div className="form-section">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '15px' }}>
              <h3>Удръжки</h3>
              <button className="btn-small" onClick={() => { setShowDeductionForm(!showDeductionForm); setEditingDeductionId(null); setDeductionForm({ deductionCode: '', deductionName: '', amount: 0, fromYear: new Date().getFullYear(), fromMonth: 1, toYear: 0, toMonth: 0, active: true }); }}>
                {showDeductionForm ? 'Отказ' : '+ Добави удръжка'}
              </button>
            </div>

            {showDeductionForm && (
              <div style={{ background: '#f9f9f9', padding: '15px', borderRadius: '4px', marginBottom: '20px', border: '1px solid #eee' }}>
                <div className="form-grid">
                  <label>Код <input value={deductionForm.deductionCode} onChange={e => setDeductionForm({ ...deductionForm, deductionCode: e.target.value })} /></label>
                  <label>Наименование <input value={deductionForm.deductionName} onChange={e => setDeductionForm({ ...deductionForm, deductionName: e.target.value })} /></label>
                  <label>Сума <input type="number" step="0.01" value={deductionForm.amount} onChange={e => setDeductionForm({ ...deductionForm, amount: parseFloat(e.target.value) || 0 })} /></label>
                  <label>От година <input type="number" value={deductionForm.fromYear} onChange={e => setDeductionForm({ ...deductionForm, fromYear: parseInt(e.target.value) || 0 })} /></label>
                  <label>От месец <input type="number" min="1" max="12" value={deductionForm.fromMonth} onChange={e => setDeductionForm({ ...deductionForm, fromMonth: parseInt(e.target.value) || 1 })} /></label>
                  <label>До година (0=безср.) <input type="number" value={deductionForm.toYear} onChange={e => setDeductionForm({ ...deductionForm, toYear: parseInt(e.target.value) || 0 })} /></label>
                  <label>До месец (0=безср.) <input type="number" min="0" max="12" value={deductionForm.toMonth} onChange={e => setDeductionForm({ ...deductionForm, toMonth: parseInt(e.target.value) || 0 })} /></label>
                </div>
                <div style={{ marginTop: '10px' }}>
                  <button onClick={handleSaveDeduction}>{editingDeductionId ? 'Обнови' : 'Запази'}</button>
                </div>
              </div>
            )}

            <table className="data-table">
              <thead>
                <tr>
                  <th>Код</th>
                  <th>Наименование</th>
                  <th>Сума</th>
                  <th>Период</th>
                  <th>Статус</th>
                  <th>Действия</th>
                </tr>
              </thead>
              <tbody>
                {deductions.map(d => (
                  <tr key={d.id}>
                    <td>{d.deductionCode}</td>
                    <td>{d.deductionName}</td>
                    <td>{Number(d.amount).toFixed(2)}</td>
                    <td>{d.fromMonth}/{d.fromYear} - {d.toYear > 0 ? `${d.toMonth}/${d.toYear}` : 'безсрочно'}</td>
                    <td><span className={`status-badge ${d.active ? 'active' : 'inactive'}`}>{d.active ? 'Активна' : 'Неактивна'}</span></td>
                    <td>
                      <button className="btn-small" style={{ marginRight: '5px' }} onClick={() => { setDeductionForm(d); setEditingDeductionId(d.id); setShowDeductionForm(true); }}>Редакция</button>
                      <button className="btn-small" style={{ background: '#c62828' }} onClick={() => handleDeleteDeduction(d.id)}>Изтрий</button>
                    </td>
                  </tr>
                ))}
                {deductions.length === 0 && <tr><td colSpan={6}>Няма добавени удръжки.</td></tr>}
              </tbody>
            </table>
          </div>
        </>
      )}

      {tab === 'garnishments' && (
        <div className="form-section">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '15px' }}>
            <h3>Запори (ГПК чл. 446)</h3>
            <button className="btn-small" onClick={() => { setShowGarnishmentForm(!showGarnishmentForm); setEditingGarnishmentId(null); setGarnishmentForm({ type: 'CHSI', description: '', creditorName: '', bailiffName: '', totalAmount: null, paidAmount: 0, monthlyAmount: null, priority: 1, startDate: new Date().toISOString().split('T')[0], active: true, hasChildren: employee.childrenCount! > 0 }); }}>
              {showGarnishmentForm ? 'Отказ' : '+ Добави запор'}
            </button>
          </div>

          {showGarnishmentForm && (
            <div style={{ background: '#f9f9f9', padding: '15px', borderRadius: '4px', marginBottom: '20px', border: '1px solid #eee' }}>
              <div className="form-grid">
                <label>Тип
                  <select value={garnishmentForm.type} onChange={e => setGarnishmentForm({ ...garnishmentForm, type: e.target.value as any })}>
                    <option value="CHSI">ЧСИ (Частен съдия изпълнител)</option>
                    <option value="PUBLIC">Публичен изпълнител</option>
                    <option value="ALIMONY">Издръжка</option>
                  </select>
                </label>
                <label>Описание/Дело № <input value={garnishmentForm.description} onChange={e => setGarnishmentForm({ ...garnishmentForm, description: e.target.value })} /></label>
                <label>Взискател <input value={garnishmentForm.creditorName} onChange={e => setGarnishmentForm({ ...garnishmentForm, creditorName: e.target.value })} /></label>
                <label>Изпълнител <input value={garnishmentForm.bailiffName} onChange={e => setGarnishmentForm({ ...garnishmentForm, bailiffName: e.target.value })} /></label>
                <label>Обща сума <input type="number" step="0.01" value={garnishmentForm.totalAmount || ''} onChange={e => setGarnishmentForm({ ...garnishmentForm, totalAmount: parseFloat(e.target.value) || null })} /></label>
                <label>Удържана сума <input type="number" step="0.01" value={garnishmentForm.paidAmount} onChange={e => setGarnishmentForm({ ...garnishmentForm, paidAmount: parseFloat(e.target.value) || 0 })} /></label>
                <label>Месечна сума (за издръжка) <input type="number" step="0.01" value={garnishmentForm.monthlyAmount || ''} onChange={e => setGarnishmentForm({ ...garnishmentForm, monthlyAmount: parseFloat(e.target.value) || null })} /></label>
                <label>Приоритет <input type="number" value={garnishmentForm.priority} onChange={e => setGarnishmentForm({ ...garnishmentForm, priority: parseInt(e.target.value) || 1 })} /></label>
                <label>Начална дата <input type="date" value={garnishmentForm.startDate} onChange={e => setGarnishmentForm({ ...garnishmentForm, startDate: e.target.value })} /></label>
                <label style={{ display: 'flex', alignItems: 'center', gap: '10px', flexDirection: 'row' }}>
                  <input type="checkbox" checked={garnishmentForm.hasChildren} onChange={e => setGarnishmentForm({ ...garnishmentForm, hasChildren: e.target.checked })} />
                  Има деца (за ГПК лимит)
                </label>
                <label style={{ display: 'flex', alignItems: 'center', gap: '10px', flexDirection: 'row' }}>
                  <input type="checkbox" checked={garnishmentForm.active} onChange={e => setGarnishmentForm({ ...garnishmentForm, active: e.target.checked })} />
                  Активен
                </label>
              </div>
              <div style={{ marginTop: '10px' }}>
                <button onClick={handleSaveGarnishment}>{editingGarnishmentId ? 'Обнови' : 'Запази'}</button>
              </div>
            </div>
          )}

          <table className="data-table">
            <thead>
              <tr>
                <th>Тип</th>
                <th>Дело / Описание</th>
                <th>Взискател</th>
                <th>Обща сума</th>
                <th>Удържано</th>
                <th>Остатък</th>
                <th>Приоритет</th>
                <th>Статус</th>
                <th>Действия</th>
              </tr>
            </thead>
            <tbody>
              {garnishments.map(g => (
                <tr key={g.id}>
                  <td>{g.type === 'ALIMONY' ? 'Издръжка' : g.type === 'PUBLIC' ? 'Публичен' : 'ЧСИ'}</td>
                  <td>{g.description}</td>
                  <td>{g.creditorName}</td>
                  <td>{g.totalAmount?.toFixed(2) || '—'}</td>
                  <td>{g.paidAmount.toFixed(2)}</td>
                  <td>{g.totalAmount ? (g.totalAmount - g.paidAmount).toFixed(2) : '—'}</td>
                  <td>{g.priority}</td>
                  <td><span className={`status-badge ${g.active ? 'active' : 'inactive'}`}>{g.active ? 'Активен' : 'Приключен'}</span></td>
                  <td>
                    <button className="btn-small" style={{ marginRight: '5px' }} onClick={() => { setGarnishmentForm(g); setEditingGarnishmentId(g.id); setShowGarnishmentForm(true); }}>Редакция</button>
                    <button className="btn-small" style={{ background: '#c62828' }} onClick={() => handleDeleteGarnishment(g.id)}>Изтрий</button>
                  </td>
                </tr>
              ))}
              {garnishments.length === 0 && <tr><td colSpan={9}>Няма активни запори.</td></tr>}
            </tbody>
          </table>
          <div style={{ marginTop: '20px', padding: '15px', background: '#e3f2fd', borderRadius: '4px', fontSize: '14px' }}>
            <strong>Забележка:</strong> Системата автоматично изчислява секвестируемата част от заплатата съгласно чл. 446 от ГПК, като взема предвид минималната работна заплата за периода и наличието на деца. Издръжките се удържат с предимство и не се ограничават от несеквестируемия минимум.
          </div>
        </div>
      )}
    </div>
  );
}
