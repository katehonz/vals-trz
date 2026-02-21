import { NavLink } from 'react-router-dom';

const menuItems = [
  {
    group: '',
    items: [
      { path: '/dashboard', label: '🏠 Начало' },
    ],
  },
  {
    group: 'Данни и настройки',
    items: [
      { path: '/settings/company', label: 'Фирмени данни' },
      { path: '/settings/personnel-types', label: 'Видове персонал' },
      { path: '/settings/calendar', label: 'Времеви данни' },
      { path: '/settings/work-schedules', label: 'Часови схеми' },
      { path: '/settings/shift-schedules', label: 'Графици на смени' },
      { path: '/settings/economic-activities', label: 'КИД / ТЗПБ' },
      { path: '/settings/insurance', label: 'Осигурителни вноски' },
      { path: '/settings/insurance-thresholds', label: 'МОД' },
      { path: '/settings/pay-items', label: 'Пера за възнаграждение' },
      { path: '/settings/deduction-items', label: 'Пера за удръжки' },
      { path: '/settings/nomenclatures', label: 'Номенклатури' },
      { path: '/settings/seniority', label: 'ДТВ за стаж (клас)' },
    ],
  },
  {
    group: 'Структура и персонал',
    items: [
      { path: '/personnel/departments', label: 'Организационна структура' },
      { path: '/personnel/employees', label: 'Служители' },
    ],
  },
  {
    group: 'Документи',
    items: [
      { path: '/documents/templates', label: 'Шаблони' },
      { path: '/documents/generate', label: 'Генериране' },
      { path: '/documents/certificates', label: 'Удостоверения УП-2/3' },
    ],
  },
  {
    group: 'Заплати',
    items: [
      { path: '/payroll', label: 'Месечна ведомост' },
    ],
  },
  {
    group: 'Плащания',
    items: [
      { path: '/payments/bank', label: 'Банкови плащания' },
      { path: '/accounting', label: 'Счетоводни операции' },
    ],
  },
  {
    group: 'Справки',
    items: [
      { path: '/payroll/reports', label: 'Справки за заплати' },
      { path: '/reports/personnel', label: 'Анализ на персонала' },
    ],
  },
  {
    group: 'Декларации НАП',
    items: [
      { path: '/declarations', label: 'Декларации и уведомления' },
    ],
  },
  {
    group: 'Администриране',
    items: [
      { path: '/admin/users', label: 'Потребители' },
      { path: '/admin', label: 'Импорт / Експорт / Одит' },
    ],
  },
];

export default function Sidebar() {
  return (
    <nav className="sidebar">
      <div className="sidebar-header">
        <h2>vals-trz</h2>
        <span className="sidebar-subtitle">Заплати и ЧР</span>
      </div>
      {menuItems.map((group) => (
        <div key={group.group} className="sidebar-group">
          <h3 className="sidebar-group-title">{group.group}</h3>
          <ul>
            {group.items.map((item) => (
              <li key={item.path}>
                <NavLink
                  to={item.path}
                  className={({ isActive }) => isActive ? 'active' : ''}
                >
                  {item.label}
                </NavLink>
              </li>
            ))}
          </ul>
        </div>
      ))}
    </nav>
  );
}
