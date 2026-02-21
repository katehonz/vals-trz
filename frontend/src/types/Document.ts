export interface DocumentTemplate {
  id: string;
  tenantId: string;
  name: string;
  category: string;
  documentType: string;
  content: string;
  system: boolean;
  active: boolean;
}

export interface GenerateRequest {
  templateId: string;
  employeeId: string;
  contextId?: string;
}
