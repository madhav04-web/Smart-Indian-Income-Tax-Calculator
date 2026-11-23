package com.smarttax.service;

import com.smarttax.model.*;
import java.math.*;
import java.util.List;

public final class TaxCalculationService {
    private static final BigDecimal ZERO=BigDecimal.ZERO, CESS=new BigDecimal("0.04");
    private record Calc(BigDecimal base, BigDecimal rebate, BigDecimal surcharge, BigDecimal cess, BigDecimal due, BigDecimal taxable) {}
    public record Comparison(TaxResult oldRegime, TaxResult newRegime, String recommendedRegime, BigDecimal savings) {}
    public Comparison compare(TaxInput in, String financialYear) {
        if (in.capitalGains().signum()!=0) throw new IllegalArgumentException("Capital gains may use special rates. Remove the amount or calculate that income separately before comparing regimes.");
        TaxRules rules=TaxRules.forYear(financialYear); BigDecimal gross=in.gross().max(ZERO);
        BigDecimal oldDed=eligibleOld(in), newDed=in.standardDeduction().min(rules.newStandardDeduction()).max(ZERO);
        Calc old=calculate(gross,oldDed,rules.oldSlabsForAge(in.age()),rules.oldRebateThreshold(),rules.oldRebateMax(),in.residentIndividual());
        Calc newer=calculate(gross,newDed,rules.newSlabs(),rules.newRebateThreshold(),rules.newRebateMax(),in.residentIndividual());
        TaxResult o=result("OLD",gross,oldDed,old), n=result("NEW",gross,newDed,newer);
        return new Comparison(o,n,o.taxPayable().compareTo(n.taxPayable())<0?"OLD":"NEW",o.taxPayable().subtract(n.taxPayable()).abs());
    }
    private BigDecimal eligibleOld(TaxInput i) {
        return i.section80c().min(new BigDecimal("150000")).max(ZERO).add(i.section80d().min(new BigDecimal("100000")).max(ZERO)).add(i.section80g().max(ZERO)).add(i.section80e().max(ZERO)).add(i.section80tta().min(new BigDecimal("10000")).max(ZERO)).add(i.section80ccd1b().min(new BigDecimal("50000")).max(ZERO)).add(i.otherDeductions().max(ZERO)).add(i.hraExemption().max(ZERO)).add(i.standardDeduction().min(new BigDecimal("50000")).max(ZERO));
    }
    private Calc calculate(BigDecimal gross,BigDecimal deductions,List<TaxRules.Slab> slabs,BigDecimal rebateThreshold,BigDecimal rebateMax,boolean resident) {
        BigDecimal taxable=gross.subtract(deductions).max(ZERO).setScale(0,RoundingMode.DOWN), base=slabTax(taxable,slabs), rebate=ZERO;
        if(resident) {
            if(taxable.compareTo(rebateThreshold)<=0) rebate=base.min(rebateMax);
            else rebate=base.subtract(taxable.subtract(rebateThreshold)).max(ZERO).min(rebateMax);
        }
        BigDecimal net=base.subtract(rebate).max(ZERO);
        if(taxable.compareTo(new BigDecimal("5000000"))>0) throw new IllegalArgumentException("Surcharge and marginal relief above ₹50 lakh are not enabled; calculation stopped to avoid an inaccurate estimate.");
        BigDecimal cess=net.multiply(CESS).setScale(0,RoundingMode.HALF_UP), due=net.add(cess).setScale(0,RoundingMode.HALF_UP);
        return new Calc(base,rebate,ZERO,cess,due,taxable);
    }
    private BigDecimal slabTax(BigDecimal income,List<TaxRules.Slab> slabs) {
        BigDecimal low=ZERO,tax=ZERO;
        for(TaxRules.Slab s:slabs) { BigDecimal top=s.upper()==null?income:income.min(s.upper()); if(top.compareTo(low)>0) tax=tax.add(top.subtract(low).multiply(s.rate())); if(s.upper()==null||income.compareTo(s.upper())<=0) break; low=s.upper(); }
        return tax.setScale(0,RoundingMode.HALF_UP);
    }
    private TaxResult result(String regime,BigDecimal gross,BigDecimal ded,Calc c) {
        BigDecimal eff=gross.signum()==0?ZERO:c.due().multiply(new BigDecimal("100")).divide(gross,2,RoundingMode.HALF_UP);
        return new TaxResult(regime,gross,ded,c.taxable(),c.base(),c.rebate(),c.surcharge(),c.cess(),c.due(),eff);
    }
}
