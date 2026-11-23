package com.smarttax.dao;

import com.smarttax.config.DatabaseConnection;
import com.smarttax.model.Taxpayer;
import java.sql.*;
import java.util.*;

public final class TaxpayerDAO {
    public List<Taxpayer> find(String query) throws SQLException {
        String q="%"+(query==null?"":query.trim())+"%";
        try(Connection c=DatabaseConnection.open();PreparedStatement p=c.prepareStatement("SELECT * FROM taxpayers WHERE full_name LIKE ? OR pan LIKE ? OR CAST(id AS CHAR) LIKE ? ORDER BY id DESC")) {
            p.setString(1,q);p.setString(2,q);p.setString(3,q);try(ResultSet r=p.executeQuery()){List<Taxpayer> out=new ArrayList<>();while(r.next())out.add(read(r));return out;}
        }
    }
    public void save(Taxpayer t) throws SQLException {
        String sql=t.id()==null?"INSERT INTO taxpayers(full_name,pan,age,email,mobile,residential_status,financial_year,tax_year) VALUES(?,?,?,?,?,?,?,?)":"UPDATE taxpayers SET full_name=?,pan=?,age=?,email=?,mobile=?,residential_status=?,financial_year=?,tax_year=? WHERE id=?";
        try(Connection c=DatabaseConnection.open();PreparedStatement p=c.prepareStatement(sql)) {bind(p,t);if(t.id()!=null)p.setLong(9,t.id());if(p.executeUpdate()!=1)throw new SQLException("Taxpayer record was not saved.");}
    }
    public void delete(long id) throws SQLException { try(Connection c=DatabaseConnection.open();PreparedStatement p=c.prepareStatement("DELETE FROM taxpayers WHERE id=?")){p.setLong(1,id);if(p.executeUpdate()!=1)throw new SQLException("Taxpayer no longer exists.");} }
    private void bind(PreparedStatement p,Taxpayer t)throws SQLException {p.setString(1,t.fullName());p.setString(2,t.pan().toUpperCase());p.setInt(3,t.age());p.setString(4,t.email());p.setString(5,t.mobile());p.setString(6,t.residentialStatus());p.setString(7,t.financialYear());p.setString(8,t.taxYear());}
    private Taxpayer read(ResultSet r)throws SQLException{return new Taxpayer(r.getLong("id"),r.getString("full_name"),r.getString("pan"),r.getInt("age"),r.getString("email"),r.getString("mobile"),r.getString("residential_status"),r.getString("financial_year"),r.getString("tax_year"));}
}
