package com.valstrz.service;

import com.valstrz.entity.personnel.Garnishment;
import com.valstrz.util.MoneyUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GarnishmentService {

    /**
     * Изчислява общата секвестируема сума (секвестируемия доход) съгласно чл. 446 от ГПК.
     * 
     * @param netSalary Нетна заплата (след данъци и осигуровки)
     * @param minSalary Минимална работна заплата (МРЗ) за периода
     * @param hasChildren Дали служителят има деца, които издържа
     * @return Секвестируемата сума за месеца
     */
    public BigDecimal calculateGarnishableAmount(BigDecimal netSalary, BigDecimal minSalary, boolean hasChildren) {
        if (netSalary == null || minSalary == null) return BigDecimal.ZERO;
        
        // 1. Несеквестируем минимум: ако нетното възнаграждение е до размера на МРЗ, не се удържа нищо.
        if (netSalary.compareTo(minSalary) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal ratio = netSalary.divide(minSalary, MoneyUtil.CALC_SCALE, MoneyUtil.ROUNDING);

        // 2. Нетен доход между една и две МРЗ
        if (ratio.compareTo(new BigDecimal("2")) < 0) {
            if (!hasChildren) {
                // без деца: 1/3 от нетното възнаграждение
                return netSalary.divide(new BigDecimal("3"), MoneyUtil.MONEY_SCALE, MoneyUtil.ROUNDING);
            } else {
                // с деца: 1/4 от нетното възнаграждение
                return netSalary.divide(new BigDecimal("4"), MoneyUtil.MONEY_SCALE, MoneyUtil.ROUNDING);
            }
        }
        
        // 3. Нетен доход между две и четири МРЗ
        if (ratio.compareTo(new BigDecimal("4")) < 0) {
            if (!hasChildren) {
                // без деца: 1/2 от нетното възнаграждение
                return netSalary.divide(new BigDecimal("2"), MoneyUtil.MONEY_SCALE, MoneyUtil.ROUNDING);
            } else {
                // с деца: 1/3 от нетното възнаграждение
                return netSalary.divide(new BigDecimal("3"), MoneyUtil.MONEY_SCALE, MoneyUtil.ROUNDING);
            }
        }
        
        // 4. Нетен доход над четири МРЗ
        // ГПК чл. 446 (1) т. 4: удържа се горницата над двукратния размер на МРЗ (без деца) 
        // или над два и половина кратния размер на МРЗ (с деца).
        if (!hasChildren) {
            BigDecimal threshold = minSalary.multiply(new BigDecimal("2"));
            return netSalary.subtract(threshold).max(BigDecimal.ZERO);
        } else {
            BigDecimal threshold = minSalary.multiply(new BigDecimal("2.5"));
            return netSalary.subtract(threshold).max(BigDecimal.ZERO);
        }
    }

    /**
     * Разпределя сумите между активните запори съгласно правилата за приоритет и ГПК.
     */
    public List<GarnishmentDeduction> distribute(List<Garnishment> garnishments, 
                                                  BigDecimal netSalary, 
                                                  BigDecimal minSalary) {
        List<GarnishmentDeduction> results = new ArrayList<>();
        if (garnishments == null || garnishments.isEmpty()) return results;

        // 1. Проверяваме дали служителят има деца (за ГПК лимита)
        boolean hasChildren = garnishments.stream().anyMatch(Garnishment::isHasChildren);
        
        // 2. Изчисляваме общия лимит за "нормални" запори по ГПК
        BigDecimal totalGarnishable = calculateGarnishableAmount(netSalary, minSalary, hasChildren);
        BigDecimal remainingGarnishable = totalGarnishable;
        BigDecimal remainingNet = netSalary;

        // 3. Сортираме запорите по тип (Издръжка първо) и приоритет
        List<Garnishment> sorted = garnishments.stream()
                .filter(Garnishment::isActive)
                .sorted((g1, g2) -> {
                    // Издръжката е с най-висок приоритет по закон
                    if (g1.getType() == Garnishment.GarnishmentType.ALIMONY && g2.getType() != Garnishment.GarnishmentType.ALIMONY) return -1;
                    if (g1.getType() != Garnishment.GarnishmentType.ALIMONY && g2.getType() == Garnishment.GarnishmentType.ALIMONY) return 1;
                    return Integer.compare(g1.getPriority(), g2.getPriority());
                })
                .collect(Collectors.toList());

        for (Garnishment g : sorted) {
            BigDecimal amountToDeduct = BigDecimal.ZERO;

            if (g.getType() == Garnishment.GarnishmentType.ALIMONY) {
                // Издръжката не е ограничена от ГПК лимитите, а само от нетото
                amountToDeduct = g.getMonthlyAmount() != null ? g.getMonthlyAmount() : BigDecimal.ZERO;
                if (amountToDeduct.compareTo(remainingNet) > 0) {
                    amountToDeduct = remainingNet;
                }
                
                // Намаляваме наличния лимит за останалите запори
                remainingGarnishable = remainingGarnishable.subtract(amountToDeduct);
                if (remainingGarnishable.compareTo(BigDecimal.ZERO) < 0) {
                    remainingGarnishable = BigDecimal.ZERO;
                }
            } else {
                // ЧСИ / Публичен запор - ограничени от оставащия ГПК лимит
                if (remainingGarnishable.compareTo(BigDecimal.ZERO) <= 0) continue;

                BigDecimal debt = g.getRemainingAmount();
                amountToDeduct = remainingGarnishable;
                if (debt != null && amountToDeduct.compareTo(debt) > 0) {
                    amountToDeduct = debt;
                }
                
                remainingGarnishable = remainingGarnishable.subtract(amountToDeduct);
            }

            if (amountToDeduct.compareTo(BigDecimal.ZERO) > 0) {
                results.add(new GarnishmentDeduction(g.getId(), g.getDescription(), amountToDeduct));
                remainingNet = remainingNet.subtract(amountToDeduct);
            }
        }

        return results;
    }

    public record GarnishmentDeduction(String garnishmentId, String name, BigDecimal amount) {}
}
