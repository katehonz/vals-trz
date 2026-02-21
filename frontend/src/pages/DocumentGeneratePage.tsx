import { useEffect, useState, useRef } from 'react';
import { documentApi, employeeApi } from '../api/apiClient';
import type { DocumentTemplate } from '../types/Document';

interface Props {
  companyId: string | null;
}

const categories = [
  { value: 'LABOR_CONTRACT', label: 'Трудови договори' },
  { value: 'AMENDMENT', label: 'Доп. споразумения' },
  { value: 'TERMINATION', label: 'Прекратяване' },
  { value: 'LEAVE_ORDER', label: 'Отпуски' },
  { value: 'CERTIFICATE', label: 'Удостоверения' },
];

export default function DocumentGeneratePage({ companyId }: Props) {
  const [category, setCategory] = useState('LABOR_CONTRACT');
  const [templates, setTemplates] = useState<DocumentTemplate[]>([]);
  const [templateId, setTemplateId] = useState('');
  const [employees, setEmployees] = useState<any[]>([]);
  const [employeeId, setEmployeeId] = useState('');
  const [contextId, setContextId] = useState('');
  const [html, setHtml] = useState('');
  const [loading, setLoading] = useState(false);
  const previewRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!companyId) return;
    documentApi.getTemplates(companyId, category).then(list => {
      setTemplates(list);
      setTemplateId(list.length > 0 ? list[0].id : '');
    });
  }, [companyId, category]);

  useEffect(() => {
    if (!companyId) return;
    employeeApi.getAll(companyId, 'active=true').then(setEmployees).catch(() => setEmployees([]));
  }, [companyId]);

  if (!companyId) return <div className="page"><p>Моля, изберете фирма.</p></div>;

  const needsContext = category === 'AMENDMENT' || category === 'TERMINATION' || category === 'LEAVE_ORDER';

  const handleGenerate = () => {
    if (!templateId || !employeeId) return;
    setLoading(true);
    documentApi.generate(companyId, { templateId, employeeId, contextId: contextId || undefined })
      .then(res => setHtml(res.html))
      .catch(() => setHtml('<p style="color:red;">Грешка при генериране.</p>'))
      .finally(() => setLoading(false));
  };

  const handlePrint = () => {
    const content = previewRef.current?.innerHTML;
    if (!content) return;
    const win = window.open('', '_blank');
    if (!win) return;
    win.document.write(`<!DOCTYPE html><html><head><meta charset="utf-8"><title>Документ</title>
      <style>body{margin:2cm;font-family:'Times New Roman',serif;} @media print{body{margin:0;}}</style>
      </head><body>${content}</body></html>`);
    win.document.close();
    win.focus();
    win.print();
  };

  return (
    <div className="page" style={{ maxWidth: 1200 }}>
      <h1>Генериране на документ</h1>

      <div className="form-section">
        <div className="form-grid">
          <label>Категория
            <select value={category} onChange={e => setCategory(e.target.value)}>
              {categories.map(c => (
                <option key={c.value} value={c.value}>{c.label}</option>
              ))}
            </select>
          </label>
          <label>Шаблон
            <select value={templateId} onChange={e => setTemplateId(e.target.value)}>
              {templates.map(t => (
                <option key={t.id} value={t.id}>{t.name}</option>
              ))}
              {templates.length === 0 && <option value="">Няма шаблони</option>}
            </select>
          </label>
          <label>Служител
            <select value={employeeId} onChange={e => setEmployeeId(e.target.value)}>
              <option value="">-- Изберете --</option>
              {employees.map((emp: any) => (
                <option key={emp.id} value={emp.id}>
                  {emp.firstName} {emp.lastName} ({emp.egn})
                </option>
              ))}
            </select>
          </label>
          {needsContext && (
            <label>ID на контекст (ДС/Прекратяване/Отпуск)
              <input value={contextId} onChange={e => setContextId(e.target.value)}
                placeholder="ID от съответната колекция" />
            </label>
          )}
        </div>

        <div className="form-actions">
          <button onClick={handleGenerate} disabled={loading || !templateId || !employeeId}>
            {loading ? 'Генериране...' : 'Генерирай'}
          </button>
          {html && <button onClick={handlePrint} style={{ background: '#2e7d32' }}>Печат</button>}
        </div>
      </div>

      {html && (
        <div className="form-section">
          <h3>Преглед на документа</h3>
          <div ref={previewRef}
            style={{ border: '1px solid #ddd', padding: '2rem', background: 'white', borderRadius: 4 }}
            dangerouslySetInnerHTML={{ __html: html }} />
        </div>
      )}
    </div>
  );
}
