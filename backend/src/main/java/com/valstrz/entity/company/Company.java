package com.valstrz.entity.company;

import com.arangodb.springframework.annotation.Document;
import com.valstrz.entity.BaseEntity;

/**
 * Фирмени данни - БУЛСТАТ, адрес, контактна информация.
 * Служи и като tenant идентификатор - id на Company = tenantId за всички останали entities.
 */
@Document("companies")
public class Company extends BaseEntity {

    private String name;
    private String bulstat;
    private String region;
    private String municipality;
    private String city;
    private String postalCode;
    private String ekatte;
    private String address;
    private String correspondenceAddress;
    private String firmDossierNumber;
    private String firmDossierDescription;
    private String phone;
    private String email;
    private String napTerritorialDirectorate;
    private String napOffice;
    private String noiTerritorialUnit;

    // Флагове
    private boolean budgetOrganization;
    private boolean insuranceFund;
    private boolean insolvencyEligible;    // по ЗГВРС
    private boolean cukSystem;             // в системата на ЦУК
    private boolean disabilityEnterprise;  // специализирано предприятие за инвалиди

    // Настройки за работа
    private int paymentDeadlineDay;        // срок за изплащане (напр. 20-то число)
    private int noticePeriodDays;          // срок за предизвестие (напр. 30 дни)
    private boolean electronicSubmission;  // подаване по електронен път

    // Отговорни лица
    private PersonInfo director;           // ръководител на предприятието
    private PersonInfo napContact;         // лице подаващо в НАП
    private PersonInfo noiContact;         // лице подаващо в НОИ
    private String hrManagerTitle;         // длъжност на Завеждащ ЛС/ЧР
    private String hrManagerName;
    private String chiefAccountantName;

    // Банкова сметка на фирмата
    private String companyIban;
    private String companyBic;
    private String companyBankName;

    // НКИД
    private String nkidCode;              // код на основна икон. дейност
    private int nkidSerialNumber;         // пореден номер
    private String nkpdClassifier;        // действащ НКПД класификатор

    public Company() {}

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBulstat() { return bulstat; }
    public void setBulstat(String bulstat) { this.bulstat = bulstat; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getMunicipality() { return municipality; }
    public void setMunicipality(String municipality) { this.municipality = municipality; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getEkatte() { return ekatte; }
    public void setEkatte(String ekatte) { this.ekatte = ekatte; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCorrespondenceAddress() { return correspondenceAddress; }
    public void setCorrespondenceAddress(String correspondenceAddress) { this.correspondenceAddress = correspondenceAddress; }

    public String getFirmDossierNumber() { return firmDossierNumber; }
    public void setFirmDossierNumber(String firmDossierNumber) { this.firmDossierNumber = firmDossierNumber; }

    public String getFirmDossierDescription() { return firmDossierDescription; }
    public void setFirmDossierDescription(String firmDossierDescription) { this.firmDossierDescription = firmDossierDescription; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNapTerritorialDirectorate() { return napTerritorialDirectorate; }
    public void setNapTerritorialDirectorate(String napTerritorialDirectorate) { this.napTerritorialDirectorate = napTerritorialDirectorate; }

    public String getNapOffice() { return napOffice; }
    public void setNapOffice(String napOffice) { this.napOffice = napOffice; }

    public String getNoiTerritorialUnit() { return noiTerritorialUnit; }
    public void setNoiTerritorialUnit(String noiTerritorialUnit) { this.noiTerritorialUnit = noiTerritorialUnit; }

    public boolean isBudgetOrganization() { return budgetOrganization; }
    public void setBudgetOrganization(boolean budgetOrganization) { this.budgetOrganization = budgetOrganization; }

    public boolean isInsuranceFund() { return insuranceFund; }
    public void setInsuranceFund(boolean insuranceFund) { this.insuranceFund = insuranceFund; }

    public boolean isInsolvencyEligible() { return insolvencyEligible; }
    public void setInsolvencyEligible(boolean insolvencyEligible) { this.insolvencyEligible = insolvencyEligible; }

    public boolean isCukSystem() { return cukSystem; }
    public void setCukSystem(boolean cukSystem) { this.cukSystem = cukSystem; }

    public boolean isDisabilityEnterprise() { return disabilityEnterprise; }
    public void setDisabilityEnterprise(boolean disabilityEnterprise) { this.disabilityEnterprise = disabilityEnterprise; }

    public int getPaymentDeadlineDay() { return paymentDeadlineDay; }
    public void setPaymentDeadlineDay(int paymentDeadlineDay) { this.paymentDeadlineDay = paymentDeadlineDay; }

    public int getNoticePeriodDays() { return noticePeriodDays; }
    public void setNoticePeriodDays(int noticePeriodDays) { this.noticePeriodDays = noticePeriodDays; }

    public boolean isElectronicSubmission() { return electronicSubmission; }
    public void setElectronicSubmission(boolean electronicSubmission) { this.electronicSubmission = electronicSubmission; }

    public PersonInfo getDirector() { return director; }
    public void setDirector(PersonInfo director) { this.director = director; }

    public PersonInfo getNapContact() { return napContact; }
    public void setNapContact(PersonInfo napContact) { this.napContact = napContact; }

    public PersonInfo getNoiContact() { return noiContact; }
    public void setNoiContact(PersonInfo noiContact) { this.noiContact = noiContact; }

    public String getHrManagerTitle() { return hrManagerTitle; }
    public void setHrManagerTitle(String hrManagerTitle) { this.hrManagerTitle = hrManagerTitle; }

    public String getHrManagerName() { return hrManagerName; }
    public void setHrManagerName(String hrManagerName) { this.hrManagerName = hrManagerName; }

    public String getChiefAccountantName() { return chiefAccountantName; }
    public void setChiefAccountantName(String chiefAccountantName) { this.chiefAccountantName = chiefAccountantName; }

    public String getCompanyIban() { return companyIban; }
    public void setCompanyIban(String companyIban) { this.companyIban = companyIban; }

    public String getCompanyBic() { return companyBic; }
    public void setCompanyBic(String companyBic) { this.companyBic = companyBic; }

    public String getCompanyBankName() { return companyBankName; }
    public void setCompanyBankName(String companyBankName) { this.companyBankName = companyBankName; }

    public String getNkidCode() { return nkidCode; }
    public void setNkidCode(String nkidCode) { this.nkidCode = nkidCode; }

    public int getNkidSerialNumber() { return nkidSerialNumber; }
    public void setNkidSerialNumber(int nkidSerialNumber) { this.nkidSerialNumber = nkidSerialNumber; }

    public String getNkpdClassifier() { return nkpdClassifier; }
    public void setNkpdClassifier(String nkpdClassifier) { this.nkpdClassifier = nkpdClassifier; }
}
