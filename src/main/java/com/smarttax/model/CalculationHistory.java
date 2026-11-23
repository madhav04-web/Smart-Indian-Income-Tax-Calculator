package com.smarttax.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CalculationHistory(long id, String taxpayerName, String pan, String financialYear, BigDecimal grossIncome, BigDecimal eligibleDeductions, BigDecimal oldTax, BigDecimal newTax, String recommendation, LocalDateTime calculatedAt) {}
