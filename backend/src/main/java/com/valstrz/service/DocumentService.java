package com.valstrz.service;

import com.valstrz.entity.company.Company;
import com.valstrz.entity.document.DocumentTemplate;
import com.valstrz.entity.personnel.*;
import com.valstrz.repository.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.StreamSupport;

/**
 * Оркестрация на генериране на документи по шаблони.
 */
@Service
public class DocumentService {

    private final DocumentTemplateRepository templateRepo;
    private final CompanyRepository companyRepo;
    private final EmployeeRepository employeeRepo;
    private final EmploymentRepository employmentRepo;
    private final AmendmentRepository amendmentRepo;
    private final TerminationRepository terminationRepo;
    private final AbsenceRepository absenceRepo;
    private final TemplateSubstitutionService substitutionService;

    public DocumentService(DocumentTemplateRepository templateRepo,
                           CompanyRepository companyRepo,
                           EmployeeRepository employeeRepo,
                           EmploymentRepository employmentRepo,
                           AmendmentRepository amendmentRepo,
                           TerminationRepository terminationRepo,
                           AbsenceRepository absenceRepo,
                           TemplateSubstitutionService substitutionService) {
        this.templateRepo = templateRepo;
        this.companyRepo = companyRepo;
        this.employeeRepo = employeeRepo;
        this.employmentRepo = employmentRepo;
        this.amendmentRepo = amendmentRepo;
        this.terminationRepo = terminationRepo;
        this.absenceRepo = absenceRepo;
        this.substitutionService = substitutionService;
    }

    /**
     * Генерира документ: зарежда шаблон + данни → заместване → HTML.
     *
     * @param contextId опционален ID на Amendment/Termination/Absence (зависи от категорията)
     */
    public String generateDocument(String tenantId, String templateId,
                                    String employeeId, String contextId) {
        DocumentTemplate template = templateRepo.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Шаблон не е намерен: " + templateId));

        Company company = companyRepo.findById(tenantId).orElse(null);

        Employee employee = employeeRepo.findById(employeeId).orElse(null);

        Employment employment = null;
        List<Employment> employments = StreamSupport.stream(
                employmentRepo.findByTenantIdAndEmployeeId(tenantId, employeeId).spliterator(), false).toList();
        if (!employments.isEmpty()) {
            employment = employments.stream()
                    .filter(Employment::isCurrent)
                    .findFirst()
                    .orElse(employments.get(0));
        }

        Amendment amendment = null;
        Termination termination = null;
        Absence absence = null;

        if (contextId != null && !contextId.isEmpty()) {
            String cat = template.getCategory();
            if ("AMENDMENT".equals(cat)) {
                amendment = amendmentRepo.findById(contextId).orElse(null);
            } else if ("TERMINATION".equals(cat)) {
                termination = terminationRepo.findById(contextId).orElse(null);
            } else if ("LEAVE_ORDER".equals(cat)) {
                absence = absenceRepo.findById(contextId).orElse(null);
            }
        }

        Map<String, String> vars = substitutionService.buildVariables(
                company, employee, employment, amendment, termination, absence);

        return substitutionService.substitute(template.getContent(), vars);
    }

    /**
     * Създава 6 системни шаблона за дадена фирма (ако вече не съществуват).
     */
    public List<DocumentTemplate> seedSystemTemplates(String tenantId) {
        List<DocumentTemplate> existing = StreamSupport.stream(
                templateRepo.findByTenantId(tenantId).spliterator(), false).toList();
        if (existing.stream().anyMatch(DocumentTemplate::isSystem)) {
            return existing.stream().filter(DocumentTemplate::isSystem).toList();
        }

        List<DocumentTemplate> templates = new ArrayList<>();
        templates.add(createSystemTemplate(tenantId, "LABOR_CONTRACT",
                "Безсрочен ТД по чл. 67, ал. 1 от КТ",
                "Безсрочен трудов договор",
                laborContractTemplate()));

        templates.add(createSystemTemplate(tenantId, "LABOR_CONTRACT",
                "Срочен ТД по чл. 68 от КТ",
                "Срочен трудов договор",
                fixedTermContractTemplate()));

        templates.add(createSystemTemplate(tenantId, "AMENDMENT",
                "Допълнително споразумение към ТД",
                "Допълнително споразумение",
                amendmentTemplate()));

        templates.add(createSystemTemplate(tenantId, "TERMINATION",
                "Заповед за прекратяване на ТД",
                "Заповед за прекратяване",
                terminationTemplate()));

        templates.add(createSystemTemplate(tenantId, "LEAVE_ORDER",
                "Заповед за отпуск",
                "Заповед за отпуск",
                leaveOrderTemplate()));

        templates.add(createSystemTemplate(tenantId, "CERTIFICATE",
                "Служебна бележка",
                "Служебна бележка",
                certificateTemplate()));

        templates.add(createSystemTemplate(tenantId, "BUSINESS_TRIP",
                "Заповед за командировка",
                "Заповед за командировка",
                businessTripTemplate()));

        List<DocumentTemplate> saved = new ArrayList<>();
        for (DocumentTemplate t : templates) {
            saved.add(templateRepo.save(t));
        }
        return saved;
    }

    private DocumentTemplate createSystemTemplate(String tenantId, String category,
                                                    String documentType, String name, String content) {
        DocumentTemplate t = new DocumentTemplate();
        t.setTenantId(tenantId);
        t.setCategory(category);
        t.setDocumentType(documentType);
        t.setName(name);
        t.setContent(content);
        t.setSystem(true);
        t.setActive(true);
        return t;
    }

    // ---- Системни шаблони (HTML) ----

    private String laborContractTemplate() {
        return """
                <div style="font-family: 'Times New Roman', serif; max-width: 700px; margin: 0 auto; line-height: 1.6;">
                <div style="text-align: center; margin-bottom: 2rem;">
                  <strong>{{company.name}}</strong><br>
                  ЕИК по БУЛСТАТ: {{company.bulstat}}<br>
                  гр. {{company.city}}, {{company.address}}
                </div>
                <h2 style="text-align: center;">ТРУДОВ ДОГОВОР № {{employment.contractNumber}}</h2>
                <p style="text-align: center;">от {{employment.contractDate}} г.</p>
                <p>На основание {{employment.contractBasis}} от Кодекса на труда</p>
                <p><strong>{{company.name}}</strong>, ЕИК {{company.bulstat}}, представлявано от {{company.director}} — {{company.directorTitle}}, наричано по-долу <em>РАБОТОДАТЕЛ</em>,</p>
                <p>и</p>
                <p><strong>{{employee.fullName}}</strong>, ЕГН {{employee.egn}}, с постоянен адрес: гр. {{employee.city}}, {{employee.address}}, ЛК {{employee.idCard}}, наричан/а по-долу <em>РАБОТНИК/СЛУЖИТЕЛ</em>,</p>
                <p>сключиха настоящия трудов договор за следното:</p>
                <ol>
                  <li>РАБОТОДАТЕЛЯТ назначава РАБОТНИКА/СЛУЖИТЕЛЯ на длъжност: <strong>{{employment.jobTitle}}</strong>, код по НКПД: {{employment.nkpdCode}}.</li>
                  <li>Място на работа: {{employment.workplace}}.</li>
                  <li>Характер на работата: {{employment.contractType}}.</li>
                  <li>Дата на постъпване: <strong>{{employment.startDate}}</strong> г.</li>
                  <li>Срок на договора: <strong>безсрочен</strong>.</li>
                  <li>Основно месечно възнаграждение: <strong>{{employment.baseSalary}} EUR</strong>.</li>
                  <li>Допълнително възнаграждение за трудов стаж и професионален опит: {{employment.seniorityBonusPercent}}%.</li>
                  <li>Вид работно време: {{employment.workTimeType}}.</li>
                  <li>Основен платен годишен отпуск: 20 работни дни.</li>
                  <li>Срок на предизвестие при прекратяване: 30 дни.</li>
                </ol>
                <div style="margin-top: 3rem; display: flex; justify-content: space-between;">
                  <div>РАБОТОДАТЕЛ:<br><br><br>{{company.director}}</div>
                  <div>РАБОТНИК/СЛУЖИТЕЛ:<br><br><br>{{employee.fullName}}</div>
                </div>
                <p style="text-align: right; margin-top: 2rem;">Дата: {{today}}</p>
                </div>
                """;
    }

    private String fixedTermContractTemplate() {
        return """
                <div style="font-family: 'Times New Roman', serif; max-width: 700px; margin: 0 auto; line-height: 1.6;">
                <div style="text-align: center; margin-bottom: 2rem;">
                  <strong>{{company.name}}</strong><br>
                  ЕИК по БУЛСТАТ: {{company.bulstat}}<br>
                  гр. {{company.city}}, {{company.address}}
                </div>
                <h2 style="text-align: center;">СРОЧЕН ТРУДОВ ДОГОВОР № {{employment.contractNumber}}</h2>
                <p style="text-align: center;">от {{employment.contractDate}} г.</p>
                <p>На основание {{employment.contractBasis}} от Кодекса на труда</p>
                <p><strong>{{company.name}}</strong>, представлявано от {{company.director}}, наричано РАБОТОДАТЕЛ,</p>
                <p>и <strong>{{employee.fullName}}</strong>, ЕГН {{employee.egn}}, наричан/а РАБОТНИК/СЛУЖИТЕЛ,</p>
                <p>сключиха настоящия срочен трудов договор:</p>
                <ol>
                  <li>Длъжност: <strong>{{employment.jobTitle}}</strong>, код по НКПД: {{employment.nkpdCode}}.</li>
                  <li>Място на работа: {{employment.workplace}}.</li>
                  <li>Дата на постъпване: <strong>{{employment.startDate}}</strong> г.</li>
                  <li>Срок на договора: до <strong>{{employment.contractEndDate}}</strong> г.</li>
                  <li>Основно месечно възнаграждение: <strong>{{employment.baseSalary}} EUR</strong>.</li>
                  <li>Вид работно време: {{employment.workTimeType}}.</li>
                </ol>
                <div style="margin-top: 3rem; display: flex; justify-content: space-between;">
                  <div>РАБОТОДАТЕЛ:<br><br><br>{{company.director}}</div>
                  <div>РАБОТНИК/СЛУЖИТЕЛ:<br><br><br>{{employee.fullName}}</div>
                </div>
                </div>
                """;
    }

    private String amendmentTemplate() {
        return """
                <div style="font-family: 'Times New Roman', serif; max-width: 700px; margin: 0 auto; line-height: 1.6;">
                <div style="text-align: center; margin-bottom: 2rem;">
                  <strong>{{company.name}}</strong><br>
                  ЕИК: {{company.bulstat}}
                </div>
                <h2 style="text-align: center;">ДОПЪЛНИТЕЛНО СПОРАЗУМЕНИЕ № {{amendment.number}}</h2>
                <p style="text-align: center;">от {{amendment.date}} г.<br>към трудов договор № {{employment.contractNumber}} / {{employment.contractDate}} г.</p>
                <p>На основание {{amendment.basis}} от Кодекса на труда</p>
                <p>между <strong>{{company.name}}</strong>, представлявано от {{company.director}},</p>
                <p>и <strong>{{employee.fullName}}</strong>, ЕГН {{employee.egn}},</p>
                <p>се сключи настоящото допълнително споразумение, с което считано от <strong>{{amendment.effectiveDate}}</strong> г. се променят следните условия по трудовия договор:</p>
                <p>{{amendment.specificText}}</p>
                <p>Всички останали условия по трудовия договор остават непроменени.</p>
                <div style="margin-top: 3rem; display: flex; justify-content: space-between;">
                  <div>РАБОТОДАТЕЛ:<br><br><br>{{company.director}}</div>
                  <div>РАБОТНИК/СЛУЖИТЕЛ:<br><br><br>{{employee.fullName}}</div>
                </div>
                </div>
                """;
    }

    private String terminationTemplate() {
        return """
                <div style="font-family: 'Times New Roman', serif; max-width: 700px; margin: 0 auto; line-height: 1.6;">
                <div style="text-align: center; margin-bottom: 2rem;">
                  <strong>{{company.name}}</strong><br>
                  ЕИК: {{company.bulstat}}<br>
                  гр. {{company.city}}, {{company.address}}
                </div>
                <h2 style="text-align: center;">ЗАПОВЕД № {{termination.orderNumber}}</h2>
                <p style="text-align: center;">от {{termination.orderDate}} г.</p>
                <p>На основание {{termination.basis}} от Кодекса на труда</p>
                <h3 style="text-align: center;">ЗАПОВЯДВАМ:</h3>
                <p>Да се прекрати трудовото правоотношение с <strong>{{employee.fullName}}</strong>, ЕГН {{employee.egn}}, на длъжност {{employment.jobTitle}}, код по НКПД: {{employment.nkpdCode}}, по трудов договор № {{employment.contractNumber}} / {{employment.contractDate}} г.</p>
                <p>Последен работен ден: <strong>{{termination.lastWorkDay}}</strong> г.</p>
                <p>{{termination.specificText}}</p>
                <div style="margin-top: 3rem;">
                  <div>Работодател:<br><br><br>{{company.director}}<br>{{company.directorTitle}}</div>
                </div>
                <p style="margin-top: 2rem;">Запознат/а:<br><br>{{employee.fullName}} / __________ /</p>
                </div>
                """;
    }

    private String leaveOrderTemplate() {
        return """
                <div style="font-family: 'Times New Roman', serif; max-width: 700px; margin: 0 auto; line-height: 1.6;">
                <div style="text-align: center; margin-bottom: 2rem;">
                  <strong>{{company.name}}</strong><br>
                  ЕИК по БУЛСТАТ: {{company.bulstat}}
                </div>
                <h2 style="text-align: center;">ЗАПОВЕД ЗА ОТПУСК № {{absence.orderNumber}}</h2>
                <p style="text-align: center;">от {{absence.orderDate}} г.</p>
                <p>На основание {{absence.typeName}}</p>
                <h3 style="text-align: center;">ЗАПОВЯДВАМ</h3>
                <p>лицето:</p>
                <p><strong>{{employee.fullName}}</strong></p>
                <p>ЕГН {{employee.egn}}, постоянен адрес: {{employee.address}},<br>
                длъжност: {{employment.jobTitle}},<br>
                място на работа: {{employment.workplace}}</p>
                <p>да ползва <strong>{{absence.typeName}}</strong><br>
                брой работни дни: <strong>{{absence.workingDays}}</strong>,<br>
                от {{absence.fromDate}} г. до {{absence.toDate}} г.</p>
                <p>{{absence.notes}}</p>
                <div style="margin-top: 3rem;">
                  <div>Работодател:<br><br><br>{{company.director}}</div>
                </div>
                </div>
                """;
    }

    private String businessTripTemplate() {
        return """
                <div style="font-family: 'Times New Roman', serif; max-width: 700px; margin: 0 auto; line-height: 1.6;">
                <div style="text-align: center; margin-bottom: 2rem;">
                  <strong>{{company.name}}</strong><br>
                  ЕИК по БУЛСТАТ: {{company.bulstat}}<br>
                  гр. {{company.city}}, {{company.address}}
                </div>
                <h2 style="text-align: center;">ЗАПОВЕД ЗА КОМАНДИРОВКА</h2>
                <p style="text-align: center;">от {{today}} г.</p>
                <p>На основание чл. 121 от Кодекса на труда и Наредба за командировките в страната</p>
                <h3 style="text-align: center;">КОМАНДИРОВАМ</h3>
                <p><strong>{{employee.fullName}}</strong>, ЕГН {{employee.egn}},<br>
                длъжност: {{employment.jobTitle}},<br>
                месторабота: {{employment.workplace}}</p>
                <p>Да замине в командировка при следните условия:</p>
                <table style="border-collapse: collapse; width: 100%; margin: 10px 0;">
                  <tr><td style="border: 1px solid #000; padding: 6px; width: 200px;"><strong>Населено място:</strong></td>
                      <td style="border: 1px solid #000; padding: 6px;">___________________________</td></tr>
                  <tr><td style="border: 1px solid #000; padding: 6px;"><strong>Начална дата:</strong></td>
                      <td style="border: 1px solid #000; padding: 6px;">___________________________</td></tr>
                  <tr><td style="border: 1px solid #000; padding: 6px;"><strong>Крайна дата:</strong></td>
                      <td style="border: 1px solid #000; padding: 6px;">___________________________</td></tr>
                  <tr><td style="border: 1px solid #000; padding: 6px;"><strong>Брой дни:</strong></td>
                      <td style="border: 1px solid #000; padding: 6px;">___________________________</td></tr>
                  <tr><td style="border: 1px solid #000; padding: 6px;"><strong>Цел на командировката:</strong></td>
                      <td style="border: 1px solid #000; padding: 6px;">___________________________</td></tr>
                  <tr><td style="border: 1px solid #000; padding: 6px;"><strong>Дневни пари:</strong></td>
                      <td style="border: 1px solid #000; padding: 6px;">по 40 лв./ден</td></tr>
                  <tr><td style="border: 1px solid #000; padding: 6px;"><strong>Квартирни пари:</strong></td>
                      <td style="border: 1px solid #000; padding: 6px;">срещу документ</td></tr>
                  <tr><td style="border: 1px solid #000; padding: 6px;"><strong>Пътни пари:</strong></td>
                      <td style="border: 1px solid #000; padding: 6px;">срещу документ</td></tr>
                </table>
                <p>Разходите за командировката са за сметка на {{company.name}}.</p>
                <div style="margin-top: 3rem;">
                  <div>Работодател:<br><br><br>{{company.director}}<br>{{company.directorTitle}}</div>
                </div>
                <p style="margin-top: 2rem;">Запознат/а: {{employee.fullName}} / __________ /</p>
                </div>
                """;
    }

    private String certificateTemplate() {
        return """
                <div style="font-family: 'Times New Roman', serif; max-width: 700px; margin: 0 auto; line-height: 1.6;">
                <div style="text-align: center; margin-bottom: 2rem;">
                  <strong>{{company.name}}</strong><br>
                  ЕИК по БУЛСТАТ: {{company.bulstat}}<br>
                  гр. {{company.city}}, {{company.address}}<br>
                  тел.: {{company.phone}}, e-mail: {{company.email}}
                </div>
                <h2 style="text-align: center;">СЛУЖЕБНА БЕЛЕЖКА</h2>
                <p>С настоящата се удостоверява, че <strong>{{employee.fullName}}</strong>, ЕГН {{employee.egn}}, е в трудово правоотношение с {{company.name}} от {{employment.startDate}} г. на длъжност {{employment.jobTitle}}.</p>
                <p>Настоящата служебна бележка се издава, за да послужи пред _______________.</p>
                <p style="text-align: right; margin-top: 3rem;">Дата: {{today}}<br>гр. {{company.city}}</p>
                <div style="margin-top: 2rem;">
                  <div>{{company.hrManager}}<br>{{company.directorTitle}}</div>
                </div>
                </div>
                """;
    }
}
