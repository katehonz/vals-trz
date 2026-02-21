package com.valstrz.entity.company;

import com.arangodb.springframework.annotation.Document;
import com.valstrz.entity.BaseEntity;

import java.math.BigDecimal;
import java.util.List;

/**
 * Настройки за ДТВ за ТСПО (допълнително трудово възнаграждение
 * за трудов стаж и професионален опит).
 */
@Document("seniorityBonusConfigs")
public class SeniorityBonusConfig extends BaseEntity {

    private BigDecimal percentPerYear;         // процент за 1 година стаж (напр. 0.6)
    private boolean autoUpdateOnMonthClose;    // промяна на класа при приключване на месец
    private List<SeniorityBracket> brackets;   // таблица с проценти по години

    public SeniorityBonusConfig() {}

    public BigDecimal getPercentPerYear() { return percentPerYear; }
    public void setPercentPerYear(BigDecimal percentPerYear) { this.percentPerYear = percentPerYear; }

    public boolean isAutoUpdateOnMonthClose() { return autoUpdateOnMonthClose; }
    public void setAutoUpdateOnMonthClose(boolean autoUpdateOnMonthClose) { this.autoUpdateOnMonthClose = autoUpdateOnMonthClose; }

    public List<SeniorityBracket> getBrackets() { return brackets; }
    public void setBrackets(List<SeniorityBracket> brackets) { this.brackets = brackets; }

    /**
     * Диапазон години стаж -> процент ДТВ.
     * Напр.: от 1.01 до 2.00 години -> 0.60%
     */
    public static class SeniorityBracket {
        private BigDecimal fromYears;
        private BigDecimal toYears;
        private BigDecimal percent;

        public SeniorityBracket() {}

        public BigDecimal getFromYears() { return fromYears; }
        public void setFromYears(BigDecimal fromYears) { this.fromYears = fromYears; }

        public BigDecimal getToYears() { return toYears; }
        public void setToYears(BigDecimal toYears) { this.toYears = toYears; }

        public BigDecimal getPercent() { return percent; }
        public void setPercent(BigDecimal percent) { this.percent = percent; }
    }
}
