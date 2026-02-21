export interface Declaration1Record {
  employeeId: string;
  employeeName: string;
  egn: string;
  fields: string[];
  validationErrors: string[];
}

export interface Declaration6Data {
  companyBulstat: string;
  year: number;
  month: number;
  employeeCount: number;
  totalPensionEmployer: number;
  totalPensionEmployee: number;
  totalSicknessEmployer: number;
  totalSicknessEmployee: number;
  totalUnemploymentEmployer: number;
  totalUnemploymentEmployee: number;
  totalSupplementaryEmployer: number;
  totalSupplementaryEmployee: number;
  totalHealthEmployer: number;
  totalHealthEmployee: number;
  totalWorkAccident: number;
  totalIncomeTax: number;
  grandTotalInsurance: number;
  grandTotalTax: number;
}

export interface Article62Record {
  employeeId: string;
  employeeName: string;
  egn: string;
  eventType: string;
  eventTypeName: string;
  fields: string[];
}

export interface NapSubmission {
  id: string;
  tenantId: string;
  type: string;
  year: number;
  month: number;
  fileName: string;
  fileContent: string;
  recordCount: number;
  status: string;
  correctionCode: number;
  generatedAt: string;
  validationErrors: string[];
  employeeIds: string[];
}

export interface ValidationError {
  employeeId: string;
  employeeName: string;
  field: string;
  message: string;
}
