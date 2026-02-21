package com.valstrz.entity.declaration;

import com.arangodb.springframework.annotation.Document;
import com.valstrz.entity.BaseEntity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Генериран файл за подаване към НАП (Д1, Д6, чл. 62).
 * Съхранява се за одит — fileName + fileContent.
 */
@Document("napSubmissions")
public class NapSubmission extends BaseEntity {

    private String type;                  // D1, D6_INS, D6_TAX, ART62
    private int year;
    private int month;
    private String fileName;
    private String fileContent;           // пълен текст на файла
    private int recordCount;
    private String status;                // DRAFT, SUBMITTED, ACCEPTED, REJECTED
    private int correctionCode;           // 0=редовна, 1=коригираща, 8=заличаваща
    private LocalDateTime generatedAt;
    private List<String> validationErrors;
    private List<String> employeeIds;     // кои служители са включени

    public NapSubmission() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileContent() { return fileContent; }
    public void setFileContent(String fileContent) { this.fileContent = fileContent; }

    public int getRecordCount() { return recordCount; }
    public void setRecordCount(int recordCount) { this.recordCount = recordCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getCorrectionCode() { return correctionCode; }
    public void setCorrectionCode(int correctionCode) { this.correctionCode = correctionCode; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public List<String> getValidationErrors() { return validationErrors; }
    public void setValidationErrors(List<String> validationErrors) { this.validationErrors = validationErrors; }

    public List<String> getEmployeeIds() { return employeeIds; }
    public void setEmployeeIds(List<String> employeeIds) { this.employeeIds = employeeIds; }
}
