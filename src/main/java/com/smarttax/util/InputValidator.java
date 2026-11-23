package com.smarttax.util;

import java.math.BigDecimal;

public final class InputValidator {
    private InputValidator() {}
    public static boolean pan(String s) { return s!=null && s.trim().toUpperCase().matches("[A-Z]{5}[0-9]{4}[A-Z]"); }
    public static boolean email(String s) { return s!=null && s.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"); }
    public static boolean mobile(String s) { return s!=null && s.matches("[0-9]{10}"); }
    public static BigDecimal amount(String s) { BigDecimal n=new BigDecimal(s.trim()); if(n.signum()<0) throw new IllegalArgumentException("Amounts cannot be negative"); return n; }
}
