package com.valstrz.entity.calendar;

import com.arangodb.springframework.annotation.Document;
import com.valstrz.entity.BaseEntity;

import java.math.BigDecimal;

/**
 * Часова схема на работа.
 * Напр.: Пълно работно време (8ч), Непълно 7ч, 6ч, 4ч, СИРВ и др.
 */
@Document("workSchedules")
public class WorkSchedule extends BaseEntity {

    private String code;              // код на схемата
    private String name;              // наименование
    private BigDecimal hoursPerDay;   // брой часове на ден

    public WorkSchedule() {}

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getHoursPerDay() { return hoursPerDay; }
    public void setHoursPerDay(BigDecimal hoursPerDay) { this.hoursPerDay = hoursPerDay; }
}
