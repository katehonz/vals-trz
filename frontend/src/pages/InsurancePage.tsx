import { useEffect, useState } from 'react';
import { insuranceApi } from '../api/apiClient';

interface Props {
  companyId: string | null;
}

interface InsuranceRates {
  id: string;
  year: number;
  minimumWage: number;
  maxInsurableIncome: number;
  flatTaxRate: number;
  disabilityTaxExemption: number;
}

interface InsuranceContributions {
  id: string;
  year: number;
  category: string;
  insuredType: string;
  pensionEmployer: number;
  pensionEmployee: number;
  sicknessEmployer: number;
  sicknessEmployee: number;
  unemploymentEmployer: number;
  unemploymentEmployee: number;
  supplementaryPensionEmployer: number;
  supplementaryPensionEmployee: number;
  healthEmployer: number;
  healthEmployee: number;
  workAccidentEmployer: number;
  professionalPensionEmployer: number;
  teacherPensionEmployer: number;
}

const categoryLabels: Record<string, string> = {
  before1960: 'Преди 01.01.1960',
  after1960: 'След 31.12.1959',
};

const insuredTypeLabels: Record<string, string> = {
  '01': '01 - Трета категория',
  '02': '02 - Първа категория',
  '03': '03 - Втора категория',
  '05': '05 - Държавни служители',
  '08': '08 - Учители',
  '10': '10 - Без безработица',
  '12': '12 - Самоосигуряващи се',
  '14': '14 - Без трудово правоотн.',
  '24': '24 - Държ. служители (доп.)',
  '27': '27 - Морски лица',
};

export default function InsurancePage({ companyId }: Props) {
  const now = new Date();
  const [year, setYear] = useState(now.getFullYear());
  const [rates, setRates] = useState<InsuranceRates[]>([]);
  const [contributions, setContributions] = useState<InsuranceContributions[]>([]);
  const [showRatesForm, setShowRatesForm] = useState(false);
  const [ratesForm, setRatesForm] = useState({
    year: now.getFullYear(), minimumWage: 933, maxInsurableIncome: 3750, flatTaxRate: 10, disabilityTaxExemption: 660,
  });

  const loadData = async () => {
    if (!companyId) return;
    const [r, c] = await Promise.all([
      insuranceApi.getRates(companyId, year),
      insuranceApi.getContributions(companyId, year),
    ]);
    setRates(r);
    setContributions(c);
  };

  useEffect(() => { loadData(); }, [companyId, year]);

  const handleCreateRates = async () => {
    if (!companyId) return;
    await insuranceApi.createRates(companyId, ratesForm);
    setShowRatesForm(false);
    await loadData();
  };

  const handleSeedContributions = async () => {
    if (!companyId) return;
    await insuranceApi.seedContributions(companyId, year);
    await loadData();
  };

  if (!companyId) return <div className="page"><p>Моля, изберете фирма.</p></div>;

  return (
    <div className="page">
      <h1>Осигурителни вноски</h1>

      <div className="toolbar">
        <label>Година:
          <select value={year} onChange={(e) => setYear(parseInt(e.target.value))}>
            {[year - 1, year, year + 1].map((y) => (
              <option key={y} value={y}>{y}</option>
            ))}
          </select>
        </label>
      </div>

      <h2>Основни параметри</h2>
      <button onClick={() => { setRatesForm({ ...ratesForm, year }); setShowRatesForm(!showRatesForm); }}>
        {showRatesForm ? 'Отказ' : '+ Добави параметри'}
      </button>

      {showRatesForm && (
        <div className="form-inline">
          <input type="number" placeholder="МРЗ" value={ratesForm.minimumWage}
            onChange={(e) => setRatesForm({ ...ratesForm, minimumWage: parseFloat(e.target.value) || 0 })} />
          <input type="number" placeholder="Макс. осиг. доход" value={ratesForm.maxInsurableIncome}
            onChange={(e) => setRatesForm({ ...ratesForm, maxInsurableIncome: parseFloat(e.target.value) || 0 })} />
          <input type="number" placeholder="Плосък данък %" value={ratesForm.flatTaxRate}
            onChange={(e) => setRatesForm({ ...ratesForm, flatTaxRate: parseFloat(e.target.value) || 0 })} />
          <input type="number" placeholder="Необл. сума инвалид/мес." value={ratesForm.disabilityTaxExemption}
            onChange={(e) => setRatesForm({ ...ratesForm, disabilityTaxExemption: parseFloat(e.target.value) || 0 })} />
          <button onClick={handleCreateRates}>Запази</button>
        </div>
      )}

      <table className="data-table">
        <thead>
          <tr>
            <th>Година</th>
            <th>МРЗ</th>
            <th>Макс. осиг. доход</th>
            <th>Плосък данък %</th>
            <th>Необл. инвалид/мес.</th>
            <th>Необл. инвалид/год.</th>
          </tr>
        </thead>
        <tbody>
          {rates.map((r) => (
            <tr key={r.id}>
              <td>{r.year}</td>
              <td>{r.minimumWage}</td>
              <td>{r.maxInsurableIncome}</td>
              <td>{r.flatTaxRate}%</td>
              <td>{r.disabilityTaxExemption ? `${Number(r.disabilityTaxExemption).toFixed(2)} лв.` : '—'}</td>
              <td>{r.disabilityTaxExemption ? `${(Number(r.disabilityTaxExemption) * 12).toFixed(2)} лв.` : '—'}</td>
            </tr>
          ))}
          {rates.length === 0 && <tr><td colSpan={6}>Няма параметри за {year} г.</td></tr>}
        </tbody>
      </table>

      <h2>Осигурителни проценти</h2>
      <div className="toolbar">
        <button className="btn-success" onClick={handleSeedContributions}>
          Зареди стандартни за {year} г.
        </button>
      </div>

      <table className="data-table">
        <thead>
          <tr>
            <th>Вид осигурен</th>
            <th>Категория</th>
            <th>Пенсия (Р-л / Р-к)</th>
            <th>Болест (Р-л / Р-к)</th>
            <th>Безработица (Р-л / Р-к)</th>
            <th>ДЗПО (Р-л / Р-к)</th>
            <th>Здравно (Р-л / Р-к)</th>
            <th>ТЗПБ</th>
            <th>ППФ</th>
            <th>Учит.</th>
          </tr>
        </thead>
        <tbody>
          {contributions.map((c) => (
            <tr key={c.id}>
              <td>{insuredTypeLabels[c.insuredType] || c.insuredType || '01'}</td>
              <td>{categoryLabels[c.category] || c.category}</td>
              <td>{c.pensionEmployer}% / {c.pensionEmployee}%</td>
              <td>{c.sicknessEmployer}% / {c.sicknessEmployee}%</td>
              <td>{c.unemploymentEmployer}% / {c.unemploymentEmployee}%</td>
              <td>{c.supplementaryPensionEmployer}% / {c.supplementaryPensionEmployee}%</td>
              <td>{c.healthEmployer}% / {c.healthEmployee}%</td>
              <td>{c.workAccidentEmployer}%</td>
              <td>{c.professionalPensionEmployer ? `${c.professionalPensionEmployer}%` : '-'}</td>
              <td>{c.teacherPensionEmployer ? `${c.teacherPensionEmployer}%` : '-'}</td>
            </tr>
          ))}
          {contributions.length === 0 && <tr><td colSpan={10}>Няма проценти за {year} г.</td></tr>}
        </tbody>
      </table>
    </div>
  );
}
