# Smart Indian Income Tax Calculator

A Java 17 Swing desktop application for recording taxpayers, estimating old and new regime tax, comparing the results, and exporting a summary PDF or CSV. Financial amounts use `BigDecimal`; database access uses JDBC and prepared statements.

## Current scope

This workspace started empty. The application includes a database-backed overview dashboard, taxpayer CRUD/search, a two-regime estimate screen, calculation history search, MySQL schema, and PDF/CSV export. The FY 2026-27 slab configuration is year-keyed. The application deliberately refuses capital-gains inputs and taxable income above ₹50 lakh because capital gains can have special rates and higher-income surcharge treatment needs more detail than the current form collects. Deduction inputs have common caps applied, but section-specific eligibility (including 80G, 80E and 80D) is not fully modeled. Treat outputs as educational estimates, not filing advice.

The FY 2026-27 configuration is based on the official Finance Bill 2026 memorandum's Tax Year 2026-27 rates. Under the Income-tax Act, 2025, the period is called **Tax Year 2026-27**; it does not have a corresponding Assessment Year label. This app displays the tax year separately from FY to avoid presenting AY 2027-28 as the governing period. Always check current official guidance before relying on the estimate.

Sources: [Finance Bill 2026 memorandum, Tax Year 2026-27 rates](https://www.indiabudget.gov.in/doc/memo.pdf) and [Income Tax Department explanation of Tax Year and the new Act](https://www.incometax.gov.in/iec/foportal/help/all-topics/e-filing-services/objective-and-scope-new-act-faq).

## Technology

- Java 17, Swing/AWT, FlatLaf
- Maven
- MySQL 8 and Connector/J
- OpenPDF
- JUnit 5

## Requirements

- JDK 17+
- Maven 3.9+
- MySQL 8.x

## Database setup

1. Install MySQL Server 8 if it is not already installed. Start the Windows service (open `services.msc` and start `MySQL80`, if present). The app connects to `localhost:3306` by default.
2. From this project folder, initialize the database using the MySQL command-line client: `Get-Content -Raw database/schema.sql | mysql -u root -p`.
3. In the same PowerShell window, set your own MySQL credentials before starting the app, for example:

   ```powershell
   $env:DB_URL = 'jdbc:mysql://localhost:3306/smart_income_tax_db?serverTimezone=Asia/Kolkata'
   $env:DB_USER = 'root'
   $env:DB_PASSWORD = 'your-MySQL-password'
   ```

4. Launch the application with `.\mvnw.cmd exec:java`. Taxpayer save, search and history need a running MySQL server and the schema loaded. Connection errors now explain whether the server, database, or credentials need attention.

The schema includes taxpayers, income details, deductions, tax calculations (with JSON snapshots) and calculation-history events. The current desktop flow writes taxpayer and calculation/history records. Income/deduction detail tables are included for extension and are not independently edited by the UI yet.
## Build and run

From this directory:

```bash
mvnw.cmd test
mvnw.cmd exec:java
```

The included Maven Wrapper downloads Maven 3.9.9 on first use. If Maven cannot fetch dependencies, check network access and retry. Import the root folder as an existing Maven project in IntelliJ IDEA, or open it in VS Code with the Extension Pack for Java and Maven for Java.

## Workflow

1. Start MySQL and initialize the schema.
2. Launch the app; select the Taxpayers tab and add a taxpayer.
3. Select a taxpayer row, then return to Tax Calculator.
4. Enter income/deduction values and calculate. The capital-gains field must be zero for now.
5. Save the calculation to history; generate a PDF or CSV in `reports/`.

## Validation and errors

PAN shape, email, mobile, age, and nonnegative numeric inputs are validated. Database, PDF, and input errors are shown in dialogs. Duplicate PAN is enforced by the database unique constraint. Deleting a taxpayer cascades to associated income, deductions, calculations and history.

## Screenshots

Add screenshots here after running the application in a configured MySQL environment.

## Troubleshooting

- **Connection refused / access denied:** check MySQL service, schema creation, and `DB_*` variables.
- **Duplicate PAN:** each PAN can only be registered once.
- **Tax calculation stops:** the configured estimate currently supports FY 2026-27 ordinary slab income only, below ₹50 lakh and without capital gains. Unsupported financial years are rejected.
- **PDF/CSV not visible:** check the `reports/` directory under the application working directory.

## Future enhancements

Add fully modeled deduction eligibility and regime restrictions, business-income regime selection, capital-gains categories/rates, surcharge and marginal relief above ₹50 lakh, independent income/deduction record editing, reopen/report workflows for saved calculations, recent-calculation dashboard cards, and rule configurations for additional tax years.
