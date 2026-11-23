package com.smarttax.model;

import java.math.BigDecimal;

public record DashboardStats(long taxpayerCount, long calculationCount, BigDecimal averageRecommendedTax) {}
