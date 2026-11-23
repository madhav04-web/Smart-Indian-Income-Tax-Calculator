package com.smarttax.dao;

import com.smarttax.config.DatabaseConnection;
import com.smarttax.model.*;
import com.smarttax.service.TaxCalculationService;
import java.sql.*;
import java.util.*;

public final class CalculationDAO {
    public DashboardStats dashboardStats() throws SQLException {
        String sql="SELECT (SELECT COUNT(*) FROM taxpayers) taxpayers,(SELECT COUNT(*) FROM tax_calculations) calculations,(SELECT COALESCE(AVG(LEAST(old_tax,new_tax)),0) FROM tax_calculations) avg_tax";
        try(Connection db=DatabaseConnection.open();PreparedStatement p=db.prepareStatement(sql);ResultSet r=p.executeQuery()){r.next();return new DashboardStats(r.getLong("taxpayers"),r.getLong("calculations"),r.getBigDecimal("avg_tax"));}
    }
    public List<CalculationHistory> history(String pan, String year) throws SQLException {
        String sql="SELECT c.id,t.full_name,t.pan,c.financial_year,c.gross_income,c.eligible_deductions,c.old_tax,c.new_tax,c.recommended_regime,c.calculation_date FROM tax_calculations c JOIN taxpayers t ON t.id=c.taxpayer_id WHERE (?='' OR t.pan LIKE ?) AND (?='' OR c.financial_year=?) ORDER BY c.calculation_date DESC";
        try(Connection db=DatabaseConnection.open();PreparedStatement p=db.prepareStatement(sql)) {String key=pan==null?"":pan.trim();String fy=year==null?"":year.trim();p.setString(1,key);p.setString(2,"%"+key+"%");p.setString(3,fy);p.setString(4,fy);try(ResultSet r=p.executeQuery()){List<CalculationHistory> rows=new ArrayList<>();while(r.next())rows.add(new CalculationHistory(r.getLong("id"),r.getString("full_name"),r.getString("pan"),r.getString("financial_year"),r.getBigDecimal("gross_income"),r.getBigDecimal("eligible_deductions"),r.getBigDecimal("old_tax"),r.getBigDecimal("new_tax"),r.getString("recommended_regime"),r.getTimestamp("calculation_date").toLocalDateTime()));return rows;}}
    }
    public void save(long taxpayerId,String year,TaxInput input,TaxCalculationService.Comparison c)throws SQLException {
        String sql="INSERT INTO tax_calculations(taxpayer_id,financial_year,gross_income,eligible_deductions,old_tax,new_tax,recommended_regime,snapshot_json) VALUES(?,?,?,?,?,?,?,JSON_OBJECT('salary',?,'houseProperty',?,'business',?,'capitalGains',?,'otherIncome',?,'section80c',?,'section80d',?,'section80g',?,'section80e',?,'section80tta',?,'section80ccd1b',?,'otherDeductions',?,'hraExemption',?,'standardDeduction',?))";
        try(Connection db=DatabaseConnection.open()) {
            db.setAutoCommit(false);
            try(PreparedStatement p=db.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
                p.setLong(1,taxpayerId); p.setString(2,year); p.setBigDecimal(3,input.gross());
                p.setBigDecimal(4,c.oldRegime().deductions()); p.setBigDecimal(5,c.oldRegime().taxPayable());
                p.setBigDecimal(6,c.newRegime().taxPayable()); p.setString(7,c.recommendedRegime());
                int n=8;
                for(var v:new java.math.BigDecimal[]{input.salary(),input.houseProperty(),input.business(),input.capitalGains(),input.otherIncome(),input.section80c(),input.section80d(),input.section80g(),input.section80e(),input.section80tta(),input.section80ccd1b(),input.otherDeductions(),input.hraExemption(),input.standardDeduction()}) p.setBigDecimal(n++,v);
                p.executeUpdate();
                try(ResultSet keys=p.getGeneratedKeys()) {
                    keys.next(); long id=keys.getLong(1);
                    try(PreparedStatement h=db.prepareStatement("INSERT INTO tax_calculation_history(calculation_id,taxpayer_id) VALUES(?,?)")) { h.setLong(1,id); h.setLong(2,taxpayerId); h.executeUpdate(); }
                }
                db.commit();
            } catch(SQLException e) { db.rollback(); throw e; }
        }
    }
}
