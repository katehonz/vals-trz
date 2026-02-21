package com.valstrz.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.valstrz.entity.company.Company;
import com.valstrz.entity.payroll.PayrollSnapshot;
import com.valstrz.entity.payroll.PayrollSnapshot.PayrollLine;
import com.valstrz.repository.CompanyRepository;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class PdfExportService {

    private final CompanyRepository companyRepository;

    public PdfExportService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public byte[] generateSalarySlip(PayrollSnapshot snapshot, String tenantId) throws Exception {
        Company company = companyRepository.findById(tenantId).orElse(null);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter.getInstance(document, baos);
        document.open();

        // Fonts - use Helvetica (built-in, supports basic Latin)
        Font titleFont = new Font(Font.HELVETICA, 14, Font.BOLD);
        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD);
        Font normalFont = new Font(Font.HELVETICA, 9, Font.NORMAL);
        Font smallFont = new Font(Font.HELVETICA, 8, Font.NORMAL);
        Font boldFont = new Font(Font.HELVETICA, 9, Font.BOLD);

        Map<String, Object> empData = snapshot.getEmployeeData();
        String fullName = empData != null ? str(empData.get("fullName")) : "";
        String egn = empData != null ? str(empData.get("egn")) : "";
        String jobTitle = empData != null ? str(empData.get("jobTitle")) : "";
        String department = empData != null ? str(empData.get("departmentName")) : "";
        String companyName = company != null ? company.getName() : tenantId;
        String companyEik = company != null && company.getBulstat() != null ? company.getBulstat() : "";

        // Title
        Paragraph title = new Paragraph("PAYROLL SLIP / FISHKA ZAPLATA", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph period = new Paragraph(
                String.format("Period: %02d/%d", snapshot.getMonth(), snapshot.getYear()), headerFont);
        period.setAlignment(Element.ALIGN_CENTER);
        period.setSpacingAfter(15);
        document.add(period);

        // Company + Employee info
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingAfter(15);

        addInfoRow(infoTable, "Company:", companyName, headerFont, normalFont);
        addInfoRow(infoTable, "EIK:", companyEik, headerFont, normalFont);
        addInfoRow(infoTable, "Employee:", fullName, headerFont, normalFont);
        addInfoRow(infoTable, "EGN:", egn, headerFont, normalFont);
        addInfoRow(infoTable, "Position:", jobTitle, headerFont, normalFont);
        addInfoRow(infoTable, "Department:", department, headerFont, normalFont);
        document.add(infoTable);

        // === EARNINGS ===
        document.add(new Paragraph("EARNINGS", headerFont));
        document.add(new Paragraph(" ", smallFont));

        PdfPTable earningsTable = createLineTable();
        addLineHeader(earningsTable, headerFont);

        if (snapshot.getEarnings() != null) {
            for (PayrollLine line : snapshot.getEarnings()) {
                addLineRow(earningsTable, line, normalFont);
            }
        }
        addTotalRow(earningsTable, "GROSS SALARY", snapshot.getGrossSalary(), boldFont);
        document.add(earningsTable);
        document.add(new Paragraph(" ", smallFont));

        // === DEDUCTIONS ===
        document.add(new Paragraph("DEDUCTIONS", headerFont));
        document.add(new Paragraph(" ", smallFont));

        PdfPTable deductionsTable = createLineTable();
        addLineHeader(deductionsTable, headerFont);

        if (snapshot.getDeductions() != null) {
            for (PayrollLine line : snapshot.getDeductions()) {
                addLineRow(deductionsTable, line, normalFont);
            }
        }
        addTotalRow(deductionsTable, "TOTAL DEDUCTIONS", snapshot.getTotalDeductions(), boldFont);
        document.add(deductionsTable);
        document.add(new Paragraph(" ", smallFont));

        // === EMPLOYER CONTRIBUTIONS ===
        document.add(new Paragraph("EMPLOYER CONTRIBUTIONS", headerFont));
        document.add(new Paragraph(" ", smallFont));

        PdfPTable employerTable = createLineTable();
        addLineHeader(employerTable, headerFont);

        if (snapshot.getEmployerContributions() != null) {
            for (PayrollLine line : snapshot.getEmployerContributions()) {
                addLineRow(employerTable, line, normalFont);
            }
        }
        addTotalRow(employerTable, "TOTAL EMPLOYER", snapshot.getTotalEmployerInsurance(), boldFont);
        document.add(employerTable);
        document.add(new Paragraph(" ", smallFont));

        // === SUMMARY ===
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(60);
        summaryTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        summaryTable.setWidths(new float[]{3, 2});

        addSummaryRow(summaryTable, "Gross salary:", snapshot.getGrossSalary(), boldFont);
        addSummaryRow(summaryTable, "Insurable income:", snapshot.getInsurableIncome(), normalFont);
        addSummaryRow(summaryTable, "Employee insurance:", snapshot.getTotalEmployeeInsurance(), normalFont);
        addSummaryRow(summaryTable, "Tax base:", snapshot.getTaxBase(), normalFont);
        addSummaryRow(summaryTable, "Income tax:", snapshot.getIncomeTax(), normalFont);
        addSummaryRow(summaryTable, "Total deductions:", snapshot.getTotalDeductions(), normalFont);

        PdfPCell netLabel = new PdfPCell(new Phrase("NET SALARY:", titleFont));
        netLabel.setBorder(com.lowagie.text.Rectangle.TOP);
        netLabel.setPaddingTop(8);
        summaryTable.addCell(netLabel);

        PdfPCell netVal = new PdfPCell(new Phrase(money(snapshot.getNetSalary()), titleFont));
        netVal.setBorder(com.lowagie.text.Rectangle.TOP);
        netVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        netVal.setPaddingTop(8);
        summaryTable.addCell(netVal);

        addSummaryRow(summaryTable, "Total employer cost:", snapshot.getTotalEmployerCost(), boldFont);

        document.add(summaryTable);

        document.close();
        return baos.toByteArray();
    }

    public byte[] generateAllSlips(List<PayrollSnapshot> snapshots, String tenantId) throws Exception {
        if (snapshots.isEmpty()) {
            throw new IllegalArgumentException("No payroll data to export.");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter.getInstance(document, baos);
        document.open();

        for (int i = 0; i < snapshots.size(); i++) {
            if (i > 0) document.newPage();
            addSlipToDocument(document, snapshots.get(i), tenantId);
        }

        document.close();
        return baos.toByteArray();
    }

    private void addSlipToDocument(Document document, PayrollSnapshot snapshot, String tenantId) throws Exception {
        Company company = companyRepository.findById(tenantId).orElse(null);

        Font titleFont = new Font(Font.HELVETICA, 14, Font.BOLD);
        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD);
        Font normalFont = new Font(Font.HELVETICA, 9, Font.NORMAL);
        Font smallFont = new Font(Font.HELVETICA, 8, Font.NORMAL);
        Font boldFont = new Font(Font.HELVETICA, 9, Font.BOLD);

        Map<String, Object> empData = snapshot.getEmployeeData();
        String fullName = empData != null ? str(empData.get("fullName")) : "";
        String egn = empData != null ? str(empData.get("egn")) : "";
        String jobTitle = empData != null ? str(empData.get("jobTitle")) : "";
        String companyName = company != null ? company.getName() : tenantId;

        Paragraph title = new Paragraph("PAYROLL SLIP", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph period = new Paragraph(
                String.format("%s | %02d/%d | %s - %s",
                        companyName, snapshot.getMonth(), snapshot.getYear(), fullName, jobTitle), headerFont);
        period.setAlignment(Element.ALIGN_CENTER);
        period.setSpacingAfter(10);
        document.add(period);

        // Compact summary table
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 2, 3, 2});

        addCompactRow(table, "Gross:", snapshot.getGrossSalary(), "Ins. income:", snapshot.getInsurableIncome(), normalFont, boldFont);
        addCompactRow(table, "Ee insurance:", snapshot.getTotalEmployeeInsurance(), "Tax base:", snapshot.getTaxBase(), normalFont, boldFont);
        addCompactRow(table, "Income tax:", snapshot.getIncomeTax(), "Total deduct:", snapshot.getTotalDeductions(), normalFont, boldFont);
        addCompactRow(table, "NET SALARY:", snapshot.getNetSalary(), "Employer cost:", snapshot.getTotalEmployerCost(), boldFont, boldFont);

        document.add(table);
    }

    // --- Helpers ---

    private PdfPTable createLineTable() throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.5f, 4, 2, 2});
        return table;
    }

    private void addLineHeader(PdfPTable table, Font font) {
        Color headerBg = new Color(230, 236, 245);
        addHeaderCell(table, "Code", font, headerBg);
        addHeaderCell(table, "Description", font, headerBg);
        addHeaderCell(table, "Rate/%", font, headerBg);
        addHeaderCell(table, "Amount", font, headerBg);
    }

    private void addHeaderCell(PdfPTable table, String text, Font font, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(4);
        table.addCell(cell);
    }

    private void addLineRow(PdfPTable table, PayrollLine line, Font font) {
        table.addCell(new Phrase(line.getCode() != null ? line.getCode() : "", font));
        table.addCell(new Phrase(line.getName() != null ? line.getName() : "", font));

        String rateStr = "";
        if (line.getRate() != null && line.getRate().compareTo(BigDecimal.ZERO) != 0) {
            rateStr = line.getRate().stripTrailingZeros().toPlainString() + "%";
        }
        PdfPCell rateCell = new PdfPCell(new Phrase(rateStr, font));
        rateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(rateCell);

        PdfPCell amtCell = new PdfPCell(new Phrase(money(line.getAmount()), font));
        amtCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(amtCell);
    }

    private void addTotalRow(PdfPTable table, String label, BigDecimal total, Font font) {
        PdfPCell emptyCell = new PdfPCell(new Phrase("", font));
        emptyCell.setBorder(com.lowagie.text.Rectangle.TOP);
        table.addCell(emptyCell);

        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setBorder(com.lowagie.text.Rectangle.TOP);
        table.addCell(labelCell);

        PdfPCell emptyCell2 = new PdfPCell(new Phrase("", font));
        emptyCell2.setBorder(com.lowagie.text.Rectangle.TOP);
        table.addCell(emptyCell2);

        PdfPCell totalCell = new PdfPCell(new Phrase(money(total), font));
        totalCell.setBorder(com.lowagie.text.Rectangle.TOP);
        totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(totalCell);
    }

    private void addInfoRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell lCell = new PdfPCell(new Phrase(label, labelFont));
        lCell.setBorder(0);
        lCell.setPaddingBottom(3);
        table.addCell(lCell);

        PdfPCell vCell = new PdfPCell(new Phrase(value != null ? value : "", valueFont));
        vCell.setBorder(0);
        vCell.setPaddingBottom(3);
        table.addCell(vCell);
    }

    private void addSummaryRow(PdfPTable table, String label, BigDecimal value, Font font) {
        PdfPCell lCell = new PdfPCell(new Phrase(label, font));
        lCell.setBorder(0);
        lCell.setPaddingBottom(2);
        table.addCell(lCell);

        PdfPCell vCell = new PdfPCell(new Phrase(money(value), font));
        vCell.setBorder(0);
        vCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        vCell.setPaddingBottom(2);
        table.addCell(vCell);
    }

    private void addCompactRow(PdfPTable table, String l1, BigDecimal v1, String l2, BigDecimal v2,
                                Font labelFont, Font valueFont) {
        PdfPCell c1 = new PdfPCell(new Phrase(l1, labelFont));
        c1.setBorder(0);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(money(v1), valueFont));
        c2.setBorder(0);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c2);

        PdfPCell c3 = new PdfPCell(new Phrase(l2, labelFont));
        c3.setBorder(0);
        table.addCell(c3);

        PdfPCell c4 = new PdfPCell(new Phrase(money(v2), valueFont));
        c4.setBorder(0);
        c4.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c4);
    }

    private String money(BigDecimal val) {
        return val != null ? String.format("%.2f", val) : "0.00";
    }

    private String str(Object obj) {
        if (obj == null) return "";
        String s = obj.toString();
        return "null".equals(s) ? "" : s;
    }
}
