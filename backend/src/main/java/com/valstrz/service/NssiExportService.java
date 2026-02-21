package com.valstrz.service;

import com.valstrz.entity.company.Company;
import com.valstrz.entity.personnel.Absence;
import com.valstrz.entity.personnel.Employee;
import com.valstrz.repository.CompanyRepository;
import com.valstrz.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class NssiExportService {

    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;

    public NssiExportService(CompanyRepository companyRepository, EmployeeRepository employeeRepository) {
        this.companyRepository = companyRepository;
        this.employeeRepository = employeeRepository;
    }

    public String generateAnnex9Xml(String tenantId, List<Absence> absences) {
        Company company = companyRepository.findById(tenantId).orElseThrow();
        DateTimeFormatter dtfXml = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        xml.append("<BPril9 xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n");
        xml.append("  <FlagDelegated>0</FlagDelegated>\n");
        xml.append("  <TypeOfDocument>0</TypeOfDocument>\n");
        
        xml.append("  <NSSI_RO_All>\n");
        xml.append("    <NSSI_RO>01</NSSI_RO>\n");
        xml.append("    <NSSI_RO_Name>София</NSSI_RO_Name>\n");
        xml.append("  </NSSI_RO_All>\n");

        xml.append("  <Insurer>\n");
        xml.append("    <FlagKasa>0</FlagKasa>\n");
        xml.append("    <BULSTATKasa>").append(company.getBulstat()).append("</BULSTATKasa>\n");
        xml.append("    <BulName>").append(escapeXml(company.getName())).append("</BulName>\n");
        xml.append("    <BULSTAT>").append(company.getBulstat()).append("</BULSTAT>\n");
        xml.append("    <BulAddressFull>\n");
        xml.append("      <BulRegion>").append(escapeXml(company.getRegion())).append("</BulRegion>\n");
        xml.append("      <BulSubRegion>").append(escapeXml(company.getMunicipality())).append("</BulSubRegion>\n");
        xml.append("      <BulCity>").append(escapeXml(company.getCity())).append("</BulCity>\n");
        xml.append("      <BulPostalCode>").append(company.getPostalCode()).append("</BulPostalCode>\n");
        xml.append("      <BulAddress>").append(escapeXml(company.getAddress())).append("</BulAddress>\n");
        xml.append("      <BulPhone>").append(company.getPhone() != null ? company.getPhone() : "").append("</BulPhone>\n");
        xml.append("      <BulCellPhoneNumber></BulCellPhoneNumber>\n");
        xml.append("      <BulEMail>").append(company.getEmail() != null ? company.getEmail() : "").append("</BulEMail>\n");
        xml.append("    </BulAddressFull>\n");
        xml.append("  </Insurer>\n");

        xml.append("  <DocList>\n");
        int count = 1;
        for (Absence abs : absences) {
            Employee emp = employeeRepository.findById(abs.getEmployeeId()).orElse(null);
            if (emp == null) continue;

            Map<String, Object> sickData = abs.getSickLeaveData();
            String chartNo = sickData != null ? (String) sickData.get("chartNumber") : "000000000000";

            xml.append("    <NumberPril9>\n");
            xml.append("      <Number>").append(count++).append("</Number>\n");
            xml.append("      <CodeOperation>0</CodeOperation>\n");
            xml.append("      <PatientsChartNumber>").append(chartNo).append("</PatientsChartNumber>\n");
            xml.append("      <DateIssued>").append(formatDate(abs.getFromDate())).append("</DateIssued>\n");
            xml.append("      <SickLeaveStartDate>").append(formatDate(abs.getFromDate())).append("</SickLeaveStartDate>\n");
            xml.append("      <SickLeaveEndDate>").append(formatDate(abs.getToDate())).append("</SickLeaveEndDate>\n");
            
            xml.append("      <Insured>\n");
            xml.append("        <FirstName>").append(escapeXml(emp.getFirstName())).append("</FirstName>\n");
            xml.append("        <SurName>").append(escapeXml(emp.getMiddleName())).append("</SurName>\n");
            xml.append("        <FamilyName>").append(escapeXml(emp.getLastName())).append("</FamilyName>\n");
            xml.append("        <EGN>").append(emp.getEgn()).append("</EGN>\n");
            xml.append("        <FlagEGN>0</FlagEGN>\n");
            xml.append("        <InsuredAddress>\n");
            xml.append("          <EGNRegion>").append(escapeXml(emp.getPermanentRegion())).append("</EGNRegion>\n");
            xml.append("          <EGNSubRegion>").append(escapeXml(emp.getPermanentMunicipality())).append("</EGNSubRegion>\n");
            xml.append("          <EGNCity>").append(escapeXml(emp.getPermanentCity())).append("</EGNCity>\n");
            xml.append("          <EGNPostalCode>").append(emp.getPermanentPostalCode()).append("</EGNPostalCode>\n");
            xml.append("          <EGNAddress>").append(escapeXml(emp.getPermanentAddress())).append("</EGNAddress>\n");
            xml.append("          <EGNPhone>").append(emp.getPhone() != null ? emp.getPhone() : "").append("</EGNPhone>\n");
            xml.append("          <EGNCellPhoneNumber></EGNCellPhoneNumber>\n");
            xml.append("          <EGNEMail>").append(emp.getEmail() != null ? emp.getEmail() : "").append("</EGNEMail>\n");
            xml.append("        </InsuredAddress>\n");
            xml.append("      </Insured>\n");

            xml.append("      <FlagCertificate>1</FlagCertificate>\n");
            xml.append("      <Certificates>\n");
            xml.append("        <RiskInsured>1</RiskInsured>\n");
            xml.append("        <EmploymentType>0</EmploymentType>\n");
            xml.append("        <EploymentDateExpire><Day></Day><Month></Month><Year></Year></EploymentDateExpire>\n");
            xml.append("        <SelfEmploymentType>9</SelfEmploymentType>\n");
            xml.append("        <SelfEmploymentDateExpire><Day></Day><Month></Month><Year></Year></SelfEmploymentDateExpire>\n");
            xml.append("        <SixMonthsInsurance>1</SixMonthsInsurance>\n");
            xml.append("        <DateOfAchievingInsuranceRigths><Day></Day><Month></Month><Year></Year></DateOfAchievingInsuranceRigths>\n");
            xml.append("        <InsuranceRigthsOZM>1</InsuranceRigthsOZM>\n");
            xml.append("        <DateOfAchievingInsuranceRigthsOZM><Day></Day><Month></Month><Year></Year></DateOfAchievingInsuranceRigthsOZM>\n");
            xml.append("        <TypeOfInsured>01</TypeOfInsured>\n");
            xml.append("        <EGNBulCountTOI>1</EGNBulCountTOI>\n");
            xml.append("        <DocNumber>1</DocNumber>\n");
            xml.append("        <InsuranceTerminationDate><Day></Day><Month></Month><Year></Year></InsuranceTerminationDate>\n");
            xml.append("        <InsuranceStopDate><Day></Day><Month></Month><Year></Year></InsuranceStopDate>\n");
            
            xml.append("        <SickLeaveMonths>\n");
            xml.append("          <BSickLeave>\n");
            xml.append("            <SickNumber>1</SickNumber>\n");
            xml.append("            <Month>").append(abs.getFromDate().getMonthValue()).append("</Month>\n");
            xml.append("            <Year>").append(abs.getFromDate().getYear()).append("</Year>\n");
            xml.append("            <FromDay>").append(abs.getFromDate().getDayOfMonth()).append("</FromDay>\n");
            xml.append("            <ToDay>").append(abs.getToDate().getDayOfMonth()).append("</ToDay>\n");
            xml.append("            <Days>").append(abs.getWorkingDays()).append("</Days>\n");
            xml.append("            <Hours>").append(abs.getWorkingDays() * 8).append("</Hours>\n");
            xml.append("            <PersonalLegalWorkHours>8</PersonalLegalWorkHours>\n");
            xml.append("          </BSickLeave>\n");
            xml.append("        </SickLeaveMonths>\n");

            xml.append("        <FlagJobTransfer>0</FlagJobTransfer>\n");
            xml.append("        <Anketa>0</Anketa>\n");
            xml.append("        <LeaveWithPay>0</LeaveWithPay>\n");
            xml.append("        <ChildDataFlag>0</ChildDataFlag>\n");
            xml.append("        <FlagOtherCircumstances>0</FlagOtherCircumstances>\n");
            xml.append("        <NumberInEmployerRegistry>").append(abs.getOrderNumber() != null ? abs.getOrderNumber() : "1").append("</NumberInEmployerRegistry>\n");
            xml.append("        <DateInEmployerRegistry>").append(formatDate(abs.getFromDate())).append("</DateInEmployerRegistry>\n");
            xml.append("        <BankAccountDeclared>1</BankAccountDeclared>\n");
            xml.append("        <FlagBankAccount>0</FlagBankAccount>\n");
            xml.append("        <IBAN>").append(emp.getIban() != null ? emp.getIban() : "").append("</IBAN>\n");
            xml.append("      </Certificates>\n");

            xml.append("      <Representative>\n");
            xml.append("        <FirstName>Управител</FirstName>\n");
            xml.append("        <SurName></SurName>\n");
            xml.append("        <FamilyName></FamilyName>\n");
            xml.append("        <Position>Управител</Position>\n");
            xml.append("        <City>").append(escapeXml(company.getCity())).append("</City>\n");
            xml.append("      </Representative>\n");
            xml.append("      <DateCertificate>").append(formatDate(java.time.LocalDate.now())).append("</DateCertificate>\n");
            xml.append("      <DateSaved>").append(java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))).append("</DateSaved>\n");
            xml.append("      <TimeSaved>12:00:00</TimeSaved>\n");
            xml.append("    </NumberPril9>\n");
        }
        xml.append("  </DocList>\n");
        
        xml.append("  <Exported>1</Exported>\n");
        xml.append("  <DateExport>").append(formatDate(java.time.LocalDate.now())).append("</DateExport>\n");
        xml.append("  <Source>").append(company.getBulstat()).append("</Source>\n");
        xml.append("</BPril9>");

        return xml.toString();
    }

    private String formatDate(java.time.LocalDate date) {
        if (date == null) return "<Day></Day><Month></Month><Year></Year>";
        return "<Day>" + date.getDayOfMonth() + "</Day><Month>" + date.getMonthValue() + "</Month><Year>" + date.getYear() + "</Year>";
    }

    private String escapeXml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&apos;");
    }
}
