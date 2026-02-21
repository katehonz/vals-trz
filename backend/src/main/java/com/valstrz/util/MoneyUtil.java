package com.valstrz.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Централизирана помощна логика за парични изчисления в ТРЗ.
 *
 * Правила:
 * - Крайни суми (заплата, данък, осигуровка) → scale=2, HALF_UP
 * - Междинни стойности (коефициент за стаж, дневна ставка) → scale=6, HALF_UP
 * - Процентни ставки → scale=4, HALF_UP
 * - Никога double/float за пари!
 */
public final class MoneyUtil {

    /** Закръгляне за крайни парични суми: 2 знака */
    public static final int MONEY_SCALE = 2;

    /** Закръгляне за междинни изчисления: 6 знака */
    public static final int CALC_SCALE = 6;

    /** Закръгляне за процентни ставки: 4 знака */
    public static final int RATE_SCALE = 4;

    /** Стандартен режим на закръгляне за ТРЗ */
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /** MathContext за междинни изчисления */
    public static final MathContext MC = new MathContext(16, ROUNDING);

    public static final BigDecimal HUNDRED = new BigDecimal("100");
    public static final BigDecimal TWELVE = new BigDecimal("12");

    private MoneyUtil() {}

    // ── Закръгляне ──

    /** Закръгля до 2 знака (крайна сума). */
    public static BigDecimal round(BigDecimal amount) {
        return amount.setScale(MONEY_SCALE, ROUNDING);
    }

    /** Закръгля междинен резултат (6 знака). */
    public static BigDecimal roundCalc(BigDecimal amount) {
        return amount.setScale(CALC_SCALE, ROUNDING);
    }

    /** Закръгля процентна ставка (4 знака). */
    public static BigDecimal roundRate(BigDecimal rate) {
        return rate.setScale(RATE_SCALE, ROUNDING);
    }

    // ── Процентни изчисления ──

    /** Изчислява процент от сума: amount * percent / 100, междинно. */
    public static BigDecimal percentOf(BigDecimal amount, BigDecimal percent) {
        return amount.multiply(percent, MC).divide(HUNDRED, CALC_SCALE, ROUNDING);
    }

    /** Изчислява процент от сума, закръглен до 2 знака (крайна сума). */
    public static BigDecimal percentOfRounded(BigDecimal amount, BigDecimal percent) {
        return round(percentOf(amount, percent));
    }

    // ── Дневна/часова ставка ──

    /** Месечна заплата / работни дни = дневна ставка (междинна). */
    public static BigDecimal dailyRate(BigDecimal monthlySalary, int workingDays) {
        if (workingDays == 0) return BigDecimal.ZERO;
        return monthlySalary.divide(BigDecimal.valueOf(workingDays), CALC_SCALE, ROUNDING);
    }

    /** Месечна заплата / работни часове = часова ставка (междинна). */
    public static BigDecimal hourlyRate(BigDecimal monthlySalary, BigDecimal workingHours) {
        if (workingHours.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return monthlySalary.divide(workingHours, CALC_SCALE, ROUNDING);
    }

    // ── Сумиране ──

    /** Безопасно събиране (null = 0). */
    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return (a != null ? a : BigDecimal.ZERO).add(b != null ? b : BigDecimal.ZERO);
    }

    /** Безопасно изваждане (null = 0). */
    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return (a != null ? a : BigDecimal.ZERO).subtract(b != null ? b : BigDecimal.ZERO);
    }

    /** Безопасно умножение (null = 0). */
    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return BigDecimal.ZERO;
        return a.multiply(b, MC);
    }

    // ── Валидация ──

    /** Проверява дали сумата е положителна. */
    public static boolean isPositive(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /** Проверява дали е нула или null. */
    public static boolean isZeroOrNull(BigDecimal amount) {
        return amount == null || amount.compareTo(BigDecimal.ZERO) == 0;
    }
}
