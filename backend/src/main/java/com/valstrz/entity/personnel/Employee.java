package com.valstrz.entity.personnel;

import com.arangodb.springframework.annotation.Document;
import com.valstrz.entity.BaseEntity;

import java.time.LocalDate;

/**
 * Лични данни на служител.
 */
@Document("employees")
public class Employee extends BaseEntity {

    // Идентификация
    private String egn;               // ЕГН
    private String lnch;              // ЛНЧ (за чуждестранни, ако няма ЕГН)
    private String firstName;
    private String middleName;
    private String lastName;
    private LocalDate birthDate;
    private String gender;             // M / F
    private String citizenship;

    // Документ за самоличност
    private String idCardNumber;
    private LocalDate idCardDate;
    private String idCardIssuedBy;     // издадена от (МВР)

    // Адрес постоянен
    private String permanentAddress;
    private String permanentCity;
    private String permanentPostalCode;
    private String permanentMunicipality;
    private String permanentRegion;

    // Адрес за кореспонденция
    private String currentAddress;

    // Контакти
    private String phone;
    private String email;

    // Образование
    private int educationCode;         // 1-7 (начално..докторат)
    private String specialty;
    private String school;
    private String diplomaNumber;
    private String degree;             // степен/звание

    // Семейно положение
    private String maritalStatus;
    private int childrenCount;

    // Банкова сметка
    private String iban;
    private String bic;

    // Снимка
    private String photoPath;

    // Статус
    private boolean active;            // активен служител или напуснал

    public Employee() {}

    public String getEgn() { return egn; }
    public void setEgn(String egn) { this.egn = egn; }

    public String getLnch() { return lnch; }
    public void setLnch(String lnch) { this.lnch = lnch; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getCitizenship() { return citizenship; }
    public void setCitizenship(String citizenship) { this.citizenship = citizenship; }

    public String getIdCardNumber() { return idCardNumber; }
    public void setIdCardNumber(String idCardNumber) { this.idCardNumber = idCardNumber; }

    public LocalDate getIdCardDate() { return idCardDate; }
    public void setIdCardDate(LocalDate idCardDate) { this.idCardDate = idCardDate; }

    public String getIdCardIssuedBy() { return idCardIssuedBy; }
    public void setIdCardIssuedBy(String idCardIssuedBy) { this.idCardIssuedBy = idCardIssuedBy; }

    public String getPermanentAddress() { return permanentAddress; }
    public void setPermanentAddress(String permanentAddress) { this.permanentAddress = permanentAddress; }

    public String getPermanentCity() { return permanentCity; }
    public void setPermanentCity(String permanentCity) { this.permanentCity = permanentCity; }

    public String getPermanentPostalCode() { return permanentPostalCode; }
    public void setPermanentPostalCode(String permanentPostalCode) { this.permanentPostalCode = permanentPostalCode; }

    public String getPermanentMunicipality() { return permanentMunicipality; }
    public void setPermanentMunicipality(String permanentMunicipality) { this.permanentMunicipality = permanentMunicipality; }

    public String getPermanentRegion() { return permanentRegion; }
    public void setPermanentRegion(String permanentRegion) { this.permanentRegion = permanentRegion; }

    public String getCurrentAddress() { return currentAddress; }
    public void setCurrentAddress(String currentAddress) { this.currentAddress = currentAddress; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getEducationCode() { return educationCode; }
    public void setEducationCode(int educationCode) { this.educationCode = educationCode; }

    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }

    public String getSchool() { return school; }
    public void setSchool(String school) { this.school = school; }

    public String getDiplomaNumber() { return diplomaNumber; }
    public void setDiplomaNumber(String diplomaNumber) { this.diplomaNumber = diplomaNumber; }

    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }

    public String getMaritalStatus() { return maritalStatus; }
    public void setMaritalStatus(String maritalStatus) { this.maritalStatus = maritalStatus; }

    public int getChildrenCount() { return childrenCount; }
    public void setChildrenCount(int childrenCount) { this.childrenCount = childrenCount; }

    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }

    public String getBic() { return bic; }
    public void setBic(String bic) { this.bic = bic; }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    /** Пълно име */
    public String getFullName() {
        return (firstName != null ? firstName : "") + " "
             + (middleName != null ? middleName : "") + " "
             + (lastName != null ? lastName : "");
    }
}
