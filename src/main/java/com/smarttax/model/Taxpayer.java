package com.smarttax.model;

public record Taxpayer(Long id, String fullName, String pan, int age, String email, String mobile, String residentialStatus, String financialYear, String taxYear) {}
