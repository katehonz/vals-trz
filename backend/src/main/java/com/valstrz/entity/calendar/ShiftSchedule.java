package com.valstrz.entity.calendar;

import com.arangodb.springframework.annotation.Document;
import com.valstrz.entity.BaseEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * График за работа на смени.
 * Дефинира шаблон от смени (дневна, нощна, почивка) за период.
 * Използва се при сумирано изчисляване на работното време (СИРВ).
 */
@Document("shiftSchedules")
public class ShiftSchedule extends BaseEntity {

    private String name;              // напр. "12/24 двусменен", "Тридневен цикъл"
    private String code;              // кратък код
    private String type;              // ROTATING, FIXED, FLEXIBLE
    private int referenceMonths;      // период на СИРВ (1, 2, 3, 4 или 6 месеца)

    private List<ShiftDefinition> shifts;  // дефиниции на смените
    private List<Integer> rotationPattern; // ротационен модел (индекси в shifts[] + 0=почивен)

    private boolean active;

    public ShiftSchedule() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getReferenceMonths() { return referenceMonths; }
    public void setReferenceMonths(int referenceMonths) { this.referenceMonths = referenceMonths; }

    public List<ShiftDefinition> getShifts() { return shifts; }
    public void setShifts(List<ShiftDefinition> shifts) { this.shifts = shifts; }

    public List<Integer> getRotationPattern() { return rotationPattern; }
    public void setRotationPattern(List<Integer> rotationPattern) { this.rotationPattern = rotationPattern; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    /**
     * Дефиниция на смяна.
     */
    public static class ShiftDefinition {
        private int index;           // 1-based, 0 = почивен ден
        private String name;         // "Дневна", "Нощна", "12-часова ден"
        private LocalTime startTime; // начало
        private LocalTime endTime;   // край
        private double totalHours;   // общо часове
        private double nightHours;   // от тях нощни (22:00-06:00)

        public ShiftDefinition() {}

        public int getIndex() { return index; }
        public void setIndex(int index) { this.index = index; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public LocalTime getStartTime() { return startTime; }
        public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

        public LocalTime getEndTime() { return endTime; }
        public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

        public double getTotalHours() { return totalHours; }
        public void setTotalHours(double totalHours) { this.totalHours = totalHours; }

        public double getNightHours() { return nightHours; }
        public void setNightHours(double nightHours) { this.nightHours = nightHours; }
    }
}
