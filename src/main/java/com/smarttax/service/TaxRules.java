package com.smarttax.service;

import java.math.BigDecimal;
import java.util.List;

/** Tax Year 2026-27 rates from Finance Bill 2026 memorandum; deduction eligibility remains only partially modeled. */
public record TaxRules(String financialYear, String taxYear, List<Slab> oldSlabs, List<Slab> newSlabs, BigDecimal newRebateThreshold, BigDecimal newRebateMax, BigDecimal oldRebateThreshold, BigDecimal oldRebateMax, BigDecimal newStandardDeduction, BigDecimal oldStandardDeduction) {
    public record Slab(BigDecimal upper, BigDecimal rate) {}
    private static BigDecimal r(String s) { return new BigDecimal(s); }
    private static List<Slab> old() { return List.of(new Slab(r("250000"),r("0")),new Slab(r("500000"),r(".05")),new Slab(r("1000000"),r(".20")),new Slab(null,r(".30"))); }
    private static List<Slab> old(int age) { if(age>=80)return List.of(new Slab(r("500000"),r("0")),new Slab(r("1000000"),r(".20")),new Slab(null,r(".30")));if(age>=60)return List.of(new Slab(r("300000"),r("0")),new Slab(r("500000"),r(".05")),new Slab(r("1000000"),r(".20")),new Slab(null,r(".30")));return old(); }
    private static List<Slab> newer() { return List.of(new Slab(r("400000"),r("0")),new Slab(r("800000"),r(".05")),new Slab(r("1200000"),r(".10")),new Slab(r("1600000"),r(".15")),new Slab(r("2000000"),r(".20")),new Slab(r("2400000"),r(".25")),new Slab(null,r(".30"))); }
    public List<Slab> oldSlabsForAge(int age){return old(age);}
    public static TaxRules forYear(String year) {
        if (!"2026-27".equals(year)) throw new IllegalArgumentException("No verified tax rules configured for FY " + year);
        return new TaxRules(year,year,old(),newer(),r("1200000"),r("60000"),r("500000"),r("12500"),r("75000"),r("50000"));
    }
}
