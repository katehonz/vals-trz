package com.valstrz.entity.company;

/**
 * Данни за физическо лице (ръководител, подаващ в НАП/НОИ).
 * Вграден обект (не е отделен документ в ArangoDB).
 */
public class PersonInfo {

    private String name;
    private String title;          // длъжност
    private String egn;            // ЕГН
    private String citizenship;    // гражданство
    private String idCardNumber;   // номер на ЛК
    private String idCardDate;     // дата на издаване
    private String idCardIssuedBy; // издадена от
    private String address;
    private String municipality;   // община
    private String region;         // област

    public PersonInfo() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getEgn() { return egn; }
    public void setEgn(String egn) { this.egn = egn; }

    public String getCitizenship() { return citizenship; }
    public void setCitizenship(String citizenship) { this.citizenship = citizenship; }

    public String getIdCardNumber() { return idCardNumber; }
    public void setIdCardNumber(String idCardNumber) { this.idCardNumber = idCardNumber; }

    public String getIdCardDate() { return idCardDate; }
    public void setIdCardDate(String idCardDate) { this.idCardDate = idCardDate; }

    public String getIdCardIssuedBy() { return idCardIssuedBy; }
    public void setIdCardIssuedBy(String idCardIssuedBy) { this.idCardIssuedBy = idCardIssuedBy; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getMunicipality() { return municipality; }
    public void setMunicipality(String municipality) { this.municipality = municipality; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
}
