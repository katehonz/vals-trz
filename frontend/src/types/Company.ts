export interface PersonInfo {
  name: string;
  title: string;
  egn: string;
  citizenship: string;
  idCardNumber: string;
  idCardDate: string;
  idCardIssuedBy: string;
  address: string;
  municipality: string;
  region: string;
}

export interface Company {
  id: string;
  tenantId: string;
  name: string;
  bulstat: string;
  city: string;
  postalCode: string;
  ekatte: string;
  address: string;
  correspondenceAddress: string;
  firmDossierNumber: string;
  firmDossierDescription: string;
  phone: string;
  email: string;
  napTerritorialDirectorate: string;
  napOffice: string;
  noiTerritorialUnit: string;
  budgetOrganization: boolean;
  insuranceFund: boolean;
  insolvencyEligible: boolean;
  cukSystem: boolean;
  disabilityEnterprise: boolean;
  paymentDeadlineDay: number;
  noticePeriodDays: number;
  electronicSubmission: boolean;
  director: PersonInfo;
  napContact: PersonInfo;
  noiContact: PersonInfo;
  hrManagerTitle: string;
  hrManagerName: string;
  chiefAccountantName: string;
  nkidCode: string;
  nkidSerialNumber: number;
  nkpdClassifier: string;
}

export interface PersonnelType {
  id: string;
  tenantId: string;
  number: number;
  name: string;
  nkpdCode: string;
  minInsurableIncome: number;
}

export interface PayItem {
  id: string;
  tenantId: string;
  code: string;
  name: string;
  type: 'PERCENT' | 'PER_UNIT' | 'FIXED' | 'CALCULATED';
  system: boolean;
  active: boolean;
}

export interface DeductionItem {
  id: string;
  tenantId: string;
  code: string;
  name: string;
  type: 'FIXED' | 'CALCULATED';
  system: boolean;
  active: boolean;
}

export interface WorkSchedule {
  id: string;
  tenantId: string;
  code: string;
  name: string;
  hoursPerDay: number;
}

export interface MonthlyCalendar {
  id: string;
  tenantId: string;
  year: number;
  month: number;
  workingDays: number;
  calendarDays: number;
  holidays: number;
  workingHoursPerDay: number;
  totalWorkingHours: number;
  advancePaymentDate: string;
  salaryPaymentDate: string;
}
