package com.valstrz.entity.document;

import com.arangodb.springframework.annotation.Document;
import com.valstrz.entity.BaseEntity;

/**
 * Шаблон за документ (трудов договор, заповед, служебна бележка и др.).
 * Съдържанието е HTML с {{placeholder}} кодове за автоматично заместване.
 */
@Document("documentTemplates")
public class DocumentTemplate extends BaseEntity {

    private String name;              // наименование на шаблона
    private String category;          // LABOR_CONTRACT, AMENDMENT, TERMINATION, LEAVE_ORDER, CERTIFICATE, OTHER
    private String documentType;      // конкретен вид (напр. "Безсрочен ТД по чл. 67, ал. 1 от КТ")
    private String content;           // HTML текст с {{placeholder}} кодове
    private boolean system;           // системен шаблон (не може да се трие)
    private boolean active;

    public DocumentTemplate() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isSystem() { return system; }
    public void setSystem(boolean system) { this.system = system; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
