package com.smarttax.model;

import java.math.BigDecimal;

public record TaxResult(String regime, BigDecimal grossIncome, BigDecimal deductions, BigDecimal taxableIncome, BigDecimal taxBeforeRebate, BigDecimal rebate, BigDecimal surcharge, BigDecimal cess, BigDecimal taxPayable, BigDecimal effectiveRate) {}
