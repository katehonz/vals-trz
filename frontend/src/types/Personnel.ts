export interface Department {
  id: string;
  tenantId: string;
  code: string;
  name: string;
  parentId: string | null;
  managerId: string | null;
  sortOrder: number;
}

export interface Employee {
  id: string;
  tenantId: string;
  egn: string;
  lnch: string;
  firstName: string;
  middleName: string;
  lastName: string;
  birthDate: string;
  gender: string;
  citizenship: string;
  idCardNumber: string;
  idCardDate: string;
  idCardIssuedBy: string;
  permanentAddress: string;
  permanentCity: string;
  permanentPostalCode: string;
  permanentMunicipality: string;
  permanentRegion: string;
  currentAddress: string;
  phone: string;
  email: string;
  educationCode: number;
  specialty: string;
  school: string;
  diplomaNumber: string;
  degree: string;
  maritalStatus: string;
  childrenCount: number;
  iban: string;
  bic: string;
  photoPath: string;
  active: boolean;
}

export interface Employment {
  id: string;
  tenantId: string;
  employeeId: string;
  departmentId: string;
  contractNumber: string;
  contractDate: string;
  startDate: string;
  contractBasis: string;
  contractType: string;
  contractEndDate: string;
  contractSpecificText: string;
  jobTitle: string;
  nkpdCode: string;
  kidCode: string;
  workplace: string;
  workPhone: string;
  workTimeType: string;
  workScheduleCode: string;
  insuranceType: string;
  insuredType: string;
  baseSalary: number;
  paymentType: string;
  workCardType: string;
  previousExperienceYears: number;
  seniorityBonusYears: number;
  seniorityBonusPercent: number;
  personnelGroup: number;
  personnelType: string;
  workingHoursPerDay: number;
  noticePeriodDays: number;
  basicAnnualLeaveDays: number;
  additionalAnnualLeaveDays: number;
  additionalPayItemCode: string;
  additionalPayItemName: string;
  additionalPayAmount: number;
  pensioner: boolean;
  disability50Plus: boolean;
  zgvrsArticle7: boolean;
  zgvrsFromDate: string;
  text1: string;
  text2: string;
  text3: string;
  text4: string;
  current: boolean;
  terminationDate: string;
}

export interface Absence {
  id: string;
  tenantId: string;
  employeeId: string;
  type: string;
  typeName: string;
  fromDate: string;
  toDate: string;
  workingDays: number;
  calendarDays: number;
  employerDays: number;
  nssiDays: number;
  status: string;
  sickLeaveData: Record<string, any> | null;
  orderNumber: string;
  orderDate: string;
  notes: string;
}

export interface LeaveEntitlement {
  id: string;
  tenantId: string;
  employeeId: string;
  year: number;
  basicLeaveDays: number;
  basicLeaveUsed: number;
  basicLeaveRemaining: number;
  additionalLeaveDays: number;
  additionalLeaveUsed: number;
  irregularLeaveDays: number;
  irregularLeaveUsed: number;
  agreedLeaveDays: number;
  agreedLeaveUsed: number;
  carriedOverDays: number;
  totalEntitled: number;
  totalUsed: number;
}

export type GarnishmentType = 'CHSI' | 'PUBLIC' | 'ALIMONY';

export interface Garnishment {
  id: string;
  employeeId: string;
  type: GarnishmentType;
  description: string;
  creditorName: string;
  bailiffName: string;
  totalAmount: number | null;
  paidAmount: number;
  monthlyAmount: number | null;
  priority: number;
  startDate: string;
  endDate: string | null;
  active: boolean;
  hasChildren: boolean;
}
