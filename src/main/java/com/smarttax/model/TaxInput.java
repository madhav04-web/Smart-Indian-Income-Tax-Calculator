package com.smarttax.model;

import java.math.BigDecimal;

public record TaxInput(int age, BigDecimal salary, BigDecimal houseProperty, BigDecimal business, BigDecimal capitalGains, BigDecimal otherIncome, BigDecimal section80c, BigDecimal section80d, BigDecimal section80g, BigDecimal section80e, BigDecimal section80tta, BigDecimal section80ccd1b, BigDecimal otherDeductions, BigDecimal hraExemption, BigDecimal standardDeduction, boolean residentIndividual) {
    public TaxInput {
        for (BigDecimal n : new BigDecimal[]{salary,houseProperty,business,capitalGains,otherIncome,section80c,section80d,section80g,section80e,section80tta,section80ccd1b,otherDeductions,hraExemption,standardDeduction})
            if (n == null) throw new IllegalArgumentException("Financial amounts are required");
        if(age<1||age>120) throw new IllegalArgumentException("Age must be between 1 and 120");
    }
    public BigDecimal gross() { return salary.add(houseProperty).add(business).add(capitalGains).add(otherIncome); }
}
