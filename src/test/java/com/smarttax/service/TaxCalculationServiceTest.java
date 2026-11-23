package com.smarttax.service;

import com.smarttax.model.TaxInput;
import com.smarttax.model.Taxpayer;
import com.smarttax.util.InputValidator;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class TaxCalculationServiceTest {
    private TaxInput input(String salary) { BigDecimal z=BigDecimal.ZERO;return new TaxInput(40,new BigDecimal(salary),z,z,z,z,z,z,z,z,z,z,z,z,z,true); }
    @Test void newRegimeRebateAtTwelveLakhThreshold(){var c=new TaxCalculationService().compare(input("1200000"),"2026-27");assertEquals(new BigDecimal("0"),c.newRegime().taxPayable());assertEquals("NEW",c.recommendedRegime());}
    @Test void oldRegimeSlabAndCess(){var c=new TaxCalculationService().compare(input("1000000"),"2026-27");assertEquals(new BigDecimal("117000"),c.oldRegime().taxPayable());}
    @Test void appliesCessAboveRebateBand(){var c=new TaxCalculationService().compare(input("1300000"),"2026-27");assertEquals(new BigDecimal("78000"),c.newRegime().taxPayable());}
    @Test void appliesMarginalReliefJustAboveNewRebateThreshold(){var c=new TaxCalculationService().compare(input("1201000"),"2026-27");assertEquals(new BigDecimal("1040"),c.newRegime().taxPayable());}
    @Test void rejectsUnsupportedCapitalGains(){BigDecimal z=BigDecimal.ZERO;TaxInput i=new TaxInput(40,z,z,z,new BigDecimal("1000"),z,z,z,z,z,z,z,z,z,z,true);assertThrows(IllegalArgumentException.class,()->new TaxCalculationService().compare(i,"2026-27"));}
    @Test void refusesUnknownYear(){assertThrows(IllegalArgumentException.class,()->new TaxCalculationService().compare(input("100000"),"2025-26"));}
    @Test void appliesSeniorCitizenOldRegimeBasicExemption(){BigDecimal z=BigDecimal.ZERO;TaxInput senior=new TaxInput(65,new BigDecimal("300000"),z,z,z,z,z,z,z,z,z,z,z,z,z,true);assertEquals(new BigDecimal("0"),new TaxCalculationService().compare(senior,"2026-27").oldRegime().taxPayable());}
    @Test void validatesIndianPanShape(){assertTrue(InputValidator.pan("ABCDE1234F"));assertFalse(InputValidator.pan("ABCDE12345"));}
    @Test void generatesPdfReport() throws Exception {TaxInput i=input("1000000");var comparison=new TaxCalculationService().compare(i,"2026-27");Taxpayer person=new Taxpayer(1L,"Test Taxpayer","ABCDE1234F",40,"test@example.com","9876543210","Resident","2026-27","2026-27");Path file=new ReportService().pdf(person,i,comparison);try{assertTrue(Files.size(file)>100);}finally{Files.deleteIfExists(file);}}
}
